#!/usr/bin/env python3
# Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — Vigil compliance validator: branch, hygiene, WORKLOG, append-only ledgers, attribution, PR title, phase claim (P0)
# Modified by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — P0 review: PR-base rule, preserved attribution, accepted-ADR immutability, more file types
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 — Recognise Codex workspace branches
"""Vigil — Project Compliance Validator.

Automates the checkable parts of INSTRUCTIONS.md and docs/BRANCHING_STRATEGY.md. It runs as a
pre-commit hook (local mode) and in .github/workflows/pr-checks.yml (--ci mode).

Checks:
  1. Branch      no work on main/develop/master; feature|bugfix/P<ID>-<slug>, hotfix|fix|chore/<slug>,
                 release/v<X.Y.Z>. GITHUB_HEAD_REF is read before git, because pull_request checkouts
                 are detached (docs/AI_COMMUNICATION.md MSG-004).
  2. Hygiene     no tracked docs/engineers/* (except .gitkeep), .env/.env.* (.env.example is allowed),
                 node_modules/, __pycache__/, .venv/, target/, build/, dist/, and no tracked file that
                 .gitignore says must stay untracked. Lockfiles are required, never forbidden (MSG-005).
  3. WORKLOG     local: NOTICE when WORKLOG.md is not modified. --ci: FAIL unless the branch changes it.
  4. Ledgers     --ci: WORKLOG.md and docs/API_DESIGN.md lose no lines; every docs/AI_COMMUNICATION.md
                 MSG-NNN row keeps every cell except its final Status cell.
  5. Attribution --ci: added/modified code, config and docs/*.md files carry "Written by" or
                 "Modified by" in their first 15 lines (INSTRUCTIONS.md §8).
  6. PR title    when PR_TITLE or --pr-title is given: type(P<ID>|repo): summary.
  7. Phase claim a feature/P<ID>-... branch warns unless P<ID> is 🚀 In Progress in PHASES_AND_TASKS.
  8. PR base     --ci: release/ and hotfix/ target main; feature/, bugfix/, fix/, chore/ target develop;
                 main → develop is the only back-merge (BRANCHING_STRATEGY §1, §3).
  9. Preserved   --ci: a modified file keeps every earlier attribution line (INSTRUCTIONS §8), and warns
                 if it gained no "Modified by" line.
 10. ADRs        --ci: an Accepted ADR is immutable, except for changing its Status to "Superseded by".

Exit codes: 0 = compliant, 1 = compliance errors, 2 = cannot run (not a git repository, bad usage).
Standard library only; Python 3.10+.
"""

from __future__ import annotations

import argparse
import difflib
import os
import re
import subprocess
import sys
from pathlib import Path

PROTECTED_BRANCHES = ("main", "develop", "master")
VALID_PREFIXES = ("feature/", "bugfix/", "hotfix/", "release/", "fix/", "chore/", "codex/")
_SLUG = r"[a-z0-9]+(?:-[a-z0-9]+)*"  # lowercase kebab-case (BRANCHING_STRATEGY §1)
BRANCH_PATTERNS: dict[str, tuple[re.Pattern[str], str]] = {
    "codex/": (re.compile(rf"codex/{_SLUG}"), "codex/mission-foundation"),
    "feature/": (re.compile(rf"feature/P\d+-{_SLUG}"), "feature/P8-outbox-relay"),
    "bugfix/": (re.compile(rf"bugfix/P\d+-{_SLUG}"), "bugfix/P7-cursor-overflow"),
    "hotfix/": (re.compile(rf"hotfix/{_SLUG}"), "hotfix/cve-2027-1234"),
    "release/": (re.compile(r"release/v\d+\.\d+\.\d+"), "release/v0.1.0"),
    "fix/": (re.compile(rf"fix/{_SLUG}"), "fix/typo-in-gateway-config"),
    "chore/": (re.compile(rf"chore/{_SLUG}"), "chore/bump-toolchains"),
}
PR_TITLE_PATTERN = re.compile(
    r"^(feat|fix|chore|docs|test|refactor|perf|build|ci|security)\((P[0-9]+|repo)\): .+"
)
PHASE_BRANCH = re.compile(r"^feature/(P\d+)-")

# Tracked-file hygiene (INSTRUCTIONS §10 #4-#5). Lockfiles (Cargo.lock, go.sum, pnpm-lock.yaml,
# uv.lock, gradle.lockfile, mix.lock) are deliberately NOT here: Vigil requires them for reproducible,
# air-gapped, SBOM-verifiable builds (INSTRUCTIONS §6 rule 5; AI_COMMUNICATION MSG-005).
FORBIDDEN_DIRS = ("node_modules", "__pycache__", ".venv", "target", "build", "dist")
ALLOWED_ENV_FILES = (".env.example",)
ENGINEER_PROFILES = "docs/engineers/"

WORKLOG = "WORKLOG.md"
LINE_APPEND_ONLY = (WORKLOG, "docs/API_DESIGN.md")
AI_COMMUNICATION = "docs/AI_COMMUNICATION.md"
PHASES_FILE = "docs/PHASES_AND_TASKS.md"
MSG_ROW = re.compile(r"^\|\s*(MSG-\d+)\s*\|")

ATTRIBUTION_MARKERS = ("Written by", "Modified by")
ATTRIBUTION_HEADER_LINES = 15
ATTRIBUTION_SUFFIXES = {
    ".py", ".java", ".kt", ".kts", ".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs",
    ".go", ".rs", ".ex", ".exs", ".sql", ".sh", ".yml", ".yaml", ".toml",
    ".proto", ".wit", ".tf", ".hcl", ".css", ".scss",
}  # fmt: skip
ATTRIBUTION_SCAN_LINES = 60  # headers grow with each "Modified by" line
LEDGERS = (WORKLOG, "docs/API_DESIGN.md", AI_COMMUNICATION, PHASES_FILE)  # rows, not authors
ADR_DIR = "docs/adr/"
ADR_STATUS = re.compile(r"^\*\*Status:\*\*.*$", re.M)
PR_BASE_PREFIXES = {
    "main": ("release/", "hotfix/"),
    "develop": ("feature/", "bugfix/", "fix/", "chore/", "codex/"),
}
ATTRIBUTION_NAMES = {"Dockerfile", "justfile"}
LOCKFILES = {
    "Cargo.lock", "go.sum", "go.work.sum", "pnpm-lock.yaml", "uv.lock", "gradle.lockfile",
    "mix.lock", "package-lock.json", "yarn.lock", "poetry.lock", ".terraform.lock.hcl",
}  # fmt: skip
ATTRIBUTION_EXEMPT_PREFIXES = ("docs/research/appendix/",)
GENERATED_MARKERS = (
    "Code generated",
    "DO NOT EDIT",
    "@generated",
    "Generated by the protocol buffer",
)

IN_GITHUB_ACTIONS = os.environ.get("GITHUB_ACTIONS") == "true"


class Report:
    """Collects findings per check, prints them, and summarises the run."""

    def __init__(self) -> None:
        self.errors = 0
        self.warnings = 0
        self.sections: list[list] = []  # [name, errors, warnings]

    def section(self, name: str, banner: str) -> None:
        print(f"[*] {banner}")
        self.sections.append([name, 0, 0])

    def _emit(self, level: str, msg: str) -> None:
        # Escape newlines so a crafted path cannot forge GitHub workflow commands.
        msg = msg.replace("\r", "\\r").replace("\n", "\\n")
        print(f"[!] {level}: {msg}")
        if IN_GITHUB_ACTIONS and level in ("ERROR", "WARNING"):
            command = "error" if level == "ERROR" else "warning"
            print(f"::{command} title=Vigil compliance::{msg.replace('%', '%25')}")

    def error(self, msg: str) -> None:
        self.errors += 1
        if self.sections:
            self.sections[-1][1] += 1
        self._emit("ERROR", msg)

    def warn(self, msg: str) -> None:
        self.warnings += 1
        if self.sections:
            self.sections[-1][2] += 1
        self._emit("WARNING", msg)

    def notice(self, msg: str) -> None:
        self._emit("NOTICE", msg)

    def write_step_summary(self) -> None:
        path = os.environ.get("GITHUB_STEP_SUMMARY")
        if not path:
            return
        rows = ["### Vigil compliance validator", "", "| Check | Result |", "|---|---|"]
        for name, errors, warnings in self.sections:
            result = "✅ pass"
            if errors:
                result = f"❌ {errors} error(s)"
            elif warnings:
                result = f"⚠️ {warnings} warning(s)"
            rows.append(f"| {name} | {result} |")
        with open(path, "a", encoding="utf-8") as fh:
            fh.write("\n".join(rows) + "\n")


def run_git(args: list[str], *, strip: bool = True) -> str:
    """Run a git command and return its stdout; raises CalledProcessError on failure."""
    res = subprocess.run(
        ["git", *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=True,
    )
    return res.stdout.strip() if strip else res.stdout


def git_ok(args: list[str]) -> bool:
    try:
        run_git(args)
    except subprocess.CalledProcessError:
        return False
    return True


def git_paths(args: list[str]) -> list[str]:
    """Run a git command that prints NUL-separated paths (-z) and return them."""
    return [p for p in run_git(args, strip=False).split("\0") if p]


def file_at(rev: str, path: str) -> str | None:
    """Content of `path` at `rev`, or None when it does not exist there."""
    try:
        return run_git(["show", f"{rev}:{path}"], strip=False)
    except subprocess.CalledProcessError:
        return None


def current_branch() -> str:
    """PR head branch in CI (checkouts are detached, MSG-004), else the local branch or 'HEAD'."""
    head_ref = os.environ.get("GITHUB_HEAD_REF", "").strip()
    if head_ref:
        return head_ref
    try:
        return run_git(["rev-parse", "--abbrev-ref", "HEAD"])
    except subprocess.CalledProcessError:  # unborn branch: no commits yet
        return run_git(["symbolic-ref", "--short", "HEAD"])


def validate_active_branch(report: Report, *, ci: bool, base: str) -> str | None:
    branch = current_branch()
    report.section("Branch", f"Checking active branch: {branch}")

    if branch == "HEAD":
        if ci:
            report.error(
                "Cannot determine the PR head branch: HEAD is detached and GITHUB_HEAD_REF is not "
                "set (AI_COMMUNICATION MSG-004)."
            )
        else:
            report.warn(
                "HEAD is detached (rebase or bisect in progress?); branch checks skipped."
            )
        return None

    if ci and branch == "main" and base == "develop":
        report.notice("Back-merge PR main → develop (BRANCHING_STRATEGY §3): allowed.")
        return branch

    if branch in PROTECTED_BRANCHES:
        report.error(
            f"Direct commits to '{branch}' are prohibited! Create a feature branch "
            "(INSTRUCTIONS §3)."
        )
        return branch

    prefix = next((p for p in VALID_PREFIXES if branch.startswith(p)), None)
    if prefix is None:
        report.error(
            f"Branch '{branch}' does not match naming convention (e.g. feature/P8-outbox-relay). "
            "Use feature|bugfix/P<ID>-<slug>, hotfix|fix|chore|codex/<slug> or release/v<X.Y.Z>."
        )
        return branch

    pattern, example = BRANCH_PATTERNS[prefix]
    if not pattern.fullmatch(branch):
        report.error(
            f"Branch '{branch}' must look like '{example}' (BRANCHING_STRATEGY §1: "
            "lowercase kebab-case slugs, releases as v<X.Y.Z>)."
        )
    return branch


def validate_git_hygiene(report: Report) -> None:
    report.section("Git hygiene", "Checking for gitignored file violations...")
    for path in git_paths(["ls-files", "-z"]):
        parts = path.split("/")
        name = parts[-1]
        if path.startswith(ENGINEER_PROFILES) and name != ".gitkeep":
            report.error(f"Local engineer identity file '{path}' is tracked in git!")
        elif name.endswith(".env") or (
            name.startswith(".env.") and name not in ALLOWED_ENV_FILES
        ):
            report.error(
                f"Environment file '{path}' is tracked in git! (only .env.example may be)"
            )
        elif any(part in FORBIDDEN_DIRS for part in parts[:-1]):
            report.error(f"Build/dependency artefact '{path}' is tracked in git!")

    # Anything tracked that the repository's own .gitignore files exclude (personal global
    # excludes are ignored on purpose, so results match CI).
    ignored = git_paths(
        [
            "ls-files",
            "-z",
            "--cached",
            "--ignored",
            "--exclude-per-directory=.gitignore",
        ]
    )
    for path in ignored:
        report.error(
            f"'{path}' is tracked although .gitignore excludes it; remove it from the index."
        )


def resolve_merge_base(report: Report, base: str) -> str | None:
    """Merge-base of HEAD with origin/<base> (CI) or the local <base> branch."""
    report.section("Base branch", f"Resolving base branch '{base}'...")
    for ref in (f"origin/{base}", base):
        if git_ok(["rev-parse", "--verify", "--quiet", f"{ref}^{{commit}}"]):
            try:
                merge_base = run_git(["merge-base", ref, "HEAD"])
            except subprocess.CalledProcessError:
                report.error(
                    f"'{ref}' and HEAD share no history; cannot compare against the base."
                )
                return None
            print(f"    base: {ref} (merge-base {merge_base[:12]})")
            return merge_base
    report.error(
        f"Neither 'origin/{base}' nor '{base}' exists. In CI check out with fetch-depth: 0; "
        "locally pass --base <branch>."
    )
    return None


def changed_files(merge_base: str) -> list[str]:
    """Paths added, copied, modified or renamed between the merge-base and HEAD."""
    return git_paths(
        ["diff", "--name-only", "-z", "--diff-filter=ACMR", merge_base, "HEAD"]
    )


def modified_files(merge_base: str) -> list[str]:
    """Paths that existed at the merge-base and were modified on the branch."""
    return git_paths(["diff", "--name-only", "-z", "--diff-filter=M", merge_base, "HEAD"])


def locally_modified() -> set[str]:
    """Paths with staged, unstaged or untracked changes (git status)."""
    entries = run_git(
        ["status", "--porcelain=v1", "-z", "--untracked-files=all"], strip=False
    )
    tokens = entries.split("\0")
    paths: set[str] = set()
    i = 0
    while i < len(tokens):
        entry = tokens[i]
        i += 1
        if len(entry) < 4:
            continue
        paths.add(entry[3:])
        if entry[0] in "RC":  # renames/copies carry the source path in the next token
            i += 1
    return paths


def validate_worklog_updated(
    report: Report, *, ci: bool, changed: list[str] | None
) -> None:
    report.section("WORKLOG", "Checking WORKLOG.md status...")
    if not ci:
        if WORKLOG not in locally_modified():
            report.notice(
                "WORKLOG.md does not appear to be modified. Ensure changes are logged."
            )
        return
    if changed is not None and WORKLOG not in changed:
        report.error(
            "WORKLOG.md was not updated on this branch. Append one row per commit "
            "(INSTRUCTIONS §12)."
        )


def removed_lines(old: str, new: str) -> list[str]:
    """Non-blank lines of `old` that are deleted or rewritten in `new` (trailing whitespace ignored)."""
    old_lines = [line.rstrip() for line in old.splitlines()]
    new_lines = [line.rstrip() for line in new.splitlines()]
    matcher = difflib.SequenceMatcher(None, old_lines, new_lines, autojunk=False)
    removed: list[str] = []
    for tag, i1, i2, _j1, _j2 in matcher.get_opcodes():
        if tag in ("delete", "replace"):
            removed.extend(line for line in old_lines[i1:i2] if line.strip())
    return removed


def msg_rows(text: str) -> dict[str, list[list[str]]]:
    """MSG-NNN table rows of the AI_COMMUNICATION ledger, as lists of stripped cells."""
    rows: dict[str, list[list[str]]] = {}
    for line in text.splitlines():
        line = line.strip()
        match = MSG_ROW.match(line)
        if not match:
            continue
        inner = line[1:]
        if inner.endswith("|") and not inner.endswith("\\|"):
            inner = inner[:-1]
        cells = [cell.strip() for cell in re.split(r"(?<!\\)\|", inner)]
        rows.setdefault(match.group(1), []).append(cells)
    return rows


def validate_append_only(report: Report, merge_base: str) -> None:
    report.section("Append-only ledgers", "Checking append-only ledgers...")
    for path in LINE_APPEND_ONLY:
        old = file_at(merge_base, path)
        if old is None:
            continue  # new file: nothing to preserve yet
        new = file_at("HEAD", path)
        if new is None:
            report.error(f"Append-only file '{path}' was deleted.")
            continue
        removed = removed_lines(old, new)
        if removed:
            report.error(
                f"'{path}' is append-only but {len(removed)} existing line(s) were deleted or "
                f"edited, e.g.: {removed[0][:120]}"
            )

    old = file_at(merge_base, AI_COMMUNICATION)
    if old is None:
        return
    new = file_at("HEAD", AI_COMMUNICATION)
    if new is None:
        report.error(f"Append-only ledger '{AI_COMMUNICATION}' was deleted.")
        return
    base_rows, head_rows = msg_rows(old), msg_rows(new)
    for msg_id, rows in head_rows.items():
        if len(rows) > 1:
            report.error(
                f"{AI_COMMUNICATION}: {msg_id} appears {len(rows)} times; IDs are unique."
            )
    for msg_id, rows in base_rows.items():
        after = head_rows.get(msg_id)
        if not after:
            report.error(
                f"{AI_COMMUNICATION}: {msg_id} was deleted; entries are never deleted."
            )
        elif after[0][:-1] != rows[0][:-1]:
            report.error(
                f"{AI_COMMUNICATION}: {msg_id} was edited; only its final Status cell may change."
            )


def needs_attribution(path: str) -> bool:
    name = path.rsplit("/", 1)[-1]
    if path.startswith(ATTRIBUTION_EXEMPT_PREFIXES) or name in LOCKFILES:
        return False
    if name.endswith((".lock", ".lockfile", ".min.js")):
        return False
    if path.startswith("sdks/") and not path.startswith("sdks/_codegen/"):
        return False  # generated SDK output (.gitattributes: linguist-generated)
    if name.endswith(".md"):
        return path.startswith("docs/")
    if (
        name in ATTRIBUTION_NAMES
        or name.startswith("Dockerfile.")
        or name.endswith(".dockerfile")
    ):
        return True
    return os.path.splitext(name)[1] in ATTRIBUTION_SUFFIXES


def validate_attribution(report: Report, changed: list[str]) -> None:
    report.section("Attribution", "Checking attribution headers (INSTRUCTIONS §8)...")
    for path in changed:
        if not needs_attribution(path):
            continue
        file = Path(path)
        if file.is_symlink() or not file.is_file():
            continue
        with file.open(encoding="utf-8", errors="replace") as fh:
            head = [line for _, line in zip(range(ATTRIBUTION_HEADER_LINES), fh)]
        if any(marker in line for line in head[:5] for marker in GENERATED_MARKERS):
            continue  # generated code carries its generator's header instead
        if not any(marker in line for line in head for marker in ATTRIBUTION_MARKERS):
            report.error(
                f"'{path}' has no 'Written by' / 'Modified by' line in its first "
                f"{ATTRIBUTION_HEADER_LINES} lines."
            )


def attribution_lines(text: str) -> list[str]:
    head = text.splitlines()[:ATTRIBUTION_SCAN_LINES]
    return [line.strip() for line in head if any(m in line for m in ATTRIBUTION_MARKERS)]


def validate_attribution_preserved(report: Report, merge_base: str, modified: list[str]) -> None:
    report.section("Attribution kept", "Checking earlier attribution lines are preserved...")
    for path in modified:
        if not needs_attribution(path) or not Path(path).is_file():
            continue
        old = file_at(merge_base, path)
        if old is None:
            continue
        before = attribution_lines(old)
        after = attribution_lines(Path(path).read_text(encoding="utf-8", errors="replace"))
        missing = [line for line in before if line not in after]
        if missing:
            report.error(
                f"'{path}': an earlier attribution line was removed or altered "
                f"(INSTRUCTIONS §8, never delete another author's line): {missing[0][:90]}"
            )
        elif len(after) <= len(before) and path not in LEDGERS:
            report.warn(f"'{path}' was modified but gained no 'Modified by' line (INSTRUCTIONS §8).")


def validate_adr_immutability(report: Report, merge_base: str, modified: list[str]) -> None:
    report.section("ADRs", "Checking accepted ADRs are unchanged...")
    for path in modified:
        if not (path.startswith(ADR_DIR) and path.endswith(".md")):
            continue
        old = file_at(merge_base, path)
        status = ADR_STATUS.search(old or "")
        if not status or "Accepted" not in status.group(0):
            continue  # Proposed ADRs may still change
        new = Path(path).read_text(encoding="utf-8", errors="replace")
        new_status = ADR_STATUS.search(new)
        same_body = ADR_STATUS.sub("", old or "") == ADR_STATUS.sub("", new)
        if not (same_body and new_status and "Superseded by" in new_status.group(0)):
            report.error(
                f"'{path}' is an Accepted ADR and is immutable: write a superseding ADR and change "
                "only this ADR's Status to 'Superseded by ADR-NNNN' (INSTRUCTIONS §4.2)."
            )


def validate_pr_base(report: Report, branch: str, base: str) -> None:
    report.section("PR base", f"Checking that '{branch}' may target '{base}'...")
    if branch == "main" and base == "develop":
        return  # back-merge (BRANCHING_STRATEGY §3)
    allowed = PR_BASE_PREFIXES.get(base)
    if allowed is None:
        report.error(f"PRs target 'develop' or 'main', not '{base}' (BRANCHING_STRATEGY §1).")
    elif not branch.startswith(allowed):
        expected = "main" if base == "develop" else "develop"
        report.error(
            f"'{branch}' may not target '{base}'; it targets '{expected}' (BRANCHING_STRATEGY §1)."
        )


def validate_pr_title(report: Report, title: str) -> None:
    report.section("PR title", "Checking PR title convention...")
    if not title:
        print("    no PR title provided (PR_TITLE / --pr-title); skipped.")
        return
    if not PR_TITLE_PATTERN.match(title):
        report.error(
            f"PR title '{title}' must match 'type(P<ID>|repo): summary', type one of "
            "feat|fix|chore|docs|test|refactor|perf|build|ci|security (INSTRUCTIONS §3)."
        )


def validate_phase_claim(report: Report, branch: str) -> None:
    match = PHASE_BRANCH.match(branch)
    if not match:
        return
    phase = match.group(1)
    report.section("Phase claim", f"Checking {phase} is claimed in {PHASES_FILE}...")
    try:
        lines = Path(PHASES_FILE).read_text(encoding="utf-8").splitlines()
    except OSError:
        report.warn(f"{PHASES_FILE} not found; cannot confirm {phase} is claimed.")
        return
    status = None
    for i, line in enumerate(lines):
        if line.startswith(f"## {phase}:"):
            for card_line in lines[i + 1 :]:
                if card_line.startswith("## "):
                    break
                if card_line.startswith("**Status**"):
                    status = card_line
                    break
            break
    if status is None:
        report.warn(
            f"No phase card with a **Status** line for {phase} in {PHASES_FILE}."
        )
    elif "🚀 In Progress" not in status:
        report.warn(
            f"{phase} is not claimed: set '🚀 In Progress — <Name> (<AI>) | Started: YYYY-MM-DD' "
            "before your first commit (Rule 9)."
        )


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="validate-compliance.py",
        description="Vigil — Project Compliance Validator (INSTRUCTIONS.md §11, BRANCHING_STRATEGY).",
        epilog=(
            "Local mode (pre-commit): branch, hygiene, WORKLOG notice, PR title if given, phase claim. "
            "--ci adds the base-branch checks: PR base, WORKLOG updated, append-only ledgers, "
            "attribution (present and preserved), accepted-ADR immutability. "
            "Environment: GITHUB_HEAD_REF (PR head branch), GITHUB_BASE_REF (base), PR_TITLE."
        ),
    )
    parser.add_argument(
        "--ci",
        action="store_true",
        help="CI mode: compare the branch with origin/<base> and fail on WORKLOG/ledger/attribution issues",
    )
    parser.add_argument(
        "--base",
        default="",
        help="base branch for --ci (default: $GITHUB_BASE_REF, else develop)",
    )
    parser.add_argument(
        "--pr-title",
        default=None,
        help="PR title to check (default: $PR_TITLE; skipped when empty)",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")  # e.g. Windows code pages vs 🚀
    args = parse_args(argv)

    try:
        os.chdir(run_git(["rev-parse", "--show-toplevel"]))
    except (OSError, subprocess.CalledProcessError):
        print("[!] ERROR: not inside a git repository (or git is not installed).")
        return 2

    base = args.base or os.environ.get("GITHUB_BASE_REF", "") or "develop"
    title = (
        args.pr_title if args.pr_title is not None else os.environ.get("PR_TITLE", "")
    )
    report = Report()

    print("=" * 60)
    print("Vigil — Project Compliance Validator")
    print("=" * 60)
    print(f"[*] Mode: {'CI (base: ' + base + ')' if args.ci else 'local'}")

    branch = validate_active_branch(report, ci=args.ci, base=base)
    if args.ci and branch and branch != "HEAD":
        validate_pr_base(report, branch, base)
    validate_git_hygiene(report)

    changed: list[str] | None = None
    merge_base = resolve_merge_base(report, base) if args.ci else None
    if merge_base is not None:
        changed = changed_files(merge_base)
    validate_worklog_updated(report, ci=args.ci, changed=changed)
    if merge_base is not None and changed is not None:
        validate_append_only(report, merge_base)
        validate_attribution(report, changed)
        modified = modified_files(merge_base)
        validate_attribution_preserved(report, merge_base, modified)
        validate_adr_immutability(report, merge_base, modified)
    validate_pr_title(report, title)
    if branch:
        validate_phase_claim(report, branch)

    report.write_step_summary()
    print("=" * 60)
    if report.errors:
        print(
            f"[FAIL] {report.errors} compliance error(s), {report.warnings} warning(s). "
            "Fix the [!] ERROR lines above."
        )
    else:
        suffix = f" ({report.warnings} warning(s))" if report.warnings else ""
        print(f"[OK] All automated compliance checks passed!{suffix}")
    print("=" * 60)
    return 1 if report.errors else 0


if __name__ == "__main__":
    sys.exit(main())
