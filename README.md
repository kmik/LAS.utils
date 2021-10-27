# LASutils


A collection of .las file processing tools specifically designed to be used with point cloud data from forest environments. Tools written in Java, executed with scripts. Project in progress...

Scripts are run in the following fasion:

    ./las2las.sh -i ./test_data/test_file_1.las -drop_z_below 0.0 -set_version_minor 3 -o out.las
Currently implemented tools:

  * LAS conversion to LAS and .txt files.
  * Classification of ground echoes.
  * Normalization of LiDAR data wrt. ground surface.
  * Clipping LAS files using polygons (.shp or WKT)
  * DSM/DTM creation.
  * Noise removal.
  * Thinning (both using 2D grid and 3D voxels).
  * Tiling.
  * Merging.
  * Delineation of tree crowns.
  * Detection of vertical tree trunks.
  * Strip alignment.
  * Computation of point cloud metrics.

