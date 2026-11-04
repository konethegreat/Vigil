// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Unit and acceptance tests for Core
// Ontology Metadata Service (OMS): pack loader, versioning, breaking-change gate, and
// PostgreSQL/OpenSearch DDL generation (P5 tasks 5.1-5.5)
package vigil.core.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vigil.core.ontology.OntologyMetadataService.ActionTypeDefinition;
import vigil.core.ontology.OntologyMetadataService.CallerContext;
import vigil.core.ontology.OntologyMetadataService.Compatibility;
import vigil.core.ontology.OntologyMetadataService.ObjectTypeDefinition;
import vigil.core.ontology.OntologyMetadataService.ObjectTypeView;
import vigil.core.ontology.OntologyMetadataService.PropertyDefinition;
import vigil.core.ontology.OntologyMetadataService.SubmissionCriterion;
import vigil.core.ontology.OntologyMetadataService.TypeVersionRecord;

class OntologyMetadataServiceTest {

  @Test
  void loadsCorePackGeneratesTypedTablesAndEnforcesVisibilityAndVersioning() throws IOException {
    OntologyMetadataService oms = new OntologyMetadataService("demo");
    String generatedDdl = oms.loadPackDirectory(Path.of("packs/_core"), "main", "user:architect");

    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_organisation"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_person"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_place"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_asset"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_event"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_document"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.ot_identifier"));
    assertTrue(generatedDdl.contains("CREATE TABLE IF NOT EXISTS objects.lt_event_involves"));
    assertTrue(generatedDdl.contains("ENABLE ROW LEVEL SECURITY"));
    assertTrue(generatedDdl.contains("FORCE ROW LEVEL SECURITY"));
    assertTrue(generatedDdl.contains("CREATE OR REPLACE VIEW objects_v.person"));

    // Public caller with clearance 0 sees only PUBLIC types without markings (5 of the 7 _core
    // types: Organisation, Place, Event, Document; Asset is RESTRICTED(1), Person is
    // CONFIDENTIAL+PII,
    // Identifier is RESTRICTED+PII)
    CallerContext publicCaller = new CallerContext("user:pub", Set.of(), 0, Set.of());
    List<ObjectTypeDefinition> publicVisible = oms.listObjectTypes("main", publicCaller);
    assertEquals(4, publicVisible.size());

    // Cleared caller with PII and clearance 2 sees all 7 _core object types
    CallerContext clearedCaller =
        new CallerContext("user:analyst", Set.of("PII"), 2, Set.of("ontology-editor"));
    List<ObjectTypeDefinition> allVisible = oms.listObjectTypes("main", clearedCaller);
    assertEquals(7, allVisible.size());

    // Asset has security_controls marked [SECURITY-MEASURE]: accessible is false for caller
    // without SECURITY-MEASURE
    ObjectTypeView assetView = oms.getObjectType("main", "Asset", clearedCaller);
    assertEquals(1, assetView.version());
    assertFalse(assetView.propertyAccessible().get("security_controls"));
    assertTrue(assetView.propertyAccessible().get("name"));

    String osMapping =
        OntologyMetadataService.generateOpenSearchMapping("demo", assetView.definition(), 1);
    assertTrue(osMapping.contains("\"index\":\"vt-demo-asset-v1\""));
    assertTrue(osMapping.contains("\"dynamic\":\"strict\""));
  }

  @Test
  void rejectsBreakingChangesWithoutMigrationPlanAndForbidsLabelDowngrade() {
    OntologyMetadataService oms = new OntologyMetadataService("demo");
    UUID supplierUuid = UUID.fromString("01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f");

    Map<String, PropertyDefinition> v1Props = new LinkedHashMap<>();
    v1Props.put(
        "supplier_key",
        new PropertyDefinition(
            "supplier_key", "string", false, false, false, null, false, List.of(), 0, null, null));
    v1Props.put(
        "name_raw",
        new PropertyDefinition(
            "name_raw", "string", false, false, false, null, true, List.of(), 0, null, null));
    v1Props.put(
        "contact_phone",
        new PropertyDefinition(
            "contact_phone",
            "cipher_text",
            false,
            true,
            false,
            null,
            false,
            List.of("PII"),
            1,
            null,
            null));

    ObjectTypeDefinition v1 =
        new ObjectTypeDefinition(
            supplierUuid,
            "Supplier",
            "Supplier",
            "ACTIVE",
            "supplier_key",
            "name_raw",
            "SOURCE_WINS",
            List.of(),
            List.of("PII"),
            1,
            "integrity",
            "1.0.0",
            v1Props);

    TypeVersionRecord rec1 = oms.publishObjectType("main", v1, 0, null, false, "user:editor");
    assertEquals(1, rec1.version());
    assertEquals(Compatibility.INITIAL, rec1.compatibility());
    assertNotNull(oms.getTypeVersion(rec1.versionRid()));

    // Compatible v2: add optional vat_number property
    Map<String, PropertyDefinition> v2Props = new LinkedHashMap<>(v1Props);
    v2Props.put(
        "vat_number",
        new PropertyDefinition(
            "vat_number", "string", false, true, false, null, false, List.of(), 0, null, null));
    ObjectTypeDefinition v2 =
        new ObjectTypeDefinition(
            supplierUuid,
            "Supplier",
            "Supplier",
            "ACTIVE",
            "supplier_key",
            "name_raw",
            "SOURCE_WINS",
            List.of(),
            List.of("PII"),
            1,
            "integrity",
            "1.0.0",
            v2Props);

    TypeVersionRecord rec2 = oms.publishObjectType("main", v2, 1, null, false, "user:editor");
    assertEquals(2, rec2.version());
    assertEquals(Compatibility.COMPATIBLE, rec2.compatibility());

    // Breaking v3 (dropping vat_number) without migrationPlanJson is rejected
    assertThrows(
        IllegalArgumentException.class,
        () -> oms.publishObjectType("main", v1, 2, null, false, "user:editor"));

    // Breaking v3 WITH migrationPlanJson succeeds
    TypeVersionRecord rec3 =
        oms.publishObjectType(
            "main",
            v1,
            2,
            "{\"backfill_transform\":\"drop_vat_number\",\"reindex\":true}",
            false,
            "user:editor");
    assertEquals(3, rec3.version());
    assertEquals(Compatibility.BREAKING, rec3.compatibility());
    assertTrue(rec3.hasMigrationPlan());

    // Attempting to lower classification or drop PII marking is rejected with SecurityException
    // (W8)
    ObjectTypeDefinition downgraded =
        new ObjectTypeDefinition(
            supplierUuid,
            "Supplier",
            "Supplier",
            "ACTIVE",
            "supplier_key",
            "name_raw",
            "SOURCE_WINS",
            List.of(),
            List.of(),
            0,
            "integrity",
            "1.0.0",
            v1Props);
    assertThrows(
        SecurityException.class,
        () -> oms.publishObjectType("main", downgraded, 3, "{}", false, "user:editor"));
  }

  @Test
  void validatesActionTypePopiaS71AndPropertyConstraints() {
    OntologyMetadataService oms = new OntologyMetadataService("demo");

    // POPIA s71: ADVERSE action without human approval is rejected
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ActionTypeDefinition(
                "refer-supplier",
                "Refer Supplier",
                "ACTIVE",
                "ADVERSE",
                false,
                true,
                true,
                List.of("PROCUREMENT_INTEGRITY"),
                false,
                null,
                3,
                List.of()));

    ActionTypeDefinition validAction =
        new ActionTypeDefinition(
            "refer-supplier",
            "Refer Supplier",
            "ACTIVE",
            "ADVERSE",
            true,
            true,
            true,
            List.of("PROCUREMENT_INTEGRITY"),
            true,
            5000,
            3,
            List.of(
                new SubmissionCriterion(
                    "not_referred",
                    "params.supplier.review_status != 'REFERRED'",
                    "Already referred")));
    oms.registerActionType("main", validAction);

    CallerContext nonEditor = new CallerContext("user:viewer", Set.of(), 1, Set.of());
    ActionTypeDefinition forViewer = oms.getActionType("main", "refer-supplier", nonEditor);
    assertEquals("", forViewer.submissionCriteria().get(0).condition());

    CallerContext editor = new CallerContext("user:ed", Set.of(), 1, Set.of("ontology-editor"));
    ActionTypeDefinition forEditor = oms.getActionType("main", "refer-supplier", editor);
    assertFalse(forEditor.submissionCriteria().get(0).condition().isEmpty());

    // Reserved column name and editable without conflict_policy are rejected
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PropertyDefinition(
                "markings", "string", false, true, false, null, false, List.of(), 0, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PropertyDefinition(
                "status", "string", false, true, true, null, false, List.of(), 0, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PropertyDefinition(
                "emb", "vector", false, true, false, null, false, List.of(), 0, null, null));
  }
}
