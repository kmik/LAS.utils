#!/bin/bash
set -f
BR=0 #apply point filter before reading

OTYPE="las"

NUMARG1=0
NUMARG2=-999
NUMARG3=-999
NUMARG4=-999

CORES=1
SEP="\s"
IPARSE="xyz"
OPARSE="xyz"
OUTPUT="asd"
INPUT="asd"

ECHO_CLASS=0
DROPNOISE=0
DROPCLASSIFICATION=-999

FIRSTONLY=0
KEEPFIRST=0
DROPFIRST=0

REMOVEBUFFER=0

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
KEEPCLASSIFICATION=-999
SETUSERDATA=-999

DROPZBELOW=-999
DROPZABOVE=-999

BUFFER=0
ODIR="asd"

FEW=2
STEP=2

GROUND_CLASS=-1
METHOD="null"

POLY="null"

TRANSLATE_X=-999
TRANSLATE_Y=-999
TRANSLATE_Z=-999
TRANSLATE_i=-999

KERNEL=3
THETA=1

SKIP_GLOBAL=0
TRAJECTORY="null"

THIN3D=0
TEMP="asd"
ENDLOOP=0

DIST=-999

SPLITBY="asd"
LAMBDA=10
DELTA=5

CONCAVITY=50

INTERVAL=20

CHANGE_POINT_TYPE=-999


#echo "$1 $2"

while [ "$#" -gt 0 ]
do
CHECK=1
	if [ "$1" == "-i" ]; then
		CHECK=0
		INPUT=$2
		shift
		TEMP=$2
		while [ $ENDLOOP == 0 ]
		do
			if [[ ${TEMP:0:1} =~ "-" ]]; then
   				ENDLOOP=1
			fi

			if [ $ENDLOOP != 1 ]; then
				INPUT="$INPUT;$2"
			
			fi
			shift
			TEMP=$2

			if [ "$#" == 0 ]; then
				ENDLOOP=1
			fi
			
		done	

	fi
	if [ "$1" == "-br" ]; then
		CHECK=0
		BR=1
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

	if [ "$1" == "-concavity" ]; then
		CHECK=0
		CONCAVITY=$2
		shift
		shift
	fi

	if [ "$1" == "-dist" ]; then
		CHECK=0
		DIST=$2
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

	if [ "$1" == "-keep_classification" ]; then
		CHECK=0
		KEEPCLASSIFICATION=$2
		shift
		shift
	fi

	if [ "$1" == "-change_point_type" ]; then
		CHECK=0
		CHANGE_POINT_TYPE=$2
		shift
		shift
	fi

	if [ "$1" == "-drop_noise" ]; then
		CHECK=0
		DROPNOISE=1
		shift
	fi


	if [ "$1" == "-remove_buffer" ]; then
		CHECK=0
		REMOVEBUFFER=1
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

	if [ "$1" == "-echo_class" ]; then
		CHECK=0
		ECHO_CLASS=1
		shift
	fi

	if [ "$1" == "-thin3d" ]; then
		CHECK=0
		THIN3D=1
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

	if [ "$1" == "-interval" ]; then
		CHECK=0
		INTERVAL=$2
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

	if [ "$1" == "-theta" ]; then
		CHECK=0
		THETA=$2
		shift
		shift
	fi

	if [ "$1" == "-kernel" ]; then
		CHECK=0
		KERNEL=$2
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

	if [ "$1" == "-traj" ]; then
		CHECK=0
		TRAJECTORY=$2
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

	if [ "$1" == "-otype" ]; then
		CHECK=0
		OTYPE=$2
		shift
		shift
	fi

	if [ "$1" == "-ground_points" ]; then
		CHECK=0
		NUMARG4=$2
		shift
		shift
	fi

	if [ "$1" == "-split" ]; then
		CHECK=0
		NUMARG1=1
		shift
	fi

	if [ "$1" == "-splitBy" ]; then
		CHECK=0
		SPLITBY=$2
		shift
		shift
	fi

	if [ "$1" == "-poly" ]; then
		CHECK=0
		POLY=$2
		shift
		shift
	fi

	if [ "$1" == "-axGrid" ]; then
		CHECK=0
		NUMARG3=$2
		shift
		shift
	fi

	if [ "$1" == "-angle" ]; then
		CHECK=0
		NUMARG2=$2
		shift
		shift
	fi
	if [ "$1" == "-lambda" ]; then
		CHECK=0
		LAMBDA=$2
		shift
		shift
	fi
	if [ "$1" == "-delta" ]; then
		CHECK=0
		DELTA=$2
		shift
		shift
	fi

	if [ "$1" == "-translate_x" ]; then
		CHECK=0
		TRANSLATE_X=$2
		shift
		shift
	fi

	if [ "$1" == "-translate_y" ]; then
		CHECK=0
		TRANSLATE_Y=$2
		shift
		shift
	fi

	if [ "$1" == "-translate_z" ]; then
		CHECK=0
		TRANSLATE_Z=$2
		shift
		shift
	fi

	if [ "$1" == "-translate_i" ]; then
		CHECK=0
		TRANSLATE_i=$2
		shift
		shift
	fi

	if [ "$1" == "-skip_global" ]; then
		CHECK=0
		SKIP_GLOBAL=1
		shift
	fi

	if [ "$CHECK" == 1 ]; then
		echo "Error! Could not parse " $1
		exit 0
	fi
done

echo $IPARSE $OPARSE $OUTPUT $INPUT $DROPNOISE $DROPCLASSIFICATION $FIRSTONLY $KEEPFIRST $DROPFIRST $LASTONLY $KEEPLAST $DROPLAST $DROPFIRSTOFMANY $DROPLASTOFMANY $KEEPMIDDLE $DROPMIDDLE $KEEPSINGLE $DROPSINGLE $KEEPDOUBLE $DROPDOUBLE $KEEPTRIPLE $DROPTRIPLE $KEEPQUADRUPLE $DROPQUADRUPLE $KEEPQUINTUPLE $DROPQUINTUPLE $DROPSYNTHETIC $DROPUSERDATA $KEEPUSERDATA $SETCLASSIFICATION $SETUSERDATA $DROPZBELOW $DROPZABOVE $BUFFER $ODIR $FEW $STEP $GROUND_CLASS $METHOD $SEP $CORES $OTYPE $NUMARG1 $NUMARG2 $NUMARG3 $NUMARG4 $BR $POLY $TRANSLATE_X $TRANSLATE_Y $TRANSLATE_Z $SKIP_GLOBAL $TRAJECTORY $TRANSLATE_i $THIN3D $SPLITBY $REMOVEBUFFER $DIST $THETA $KERNEL $LAMBDA $DELTA $CONCAVITY $INTERVAL $ECHO_CLASS $KEEPCLASSIFICATION $CHANGE_POINT_TYPE

#PARSED="$IPARSE $OPARSE $OUTPUT $INPUT $DROPNOISE $DROPCLASSIFICATION $FIRSTONLY $KEEPFIRST $DROPFIRST $LASTONLY $KEEPLAST $DROPLAST $DROPFIRSTOFMANY $DROPLASTOFMANY $KEEPMIDDLE $DROPMIDDLE $KEEPSINGLE $DROPSINGLE $KEEPDOUBLE $DROPDOUBLE $KEEPTRIPLE $DROPTRIPLE $KEEPQUADRUPLE $DROPQUADRUPLE $KEEPQUINTUPLE $DROPQUINTUPLE $DROPSYNTHETIC $DROPUSERDATA $KEEPUSERDATA $SETCLASSIFICATION $SETUSERDATA $DROPZBELOW $DROPZABOVE $BUFFER $ODIR $FEW $STEP $GROUND_CLASS $METHOD $SEP $CORES $OTYPE $NUMARG1 $NUMARG2 $NUMARG3 $NUMARG4 $BR $POLY $TRANSLATE_X $TRANSLATE_Y $TRANSLATE_Z $SKIP_GLOBAL $TRAJECTORY $TRANSLATE_i $THIN3D $SPLITBY"
