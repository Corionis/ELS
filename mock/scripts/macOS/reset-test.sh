#!/bin/bash

base=`dirname $0`
cd ${base}
cd ../..

if [ "$1" != "-f" ]; then
    if [ -e ./test ]; then
        echo ""
        echo "Reset Test Data"
        read -p "Confirm: DESTROY Test data directories and recreate from templates (y/N)? " R
        R=${R:0:1}
        if [ "$R" != 'y' -a "$R" != 'Y' ]; then
            echo -e "Cancelled\n"
            exit 1
        fi
    fi
fi

rm -rf ./test
rm -f ./*.log
rm -f ./*received*.json
rm -f ./*generated*.json

cp -rpv ./copy-only_media-base ./test
echo ""

echo -e "Reset test data done"
date
echo ""

