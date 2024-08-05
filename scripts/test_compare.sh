#!/bin/bash

TESTSITE=/tmp/ranflood_testsite
ATTACKSITE="$TESTSITE/attackedFolder"

mkdir "$TESTSITE" "$ATTACKSITE"
cp src/tests/java/playground/compare/settings*.ini "$TESTSITE"


./gradlew build
./gradlew testCompareJar

for SETTINGS in $@
do
	echo "Running with settings file: $SETTINGS"
	java -Xmx6g -jar build/libs/testCompare.jar $SETTINGS
done
