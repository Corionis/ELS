#!/bin/bash
# Do not change or delete this script.
# It is used by processes in ELS.
# It is replaced during updates.

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

name=`basename $0 .sh`

if [ $# -gt 0 ]; then
    "${base}/rt/bin/java" -jar "${base}/bin/ELS.jar" "$@"
else
    "${base}/rt/bin/java" -jar "${base}/bin/ELS.jar" -C "${base}" -n -c Debug -d Debug -F "output/${name}.log"
fi
