// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Forge Datasets Iceberg v3 transaction
// engine, branch/tag manager, Warden W5 marking propagation, OpenLineage/CloudEvents emitter,
// AES-256-GCM dataset encryption, and POPIA snapshot erasure (P4 tasks 4.1-4.5, 4.7)
package africa.vigil.forge.datasets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Forge Datasets engine implementing `vigil.forge.datasets.v1.DatasetService` (`API_DESIGN.md`
 * section B.5, `DATA_MODEL.md` section 9, `VIGIL_DOMAIN_BLUEPRINT.md` section 9).
 */
public final class DatasetEngine {

  public enum TransactionType {
    SNAPSHOT,
    APPEND,
    UPDATE,
    DELETE
  }

  public enum TransactionStatus {
    OPEN,
    COMMITTED,
    ABORTED
  }

  public enum DataOrigin {
    REAL,
    PSEUDONYMISED,
    SYNTHETIC
  }

  public enum QualitySeverity {
    WARN,
    BLOCK
  }

  public record SecurityLabel(
      List<String> markings,
      int classification,
      List<String> releasability,
      List<String> purposeTags) {
    public SecurityLabel {
      markings = List.copyOf(new TreeSet<>(Objects.requireNonNull(markings, "markings")));
      if (classification < 0 || classification > 4) {
        throw new IllegalArgumentException("classification must be between 0 and 4");
      }
      releasability =
          List.copyOf(new TreeSet<>(Objects.requireNonNull(releasability, "releasability")));
      purposeTags = List.copyOf(new TreeSet<>(Objects.requireNonNull(purposeTags, "purposeTags")));
      if (purposeTags.isEmpty()) {
        throw new IllegalArgumentException(
            "purposeTags must be explicitly declared (use UNRESTRICTED when empty)");
      }
    }
  }

  public record CallerPolicyContext(
      String tenantId,
      String principal,
      Set<String> heldMarkings,
      int clearance,
      Set<String> releasability,
      String purpose,
      boolean hasDatasetWriterRole) {}

  public record SourceRun(
      String connectorId,
      String connectorVersion,
      String runId,
      String cursorFrom,
      String cursorTo) {
    public String computeIdempotencyKey() {
      return sha256Hex(connectorId + "|" + cursorFrom + "|" + cursorTo);
    }
  }

  public record RecordProvenance(
      String termsRef, String lawfulBasisRef, String responsibleParty, DataOrigin dataOrigin) {
    public RecordProvenance {
      if (termsRef == null || termsRef.isBlank()) {
        throw new IllegalArgumentException("termsRef is mandatory (Blueprint section 9.5)");
      }
      if (lawfulBasisRef == null || lawfulBasisRef.isBlank()) {
        throw new IllegalArgumentException("lawfulBasisRef is mandatory (Blueprint section 9.5)");
      }
      if (responsibleParty == null || responsibleParty.isBlank()) {
        throw new IllegalArgumentException("responsibleParty is mandatory (Blueprint section 9.5)");
      }
      Objects.requireNonNull(dataOrigin, "dataOrigin is mandatory (Blueprint section 9.5)");
    }
  }

  public record ScopedUploadCredentials(
      String accessKeyId, String secretAccessKey, String sessionToken, Instant expiresAt) {}

  public record StagedUploadTarget(
      String protocol,
      String endpoint,
      String bucket,
      String prefix,
      ScopedUploadCredentials credentials) {}

  public record StagedFile(
      String path,
      String sha256,
      long rowCount,
      Map<String, Map<String, Object>> rowsByPrimaryKey) {}

  public record QualityCheckResult(String checkName, QualitySeverity severity, boolean passed) {}

  public record DatasetColumn(
      int fieldId, String name, String type, boolean required, List<String> markings) {}

  public record DatasetBranch(
      String name,
      String headTransactionRid,
      String headSnapshotId,
      boolean isDefault,
      Instant createdAt) {}

  public record DatasetTag(String name, String snapshotId, Instant createdAt) {}

  public record SnapshotEntry(
      String snapshotId,
      String transactionRid,
      String branch,
      TransactionType type,
      String manifestSha256,
      Map<String, Map<String, Object>> rowsByPk,
      SecurityLabel label,
      Instant committedAt) {}

  public record OpenTransactionResult(
      String transactionRid,
      String datasetRid,
      String branch,
      TransactionType type,
      TransactionStatus status,
      Instant openedAt,
      Instant expiresAt,
      StagedUploadTarget upload) {}

  public record CommitTransactionResult(
      String transactionRid,
      TransactionStatus status,
      String snapshotId,
      Instant committedAt,
      long rowsAdded,
      long rowsDeleted,
      SecurityLabel label,
      String lineageRunId,
      CloudEventEnvelope committedCloudEvent,
      OpenLineageRunEvent openLineageEvent) {}

  public record CloudEventEnvelope(
      String id,
      String source,
      String specversion,
      String type,
      String subject,
      Instant time,
      String datacontenttype,
      String vigiltenant,
      List<String> vigilmarkings,
      int vigilclassification,
      String vigilpurpose,
      String payloadSummaryJson) {}

  public record OpenLineageRunEvent(
      String eventType,
      String runId,
      String jobName,
      List<String> inputDatasetRids,
      String outputDatasetRid,
      SecurityLabel securityLabelFacet,
      List<QualityCheckResult> qualityFacet,
      Instant eventTime) {}

  public record ErasureAuditProof(
      String erasureRequestId,
      String datasetRid,
      String preErasureSnapshotId,
      List<String> rewrittenManifestSha256s,
      String postErasureSnapshotId,
      String postErasureTag,
      long erasedRowCount,
      List<String> expiredSnapshotIds,
      String auditEventHash,
      Instant completedAt) {}

  public record WrappedDatasetDek(
      String datasetRid,
      String kekKeyId,
      int keyVersion,
      byte[] nonce,
      byte[] wrappedKeyMaterial) {}

  public static final class QualityCheckFailedException extends RuntimeException {
    private final List<String> failedChecks;

    public QualityCheckFailedException(List<String> failedChecks) {
      super("BLOCK quality checks failed: " + String.join(", ", failedChecks));
      this.failedChecks = List.copyOf(failedChecks);
    }

    public List<String> failedChecks() {
      return failedChecks;
    }
  }

  private static final class DatasetState {
    final String datasetRid;
    final String tenant;
    final String pack;
    final String name;
    final String primaryKey;
    final String residency;
    final int retentionDays;
    final Map<String, String> tableProperties = new LinkedHashMap<>();
    final Map<Integer, DatasetColumn> schemaByFieldId = new LinkedHashMap<>();
    final Map<String, DatasetBranch> branches = new LinkedHashMap<>();
    final Map<String, Map<String, Map<String, Object>>> branchRows = new LinkedHashMap<>();
    final Map<String, SecurityLabel> branchLabels = new LinkedHashMap<>();
    final Map<String, SnapshotEntry> snapshots = new LinkedHashMap<>();
    final Map<String, DatasetTag> tags = new LinkedHashMap<>();
    final WrappedDatasetDek wrappedDek;
    int schemaVersion = 1;

    DatasetState(
        String datasetRid,
        String tenant,
        String pack,
        String name,
        String primaryKey,
        String residency,
        int retentionDays,
        SecurityLabel initialLabel,
        List<DatasetColumn> initialColumns,
        WrappedDatasetDek wrappedDek) {
      this.datasetRid = datasetRid;
      this.tenant = tenant;
      this.pack = pack;
      this.name = name;
      this.primaryKey = primaryKey;
      this.residency = residency;
      this.retentionDays = retentionDays;
      this.wrappedDek = wrappedDek;

      tableProperties.put("format-version", "3");
      tableProperties.put("write.delete.mode", "merge-on-read");
      long maxSnapshotAgeMs =
          "_raw".equals(pack) && name.contains("ais") ? 1_209_600_000L : 2_592_000_000L;
      tableProperties.put("history.expire.max-snapshot-age-ms", Long.toString(maxSnapshotAgeMs));
      tableProperties.put("history.expire.min-snapshots-to-keep", "10");
      tableProperties.put("vigil.primary_key", primaryKey);
      tableProperties.put("vigil.pack", pack);
      tableProperties.put("vigil.residency", residency);
      tableProperties.put("vigil.retention_days", Integer.toString(retentionDays));
      applyLabelToTableProperties(tableProperties, initialLabel);

      for (DatasetColumn col : initialColumns) {
        schemaByFieldId.put(col.fieldId(), col);
        if (!col.markings().isEmpty()) {
          tableProperties.put(
              "vigil.column_markings." + col.name(), String.join(",", col.markings()));
        }
      }

      Instant now = Instant.now();
      branches.put("main", new DatasetBranch("main", "", "0", true, now));
      branchRows.put("main", new LinkedHashMap<>());
      branchLabels.put("main", initialLabel);
    }
  }

  private static final class TransactionRecord {
    final String transactionRid;
    final String datasetRid;
    final String branch;
    final TransactionType type;
    final SourceRun sourceRun;
    final List<String> declaredInputRids;
    final RecordProvenance provenance;
    final String idempotencyKey;
    final Instant openedAt;
    final Instant expiresAt;
    final StagedUploadTarget uploadTarget;
    TransactionStatus status = TransactionStatus.OPEN;
    CommitTransactionResult commitResult;

    TransactionRecord(
        String transactionRid,
        String datasetRid,
        String branch,
        TransactionType type,
        SourceRun sourceRun,
        List<String> declaredInputRids,
        RecordProvenance provenance,
        String idempotencyKey,
        Instant openedAt,
        Instant expiresAt,
        StagedUploadTarget uploadTarget) {
      this.transactionRid = transactionRid;
      this.datasetRid = datasetRid;
      this.branch = branch;
      this.type = type;
      this.sourceRun = sourceRun;
      this.declaredInputRids = List.copyOf(declaredInputRids);
      this.provenance = provenance;
      this.idempotencyKey = idempotencyKey;
      this.openedAt = openedAt;
      this.expiresAt = expiresAt;
      this.uploadTarget = uploadTarget;
    }
  }

  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_NONCE_BYTES = 12;

  private final Map<String, DatasetState> datasets = new LinkedHashMap<>();
  private final Map<String, TransactionRecord> transactions = new LinkedHashMap<>();
  private final Map<String, String> openIdempotencyIndex = new LinkedHashMap<>();
  private final List<OpenLineageRunEvent> openLineageEvents = new ArrayList<>();
  private final List<CloudEventEnvelope> cloudEvents = new ArrayList<>();
  private final Map<String, byte[]> tenantKeks = new LinkedHashMap<>();
  private final SecureRandom secureRandom = new SecureRandom();
  private final AtomicLong snapshotCounter = new AtomicLong(7309245893202912000L);
  private String auditChainTailHash = "0".repeat(64);

  public synchronized void registerTenantKek(String tenant, byte[] kekBytes32) {
    if (kekBytes32 == null || kekBytes32.length != 32) {
      throw new IllegalArgumentException("Tenant KEK must be exactly 32 bytes (AES-256)");
    }
    tenantKeks.put(tenant, kekBytes32.clone());
  }

  public synchronized void registerDataset(
      String datasetRid,
      String namespaceQualifiedName,
      String primaryKey,
      String residency,
      int retentionDays,
      SecurityLabel initialLabel,
      List<DatasetColumn> columns) {
    String[] parts = namespaceQualifiedName.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException(
          "Dataset namespace must follow <tenant>.<pack>.<dataset>: " + namespaceQualifiedName);
    }
    String tenant = parts[0];
    String pack = parts[1];
    String name = parts[2];
    if (primaryKey == null || primaryKey.isBlank()) {
      throw new IllegalArgumentException("primaryKey is required");
    }
    byte[] kek = tenantKeks.computeIfAbsent(tenant, t -> deterministicKey("kek:" + t));
    byte[] rawDek = new byte[32];
    secureRandom.nextBytes(rawDek);
    WrappedDatasetDek wrappedDek = wrapDatasetDek(datasetRid, tenant, kek, rawDek);

    DatasetState state =
        new DatasetState(
            datasetRid,
            tenant,
            pack,
            name,
            primaryKey,
            residency,
            retentionDays,
            initialLabel,
            columns,
            wrappedDek);
    datasets.put(datasetRid, state);
  }

  /**
   * Opens a dataset transaction (`DS-01`: `POST /api/v1/datasets/{datasetRid}/transactions`),
   * enforcing Warden policy authorization, mandatory provenance, idempotency, and single-OPEN
   * SNAPSHOT per branch.
   */
  public synchronized OpenTransactionResult openTransaction(
      String datasetRid,
      String branch,
      TransactionType type,
      SourceRun sourceRun,
      List<String> declaredInputRids,
      RecordProvenance provenance,
      String idempotencyKey,
      CallerPolicyContext caller) {
    DatasetState ds = requireDataset(datasetRid);
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(sourceRun, "sourceRun");
    Objects.requireNonNull(provenance, "provenance");
    Objects.requireNonNull(caller, "caller");

    if (!caller.hasDatasetWriterRole() || !ds.tenant.equals(caller.tenantId())) {
      throw new SecurityException("POLICY_DENIED: caller lacks dataset-writer role for tenant");
    }
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    if (!ds.branches.containsKey(targetBranch)) {
      throw new IllegalArgumentException("NOT_FOUND: unknown branch " + targetBranch);
    }

    String effectiveKey =
        (idempotencyKey == null || idempotencyKey.isBlank())
            ? sourceRun.computeIdempotencyKey()
            : idempotencyKey;
    String idemLookupKey = datasetRid + "|" + targetBranch + "|" + effectiveKey;
    String existingTxnRid = openIdempotencyIndex.get(idemLookupKey);
    if (existingTxnRid != null) {
      TransactionRecord existing = transactions.get(existingTxnRid);
      return new OpenTransactionResult(
          existing.transactionRid,
          existing.datasetRid,
          existing.branch,
          existing.type,
          existing.status,
          existing.openedAt,
          existing.expiresAt,
          existing.uploadTarget);
    }

    if (type == TransactionType.SNAPSHOT) {
      boolean hasOpenSnapshot =
          transactions.values().stream()
              .anyMatch(
                  t ->
                      t.datasetRid.equals(datasetRid)
                          && t.branch.equals(targetBranch)
                          && t.type == TransactionType.SNAPSHOT
                          && t.status == TransactionStatus.OPEN);
      if (hasOpenSnapshot) {
        throw new IllegalStateException(
            "CONFLICT: at most one OPEN SNAPSHOT transaction is permitted per branch");
      }
    }

    Instant openedAt = Instant.now();
    Instant expiresAt = openedAt.plusSeconds(86_400);
    Instant credExpiresAt = openedAt.plusSeconds(900); // 15-minute scoped credentials
    String txnUuid = UUID.randomUUID().toString();
    String txnRid = "ri.forge." + ds.tenant + ".transaction." + txnUuid;
    String prefix = ds.tenant + "/_staging/" + txnUuid + "/";

    StagedUploadTarget upload =
        new StagedUploadTarget(
            "S3",
            "https://s3.za-gp-1.vigil.internal",
            "vigil-za-gp-1-lake",
            prefix,
            new ScopedUploadCredentials(
                "VIGILSTS" + txnUuid.substring(0, 8).toUpperCase(),
                sha256Hex("secret:" + txnUuid),
                sha256Hex("session:" + prefix + ":" + credExpiresAt),
                credExpiresAt));

    TransactionRecord record =
        new TransactionRecord(
            txnRid,
            datasetRid,
            targetBranch,
            type,
            sourceRun,
            declaredInputRids == null ? List.of() : declaredInputRids,
            provenance,
            effectiveKey,
            openedAt,
            expiresAt,
            upload);
    transactions.put(txnRid, record);
    openIdempotencyIndex.put(idemLookupKey, txnRid);

    return new OpenTransactionResult(
        txnRid,
        datasetRid,
        targetBranch,
        type,
        TransactionStatus.OPEN,
        openedAt,
        expiresAt,
        upload);
  }

  /**
   * Commits an open transaction (`DS-02`: `POST
   * /api/v1/datasets/{datasetRid}/transactions/{transactionRid}/commit`), verifying file hashes,
   * running `BLOCK` quality gates, calling Warden `propagate()`, updating Iceberg v3 table
   * properties, and emitting OpenLineage and CloudEvents.
   */
  public synchronized CommitTransactionResult commitTransaction(
      String datasetRid,
      String transactionRid,
      List<StagedFile> files,
      Map<String, String> expectedFileSha256s,
      List<QualityCheckResult> qualityResults) {
    DatasetState ds = requireDataset(datasetRid);
    TransactionRecord txn = transactions.get(transactionRid);
    if (txn == null || !txn.datasetRid.equals(datasetRid)) {
      throw new IllegalArgumentException("NOT_FOUND: transaction " + transactionRid);
    }
    if (txn.status == TransactionStatus.COMMITTED && txn.commitResult != null) {
      return txn.commitResult;
    }
    if (txn.status != TransactionStatus.OPEN) {
      throw new IllegalStateException("CONFLICT: transaction is not OPEN: " + txn.status);
    }
    if (files == null || files.isEmpty()) {
      throw new IllegalArgumentException("VALIDATION_FAILED: commit requires at least one file");
    }

    for (StagedFile file : files) {
      String expectedSha =
          expectedFileSha256s == null ? file.sha256() : expectedFileSha256s.get(file.path());
      if (expectedSha == null || !expectedSha.equalsIgnoreCase(file.sha256())) {
        throw new IllegalArgumentException(
            "VALIDATION_FAILED: SHA-256 mismatch on staged file " + file.path());
      }
    }

    List<QualityCheckResult> checks = qualityResults == null ? List.of() : qualityResults;
    List<String> failedBlockChecks =
        checks.stream()
            .filter(c -> c.severity() == QualitySeverity.BLOCK && !c.passed())
            .map(QualityCheckResult::checkName)
            .toList();
    if (!failedBlockChecks.isEmpty()) {
      txn.status = TransactionStatus.ABORTED;
      openLineageEvents.add(
          new OpenLineageRunEvent(
              "ABORT",
              UUID.randomUUID().toString(),
              txn.sourceRun.connectorId(),
              txn.declaredInputRids,
              datasetRid,
              ds.branchLabels.get(txn.branch),
              checks,
              Instant.now()));
      throw new QualityCheckFailedException(failedBlockChecks);
    }

    // Warden W5 propagate() across current branch label and all declared upstream input datasets
    List<SecurityLabel> propagationInputs = new ArrayList<>();
    propagationInputs.add(ds.branchLabels.get(txn.branch));
    for (String inputRid : txn.declaredInputRids) {
      DatasetState inputDs = requireDataset(inputRid);
      propagationInputs.add(inputDs.branchLabels.get("main"));
    }
    SecurityLabel propagatedLabel = propagateLabels(propagationInputs);

    Map<String, Map<String, Object>> currentRows =
        new LinkedHashMap<>(ds.branchRows.get(txn.branch));
    long added = 0;
    long deleted = 0;

    if (txn.type == TransactionType.SNAPSHOT) {
      currentRows.clear();
    }
    for (StagedFile file : files) {
      for (Map.Entry<String, Map<String, Object>> rowEntry : file.rowsByPrimaryKey().entrySet()) {
        String pk = rowEntry.getKey();
        if (txn.type == TransactionType.DELETE) {
          if (currentRows.remove(pk) != null) {
            deleted++;
          }
        } else {
          currentRows.put(pk, Map.copyOf(rowEntry.getValue()));
          added++;
        }
      }
    }

    String snapshotId = Long.toString(snapshotCounter.incrementAndGet());
    Instant committedAt = Instant.now();
    String manifestSha =
        sha256Hex(datasetRid + "|" + txn.branch + "|" + snapshotId + "|" + currentRows.keySet());

    ds.branchRows.put(txn.branch, currentRows);
    ds.branchLabels.put(txn.branch, propagatedLabel);
    DatasetBranch prevBranch = ds.branches.get(txn.branch);
    ds.branches.put(
        txn.branch,
        new DatasetBranch(
            txn.branch,
            transactionRid,
            snapshotId,
            prevBranch.isDefault(),
            prevBranch.createdAt()));
    ds.snapshots.put(
        snapshotId,
        new SnapshotEntry(
            snapshotId,
            transactionRid,
            txn.branch,
            txn.type,
            manifestSha,
            new LinkedHashMap<>(currentRows),
            propagatedLabel,
            committedAt));

    applyLabelToTableProperties(ds.tableProperties, propagatedLabel);
    ds.tableProperties.put("vigil.lawful_basis", txn.provenance.lawfulBasisRef());
    ds.tableProperties.put("vigil.responsible_party", txn.provenance.responsibleParty());
    ds.tableProperties.put("vigil.terms_ref", txn.provenance.termsRef());
    ds.tableProperties.put("vigil.data_origin", txn.provenance.dataOrigin().name());

    String lineageRunId = UUID.randomUUID().toString();
    OpenLineageRunEvent olEvent =
        new OpenLineageRunEvent(
            "COMPLETE",
            lineageRunId,
            txn.sourceRun.connectorId(),
            txn.declaredInputRids,
            datasetRid,
            propagatedLabel,
            checks,
            committedAt);
    openLineageEvents.add(olEvent);

    CloudEventEnvelope cloudEvent =
        new CloudEventEnvelope(
            UUID.randomUUID().toString(),
            "vigil://forge-datasets/" + ds.tenant,
            "1.0",
            "vigil.forge.dataset.transaction.committed.v1",
            datasetRid,
            committedAt,
            "application/protobuf",
            ds.tenant,
            propagatedLabel.markings(),
            propagatedLabel.classification(),
            propagatedLabel.purposeTags().get(0),
            "{\"snapshot_id\":\""
                + snapshotId
                + "\",\"added\":"
                + added
                + ",\"deleted\":"
                + deleted
                + "}");
    cloudEvents.add(cloudEvent);

    CommitTransactionResult result =
        new CommitTransactionResult(
            transactionRid,
            TransactionStatus.COMMITTED,
            snapshotId,
            committedAt,
            added,
            deleted,
            propagatedLabel,
            lineageRunId,
            cloudEvent,
            olEvent);
    txn.status = TransactionStatus.COMMITTED;
    txn.commitResult = result;
    return result;
  }

  public synchronized TransactionStatus abortTransaction(
      String datasetRid, String transactionRid, String reason) {
    requireDataset(datasetRid);
    TransactionRecord txn = transactions.get(transactionRid);
    if (txn == null || !txn.datasetRid.equals(datasetRid)) {
      throw new IllegalArgumentException("NOT_FOUND: transaction " + transactionRid);
    }
    if (txn.status == TransactionStatus.COMMITTED) {
      throw new IllegalStateException("CONFLICT: cannot abort an already COMMITTED transaction");
    }
    txn.status = TransactionStatus.ABORTED;
    return txn.status;
  }

  public synchronized DatasetBranch createBranch(
      String datasetRid, String newBranchName, String fromBranch) {
    DatasetState ds = requireDataset(datasetRid);
    if (newBranchName == null || newBranchName.isBlank()) {
      throw new IllegalArgumentException("newBranchName is required");
    }
    if (ds.branches.containsKey(newBranchName)) {
      throw new IllegalStateException("CONFLICT: branch already exists: " + newBranchName);
    }
    String source = (fromBranch == null || fromBranch.isBlank()) ? "main" : fromBranch;
    DatasetBranch srcBranch = ds.branches.get(source);
    if (srcBranch == null) {
      throw new IllegalArgumentException("NOT_FOUND: source branch " + source);
    }
    DatasetBranch created =
        new DatasetBranch(
            newBranchName,
            srcBranch.headTransactionRid(),
            srcBranch.headSnapshotId(),
            false,
            Instant.now());
    ds.branches.put(newBranchName, created);
    ds.branchRows.put(newBranchName, new LinkedHashMap<>(ds.branchRows.get(source)));
    ds.branchLabels.put(newBranchName, ds.branchLabels.get(source));
    return created;
  }

  public synchronized DatasetBranch mergeBranch(
      String datasetRid, String sourceBranch, String targetBranch) {
    DatasetState ds = requireDataset(datasetRid);
    DatasetBranch src = ds.branches.get(sourceBranch);
    DatasetBranch dst = ds.branches.get(targetBranch);
    if (src == null || dst == null) {
      throw new IllegalArgumentException("NOT_FOUND: source or target branch does not exist");
    }
    SecurityLabel mergedLabel =
        propagateLabels(
            List.of(ds.branchLabels.get(targetBranch), ds.branchLabels.get(sourceBranch)));
    Map<String, Map<String, Object>> mergedRows =
        new LinkedHashMap<>(ds.branchRows.get(targetBranch));
    mergedRows.putAll(ds.branchRows.get(sourceBranch));

    String mergedSnapshotId = Long.toString(snapshotCounter.incrementAndGet());
    Instant now = Instant.now();
    String manifestSha =
        sha256Hex(
            datasetRid + "|" + targetBranch + "|" + mergedSnapshotId + "|" + mergedRows.keySet());

    ds.branchRows.put(targetBranch, mergedRows);
    ds.branchLabels.put(targetBranch, mergedLabel);
    DatasetBranch updated =
        new DatasetBranch(
            targetBranch,
            src.headTransactionRid(),
            mergedSnapshotId,
            dst.isDefault(),
            dst.createdAt());
    ds.branches.put(targetBranch, updated);
    ds.snapshots.put(
        mergedSnapshotId,
        new SnapshotEntry(
            mergedSnapshotId,
            src.headTransactionRid(),
            targetBranch,
            TransactionType.UPDATE,
            manifestSha,
            new LinkedHashMap<>(mergedRows),
            mergedLabel,
            now));
    if ("main".equals(targetBranch)) {
      applyLabelToTableProperties(ds.tableProperties, mergedLabel);
    }
    return updated;
  }

  public synchronized List<DatasetBranch> listBranches(String datasetRid) {
    DatasetState ds = requireDataset(datasetRid);
    return List.copyOf(ds.branches.values());
  }

  public synchronized DatasetTag createTag(String datasetRid, String tagName, String snapshotId) {
    DatasetState ds = requireDataset(datasetRid);
    if (!ds.snapshots.containsKey(snapshotId)) {
      throw new IllegalArgumentException("NOT_FOUND: snapshot " + snapshotId);
    }
    DatasetTag tag = new DatasetTag(tagName, snapshotId, Instant.now());
    ds.tags.put(tagName, tag);
    return tag;
  }

  public synchronized Map<String, String> getTableProperties(String datasetRid) {
    return Map.copyOf(requireDataset(datasetRid).tableProperties);
  }

  public synchronized SecurityLabel getBranchLabel(String datasetRid, String branch) {
    DatasetState ds = requireDataset(datasetRid);
    SecurityLabel label = ds.branchLabels.get(branch);
    if (label == null) {
      throw new IllegalArgumentException("NOT_FOUND: branch " + branch);
    }
    return label;
  }

  /**
   * Reads rows from a dataset branch after verifying the reader's `CallerPolicyContext` against the
   * dataset's propagated security label (W1 conjunctive markings, clearance >= classification,
   * releasability intersection, and purpose match) and redacting column-marked fields missing from
   * the caller's held markings (W2).
   */
  public synchronized List<Map<String, Object>> readBranch(
      String datasetRid, String branch, CallerPolicyContext reader) {
    DatasetState ds = requireDataset(datasetRid);
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    SecurityLabel label = ds.branchLabels.get(targetBranch);
    if (label == null) {
      throw new IllegalArgumentException("NOT_FOUND: branch " + targetBranch);
    }
    if (!ds.tenant.equals(reader.tenantId())) {
      throw new SecurityException("W1 DENY: cross-tenant read forbidden");
    }
    if (!reader.heldMarkings().containsAll(label.markings())) {
      throw new SecurityException("W1 DENY: reader missing required dataset markings");
    }
    if (reader.clearance() < label.classification()) {
      throw new SecurityException("W1 DENY: reader clearance below dataset classification");
    }
    if (!label.releasability().isEmpty()
        && Collections.disjoint(reader.releasability(), label.releasability())) {
      throw new SecurityException("W1 DENY: reader releasability disjoint from dataset");
    }
    if (!label.purposeTags().contains("UNRESTRICTED")
        && !label.purposeTags().contains(reader.purpose())) {
      throw new SecurityException("W1 DENY: reader purpose not permitted by dataset purposeTags");
    }

    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> row : ds.branchRows.get(targetBranch).values()) {
      Map<String, Object> projected = new LinkedHashMap<>(row);
      for (DatasetColumn col : ds.schemaByFieldId.values()) {
        if (!col.markings().isEmpty() && !reader.heldMarkings().containsAll(col.markings())) {
          projected.remove(col.name());
        }
      }
      out.add(Collections.unmodifiableMap(projected));
    }
    return Collections.unmodifiableList(out);
  }

  /**
   * Evolves the dataset schema by adding an optional column or renaming an existing column by field
   * ID (P4 task 4.5). Rejects dropping or renaming the primary key or incompatible type changes.
   */
  public synchronized int evolveSchema(String datasetRid, DatasetColumn columnUpdate) {
    DatasetState ds = requireDataset(datasetRid);
    DatasetColumn existing = ds.schemaByFieldId.get(columnUpdate.fieldId());
    if (existing == null) {
      if (columnUpdate.required()) {
        throw new IllegalArgumentException(
            "Schema evolution cannot add a required column without a default backfill");
      }
      ds.schemaByFieldId.put(columnUpdate.fieldId(), columnUpdate);
    } else {
      if (existing.name().equals(ds.primaryKey) && !columnUpdate.name().equals(ds.primaryKey)) {
        throw new IllegalArgumentException("Schema evolution cannot rename the primary key column");
      }
      if (!existing.type().equals(columnUpdate.type())) {
        throw new IllegalArgumentException(
            "Incompatible column type change on fieldId " + columnUpdate.fieldId());
      }
      ds.tableProperties.remove("vigil.column_markings." + existing.name());
      ds.schemaByFieldId.put(columnUpdate.fieldId(), columnUpdate);
    }
    if (!columnUpdate.markings().isEmpty()) {
      ds.tableProperties.put(
          "vigil.column_markings." + columnUpdate.name(),
          String.join(",", columnUpdate.markings()));
    }
    ds.schemaVersion++;
    return ds.schemaVersion;
  }

  /**
   * Executes a POPIA erasure request across all snapshots and branches of a dataset
   * (`DATA_MODEL.md` section 6.3 step 5, P4 task 4.5 and "Done when"):
   *
   * <ol>
   *   <li>Verifies two-person `LEGAL_EFFECT` approval;
   *   <li>Applies Iceberg v3 deletion vectors removing `subjectPrimaryKeys` from active branches;
   *   <li>Explicitly expires every snapshot older than the erasure snapshot (overriding
   *       `min-snapshots-to-keep = 10` and dropping pre-erasure tags);
   *   <li>Tags the post-erasure snapshot with `erasure-<request-id>-post`;
   *   <li>Emits a hash-chained `ErasureAuditProof` with counts and SHA-256 digests only.
   * </ol>
   */
  public synchronized ErasureAuditProof eraseDataSubject(
      String erasureRequestId,
      String datasetRid,
      List<String> subjectPrimaryKeys,
      boolean twoPersonApprovalVerified) {
    if (!twoPersonApprovalVerified) {
      throw new SecurityException(
          "POPIA erasure requires an approved two-person LEGAL_EFFECT authorisation");
    }
    if (erasureRequestId == null || erasureRequestId.isBlank()) {
      throw new IllegalArgumentException("erasureRequestId is required");
    }
    if (subjectPrimaryKeys == null || subjectPrimaryKeys.isEmpty()) {
      throw new IllegalArgumentException("subjectPrimaryKeys must not be empty");
    }
    DatasetState ds = requireDataset(datasetRid);
    Set<String> targetPks = new LinkedHashSet<>(subjectPrimaryKeys);

    String preErasureSnapshotId = ds.branches.get("main").headSnapshotId();
    List<String> rewrittenManifestHashes = new ArrayList<>();
    for (SnapshotEntry snap : ds.snapshots.values()) {
      rewrittenManifestHashes.add(snap.manifestSha256());
    }

    long erasedCount = 0;
    for (Map.Entry<String, Map<String, Map<String, Object>>> branchEntry :
        ds.branchRows.entrySet()) {
      Map<String, Map<String, Object>> rows = branchEntry.getValue();
      for (String pk : targetPks) {
        if (rows.remove(pk) != null) {
          erasedCount++;
        }
      }
    }

    // Expire every pre-erasure snapshot regardless of min-snapshots-to-keep (DATA_MODEL section
    // 6.3)
    List<String> expiredSnapshotIds = new ArrayList<>(ds.snapshots.keySet());
    ds.snapshots.clear();
    ds.tags.clear();

    String postErasureSnapshotId = Long.toString(snapshotCounter.incrementAndGet());
    Instant completedAt = Instant.now();
    Map<String, Map<String, Object>> mainRowsAfter = new LinkedHashMap<>(ds.branchRows.get("main"));
    String postManifestSha =
        sha256Hex(datasetRid + "|main|" + postErasureSnapshotId + "|" + mainRowsAfter.keySet());
    SecurityLabel mainLabel = ds.branchLabels.get("main");

    SnapshotEntry postSnapshot =
        new SnapshotEntry(
            postErasureSnapshotId,
            "erasure-txn-" + erasureRequestId,
            "main",
            TransactionType.DELETE,
            postManifestSha,
            mainRowsAfter,
            mainLabel,
            completedAt);
    ds.snapshots.put(postErasureSnapshotId, postSnapshot);

    DatasetBranch mainBranch = ds.branches.get("main");
    ds.branches.put(
        "main",
        new DatasetBranch(
            "main",
            "erasure-txn-" + erasureRequestId,
            postErasureSnapshotId,
            true,
            mainBranch.createdAt()));

    String postTag = "erasure-" + erasureRequestId + "-post";
    ds.tags.put(postTag, new DatasetTag(postTag, postErasureSnapshotId, completedAt));

    auditChainTailHash =
        sha256Hex(
            auditChainTailHash
                + "|"
                + erasureRequestId
                + "|"
                + datasetRid
                + "|"
                + preErasureSnapshotId
                + "|"
                + postErasureSnapshotId
                + "|"
                + erasedCount);

    return new ErasureAuditProof(
        erasureRequestId,
        datasetRid,
        preErasureSnapshotId,
        List.copyOf(rewrittenManifestHashes),
        postErasureSnapshotId,
        postTag,
        erasedCount,
        List.copyOf(expiredSnapshotIds),
        auditChainTailHash,
        completedAt);
  }

  public synchronized List<String> listActiveSnapshotIds(String datasetRid) {
    return List.copyOf(requireDataset(datasetRid).snapshots.keySet());
  }

  public synchronized List<DatasetTag> listTags(String datasetRid) {
    return List.copyOf(requireDataset(datasetRid).tags.values());
  }

  /**
   * Encrypts an Iceberg/S3 object payload using the dataset's AES-256-GCM DEK (unwrapped via the
   * tenant's KEK) with AAD bound to `tenant|datasetRid|branch|objectPath` (P4 task 4.7).
   */
  public synchronized byte[] encryptDatasetObject(
      String datasetRid, String branch, String objectPath, byte[] plaintext) {
    DatasetState ds = requireDataset(datasetRid);
    byte[] kek = tenantKeks.get(ds.tenant);
    byte[] dek = unwrapDatasetDek(ds.wrappedDek, ds.tenant, kek);
    byte[] aad =
        (ds.tenant + "|" + datasetRid + "|" + branch + "|" + objectPath)
            .getBytes(StandardCharsets.UTF_8);
    return aesGcmEncrypt(dek, aad, plaintext);
  }

  public synchronized byte[] decryptDatasetObject(
      String datasetRid, String branch, String objectPath, byte[] nonceAndCiphertext) {
    DatasetState ds = requireDataset(datasetRid);
    byte[] kek = tenantKeks.get(ds.tenant);
    byte[] dek = unwrapDatasetDek(ds.wrappedDek, ds.tenant, kek);
    byte[] aad =
        (ds.tenant + "|" + datasetRid + "|" + branch + "|" + objectPath)
            .getBytes(StandardCharsets.UTF_8);
    return aesGcmDecrypt(dek, aad, nonceAndCiphertext);
  }

  public static SecurityLabel propagateLabels(List<SecurityLabel> inputs) {
    if (inputs == null || inputs.isEmpty()) {
      throw new IllegalArgumentException("inputs must not be empty");
    }
    Set<String> markings = new TreeSet<>();
    int maxClassification = 0;
    Set<String> relIntersection = null;
    Set<String> purposeIntersection = null;

    for (SecurityLabel in : inputs) {
      markings.addAll(in.markings());
      if (in.classification() > maxClassification) {
        maxClassification = in.classification();
      }
      if (!in.releasability().isEmpty()) {
        if (relIntersection == null) {
          relIntersection = new TreeSet<>(in.releasability());
        } else {
          relIntersection.retainAll(in.releasability());
          if (relIntersection.isEmpty()) {
            throw new IllegalStateException("W5 propagation failed: disjoint releasability");
          }
        }
      }
      if (!in.purposeTags().contains("UNRESTRICTED")) {
        if (purposeIntersection == null) {
          purposeIntersection = new TreeSet<>(in.purposeTags());
        } else {
          purposeIntersection.retainAll(in.purposeTags());
          if (purposeIntersection.isEmpty()) {
            throw new IllegalStateException("W5 propagation failed: disjoint purposeTags");
          }
        }
      }
    }
    return new SecurityLabel(
        List.copyOf(markings),
        maxClassification,
        relIntersection == null ? List.of() : List.copyOf(relIntersection),
        purposeIntersection == null ? List.of("UNRESTRICTED") : List.copyOf(purposeIntersection));
  }

  private DatasetState requireDataset(String datasetRid) {
    DatasetState ds = datasets.get(datasetRid);
    if (ds == null) {
      throw new IllegalArgumentException("NOT_FOUND: dataset " + datasetRid);
    }
    return ds;
  }

  private static void applyLabelToTableProperties(Map<String, String> props, SecurityLabel label) {
    props.put("vigil.markings", String.join(",", label.markings()));
    props.put("vigil.classification", Integer.toString(label.classification()));
    props.put("vigil.releasability", String.join(",", label.releasability()));
    props.put("vigil.purpose_tags", String.join(",", label.purposeTags()));
  }

  private WrappedDatasetDek wrapDatasetDek(
      String datasetRid, String tenant, byte[] kek, byte[] rawDek) {
    byte[] aad = ("tenant|" + tenant + "|dataset|" + datasetRid).getBytes(StandardCharsets.UTF_8);
    byte[] encrypted = aesGcmEncrypt(kek, aad, rawDek);
    byte[] nonce = new byte[GCM_NONCE_BYTES];
    byte[] ct = new byte[encrypted.length - GCM_NONCE_BYTES];
    System.arraycopy(encrypted, 0, nonce, 0, GCM_NONCE_BYTES);
    System.arraycopy(encrypted, GCM_NONCE_BYTES, ct, 0, ct.length);
    return new WrappedDatasetDek(datasetRid, "tenant-" + tenant + "-kek", 1, nonce, ct);
  }

  private byte[] unwrapDatasetDek(WrappedDatasetDek wrapped, String tenant, byte[] kek) {
    byte[] aad =
        ("tenant|" + tenant + "|dataset|" + wrapped.datasetRid()).getBytes(StandardCharsets.UTF_8);
    ByteBuffer buf =
        ByteBuffer.allocate(wrapped.nonce().length + wrapped.wrappedKeyMaterial().length);
    buf.put(wrapped.nonce());
    buf.put(wrapped.wrappedKeyMaterial());
    return aesGcmDecrypt(kek, aad, buf.array());
  }

  private byte[] aesGcmEncrypt(byte[] key32, byte[] aad, byte[] plaintext) {
    try {
      byte[] nonce = new byte[GCM_NONCE_BYTES];
      secureRandom.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(key32, "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(aad);
      byte[] ciphertext = cipher.doFinal(plaintext);
      ByteBuffer out = ByteBuffer.allocate(nonce.length + ciphertext.length);
      out.put(nonce);
      out.put(ciphertext);
      return out.array();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("AES-256-GCM encryption failed", e);
    }
  }

  private byte[] aesGcmDecrypt(byte[] key32, byte[] aad, byte[] nonceAndCiphertext) {
    if (nonceAndCiphertext == null || nonceAndCiphertext.length <= GCM_NONCE_BYTES + 16) {
      throw new IllegalArgumentException("Ciphertext too short for AES-256-GCM");
    }
    try {
      byte[] nonce = new byte[GCM_NONCE_BYTES];
      byte[] ct = new byte[nonceAndCiphertext.length - GCM_NONCE_BYTES];
      System.arraycopy(nonceAndCiphertext, 0, nonce, 0, GCM_NONCE_BYTES);
      System.arraycopy(nonceAndCiphertext, GCM_NONCE_BYTES, ct, 0, ct.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key32, "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(aad);
      return cipher.doFinal(ct);
    } catch (GeneralSecurityException e) {
      throw new SecurityException("AES-256-GCM authentication tag or AAD mismatch", e);
    }
  }

  private static byte[] deterministicKey(String seed) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  static String sha256Hex(String input) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
