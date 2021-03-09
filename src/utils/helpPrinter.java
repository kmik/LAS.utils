package utils;

public class helpPrinter {

    public static void printHelp(int tool){


        if(tool == 1){
            System.out.printf("----------------------------------------------\n");
            System.out.printf("     lasclip -- LASutils build $line\n");
            System.out.printf(" \n");
            System.out.printf("        (c) M.Kukkonen \n");
            System.out.printf("University of Eastern Finland\n");
            System.out.printf("----------------------------------------------\n");

            System.out.printf("        Clips .las files. The output can be either a las\n");
            System.out.printf("        file (default) or .txt file (-otype txt). The output\n");
            System.out.printf("        can be merged (default) or splitted (-split).\n");
            System.out.printf(" \n");
            System.out.printf("        Usage:\n");
            System.out.printf(" \n");
            System.out.printf("    -i		    Input file(s) \n");
            System.out.printf("    -o		    Output file\n");
            System.out.printf("    -odir	    Output directory\n");
            System.out.printf("    -poly	    Input polygon (.shp, wkt, txt)\n");
            System.out.printf("    -sep 	    txt file field separator\n");
            System.out.printf("    -omet 	    Output metrics alongside the clipped .las file\n");


        }

    }
}
