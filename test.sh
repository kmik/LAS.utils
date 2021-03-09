#!/bin/bash
set -f
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)

ENDLOOP=0
INPUT="asd"
while [ "$#" -gt 0 ]
do
CHECK=1
	if [ "$1" == "-i" ]; then
		CHECK=0
		INPUT="\"$2"
		shift
		TEMP=$2
		while [ $ENDLOOP == 0 ]
		do
			if [[ ${TEMP:0:1} =~ "-" ]]; then
   				ENDLOOP=1
			fi

			if [ $ENDLOOP != 1 ]; then
				INPUT="$INPUT $2"
			
			fi
			shift
			TEMP=$2
			#echo $TEMP
			if [ "$#" == 0 ]; then
				ENDLOOP=1
			fi
			
		done	
		INPUT="$INPUT\""
		echo $INPUT
	fi
	echo $1
	
done

echo $INPUT
