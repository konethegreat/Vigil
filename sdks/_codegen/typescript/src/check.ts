// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Type-check generated TypeScript contract stubs (P2 task 2.5)
import { create } from "@bufbuild/protobuf";
import { ResourceIdSchema } from "./gen/vigil/common/v1/rid_pb.js";
import { Classification, PolicyContextSchema } from "./gen/vigil/common/v1/security_pb.js";
import { PageRequestSchema, PageInfoSchema } from "./gen/vigil/common/v1/page_pb.js";
import { ErrorCode, VigilErrorSchema } from "./gen/vigil/common/v1/error_pb.js";
import { WatermarkSchema } from "./gen/vigil/common/v1/watermark_pb.js";
import { EventEnvelopeSchema } from "./gen/vigil/events/v1/envelope_pb.js";

const rid = create(ResourceIdSchema, {
  value: "ri.objects.demo.object.Supplier.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c",
});
const ctx = create(PolicyContextSchema, {
  principal: "user:demo",
  tenant: "demo",
  clearance: Classification.PUBLIC,
});
const pageReq = create(PageRequestSchema, { pageSize: 100 });
const pageInfo = create(PageInfoSchema, { nextPageToken: "", totalEstimate: 10n });
const err = create(VigilErrorSchema, {
  error: ErrorCode.NOT_FOUND,
  statusCode: 404,
  message: "Resource not found.",
});
const wm = create(WatermarkSchema, { value: "wm1_0000000001f4a2_0000000000016018" });
const env = create(EventEnvelopeSchema, {
  specVersion: "1.0",
  subject: rid.value,
  vigilTenant: ctx.tenant,
  vigilClass: 0,
  vigilWatermark: wm.value,
});

export { rid, ctx, pageReq, pageInfo, err, wm, env };
