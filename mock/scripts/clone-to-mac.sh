#!/bin/bash
#
# Clone the 0* through 4* Linux scripts to macOS
#

out=mac-test
if [ ! -e "$out" ]; then
  mkdir $out
fi

n=0
for f in linux/[01234]*.sh; do
  # get the java command
  cmd=`fgrep "rt/bin/java" $f`
  if [ -n "$cmd" ]; then
    # clean-up command
    cmd=`echo "$cmd"|sed -e 's#rt/bin/##g'`
    cmd="rt/Contents/Home/bin/$cmd"

    # get the output filename
    mac=`echo "$f"|sed -e 's#linux/##'`
    echo "$mac"

    # write new file
    echo '#!/bin/bash' >$out/$mac
    echo '' >>$out/$mac
    echo 'base=`dirname $0`' >>$out/$mac
    echo 'if [ "$base" = "." ]; then' >>$out/$mac
    echo '    base=$PWD' >>$out/$mac
    echo 'fi' >>$out/$mac
    echo 'cd "$base"' >>$out/$mac
    echo '' >>$out/$mac
    echo 'cd ../..' >>$out/$mac
    echo '' >>$out/$mac
    echo "$cmd" >>$out/$mac
    echo '' >>$out/$mac

    #unix2mac $out/$mac

    n=$((n + 1))

#    if [ 1 -eq 1 ]; then
#      break;
#    fi
  fi
done
echo "Process $n scripts"
