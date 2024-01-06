#!/bin/bash

base=`dirname $0`
cd ${base}

if [ "$1" != "-f" ]; then
    echo ""
    echo "Reset Test Configuration and Data"
    read -p "Confirm: DESTROY Test Configuration and Data directories and recreate from templates (y/N)? " R
    R=${R:0:1}
    if [ "$R" != 'y' -a "$R" != 'Y' ]; then
        echo -e "Cancelled\n"
        exit 1
    fi
fi

./reset-config.sh $1
./reset-test.sh $1

if [ -e ../../../build/ELS.jar ]; then
	cp ../../../build/ELS.jar ../../bin
fi

echo ""
