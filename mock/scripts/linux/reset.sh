#!/bin/bash

base=`dirname $0`
cd ${base}
cd ../..

if [ "$1" != "-f" ]; then
    if [ -e ./TestRun ]; then
        echo ""
        echo "Reset Test Directory"
        read -p "Confirm: DESTROY Test directory and recreate from templates (y/N)? " R
        R=${R:0:1}
        if [ "$R" != 'y' -a "$R" != 'Y' ]; then
            echo -e "Cancelled\n"
            exit 1
        fi
    fi
fi

rm -rf ./test
rm -f ./*.log

cp -rpv ./media-base_copy-only ./test
echo ""

echo -e "Done\n"

