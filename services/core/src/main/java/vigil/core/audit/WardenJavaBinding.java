// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Java 25 Panama FFM binding and
// 560-case conformance runner for the Warden Security Kernel (P3 task 3.3)
package vigil.core.audit;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Java 25 Foreign Function & Memory (Panama FFM) binding over the compiled {@code vigil_warden}
 * native library.
 */
public final class WardenJavaBinding {

  /** Summary of a single conformance case evaluation. */
  public record ConformanceDecisionSummary(
      boolean allow, int externalHttpStatus, int redactionCount, int obligationCount) {}

  private WardenJavaBinding() {}

  /** Decodes the 32-bit packed decision word returned by {@code warden_eval_conformance_case}. */
  public static ConformanceDecisionSummary decodePackedDecision(int packed) {
    return new ConformanceDecisionSummary(
        (packed & 1) == 1, (packed >>> 1) & 0x7fff, (packed >>> 16) & 0xff, (packed >>> 24) & 0x7f);
  }

  /** Attempts to locate the compiled {@code vigil_warden} shared library on disk. */
  public static Path findNativeLibrary(Path repoRoot) {
    for (String rel :
        List.of(
            "services/warden/target/release/vigil_warden.dll",
            "services/warden/target/release/libvigil_warden.so",
            "services/warden/target/release/libvigil_warden.dylib")) {
      Path p = repoRoot.resolve(rel).normalize();
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    throw new IllegalStateException("Missing vigil_warden library under " + repoRoot);
  }

  /** Executes the 560-case shared conformance suite via Java 25 Panama FFM ({@link Linker}). */
  public static int runNativeConformanceSuite(Path libraryPath) throws Throwable {
    Linker linker = Linker.nativeLinker();
    try (Arena arena = Arena.ofConfined()) {
      SymbolLookup lookup = SymbolLookup.libraryLookup(libraryPath, arena);
      MethodHandle countHandle =
          linker.downcallHandle(
              lookup.find("warden_conformance_count").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT));
      MethodHandle runHandle =
          linker.downcallHandle(
              lookup.find("warden_run_conformance_suite").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT));
      MethodHandle evalHandle =
          linker.downcallHandle(
              lookup.find("warden_eval_conformance_case").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
      int total = (int) countHandle.invokeExact();
      int verified = (int) runHandle.invokeExact();
      if (total < 500 || verified != total) {
        throw new IllegalStateException("Conformance suite mismatch: " + verified + "/" + total);
      }
      for (int idx = 0; idx < total; idx++) {
        if (decodePackedDecision((int) evalHandle.invokeExact(idx)).obligationCount() < 1) {
          throw new IllegalStateException("Missing W12 obligation at case " + idx);
        }
      }
      return verified;
    }
  }

  /** CLI entrypoint invoked by {@code services/warden/verify_p3.sh}. */
  public static void main(String[] args) throws Throwable {
    byte[] key = "synthetic-audit-signing-key-32b!".getBytes(StandardCharsets.UTF_8);
    AuditChainVerifier.AuditRecord r1 =
        AuditChainVerifier.createRecord(
            "demo",
            1L,
            AuditChainVerifier.GENESIS_PREV_HASH,
            "evt-1",
            "policy.evaluate",
            "user:analyst-1",
            "PROCUREMENT_INTEGRITY",
            "ri.vigil.demo.object.supplier-1",
            true,
            "audit-k1",
            key);
    if (!AuditChainVerifier.verifyChain(List.of(r1), key)) {
      throw new IllegalStateException("AuditChainVerifier self-test failed");
    }
    Path root = args.length > 0 ? Path.of(args[0]) : Path.of(".");
    int verified = runNativeConformanceSuite(findNativeLibrary(root));
    System.out.println(
        "PASS [java-ffm-binding]: verified " + verified + " Warden conformance cases + W12 chain");
  }
}
