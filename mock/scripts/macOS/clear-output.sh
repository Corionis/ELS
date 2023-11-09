#!/bin/bash

base=`dirname $0`
cd ${base}

if [ "$1" != "-f" ]; then
    echo ""
    echo "Clear output directory"
    read -p "Confirm: DESTROY the logs in the output directory (Y/n)? " R
    R=${R:0:1}
    if [ "$R" != 'y' -a "$R" != 'Y' ]; then
        echo -e "Cancelled\n"
        exit 1
    fi
fi

if [ -d ../../output ]; then
    rm -f ../../output/*
fi

echo ""
echo -e "Clear done"
date
echo ""

