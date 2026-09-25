// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Vigil application frame
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "Vigil — the mission continues",
  description:
    "An African mission operating system. Evidence, decisions, and accountable outcomes.",
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
