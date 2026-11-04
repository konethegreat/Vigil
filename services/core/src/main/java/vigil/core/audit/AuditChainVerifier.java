// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - W12 hash-chained and HMAC-signed
// audit ledger for services/core (P3 task 3.7)
package vigil.core.audit;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Append-only, SHA-256 hash-chained and HMAC-SHA-256 signed W12 audit ledger for the Core service
 * ({@code vigil.core.audit}, P3 task 3.7).
 */
public final class AuditChainVerifier {

  /** Genesis {@code prev_hash} for sequence 1 in every tenant ledger. */
  public static final String GENESIS_PREV_HASH = "0".repeat(64);

  /** Immutable hash-chain proof attached to an {@link AuditRecord}. */
  public record AuditChainProof(
      long seq, String prevHash, String hash, String signature, String signingKeyId) {}

  /** Immutable W12 audit event record. */
  public record AuditRecord(
      String eventId,
      String tenant,
      String action,
      String principal,
      String purpose,
      String resourceRid,
      String decision,
      AuditChainProof chain) {}

  private final String tenant;
  private final String signingKeyId;
  private final byte[] signingKey;
  private final List<AuditRecord> records = new ArrayList<>();
  private String headHash = GENESIS_PREV_HASH;
  private long headSeq = 0L;

  public AuditChainVerifier(String tenant, String signingKeyId, byte[] signingKey) {
    if (tenant == null || tenant.isBlank() || signingKeyId == null || signingKeyId.isBlank()) {
      throw new IllegalArgumentException("tenant and signingKeyId must not be blank");
    }
    if (signingKey == null || signingKey.length < 16) {
      throw new IllegalArgumentException("signingKey must be at least 16 bytes");
    }
    this.tenant = tenant;
    this.signingKeyId = signingKeyId;
    this.signingKey = signingKey.clone();
  }

  /** Appends a decision audit event and advances the tenant hash chain. */
  public synchronized AuditRecord appendDecision(
      String eventId,
      String action,
      String principal,
      String purpose,
      String resourceRid,
      boolean allow) {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(principal, "principal");
    Objects.requireNonNull(purpose, "purpose");
    Objects.requireNonNull(resourceRid, "resourceRid");

    long seq = this.headSeq + 1L;
    String prevHash = this.headHash;
    String decisionStr = allow ? "ALLOW" : "DENY";
    String canonical =
        canonicalPayload(
            this.tenant,
            seq,
            prevHash,
            eventId,
            action,
            principal,
            purpose,
            resourceRid,
            decisionStr);
    String hash = sha256Hex(canonical);
    String signature = hmacSha256Hex(this.signingKey, hash + "|" + this.signingKeyId);

    AuditChainProof proof = new AuditChainProof(seq, prevHash, hash, signature, this.signingKeyId);
    AuditRecord record =
        new AuditRecord(
            eventId, this.tenant, action, principal, purpose, resourceRid, decisionStr, proof);
    this.records.add(record);
    this.headSeq = seq;
    this.headHash = hash;
    return record;
  }

  /** Returns an unmodifiable view of the appended audit records. */
  public synchronized List<AuditRecord> records() {
    return Collections.unmodifiableList(new ArrayList<>(this.records));
  }

  /** Verifies that {@code chain} is contiguous, hash-linked from genesis, and validly signed. */
  public static boolean verifyChain(List<AuditRecord> chain, byte[] signingKey) {
    String expectedPrev = GENESIS_PREV_HASH;
    long expectedSeq = 1L;
    for (AuditRecord rec : chain) {
      if (rec.chain().seq() != expectedSeq || !rec.chain().prevHash().equals(expectedPrev)) {
        return false;
      }
      String canonical =
          canonicalPayload(
              rec.tenant(),
              rec.chain().seq(),
              rec.chain().prevHash(),
              rec.eventId(),
              rec.action(),
              rec.principal(),
              rec.purpose(),
              rec.resourceRid(),
              rec.decision());
      String expectedHash = sha256Hex(canonical);
      String expectedSig =
          hmacSha256Hex(signingKey, expectedHash + "|" + rec.chain().signingKeyId());
      if (!rec.chain().hash().equals(expectedHash)
          || !MessageDigest.isEqual(
              rec.chain().signature().getBytes(StandardCharsets.UTF_8),
              expectedSig.getBytes(StandardCharsets.UTF_8))) {
        return false;
      }
      expectedPrev = expectedHash;
      expectedSeq++;
    }
    return true;
  }

  /** Verifies self-test invariants on the W12 hash chain and tamper detection. */
  public static void verifySelfTest() {
    byte[] key = "synthetic-audit-signing-key-32b!".getBytes(StandardCharsets.UTF_8);
    AuditChainVerifier verifier = new AuditChainVerifier("demo", "audit-k1", key);
    AuditRecord r1 =
        verifier.appendDecision(
            "evt-1",
            "policy.evaluate",
            "user:analyst-1",
            "PROCUREMENT_INTEGRITY",
            "ri.vigil.demo.object.supplier-1",
            true);
    AuditRecord r2 =
        verifier.appendDecision(
            "evt-2",
            "policy.evaluate",
            "user:analyst-1",
            "PROCUREMENT_INTEGRITY",
            "ri.vigil.demo.object.supplier-2",
            false);
    if (r1.chain().seq() != 1L || r2.chain().seq() != 2L || !verifyChain(verifier.records(), key)) {
      throw new IllegalStateException("Valid chain failed verification");
    }
    AuditRecord tamperedDecision =
        new AuditRecord(
            r2.eventId(),
            r2.tenant(),
            r2.action(),
            r2.principal(),
            r2.purpose(),
            r2.resourceRid(),
            "ALLOW",
            r2.chain());
    AuditChainProof badSeqProof =
        new AuditChainProof(
            99L,
            r1.chain().prevHash(),
            r1.chain().hash(),
            r1.chain().signature(),
            r1.chain().signingKeyId());
    AuditRecord badSeqRecord =
        new AuditRecord(
            r1.eventId(),
            r1.tenant(),
            r1.action(),
            r1.principal(),
            r1.purpose(),
            r1.resourceRid(),
            r1.decision(),
            badSeqProof);
    if (verifyChain(List.of(r1, tamperedDecision), key)
        || verifyChain(List.of(badSeqRecord), key)) {
      throw new IllegalStateException("Tampered chain should fail verification");
    }
  }

  private static String canonicalPayload(
      String tenant,
      long seq,
      String prevHash,
      String eventId,
      String action,
      String principal,
      String purpose,
      String resourceRid,
      String decision) {
    return "v1|tenant="
        + tenant
        + "|seq="
        + seq
        + "|prev="
        + prevHash
        + "|id="
        + eventId
        + "|action="
        + action
        + "|principal="
        + principal
        + "|purpose="
        + purpose
        + "|rid="
        + resourceRid
        + "|decision="
        + decision;
  }

  static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  static String hmacSha256Hex(byte[] key, String input) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HmacSHA256 unavailable", e);
    }
  }
}
