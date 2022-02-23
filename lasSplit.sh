#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)

java -Xmx16g -XX:ParallelGCThreads=4 -XX:ConcGCThreads=4 -cp ".:$curDir/lib/*:$curDir/target/" lasSplit $@

set +f

