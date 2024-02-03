#!/bin/bash

base=`dirname $0`
cd ${base}


if [ -d ../../datastore-backup ]; then
    rm -rf ../../datastore-backup/*
fi

if [ -d ../../datastore ]; then
    cp -rp ../../datastore ../../datastore-backup
fi

if [ -d ../../test-backup ]; then
    rm -rf ../../test-backup/*
fi

cp -rp ../../test ../../test-backup


echo ""
echo -e "Backup done"
date
echo ""

