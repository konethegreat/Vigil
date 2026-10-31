# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Reproducible local cockpit
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
FROM node:25-alpine@sha256:bdf2cca6fe3dabd014ea60163eca3f0f7015fbd5c7ee1b0e9ccb4ced6eb02ef4 AS build
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /workspace
RUN npm install --global pnpm@11.17.0
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/cockpit/package.json apps/cockpit/package.json
RUN pnpm install --frozen-lockfile
COPY apps/cockpit apps/cockpit
RUN pnpm --dir apps/cockpit build

FROM node:25-alpine@sha256:bdf2cca6fe3dabd014ea60163eca3f0f7015fbd5c7ee1b0e9ccb4ced6eb02ef4 AS runtime
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3100
WORKDIR /app
COPY --from=build --chown=node:node /workspace/apps/cockpit/.next/standalone ./
COPY --from=build --chown=node:node /workspace/apps/cockpit/.next/static ./apps/cockpit/.next/static
USER node
EXPOSE 3100
CMD ["node", "apps/cockpit/server.js"]
