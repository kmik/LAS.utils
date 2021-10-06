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

            default:
                break;
        }
    }


    public static void las2las(){
        System.out.println("HAHAHAHA");
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
