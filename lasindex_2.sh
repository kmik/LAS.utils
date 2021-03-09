#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)
usage()
{
cat << EOF

----------------------------------------------
 lasindex (LASutils version 0.1)
----------------------------------------------

Indexes .las files for faster spatial queries. 
At the moment only increa

Usage:

	-i		Input file(s)

Optional arguments:

	Refer to manual
	     
EOF
}

NUMARG1=-999
NUMARG2=-999
NUMARG3=-999
NUMARG4=-999

INPUT="asd"
CORES=1

if [ $# == 0 ]; then
	
	usage
        exit 1

fi


while [ "$#" -gt 0 ]
do
CHECK=1
	if [ "$1" == "-i" ]; then
		CHECK=0
		INPUT=$2
		shift
		shift
	fi

	if [ "$1" == "-cores" ]; then
		CHECK=0
		CORES=$2
		shift
		shift
	fi

	if [ "$CHECK" == 1 ]; then
		echo "Error! Could not parse " $1	
		exit 0
	fi
done
echo ""
java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" runners.LASindex $INPUT $CORES

set +f

