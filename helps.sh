#!/bin/bash
set -f

curDir=$(dirname $0)

versionFile="info"

line=$(head -n 1 $curDir/$versionFile)

if [ "$1" == 1 ]; then
cat << EOF

----------------------------------------------
 lasheight -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Subtracts every point from a TIN surface based
on Delaunay triangulation. The classification of 
ground points can be specified with -ground_class
parameter. If there are not enough points to 
trianulate a decent TIN, an error is thrown.

Usage:

	-i		Input file(s)
	-o		Output file
	-odir		Output directory
	-ground_class	Ground point classification (default 2)
	-ground_points 	Ground points from another .las file (class 2)

	     
EOF
fi

if [ "$1" == 2 ]; then
cat << EOF

----------------------------------------------
 las2dsm -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Creates a Digital Surface Model (DSM) from .las file.
User can set an interpolation method (TODO). -step
parameter defines the output resolution (in meters)
of the surface model. 

Usage:

	-i		Input file(s)
	-o		Output file
	-step		DSM resolution (in meters)
	-kernel		Gaussian kernel size (default 1.0)
	-theta		Gaussian theta (default 1.0)

	     
EOF
fi


if [ "$1" == 3 ]; then
cat << EOF

----------------------------------------------
 lasground -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Detects ground points from .las file. Can be either set in
"city" mode or "forest" mode (default). Based on Axelsson
(2000) algorithm, with an automatic thinning of ground points
to avoid redundancy in data.

Usage:

	-i		Input file(s)
	-o		Output file / prefix("xxx_")
	-odir		Output directory
	-method		"city" or "forest"
	-distance	Distance threshold (default 0.10m)
	-angle		Angle threshold (default 2.5 degrees)
	-axGrid		Size of the kernel for seed point search

	     
EOF
fi

if [ "$1" == 4 ]; then
cat << EOF

----------------------------------------------
 lasclip -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Clips .las files. The output can be either a las
file (default) or .txt file (-otype txt). The output
can be merged (default) or splitted (-split).

Usage:

	-i		Input file(s)
	-o		Output file
	-odir		Output directory
	-poly		Input polygon (.shp, wkt, txt)
	-sep 		txt file field separator

	     
EOF
fi


if [ "$1" == 5 ]; then
cat << EOF

----------------------------------------------
 lasthin -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Thins a .las point cloud. -step specifies the
size of the square in which only one point
is kept. i.e if 1 observation per square meter
is wanted, -step = 1.

Usage:

	-i		Input file(s)
	-odir		Output directory
	-step		Thinning parameter

	     
EOF
fi

if [ "$1" == 6 ]; then
cat << EOF

----------------------------------------------
 lastile -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Splits many .las files into convenient sized square
tiles (size specified by -step parameter). Tiles can 
be buffered to avoid distinct edges in post-processing.

Usage:

	-i		Input file(s)
	-odir		Output directory
	-step		Tile size (step x step)
	-buffer		Buffer (m)

	     
EOF
fi

if [ "$1" == 7 ]; then
cat << EOF

----------------------------------------------
 txt2las -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Converts .txt files into .las files. The order
of columns in .txt file can be set with -iparse
argument. e.g. if the order is x coordinate, y
coordinate, z coordinate and intensity, 
-iparse xyzi. The field separator for the .txt
file is whitespace by default, but can be set
with -sep "separator" argument.

Usage:

	-i		Input file(s)
	-o		Output file
	-odir		Output directory
	-iparse		...
	-sep 		txt file field separator

	     
EOF
fi

if [ "$1" == 8 ]; then
cat << EOF

----------------------------------------------
 lasmerge -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Merges multiple .las files into one. 

Usage:

	-i		"Input file(s)"
	-o		Output file

	     
EOF
fi

if [ "$1" == 9 ]; then
cat << EOF

----------------------------------------------
 lasnoise -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Detects outliers in .las file and either classifies
them as 7, or deletes them (-drop_noise). 

Usage:

	-i		Input file(s)
	-o		Output file / prefix("xxx_")
	-odir		Output directory
	-step		Voxel size
	-few		Maximum number of points in neighbourhood

	     
EOF
fi

if [ "$1" == 10 ]; then
cat << EOF

----------------------------------------------
 lasborder -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Creates an ESRI shapefile representing the spatial extent
of LAS points. By default creates a "concave hull", but can 
be changed to "convex hull" with -method convex flag 

Usage:

	-i		Input file(s)
	
	     
EOF
fi

if [ "$1" == 11 ]; then
cat << EOF

----------------------------------------------
 las2shp -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

.

Usage:

	-i		Input file(s)

	     
EOF
fi

if [ "$1" == 12 ]; then
cat << EOF

----------------------------------------------
 las2txt -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

.

Usage:

	-i		Input file(s)

	     
EOF
fi


if [ "$1" == 13 ]; then
cat << EOF

----------------------------------------------
 las2las -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Converts .las file to another .las file
according to parameter specifications.

See ./arguments.sh for more information
about different parameters

Usage:

	-i		Input file(s)

	     
EOF
fi

if [ "$1" == 14 ]; then
cat << EOF

----------------------------------------------
 lasStrip -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------



Usage:

	-i		Input file(s)
	-odir		Output directory
	-skip_global	Do not perform boresight and
			leverarm optimization
	-angle		Angle threshold used in filtering
			outliers from DTMs
	     
EOF
fi

if [ "$1" == 15 ]; then
cat << EOF

----------------------------------------------
 lasITD -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------



Usage:

	-i		Input file(s)

	     
EOF
fi

if [ "$1" == 16 ]; then
cat << EOF

----------------------------------------------
 lasIndex -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------



Usage:

	-i		Input file(s)

	     
EOF
fi

if [ "$1" == 17 ]; then
cat << EOF

----------------------------------------------
 lasSort -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------



Usage:

	-i		Input file(s)

	     
EOF
fi

if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasSplit -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

A tool to split a LAS file under various criterion 
with the flag -splitBy classification / pointSourceId /
return / userData / gps. GPS splitting is a special case,
where flight lines are extracted from the las file based
on -interval flag. Interval determines the maximum
gap between consecutive points in a GPS sorted LAS file.

Usage:

	-i		Input file(s)
	-interval	Used in flightline (gps) splitting
			to determine bin size. (default 20.0)

	     
EOF
fi

set +f
