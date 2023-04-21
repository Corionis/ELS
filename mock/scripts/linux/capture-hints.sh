#!/bin/bash
#
# Capture .els Hint files from test directory
#
#   -c  Capture .els files and datastore
#   -d  Change captured .els files status from Done to For
#   -r  Reset test data and restore .els files into it
#

base=$PWD
cd "$base"

name=`basename $0 .sh`

cd ../../test

if [[ $* == *"-c"* ]]; then
    if [ -d ../capture ]; then
        rm -rf ../capture
    fi

    echo 'Creating tree structure'
    find . -type d | xargs -I{} mkdir -p "../capture/{}"

    echo 'Copying .els files'
    find . -name "*.els" -exec cp -p "{}" "../capture/{}" \;


    if [ -d ../datastore ]; then
        cd ..
        if [ -e capture-ds ]; then
            rm -rf capture-ds
        fi
        mkdir capture-ds
        cp -rp datastore/* capture-ds/
    fi
fi

if [[ $* == *"-d"* ]]; then
    echo 'Changing Done to For in .els files'
    cd ../capture
    find . -type f -name '*.els' -exec sed --in-place -e 's/Done/For/i' "{}" \;

    if [ -d ../datastore ]; then
        cd ../capture-ds
        find . -type f -name '*.els' -exec sed --in-place -e 's/Done/For/i' "{}" \;
    fi
    cd ..
fi

if [[ $* == *"-r"* ]]; then
    cd "$base"
    ./reset.sh -f

    cd ../../capture
    echo 'Copying .els files back'
    find . -name "*.els" -exec cp -p "{}" "../test/{}" \;

    cd ..
    if [ -e capture-ds ]; then
        mkdir datastore
        cp -rp capture-ds/* datastore/
    fi
fi

cd "$base"

echo 'Done'
echo ''


