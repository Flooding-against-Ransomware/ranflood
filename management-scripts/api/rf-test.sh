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

	./rf-vm-push-ransomware.sh "$1" ../start-ransomware-$2-$3-$4.bat
	echo "[ ] running"
	./rf-vm-run-ransomware.sh "$1" "start-ransomware-$2-$3-$4.bat"

	echo "[ ] shutdown"
	./rf-vm-shutdown.sh "$1"

	echo "[ ] extracting disk"
	./rf-vm-extract-disk.sh "$1" "/mnt/ransom"

	echo "[ ] checking"
	ID="$( echo "$1" | sed 's/^pve//' )"
	IP="$(( 20 + $ID ))"
	ssh "$1" "grep 192.168.1.$IP /mnt/ransom/Users/IEUser/out.txt"

	# OUT="../$@.txt"
	# for f in /mnt/ransom/Users/IEUser/....; do
	# 	if ! grep $(md5sum $f | cut -f 1 -d ' ') ../hash-db.txt; then
	#		echo "Corrupted $f" >> $OUT
	#	else
	#		echo "Ok $f" >> $OUT
	#	fi
	# done

	echo "[ ] unmounting"
	./rf-vm-umount-disk.sh "$1" "/mnt/ransom"

	echo "[ ] destroying"
	./rf-vm-destroy.sh "$1"
}

generate_script() {
	# XXX TODO
	echo "$1" "$2" "$3" > startup-ransomware....
}

# Check speed rerun

RANSOMWARES=( ... )

for mod in "random" "shadow" "onthefly"; do
	for t in $(( 5*60 )) $(( 10 * 60 )) $(( 15 * 60 )); then
		for i in "${RANSOMWARES[@]}"; do
			generate_script "$i" "$t" "$mod"
			for j in $(seq 5); do
			baserun="$(( j * 4 ))"
				test_vm "pve1" "$i" "$t" "$mod" "$(( $baserun + 0 ))" &
				test_vm "pve2" "$i" "$t" "$mod" "$(( $baserun + 1 ))" &
				test_vm "pve3" "$i" "$t" "$mod" "$(( $baserun + 2 ))" &
				test_vm "pve4" "$i" "$t" "$mod" "$(( $baserun + 3 ))" &
				wait
			done
		done
	done
done
