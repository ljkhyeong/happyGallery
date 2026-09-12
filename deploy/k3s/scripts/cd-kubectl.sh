#!/usr/bin/env sh
set -eu
exec sudo -n /usr/local/bin/k3s kubectl "$@"
