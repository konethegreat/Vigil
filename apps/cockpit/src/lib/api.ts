// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
export async function api<T>(
  path: string,
  body?: unknown,
  key?: string,
): Promise<T> {
  const response = await fetch(`/api/vigil/${path}`, {
    method: body ? "POST" : "GET",
    headers: body
      ? {
          "Content-Type": "application/json",
          "Idempotency-Key": key || crypto.randomUUID(),
        }
      : undefined,
    body: body ? JSON.stringify(body) : undefined,
    cache: "no-store",
  });
  const result = await response.json();
  if (!response.ok)
    throw new Error(
      result.error?.message ||
        result.message ||
        "The operation could not be completed.",
    );
  return result;
}
