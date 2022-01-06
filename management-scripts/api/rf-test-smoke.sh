#!/bin/sh

set -eu

test_vm() {
	echo "[ ] Starting"
	./rf-vm-start.sh "$1"
	echo "[ ] getting status"
	./rf-vm-get-status.sh
	echo "[ ] waiting"
	./rf-vm-ssh-wait.sh "$1"

	echo "[ ] initializing"
	./rf-vm-init.sh "$1"
	echo "[ ] pushing"
	./rf-vm-push-ransomware.sh "$1" ../start-ransomware.bat
	echo "[ ] running"
	./rf-vm-run-ransomware.sh "$1" "start-ransomware.bat"

	echo "[ ] shutdown"
	./rf-vm-shutdown.sh "$1"

	echo "[ ] extracting disk"
	./rf-vm-extract-disk.sh "$1" "/mnt/ransom"

	echo "[ ] checking"
	ID="$( echo "$1" | sed 's/^pve//' )"
	IP="$(( 20 + $ID ))"
	ssh "$1" "grep 192.168.1.$IP /mnt/ransom/Users/IEUser/out.txt"

	echo "[ ] unmounting"
	./rf-vm-umount-disk.sh "$1" "/mnt/ransom"

	echo "[ ] destroying"
	./rf-vm-destroy.sh "$1"
}

# Single vm
test_vm pve1
# Check speed rerun
for i in pve1 pve2 pve3 pve4 ; do
	test_vm "$i" &
done

wait
