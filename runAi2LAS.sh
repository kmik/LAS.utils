#!/bin/bash

FILES=lasrelate_xy_test/all_point_clouds/clipped/*.las

#./runners.ai2las.sh -exterior lasrelate_xy_test/exterior.txt -interior lasrelate_xy_test/interior -i lasrelate_xy_test/all_point_clouds/clipped/pointCloudHigh3_000000.las

for f in $FILES
do
	echo $f
	./runners.ai2las.sh -exterior lasrelate_xy_test/exterior.txt -interior lasrelate_xy_test/interior -i $f
done
