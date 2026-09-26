// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Server-side session and origin boundary
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Allow scoped investigation endpoints
import "server-only";
import { NextRequest, NextResponse } from "next/server";
export const runtime = "nodejs";
export const dynamic = "force-dynamic";
const core = process.env.VIGIL_CORE_URL || "http://127.0.0.1:8087/api/v1";
const gateway =
  process.env.VIGIL_INTERNAL_TOKEN || "local-synthetic-mission-only";
const cookieName = "vigil_demo_session";
const allowed =
  /^(missions(?:\/VGL-[A-Za-z0-9-]+(?:\/(commands|audit|export))?)?|investigations(?:\/commands)?|session|intelligence\/status|health)$/;
async function session(actor: string) {
  const response = await fetch(`${core}/session`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Vigil-Gateway": gateway },
    body: JSON.stringify({ actor }),
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });
  if (!response.ok)
    throw new Error("The local session service is unavailable.");
  return response.json() as Promise<{ token: string; identity: unknown }>;
}
async function handle(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
) {
  const path = (await context.params).path.join("/");
  if (!allowed.test(path))
    return NextResponse.json({ message: "Unknown endpoint." }, { status: 404 });
  const origin = request.headers.get("origin") || "";
  const origins = process.env.VIGIL_COCKPIT_ORIGIN
    ? [process.env.VIGIL_COCKPIT_ORIGIN]
    : ["http://127.0.0.1:3100", "http://localhost:3100"];
  if (
    request.method === "POST" &&
    (!origins.includes(origin) ||
      new URL(origin).host !== request.headers.get("host"))
  )
    return NextResponse.json(
      { message: "This operation requires the cockpit origin." },
      { status: 403 },
    );
  try {
    let token = request.cookies.get(cookieName)?.value;
    let issued = false;
    let body: string | undefined;
    if (request.method === "POST") {
      body = await request.text();
      if (body.length > 48000)
        return NextResponse.json(
          { message: "The request is too large." },
          { status: 413 },
        );
      try {
        JSON.parse(body);
      } catch {
        return NextResponse.json(
          { message: "Send valid JSON." },
          { status: 400 },
        );
      }
    }
    if (path === "session" && request.method === "POST") {
      const actor = JSON.parse(body!).actor;
      if (!["operations", "protection", "reviewer"].includes(actor))
        return NextResponse.json(
          { message: "Unknown demonstration workspace." },
          { status: 400 },
        );
      const result = await session(actor);
      const response = NextResponse.json({ identity: result.identity });
      response.cookies.set(cookieName, result.token, {
        httpOnly: true,
        sameSite: "strict",
        secure: request.nextUrl.protocol === "https:",
        path: "/",
        maxAge: 3600,
      });
      response.headers.set("Cache-Control", "no-store");
      return response;
    }
    if (!token) {
      token = (await session("operations")).token;
      issued = true;
    }
    const upstream = await fetch(`${core}/${path}`, {
      method: request.method,
      headers: {
        "Content-Type": "application/json",
        "X-Vigil-Gateway": gateway,
        Authorization: `Bearer ${token}`,
        "Idempotency-Key": request.headers.get("idempotency-key") || "",
      },
      body,
      cache: "no-store",
      signal: AbortSignal.timeout(220000),
    });
    const response = new NextResponse(await upstream.text(), {
      status: upstream.status,
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": "no-store",
      },
    });
    if (issued)
      response.cookies.set(cookieName, token, {
        httpOnly: true,
        sameSite: "strict",
        secure: request.nextUrl.protocol === "https:",
        path: "/",
        maxAge: 3600,
      });
    if (upstream.status === 401) response.cookies.delete(cookieName);
    if (path.endsWith("/export") && upstream.ok)
      response.headers.set(
        "Content-Disposition",
        `attachment; filename="${path.split("/")[1]}-synthetic-record.json"`,
      );
    return response;
  } catch {
    return NextResponse.json(
      {
        code: "CORE_UNAVAILABLE",
        message:
          "The mission service could not be reached. Check the local services and retry; an interrupted command may have committed.",
      },
      { status: 503 },
    );
  }
}
export { handle as GET, handle as POST };
