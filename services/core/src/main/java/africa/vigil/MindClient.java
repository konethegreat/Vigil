// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Authorised snapshot boundary
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// to Mind
package africa.vigil;

// Modified for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Scoped extraction request

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class MindClient {
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
  private final String url, token;

  MindClient(
      @Value("${VIGIL_MIND_URL:http://localhost:8017}") String url,
      @Value("${VIGIL_INTERNAL_TOKEN}") String token) {
    this.url = url;
    this.token = token;
  }

  Map<String, Object> status() {
    try {
      return call("/v1/status", null);
    } catch (RuntimeException error) {
      return Json.object(
          "state",
          "UNAVAILABLE",
          "mode",
          "BASELINE",
          "reasoner_model",
          null,
          "challenger_model",
          null,
          "message",
          "The intelligence service is unavailable. Existing missions and evidence remain accessible.");
    }
  }

  Map<String, Object> plan(Policy.Context context, Map<String, Object> mission) {
    Policy.requireWrite(context);
    var evidence =
        Json.rows(mission.get("evidence")).stream()
            .map(
                item ->
                    Json.object(
                        "id",
                        item.get("id"),
                        "title",
                        item.get("title"),
                        "statement",
                        item.get("statement"),
                        "status",
                        item.get("status"),
                        "sha256",
                        item.get("sha256")))
            .toList();
    return call(
        "/v1/plan",
        Json.object(
            "mission_id",
            mission.get("id"),
            "domain",
            mission.get("domain"),
            "objective",
            mission.get("objective"),
            "evidence_version",
            mission.get("evidence_version"),
            "evidence",
            evidence));
  }

  Map<String, Object> extract(
      Policy.Context context, Map<String, Object> mission, Map<String, Object> source) {
    Policy.requireWrite(context);
    return call(
        "/v1/extract",
        Json.object(
            "mission_id",
            mission.get("id"),
            "evidence_id",
            source.get("id"),
            "evidence_version",
            mission.get("evidence_version"),
            "original",
            source.get("original"),
            "sha256",
            source.get("sha256")));
  }

  private Map<String, Object> call(String path, Map<String, Object> body) {
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(url + path))
              .timeout(Duration.ofSeconds(210))
              .header("X-Vigil-Service", token)
              .header("Content-Type", "application/json");
      if (body == null) builder.GET();
      else builder.POST(HttpRequest.BodyPublishers.ofString(Json.encode(body)));
      var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      Problem.require(
          response.statusCode() == 200,
          503,
          "MIND_UNAVAILABLE",
          "Intelligence could not complete the request. Existing work has been preserved.");
      return Json.decode(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new Problem(503, "MIND_UNAVAILABLE", "Intelligence was interrupted. Try again.");
    } catch (Problem e) {
      throw e;
    } catch (Exception e) {
      throw new Problem(
          503,
          "MIND_UNAVAILABLE",
          "The intelligence service is unavailable. Existing work has been preserved.");
    }
  }
}
