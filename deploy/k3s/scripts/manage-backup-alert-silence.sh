#!/usr/bin/env bash

set -Eeuo pipefail
. "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/common.sh"

[ "$#" -eq 2 ] || die "사용법: $0 <start|stop> <silence-id 파일>"
action=$1
state_file=$2

require_command curl
require_command ruby

alertmanager_ip=$(kube -n "$NAMESPACE" get service alertmanager \
    -o jsonpath='{.spec.clusterIP}')
[ -n "$alertmanager_ip" ] && [ "$alertmanager_ip" != "None" ] \
    || die "Alertmanager ClusterIP를 확인할 수 없습니다."
alertmanager_url="http://$alertmanager_ip:9093"

case "$action" in
    start)
        [ ! -e "$state_file" ] \
            || die "이전 백업 AppDown silence 상태 파일이 남아 있습니다: $state_file"
        payload=$(ruby -rjson -rtime -e '
          now = Time.now.utc
          puts JSON.generate(
            matchers: [
              {name: "alertname", value: "AppDown", isRegex: false, isEqual: true}
            ],
            startsAt: now.iso8601,
            endsAt: (now + 45 * 60).iso8601,
            createdBy: "happygallery-backup.service",
            comment: "DB와 미디어의 일관 백업을 위한 계획 app 중단"
          )
        ')
        response=$(curl --fail --silent --show-error \
            -H 'Content-Type: application/json' \
            -d "$payload" \
            "$alertmanager_url/api/v2/silences")
        silence_id=$(printf '%s' "$response" | ruby -rjson -e '
          parsed = JSON.parse(STDIN.read)
          value = parsed["silenceID"]
          abort unless value.is_a?(String) && value.match?(/\A[0-9a-f-]{36}\z/)
          print value
        ') || die "Alertmanager가 유효한 silence ID를 반환하지 않았습니다."
        umask 077
        printf '%s\n' "$silence_id" > "$state_file"
        info "계획 백업 동안 AppDown 경보를 최대 45분 silence 처리합니다."
        ;;
    stop)
        [ -f "$state_file" ] || exit 0
        silence_id=$(cat "$state_file")
        printf '%s' "$silence_id" | grep -Eq '^[0-9a-f-]{36}$' \
            || die "저장된 Alertmanager silence ID가 올바르지 않습니다."
        curl --fail --silent --show-error \
            -X DELETE \
            "$alertmanager_url/api/v2/silence/$silence_id" >/dev/null
        rm -f "$state_file"
        info "계획 백업 AppDown silence를 해제했습니다."
        ;;
    *)
        die "지원하지 않는 동작입니다: $action"
        ;;
esac
