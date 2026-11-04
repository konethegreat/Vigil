// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Unit and acceptance tests for Forge
// Datasets Iceberg v3 transactions, branches, Warden W5 marking propagation, AES-256-GCM
// encryption, and POPIA erasure across snapshots (P4 tasks 4.1-4.5, 4.7)
package africa.vigil.forge.datasets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import africa.vigil.forge.datasets.DatasetEngine.CallerPolicyContext;
import africa.vigil.forge.datasets.DatasetEngine.CommitTransactionResult;
import africa.vigil.forge.datasets.DatasetEngine.DataOrigin;
import africa.vigil.forge.datasets.DatasetEngine.DatasetBranch;
import africa.vigil.forge.datasets.DatasetEngine.DatasetColumn;
import africa.vigil.forge.datasets.DatasetEngine.DatasetTag;
import africa.vigil.forge.datasets.DatasetEngine.ErasureAuditProof;
import africa.vigil.forge.datasets.DatasetEngine.OpenTransactionResult;
import africa.vigil.forge.datasets.DatasetEngine.QualityCheckFailedException;
import africa.vigil.forge.datasets.DatasetEngine.QualityCheckResult;
import africa.vigil.forge.datasets.DatasetEngine.QualitySeverity;
import africa.vigil.forge.datasets.DatasetEngine.RecordProvenance;
import africa.vigil.forge.datasets.DatasetEngine.SecurityLabel;
import africa.vigil.forge.datasets.DatasetEngine.SourceRun;
import africa.vigil.forge.datasets.DatasetEngine.StagedFile;
import africa.vigil.forge.datasets.DatasetEngine.TransactionStatus;
import africa.vigil.forge.datasets.DatasetEngine.TransactionType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatasetEngineTest {

  @Test
  void writesBranchesMergesReadsWithPropagatedMarkingsAndErasesAcrossSnapshots() {
    DatasetEngine engine = new DatasetEngine();
    engine.registerTenantKek(
        "demo", "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    SecurityLabel upstreamPiiLabel =
        new SecurityLabel(
            List.of("PII"),
            2,
            List.of("REL-ZA"),
            List.of("PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"));
    SecurityLabel basePublicLabel =
        new SecurityLabel(
            List.of(),
            0,
            List.of("REL-ZA", "REL-SADC"),
            List.of("PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"));

    String rawRestrictRid = "ri.forge.demo.dataset.treasury-restricted-raw";
    engine.registerDataset(
        rawRestrictRid,
        "demo._raw.treasury_restricted",
        "restriction_id",
        "ZA",
        365,
        upstreamPiiLabel,
        List.of(
            new DatasetColumn(1, "restriction_id", "STRING", true, List.of()),
            new DatasetColumn(2, "identifier_psn", "STRING", false, List.of("PII"))));

    String derivedRid = "ri.forge.demo.dataset.restrictions";
    engine.registerDataset(
        derivedRid,
        "demo.integrity.restrictions",
        "restriction_id",
        "ZA",
        365,
        basePublicLabel,
        List.of(
            new DatasetColumn(1, "restriction_id", "STRING", true, List.of()),
            new DatasetColumn(2, "party_name_raw", "STRING", false, List.of()),
            new DatasetColumn(3, "identifier_psn", "STRING", false, List.of("PII"))));

    CallerPolicyContext writer =
        new CallerPolicyContext(
            "demo",
            "svc:connector-treasury",
            Set.of("PII"),
            2,
            Set.of("REL-ZA"),
            "DATA_INTEGRATION",
            true);

    SourceRun run1 =
        new SourceRun(
            "treasury-restricted",
            "1.0.0",
            "01926e62-0001-7000-8000-000000000001",
            "2026-11-01T00:00:00Z",
            "2026-11-02T00:00:00Z");
    RecordProvenance prov =
        new RecordProvenance(
            "terms/treasury-restricted@1", "lb/treasury-restricted@1", "NT_OCPO", DataOrigin.REAL);

    // 1. Open and commit initial SNAPSHOT on main, declaring rawRestrictRid as upstream input
    OpenTransactionResult open1 =
        engine.openTransaction(
            derivedRid,
            "main",
            TransactionType.SNAPSHOT,
            run1,
            List.of(rawRestrictRid),
            prov,
            null,
            writer);
    assertEquals(TransactionStatus.OPEN, open1.status());
    assertTrue(open1.upload().prefix().startsWith("demo/_staging/"));

    // Idempotent replay of openTransaction returns identical transaction RID
    OpenTransactionResult open1Replay =
        engine.openTransaction(
            derivedRid,
            "main",
            TransactionType.SNAPSHOT,
            run1,
            List.of(rawRestrictRid),
            prov,
            null,
            writer);
    assertEquals(open1.transactionRid(), open1Replay.transactionRid());

    String file1Sha = DatasetEngine.sha256Hex("part-000.parquet");
    StagedFile file1 =
        new StagedFile(
            "part-000.parquet",
            file1Sha,
            2L,
            Map.of(
                "rest-101",
                Map.of(
                    "restriction_id", "rest-101",
                    "party_name_raw", "Ubuntu Civils Pty Ltd",
                    "identifier_psn", "psn_1111"),
                "rest-202",
                Map.of(
                    "restriction_id", "rest-202",
                    "party_name_raw", "Subject To Erase",
                    "identifier_psn", "psn_erase_me")));

    CommitTransactionResult commit1 =
        engine.commitTransaction(
            derivedRid,
            open1.transactionRid(),
            List.of(file1),
            Map.of("part-000.parquet", file1Sha),
            List.of(new QualityCheckResult("pk_not_null", QualitySeverity.BLOCK, true)));

    assertEquals(TransactionStatus.COMMITTED, commit1.status());
    assertEquals(2L, commit1.rowsAdded());
    // Inherited markings from rawRestrictRid via Warden W5 propagate()
    assertEquals(List.of("PII"), commit1.label().markings());
    assertEquals(2, commit1.label().classification());
    assertEquals(List.of("REL-ZA"), commit1.label().releasability());
    assertEquals(
        "vigil.forge.dataset.transaction.committed.v1", commit1.committedCloudEvent().type());
    assertEquals("COMPLETE", commit1.openLineageEvent().eventType());
    assertEquals("PII", engine.getTableProperties(derivedRid).get("vigil.markings"));
    assertEquals("2", engine.getTableProperties(derivedRid).get("vigil.classification"));

    // Pre-erasure tag that should be removed if its snapshot is expired during POPIA erasure
    DatasetTag preTag = engine.createTag(derivedRid, "release-20261104-1", commit1.snapshotId());
    assertEquals(commit1.snapshotId(), preTag.snapshotId());

    // 2. Create branch wap-run-02, append a third row, and merge back into main
    DatasetBranch wapBranch = engine.createBranch(derivedRid, "wap-run-02", "main");
    assertEquals("wap-run-02", wapBranch.name());
    assertFalse(wapBranch.isDefault());
    assertEquals(2, engine.listBranches(derivedRid).size());

    SourceRun run2 =
        new SourceRun(
            "treasury-restricted",
            "1.0.0",
            "01926e62-0002-7000-8000-000000000002",
            "2026-11-02T00:00:00Z",
            "2026-11-03T00:00:00Z");
    OpenTransactionResult open2 =
        engine.openTransaction(
            derivedRid,
            "wap-run-02",
            TransactionType.APPEND,
            run2,
            List.of(rawRestrictRid),
            prov,
            null,
            writer);
    String file2Sha = DatasetEngine.sha256Hex("part-001.parquet");
    StagedFile file2 =
        new StagedFile(
            "part-001.parquet",
            file2Sha,
            1L,
            Map.of(
                "rest-303",
                Map.of(
                    "restriction_id", "rest-303",
                    "party_name_raw", "Karoo Logistics Ltd",
                    "identifier_psn", "psn_3333")));
    engine.commitTransaction(
        derivedRid,
        open2.transactionRid(),
        List.of(file2),
        Map.of("part-001.parquet", file2Sha),
        List.of());

    DatasetBranch mergedMain = engine.mergeBranch(derivedRid, "wap-run-02", "main");
    assertTrue(mergedMain.isDefault());
    assertEquals(List.of("PII"), engine.getBranchLabel(derivedRid, "main").markings());

    // 3. Read dataset on main with inherited markings enforced
    CallerPolicyContext clearedReader =
        new CallerPolicyContext(
            "demo",
            "user:analyst-1",
            Set.of("PII"),
            2,
            Set.of("REL-ZA"),
            "PROCUREMENT_INTEGRITY",
            false);
    List<Map<String, Object>> rowsBeforeErasure =
        engine.readBranch(derivedRid, "main", clearedReader);
    assertEquals(3, rowsBeforeErasure.size());

    // Reader missing PII marking is denied by W1
    CallerPolicyContext unclearedReader =
        new CallerPolicyContext(
            "demo", "user:public-1", Set.of(), 2, Set.of("REL-ZA"), "PROCUREMENT_INTEGRITY", false);
    assertThrows(
        SecurityException.class, () -> engine.readBranch(derivedRid, "main", unclearedReader));

    // 4. POPIA erasure across snapshots removes rest-202, expires pre-erasure snapshots, and leaves
    // only erasure-<request-id>-post tag with a hash-chained proof
    ErasureAuditProof proof =
        engine.eraseDataSubject("dsar-2026-009", derivedRid, List.of("rest-202"), true);
    assertNotNull(proof);
    assertEquals("dsar-2026-009", proof.erasureRequestId());
    assertEquals(2L, proof.erasedRowCount()); // removed from both main and wap-run-02
    assertEquals("erasure-dsar-2026-009-post", proof.postErasureTag());
    assertEquals(64, proof.auditEventHash().length());
    assertTrue(proof.expiredSnapshotIds().size() >= 2);
    assertEquals(List.of(proof.postErasureSnapshotId()), engine.listActiveSnapshotIds(derivedRid));
    assertEquals(1, engine.listTags(derivedRid).size());
    assertEquals("erasure-dsar-2026-009-post", engine.listTags(derivedRid).get(0).name());

    List<Map<String, Object>> rowsAfterErasure =
        engine.readBranch(derivedRid, "main", clearedReader);
    assertEquals(2, rowsAfterErasure.size());
    assertTrue(
        rowsAfterErasure.stream().noneMatch(r -> "rest-202".equals(r.get("restriction_id"))));
  }

  @Test
  void enforcesQualityGatesSchemaEvolutionAndDatasetEncryption() {
    DatasetEngine engine = new DatasetEngine();
    SecurityLabel label =
        new SecurityLabel(List.of(), 1, List.of("REL-ZA"), List.of("CORRIDOR_OPERATIONS"));
    String aisRid = "ri.forge.demo.dataset.ais-raw";
    engine.registerDataset(
        aisRid,
        "demo._raw.ais_positions",
        "mmsi",
        "ZA",
        14,
        label,
        List.of(new DatasetColumn(1, "mmsi", "STRING", true, List.of())));

    // _raw AIS table gets 14-day snapshot retention override (1209600000 ms)
    assertEquals(
        "1209600000", engine.getTableProperties(aisRid).get("history.expire.max-snapshot-age-ms"));

    // Dataset AES-256-GCM encryption & AAD binding
    byte[] rawParquet = "PAR1-synthetic-ais-payload-PAR1".getBytes(StandardCharsets.UTF_8);
    byte[] encrypted =
        engine.encryptDatasetObject(aisRid, "main", "demo/ais/part-0.parquet", rawParquet);
    byte[] decrypted =
        engine.decryptDatasetObject(aisRid, "main", "demo/ais/part-0.parquet", encrypted);
    assertArrayEquals(rawParquet, decrypted);
    assertThrows(
        SecurityException.class,
        () ->
            engine.decryptDatasetObject(
                aisRid, "wrong-branch", "demo/ais/part-0.parquet", encrypted));

    // Schema evolution: add optional column with markings, then rename it
    int v2 =
        engine.evolveSchema(
            aisRid, new DatasetColumn(2, "vessel_owner", "STRING", false, List.of("FIN")));
    assertEquals(2, v2);
    assertEquals(
        "FIN", engine.getTableProperties(aisRid).get("vigil.column_markings.vessel_owner"));
    int v3 =
        engine.evolveSchema(
            aisRid, new DatasetColumn(2, "registered_owner", "STRING", false, List.of("FIN")));
    assertEquals(3, v3);
    assertEquals(
        "FIN", engine.getTableProperties(aisRid).get("vigil.column_markings.registered_owner"));

    // Reject renaming primary key or adding required column without backfill
    assertThrows(
        IllegalArgumentException.class,
        () ->
            engine.evolveSchema(aisRid, new DatasetColumn(1, "new_pk", "STRING", true, List.of())));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            engine.evolveSchema(aisRid, new DatasetColumn(99, "req_col", "INT", true, List.of())));

    // BLOCK quality check aborts transaction
    CallerPolicyContext writer =
        new CallerPolicyContext(
            "demo", "svc:ais", Set.of(), 1, Set.of("REL-ZA"), "DATA_INTEGRATION", true);
    SourceRun run =
        new SourceRun("ais", "1.0", "run-1", "2026-11-04T00:00:00Z", "2026-11-04T00:05:00Z");
    RecordProvenance prov =
        new RecordProvenance("terms/ais@1", "lb/ais@1", "TNPA", DataOrigin.SYNTHETIC);
    OpenTransactionResult open =
        engine.openTransaction(
            aisRid, "main", TransactionType.SNAPSHOT, run, List.of(), prov, "idem-block-1", writer);

    // Second OPEN SNAPSHOT on the same branch is rejected with CONFLICT
    assertThrows(
        IllegalStateException.class,
        () ->
            engine.openTransaction(
                aisRid,
                "main",
                TransactionType.SNAPSHOT,
                run,
                List.of(),
                prov,
                "idem-block-2",
                writer));

    String sha = DatasetEngine.sha256Hex("bad.parquet");
    StagedFile staged = new StagedFile("bad.parquet", sha, 1L, Map.of("m1", Map.of("mmsi", "m1")));
    assertThrows(
        QualityCheckFailedException.class,
        () ->
            engine.commitTransaction(
                aisRid,
                open.transactionRid(),
                List.of(staged),
                Map.of("bad.parquet", sha),
                List.of(new QualityCheckResult("volume_band", QualitySeverity.BLOCK, false))));

    // Abort on an open transaction succeeds
    OpenTransactionResult openToAbort =
        engine.openTransaction(
            aisRid, "main", TransactionType.APPEND, run, List.of(), prov, "idem-abort-1", writer);
    assertEquals(
        TransactionStatus.ABORTED,
        engine.abortTransaction(aisRid, openToAbort.transactionRid(), "upstream timeout"));

    // Unapproved erasure is rejected
    assertThrows(
        SecurityException.class,
        () -> engine.eraseDataSubject("dsar-unapproved", aisRid, List.of("m1"), false));
  }
}
