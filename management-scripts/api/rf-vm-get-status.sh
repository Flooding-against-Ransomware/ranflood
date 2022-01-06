#!/bin/sh

set -eu

VMID=10

for i in pve1 pve2 pve3 pve4 ; do
	STATUS="$(ssh "$i" "qm list" | awk "/$VMID/{print \$3}")"
	if test "x$STATUS" != "x"; then
		echo "$i $STATUS"
	fi
	VMID="$(( $VMID + 1 ))"
done
