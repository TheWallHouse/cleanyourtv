#!/usr/bin/env bash
# Brings in what CleanYourTV (Gathin23/cleanyourtv) has changed since the last sync, keeping only what this repo
# holds (.wallhouse-keep): the launcher and the tools that install it. Their launcher fixes arrive; their TV lists,
# add-ons, themes and docs stay out.
#
# Usage: tools/sync-cleanyourtv.sh
#
# It merges upstream/main without committing, drops every file outside .wallhouse-keep (new ones, and the ones
# they changed that this repo removed), and commits if nothing else conflicts. When their change and ours touch the
# same lines of a kept file, it stops and lists those files: fix them, `git add` them and `git commit`.
# Afterwards build and test the launcher (launcher/: ./gradlew testReleaseUnitTest lintRelease assembleRelease).
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
if [ -n "$(git status --porcelain)" ]; then
    echo "The working tree has changes; commit or stash them first." >&2
    exit 1
fi
git remote get-url upstream >/dev/null 2>&1 || git remote add upstream https://github.com/Gathin23/cleanyourtv.git
git fetch -q upstream
REF=${SYNC_REF:-upstream/main}   # SYNC_REF: another ref, only to try the script
behind=$(git rev-list --count HEAD..$REF)
if [ "$behind" = 0 ]; then
    echo "Up to date with CleanYourTV."
    exit 0
fi
echo "CleanYourTV has $behind new commits; $(git rev-list --count HEAD..$REF -- launcher) of them touch the launcher."

git merge --no-ff --no-commit $REF >/dev/null 2>&1 || true

kept() {
    local path=$1 line
    while IFS= read -r line; do
        case "$line" in ''|'#'*) continue ;; esac
        if [ "$path" = "$line" ]; then return 0; fi
        case "$line" in */) case "$path" in "$line"*) return 0 ;; esac ;; esac
    done < .wallhouse-keep
    return 1
}

# Everything the merge brought in or left in conflict that this repo doesn't keep goes.
{ git diff --cached --name-only; git diff --name-only --diff-filter=U; } | sort -u | while IFS= read -r f; do
    [ -n "$f" ] || continue
    if ! kept "$f"; then
        git rm -q --cached --ignore-unmatch -- "$f" 2>/dev/null || true
        rm -f -- "$f"
    fi
done
# Folders a removed file leaves empty.
find . -path ./.git -prune -o -type d -empty -print 2>/dev/null | grep -v '^\./\.git' | xargs -r rmdir -p --ignore-fail-on-non-empty 2>/dev/null || true

conflicts=$(git diff --name-only --diff-filter=U)
if [ -n "$conflicts" ]; then
    echo "These kept files changed on both sides; fix them, git add them and git commit:" >&2
    echo "$conflicts" | sed 's/^/  /' >&2
    exit 2
fi
git commit -q -m "Sync CleanYourTV up to $(git rev-parse --short $REF)

Their changes to the launcher and its install tools; the rest of their repo stays out (.wallhouse-keep)."
echo "Synced: $(git log --oneline -1)"
echo "Now build and test the launcher before a release."
