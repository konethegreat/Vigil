// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - W12 hash-chained and HMAC-signed
// audit ledger for services/core (P3 task 3.7)
package vigil.core.audit;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Append-only, SHA-256 hash-chained and HMAC-SHA-256 signed W12 audit ledger verifier for the Core
 * service ({@code vigil.core.audit}, P3 task 3.7).
 */
public final class AuditChainVerifier {

  /** Genesis {@code prev_hash} for sequence 1 in every tenant ledger. */
  public static final String GENESIS_PREV_HASH = "0".repeat(64);

  /** Immutable W12 audit event record. */
  public record AuditRecord(
      String tenant,
      long seq,
      String prevHash,
      String eventId,
      String action,
      String principal,
      String purpose,
      String resourceRid,
      String decision,
      String signingKeyId,
      String hash,
      String signature) {}

  private AuditChainVerifier() {}

  /** Builds and signs a chained {@link AuditRecord} following {@code prevHash}. */
  public static AuditRecord createRecord(
      String tenant,
      long seq,
      String prevHash,
      String eventId,
      String action,
      String principal,
      String purpose,
      String resourceRid,
      boolean allow,
      String signingKeyId,
      byte[] signingKey) {
    String decision = allow ? "ALLOW" : "DENY";
    String hash =
        sha256Hex(
            canonical(
                tenant, seq, prevHash, eventId, action, principal, purpose, resourceRid, decision));
    String sig = hmacSha256Hex(signingKey, hash + "|" + signingKeyId);
    return new AuditRecord(
        tenant,
        seq,
        prevHash,
        eventId,
        action,
        principal,
        purpose,
        resourceRid,
        decision,
        signingKeyId,
        hash,
        sig);
  }

  /** Verifies that {@code chain} is contiguous, hash-linked from genesis, and validly signed. */
  public static boolean verifyChain(List<AuditRecord> chain, byte[] signingKey) {
    String expectedPrev = GENESIS_PREV_HASH;
    long expectedSeq = 1L;
    for (AuditRecord rec : chain) {
      String expectedHash =
          sha256Hex(
              canonical(
                  rec.tenant(),
                  rec.seq(),
                  rec.prevHash(),
                  rec.eventId(),
                  rec.action(),
                  rec.principal(),
                  rec.purpose(),
                  rec.resourceRid(),
                  rec.decision()));
      String expectedSig = hmacSha256Hex(signingKey, expectedHash + "|" + rec.signingKeyId());
      if (rec.seq() != expectedSeq
          || !rec.prevHash().equals(expectedPrev)
          || !rec.hash().equals(expectedHash)
          || !MessageDigest.isEqual(
              rec.signature().getBytes(StandardCharsets.UTF_8),
              expectedSig.getBytes(StandardCharsets.UTF_8))) {
        return false;
      }
      expectedPrev = expectedHash;
      expectedSeq++;
    }
    return true;
  }

  private static String canonical(
      String tenant,
      long seq,
      String prev,
      String id,
      String action,
      String principal,
      String purpose,
      String rid,
      String decision) {
    return String.join(
        "|", tenant, Long.toString(seq), prev, id, action, principal, purpose, rid, decision);
  }

  static String sha256Hex(String input) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  static String hmacSha256Hex(byte[] key, String input) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
