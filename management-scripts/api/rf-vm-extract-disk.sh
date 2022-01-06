#!/bin/sh

set -eu

if test $# -lt 1; then
	echo "Usage: $0 <host> <mountpoint>" 2>&1
	exit 1
fi

SCRIPTDIR="$( cd $(dirname $0); pwd )"

TARGET="$1"
ID="$( echo "$TARGET" | sed 's/^pve//' )"
VMID="$(( 100 + $ID ))"

ssh "$TARGET" mkdir -p "$2"
ssh "$TARGET" mount -o offset="$(( 2048 * 512 ))" "/dev/mapper/pve-vm--$VMID--disk--0" "$2"
