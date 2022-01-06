#!/bin/sh

try_in_system_shutdown() {
	VMID="$1"
	TIMEOUT="$2"
	VMIP="192.168.1.$(( $VMID - 100 + 20 ))"

	VMPASS="Passw0rd!"

	IP="192.168.1.$(( $VMID + 20 - 100 ))"
	sshpass -p "$VMPASS" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 "IEUser@$VMIP" shutdown /s /t 0  || true

	
	I=0
	while test "$I" -lt "$TIMEOUT"; do
		echo -n ","
		(qm status "$VMID" | grep -q stopped) && break
		sleep 1
		I="$(( $I + 1 ))"
	done

	test "$I" -lt "$TIMEOUT"
}

try_qm_shutdown() {
	HOST="$1"
	VMID="$2"
	ssh "$HOST" qm shutdown "$VMID"
}

stop_vm() {
	HOST="$1"
	VMID="$2"
	ssh "$HOST" qm stop "$VMID"
}

if test $# -lt 1 ; then
	echo "Usage: $0 <host>" 2>&1
	exit 1
fi

HOST="$1"
ID="$( echo "$HOST" | sed 's/pve//' )"
VMID="$(( $ID + 100 ))"
# 5 minutes of timeout
TIMEOUT="$(( 5 * 60 ))"

try_in_system_shutdown "$VMID" "$TIMEOUT" ||
	try_qm_shutdown "$HOST" "$VMID"   ||
	stop_vm "$HOST" "$VMID"
