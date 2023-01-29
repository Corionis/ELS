#!/bin/bash
#
# Capture .els Hint files from test directory
#
#   -d  Change captured .els files status from Done to For
#   -r  Reset test data and restore .els files into it
#

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

name=`basename $0 .sh`

cd ../../test

echo 'Creating tree structure'
find . -type d | xargs -I{} mkdir -p "../output/capture/{}"

echo 'Copying .els files'
find . -name "*.els" -exec cp -p "{}" "../output/capture/{}" \;


if [[ $* == *"-d"* ]]; then
    echo 'Changing Done to For in .els files'
    cd ../output/capture
    find . -type f -name '*.els' -exec sed --in-place -e 's/Done/For/i' "{}" \;
fi

if [[ $* == *"-r"* ]]; then
    cd "$base"
    ./reset.sh -f

    mkdir "../../test/hints/datastore"

    cd "../../output/capture" 
    echo 'Copying .els files back'
    find . -name "*.els" -exec cp -p "{}" "../../test/{}" \;
fi

echo 'Done'
echo ''


