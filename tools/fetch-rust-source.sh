#!/usr/bin/env bash
# Fetch the upstream codex-rs lmstudio crate into tmp/codex-lmstudio/.
#
# The upstream is the openai/codex repository; we only need the
# `codex-rs/lmstudio/` subtree. Sparse-checkout keeps the working
# tree small.
set -euo pipefail

UPSTREAM_REPO="${UPSTREAM_REPO:-https://github.com/openai/codex.git}"
UPSTREAM_REF="${UPSTREAM_REF:-main}"
SUBPATH="codex-rs/lmstudio"

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$PROJECT_ROOT/tmp/codex-lmstudio"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Fetching $UPSTREAM_REPO ($UPSTREAM_REF) :: $SUBPATH -> $DEST"

git -C "$WORK_DIR" init -q
git -C "$WORK_DIR" remote add origin "$UPSTREAM_REPO"
git -C "$WORK_DIR" config core.sparseCheckout true
echo "$SUBPATH/" > "$WORK_DIR/.git/info/sparse-checkout"
git -C "$WORK_DIR" fetch -q --depth=1 origin "$UPSTREAM_REF"
git -C "$WORK_DIR" checkout -q FETCH_HEAD

mkdir -p "$DEST"
rm -rf "$DEST"/*
cp -R "$WORK_DIR/$SUBPATH/." "$DEST/"

echo "Done. Vendored at: $DEST"
