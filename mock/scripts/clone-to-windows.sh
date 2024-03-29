#!/bin/bash
#
# Clone the 0* through 4* Linux scripts to DOS
#

out=win-test
if [ ! -e "$out" ]; then
  mkdir $out
fi

n=0
for f in linux/[01234]*.sh; do
  # get the java command
  cmd=`fgrep "rt/bin/java" $f`
  if [ -n "$cmd" ]; then
    # clean-up command
    cmd=`echo "$cmd"|sed -e 's#/#\\\\#g'`

    # get the output filename
    bat=`echo "$f"|sed -e 's#linux/##' -e 's#\.sh#.bat#'`
    echo "$bat"

    # write new file
    echo '@echo off' >$out/$bat
    echo '' >>$out/$bat
    echo 'set base=%~dp0' >>$out/$bat
    echo 'cd /d "%base%"' >>$out/$bat
    echo 'cd ..\..' >>$out/$bat
    echo '' >>$out/$bat
    echo "$cmd" >>$out/$bat
    echo '' >>$out/$bat
    echo 'cd /d "%base%"' >>$out/$bat
    echo '' >>$out/$bat

    unix2dos $out/$bat

    n=$((n + 1))

#    if [ 1 -eq 1 ]; then
#      break;
#    fi
  fi
done
echo "Process $n scripts"
