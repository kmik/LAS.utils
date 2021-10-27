#!/bin/bash
set -f
curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
#export GDAL_DATA=$curDir/gdal/gdal_data

#echo "current dir: " $curDir


java -Djava.library.path="usr/local/lib/" -cp ".:$curDir/lib/*:$curDir/target/:$curDir/lib/gdal/" runners.RunLASutils 7 $@

set +f

