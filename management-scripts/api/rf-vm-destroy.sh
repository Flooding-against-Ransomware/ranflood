#!/bin/sh

set -eu

if test $# -lt 1; then
	echo "Usage: $0 <host>" 2>&1
	exit 1
fi

HOST="$1"
ID="$( echo "$HOST" | sed 's/^pve//' )"
VMID="$(( $ID + 100 ))"

ssh "$HOST" qm destroy "$VMID"
