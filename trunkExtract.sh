#!/bin/bash
set -f
curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib:$curDir/gdal/java
export GDAL_DATA=$curDir/gdal/gdal_data
echo $LD_LIBRARY_PATH 

java -Djava.library.path=$curDir/gdal/java -cp ".:$curDir/lib/*:$curDir/target/" runners.RunLASutils 26 $@

set +f

