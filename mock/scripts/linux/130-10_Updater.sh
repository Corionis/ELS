#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi

cd "/tmp/ELS_Updater_${USER}"

# rt/bin/java -jar bin/ELS_Updater.jar -Y $*


# Assumes 130-02_InstallUpdate.sh has been run
/tmp/ELS_Updater_${USER}/rt/bin/java -jar /tmp/ELS_Updater_${USER}/bin/ELS_Updater.jar -Y $*
echo "Status: $?"

cd "$base"

