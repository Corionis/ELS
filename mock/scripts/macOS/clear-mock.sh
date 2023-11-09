#!/bin/bash

base=`dirname $0`
cd ${base}

if [ "$1" != "-f" ]; then
    echo ""
    echo "Clear mock directory"
    read -p "Confirm: DESTROY all test configuration and data directories (Y/n)? " R
    R=${R:0:1}
    if [ "$R" = 'n' -o "$R" = 'N' ]; then
        echo -e "Cancelled\n"
        exit 1
    fi
fi

if [ -d ../../output ]; then
    rm -f ../../output
fi
rm -f ../../bin/*.jar ../../bin/*.info
rm -rf ../../datastore
rm -rf ../../jobs
rm -rf ../../libraries
rm -rf ../../local
rm -rf ../../output
rm -rf ../../system
rm -rf ../../test
rm -rf ../../tools
rm -f ../../*.sh ../../*.txt

echo ""
echo -e "Clear done"
date
echo ""

