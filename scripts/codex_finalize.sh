#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/codex_finalize.sh \
    --test-command "<command>" \
    --commit-message "<message>" \
    --pr-title "<title>" \
    [--summary "<line>"]... \
    [--test-note "<line>"]... \
    [--doc "<path>"]... \
    [--no-doc-update] \
    [--tests-added] \
    [--breaking-change] \
    [--base-branch "<branch>"] \
    [--log-dir "<dir>"] \
    [--skip-pr]

Required:
  --test-command     Test command to run after code changes
  --commit-message   Commit message for the current branch
  --pr-title         Draft PR title for codexReview

Document decision:
  Pass at least one --doc, or explicitly pass --no-doc-update.

Examples:
  scripts/codex_finalize.sh \
    --test-command "./gradlew :app:policyTest" \
    --commit-message "Feat: 자동 후처리 스크립트 추가" \
    --pr-title "자동 후처리 스크립트 추가" \
    --summary "codex 후처리 자동화 스크립트를 추가했습니다." \
    --summary "테스트 로그 저장과 Draft PR 생성 로직을 포함합니다." \
    --doc "docs/1Pager/0003_codex_finish_automation/guide.md"
EOF
}

die() {
  echo "Error: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

check_doc_changed() {
  local path="$1"

  if git ls-files --others --exclude-standard -- "$path" | grep -q .; then
    return 0
  fi

  if ! git diff --quiet -- "$path"; then
    return 0
  fi

  if ! git diff --cached --quiet -- "$path"; then
    return 0
  fi

  return 1
}

append_checkbox() {
  local checked="$1"
  local label="$2"

  if [[ "$checked" == "true" ]]; then
    printf -- "- [x] %s\n" "$label"
    return
  fi

  printf -- "- [ ] %s\n" "$label"
}

build_pr_body() {
  local branch_name="$1"
  local log_file="$2"

  {
    echo "## 변경 사항"
    if ((${#summaries[@]} > 0)); then
      for item in "${summaries[@]}"; do
        printf -- "- %s\n" "$item"
      done
    else
      echo "- 구현 변경 사항을 반영했습니다."
    fi
    echo

    echo "## 테스트 방법"
    printf -- "- \`%s\`\n" "$test_command"
    for note in "${test_notes[@]}"; do
      printf -- "- %s\n" "$note"
    done
    printf -- "- 테스트 로그: \`%s\`\n" "$log_file"
    echo

    if [[ "$doc_update_declared" == "true" ]]; then
      echo "## 관련 문서"
      for doc in "${docs[@]}"; do
        printf -- "- \`%s\`\n" "$doc"
      done
      echo
    fi

    echo "## 체크리스트"
    append_checkbox "$tests_added" "테스트 추가됨"
    append_checkbox "$doc_update_declared" "문서 업데이트됨"
    append_checkbox "$breaking_change_free" "Breaking change 없음"
    echo

    echo "## 메모"
    printf -- "- 브랜치: \`%s\`\n" "$branch_name"
    echo "- base 브랜치: \`$base_branch\`"
  } >"$pr_body_file"
}

require_command git
require_command gh
require_command bash
require_command tee
require_command date
require_command mktemp

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || die "Run this script inside a git repository."
cd "$repo_root"

test_command=""
commit_message=""
pr_title=""
base_branch="codexReview"
log_dir=".context/test-logs"
skip_pr="false"
tests_added="false"
breaking_change_free="true"
doc_update_declared="false"
doc_decision_made="false"
declare -a summaries=()
declare -a test_notes=()
declare -a docs=()

while (($# > 0)); do
  case "$1" in
    --test-command)
      [[ $# -ge 2 ]] || die "--test-command requires a value"
      test_command="$2"
      shift 2
      ;;
    --commit-message)
      [[ $# -ge 2 ]] || die "--commit-message requires a value"
      commit_message="$2"
      shift 2
      ;;
    --pr-title)
      [[ $# -ge 2 ]] || die "--pr-title requires a value"
      pr_title="$2"
      shift 2
      ;;
    --summary)
      [[ $# -ge 2 ]] || die "--summary requires a value"
      summaries+=("$2")
      shift 2
      ;;
    --test-note)
      [[ $# -ge 2 ]] || die "--test-note requires a value"
      test_notes+=("$2")
      shift 2
      ;;
    --doc)
      [[ $# -ge 2 ]] || die "--doc requires a value"
      docs+=("$2")
      doc_update_declared="true"
      doc_decision_made="true"
      shift 2
      ;;
    --no-doc-update)
      doc_update_declared="false"
      doc_decision_made="true"
      shift
      ;;
    --tests-added)
      tests_added="true"
      shift
      ;;
    --breaking-change)
      breaking_change_free="false"
      shift
      ;;
    --base-branch)
      [[ $# -ge 2 ]] || die "--base-branch requires a value"
      base_branch="$2"
      shift 2
      ;;
    --log-dir)
      [[ $# -ge 2 ]] || die "--log-dir requires a value"
      log_dir="$2"
      shift 2
      ;;
    --skip-pr)
      skip_pr="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

[[ -n "$test_command" ]] || die "--test-command is required"
[[ -n "$commit_message" ]] || die "--commit-message is required"
[[ -n "$pr_title" ]] || die "--pr-title is required"
[[ "$doc_decision_made" == "true" ]] || die "Pass --doc at least once, or explicitly pass --no-doc-update"

current_branch="$(git branch --show-current)"
[[ -n "$current_branch" ]] || die "Detached HEAD is not supported"

if [[ -z "$(git status --short)" ]]; then
  die "No working tree changes found"
fi

if [[ "$skip_pr" != "true" ]]; then
  git ls-remote --exit-code --heads origin "$base_branch" >/dev/null 2>&1 \
    || die "Remote branch '$base_branch' does not exist on origin"
fi

if [[ "$doc_update_declared" == "true" ]]; then
  for doc in "${docs[@]}"; do
    [[ -e "$doc" ]] || die "Document path not found: $doc"
    check_doc_changed "$doc" || die "Document has no changes relative to HEAD: $doc"
  done
fi

mkdir -p "$log_dir"
timestamp="$(date +%Y%m%d-%H%M%S)"
safe_branch="${current_branch//\//-}"
log_file="$log_dir/${timestamp}-${safe_branch}.log"

echo "Running tests: $test_command"
echo "Log file: $log_file"

set +e
bash -lc "$test_command" 2>&1 | tee "$log_file"
test_exit_code=${PIPESTATUS[0]}
set -e

if [[ "$test_exit_code" -ne 0 ]]; then
  echo "Test command failed. Review log: $log_file" >&2
  exit "$test_exit_code"
fi

git add -A

if git diff --cached --quiet; then
  die "No staged changes after git add -A"
fi

git commit -m "$commit_message"
git push -u origin "$current_branch"

if [[ "$skip_pr" == "true" ]]; then
  echo "Commit and push completed. PR creation skipped."
  exit 0
fi

existing_pr_url="$(
  gh pr list \
    --base "$base_branch" \
    --head "$current_branch" \
    --state open \
    --json url \
    --jq '.[0].url // ""'
)"

if [[ -n "$existing_pr_url" ]]; then
  echo "Existing PR found: $existing_pr_url"
  exit 0
fi

pr_body_file="$(mktemp "${TMPDIR:-/tmp}/codex-pr-body.XXXXXX")"
trap 'rm -f "$pr_body_file"' EXIT

build_pr_body "$current_branch" "$log_file"

new_pr_url="$(
  gh pr create \
    --base "$base_branch" \
    --head "$current_branch" \
    --draft \
    --title "$pr_title" \
    --body-file "$pr_body_file"
)"

echo "Draft PR created: $new_pr_url"
