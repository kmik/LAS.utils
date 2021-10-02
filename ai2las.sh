#!/bin/bash
set -f
#export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)
usage()
{
cat << EOF

----------------------------------------------
 runners.ai2las (LASutils version 0.1)
----------------------------------------------

Assigns a DN value for each point in .las (or .txt) point
cloud file. Output column order is defined with -oparse flag.

Requires external and internal orientation files. Algorithm
calculates the mean value for each band from each image that 
has observed the point. Last columns of the output file are thus
band_1, band_2, ... , band_n.

Internal orientation (tab delimited):

fc	ps	ppx	ppy

	where	fc = focal length (m)
		ps = pixel size (mm)
		ppx = principal point x offset (m)
		ppy = principal point y offset (m)

External orientation (tab delimited):
	
file_1	id_1	x_1	y_1	z_1	o_1	p_1	k_1
file_2	id_2	x_2	y_2	z_2	o_2	p_2	k_2
			.
			.
			.
file_n	id_n	x_n	y_n	z_n	o_n	p_n	k_n

where	n = number of images	
	file = filepath
	id = image id (can be arbitary, but unique)
	x = x coordinate
	y = y coordinate
	z = altitude
	o = omega (Rotation about the X axis) 
	p = phi (Rotation about the Y axis)
	k = kappa (Rotation about the Z axis)

Flying direction towards the x-axis:

        |     y    ___
      z	|   /	 --_-- |\______.
	|  /	 --_-- '-====---"
	| /     x   ----  **
	|/______	 *  *
			*    *
      		       *      *
        	      *	+ /|\  *	
________________________+__|______________	
Usage:

	-exterior		Input orientation file
	-interior		Input interior orientation file
	-oparse
	-iparse
	-edges			Percentage of "no-go-zone" from
				the edges of the images. Floating
				point 0.0 - 1.0. Example 0.2 means
				we don't consider the pixel if
				it is 0.2 x width from either the 
				left or right side of the image.
				Same goes for top and bottom.
	

Optional arguments:

	Refer to manual
	     
EOF
}

INTERIOR=0
EXTERIOR=0
IPARSE="xyz"
OPARSE="xyz"
ODIR="asd"
CORES=1

#echo $@

#exit

if [ $# == 0 ]; then
	
	usage
        exit 1

fi

curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib:$curDir/gdal/java
export GDAL_DATA=$curDir/gdal/gdal_data
	

java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" runners.ai2las 0 $@

set +f

