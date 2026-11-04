// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Emit a canonical EventEnvelope wire payload for the P2 Java -> Kafka -> Python -> Rust round-trip check
package africa.vigil.contracts.codegen;

import africa.vigil.contracts.common.v1.ResourceId;
import africa.vigil.contracts.events.v1.EventEnvelope;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import java.util.Base64;

/**
 * Constructs a sample {@link EventEnvelope} carrying a serialized {@link ResourceId} payload and
 * writes the deterministic Base64-encoded Protobuf wire bytes to standard output.
 */
public final class RoundTripMain {
  private RoundTripMain() {}

  public static void main(String[] args) throws Exception {
    ResourceId payload =
        ResourceId.newBuilder()
            .setValue("ri.ontology.demo.type-version.01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b")
            .build();

    EventEnvelope envelope =
        EventEnvelope.newBuilder()
            .setSpecVersion("1.0")
            .setId("01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b")
            .setSource("//vigil/za-gp-1/demo/core-oms")
            .setType("vigil.ontology.type.published.v1")
            .setSubject("ri.ontology.demo.object-type.01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f")
            .setTime(Timestamp.newBuilder().setSeconds(1790330531L).setNanos(4_000_000).build())
            .setDataContentType("application/protobuf")
            .setDataSchema("urn:vigil:proto:vigil.common.v1.ResourceId")
            .setData(
                Any.newBuilder()
                    .setTypeUrl("type.googleapis.com/vigil.common.v1.ResourceId")
                    .setValue(payload.toByteString())
                    .build())
            .setVigilTenant("demo")
            .setVigilClass(0)
            .addVigilMarkings("PII")
            .setVigilWatermark("wm1_0000000001f4a2_0000000000016018")
            .setPartitionKey("Supplier")
            .setTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
            .build();

    byte[] wireBytes = envelope.toByteArray();
    EventEnvelope reparsed = EventEnvelope.parseFrom(wireBytes);
    if (!reparsed.getId().equals(envelope.getId())) {
      throw new IllegalStateException("Java self-parse mismatch");
    }
    System.out.println(Base64.getEncoder().encodeToString(wireBytes));
  }
}
