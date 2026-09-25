// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — RLS-scoped transactions and
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// auditable commands
package africa.vigil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
class MissionRepository {
  private final JdbcTemplate db;
  private final TransactionTemplate transactions;

  MissionRepository(JdbcTemplate db, TransactionTemplate transactions) {
    this.db = db;
    this.transactions = transactions;
  }

  <T> T scoped(Policy.Context context, Supplier<T> work) {
    Policy.requireContext(context);
    return transactions.execute(
        status -> {
          db.queryForObject(
              "select set_config('vigil.tenant', ?, true)", String.class, context.tenant());
          return work.get();
        });
  }

  List<Map<String, Object>> list(Policy.Context context) {
    return scoped(
        context,
        () -> {
          access(context, "LIST", "missions");
          return db.query(
              "select body::text from missions order by updated_at desc,id",
              (rs, row) -> Json.decode(rs.getString(1)));
        });
  }

  Map<String, Object> get(Policy.Context context, String id) {
    return scoped(
        context,
        () -> {
          var mission = read(id, false);
          access(context, "READ", id);
          return mission;
        });
  }

  private void access(Policy.Context context, String operation, String resource) {
    db.update(
        "insert into mission_access(tenant_id,actor,purpose,operation,resource) values(?,?,?,?,?)",
        context.tenant(),
        context.actor(),
        context.purpose(),
        operation,
        resource);
  }

  Map<String, Object> export(Policy.Context context, String id) {
    return scoped(
        context,
        () -> {
          var mission = read(id, true);
          access(context, "EXPORT", id);
          return Json.object(
              "format",
              "vigil.mission.v1",
              "synthetic",
              true,
              "mission",
              mission,
              "audit",
              audit(context, id));
        });
  }

  Map<String, Object> read(String id, boolean lock) {
    var rows =
        db.query(
            "select body::text from missions where id=?" + (lock ? " for update" : ""),
            (rs, row) -> Json.decode(rs.getString(1)),
            id);
    Problem.require(
        !rows.isEmpty(), 404, "NOT_FOUND", "The mission is unavailable in this workspace.");
    return rows.getFirst();
  }

  void insert(Policy.Context context, Map<String, Object> mission) {
    db.update(
        "insert into missions(id,tenant_id,version,body) values(?,?,?,?::jsonb)",
        mission.get("id"),
        context.tenant(),
        mission.get("version"),
        Json.encode(mission));
  }

  void save(Map<String, Object> mission) {
    db.update(
        "update missions set version=?,body=?::jsonb,updated_at=now() where id=?",
        mission.get("version"),
        Json.encode(mission),
        mission.get("id"));
  }

  Map<String, Object> replay(
      Policy.Context context, String scope, String key, Map<String, Object> request) {
    Policy.requireWrite(context);
    Problem.require(
        key != null && key.matches("[A-Za-z0-9_-]{8,128}"),
        400,
        "IDEMPOTENCY_REQUIRED",
        "A valid idempotency key is required.");
    return scoped(
        context,
        () -> {
          var found =
              db.queryForList(
                  "select fingerprint,response::text as response from mission_commands where tenant_id=? and actor=? and scope=? and command_key=?",
                  context.tenant(),
                  context.actor(),
                  scope,
                  key);
          if (found.isEmpty()) return null;
          Problem.require(
              Json.hash(Json.encode(request)).equals(found.getFirst().get("fingerprint")),
              409,
              "IDEMPOTENCY_CONFLICT",
              "That command key was already used for different content.");
          return Json.decode((String) found.getFirst().get("response"));
        });
  }

  Map<String, Object> command(
      Policy.Context context,
      String scope,
      String key,
      Map<String, Object> request,
      Supplier<Map<String, Object>> work) {
    Policy.requireWrite(context);
    Problem.require(
        key != null && key.matches("[A-Za-z0-9_-]{8,128}"),
        400,
        "IDEMPOTENCY_REQUIRED",
        "A valid idempotency key is required.");
    return scoped(
        context,
        () -> {
          db.queryForList(
              "select pg_advisory_xact_lock(hashtextextended(?,0))",
              context.tenant() + ":" + context.actor() + ":" + scope + ":" + key);
          String fingerprint = Json.hash(Json.encode(request));
          var found =
              db.queryForList(
                  "select fingerprint,response::text as response from mission_commands where tenant_id=? and actor=? and scope=? and command_key=?",
                  context.tenant(),
                  context.actor(),
                  scope,
                  key);
          if (!found.isEmpty()) {
            Problem.require(
                fingerprint.equals(found.getFirst().get("fingerprint")),
                409,
                "IDEMPOTENCY_CONFLICT",
                "That command key was already used for different content.");
            return Json.decode((String) found.getFirst().get("response"));
          }
          Map<String, Object> response = work.get();
          db.update(
              "insert into mission_commands(tenant_id,actor,scope,command_key,fingerprint,response) values(?,?,?,?,?,?::jsonb)",
              context.tenant(),
              context.actor(),
              scope,
              key,
              fingerprint,
              Json.encode(response));
          return response;
        });
  }

  void seed(Policy.Context context, Map<String, Object> mission) {
    scoped(
        context,
        () -> {
          Integer count =
              db.queryForObject(
                  "select count(*) from missions where id=?", Integer.class, mission.get("id"));
          if (count == 0) {
            insert(context, mission);
            append(
                context,
                (String) mission.get("id"),
                "MISSION_CREATED",
                "Synthetic demonstration mission opened.");
          }
          return null;
        });
  }

  void append(Policy.Context context, String missionId, String kind, String detail) {
    var prior =
        db.queryForList(
            "select event_hash from mission_audit where mission_id=? order by id desc limit 1",
            missionId);
    String previous = prior.isEmpty() ? "GENESIS" : (String) prior.getFirst().get("event_hash");
    String eventId = UUID.randomUUID().toString(), at = Instant.now().toString();
    String hash =
        eventHash(
            context.tenant(), missionId, eventId, at, context.actor(), kind, detail, previous);
    db.update(
        "insert into mission_audit(tenant_id,mission_id,event_id,recorded_at,actor,kind,detail,previous_hash,event_hash) values(?,?,?,?,?,?,?,?,?)",
        context.tenant(),
        missionId,
        eventId,
        at,
        context.actor(),
        kind,
        detail,
        previous,
        hash);
  }

  Map<String, Object> audit(Policy.Context context, String missionId) {
    return scoped(
        context,
        () -> {
          read(missionId, false);
          access(context, "AUDIT_READ", missionId);
          var rows =
              db.queryForList(
                  "select event_id,recorded_at,actor,kind,detail,previous_hash,event_hash from mission_audit where mission_id=? order by id",
                  missionId);
          String previous = "GENESIS";
          boolean valid = true;
          for (var row : rows) {
            String computed =
                eventHash(
                    context.tenant(),
                    missionId,
                    (String) row.get("event_id"),
                    (String) row.get("recorded_at"),
                    (String) row.get("actor"),
                    (String) row.get("kind"),
                    (String) row.get("detail"),
                    previous);
            valid &=
                previous.equals(row.get("previous_hash")) && computed.equals(row.get("event_hash"));
            previous = (String) row.get("event_hash");
          }
          return Json.object(
              "events",
              new ArrayList<>(rows.reversed()),
              "verified",
              valid,
              "event_count",
              rows.size(),
              "head_hash",
              previous,
              "verification_scope",
              "Local hash-chain integrity; external anchoring is not configured.");
        });
  }

  private static String eventHash(
      String tenant,
      String mission,
      String id,
      String at,
      String actor,
      String kind,
      String detail,
      String previous) {
    return Json.hash(Json.encode(List.of(tenant, mission, id, at, actor, kind, detail, previous)));
  }
}
