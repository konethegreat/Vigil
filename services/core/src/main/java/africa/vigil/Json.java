// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Canonical JSON and evidence
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// hashes
package africa.vigil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class Json {
  private static final JsonMapper MAPPER =
      JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();

  static String encode(Object value) {
    return MAPPER.writeValueAsString(value);
  }

  static Map<String, Object> decode(String value) {
    return MAPPER.readValue(value, new TypeReference<Map<String, Object>>() {});
  }

  static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  static Map<String, Object> object(Object... pairs) {
    var result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
    return result;
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> map(Object value) {
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  static List<Map<String, Object>> rows(Object value) {
    return (List<Map<String, Object>>) value;
  }

  static int number(Map<String, Object> value, String key) {
    return ((Number) value.get(key)).intValue();
  }
}
