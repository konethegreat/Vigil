// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Include and compile generated Rust contract stubs (P2 task 2.5)
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
}
