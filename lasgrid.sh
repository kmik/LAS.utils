#!/bin/bash

set -f

usage()
{
cat << EOF

----------------------------------------------
 lasgrid (LASutils version 0.1)
----------------------------------------------

...

Usage:

	-i		Input file(s)
	-o		Output file
	-odir		Output directory
	-iparse		...
	-sep 		txt file field separator
	-step		The n x n dimension of a
			grid cell.

Optional arguments:

	Refer to manual
	     
EOF
}

OTYPE=-999

NUMARG1=-999
NUMARG2=-999
NUMARG3=-999
NUMARG4=-999

CORES=1
SEP="\s"
IPARSE="xyz"
OPARSE="xyz"
OUTPUT="asd"
INPUT="asd"

DROPNOISE=0
DROPCLASSIFICATION=-999

FIRSTONLY=0
KEEPFIRST=0
DROPFIRST=0

LASTONLY=0
KEEPLAST=0
DROPLAST=0

DROPFIRSTOFMANY=0
DROPLASTOFMANY=0

KEEPMIDDLE=0
DROPMIDDLE=0

KEEPSINGLE=0
DROPSINGLE=0
KEEPDOUBLE=0
DROPDOUBLE=0
KEEPTRIPLE=0
DROPTRIPLE=0
KEEPQUADRUPLE=0
DROPQUADRUPLE=0
KEEPQUINTUPLE=0
DROPQUINTUPLE=0

DROPSYNTHETIC=0

DROPUSERDATA=-999
KEEPUSERDATA=-999

SETCLASSIFICATION=-999
SETUSERDATA=-999

DROPZBELOW=-999
DROPZABOVE=-999

BUFFER=0
ODIR="null"

FEW=2
STEP=2

GROUND_CLASS=-1
METHOD="null"

PLOT="null"

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

	if [ "$1" == "-o" ]; then
		CHECK=0
		OUTPUT=$2
		shift
		shift
	fi

	if [ "$1" == "-drop_z_below" ]; then
		CHECK=0
		DROPZBELOW=$2
		shift
		shift
	fi

	if [ "$1" == "-drop_z_above" ]; then
		CHECK=0
		DROPZABOVE=$2
		shift
		shift
	fi

	if [ "$1" == "-o" ]; then
		CHECK=0
		OUTPUT=$2
		shift
		shift
	fi	

	if [ "$1" == "-iparse" ]; then
		CHECK=0
		IPARSE=$2
		shift
		shift
	fi

	if [ "$1" == "-oparse" ]; then
		CHECK=0
		OPARSE=$2
		shift
		shift
	fi

	if [ "$1" == "-drop_classification" ]; then
		CHECK=0
		DROPCLASSIFICATION=$2
		shift
		shift
	fi

	if [ "$1" == "-drop_noise" ]; then
		CHECK=0
		DROPNOISE=1
		shift
	fi

	if [ "$1" == "-first_only" ]; then
		CHECK=0
		FIRSTONLY=1
		shift
	fi

	if [ "$1" == "-keep_first" ]; then
		CHECK=0
		KEEPFIRST=1
		shift
	fi

	if [ "$1" == "-drop_first" ]; then
		CHECK=0
		DROPFIRST=1
		shift
	fi

	if [ "$1" == "-last_only" ]; then
		CHECK=0
		LASTONLY=1
		shift
	fi

	if [ "$1" == "-keep_last" ]; then
		CHECK=0
		KEEPLAST=1
		shift
	fi

	if [ "$1" == "-drop_last" ]; then
		CHECK=0
		DROPLAST=1
		shift
	fi

	if [ "$1" == "-drop_last_of_many" ]; then
		CHECK=0
		DROPLASTOFMANY=1
		shift
	fi

	if [ "$1" == "-drop_first_of_many" ]; then
		CHECK=0
		DROPFIRSTOFMANY=1
		shift
	fi

	if [ "$1" == "-keep_middle" ]; then
		CHECK=0
		KEEPMIDDLE=1
		shift
	fi

	if [ "$1" == "-drop_middle" ]; then
		CHECK=0		
		DROPMIDDLE=1
		shift
	fi

	if [ "$1" == "-keep_single" ]; then
		CHECK=0
		KEEPSINGLE=1
		shift
	fi

	if [ "$1" == "-drop_single" ]; then
		CHECK=0
		DROPSINGLE=1
		shift
	fi

	if [ "$1" == "-keep_double" ]; then
		CHECK=0
		KEEPDOUBLE=1
		shift
	fi

	if [ "$1" == "-drop_double" ]; then
		CHECK=0
		DROPDOUBLE=1
		shift
	fi
	
	if [ "$1" == "-keep_triple" ]; then
		CHECK=0
		KEEPTRIPLE=1
		shift
	fi

	if [ "$1" == "-drop_triple" ]; then
		CHECK=0
		DROPTRIPLE=1
		shift
	fi

	if [ "$1" == "-keep_quadruple" ]; then
		CHECK=0
		KEEPQUADRUPLE=1
		shift
	fi

	if [ "$1" == "-drop_quadruple" ]; then
		CHECK=0
		DROPQUADRUPLE=1
		shift
	fi

	if [ "$1" == "-keep_quintuple" ]; then
		CHECK=0
		KEEPQUINTUPLE=1
		shift
	fi

	if [ "$1" == "-drop_quintuple" ]; then
		CHECK=0
		DROPQUINTUPLE=1
		shift
	fi

	if [ "$1" == "-drop_synthetic" ]; then
		CHECK=0
		DROPSYNTHETIC=1
		shift
	fi

	if [ "$1" == "-drop_user_data" ]; then
		CHECK=0
		DROPUSERDATA=$2
		shift
		shift
	fi

	if [ "$1" == "-keep_user_data" ]; then
		CHECK=0
		KEEPUSERDATA=$2
		shift
		shift
	fi

	if [ "$1" == "-set_classification" ]; then
		CHECK=0
		SETCLASSIFICATION=$2
		shift
		shift
	fi

	if [ "$1" == "-set_user_data" ]; then
		CHECK=0
		SETUSERDATA=$2
		shift
		shift
	fi

	if [ "$1" == "-buffer" ]; then
		CHECK=0
		BUFFER=$2
		shift
		shift
	fi

	if [ "$1" == "-odir" ]; then
		CHECK=0
		ODIR=$2
		shift
		shift
	fi

	if [ "$1" == "-few" ]; then
		CHECK=0
		FEW=$2
		shift
		shift
	fi

	if [ "$1" == "-step" ]; then
		CHECK=0
		STEP=$2
		shift
		shift
	fi

	if [ "$1" == "-ground_class" ]; then
		CHECK=0
		GROUND_CLASS=$2
		shift
		shift
	fi

	if [ "$1" == "-method" ]; then
		CHECK=0
		METHOD=$2
		shift
		shift
	fi

	if [ "$1" == "-sep" ]; then
		CHECK=0
		SEP=$2
		shift
		shift
	fi	

	if [ "$1" == "-cores" ]; then
		CHECK=0
		CORES=$2
		shift
		shift
	fi

	if [ "$1" == "-plot" ]; then
		CHECK=0
		PLOT=$2
		shift
		shift
	fi
	
	if [ "$CHECK" == 1 ]; then
		echo "Error! Could not parse " $1	
		exit 0
	fi
done

java -Djava.library.path=./proj-4.8.0/gdal-2.2.3/swig/java -cp ".:src/Tinfour-1.0.jar:src/:src/gdal.jar:src/commons-collections4-4.1.jar:src/jai_imageio-1.1.jar:src/opencv_320.jar:src/metadata-extractor-2.10.1.jar:src/xmpcore-5.1.2.jar" runners.RunId4pointsLAS 0 $IPARSE $OPARSE $OUTPUT $INPUT $DROPNOISE $DROPCLASSIFICATION $FIRSTONLY $KEEPFIRST $DROPFIRST $LASTONLY $KEEPLAST $DROPLAST $DROPFIRSTOFMANY $DROPLASTOFMANY $KEEPMIDDLE $DROPMIDDLE $KEEPSINGLE $DROPSINGLE $KEEPDOUBLE $DROPDOUBLE $KEEPTRIPLE $DROPTRIPLE $KEEPQUADRUPLE $DROPQUADRUPLE $KEEPQUINTUPLE $DROPQUINTUPLE $DROPSYNTHETIC $DROPUSERDATA $KEEPUSERDATA $SETCLASSIFICATION $SETUSERDATA $DROPZBELOW $DROPZABOVE $BUFFER $ODIR $FEW $STEP $GROUND_CLASS $METHOD $SEP $CORES $PLOT $NUMARG1 $NUMARG2 $NUMARG3 $NUMARG4

awk '!seen[$0]++' output/hilaFile.txt > output/hilaFile2.txt
	sort --parallel=6 -k1 -n output/hilaFile.txt -o output/hilaFile_sorted.txt

	rm output/hilaFile.txt
 
	mkdir output/Cell_splitted

	java -cp ".:src/" runners.AjaPilkonta output/hilaFile_sorted.txt output/Cell_splitted/
	
	rm output/hilaFile_sorted.txt

#set +f

