// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Validate untrusted model
// output
package africa.vigil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ExtractionRules {
  static Map<String, Object> validate(Map<String, Object> response, String original) {
    try {
      fields(response, Set.of("model", "proposal", "review", "warnings"));
      text(response, "model", 1, 160);
      var proposal = map(response.get("proposal"));
      fields(proposal, Set.of("objects", "links", "gaps"));
      var objects = rows(proposal.get("objects"), 8);
      var links = rows(proposal.get("links"), 12);
      strings(proposal.get("gaps"), 5, 3, 500);
      var types = new HashMap<String, String>();
      for (var object : objects) {
        fields(object, Set.of("id", "object_type", "title", "summary", "quote"));
        String id = id(object), type = text(object, "object_type", 3, 30);
        require(InvestigationService.TYPES.contains(type) && !types.containsKey(id));
        text(object, "title", 3, 160);
        text(object, "summary", 5, 500);
        quote(object, original);
        types.put(id, type);
      }
      var ids = new HashSet<String>();
      var pairs = new HashSet<List<String>>();
      for (var link : links) {
        fields(link, Set.of("id", "from_id", "to_id", "relation", "reason", "quote"));
        require(ids.add(id(link)));
        String from = text(link, "from_id", 1, 40), to = text(link, "to_id", 1, 40);
        String relation = text(link, "relation", 3, 30);
        require(!from.equals(to) && types.containsKey(from) && types.containsKey(to));
        require(InvestigationService.validPair(relation, types.get(from), types.get(to)));
        require(pairs.add(List.of(from, to, relation)));
        text(link, "reason", 5, 500);
        quote(link, original);
      }
      var review = map(response.get("review"));
      fields(review, Set.of("state", "model", "concerns"));
      String state = text(review, "state", 3, 30);
      require(Set.of("NOT_RUN", "COMPLETE", "UNAVAILABLE").contains(state));
      if (state.equals("NOT_RUN")) require(review.get("model") == null);
      else text(review, "model", 1, 160);
      strings(review.get("concerns"), 8, 3, 1600);
      strings(response.get("warnings"), 8, 3, 1600);
      return response;
    } catch (RuntimeException error) {
      throw new Problem(
          502,
          "INVALID_EXTRACTION",
          "Model output failed source validation. No proposal was saved.");
    }
  }

  private static void fields(Map<String, Object> value, Set<String> keys) {
    require(value.keySet().equals(keys));
  }

  private static void require(boolean valid) {
    if (!valid) throw new IllegalArgumentException();
  }

  private static Map<String, Object> map(Object value) {
    require(value instanceof Map<?, ?>);
    return Json.map(value);
  }

  private static List<Map<String, Object>> rows(Object value, int max) {
    require(
        value instanceof List<?> list
            && list.size() <= max
            && list.stream().allMatch(x -> x instanceof Map<?, ?>));
    return Json.rows(value);
  }

  private static String text(Map<String, Object> value, String key, int min, int max) {
    Object raw = value.get(key);
    require(
        raw instanceof String s
            && !s.isBlank()
            && s.indexOf('\0') < 0
            && s.length() >= min
            && s.length() <= max);
    return (String) raw;
  }

  private static void strings(Object value, int count, int min, int max) {
    require(value instanceof List<?> list && list.size() <= count);
    for (Object item : (List<?>) value) text(Json.object("item", item), "item", min, max);
  }

  private static String id(Map<String, Object> value) {
    String id = text(value, "id", 1, 40);
    require(id.matches("[A-Za-z0-9_-]{1,40}"));
    return id;
  }

  private static void quote(Map<String, Object> value, String original) {
    require(original.contains(text(value, "quote", 3, 400)));
  }

  static Set<String> selected(
      Map<String, Object> request, String key, List<Map<String, Object>> proposals) {
    Object value = request.get(key);
    Problem.require(
        value instanceof List<?> list && list.size() <= 12,
        400,
        "VALIDATION_FAILED",
        "Provide a bounded list of proposal IDs.");
    var result = new HashSet<String>();
    for (Object item : (List<?>) value) {
      Problem.require(
          item instanceof String
              && result.add((String) item)
              && proposals.stream().anyMatch(p -> p.get("id").equals(item)),
          400,
          "INVALID_SELECTION",
          "Select each known proposal at most once.");
    }
    return result;
  }
}
