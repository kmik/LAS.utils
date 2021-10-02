#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)
#echo "current dir: " $curDir


java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" runners.RunLASutils 13 $@


set +f

