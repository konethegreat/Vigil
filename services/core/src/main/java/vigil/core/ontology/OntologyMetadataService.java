// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Core Ontology Metadata Service (OMS):
// type system, immutable versioning, physical PostgreSQL/RLS/OpenSearch DDL generator, and YAML
// pack loader (P5 tasks 5.1-5.5)
package vigil.core.ontology;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Core Ontology Metadata Service (`vigil.ontology.v1.OntologyService`, `API_DESIGN.md` section B.1,
 * `DATA_MODEL.md` sections 3.2-3.4 and 4.3-4.7, `VIGIL_DOMAIN_BLUEPRINT.md` sections 1-2).
 */
public final class OntologyMetadataService {

  private static final Set<String> RESERVED_COLUMNS =
      Set.of(
          "pk",
          "rid",
          "markings",
          "classification",
          "releasability",
          "purpose_tags",
          "source_id",
          "source_record_id",
          "connector_version",
          "fetched_at",
          "terms_ref",
          "responsible_party",
          "lawful_basis",
          "data_origin",
          "dataset_rid",
          "dataset_txn_id",
          "content_sha256",
          "edited_props",
          "last_edit_seq",
          "valid_from",
          "updated_at",
          "version",
          "deleted_at",
          "redacted_properties");

  private static final Set<String> VALID_BASE_TYPES =
      Set.of(
          "boolean",
          "integer",
          "long",
          "double",
          "decimal",
          "string",
          "date",
          "timestamp",
          "struct",
          "geopoint",
          "geoshape",
          "time_series",
          "geotemporal_series",
          "vector",
          "media_reference",
          "cipher_text",
          "marking");

  public enum Compatibility {
    INITIAL,
    COMPATIBLE,
    BREAKING
  }

  public record CallerContext(
      String principal, Set<String> heldMarkings, int clearance, Set<String> roles) {}

  public record PropertyDefinition(
      String apiName,
      String baseType,
      boolean isArray,
      boolean nullable,
      boolean editable,
      String conflictPolicy,
      boolean searchable,
      List<String> columnMarkings,
      int columnClassification,
      Integer vectorDimension,
      String embeddingModel) {
    public PropertyDefinition {
      if (apiName == null || !apiName.matches("^[a-z][a-z0-9_]{0,62}$")) {
        throw new IllegalArgumentException("Invalid property api_name: " + apiName);
      }
      if (RESERVED_COLUMNS.contains(apiName)) {
        throw new IllegalArgumentException("Property api_name is a reserved column: " + apiName);
      }
      if (baseType == null || !VALID_BASE_TYPES.contains(baseType)) {
        throw new IllegalArgumentException("Unsupported base_type: " + baseType);
      }
      if (isArray
          && ("vector".equals(baseType)
              || "time_series".equals(baseType)
              || "geotemporal_series".equals(baseType))) {
        throw new IllegalArgumentException("isArray is forbidden for base_type " + baseType);
      }
      if ("vector".equals(baseType)
          != (vectorDimension != null
              && vectorDimension >= 1
              && vectorDimension <= 2000
              && embeddingModel != null
              && !embeddingModel.isBlank())) {
        throw new IllegalArgumentException(
            "vector properties must declare vectorDimension (1..2000) and embeddingModel");
      }
      if (editable != (conflictPolicy != null && !conflictPolicy.isBlank())) {
        throw new IllegalArgumentException(
            "Every editable property must declare a conflict_policy (Blueprint section 1.9)");
      }
      columnMarkings =
          List.copyOf(new TreeSet<>(columnMarkings == null ? List.of() : columnMarkings));
      if (columnClassification < 0 || columnClassification > 4) {
        throw new IllegalArgumentException("columnClassification must be in 0..4");
      }
    }
  }

  public record ObjectTypeDefinition(
      UUID objectTypeId,
      String apiName,
      String displayName,
      String status,
      String primaryKey,
      String titleProperty,
      String existencePolicy,
      List<String> implementsInterfaces,
      List<String> defaultMarkings,
      int defaultClassification,
      String packName,
      String packVersion,
      Map<String, PropertyDefinition> properties) {
    public ObjectTypeDefinition {
      Objects.requireNonNull(objectTypeId, "objectTypeId");
      if (apiName == null || !apiName.matches("^[A-Z][A-Za-z0-9]{1,62}$")) {
        throw new IllegalArgumentException("Invalid ObjectType api_name: " + apiName);
      }
      if (primaryKey == null || !properties.containsKey(primaryKey)) {
        throw new IllegalArgumentException(
            "primaryKey " + primaryKey + " must exist in properties of " + apiName);
      }
      if (titleProperty == null || !properties.containsKey(titleProperty)) {
        throw new IllegalArgumentException(
            "titleProperty " + titleProperty + " must exist in properties of " + apiName);
      }
      implementsInterfaces =
          List.copyOf(implementsInterfaces == null ? List.of() : implementsInterfaces);
      defaultMarkings =
          List.copyOf(new TreeSet<>(defaultMarkings == null ? List.of() : defaultMarkings));
      properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public String physicalTable() {
      return "ot_" + toSnakeCase(apiName);
    }
  }

  public record LinkTypeDefinition(
      String apiName,
      String inverseApiName,
      String cardinality,
      String aObjectType,
      String bObjectType,
      String fkProperty,
      String joinTable,
      List<String> linkMarkings) {
    public LinkTypeDefinition {
      if (apiName == null || !apiName.matches("^[a-z][a-z0-9_]{1,62}$")) {
        throw new IllegalArgumentException("Invalid LinkType api_name: " + apiName);
      }
      if ("MANY_TO_MANY".equals(cardinality)
          && (joinTable == null || !joinTable.matches("^lt_[a-z][a-z0-9_]{1,59}$"))) {
        throw new IllegalArgumentException(
            "MANY_TO_MANY link requires a valid join_table (lt_*): " + apiName);
      }
      if (("ONE_TO_ONE".equals(cardinality) || "MANY_TO_ONE".equals(cardinality))
          && (fkProperty == null || fkProperty.isBlank())) {
        throw new IllegalArgumentException(cardinality + " link requires fkProperty: " + apiName);
      }
      linkMarkings = List.copyOf(new TreeSet<>(linkMarkings == null ? List.of() : linkMarkings));
    }
  }

  public record InterfaceTypeDefinition(
      String apiName, Map<String, String> sharedPropertyBaseTypes) {}

  public record SubmissionCriterion(String name, String condition, String failureMessage) {}

  public record ActionTypeDefinition(
      String apiName,
      String displayName,
      String status,
      String outcomeClass,
      boolean requiresHumanApproval,
      boolean agentExecutable,
      boolean justificationRequired,
      List<String> allowedPurposes,
      boolean hasWritebackWebhook,
      Integer webhookTimeoutMs,
      int maxObjects,
      List<SubmissionCriterion> submissionCriteria) {
    public ActionTypeDefinition {
      if (("ADVERSE".equals(outcomeClass) || "LEGAL_EFFECT".equals(outcomeClass))
          && !requiresHumanApproval) {
        throw new IllegalArgumentException(
            "POPIA s71 violation: ADVERSE and LEGAL_EFFECT actions require human approval");
      }
      if (hasWritebackWebhook != (webhookTimeoutMs != null)) {
        throw new IllegalArgumentException(
            "hasWritebackWebhook must match presence of webhookTimeoutMs");
      }
      if (allowedPurposes == null || allowedPurposes.isEmpty()) {
        throw new IllegalArgumentException("allowedPurposes must contain at least one purpose");
      }
      allowedPurposes = List.copyOf(allowedPurposes);
      submissionCriteria = List.copyOf(submissionCriteria == null ? List.of() : submissionCriteria);
    }
  }

  public record SchemaDiffEntry(String change, String propertyName, Compatibility compatibility) {}

  public record TypeVersionRecord(
      String versionRid,
      String typeRid,
      String apiName,
      int version,
      Compatibility compatibility,
      String definitionSha256,
      String branch,
      String publishedBy,
      Instant publishedAt,
      boolean hasMigrationPlan,
      List<SchemaDiffEntry> diff,
      String physicalDdl,
      String physicalDdlSha256) {}

  public record ObjectTypeView(
      ObjectTypeDefinition definition, int version, Map<String, Boolean> propertyAccessible) {}

  private final String tenant;
  private final Map<String, Map<String, InterfaceTypeDefinition>> interfacesByBranch =
      new LinkedHashMap<>();
  private final Map<String, Map<String, ObjectTypeDefinition>> objectTypesByBranch =
      new LinkedHashMap<>();
  private final Map<String, Map<String, Integer>> currentVersionsByBranch = new LinkedHashMap<>();
  private final Map<String, TypeVersionRecord> typeVersionsByRid = new LinkedHashMap<>();
  private final Map<String, List<LinkTypeDefinition>> linksByBranch = new LinkedHashMap<>();
  private final Map<String, Map<String, ActionTypeDefinition>> actionsByBranch =
      new LinkedHashMap<>();

  public OntologyMetadataService(String tenant) {
    this.tenant = Objects.requireNonNull(tenant, "tenant");
    interfacesByBranch.put("main", new LinkedHashMap<>());
    objectTypesByBranch.put("main", new LinkedHashMap<>());
    currentVersionsByBranch.put("main", new LinkedHashMap<>());
    linksByBranch.put("main", new ArrayList<>());
    actionsByBranch.put("main", new LinkedHashMap<>());
  }

  public synchronized void registerInterface(String branch, InterfaceTypeDefinition iface) {
    interfacesByBranch
        .computeIfAbsent(branch, b -> new LinkedHashMap<>())
        .put(iface.apiName(), iface);
  }

  public synchronized void registerActionType(String branch, ActionTypeDefinition action) {
    actionsByBranch
        .computeIfAbsent(branch, b -> new LinkedHashMap<>())
        .put(action.apiName(), action);
  }

  public synchronized void registerLinkType(String branch, LinkTypeDefinition link) {
    Map<String, ObjectTypeDefinition> types = objectTypesByBranch.getOrDefault(branch, Map.of());
    if (!types.containsKey(link.aObjectType()) || !types.containsKey(link.bObjectType())) {
      throw new IllegalArgumentException(
          "Link endpoints must exist on branch " + branch + ": " + link.apiName());
    }
    linksByBranch.computeIfAbsent(branch, b -> new ArrayList<>()).add(link);
  }

  /**
   * Validates and publishes a new immutable version of an `ObjectType` (`OMS-03`), enforcing
   * interface conformance, W8 non-downgrade of security markings/classification, and migration-plan
   * requirements on `BREAKING` changes.
   */
  public synchronized TypeVersionRecord publishObjectType(
      String branch,
      ObjectTypeDefinition candidate,
      int expectedCurrentVersion,
      String migrationPlanJson,
      boolean dryRun,
      String publishedBy) {
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    Map<String, ObjectTypeDefinition> branchObjects =
        objectTypesByBranch.computeIfAbsent(targetBranch, b -> new LinkedHashMap<>());
    Map<String, Integer> branchVersions =
        currentVersionsByBranch.computeIfAbsent(targetBranch, b -> new LinkedHashMap<>());
    Map<String, InterfaceTypeDefinition> branchInterfaces =
        interfacesByBranch.getOrDefault(targetBranch, Map.of());

    for (String ifaceName : candidate.implementsInterfaces()) {
      InterfaceTypeDefinition iface = branchInterfaces.get(ifaceName);
      if (iface == null) {
        throw new IllegalArgumentException("Unknown implemented interface: " + ifaceName);
      }
      for (Map.Entry<String, String> reqProp : iface.sharedPropertyBaseTypes().entrySet()) {
        PropertyDefinition prop = candidate.properties().get(reqProp.getKey());
        if (prop == null || !prop.baseType().equals(reqProp.getValue())) {
          throw new IllegalArgumentException(
              candidate.apiName()
                  + " does not satisfy interface "
                  + ifaceName
                  + " property "
                  + reqProp.getKey());
        }
      }
    }

    int currentVer = branchVersions.getOrDefault(candidate.apiName(), 0);
    if (currentVer != expectedCurrentVersion) {
      throw new IllegalStateException(
          "CONFLICT: expected_current_version "
              + expectedCurrentVersion
              + " does not match actual version "
              + currentVer);
    }

    ObjectTypeDefinition previous = branchObjects.get(candidate.apiName());
    List<SchemaDiffEntry> diffs = new ArrayList<>();
    Compatibility overallCompat = Compatibility.INITIAL;

    if (previous != null) {
      overallCompat = Compatibility.COMPATIBLE;
      // W8 check: cannot remove markings or lower classification without declassification
      if (!candidate.defaultMarkings().containsAll(previous.defaultMarkings())
          || candidate.defaultClassification() < previous.defaultClassification()) {
        throw new SecurityException(
            "POLICY_DENIED (W8): cannot remove markings or lower classification on "
                + candidate.apiName());
      }
      if (!candidate.primaryKey().equals(previous.primaryKey())) {
        diffs.add(
            new SchemaDiffEntry(
                "CHANGE_PRIMARY_KEY", candidate.primaryKey(), Compatibility.BREAKING));
        overallCompat = Compatibility.BREAKING;
      }
      for (Map.Entry<String, PropertyDefinition> prevEntry : previous.properties().entrySet()) {
        String propName = prevEntry.getKey();
        PropertyDefinition prevProp = prevEntry.getValue();
        PropertyDefinition nextProp = candidate.properties().get(propName);
        if (nextProp == null) {
          diffs.add(new SchemaDiffEntry("DROP_PROPERTY", propName, Compatibility.BREAKING));
          overallCompat = Compatibility.BREAKING;
        } else {
          if (!nextProp.columnMarkings().containsAll(prevProp.columnMarkings())
              || nextProp.columnClassification() < prevProp.columnClassification()) {
            throw new SecurityException(
                "POLICY_DENIED (W8): cannot remove column markings or lower classification on "
                    + propName);
          }
          if (!nextProp.baseType().equals(prevProp.baseType())
              || nextProp.isArray() != prevProp.isArray()) {
            diffs.add(new SchemaDiffEntry("CHANGE_BASE_TYPE", propName, Compatibility.BREAKING));
            overallCompat = Compatibility.BREAKING;
          } else if (prevProp.nullable() && !nextProp.nullable()) {
            diffs.add(new SchemaDiffEntry("MAKE_REQUIRED", propName, Compatibility.BREAKING));
            overallCompat = Compatibility.BREAKING;
          }
        }
      }
      for (Map.Entry<String, PropertyDefinition> nextEntry : candidate.properties().entrySet()) {
        if (!previous.properties().containsKey(nextEntry.getKey())) {
          Compatibility addCompat =
              nextEntry.getValue().nullable() ? Compatibility.COMPATIBLE : Compatibility.BREAKING;
          diffs.add(new SchemaDiffEntry("ADD_PROPERTY", nextEntry.getKey(), addCompat));
          if (addCompat == Compatibility.BREAKING) {
            overallCompat = Compatibility.BREAKING;
          }
        }
      }
    }

    boolean hasMigrationPlan = migrationPlanJson != null && !migrationPlanJson.isBlank();
    if (overallCompat == Compatibility.BREAKING && !hasMigrationPlan) {
      throw new IllegalArgumentException(
          "VALIDATION_FAILED: BREAKING schema change on "
              + candidate.apiName()
              + " requires a non-empty migration_plan");
    }

    int nextVer = currentVer + 1;
    String ddl = generateObjectTableDdl(candidate);
    String ddlSha = sha256Hex(ddl);
    String defSha =
        sha256Hex(
            candidate.apiName()
                + "|"
                + nextVer
                + "|"
                + candidate.primaryKey()
                + "|"
                + candidate.properties().keySet());
    String versionRid =
        "ri.ontology."
            + tenant
            + ".type-version."
            + UUID.nameUUIDFromBytes(
                (candidate.apiName() + ":" + targetBranch + ":" + nextVer)
                    .getBytes(StandardCharsets.UTF_8));
    String typeRid = "ri.ontology." + tenant + ".object-type." + candidate.objectTypeId();

    TypeVersionRecord record =
        new TypeVersionRecord(
            versionRid,
            typeRid,
            candidate.apiName(),
            nextVer,
            overallCompat,
            defSha,
            targetBranch,
            publishedBy,
            Instant.now(),
            hasMigrationPlan,
            List.copyOf(diffs),
            ddl,
            ddlSha);

    if (!dryRun) {
      branchObjects.put(candidate.apiName(), candidate);
      branchVersions.put(candidate.apiName(), nextVer);
      typeVersionsByRid.put(versionRid, record);
    }
    return record;
  }

  public synchronized List<ObjectTypeDefinition> listObjectTypes(
      String branch, CallerContext caller) {
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    Map<String, ObjectTypeDefinition> branchObjects =
        objectTypesByBranch.getOrDefault(targetBranch, Map.of());
    List<ObjectTypeDefinition> visible = new ArrayList<>();
    for (ObjectTypeDefinition def : branchObjects.values()) {
      if (caller.heldMarkings().containsAll(def.defaultMarkings())
          && caller.clearance() >= def.defaultClassification()) {
        visible.add(def);
      }
    }
    return List.copyOf(visible);
  }

  public synchronized ObjectTypeView getObjectType(
      String branch, String apiName, CallerContext caller) {
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    ObjectTypeDefinition def =
        objectTypesByBranch.getOrDefault(targetBranch, Map.of()).get(apiName);
    if (def == null
        || !caller.heldMarkings().containsAll(def.defaultMarkings())
        || caller.clearance() < def.defaultClassification()) {
      throw new IllegalArgumentException("NOT_FOUND: ObjectType " + apiName);
    }
    int version =
        currentVersionsByBranch.getOrDefault(targetBranch, Map.of()).getOrDefault(apiName, 1);
    Map<String, Boolean> accessible = new LinkedHashMap<>();
    for (PropertyDefinition prop : def.properties().values()) {
      boolean canRead =
          caller.heldMarkings().containsAll(prop.columnMarkings())
              && caller.clearance() >= prop.columnClassification();
      accessible.put(prop.apiName(), canRead);
    }
    return new ObjectTypeView(def, version, Map.copyOf(accessible));
  }

  public synchronized TypeVersionRecord getTypeVersion(String versionRid) {
    TypeVersionRecord rec = typeVersionsByRid.get(versionRid);
    if (rec == null) {
      throw new IllegalArgumentException("NOT_FOUND: type version " + versionRid);
    }
    return rec;
  }

  public synchronized ActionTypeDefinition getActionType(
      String branch, String apiName, CallerContext caller) {
    String targetBranch = (branch == null || branch.isBlank()) ? "main" : branch;
    ActionTypeDefinition action = actionsByBranch.getOrDefault(targetBranch, Map.of()).get(apiName);
    if (action == null) {
      throw new IllegalArgumentException("NOT_FOUND: ActionType " + apiName);
    }
    boolean isEditor = caller.roles().contains("ontology-editor");
    if (isEditor) {
      return action;
    }
    List<SubmissionCriterion> redactedCriteria =
        action.submissionCriteria().stream()
            .map(c -> new SubmissionCriterion(c.name(), "", c.failureMessage()))
            .toList();
    return new ActionTypeDefinition(
        action.apiName(),
        action.displayName(),
        action.status(),
        action.outcomeClass(),
        action.requiresHumanApproval(),
        action.agentExecutable(),
        action.justificationRequired(),
        action.allowedPurposes(),
        action.hasWritebackWebhook(),
        action.webhookTimeoutMs(),
        action.maxObjects(),
        redactedCriteria);
  }

  /**
   * Generates the physical PostgreSQL 18 DDL for a typed object table (`objects.ot_<name>`), its
   * indexes, Warden RLS policies (`p_read`, `p_actions_*`, `p_indexer`, `p_traversal`), and its
   * `security_invoker` column-masking view (`objects_v.<name>`) (`DATA_MODEL.md` sections 4.3, 4.5,
   * 4.6).
   */
  public static String generateObjectTableDdl(ObjectTypeDefinition def) {
    String snake = toSnakeCase(def.apiName());
    String table = "objects.ot_" + snake;
    String view = "objects_v." + snake;
    StringBuilder sb = new StringBuilder();
    sb.append("-- Generated by Vigil OMS (P5) for ObjectType ").append(def.apiName()).append("\n");
    sb.append("CREATE SCHEMA IF NOT EXISTS objects;\n");
    sb.append("CREATE SCHEMA IF NOT EXISTS objects_v;\n");
    sb.append("CREATE TABLE IF NOT EXISTS ").append(table).append(" (\n");
    sb.append("  pk text NOT NULL,\n");
    sb.append("  rid uuid NOT NULL GENERATED ALWAYS AS (vigil_sec.object_rid('")
        .append(def.objectTypeId())
        .append("'::uuid, pk)) STORED,\n");

    for (PropertyDefinition prop : def.properties().values()) {
      sb.append("  ")
          .append(prop.apiName())
          .append(" ")
          .append(sqlTypeFor(prop.baseType(), prop.isArray()));
      if (!prop.nullable()) {
        sb.append(" NOT NULL");
      }
      sb.append(",\n");
    }

    sb.append("  markings text[] NOT NULL DEFAULT '{}',\n");
    sb.append(
        "  classification smallint NOT NULL DEFAULT 0 CHECK (classification BETWEEN 0 AND 4),\n");
    sb.append("  releasability text[] NOT NULL DEFAULT '{}',\n");
    sb.append("  purpose_tags text[] NOT NULL DEFAULT '{}',\n");
    sb.append("  source_id text NOT NULL DEFAULT 'oms',\n");
    sb.append("  source_record_id text,\n");
    sb.append("  connector_version text,\n");
    sb.append("  fetched_at timestamptz,\n");
    sb.append("  terms_ref text,\n");
    sb.append("  responsible_party text,\n");
    sb.append("  lawful_basis text,\n");
    sb.append(
        "  data_origin text NOT NULL DEFAULT 'SYNTHETIC' CHECK (data_origin IN ('REAL','PSEUDONYMISED','SYNTHETIC')),\n");
    sb.append("  dataset_rid text,\n");
    sb.append("  dataset_txn_id text,\n");
    sb.append("  content_sha256 bytea,\n");
    sb.append("  edited_props jsonb NOT NULL DEFAULT '{}'::jsonb,\n");
    sb.append("  last_edit_seq bigint,\n");
    sb.append("  valid_from timestamptz NOT NULL DEFAULT now(),\n");
    sb.append("  updated_at timestamptz NOT NULL DEFAULT now(),\n");
    sb.append("  version bigint NOT NULL DEFAULT 1,\n");
    sb.append("  deleted_at timestamptz,\n");
    sb.append("  PRIMARY KEY (pk)\n");
    sb.append(");\n");

    sb.append("CREATE INDEX IF NOT EXISTS ot_")
        .append(snake)
        .append("_rid_idx ON ")
        .append(table)
        .append(" (rid);\n");
    sb.append("CREATE INDEX IF NOT EXISTS ot_")
        .append(snake)
        .append("_upd_brin ON ")
        .append(table)
        .append(" USING brin (updated_at);\n");

    sb.append("ALTER TABLE ").append(table).append(" ENABLE ROW LEVEL SECURITY;\n");
    sb.append("ALTER TABLE ").append(table).append(" FORCE ROW LEVEL SECURITY;\n");
    sb.append("DROP POLICY IF EXISTS p_read ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_read ON ")
        .append(table)
        .append(" FOR SELECT TO vigil_reader\n")
        .append("  USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL\n")
        .append("     AND markings <@ (SELECT vigil_sec.ctx_markings())\n")
        .append("     AND classification <= (SELECT vigil_sec.ctx_clearance())\n")
        .append(
            "     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))\n")
        .append(
            "     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));\n");
    sb.append("DROP POLICY IF EXISTS p_actions_read ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_actions_read ON ")
        .append(table)
        .append(" FOR SELECT TO vigil_actions\n")
        .append("  USING ((SELECT vigil_sec.ctx_ok())\n")
        .append("     AND markings <@ (SELECT vigil_sec.ctx_markings())\n")
        .append("     AND classification <= (SELECT vigil_sec.ctx_clearance())\n")
        .append(
            "     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))\n")
        .append(
            "     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));\n");
    sb.append("DROP POLICY IF EXISTS p_actions_update ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_actions_update ON ")
        .append(table)
        .append(" FOR UPDATE TO vigil_actions\n")
        .append(
            "  USING ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n")
        .append("     AND classification <= (SELECT vigil_sec.ctx_clearance()))\n")
        .append(
            "  WITH CHECK ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n")
        .append("     AND classification <= (SELECT vigil_sec.ctx_clearance())\n")
        .append(
            "     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability())));\n");
    sb.append("DROP POLICY IF EXISTS p_actions_insert ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_actions_insert ON ")
        .append(table)
        .append(" FOR INSERT TO vigil_actions\n")
        .append(
            "  WITH CHECK ((SELECT vigil_sec.ctx_ok()) AND markings <@ (SELECT vigil_sec.ctx_markings())\n")
        .append("     AND classification <= (SELECT vigil_sec.ctx_clearance())\n")
        .append(
            "     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability())));\n");
    sb.append("DROP POLICY IF EXISTS p_indexer ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_indexer ON ")
        .append(table)
        .append(" FOR ALL TO vigil_indexer USING (true) WITH CHECK (true);\n");
    sb.append("DROP POLICY IF EXISTS p_traversal ON ").append(table).append(";\n");
    sb.append("CREATE POLICY p_traversal ON ")
        .append(table)
        .append(" FOR SELECT TO vigil_traversal USING (true);\n");
    sb.append(
        "GRANT USAGE ON SCHEMA objects, objects_v TO vigil_reader, vigil_actions, vigil_indexer, vigil_traversal;\n");
    sb.append("GRANT SELECT ON ")
        .append(table)
        .append(" TO vigil_reader, vigil_actions, vigil_indexer;\n");
    sb.append("GRANT INSERT, UPDATE ON ")
        .append(table)
        .append(" TO vigil_actions, vigil_indexer;\n");
    sb.append(
            "GRANT SELECT (pk, rid, markings, classification, releasability, purpose_tags, deleted_at) ON ")
        .append(table)
        .append(" TO vigil_traversal;\n");

    // Column-masking view (DATA_MODEL section 4.6)
    sb.append("CREATE OR REPLACE VIEW ")
        .append(view)
        .append(" WITH (security_barrier = true, security_invoker = true) AS\nSELECT t.pk, t.rid");
    List<String> redactChecks = new ArrayList<>();
    for (PropertyDefinition prop : def.properties().values()) {
      if (prop.columnMarkings().isEmpty()) {
        sb.append(", t.").append(prop.apiName());
      } else {
        String arrayLiteral = "'{" + String.join(",", prop.columnMarkings()) + "}'::text[]";
        sb.append(", CASE WHEN ")
            .append(arrayLiteral)
            .append(" <@ (SELECT vigil_sec.ctx_markings()) THEN t.")
            .append(prop.apiName())
            .append(" END AS ")
            .append(prop.apiName());
        redactChecks.add(
            "CASE WHEN NOT ("
                + arrayLiteral
                + " <@ (SELECT vigil_sec.ctx_markings())) THEN '"
                + prop.apiName()
                + "' END");
      }
    }
    if (redactChecks.isEmpty()) {
      sb.append(", '{}'::text[] AS redacted_properties");
    } else {
      sb.append(", array_remove(ARRAY[")
          .append(String.join(", ", redactChecks))
          .append("], NULL) AS redacted_properties");
    }
    sb.append(
            ", t.markings, t.classification, t.releasability, t.purpose_tags, t.updated_at, t.version FROM ")
        .append(table)
        .append(" t;\n");
    sb.append("GRANT SELECT ON ").append(view).append(" TO vigil_reader;\n");
    return sb.toString();
  }

  /**
   * Generates the physical PostgreSQL 18 DDL and RLS policies for a `MANY_TO_MANY` link join table
   * (`DATA_MODEL.md` section 4.7).
   */
  public static String generateJoinTableDdl(LinkTypeDefinition link) {
    if (!"MANY_TO_MANY".equals(link.cardinality())) {
      return "";
    }
    String table = "objects." + link.joinTable();
    return "-- Generated by Vigil OMS (P5) for LinkType "
        + link.apiName()
        + "\n"
        + "CREATE TABLE IF NOT EXISTS "
        + table
        + " (\n"
        + "  a_pk text NOT NULL,\n"
        + "  b_pk text NOT NULL,\n"
        + "  markings text[] NOT NULL DEFAULT '{}',\n"
        + "  classification smallint NOT NULL DEFAULT 0 CHECK (classification BETWEEN 0 AND 4),\n"
        + "  releasability text[] NOT NULL DEFAULT '{}',\n"
        + "  purpose_tags text[] NOT NULL DEFAULT '{}',\n"
        + "  source_id text NOT NULL DEFAULT 'oms',\n"
        + "  dataset_rid text,\n"
        + "  dataset_txn_id text,\n"
        + "  fetched_at timestamptz,\n"
        + "  content_sha256 bytea,\n"
        + "  created_by_edit bigint,\n"
        + "  valid_from timestamptz NOT NULL DEFAULT now(),\n"
        + "  updated_at timestamptz NOT NULL DEFAULT now(),\n"
        + "  deleted_at timestamptz,\n"
        + "  PRIMARY KEY (a_pk, b_pk)\n"
        + ");\n"
        + "CREATE INDEX IF NOT EXISTS "
        + link.joinTable()
        + "_b_idx ON "
        + table
        + " (b_pk, a_pk) WHERE deleted_at IS NULL;\n"
        + "ALTER TABLE "
        + table
        + " ENABLE ROW LEVEL SECURITY;\n"
        + "ALTER TABLE "
        + table
        + " FORCE ROW LEVEL SECURITY;\n"
        + "DROP POLICY IF EXISTS p_read ON "
        + table
        + ";\n"
        + "CREATE POLICY p_read ON "
        + table
        + " FOR SELECT TO vigil_reader\n"
        + "  USING ((SELECT vigil_sec.ctx_ok()) AND deleted_at IS NULL\n"
        + "     AND markings <@ (SELECT vigil_sec.ctx_markings())\n"
        + "     AND classification <= (SELECT vigil_sec.ctx_clearance())\n"
        + "     AND (releasability = '{}' OR releasability && (SELECT vigil_sec.ctx_releasability()))\n"
        + "     AND (purpose_tags = '{}' OR (SELECT vigil_sec.ctx_purpose()) = ANY (purpose_tags)));\n"
        + "DROP POLICY IF EXISTS p_indexer ON "
        + table
        + ";\n"
        + "CREATE POLICY p_indexer ON "
        + table
        + " FOR ALL TO vigil_indexer USING (true) WITH CHECK (true);\n"
        + "DROP POLICY IF EXISTS p_traversal ON "
        + table
        + ";\n"
        + "CREATE POLICY p_traversal ON "
        + table
        + " FOR SELECT TO vigil_traversal USING (true);\n"
        + "GRANT SELECT ON "
        + table
        + " TO vigil_reader, vigil_indexer, vigil_traversal;\n"
        + "GRANT INSERT, UPDATE ON "
        + table
        + " TO vigil_indexer;\n";
  }

  public static String generateOpenSearchMapping(
      String tenant, ObjectTypeDefinition def, int version) {
    String indexName = "vt-" + tenant + "-" + toSnakeCase(def.apiName()) + "-v" + version;
    List<String> searchableFields =
        def.properties().values().stream()
            .filter(PropertyDefinition::searchable)
            .map(p -> "\"" + p.apiName() + "\":{\"type\":\"text\",\"analyzer\":\"vigil_org_name\"}")
            .toList();
    String extraProps = searchableFields.isEmpty() ? "" : "," + String.join(",", searchableFields);
    return "{\"index\":\""
        + indexName
        + "\",\"mappings\":{\"dynamic\":\"strict\",\"properties\":{\"vigil_pk\":{\"type\":\"keyword\"},\"vigil_rid\":{\"type\":\"keyword\"},\"markings\":{\"type\":\"keyword\"},\"markings_count\":{\"type\":\"integer\"},\"classification\":{\"type\":\"byte\"},\"releasability\":{\"type\":\"keyword\"},\"purpose_tags\":{\"type\":\"keyword\"}"
        + extraProps
        + "}}}";
  }

  /**
   * Loads an ontology pack directory (such as `packs/_core`) into the target branch, registering
   * its interfaces, object types, and link types, and returning the concatenated PostgreSQL 18
   * physical DDL for all loaded types and M:N links (P5 task 5.4 and "Done when").
   */
  public synchronized String loadPackDirectory(Path packDir, String branch, String publishedBy)
      throws IOException {
    Path manifest = packDir.resolve("pack.yaml");
    if (!Files.isRegularFile(manifest)) {
      throw new IllegalArgumentException("Missing pack.yaml in " + packDir);
    }
    String manifestText = Files.readString(manifest, StandardCharsets.UTF_8);
    if (!manifestText.contains("api_version: vigil.ontology/v1")) {
      throw new IllegalArgumentException("Invalid api_version in " + manifest);
    }
    String packName = extractYamlScalar(manifestText, "api_name", "_core");
    String packVersion = extractYamlScalar(manifestText, "version", "1.0.0");

    Path interfacesDir = packDir.resolve("interfaces");
    if (Files.isDirectory(interfacesDir)) {
      try (Stream<Path> files = Files.list(interfacesDir)) {
        for (Path file : files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
          String text = Files.readString(file, StandardCharsets.UTF_8);
          String ifaceName = extractYamlScalar(text, "api_name", "Party");
          registerInterface(
              branch, new InterfaceTypeDefinition(ifaceName, Map.of("display_name", "string")));
        }
      }
    }

    StringBuilder fullDdl = new StringBuilder();
    Path objectsDir = packDir.resolve("object-types");
    if (Files.isDirectory(objectsDir)) {
      try (Stream<Path> files = Files.list(objectsDir)) {
        for (Path file : files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
          ObjectTypeDefinition objDef = parseObjectTypeYaml(file, packName, packVersion);
          TypeVersionRecord ver = publishObjectType(branch, objDef, 0, null, false, publishedBy);
          fullDdl.append(ver.physicalDdl()).append("\n");
        }
      }
    }

    Path linksDir = packDir.resolve("link-types");
    if (Files.isDirectory(linksDir)) {
      try (Stream<Path> files = Files.list(linksDir)) {
        for (Path file : files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
          LinkTypeDefinition linkDef = parseLinkTypeYaml(file);
          registerLinkType(branch, linkDef);
          String linkDdl = generateJoinTableDdl(linkDef);
          if (!linkDdl.isBlank()) {
            fullDdl.append(linkDdl).append("\n");
          }
        }
      }
    }
    return fullDdl.toString();
  }

  private static ObjectTypeDefinition parseObjectTypeYaml(
      Path yamlFile, String packName, String packVersion) throws IOException {
    List<String> lines = Files.readAllLines(yamlFile, StandardCharsets.UTF_8);
    String fullText = String.join("\n", lines);
    if (!fullText.contains("api_version: vigil.ontology/v1")
        || !fullText.contains("kind: ObjectType")) {
      throw new IllegalArgumentException("Invalid ObjectType YAML: " + yamlFile);
    }
    String apiName = extractYamlScalar(fullText, "api_name", "");
    String displayName = extractYamlScalar(fullText, "display_name", apiName);
    String status = extractYamlScalar(fullText, "status", "ACTIVE");
    String pk = extractYamlScalar(fullText, "primary_key", "id");
    String title = extractYamlScalar(fullText, "title_property", pk);
    String existence = extractYamlScalar(fullText, "existence_policy", "SOURCE_WINS");
    String className = extractYamlScalar(fullText, "classification", "PUBLIC");
    int classification = parseClassificationLevel(className);
    List<String> implementsList = extractInlineList(fullText, "implements:");
    List<String> defaultMarkings = extractInlineList(fullText, "markings:");

    Map<String, PropertyDefinition> props = new LinkedHashMap<>();
    boolean inProps = false;
    for (String rawLine : lines) {
      String trimmed = rawLine.trim();
      if (trimmed.startsWith("#") || trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.equals("properties:")) {
        inProps = true;
        continue;
      }
      if (inProps && rawLine.startsWith("    ") && trimmed.contains(": {")) {
        int colon = trimmed.indexOf(':');
        String propName = trimmed.substring(0, colon).trim();
        String body = trimmed.substring(colon + 1).trim();
        String baseType = extractInlineField(body, "base_type", "string");
        boolean isArray = "true".equalsIgnoreCase(extractInlineField(body, "is_array", "false"));
        boolean required = "true".equalsIgnoreCase(extractInlineField(body, "required", "false"));
        boolean editable = "true".equalsIgnoreCase(extractInlineField(body, "editable", "false"));
        String conflict =
            editable ? extractInlineField(body, "conflict_policy", "EDIT_WINS") : null;
        boolean searchable =
            "true".equalsIgnoreCase(extractInlineField(body, "searchable", "false"));
        List<String> colMarkings = extractInlineList(body, "column_markings:");
        props.put(
            propName,
            new PropertyDefinition(
                propName,
                baseType,
                isArray,
                !required,
                editable,
                conflict,
                searchable,
                colMarkings,
                0,
                null,
                null));
      }
    }

    UUID typeUuid =
        UUID.nameUUIDFromBytes(
            ("vigil.ontology.object_type:" + apiName).getBytes(StandardCharsets.UTF_8));
    return new ObjectTypeDefinition(
        typeUuid,
        apiName,
        displayName,
        status,
        pk,
        title,
        existence,
        implementsList,
        defaultMarkings,
        classification,
        packName,
        packVersion,
        props);
  }

  private static LinkTypeDefinition parseLinkTypeYaml(Path yamlFile) throws IOException {
    String text = Files.readString(yamlFile, StandardCharsets.UTF_8);
    String apiName = extractYamlScalar(text, "api_name", "");
    String inverse = extractYamlScalar(text, "inverse_api_name", apiName + "_inv");
    String cardinality = extractYamlScalar(text, "cardinality", "MANY_TO_ONE");
    String aLine = extractLineStartingWith(text, "a:");
    String bLine = extractLineStartingWith(text, "b:");
    String aType = extractInlineField(aLine, "object_type", "");
    String fkProp = extractInlineField(aLine, "fk_property", null);
    String bType = extractInlineField(bLine, "object_type", "");
    String joinTable = extractYamlScalar(text, "join_table", null);
    return new LinkTypeDefinition(
        apiName, inverse, cardinality, aType, bType, fkProp, joinTable, List.of());
  }

  private static String extractYamlScalar(String text, String key, String defaultValue) {
    for (String line : text.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith(key + ":")) {
        String val = trimmed.substring((key + ":").length()).trim();
        if (!val.startsWith("{") && !val.startsWith("[")) {
          return val;
        }
      }
    }
    return defaultValue;
  }

  private static String extractLineStartingWith(String text, String prefix) {
    for (String line : text.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith(prefix)) {
        return trimmed;
      }
    }
    return "";
  }

  private static String extractInlineField(String inlineBlock, String key, String defaultValue) {
    int idx = inlineBlock.indexOf(key + ":");
    if (idx < 0) {
      return defaultValue;
    }
    String after = inlineBlock.substring(idx + key.length() + 1).trim();
    int end = after.length();
    for (int i = 0; i < after.length(); i++) {
      char c = after.charAt(i);
      if (c == ',' || c == '}') {
        end = i;
        break;
      }
    }
    return after.substring(0, end).trim();
  }

  private static List<String> extractInlineList(String text, String marker) {
    int idx = text.indexOf(marker);
    if (idx < 0) {
      return List.of();
    }
    int open = text.indexOf('[', idx);
    int close = text.indexOf(']', open);
    if (open < 0 || close < 0) {
      return List.of();
    }
    String inside = text.substring(open + 1, close).trim();
    if (inside.isEmpty()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (String item : inside.split(",")) {
      String cleaned = item.trim();
      if (!cleaned.isEmpty()) {
        out.add(cleaned);
      }
    }
    return out;
  }

  private static int parseClassificationLevel(String name) {
    return switch (name.toUpperCase(Locale.ROOT)) {
      case "PUBLIC" -> 0;
      case "RESTRICTED" -> 1;
      case "CONFIDENTIAL" -> 2;
      case "SECRET" -> 3;
      case "TOP_SECRET" -> 4;
      default -> 0;
    };
  }

  private static String sqlTypeFor(String baseType, boolean isArray) {
    String scalar =
        switch (baseType) {
          case "boolean" -> "boolean";
          case "integer" -> "integer";
          case "long" -> "bigint";
          case "double" -> "double precision";
          case "decimal" -> "numeric(20,4)";
          case "date" -> "date";
          case "timestamp" -> "timestamptz";
          case "struct" -> "jsonb";
          case "vector" -> "real[]";
          default -> "text";
        };
    return isArray ? scalar + "[]" : scalar;
  }

  static String toSnakeCase(String pascalCase) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pascalCase.length(); i++) {
      char c = pascalCase.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          sb.append('_');
        }
        sb.append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String sha256Hex(String text) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
