#!/bin/sh

set -eu

if test $# -lt 1; then
	echo "Usage: $0 <host>" >&2
	exit 1
fi

TARGET_HOST="$1"
TARGETID="$(echo "$TARGET_HOST" | sed 's/^pve//')"
TEMPLATEID="$(( $TARGETID + 9000 ))"
VMID="$(( $TARGETID + 100 ))"

ssh "$TARGET_HOST" "qm clone $TEMPLATEID $VMID"
ssh "$TARGET_HOST" "qm start $VMID"
