# LASutils


A collection of .las file processing tools specifically designed to be used with point cloud data from forest environments. 
Tools written in Java, executed with scripts. Inspired by LAStools by Rapidlasso. 

Scripts are run in the following fashion:

    ./las2las.sh -i ./test_data/test_file_1.las -drop_z_below 0.0 -set_version_minor 3 -o out.las

    ./lasground.sh -i ./test_data/test_file_1.las -axGrid 10 -angle 7.5 -dist 0.5 -o test_file_1_gc.las


Instructions, and a brief explanation, of the arguments for any given tool can be printed to shell by either not giving the tool
any arguments, or by giving the tool -h / -help flag.

    ./lasground.sh 


    ----------------------------------------------
    lasground -- LASutils build $line
    
    (c) M.Kukkonen
    Natural Resources Institute Finland
    ----------------------------------------------
    
    Detects ground points from .las file. Based on Axelsson
    (2000) algorithm. For photogrammetric data (usually characterized
    by excessive noise in certain flat areas) the user can use
    -photogrammetry argument that improves the accuracy of seed
    point detection. In short, the tool does not take the lowest
    point within the -axGrid, but rather computes a smaller grid
    within the -axGrid and only accepts points areas where standard
    deviation of points is in acceptable range.
    This only affects the detection of seed points
    and has no effect on the following densification of the surface.
    
    Usage:
    
        -i		    Input file(s)
        -o		    Output file
        -odir	    Output directory
        -distance       Distance threshold (default 1.0 m)
        -angle	    Angle threshold (default 10.0 degrees)
        -axGrid	    Size of the kernel for seed point search
                        (default 20.0 m)


In addition to tool-specific arguments, there are several tool-independent arguments
that can be passed to any tool. These usually modify / exclude / keep either the input or the output points.
An overview of these arguments can be printed with ./'any_tool.sh' -args:

    ./las2las.sh -args

    Optional tool-independent arguments
    -----------------------------------------

        -read_rules	    Apply these rules when a 
                        point is read.

        -write_rules	Apply these rules when a 
                        point is written to .las file
    
    ______________________________
    INCLUDING / EXCLUDING POINTS:
    ------------------------------
    
        -drop_z_below	[float]  	Exclude below (<) 
                                    z threshold.
                        
        -drop_z_above	[float]	 	Exclude points above
                                    (>) z threshold.
                        
        -drop_noise	[-]	 	        Exclude noise points 
                                    (classification 7)
                        
        -remove_buffer	[-]	 	    Exclude buffer points
                                    (synthetic).
                        
        -drop_syntetic	[-]		    Exlude synthetic
                                    points.				
                
        -keep_classification [int]	Include only points 
                                    with this class.
                        
        -drop_classification[int] 	Exclude points with 
                                    this class.
                        
        -keep_user_data [int]	    Include only points 
                                    with this user_data.
                        
        -drop_user_data		[int] 	Exclude points with 
                                    this user_data.

                            *
                            *
                            *
                    To be continued...



Most tools can be parallelized with -cores flag. This usually requires that multiple point clouds are input 
using e.g. wildcard *. If you only have one input .las file, -cores flag will likely have no effect.

    ./lasheight.sh -i ./test_data/*.las -cores 2 -odir ./test_data/

    

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
  * Computation of spectral DN values for each point. This requires
    known interior and exterior orientations. See more ./ai2las.sh.

