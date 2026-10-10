#!/usr/bin/env bash
set -euo pipefail

ROOT=${1:-.}
JUG_DIR="$ROOT/content/jugs"
TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/globalwwjugs-events.XXXXXX")
trap 'rm -rf "$TMP_DIR"' EXIT

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    printf 'Required command not found: %s\n' "$1" >&2
    exit 1
  }
}

for command in curl jq awk date; do
  require_command "$command"
done

frontmatter_value() {
  local file=$1 key=$2
  awk -v key="$key" '
    NR == 1 { in_frontmatter = ($0 == "---"); next }
    in_frontmatter && $0 == "---" { exit }
    in_frontmatter && index($0, key ":") == 1 {
      value = substr($0, length(key) + 3)
      gsub(/"/, "", value)
      gsub(/\047/, "", value)
      print value
      exit
    }
  ' "$file"
}

meetup_ical_url() {
  local url=$1
  case "$url" in
    https://www.meetup.com/*|https://meetup.com/*)
      url=${url%/}
      case "$url" in
        */events/ical) printf '%s/\n' "$url" ;;
        *) printf '%s/events/ical/\n' "$url" ;;
      esac
      ;;
  esac
}

iso_date() {
  local value=$1
  if [[ "$value" =~ ^[0-9]{8}T[0-9]{6}Z$ ]]; then
    if date -u -d "$value" '+%Y-%m-%dT%H:%M:%S+00:00' 2>/dev/null; then
      return
    fi
    date -u -j -f '%Y%m%dT%H%M%SZ' "$value" '+%Y-%m-%dT%H:%M:%S+00:00'
  elif [[ "$value" =~ ^[0-9]{8}$ ]]; then
    if date -u -d "$value" '+%Y-%m-%dT00:00:00+00:00' 2>/dev/null; then
      return
    fi
    date -u -j -f '%Y%m%d' "$value" '+%Y-%m-%dT00:00:00+00:00'
  fi
}

parse_ical() {
  local file=$1 kind=$2 source_url=$3 output=$4
  awk -v kind="$kind" -v source_url="$source_url" '
    function emit() {
      if (start != "") printf "%s\t%s\t%s\t%s\t%s\t%s\n", uid, start, summary, url, location, kind "\t" source_url
      uid = start = summary = url = location = ""
    }
    /^BEGIN:VEVENT/ { in_event = 1; next }
    /^END:VEVENT/ { if (in_event) emit(); in_event = 0; next }
    in_event && /^UID:/ { uid = substr($0, 5); next }
    in_event && /^DTSTART/ { split($0, parts, ":"); start = parts[2]; next }
    in_event && /^SUMMARY:/ { summary = substr($0, 9); next }
    in_event && /^URL:/ { url = substr($0, 5); next }
    in_event && /^LOCATION:/ { location = substr($0, 11); next }
  ' "$file" > "$output"
}

json_escape_event() {
  local uid=$1 start=$2 summary=$3 url=$4 location=$5 kind=$6 source_url=$7
  jq -n \
    --arg uid "$uid" --arg start "$start" --arg title "$summary" \
    --arg url "$url" --arg location "$location" --arg source_kind "$kind" \
    --arg source_url "$source_url" \
    '{uid:$uid,start:$start,title:$title,url:$url,location:$location,source_kind:$source_kind,source_url:$source_url}'
}

for markdown in "$JUG_DIR"/*.md; do
  calendar=$(frontmatter_value "$markdown" calendar || true)
  meetup=$(frontmatter_value "$markdown" meetup || true)
  [[ -n "$calendar" || -n "$meetup" ]] || continue

  id=$(basename "$markdown" .md)
  event_file="$JUG_DIR/${id}_events.json"
  existing='[]'
  if [[ -f "$event_file" ]]; then
    existing=$(jq -c '(.events // []) + ([.last_event, .next_event] | map(select(. != null)))' "$event_file")
  fi

  sources_file="$TMP_DIR/${id}.sources"
  : > "$sources_file"
  [[ -n "$calendar" ]] && printf 'calendar\t%s\n' "$calendar" >> "$sources_file"
  [[ -n "$meetup" ]] && printf 'meetup\t%s\n' "$meetup" >> "$sources_file"
  if [[ -n "$meetup" ]]; then
    derived=$(meetup_ical_url "$meetup" || true)
    [[ -n "$derived" ]] && printf 'meetup-ical\t%s\n' "$derived" >> "$sources_file"
  fi

  sources=$(jq -Rn '[inputs | split("\t") | {kind:.[0],url:.[1]}]' < "$sources_file")
  discovered='[]'
  errors='[]'
  while IFS=$'\t' read -r kind source_url; do
    [[ -n "$source_url" ]] || continue
    response="$TMP_DIR/${id}.${kind}.response"
    body="$TMP_DIR/${id}.${kind}.body"
    http_code=$(curl -L -sS --max-time 30 -A 'GlobalWWJugs event updater/1.0' \
      -H 'Accept: text/calendar,text/plain,text/html' -o "$body" -w '%{http_code}' "$source_url" || printf '000')
    if [[ ! "$http_code" =~ ^2 ]]; then
      errors=$(jq -c --arg kind "$kind" --arg url "$source_url" --arg error "HTTP status $http_code" \
        '. + [{kind:$kind,url:$url,error:$error}]' <<< "$errors")
      continue
    fi
    grep -q 'BEGIN:VCALENDAR' "$body" 2>/dev/null || continue
    parsed="$TMP_DIR/${id}.${kind}.parsed"
    parse_ical "$body" "$kind" "$source_url" "$parsed"
    while IFS=$'\t' read -r uid raw_start summary url location source_kind source_url_value; do
      [[ -n "$raw_start" ]] || continue
      start=$(iso_date "$raw_start" || true)
      [[ -n "$start" ]] || continue
      event=$(json_escape_event "$uid" "$start" "$summary" "$url" "$location" "$source_kind" "$source_url_value")
      discovered=$(jq -c --argjson event "$event" '. + [$event]' <<< "$discovered")
    done < "$parsed"
  done < "$sources_file"

  events=$(jq -c --argjson old "$existing" --argjson new "$discovered" \
    '($old + $new) | unique_by([.uid // "", .start // "", .source_url // ""]) | sort_by([.start // "", .uid // ""])' <<< 'null')
  generated_at=$(date -u '+%Y-%m-%dT%H:%M:%S+00:00')
  status=ok
  [[ "$errors" == '[]' ]] || status=fetch_error
  jq -n --arg id "$id" --arg name "$(frontmatter_value "$markdown" name)" \
    --arg country "$(frontmatter_value "$markdown" country)" --arg source_file "content/jugs/${id}.md" \
    --arg fetched_at "$generated_at" --arg status "$status" --argjson events "$events" \
    --argjson errors "$errors" --argjson sources "$sources" \
    '{schema_version:1,id:$id,name:$name,country:$country,source_file:$source_file,fetched_at:$fetched_at,sources:$sources,events:$events,status:$status} + (if ($errors|length)>0 then {errors:$errors} else {} end)' \
    > "$event_file"
done

printf 'Updated per-JUG event histories beside their Markdown records\n'
