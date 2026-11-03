<!-- Written by Kone Tshivhinda (Qoder) on 2026-11-03 — AsyncAPI event catalogue conventions (P2 task 2.3) -->
# Vigil event catalogue

`vigil-events.yaml` is the AsyncAPI 3 document for the events in
[API_DESIGN Part C](../API_DESIGN.md#part-c--event-catalogue-cloudevents-10).
The human registry stays authoritative: when this file and Part C disagree,
Part C wins and this file is corrected.

## Envelope

- The envelope is `vigil.events.v1.EventEnvelope` in
  [proto/vigil/events/v1/envelope.proto](../../proto/vigil/events/v1/envelope.proto).
- Kafka uses CloudEvents binary mode. Attributes travel in `ce-*` headers and
  the record value is the domain protobuf payload. Structured mode
  (`application/json`) is allowed only for debugging taps.
- Payloads are thin: RIDs, primary keys, counts and property names, never
  property values. Kafka therefore holds no personal information.
- Topics are `<tenant>.<domain>.<stream>.v1`. Retention is 7 days with no
  compaction. ACLs are per tenant and service.

## Adding a channel

Owners append their channels here when their phase first publishes an event.
A change needs, in the same PR:

1. the event row in API_DESIGN Part C (the source of truth);
2. the payload message in the domain protobuf package;
3. one message in `components.messages`, one channel, and one send operation;
4. a green `asyncapi validate`.

Validate locally with the pinned CLI image:

```sh
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/work" -w /work asyncapi/cli validate docs/asyncapi/vigil-events.yaml
```
