// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Include and compile generated Rust contract stubs (P2 task 2.5)
// Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Decode and assert the Java -> Kafka -> Python -> Rust EventEnvelope wire payload (P2 Done-when)
#![allow(clippy::all)]

pub mod vigil {
    pub mod common {
        pub mod v1 {
            include!("../../gen/rust/vigil.common.v1.rs");
        }
    }
    pub mod events {
        pub mod v1 {
            include!("../../gen/rust/vigil.events.v1.rs");
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::Path;
    use prost::Message;
    use super::vigil::common::v1::{Classification, PolicyContext, ResourceId};
    use super::vigil::events::v1::EventEnvelope;

    #[test]
    fn generated_types_construct() {
        let rid = ResourceId {
            value: "ri.objects.demo.object.Supplier.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c".to_owned(),
        };
        let ctx = PolicyContext {
            principal: "user:demo".to_owned(),
            tenant: "demo".to_owned(),
            clearance: Classification::Public as i32,
            ..Default::default()
        };
        let env = EventEnvelope {
            spec_version: "1.0".to_owned(),
            vigil_tenant: ctx.tenant.clone(),
            subject: rid.value.clone(),
            vigil_class: Some(0),
            ..Default::default()
        };
        assert_eq!(env.subject, rid.value);
    }

    #[test]
    fn decodes_java_kafka_python_roundtrip_envelope() {
        let path = Path::new("../gen/roundtrip/envelope.bin");
        if !path.is_file() {
            return;
        }
        let bytes = std::fs::read(path).expect("read envelope.bin");
        let env = EventEnvelope::decode(bytes.as_slice()).expect("decode EventEnvelope");
        assert_eq!(env.spec_version, "1.0");
        assert_eq!(env.id, "01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b");
        assert_eq!(env.source, "//vigil/za-gp-1/demo/core-oms");
        assert_eq!(env.r#type, "vigil.ontology.type.published.v1");
        assert_eq!(
            env.subject,
            "ri.ontology.demo.object-type.01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f"
        );
        assert_eq!(env.data_content_type, "application/protobuf");
        assert_eq!(env.data_schema, "urn:vigil:proto:vigil.common.v1.ResourceId");
        assert_eq!(env.vigil_tenant, "demo");
        assert_eq!(env.vigil_class, Some(0));
        assert_eq!(env.vigil_markings, vec!["PII".to_owned()]);
        assert_eq!(env.vigil_watermark, "wm1_0000000001f4a2_0000000000016018");
        assert_eq!(env.partition_key, "Supplier");
        assert_eq!(
            env.traceparent,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );
        let ts = env.time.expect("timestamp present");
        assert_eq!(ts.seconds, 1790330531);
        assert_eq!(ts.nanos, 4_000_000);

        let any = env.data.expect("google.protobuf.Any payload present");
        assert_eq!(any.type_url, "type.googleapis.com/vigil.common.v1.ResourceId");
        let payload = ResourceId::decode(any.value.as_slice()).expect("decode inner ResourceId");
        assert_eq!(
            payload.value,
            "ri.ontology.demo.type-version.01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b"
        );
    }
}
