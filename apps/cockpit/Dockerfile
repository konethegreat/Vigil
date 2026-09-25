# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Reproducible local cockpit
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS build
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /workspace
RUN npm install --global pnpm@11.17.0
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/cockpit/package.json apps/cockpit/package.json
RUN pnpm install --frozen-lockfile
COPY apps/cockpit apps/cockpit
RUN pnpm --dir apps/cockpit build

FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS runtime
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
