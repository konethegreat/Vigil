// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mandatory bounded demo policy
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Broader district operations
// purpose
// boundary
package africa.vigil;

// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Distinct synthetic agency
// identities

import java.util.List;
import java.util.Map;

final class Policy {
  record Context(String actor, String name, String tenant, String role, String purpose) {
    Map<String, Object> publicView() {
      return Json.object(
          "actor",
          actor,
          "name",
          name,
          "organisation",
          organisation(tenant),
          "role",
          role,
          "purpose",
          purpose,
          "read_only",
          role.equals("REVIEWER"),
          "agency_id",
          tenant,
          "synthetic",
          true);
    }
  }

  private static final Map<String, Context> IDENTITIES =
      Map.of(
          "operations",
              new Context(
                  "operations",
                  "Operations coordinator",
                  "operations-demo",
                  "COORDINATOR",
                  "DISTRICT_OPERATIONS"),
          "protection",
              new Context(
                  "protection",
                  "Protection practitioner",
                  "care-demo",
                  "PRACTITIONER",
                  "PROTECTION_SUPPORT"),
          "gauteng",
              new Context(
                  "gauteng",
                  "Gauteng duty coordinator",
                  "gauteng-demo",
                  "COORDINATOR",
                  "INCIDENT_COORDINATION"),
          "limpopo",
              new Context(
                  "limpopo",
                  "Limpopo duty coordinator",
                  "limpopo-demo",
                  "COORDINATOR",
                  "INCIDENT_COORDINATION"),
          "agency-reviewer",
              new Context(
                  "agency-reviewer",
                  "Gauteng case reviewer",
                  "gauteng-demo",
                  "REVIEWER",
                  "MISSION_REVIEW"),
          "reviewer",
              new Context(
                  "reviewer",
                  "Independent reviewer",
                  "operations-demo",
                  "REVIEWER",
                  "MISSION_REVIEW"));

  static String organisation(String tenant) {
    return switch (tenant) {
      case "care-demo" -> "Protection services";
      case "gauteng-demo" -> "Gauteng coordination · fictional";
      case "limpopo-demo" -> "Limpopo coordination · fictional";
      default -> "District operations";
    };
  }

  static List<String> agencies() {
    return List.of("gauteng-demo", "limpopo-demo");
  }

  static Context identity(String actor) {
    Context context = IDENTITIES.get(actor);
    Problem.require(
        context != null, 401, "INVALID_SESSION", "Choose a valid demonstration identity.");
    return context;
  }

  static void requireContext(Context context) {
    Problem.require(
        context != null && context.equals(IDENTITIES.get(context.actor())),
        403,
        "POLICY_DENIED",
        "A verified policy context is required.");
  }

  static void requireWrite(Context context) {
    requireContext(context);
    Problem.require(
        !context.role().equals("REVIEWER"),
        403,
        "POLICY_DENIED",
        "This identity has read-only access.");
  }

  static void requireDomain(Context context, String domain) {
    requireWrite(context);
    boolean allowed =
        context.tenant().equals("care-demo")
            ? domain.equals("PROTECT")
            : List.of("LIFELINES", "RESPONSE", "INTEGRITY", "INVESTIGATE").contains(domain);
    Problem.require(
        allowed, 403, "POLICY_DENIED", "This mission belongs in another organisation's workspace.");
  }
}
