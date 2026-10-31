// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Bounded coordination input rules
package africa.vigil;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CoordinationRules {
  static String text(Map<String, Object> body, String field, int min, int max) {
    String value = MissionRules.text(body, field, min, max);
    Problem.require(
        value.indexOf('\0') < 0, 400, "VALIDATION_FAILED", "Text cannot contain NUL bytes.");
    return value;
  }

  static String choice(Map<String, Object> body, String field, Set<String> choices) {
    String value = text(body, field, 1, 80);
    Problem.require(
        choices.contains(value), 400, "VALIDATION_FAILED", "Unsupported " + field + ".");
    return value;
  }

  static Instant instant(Map<String, Object> body, String field) {
    try {
      return Instant.parse(text(body, field, 10, 40));
    } catch (DateTimeParseException e) {
      throw new Problem(
          400, "VALIDATION_FAILED", "Provide an ISO timestamp with timezone for " + field + ".");
    }
  }

  static int integer(Map<String, Object> body, String field, int min, int max) {
    Object value = body.get(field);
    Problem.require(
        value instanceof Integer n && n >= min && n <= max,
        400,
        "VALIDATION_FAILED",
        "Invalid " + field + ".");
    return (Integer) value;
  }

  static double coordinate(Map<String, Object> body, String field, double min, double max) {
    Object value = body.get(field);
    Problem.require(
        value instanceof Number n
            && Double.isFinite(n.doubleValue())
            && n.doubleValue() >= min
            && n.doubleValue() <= max,
        400,
        "VALIDATION_FAILED",
        "Invalid " + field + ".");
    return ((Number) value).doubleValue();
  }

  static List<String> ids(Map<String, Object> body, String field) {
    Object value = body.get(field);
    Problem.require(
        value instanceof List<?> l
            && !l.isEmpty()
            && l.size() <= 20
            && l.stream().allMatch(x -> x instanceof String s && s.matches("OBS-[A-Za-z0-9-]+"))
            && l.stream().distinct().count() == l.size(),
        400,
        "VALIDATION_FAILED",
        "Select 1–20 distinct observations.");
    return ((List<?>) value).stream().map(String.class::cast).toList();
  }

  static void version(Map<String, Object> record, Map<String, Object> body) {
    MissionRules.expectedVersion(record, body);
  }

  static String id(String prefix) {
    return prefix + java.util.UUID.randomUUID();
  }
}
