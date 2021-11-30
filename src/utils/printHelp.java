package utils;

import err.toolException;
import org.apache.commons.lang3.SystemUtils;

public class printHelp {

    boolean isLinux = true;


    public printHelp(){

    }

    public printHelp(int tool_id){

        if(!SystemUtils.IS_OS_LINUX){
            isLinux = false;
        }

        this.print(tool_id);


    }


    public void print(int tool_id){


        switch (tool_id){

            case 0:
                lasClip();
                break;

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
                las2las();
                break;

            case 14:
                lasStrip();
                break;

            case 15:
                lasITD();
                break;

            case 16:
                lasIndex();
                break;

            case 17:
                lasSort();
                break;

            case 18:
                lasSplit();
                break;
            case 19:
                lasCheck();
                break;

            case 20:
                lasITDstats();
                break;

            case 21:
                lasLayer();
                break;

            case 25:
                lasGridStats();
                break;

            case 26:
                stemDetector();
                break;

            case 12345:
                ai2las();
                break;

            case 9999:
                new toolIndependentArgumentsPrint();
                break;


            default:
                throw new toolException("What happened?");

        }
    }

    public void ai2las(){

        String extension = isLinux ? ".sh" : ".bat";

        System.out.println("----------------------------------------------\n" +
                " runners.ai2las (LASutils version 0.1)\n" +
                "\n" +
                " Problems? Contact mikko.kukkonen@luke.fi\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Assigns a DN value for each point in .las point\n" +
                "cloud file. Output column order is defined with -oparse flag.\n" +
                "\n" +
                "Requires external and internal orientation files. Algorithm\n" +
                "calculates the mean value for each band from each image that \n" +
                "has observed the point. \n" +
                "\n" +
                "Internal orientation (tab delimited):\n" +
                "\n" +
                "fc\tps\tppx\tppy\n" +
                "\n" +
                "\twhere\tfc = focal length (mm)\n" +
                "\t\tps = pixel size (mm)\n" +
                "\t\tppx = principal point x offset (mm)\n" +
                "\t\tppy = principal point y offset (mm)\n" +
                "\n" +
                "NOTE: \tThe coordinate origo for each image is TOP LEFT. \n" +
                "\tppx and ppy are simply added to the projected image\n" +
                "\tcoordinate. Please see how the ppx and ppy have been\n" +
                "\tderived in your bundle software.\n" +
                "\t\n" +
                "External orientation (tab delimited):\n" +
                "\t\n" +
                "file_1\tid_1\tx_1\ty_1\tz_1\to_1\tp_1\tk_1\n" +
                "file_2\tid_2\tx_2\ty_2\tz_2\to_2\tp_2\tk_2\n" +
                "\t\t\t.\n" +
                "\t\t\t.\n" +
                "\t\t\t.\n" +
                "file_n\tid_n\tx_n\ty_n\tz_n\to_n\tp_n\tk_n\n" +
                "\n" +
                "where\tn = number of images\t\n" +
                "\tfile = filepath\n" +
                "\tid = image id (can be arbitary, but unique)\n" +
                "\tx = x coordinate\n" +
                "\ty = y coordinate\n" +
                "\tz = altitude\n" +
                "\to = omega (Rotation about the X axis) \n" +
                "\tp = phi (Rotation about the Y axis)\n" +
                "\tk = kappa (Rotation about the Z axis)\n" +
                "\n" +
                "Flying direction towards the x-axis:\n" +
                "\n" +
                "        |     y    ___\n" +
                "      z\t|   /\t --_-- |\\______.\n" +
                "\t|  /\t --_-- '-====---\"\n" +
                "\t| /     x   ----  **\n" +
                "\t|/______\t *  *\n" +
                "\t\t\t*    *\n" +
                "      \t\t       *      *\n" +
                "        \t      *\t+ /|\\  *\t\n" +
                "________________________+__|______________\t\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\t\tInput .las file\n" +
                "\t-o\t\t\tOutput .las (or .txt) file\n" +
                "\t-exterior\t\tInput orientation file\n" +
                "\t-interior\t\tInput interior orientation file\n" +
                "\t-olas\t\t\tOutput is .las format.\n" +
                "\t-otxt\t\t\tOutput is .txt format.\n" +
                "\t-seq\t\t\tIf output is .las format,\n" +
                "\t\t\t\tthen this is the band order.\n" +
                "\t\t\t\t(e.g. -seq 1,2,3 means \n" +
                "\t\t\t\tband 1 is assigned to R,\n" +
                "\t\t\t\tband 2 is assigned to G,\n" +
                "\t\t\t\tband 3 is assigned to B\n" +
                "\t\t\t\tof each .las point.\n" +
                "\n" +
                "Optional arguments:\n" +
                "\n" +
                "\t-input_in_radians\tInput angles are radians.\n" +
                "\t-oparse\t\t\tOutput .txt file column order.\n" +
                "\t\t\t\tSpectral DN values are appended to\n" +
                "\t\t\t\tthis ordering automatically.\n" +
                "\t-edges\t\t\tPercentage of \"no-go-zone\" from\n" +
                "\t\t\t\tthe edges of the images. Floating\n" +
                "\t\t\t\tpoint 0.0 - 1.0. Example 0.2 means\n" +
                "\t\t\t\twe don't consider the pixel if\n" +
                "\t\t\t\tit is 0.2 x width from either the \n" +
                "\t\t\t\tleft or right side of the image.\n" +
                "\t\t\t\tSame goes for top and bottom.\n" +
                "\t\t\t\t\n" +
                "Example:\n" +
                "\n" +
                " ./ai2las" + extension + " -i input.las -interior interior.txt -exterior exterior.txt -edges 0.05 -olas -seq 3,4,5 -o output.las\t\t\n");
    }


    public static void lasGridStats(){
        System.out.println("----------------------------------------------\n" +
                " lasGridStats -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Computes echo-class specific statistics from the \n" +
                "point cloud at grid cells. The \"origo\" is the \n" +
                "TOP-LEFT corner of the grid layout. This can be\n" +
                "specified using flags -orig_x X_COORDINATE and\n" +
                "-orig_y Y_COORDINATE. If no origo is input, \n" +
                "the min_x and max_y header values of the .las\n" +
                "file are used as the origo.\n" +
                "\n" +
                "The statistics are computed seperately for\n" +
                "ALL_ECHOES, FIRST_OF_MANY_AND_ONLY, \n" +
                "LAST_OF_MANY_AND_ONLY and INTERMEDIATE. The output\n" +
                "files are placed in the same directory as the input\n" +
                "file and are named according to the input file.\n" +
                "\n" +
                "The \"resolution\", i.e. the edge length of each\n" +
                "square grid unit can be specified using the flag\n" +
                "-res RESOLUTION.\n" +
                "\n" +
                "Currently only ONE file can be processed at a time.\n" +
                "The tool can, of cource, be run parallel if executed\n" +
                "multiple times for different point clouds.\n" +
                "\n" +
                "If the .las file has been clipped using lasclip.sh and\n" +
                "pointSourceId of each point corresponds to a polygon feature,\n" +
                "this tool will assign grid cells to unique pointSourceId\n" +
                "values. If a grid-cell contains multiple pointSourceIds,\n" +
                "the cell will be divided into parts and these parts will\n" +
                "be merged with neighboring cells that contain the given\n" +
                "pointSourceId. To which cell these parts are merged\n" +
                "is dependent upon the size of the cells. The tool tries\n" +
                "to optimize the size of the merged cells so that they\n" +
                "are as close to the -res as possible.\n" +
                "\n" +
                "TODO: Implement a way to just give -poly shapefile to \n" +
                "do the designation of grid cells to polygon features.\n" +
                "PointSourceId can be quite an important memory slot\n" +
                "that is not always available for the clipping...\n" +
                "\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-orig_x\t\tOrigo x coordinate\n" +
                "\t-orig_y\t\tOrigo y coordinate\n" +
                "\t-res\t\tGrid cell unit dimension");
    }

    public static void stemDetector(){
        System.out.println("----------------------------------------------\n" +
                " lasStem -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Attempts to delineate an understory tree layer \n" +
                "from .las file. Requires very high point density \n" +
                "point cloud data from a forested environment.\n" +
                "\n" +
                "\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)");
    }


    public static void lasLayer(){
        System.out.println("----------------------------------------------\n" +
                " lasLayer -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Attempts to delineate an understory tree layer \n" +
                "from .las file. Requires very high point density \n" +
                "point cloud data from a forested environment.\n" +
                "\n" +
                "\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)");
    }

    public static void lasITDstats(){
        System.out.println("----------------------------------------------\n" +
                " lasITDstats -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Computes tree-wise statistics from crown segmented (lasITD.sh)\n" +
                ".las file. Also produces outputs for tree trunks, should they \n" +
                "be available, i.e. if the .las file has been processed\n" +
                "with lasStem.sh.\n" +
                "\n" +
                "If field measured trees (-measured_trees) are available,\n" +
                "the code will link the crown segments to field measured\n" +
                "trees. This is done by using two kd-trees. Only trees that are\n" +
                "<2.5 m within each other and are each other's closest neighbors\n" +
                "are considered pairs.\n" +
                "\n" +
                "If (-poly) is provided, only trees within the boundaries of the \n" +
                "polygon are considered. \n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tName of the output file");
    }


    public static void lasCheck(){
        System.out.println("----------------------------------------------\n" +
                " lasCheck -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Checks that the contents of the .las file are \n" +
                "coherent with the header information.\n" +
                "\n" +
                "Also does various other checks which are reported\n" +
                "at the end of the run.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)");
    }


    public static void lasSplit(){
        System.out.println("----------------------------------------------\n" +
                " lasSplit -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "A tool to split a LAS file under various criterion \n" +
                "with the flag -splitBy \"classification\" / \"pointSourceId\" /\n" +
                "\"return\" / \"userData\" / \"gps\". GPS splitting is a special case,\n" +
                "where flight lines are extracted from the las file based\n" +
                "on -interval flag. Interval determines the maximum\n" +
                "gap between consecutive points in a GPS sorted LAS file.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-splitBy\tSplit by what criterion?\n" +
                "\t-interval\tUsed in flightline (gps) splitting\n" +
                "\t\t\tto determine bin size. (default 20.0)");
    }


    public static void lasSort(){
        System.out.println("----------------------------------------------\n" +
                " lasSort -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Sorts the points in a .las file. Can either be \n" +
                "set to \n" +
                "\n" +
                "-by_gps_time (default)\n" +
                "\n" +
                "or\n" +
                "\n" +
                "-by_z_order\n" +
                "\n" +
                "\n" +
                "Z-order is very useful to do prior to indexing.\n" +
                "\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-by_gps_time\tOrder by time\n" +
                "\t-by_z_order\tOrder by z-order");
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

    public static void lasClip(){
        System.out.println("----------------------------------------------\n" +
                " lasclip -- LASutils build $line\n" +
                "\n" +
                " (c) M.Kukkonen\n" +
                " University of Eastern Finland\n" +
                "----------------------------------------------\n" +
                "\n" +
                "Clips .las files. The output can be either a las\n" +
                "file (default) or .txt file (-otype txt). The output\n" +
                "can be merged (default) or splitted (-split). The split\n" +
                "option does NOT split by polygon features, but rather\n" +
                "by point clouds, i.e. one output file per input \n" +
                "point cloud.\n" +
                "\n" +
                "The -omet flag is used to output stastical features\n" +
                "of points within the boundaries of individual polygon\n" +
                "features.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file\n" +
                "\t-odir\t\tOutput directory\n" +
                "\t-otype\t\tOutput file type, \"txt\" or \"las\"\n" +
                "\t-poly\t\tInput polygon (.shp, wkt, txt)\n" +
                "\t-sep \t\ttxt file field separator\n" +
                "\t-omet\t\tOutput statistical metrics");
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
                "point detection. In short, the tool does not take the lowest\n" +
                "point within the -axGrid, but rather computes a smaller grid\n" +
                "within the -axGrid and only accepts points areas where standard\n" +
                "deviation of point heights is in acceptable range.\n" +
                "This only affects the detection of seed points\n" +
                "and has no effect on the following densification of the surface.\n" +
                "\n" +
                "Usage:\n" +
                "\n" +
                "\t-i\t\tInput file(s)\n" +
                "\t-o\t\tOutput file\n" +
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
