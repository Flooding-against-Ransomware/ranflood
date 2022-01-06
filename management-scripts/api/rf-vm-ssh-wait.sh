#!/bin/sh

set -eu

if test $# -lt 1; then
	echo "Usage: $0 <host>" 2>&1
	exit 1
fi

TARGET="$1"
TARGETID="$(echo "$TARGET" | sed 's/^pve//')"

VMIP="192.168.1.$(( 20 + $TARGETID ))"
VMPASS="Passw0rd!"

echo "Waiting for ssh"
while ! sshpass -p "$VMPASS" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 "IEUser@$VMIP" dir 2>/dev/null >/dev/null; do
	echo -n "."
	sleep 1;
done
echo ""
