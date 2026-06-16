#!/usr/bin/env bash
set -euo pipefail

output="backend/.env.generated"
files=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o|--output)
      output="$2"
      shift 2
      ;;
    *)
      files+=("$1")
      shift
      ;;
  esac
done

if [[ ${#files[@]} -eq 0 ]]; then
  files=("backend/src/main/resources/application.yml")
fi

pattern='\$\{[A-Z0-9_]+(:[^}]*)?\}'
if command -v rg >/dev/null 2>&1; then
  matches=$(rg -o -N "$pattern" "${files[@]}" || true)
else
  matches=$(grep -oE "$pattern" "${files[@]}" || true)
fi

mkdir -p "$(dirname "$output")"
{
  echo "# Generated from Spring YAML placeholders"
  if [[ -n "${matches}" ]]; then
    printf "%s\n" "$matches" \
      | sed -e 's/^${//' -e 's/}$//' \
      | awk '
function quote(v) {
  if (v == "") return ""
  if (v ~ /^[A-Za-z0-9_./:-]+$/) return v
  gsub(/\\/, "\\\\", v)
  gsub(/"/, "\\\"", v)
  return "\"" v "\""
}
{
  line = $0
  pos = index(line, ":")
  if (pos > 0) {
    name = substr(line, 1, pos - 1)
    val = substr(line, pos + 1)
  } else {
    name = line
    val = ""
  }
  if (!(name in seen)) {
    order[++n] = name
    seen[name] = val
  }
}
END {
  for (i = 1; i <= n; i++) {
    name = order[i]
    val = seen[name]
    print name "=" quote(val)
  }
}'
  fi
} > "$output"

echo "Wrote ${output}"
