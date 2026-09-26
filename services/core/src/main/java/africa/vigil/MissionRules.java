// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Explicit action lifecycle
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
package africa.vigil;

// Modified for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Preserve source whitespace

import java.util.Map;
import java.util.Set;

final class MissionRules {
  static void fields(Map<String, Object> request, Set<String> allowed) {
    Problem.require(
        allowed.containsAll(request.keySet()),
        400,
        "VALIDATION_FAILED",
        "The request includes unsupported fields.");
  }

  static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "PROPOSED",
          Set.of("AUTHORISED"),
          "AUTHORISED",
          Set.of("SENT"),
          "SENT",
          Set.of("ACCEPTED", "DECLINED", "OUTCOME_UNKNOWN"),
          "ACCEPTED",
          Set.of("IN_PROGRESS"),
          "IN_PROGRESS",
          Set.of("CONFIRMED", "OUTCOME_UNKNOWN"),
          "DECLINED",
          Set.of("PROPOSED"),
          "OUTCOME_UNKNOWN",
          Set.of("ACCEPTED", "DECLINED"),
          "CONFIRMED",
          Set.of());

  static void transition(String from, String to, boolean currentPlan) {
    Problem.require(
        TRANSITIONS.getOrDefault(from, Set.of()).contains(to),
        409,
        "INVALID_TRANSITION",
        "This task cannot move from " + from + " to " + to + ".");
    if (to.equals("AUTHORISED") || to.equals("SENT"))
      Problem.require(
          currentPlan,
          409,
          "PLAN_STALE",
          "Refresh the plan against the current evidence before authorising or sending work.");
  }

  static String text(Map<String, Object> request, String key, int min, int max) {
    Object value = request.get(key);
    Problem.require(
        value instanceof String,
        400,
        "VALIDATION_FAILED",
        "Provide " + key.replace('_', ' ') + ".");
    String result = ((String) value).strip();
    Problem.require(
        result.length() >= min && result.length() <= max,
        400,
        "VALIDATION_FAILED",
        key.replace('_', ' ') + " must contain " + min + "–" + max + " characters.");
    return result;
  }

  static String original(Map<String, Object> request) {
    Object value = request.get("original");
    Problem.require(
        value instanceof String s
            && s.length() >= 5
            && s.length() <= 16000
            && !s.isBlank()
            && s.indexOf('\0') < 0,
        400,
        "VALIDATION_FAILED",
        "Original text must contain 5–16000 characters without NUL bytes.");
    return (String) value;
  }

  static void expectedVersion(Map<String, Object> mission, Map<String, Object> request) {
    Object version = request.get("expected_version");
    Problem.require(
        version instanceof Integer && version.equals(mission.get("version")),
        409,
        "VERSION_CONFLICT",
        "This mission changed. Reload it before applying your change.");
  }

  static boolean currentPlan(Map<String, Object> mission) {
    return mission.get("plan") instanceof Map<?, ?> plan
        && mission.get("evidence_version").equals(plan.get("evidence_version"));
  }
}
