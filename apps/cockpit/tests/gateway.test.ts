// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Browser-to-core security boundary
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation gateway coverage
import { afterEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";
vi.mock("server-only", () => ({}));
import { GET, POST } from "../src/app/api/vigil/[...path]/route";
const context = (path: string) => ({
  params: Promise.resolve({ path: path.split("/") }),
});
function request(
  path: string,
  body?: unknown,
  extra: Record<string, string> = {},
) {
  return new NextRequest(`http://127.0.0.1:3100/api/vigil/${path}`, {
    method: body ? "POST" : "GET",
    headers: {
      host: "127.0.0.1:3100",
      origin: "http://127.0.0.1:3100",
      "content-type": "application/json",
      ...extra,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
}
afterEach(() => vi.unstubAllGlobals());
describe("cockpit gateway", () => {
  it("forwards investigation reads and commands while denying nested arbitrary paths", async () => {
    const fetcher = vi
      .fn()
      .mockImplementation(async () =>
        Response.json({ objects: [], links: [] }),
      );
    vi.stubGlobal("fetch", fetcher);
    expect(
      (
        await GET(
          request("investigations", undefined, {
            cookie: "vigil_demo_session=server-signed-token",
          }),
          context("investigations"),
        )
      ).status,
    ).toBe(200);
    expect(
      (
        await POST(
          request(
            "investigations/commands",
            { type: "create_object" },
            { cookie: "vigil_demo_session=server-signed-token" },
          ),
          context("investigations/commands"),
        )
      ).status,
    ).toBe(200);
    expect(
      (
        await GET(
          request("investigations/internal"),
          context("investigations/internal"),
        )
      ).status,
    ).toBe(404);
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it("rejects cross-origin mutation before any upstream request", async () => {
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    const result = await POST(
      request(
        "session",
        { actor: "protection" },
        { origin: "https://untrusted.example" },
      ),
      context("session"),
    );
    expect(result.status).toBe(403);
    expect(fetcher).not.toHaveBeenCalled();
  });
  it("rejects a forged host even with an allowed origin", async () => {
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    const result = await POST(
      request(
        "session",
        { actor: "protection" },
        { host: "untrusted.example" },
      ),
      context("session"),
    );
    expect(result.status).toBe(403);
    expect(fetcher).not.toHaveBeenCalled();
  });
  it("keeps session tokens in HttpOnly cookies and out of public JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          token: "signed-demo-token",
          identity: { actor: "protection" },
        }),
      ),
    );
    const result = await POST(
      request("session", { actor: "protection" }),
      context("session"),
    );
    expect(await result.json()).toEqual({ identity: { actor: "protection" } });
    expect(result.headers.get("set-cookie")).toContain("HttpOnly");
    expect(result.headers.get("set-cookie")).toContain("SameSite=strict");
    expect(result.headers.get("cache-control")).toBe("no-store");
  });
  it("ignores client tenant and authorization headers", async () => {
    const fetcher = vi.fn().mockResolvedValue(Response.json({ missions: [] }));
    vi.stubGlobal("fetch", fetcher);
    await GET(
      request("missions", undefined, {
        cookie: "vigil_demo_session=server-signed-token",
        authorization: "Bearer forged",
        "x-tenant-id": "care-demo",
      }),
      context("missions"),
    );
    const options = fetcher.mock.calls[0][1];
    expect(options.headers.Authorization).toBe("Bearer server-signed-token");
    expect(options.headers["x-tenant-id"]).toBeUndefined();
  });
  it("preserves command keys and upstream conflict responses", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(
        Response.json({ error: { code: "VERSION_CONFLICT" } }, { status: 409 }),
      );
    vi.stubGlobal("fetch", fetcher);
    const result = await POST(
      request(
        "missions/VGL-0041/commands",
        { type: "generate_plan", expected_version: 2 },
        {
          cookie: "vigil_demo_session=server-signed-token",
          "idempotency-key": "retry-key-123",
        },
      ),
      context("missions/VGL-0041/commands"),
    );
    expect(result.status).toBe(409);
    expect(fetcher.mock.calls[0][1].headers["Idempotency-Key"]).toBe(
      "retry-key-123",
    );
  });
  it("restricts the proxy to known paths", async () => {
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    const result = await GET(
      request("admin/secrets"),
      context("admin/secrets"),
    );
    expect(result.status).toBe(404);
    expect(fetcher).not.toHaveBeenCalled();
  });
  it("reports upstream failure without leaking internal exception content", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockRejectedValue(new Error("private internal connection string")),
    );
    const result = await GET(request("missions"), context("missions"));
    expect(result.status).toBe(503);
    expect(await result.text()).not.toContain("private internal");
  });
  it("rejects large bodies before contacting core", async () => {
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    const result = await POST(
      request("missions", { title: "x".repeat(48001) }),
      context("missions"),
    );
    expect(result.status).toBe(413);
    expect(fetcher).not.toHaveBeenCalled();
  });
});
