#!/bin/bash

base=`dirname $0`
cd ${base}


if [ -d ../../datastore-backup ]; then
    if [ -d ../../datastore ]; then
        rm -rf ../../datastore/*
    fi

    cp -rp ../../datastore-backup/* ../../datastore
else
    echo 'datastore-backup does not exist'
fi

if [ -d ../../test-backup ]; then
    if [ -d ../../test ]; then
        rm -rf ../../test/*
    fi

    cp -rp ../../test-backup/* ../../test
else
    echo 'test-backup does not exist'
fi


echo ""
echo -e "Restore done"
date
echo ""

