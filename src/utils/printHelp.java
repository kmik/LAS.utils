package utils;

public class printHelp {

    public printHelp(){

    }

    public printHelp(int tool_id){

        this.print(tool_id);

    }


    public void print(int tool_id){


        switch (tool_id){
            case 1:
                this.tiler();
                break;

            case 2:
                this.merger();
                break;

            case 3:
                this.noise();
                break;

            case 4:
                this.ground_detect();
                break;

            case 5:
                this.z_normalize();
                break;

            case 6:
                this.thin();
                break;
            case 7:
                this.chm();
                break;

            case 8:
                this.txt2las();
                break;

            case 9:
                this.border();
                break;

            case 10:
                this.border();
                break;

            case 11:
                this.las2shp();
                break;

            case 12:
                this.las2txt();
                break;

            case 13:
                this.las2las();
                break;

            case 14:
                this.lasStrip();
                break;

            case 15:
                this.lasITD();
                break;

            case 16:
                this.lasIndex();
                break;

            default:
                break;
        }
    }

    public static void lasIndex(){
        System.out.println("----------------------------------------------\n" +
                " lasIndex -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Spatially indexes a .las file for faster spatial queries.\n" +
                "Not all tools in this software package take use of this\n" +
                "functionality of spatial indexing. Creates a .lasx file\n" +
                "that is always internally linked to the .las file when \n" +
                "the .las file is opened.\n" +
                "\n" +
                "Tools that benefit from spatial indexing:\n" +
                "\n" +
                "lasClip.sh\n" +
                "lasborder.sh (only convex hull)\n" +
                "\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-step\t\tThe \"resolution\" of indexing.");
    }


    public static void lasITD(){
        System.out.println("----------------------------------------------\n" +
                " lasITD -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Segments individual trees from point cloud data. The\n" +
                "segmentation is performed using 2d methods,\n" +
                "watershed segmentation from gaussian filtered\n" +
                "canopy height model (CHM). The local maxima in the CHM\n" +
                "are used as initial markers (i.e. treetops) for\n" +
                "the watershed segmentation algorithm. The kernel size\n" +
                "for the local maxima is calculated as:\n" +
                "\n" +
                "double kernel_size_meters = 1.1 + 0.002 * (zMiddle*zMiddle);\n" +
                "\n" +
                ",which means that the kernel size is larger for taller trees\n" +
                "and smaller for shorter trees. \n" +
                "\n" +
                "The output contains several files that are names as:\n" +
                "\n" +
                "(1) originalFileName_ITD.las \n" +
                "(2) originalFileName_treeTops.shp \n" +
                "(3) originalFileName_TreeSegmentation.shp \n" +
                "(4) originalFileName.tif\n" +
                "\n" +
                "The first file is the output .las file where each point\n" +
                "is labeled with the corresponding tree segments id in \n" +
                "pointSourceId slot. This is an unsigned short, which means\n" +
                "that id:s larger than 65535 will cause issues.\n" +
                "\n" +
                "The second file is a shapefile where each point corresponds\n" +
                "to a treetop. \n" +
                "\n" +
                "The third file is a polygon representation of the \n" +
                "tree crown segmentation.\n" +
                "\n" +
                "The fourth file is the gaussian filtered CHM.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-step \t\tResolution of the CHM\n" +
                "\t-theta\t\tGaussian theta");
    }


    public static void lasStrip(){
        System.out.println("----------------------------------------------\n" +
                " lasStrip -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Aligns the flight lines of LiDAR data. Flight lines\n" +
                "should be in separate .las files and have ground\n" +
                "classified (class = 2) echoes.\n" +
                "\n" +
                "The provided trajectory file should be in degrees \n" +
                "(NOT radians). If no trajectory file is provided,\n" +
                "a pivot point in the center of the point cloud\n" +
                "will be used to rotate the point cloud. THIS IS NOT\n" +
                "IMPLEMENTED YET. ALWAYS USE TRAJECTORY FILE!!!\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-skip_global\tDo not perform boresight and\n" +
                "\t\t\tleverarm optimization\n" +
                "\t-traj\t\tTrajectory file (ASCII)");
    }

    public static void las2las(){
        System.out.println("----------------------------------------------\n" +
                " las2las -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Converts .las file to another .las file\n" +
                "according to parameter specifications.\n" +
                "\n" +
                "See ./arguments.sh for more information\n" +
                "about different parameters\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)");
    }

    public void las2txt(){
        System.out.println("----------------------------------------------\n" +
                " las2txt -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Converts a .las file into a .txt file. Output column\n" +
                "order can be given with -oparse argument. With \n" +
                "-echoClass flag, the output will contain information\n" +
                "regarding the classification of the echo into:\n" +
                "\n" +
                "(0) only echoes\n" +
                "(1) first of many and only\n" +
                "(2) last of many and only \n" +
                "(3) intermediate\n" +
                "\n" +
                "This classification is coded to \"numberOfReturns\" of\n" +
                "each point and the returnNumber of every point is set\n" +
                "to 0.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-echoClass\tOutput echo classification");
    }

    public void las2shp(){
        System.out.println("----------------------------------------------\n" +
                " las2shp -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Converts a .las file to ESRI shapefile where each \n" +
                "point is a separate point feature in the .shp \n" +
                "file.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)");
    }

    public void border(){
        System.out.println("----------------------------------------------\n" +
                " lasborder -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Creates an ESRI shapefile representing the spatial extent\n" +
                "of LAS points. By default creates a \"convex hull\", but can \n" +
                "be changed to \"concave hull\" with -concave flag \n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s) \n" +
                "\t-concave\tDo concave instead of convex");
    }

    public void txt2las(){
        System.out.println("----------------------------------------------\n" +
                " txt2las -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Converts .txt files into .las files. The order\n" +
                "of columns in .txt file can be set with -iparse\n" +
                "argument. e.g. if the order is x coordinate, y\n" +
                "coordinate, z coordinate and intensity, \n" +
                "-iparse xyzi. The field separator for the .txt\n" +
                "file is tab by default, but can be set\n" +
                "with -sep \"separator\" argument.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-iparse\t\t...\n" +
                "\t-sep \t\ttxt file field separator");
    }

    public void z_normalize(){
        System.out.println("----------------------------------------------\n" +
                " lasheight -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Subtracts every point from a TIN surface based\n" +
                "on Delaunay triangulation. The classification of \n" +
                "ground points can be specified with -ground_class\n" +
                "parameter. If there are not enough points to \n" +
                "trianulate a decent TIN, an error is thrown.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file\n" +
                "\t-odir\t\tOutput directory\n  " +
                "\t-ground_class\tGround point classification (default 2)\n" +
                "\t-ground_points \tGround points from another .las file (class 2)\n");
    }

    public void chm(){
        System.out.println("----------------------------------------------\n" +
                " las2dsm -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Creates a Digital Surface Model (DSM) from .las file.\n" +
                "-step parameter defines the output resolution (in meters)\n" +
                "of the surface model and -theta the intensitvity of the gaussian. \n" +
                "filter. Gaussian kernel is computed automatically using the  \n" +
                "provided -theta parameter. \n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file\n" +
                "\t-step\t\tDSM resolution (in meters)\n" +
                "\t-theta\t\tGaussian theta (default 1.0)\n");
    }


    public void ground_detect(){
        System.out.println("----------------------------------------------\n" +
                " lasground -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Detects ground points from .las file. Based on Axelsson\n" +
                "(2000) algorithm. For photogrammetric data (usually characterized\n" +
                "by excessive noise in certain flat areas) the user can use \n" +
                "-photogrammetry argument that improves the accuracy of seed\n" +
                "point detection. This only affects the detection of seed points\n" +
                "and has no effect on the following densification of the surface.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file / prefix(\"xxx_\")\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-distance\tDistance threshold (default 1.0 m)\n" +
                "\t-angle\t\tAngle threshold (default 10.0 degrees)\n" +
                "\t-axGrid\t\tSize of the kernel for seed point search\n" +
                "\t       \t\t(default 20.0 m)");
    }

    public void noise(){
        System.out.println("----------------------------------------------\n" +
                " lasnoise -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Detects outliers in .las file and either classifies\n" +
                "them as 7, or deletes them (-drop_noise). \n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file / prefix(\"xxx_\")\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-step\t\tVoxel size\n" +
                "\t-few\t\tMaximum number of points in neighbourhood");
    }

    public void merger(){
        System.out.println("----------------------------------------------\n" +
                " lasmerge -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Merges multiple .las files into one. \n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\t\"Input file(s)\"\n" +
                "\t-o\t\tOutput file");
    }

    public void thin(){
        System.out.println( "----------------------------------------------\n" +
                " lasthin -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Thins a .las point cloud. -step specifies the\n" +
                "size of the square in which -few points\n" +
                "are kept. If -thin_3d argument is passed, the\n" +
                "thinning is done in voxels, instead of 2d\n" +
                "squares.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-step\t\tThinning parameter\n" +
                "\t-few\t\tNumber of points kept\n");
    }

    public void tiler(){
        System.out.println( "----------------------------------------------\n" +
                " lastile -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Splits many .las files into convenient sized square\n" +
                "tiles (size specified by -step parameter). Tiles can \n" +
                "be buffered to avoid distinct edges in post-processing.\n" +
                "Points in the buffer are set as \"synthetic\" and can be.\n" +
                "ignored in processing using -remove_buffer.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-step\t\tTile size (step x step)\n" +
                "\t-buffer\t\tBuffer (m)");
    }
}
