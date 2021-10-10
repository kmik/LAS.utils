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
can be merged (default) or splitted (-split). The split
option does NOT split by polygon features, but rather
by point clouds, i.e. one output file per input 
point cloud.

The -omet flag is used to output stastical features
of points within the boundaries of individual polygon
features.

Usage:

	-i		Input file(s)
	-o		Output file
	-odir		Output directory
	-otype		Output file type, "txt" or "las"
	-poly		Input polygon (.shp, wkt, txt)
	-sep 		txt file field separator
	-omet		Output statistical metrics

	     
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
file is tab by default, but can be set
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

Converts a .las file to ESRI shapefile where each 
point is a separate point feature in the .shp 
file.

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

Converts a .las file into a .txt file. Output column
order can be given with -oparse argument. With 
-echoClass flag, the output will contain information
regarding the classification of the echo into:

(0) only echoes
(1) first of many and only
(2) last of many and only 
(3) intermediate

This classification is coded to "numberOfReturns" of
each point and the returnNumber of every point is set
to 0.

Usage:

	-i		Input file(s)
	-echoClass	Output echo classification

	     
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

Aligns the flight lines of LiDAR data. Flight lines
should be in separate .las files and have ground
classified (class = 2) echoes.

The provided trajectory file should be in degrees 
(NOT radians). If no trajectory file is provided,
a pivot point in the center of the point cloud
will be used to rotate the point cloud.

Usage:

	-i		Input file(s)
	-odir		Output directory
	-skip_global	Do not perform boresight and
			leverarm optimization
	-traj		Trajectory file (ASCII)
	     
EOF
fi

if [ "$1" == 15 ]; then
cat << EOF

----------------------------------------------
 lasITD -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Segments individual trees from point cloud data. The
segmentation is performed using 2d methods,
watershed segmentation from gaussian filtered
canopy height model (CHM). The local maxima in the CHM
are used as initial markers (i.e. treetops) for
the watershed segmentation algorithm. The kernel size
for the local maxima is calculated as:

double kernel_size_meters = 1.1 + 0.002 * (zMiddle*zMiddle);

,which means that the kernel size is larger for taller trees
and smaller for shorter trees. 

The output contains several files that are names as:

(1) originalFileName_ITD.las 
(2) originalFileName_treeTops.shp 
(3) originalFileName_TreeSegmentation.shp 
(4) originalFileName.tif

The first file is the output .las file where each point
is labeled with the corresponding tree segments id in 
pointSourceId slot. This is an unsigned short, which means
that id:s larger than 65535 will cause issues.

The second file is a shapefile where each point corresponds
to a treetop. 

The third file is a polygon representation of the 
tree crown segmentation.

The fourth file is the gaussian filtered CHM.

Usage:

	-i		Input file(s)
	-odir		Output directory
	-step 		Resolution of the CHM
	-theta		Gaussian theta

	     
EOF
fi

if [ "$1" == 16 ]; then
cat << EOF

----------------------------------------------
 lasIndex -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Spatially indexes a .las file for faster spatial queries.
Not all tools in this software package take use of this
functionality of spatial indexing. Creates a .lasx file
that is always internally linked to the .las file when 
the .las file is opened.

Tools that benefit from spatial indexing:

lasClip.sh
lasborder.sh (only convex hull)


Usage:

	-i		Input file(s)
	-step		The "resolution" of indexing.
	     
EOF
fi

if [ "$1" == 17 ]; then
cat << EOF

----------------------------------------------
 lasSort -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Sorts the points in a .las file. Can either be 
set to 

-by_gps_time (default)

or

-by_z_order


Z-order is very useful to do prior to indexing.


Usage:

	-i		Input file(s)
	-by_gps_time	Order by time
	-by_z_order	Order by z-order

	     
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
with the flag -splitBy "classification" / "pointSourceId" /
"return" / "userData" / "gps". GPS splitting is a special case,
where flight lines are extracted from the las file based
on -interval flag. Interval determines the maximum
gap between consecutive points in a GPS sorted LAS file.

Usage:

	-i		Input file(s)
	-splitBy	Split by what criterion?
	-interval	Used in flightline (gps) splitting
			to determine bin size. (default 20.0)

	     
EOF
fi


if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasCheck -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Checks that the contents of the .las file are 
coherent with the header information.

Also does various other checks which are reported
at the end of the run.

Usage:

	-i		Input file(s)

	     
EOF
fi


if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasITDstats -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Computes tree-wise statistics from crown segmented (lasITD.sh)
.las file. Also produces outputs for tree trunks, should they 
be available, i.e. if the .las file has been processed
with lasStem.sh.

If field measured trees (-measured_trees) are available,
the code will link the crown segments to field measured
trees. This is done by using two kd-trees. Only trees that are
<2.5 m within each other and are each other's closest neighbors
are considered pairs.

If (-poly) is provided, only trees within the boundaries of the 
polygon are considered. 

Usage:

	-i		Input file(s)
	-o		Name of the output file

	     
EOF
fi



if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasLayer -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Attempts to delineate an understory tree layer 
from .las file. Requires very high point density 
point cloud data from a forested environment.



Usage:

	-i		Input file(s)

	     
EOF
fi


if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasStem -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Detects, classifies and labels points belonging
to tree stems from .las file.



Usage:

	-i		Input file(s)

	     
EOF
fi




if [ "$1" == 18 ]; then
cat << EOF

----------------------------------------------
 lasGridStats -- LASutils build $line

 (c) M.Kukkonen
 University of Eastern Finland
----------------------------------------------

Computes echo-class specific statistics from the 
point cloud at grid cells. The "origo" is the 
TOP-LEFT corner of the grid layout. This can be
specified using flags -orig_x X_COORDINATE and
-orig_y Y_COORDINATE. If no origo is input, 
the min_x and max_y header values of the .las
file are used as the origo.

The statistics are computed seperately for
ALL_ECHOES, FIRST_OF_MANY_AND_ONLY, 
LAST_OF_MANY_AND_ONLY and INTERMEDIATE. The output
files are placed in the same directory as the input
file and are named according to the input file.

The "resolution", i.e. the edge length of each
square grid unit can be specified using the flag
-res RESOLUTION.

Currently only ONE file can be processed at a time.
The tool can, of cource, be run parallel if executed
multiple times for different point clouds.

If the .las file has been clipped using lasclip.sh and
pointSourceId of each point corresponds to a polygon feature,
this tool will assign grid cells to unique pointSourceId
values. If a grid-cell contains multiple pointSourceIds,
the cell will be divided into parts and these parts will
be merged with neighboring cells that contain the given
pointSourceId. To which cell these parts are merged
is dependent upon the size of the cells. The tool tries
to optimize the size of the merged cells so that they
are as close to the -res as possible.

TODO: Implement a way to just give -poly shapefile to 
do the designation of grid cells to polygon features.
PointSourceId can be quite an important memory slot
that is not always available for the clipping...


Usage:

	-i		Input file(s)
	-orig_x		Origo x coordinate
	-orig_y		Origo y coordinate
	-res		Grid cell unit dimension

	     
EOF
fi



set +f
