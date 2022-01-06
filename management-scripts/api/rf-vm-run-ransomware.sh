#!/bin/sh

set -eu

if test $# -lt 2; then
	echo "Usage: $0 <host> <ransomware entry point>" 2>&1
	exit 1
fi

SCRIPTDIR="$( cd $(dirname $0); pwd )"

TARGET="$1"
ID="$( echo "$TARGET" | sed 's/^pve//' )"
VMIP="192.168.1.$(( 20 + $ID ))"
VMPASS="Passw0rd!"
VMUSER="IEUser"

sshpass -p "$VMPASS" ssh "$VMUSER@$VMIP" "cmd /c $2"
