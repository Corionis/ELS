#!/bin/bash

base=`dirname $0`
cd ${base}

if [ "$1" != "-f" ]; then
    if [ -e ./TestRun ]; then
        echo ""
        echo "Reset TestRun Directory"
        read -p "Confirm: DESTROY TestRun directory and recreate from templates (y/N)? " R
        R=${R:0:1}
        if [ "$R" != 'y' -a "$R" != 'Y' ]; then
            echo -e "Cancelled\n"
            exit 1
        fi
    fi
fi

rm -rf ./TestRun

cp -rpv ./Template_Copy-Only ./TestRun

echo -e "Done\n"
