// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Vigil application frame
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Intelligence platform metadata and explorer styles
import type { Metadata } from "next";
import "./globals.css";
import "./investigations.css";
export const metadata: Metadata = {
  title: "Vigil — intelligence into operations",
  description:
    "An African intelligence and operations platform. Connected evidence, investigations, and accountable outcomes.",
};
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en-ZA">
      <body>{children}</body>
    </html>
  );
}
