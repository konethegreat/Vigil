# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Self-contained Mind service
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
FROM python:3.15-slim@sha256:94d3426f1def7db4a106a232ac121bfd975393e5dfd40a619b955fae45fe7913
RUN pip install --no-cache-dir uv==0.11.24
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev
COPY vigil_mind ./vigil_mind
USER 10001:10001
EXPOSE 8000
CMD ["/app/.venv/bin/uvicorn", "vigil_mind.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
