# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Self-contained Mind service
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
FROM python:3.13-slim@sha256:2d44738ebf4ce5f63b9d9ea871e9c7cfdc0426f7b0c97cac915fd88ded1d86bb
RUN pip install --no-cache-dir uv==0.11.24
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev
COPY vigil_mind ./vigil_mind
USER 10001:10001
EXPOSE 8000
CMD ["/app/.venv/bin/uvicorn", "vigil_mind.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
