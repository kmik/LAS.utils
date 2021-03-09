#!/bin/bash


set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)
#echo "current dir: " $curDir

if [ $# == 0 ]; then
	
	bash $curDir/helps.sh 7
	exit 1
fi




#echo $(bash ./parser.sh $@)

PARSER=$(bash $curDir/parser.sh $@)
LENGTH_OF_PARSER=${#PARSER} 
#echo "$LENGTH_OF_PARSER "
if [ "$LENGTH_OF_PARSER " -lt 50 ]; then
	
	exit 1
fi



java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" runners.RunLASutils 8 $PARSER

set +f

