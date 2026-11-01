// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Validated service configuration (P1 task 1.4)
/**
 * Runtime configuration.
 *
 * Values come from the environment and are validated before the server binds.
 * Secrets are KMS references, never literals (BACKEND_GUIDE §9.3, INSTRUCTIONS
 * §9.10).
 */

export type Environment = "dev" | "test" | "staging" | "production";
export type LogLevel = "INFO" | "WARNING" | "ERROR";

export interface Settings {
  readonly serviceName: string;
  readonly environment: Environment;
  readonly host: string;
  readonly port: number;
  readonly logLevel: LogLevel;
  readonly wardenUrl: string;
  readonly internalTokenRef: string;
  readonly otelEndpoint: string | undefined;
}

export class ConfigurationError extends Error {
  readonly issues: readonly string[];

  constructor(issues: readonly string[]) {
    super(`invalid configuration: ${issues.join("; ")}`);
    this.name = "ConfigurationError";
    this.issues = issues;
  }
}

const ENVIRONMENTS: readonly Environment[] = [
  "dev",
  "test",
  "staging",
  "production",
];
const LOG_LEVELS: readonly LogLevel[] = ["INFO", "WARNING", "ERROR"];
const DEFAULT_PORT = 8080;

function read(env: NodeJS.ProcessEnv, key: string): string | undefined {
  const value = env[key]?.trim();
  return value === undefined || value.length === 0 ? undefined : value;
}

function isHttp(value: string): boolean {
  return value.startsWith("http://") || value.startsWith("https://");
}

function isEnvironment(value: string): value is Environment {
  return (ENVIRONMENTS as readonly string[]).includes(value);
}

function isLogLevel(value: string): value is LogLevel {
  return (LOG_LEVELS as readonly string[]).includes(value);
}

export function loadSettings(env: NodeJS.ProcessEnv): Settings {
  const issues: string[] = [];

  const serviceName =
    read(env, "VIGIL_SERVICE_NAME") ?? "vigil-service-template";
  if (serviceName.length > 64) {
    issues.push("serviceName must be between 1 and 64 characters");
  }

  let environment: Environment = "dev";
  const rawEnvironment = read(env, "VIGIL_ENVIRONMENT");
  if (rawEnvironment !== undefined) {
    if (isEnvironment(rawEnvironment)) {
      environment = rawEnvironment;
    } else {
      issues.push(`environment must be one of ${ENVIRONMENTS.join(", ")}`);
    }
  }

  const host = read(env, "VIGIL_HOST") ?? "127.0.0.1";

  let port = DEFAULT_PORT;
  const rawPort = read(env, "VIGIL_PORT");
  if (rawPort !== undefined) {
    const parsed = Number(rawPort);
    if (Number.isInteger(parsed) && parsed >= 1 && parsed <= 65535) {
      port = parsed;
    } else {
      issues.push("port must be an integer between 1 and 65535");
    }
  }

  let logLevel: LogLevel = "INFO";
  const rawLogLevel = read(env, "VIGIL_LOG_LEVEL");
  if (rawLogLevel !== undefined) {
    if (isLogLevel(rawLogLevel)) {
      logLevel = rawLogLevel;
    } else {
      issues.push(`logLevel must be one of ${LOG_LEVELS.join(", ")}`);
    }
  }

  const wardenUrl = read(env, "VIGIL_WARDEN_URL");
  if (wardenUrl === undefined) {
    issues.push("wardenUrl is required (VIGIL_WARDEN_URL)");
  } else if (!isHttp(wardenUrl)) {
    issues.push("wardenUrl must start with http:// or https://");
  }

  const internalTokenRef = read(env, "VIGIL_INTERNAL_TOKEN_REF");
  if (internalTokenRef === undefined) {
    issues.push("internalTokenRef is required (VIGIL_INTERNAL_TOKEN_REF)");
  } else if (!internalTokenRef.startsWith("kms://")) {
    issues.push("internalTokenRef must be a kms:// reference, never a secret");
  }

  const rawOtelEndpoint =
    read(env, "VIGIL_OTEL_ENDPOINT") ??
    read(env, "OTEL_EXPORTER_OTLP_ENDPOINT");
  if (rawOtelEndpoint !== undefined && !isHttp(rawOtelEndpoint)) {
    issues.push("otelEndpoint must start with http:// or https://");
  }

  if (issues.length > 0) {
    throw new ConfigurationError(issues);
  }

  return Object.freeze({
    serviceName,
    environment,
    host,
    port,
    logLevel,
    wardenUrl: wardenUrl ?? "",
    internalTokenRef: internalTokenRef ?? "",
    otelEndpoint: rawOtelEndpoint,
  });
}
