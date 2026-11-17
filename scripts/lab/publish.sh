#!/usr/bin/env bash
# Publish the design lab to LOR_LAB_DIR, which lor-web serves read-only at /lab/ (lor-zwfd).
#
#   scripts/lab/publish.sh
#
# Rebuilds the whole served tree from SOURCES below: each source that exists is copied
# under its own folder, and /lab/index.html links them in list order. Nothing else in
# LOR_LAB_DIR survives a publish, and this script is the only writer of index.html.
#
# LOR_LAB_DIR comes from the environment, then the env file, then ~/.local/share/lor-lab.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# What the lab serves: source | folder under /lab/ | name in the index. A relative source
# is in this checkout. A source that does not exist is left out. A line goes where its
# link belongs in the index: lor-glev puts docs/design/lab/compare|compare first.
SOURCES=(
  "$HOME/.local/state/lor-shots|shots|Screenshot galleries"
  "prototype/dashboard/dist|dashboard|Dashboard prototype"
  "android/homecore/build/dist/wasmJs/developmentExecutable|launcher|Launcher lab"
)

MARK=".lor-lab"

die() { echo "publish: $*" >&2; exit 1; }

ENV_FILE="${LOR_ENV_FILE:-$HOME/.config/life-on-rails/env}"
if [[ -z "${LOR_LAB_DIR:-}" && -f "$ENV_FILE" ]]; then
  LOR_LAB_DIR="$(sed -n 's/^[[:space:]]*LOR_LAB_DIR[[:space:]]*=[[:space:]]*//p' "$ENV_FILE" | tail -n1 | sed "s/^[\"']//; s/[\"']$//")"
fi
LAB="${LOR_LAB_DIR:-$HOME/.local/share/lor-lab}"
LAB="${LAB/#\~/$HOME}"

# The tree is replaced wholesale, so refuse anywhere that could hold something else.
[[ "$LAB" == /* ]] || die "LOR_LAB_DIR must be absolute: $LAB"
LAB="$(realpath -m "$LAB")"
case "$LAB" in
  / | "$HOME" | "$(dirname "$HOME")") die "refusing to replace $LAB" ;;
  "$ROOT" | "$ROOT"/*) die "refusing $LAB: it is inside the checkout" ;;
esac
if [[ -e "$LAB" ]]; then
  [[ -d "$LAB" ]] || die "$LAB is not a directory"
  if [[ ! -f "$LAB/$MARK" && -n "$(ls -A "$LAB")" ]]; then
    die "$LAB holds files this script did not publish (no $MARK); empty it or pick another LOR_LAB_DIR"
  fi
fi

commit="$(git -C "$ROOT" rev-parse --short HEAD)"
[[ -z "$(git -C "$ROOT" status --porcelain)" ]] || commit="$commit (uncommitted changes)"

esc() { sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g' <<<"$1"; }

mkdir -p "$(dirname "$LAB")"
stage="$(mktemp -d "$(dirname "$LAB")/.lor-lab.XXXXXX")"
trap 'rm -rf "$stage"' EXIT
touch "$stage/$MARK"

items=""
published=0
for line in "${SOURCES[@]}"; do
  IFS='|' read -r src folder name <<<"$line"
  [[ "$src" == /* ]] || src="$ROOT/$src"
  if [[ ! -d "$src" ]]; then
    echo "publish: skip $folder ($src does not exist)"
    continue
  fi
  mkdir -p "$stage/$folder"
  cp -RL --preserve=timestamps "$src/." "$stage/$folder/"
  newest="$(find "$stage/$folder" -type f -printf '%T@\n' | sort -n | tail -n1)"
  when="$(date -d "@${newest%.*}" '+%Y-%m-%d %H:%M %Z' 2>/dev/null || echo "empty")"
  # A folder of several pages (the galleries, one per catalogue) links each of them.
  links="<a href=\"$folder/\">$(esc "$name")</a>"
  if [[ ! -f "$stage/$folder/index.html" ]]; then
    links="$(esc "$name"):"
    for page in "$stage/$folder"/*/index.html; do
      [[ -f "$page" ]] || continue
      sub="$(basename "$(dirname "$page")")"
      links+=" <a href=\"$folder/$sub/\">$(esc "$sub")</a>"
    done
  fi
  items+="<li>$links <span>built $when, published from $(esc "$commit")</span></li>"$'\n'
  published=$((published + 1))
  echo "publish: $folder <- $src"
done

cat >"$stage/index.html" <<HTML
<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LoR lab</title>
<style>
:root { color-scheme: dark; }
body { margin: 0; padding: 24px 16px; background: #0c0d12; color: #e8e6e1; font: 16px/1.5 system-ui, sans-serif; }
h1 { font-size: 20px; margin: 0 0 16px; color: #f08a2c; }
ul { list-style: none; padding: 0; margin: 0; } li { padding: 12px 0; border-bottom: 1px solid #2a2b33; }
a { color: #e8e6e1; font-size: 18px; } span { display: block; color: #9a978f; font-size: 13px; }
footer { margin-top: 24px; color: #9a978f; font-size: 13px; }
</style></head><body>
<h1>LoR lab</h1>
<ul>
${items:-<li>Nothing is published yet.</li>}
</ul>
<footer>Published $(date '+%Y-%m-%d %H:%M %Z') from $(esc "$commit").</footer>
</body></html>
HTML

chmod -R a+rX,go-w "$stage"
if [[ -e "$LAB" ]]; then
  old="$(mktemp -d "$(dirname "$LAB")/.lor-lab-old.XXXXXX")"
  mv "$LAB" "$old/lab"
  mv "$stage" "$LAB"
  rm -rf "$old"
else
  mv "$stage" "$LAB"
fi
trap - EXIT
echo "publish: $published source(s) at $LAB, served at /lab/"
