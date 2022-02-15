#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)

java -Xmx16g -cp ".:$curDir/lib/*:$curDir/target/" lasSplit $@

set +f

