// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Process entry point (P1 task 1.4)
/**
 * Loads validated settings, installs telemetry only when a collector is
 * configured, then serves until SIGINT or SIGTERM.
 */
import process from "node:process";

import { buildApp } from "./app.js";
import { ConfigurationError, loadSettings } from "./config.js";
import type { Settings } from "./config.js";
import { startTelemetry } from "./telemetry.js";

function main(): void {
  let settings: Settings;
  try {
    settings = loadSettings(process.env);
  } catch (error) {
    if (error instanceof ConfigurationError) {
      console.error(error.message);
      process.exitCode = 2;
      return;
    }
    throw error;
  }

  const provider = startTelemetry(settings);
  const app = buildApp({
    settings,
    ...(provider === undefined ? {} : { provider }),
  });

  const shutdown = (signal: string): void => {
    app.log.info({ signal }, "shutting down");
    void app
      .close()
      .then(async () =>
        provider === undefined ? undefined : provider.shutdown(),
      )
      .catch((error: unknown) => {
        app.log.error({ err: error }, "shutdown failed");
      });
  };

  process.once("SIGINT", () => {
    shutdown("SIGINT");
  });
  process.once("SIGTERM", () => {
    shutdown("SIGTERM");
  });

  app
    .listen({ host: settings.host, port: settings.port })
    .catch((error: unknown) => {
      app.log.error({ err: error }, "failed to start");
      process.exitCode = 1;
    });
}

main();
