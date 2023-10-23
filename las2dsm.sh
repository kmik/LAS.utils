#!/bin/bash
set -f
curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
file_p=$curDir/gdal_paths
GDAL_JAVA_PATH=$(sed '2q;d' $file_p)
LIBGDAL_PATH=$(sed '4q;d' $file_p)
GDAL_DATA_PATH=$(sed '6q;d' $file_p)

export LD_LIBRARY_PATH=$GDAL_PATH:$GDAL_DATA_PATH:$GDAL_JAVA_PATH:$LIBGDAL_PATH:$LD_LIBRARY_PATH
export GDAL_DATA=$GDAL_DATA_PATH

# Get the current date and time
current_datetime=$(date +"%Y-%m-%d_%H-%M-%S")

# Get the directory from which the script was called
callDir="$PWD"
java -Xmx16g -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:ConcGCThreads=8 -cp ".:$curDir/lib/*:$curDir/target/:$GDAL_JAVA_PATH/*" las2dsm $@ 2> $callDir/las2dsm_$current_datetime.log
 
set +f

