// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Safe API errors
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
package africa.vigil;

final class Problem extends RuntimeException {
  final int status;
  final String code;

  Problem(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  static void require(boolean condition, int status, String code, String message) {
    if (!condition) throw new Problem(status, code, message);
  }
}
