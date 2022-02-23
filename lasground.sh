#!/bin/bash
set -f
curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib:$curDir/gdal/java
export GDAL_DATA=$curDir/gdal/gdal_data
#echo "current dir: " $curDir


java -Xmx16g -XX:ParallelGCThreads=4 -XX:ConcGCThreads=4 -cp ".:$curDir/lib/*:$curDir/target/" lasground $@

set +f

