#!/bin/sh

set -eu

if test $# -lt 1; then
	echo "Usage: $0 <host>" 2>&1
	exit 1
fi

SCRIPTDIR="$( cd $(dirname $0); pwd )"

TARGET="$1"
ID="$( echo "$TARGET" | sed 's/^pve//' )"
VMIP="192.168.1.$(( 20 + $ID ))"
VMPASS="Passw0rd!"
VMUSER="IEUser"

sshpass -p "$VMPASS" scp -r "$SCRIPTDIR"/../startup-kit/* "$VMUSER@$VMIP:"
# XXX Install ranflood
