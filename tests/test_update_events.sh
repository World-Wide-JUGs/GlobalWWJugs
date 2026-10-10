#!/usr/bin/env bash
set -euo pipefail

files=$(find content/jugs -maxdepth 1 -name '*_events.json' -type f | wc -l | tr -d ' ')
references=$(awk '/^event_list: content\/jugs\/.*_events\.json$/ { count++ } END { print count + 0 }' content/jugs/*.md)
[[ "$files" -eq "$references" ]] || {
  printf 'event file/reference mismatch: %s files, %s references\n' "$files" "$references" >&2
  exit 1
}

for file in content/jugs/*_events.json; do
  jq empty "$file"
  jq -e 'has("events") and (has("last_event") | not) and (has("next_event") | not)' "$file" >/dev/null
done

printf 'validated %s history files and references\n' "$files"
