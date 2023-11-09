#!/bin/bash

base=`dirname $0`
cd ${base}
cd ../..

if [ "$1" != "-f" ]; then
    if [ -e ./test ]; then
        echo ""
        echo "Reset Test Configuration"
        read -p "Confirm: DESTROY Configuration directories and recreate from templates (y/N)? " R
        R=${R:0:1}
        if [ "$R" != 'y' -a "$R" != 'Y' ]; then
            echo -e "Cancelled\n"
            exit 1
        fi
    fi
fi

rm -rf ./bin
rm -rf ./datastore
rm -rf ./jobs
rm -rf ./libraries
rm -rf ./local
rm -rf ./output
# rm -rf ./rt
rm -rf ./system
rm -rf ./tools

cp -rpv ./copy-only_config-base/* .
cp -rpv ./copy-only_linux-base/* .
if [ -e ../deploy/ELS.jar ]; then
    cp -pv ../deploy/ELS.jar bin
fi
if [ -e ../deploy/ELS_Updater.jar ]; then
    cp -pv ../deploy/ELS_Updater.jar bin
fi
if [ -e ../deploy/update.info ]; then
    cp -pv ../deploy/update.info bin
fi
if [ -e ../deploy/version.info ]; then
    cp -pv ../deploy/version.info bin
fi
echo ""

echo -e "Reset configuration done"
date
echo ""

