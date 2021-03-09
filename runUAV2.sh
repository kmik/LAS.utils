#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)

java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" tools.uav /media/koomikko/LaCie/align_example/foto.las /media/koomikko/LaCie/align_example/lidar.las

set +f



