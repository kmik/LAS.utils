package utils;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.FastMath;

public class pointCloudMetrics {

    public double percentile_step_orig = 0.05;

    public double[] densities = new double[]{1.3, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0};

    public boolean square = true;
    public boolean clip_to_circle = false;
    public boolean clip_to_hexagon = false;

    // THIS IS FOR THE HEXAGON
    public double r = 9.0;

    //public double r = 7.5;

    double cutoff = 0;
    double cutoff_n_points = 10;
    static double TWOPI = 6.2831853071795865;
    static double RAD2DEG = 57.2957795130823209;
    static double SQRT_3 = Math.sqrt(3.0);

    int x_dim_ = 0, y_dim_ = 0, z_dim_ = 0;

    // THIS IS FOR THE HEXAGON
    //static double convolution_image_width =     20.0;
    public double convolution_image_width =     16.0;

    // THIS IS FOR THE HEXAGON
    //static double convolution_image_height =    20.0;
    public double convolution_image_height =    16.0;
    static double convolution_image_resolution = 1.0;
    //static double convolution_image_resolution = 0.8888889;
    static double convolution_image_resolution_x = convolution_image_resolution;
    static double convolution_image_resolution_y = convolution_image_resolution;
    static double convolution_image_resolution_z = 1.0;
    static double convolution_image_depth =     35.0;
    static double angle_increment =             45.0;
    public double diagonal = Math.sqrt((convolution_image_height*convolution_image_height)+(convolution_image_height*convolution_image_height));

    public pointCloudMetrics(argumentReader aR){
        this.densities = aR.densities;
        this.percentile_step_orig = aR.percentiles;
        this.cutoff = aR.z_cutoff;
        this.cutoff_n_points = aR.min_points;

        if(aR.res != 15){
            convolution_image_resolution = aR.res;
            convolution_image_resolution_x = aR.res;
            convolution_image_resolution_y = aR.res;
        }

        if(aR.image_height != 0){
            this.convolution_image_width = aR.image_height;
            this.convolution_image_height = aR.image_height;
            this.diagonal = Math.sqrt((convolution_image_height*convolution_image_height)+(convolution_image_height*convolution_image_height));
        }

    }

    public static boolean pointInPolygon(double[] point, double[][] poly) {


        int numPolyCorners = poly.length;
        int j = numPolyCorners - 1;
        boolean isInside = false;

        for (int i = 0; i < numPolyCorners; i++) {
            if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                    isInside = !isInside;
                }
            }
            j = i;
        }
        return isInside;
    }

    
    public ArrayList<ArrayList<Double>> calc_nn_input_train(ArrayList<double[]> p, String suffix, ArrayList<String> colnames, double top_left_x, double top_left_y){

        double center_x = top_left_x + diagonal / 2.0;
        double center_y = top_left_y - diagonal / 2.0;
        ArrayList<ArrayList<Double>> output = new ArrayList<>();
        /* p contains points from a buffered point cloud that has a width equal to the diagonal of the convolution_image_width */

        //ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        double max_x = center_x + convolution_image_width / 2.0;
        double max_y = center_y + convolution_image_height / 2.0;
        double min_x = center_x - convolution_image_width / 2.0;
        double min_y = center_y - convolution_image_height / 2.0;

        /* circle_diameter = a√2
        *
        *  a^2 * 2 = circle_diameter^2
        *   a^2 = circle_diameter^2 / 2
        * a = Math.sqrt(circle_diameter^2 / 2 )
        *
        * */

        //double square_side_length = Math.sqrt(circle_diameter * circle_diameter / 2.0 );
        double[] origin = new double[]{center_x, center_y};
        double angle = angle_increment;

        double point_count = (double)p.size();

        int z_dim = (int)((convolution_image_depth) / convolution_image_resolution);
        int x_dim = (int)(convolution_image_width / convolution_image_resolution);
        int y_dim = (int)(convolution_image_height / convolution_image_resolution);

        double[][][] grid = new double[x_dim][y_dim][z_dim];

        //System.out.println(Arrays.toString(p.get(0)));
        boolean first = true;


        while(angle < 360) {

            for (double[] p_ : p) {

                if(p_[0] > center_x - convolution_image_width / 2.0 && p_[0] < center_x + convolution_image_width / 2.0 &&
                        p_[1] > center_y - convolution_image_width / 2.0 && p_[1] < center_y + convolution_image_width / 2.0){

                    int x = (int)Math.min((int)((p_[0] - min_x) / convolution_image_resolution), x_dim - 1);
                    int y = (int)Math.min((int)((max_y - p_[1]) / convolution_image_resolution), y_dim - 1);
                    int z = Math.max((int)Math.min((int)((p_[2] - 0) / convolution_image_resolution), z_dim - 1), 0);

                    grid[x][y][z]++;

                }
            }

            output.add(resetGrid(grid, point_count));
            System.out.println(output.get(output.size()-1).toArray().length);

            if(first){

                for(int z=0 ; z<grid[0][0].length ; z++){
                    for(int x=0 ; x<grid.length ; x++){
                        for(int y=0 ; y<grid[0].length ; y++){
                            //out.add((double)grid[i][j][k] / n_points);
                            colnames.add(x + "_" + y + "_" + z);
                            //grid[i][j][k] = 0;
                        }
                    }
                }

            }
            rotatePoints(origin, angle_increment, p);
            angle += angle_increment;

            first = false;
        }

        System.out.println(output.size());

        return output;
    }

    public ArrayList<ArrayList<Double>> calc_nn_input_train_spectral(ArrayList<double[]> p, String suffix, ArrayList<String> colnames, double top_left_x, double top_left_y,
                                                                        double bottom_right_x, double bottom_right_y){


        double center_x = top_left_x + diagonal / 2.0;
        double center_y = top_left_y - diagonal / 2.0;

        center_x = top_left_x + ((bottom_right_x - top_left_x) / 2.0);
        center_y = top_left_y - ((top_left_y - bottom_right_y) / 2.0);

        double grid_top_left_x = center_x - convolution_image_width / 2.0;
        double grid_top_left_y = center_y + convolution_image_width / 2.0;

        double[][] hexagon = new double[7][2];
/*
        hexagon[0][0] = center_x - (r); hexagon[0][1] = center_y;
        hexagon[1][0] = center_x - (r / 2.0); hexagon[1][1] = center_y + (SQRT_3 * r / 2.0);
        hexagon[2][0] = center_x + (r / 2.0); hexagon[2][1] = center_y + (SQRT_3 * r / 2.0);
        hexagon[3][0] = center_x + (r); hexagon[3][1] = center_y;
        hexagon[4][0] = center_x + (r / 2.0); hexagon[4][1] = center_y - (SQRT_3 * r / 2.0);
        hexagon[5][0] = center_x - (r / 2.0); hexagon[5][1] = center_y - (SQRT_3 * r / 2.0);
        hexagon[6][0] = center_x - (r); hexagon[6][1] = center_y;
*/

        hexagon[0][0] = center_x; hexagon[0][1] = center_y - r;
        hexagon[1][0] = center_x - (SQRT_3 * r / 2.0); hexagon[1][1] = center_y - (r / 2.0);
        hexagon[2][0] = center_x - (SQRT_3 * r / 2.0); hexagon[2][1] = center_y + (r / 2.0);
        hexagon[3][0] = center_x; hexagon[3][1] = center_y + r;
        hexagon[4][0] = center_x + (SQRT_3 * r / 2.0); hexagon[4][1] = center_y + (r / 2.0);
        hexagon[5][0] = center_x + (SQRT_3 * r / 2.0); hexagon[5][1] = center_y - (r / 2.0);
        hexagon[6][0] = center_x; hexagon[6][1] = center_y - r;

        ArrayList<ArrayList<Double>> output = new ArrayList<>();
        /* p contains points from a buffered point cloud that has a width equal to the diagonal of the convolution_image_width */

        //ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        //System.out.println("dim: " + (bottom_right_x-top_left_x) + " " + (top_left_y-bottom_right_y));

        double max_x = center_x + convolution_image_width / 2.0;
        double max_y = center_y + convolution_image_height / 2.0;
        double min_x = center_x - convolution_image_width / 2.0;
        double min_y = center_y - convolution_image_height / 2.0;

        /* circle_diameter = a√2
         *
         *  a^2 * 2 = circle_diameter^2
         *   a^2 = circle_diameter^2 / 2
         * a = Math.sqrt(circle_diameter^2 / 2 )
         *
         * */

        //double square_side_length = Math.sqrt(circle_diameter * circle_diameter / 2.0 );
        double[] origin = new double[]{center_x, center_y};
        double angle = angle_increment;

        double point_count = (double)p.size();

        int z_dim = (int)Math.ceil((convolution_image_depth) / convolution_image_resolution_z);
        int x_dim = (int)Math.ceil(convolution_image_width / convolution_image_resolution_x);
        int y_dim = (int)Math.ceil(convolution_image_height / convolution_image_resolution_y);

        this.x_dim_ = x_dim;
        this.y_dim_ = y_dim;
        this.z_dim_ = z_dim;

        double[][][] grid = new double[x_dim][y_dim][z_dim];
        double[][][] grid_R = new double[x_dim][y_dim][z_dim];
        double[][][] grid_G = new double[x_dim][y_dim][z_dim];
        double[][][] grid_B = new double[x_dim][y_dim][z_dim];
        double[][][] grid_N = new double[x_dim][y_dim][z_dim];

        byte[][] grid_mask = new byte[x_dim][y_dim];
        //char[][] grid_mask_2 = new char[x_dim][y_dim];

        for(int x = 0; x < x_dim; x++){
            for(int y = 0; y < y_dim; y++){

                double x_coord = grid_top_left_x + convolution_image_resolution_x * x + convolution_image_resolution_x / 2.0;
                double y_coord = grid_top_left_y - convolution_image_resolution_y * y - convolution_image_resolution_y / 2.0;

                if(pointInPolygon(new double[]{x_coord, y_coord}, hexagon)){
                    grid_mask[x][y] = 1;
                    //grid_mask_2[x][y] = '@';
                }else{
                    //grid_mask_2[x][y] = '-';

                }
            }
        }

        //for(int i = 0; i < grid_mask.length; i++)
        //    System.out.println(Arrays.toString(grid_mask[i]));

        //System.exit(1);
        //if( (bottom_right_x-top_left_x) > 8)
        //    System.exit(1);
        //System.out.println(Arrays.toString(p.get(0)));
        boolean first = true;

        for(int i = 0; i < 1; i++) {

            while (angle < 360) {

                for (double[] p_ : p) {

                    if(clip_to_circle){

                        if(euclideanDistance(p_[0], p_[1], center_x, center_y) >= r){
                            continue;
                        }

                    }
                    if(clip_to_hexagon){

                        if(!pointInPolygon(new double[]{p_[0], p_[1]}, hexagon)){
                            continue;
                        }

                    }

                    if (p_[0] > center_x - convolution_image_width / 2.0 && p_[0] < center_x + convolution_image_width / 2.0 &&
                            p_[1] > center_y - convolution_image_width / 2.0 && p_[1] < center_y + convolution_image_width / 2.0) {

                        int x = (int) Math.min((int) ((p_[0] - min_x) / convolution_image_resolution_x), x_dim - 1);
                        int y = (int) Math.min((int) ((max_y - p_[1]) / convolution_image_resolution_y), y_dim - 1);
                        int z = Math.max((int) Math.min((int) ((p_[2] - 0) / convolution_image_resolution_z), z_dim - 1), 0);

                        if (grid_mask[x][y] == 1 || square) {


                            if (p_[2] <= 2) {
                                continue;
                            }

                            grid[x][y][z]++;
                            grid_R[x][y][z] += p_[3];
                            grid_G[x][y][z] += p_[4];
                            grid_B[x][y][z] += p_[5];
                            grid_N[x][y][z] += p_[6];

                        }


                    }
                }

                output.add(resetGrid_spectral(grid, grid_R, grid_G, grid_B, grid_N, point_count, grid_mask));
                //System.out.println(output.get(output.size()-1).toArray().length);

                String[] prefs = new String[]{"struct_", "R_", "G_", "B_", "N_"};

                if (first) {


                    for (int z = 0; z < grid[0][0].length; z++) {
                        for (int x = 0; x < grid.length; x++) {

                            for (int y = 0; y < grid[0].length; y++) {
                                //out.add((double)grid[i][j][k] / n_points);
                                //if(grid_mask[x][y] == 1)
                                for (int pref = 0; pref < 5; pref++)
                                    colnames.add(prefs[pref] + x + "_" + y + "_" + z);
                                //grid[i][j][k] = 0;
                            }
                        }
                    }

                }

                rotatePoints(origin, angle_increment, p);
                angle += angle_increment;
                first = false;

                //break;
            }
            mirrorPoints(origin, p);
            angle = angle_increment;
        }
        return output;
    }


    public ArrayList<Double> calc_nn_input_test(ArrayList<double[]> p, String suffix, ArrayList<String> colnames, double top_left_x, double top_left_y){

        double center_x = top_left_x + convolution_image_width / 2.0;
        double center_y = top_left_y - convolution_image_height / 2.0;
        ArrayList<Double> output = new ArrayList<>();
        /* p contains points from a buffered point cloud that has a width equal to the diagonal of the convolution_image_width */

        //ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        double max_x = center_x + convolution_image_width / 2.0;
        double max_y = center_y + convolution_image_height / 2.0;
        double min_x = center_x - convolution_image_width / 2.0;
        double min_y = center_y - convolution_image_height / 2.0;

        /* circle_diameter = a√2
         *
         *  a^2 * 2 = circle_diameter^2
         *   a^2 = circle_diameter^2 / 2
         * a = Math.sqrt(circle_diameter^2 / 2 )
         *
         * */

        //double square_side_length = Math.sqrt(circle_diameter * circle_diameter / 2.0 );
        double[] origin = new double[]{center_x, center_y};
        double angle = angle_increment;

        double point_count = (double)p.size();

        int z_dim = (int)((convolution_image_depth) / convolution_image_resolution);
        int x_dim = (int)(convolution_image_width / convolution_image_resolution);
        int y_dim = (int)(convolution_image_height / convolution_image_resolution);

        double[][][] grid = new double[x_dim][y_dim][z_dim];

        //System.out.println(Arrays.toString(p.get(0)));
        boolean first = true;

            for (double[] p_ : p) {

                //if(p_[0] > center_x - convolution_image_width / 2.0 && p_[0] < center_x + convolution_image_width / 2.0 &&
                        //p_[1] > center_y - convolution_image_width / 2.0 && p_[1] < center_y + convolution_image_width / 2.0){

                    int x = (int)Math.min((int)((p_[0] - min_x) / convolution_image_resolution), x_dim - 1);
                    int y = (int)Math.min((int)((max_y - p_[1]) / convolution_image_resolution), y_dim - 1);
                    int z = Math.max((int)Math.min((int)((p_[2] - 0) / convolution_image_resolution), z_dim - 1), 0);

                    grid[x][y][z]++;

                //}
            }

            output = (resetGrid_colnames(grid, point_count, colnames));



        //System.out.println("-------------------");

        return output;
    }

    public ArrayList<Double> calc_nn_input_test_spectral(ArrayList<double[]> p, String suffix, ArrayList<String> colnames, double top_left_x, double top_left_y,
                                                         double bottom_right_x, double bottom_right_y){

        double center_x = top_left_x + convolution_image_width / 2.0;
        double center_y = top_left_y - convolution_image_height / 2.0;
        ArrayList<Double> output = new ArrayList<>();

        /* p contains points from a buffered point cloud that has a width equal to the diagonal of the convolution_image_width */

        center_x = top_left_x + ((bottom_right_x - top_left_x) / 2.0);
        center_y = top_left_y - ((top_left_y - bottom_right_y) / 2.0);


        double grid_top_left_x = center_x - convolution_image_width / 2.0;
        double grid_top_left_y = center_y + convolution_image_width / 2.0;

        double[][] hexagon = new double[7][2];

        /*
        hexagon[0][0] = center_x - (r); hexagon[0][1] = center_y;
        hexagon[1][0] = center_x - (r / 2.0); hexagon[1][1] = center_y + (SQRT_3 * r / 2.0);
        hexagon[2][0] = center_x + (r / 2.0); hexagon[2][1] = center_y + (SQRT_3 * r / 2.0);
        hexagon[3][0] = center_x + (r); hexagon[3][1] = center_y;
        hexagon[4][0] = center_x + (r / 2.0); hexagon[4][1] = center_y - (SQRT_3 * r / 2.0);
        hexagon[5][0] = center_x - (r / 2.0); hexagon[5][1] = center_y - (SQRT_3 * r / 2.0);
        hexagon[6][0] = center_x - (r); hexagon[6][1] = center_y;
*/

        hexagon[0][0] = center_x; hexagon[0][1] = center_y - r;
        hexagon[1][0] = center_x - (SQRT_3 * r / 2.0); hexagon[1][1] = center_y - (r / 2.0);
        hexagon[2][0] = center_x - (SQRT_3 * r / 2.0); hexagon[2][1] = center_y + (r / 2.0);
        hexagon[3][0] = center_x; hexagon[3][1] = center_y + r;
        hexagon[4][0] = center_x + (SQRT_3 * r / 2.0); hexagon[4][1] = center_y + (r / 2.0);
        hexagon[5][0] = center_x + (SQRT_3 * r / 2.0); hexagon[5][1] = center_y - (r / 2.0);
        hexagon[6][0] = center_x; hexagon[6][1] = center_y - r;


        //ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        double max_x = center_x + convolution_image_width / 2.0;
        double max_y = center_y + convolution_image_height / 2.0;
        double min_x = center_x - convolution_image_width / 2.0;
        double min_y = center_y - convolution_image_height / 2.0;

        /* circle_diameter = a√2
         *
         *  a^2 * 2 = circle_diameter^2
         *   a^2 = circle_diameter^2 / 2
         * a = Math.sqrt(circle_diameter^2 / 2 )
         *
         * */

        //double square_side_length = Math.sqrt(circle_diameter * circle_diameter / 2.0 );
        double[] origin = new double[]{center_x, center_y};
        double angle = angle_increment;

        double point_count = (double)p.size();

        int z_dim = (int)Math.ceil((convolution_image_depth) / convolution_image_resolution_z);
        int x_dim = (int)Math.ceil(convolution_image_width / convolution_image_resolution_x);
        int y_dim = (int)Math.ceil(convolution_image_height / convolution_image_resolution_y);

        this.x_dim_ = x_dim;
        this.y_dim_ = y_dim;
        this.z_dim_ = z_dim;

        double[][][] grid = new double[x_dim][y_dim][z_dim];
        double[][][] grid_R = new double[x_dim][y_dim][z_dim];
        double[][][] grid_G = new double[x_dim][y_dim][z_dim];
        double[][][] grid_B = new double[x_dim][y_dim][z_dim];
        double[][][] grid_N = new double[x_dim][y_dim][z_dim];
        //System.out.println(Arrays.toString(p.get(0)));
        boolean first = true;

        byte[][] grid_mask = new byte[x_dim][y_dim];
        char[][] grid_mask_2 = new char[x_dim][y_dim];

        for(int x = 0; x < x_dim; x++){
            for(int y = 0; y < y_dim; y++){

                double x_coord = grid_top_left_x + convolution_image_resolution_x * x + convolution_image_resolution_x / 2.0;
                double y_coord = grid_top_left_y - convolution_image_resolution_y * y - convolution_image_resolution_y / 2.0;

                //if(euclideanDistance(x_coord, y_coord, center_x, center_y) < 9.0){
                //    grid_mask[x][y] = 1;
                //}

                if(pointInPolygon(new double[]{x_coord, y_coord}, hexagon)){
                    grid_mask[x][y] = 1;
                    //grid_mask_2[x][y] = '@';
                }else{
                    //grid_mask_2[x][y] = '-';

                }
            }
        }

        //for(int i = 0; i < grid_mask.length; i++)
        //    System.out.println(Arrays.toString(grid_mask_2[i]));

        //if( (bottom_right_x-top_left_x) > 8)
        //    System.exit(1);

        for (double[] p_ : p) {

            //if(p_[0] > center_x - convolution_image_width / 2.0 && p_[0] < center_x + convolution_image_width / 2.0 &&
            //p_[1] > center_y - convolution_image_width / 2.0 && p_[1] < center_y + convolution_image_width / 2.0){
            if(clip_to_circle){

                if(euclideanDistance(p_[0], p_[1], center_x, center_y) >= r){
                    continue;
                }

            }

            if(clip_to_hexagon){

                if(!pointInPolygon(new double[]{p_[0], p_[1]}, hexagon)){
                    continue;
                }

            }

            int x = (int)Math.min((int)((p_[0] - min_x) / convolution_image_resolution_x), x_dim - 1);
            int y = (int)Math.min((int)((max_y - p_[1]) / convolution_image_resolution_y), y_dim - 1);
            int z = Math.max((int)Math.min((int)((p_[2] - 0) / convolution_image_resolution_z), z_dim - 1), 0);

            if(p_[0] > center_x - convolution_image_width / 2.0 && p_[0] < center_x + convolution_image_width / 2.0 &&
                    p_[1] > center_y - convolution_image_width / 2.0 && p_[1] < center_y + convolution_image_width / 2.0) {
                if (grid_mask[x][y] == 1 || square) {



                    if (p_[2] <= 2) {
                        continue;
                    }

                    grid[x][y][z]++;
                    grid_R[x][y][z] += p_[3];
                    grid_G[x][y][z] += p_[4];
                    grid_B[x][y][z] += p_[5];
                    grid_N[x][y][z] += p_[6];
                }
            }
            //}
        }

        //System.out.println(x_dim + " " + y_dim + " " + z_dim);

        output = (resetGrid_colnames_spectral(grid, grid_R, grid_G, grid_B, grid_N, point_count, colnames, grid_mask));

        return output;
    }


    ArrayList<Double> resetGrid(double[][][] array, double n_points){

        ArrayList<Double> out = new ArrayList<>();

        for(int z=0 ; z<array[0][0].length ; z++){
            for(int x=0 ; x<array.length ; x++){
                for(int y=0 ; y<array[0].length ; y++){


                    if(array[x][y][z] > 0) {
                        out.add(1.0);

                    }
                    else {
                        out.add(0.0);
                    }


                    //out.add((double)array[i][j][k] / n_points);
                    array[x][y][z] = 0;
                }
            }
        }
        return out;
    }

    ArrayList<Double> resetGrid_spectral(double[][][] array, double[][][] array_R, double[][][] array_G, double[][][] array_B, double[][][] array_N, double n_points,
                                         byte[][] grid_mask){

        ArrayList<Double> out = new ArrayList<>();
        ArrayList<Double> out_struct = new ArrayList<>();
        ArrayList<Double> out_R = new ArrayList<>();
        ArrayList<Double> out_G = new ArrayList<>();
        ArrayList<Double> out_B = new ArrayList<>();
        ArrayList<Double> out_N = new ArrayList<>();

        for(int z=0 ; z<array[0][0].length ; z++){


            for(int x=0 ; x<array.length ; x++){
                for(int y=0 ; y<array[0].length ; y++){


                    if(grid_mask[x][y] == 1 || square) {

                        if (array[x][y][z] > 0) {


                            int surrounding_points = 1;

                            for (int x_ = x - 1; x_ <= x + 1; x_++) {
                                for (int y_ = y - 1; y_ <= y + 1; y_++) {
                                    for (int z_ = z - 1; z_ <= z + 1; z_++) {
                                        if (x_ >= 0 && y_ >= 0 && z_ >= 0 && x_ < x_dim_ &&
                                                y_ < y_dim_ && z_ < z_dim_) {

                                            surrounding_points += array[x_][y_][z_];
                                        }


                                    }
                                }
                            }

                            out_struct.add(array[x][y][z] / surrounding_points);
                            //System.out.println("Prop: " + (array[x][y][z] / surrounding_points));
                            out_R.add(array_R[x][y][z] / array[x][y][z]);
                            out_G.add(array_G[x][y][z] / array[x][y][z]);
                            out_B.add(array_B[x][y][z] / array[x][y][z]);
                            out_N.add(array_N[x][y][z] / array[x][y][z]);


                            out.add(array[x][y][z] / surrounding_points);
                            out.add(array_R[x][y][z] / array[x][y][z]);
                            out.add(array_G[x][y][z] / array[x][y][z]);
                            out.add(array_B[x][y][z] / array[x][y][z]);
                            out.add(array_N[x][y][z] / array[x][y][z]);



                        } else {
                            out_struct.add(0.0);
                            out_R.add(0.0);
                            out_G.add(0.0);
                            out_B.add(0.0);
                            out_N.add(0.0);

                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);

                        }
                    }else{
                        out_struct.add(0.0);
                        out_R.add(0.0);
                        out_G.add(0.0);
                        out_B.add(0.0);
                        out_N.add(0.0);

                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);

                    }
                }
            }

            //out.addAll(out_R);
            //out.addAll(out_G);
            //out.addAll(out_B);
            //out.addAll(out_N);

            //out_R.clear();
            //out_G.clear();
           // out_B.clear();
           // out_N.clear();

        }

        //out.addAll(out_struct);
        //out.addAll(out_R);
        //out.addAll(out_G);
        //out.addAll(out_B);
        //out.addAll(out_N);


        for(int z=0 ; z<array[0][0].length ; z++) {


            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[0].length; y++) {

                    array[x][y][z] = 0;
                    array_R[x][y][z] = 0;
                    array_G[x][y][z] = 0;
                    array_B[x][y][z] = 0;
                    array_N[x][y][z] = 0;
                }

            }

        }
        return out;
    }

    ArrayList<Double> resetGrid_colnames(double[][][] array, double n_points, ArrayList<String> colnames){

        ArrayList<Double> out = new ArrayList<>();

        for(int z=0 ; z<array[0][0].length ; z++){
            for(int x=0 ; x<array.length ; x++){
                for(int y=0 ; y<array[0].length ; y++){

                    if(array[x][y][z] > 0)
                        out.add(1.0);
                    else
                        out.add(0.0 );

                    //out.add((double)array[i][j][k] / n_points);
                    array[x][y][z] = 0;
                    colnames.add(x + "_" + y + "_" + z);

                }
            }
        }
        return out;
    }

    ArrayList<Double> resetGrid_colnames_spectral(double[][][] array, double[][][] array_R, double[][][] array_G, double[][][] array_B, double[][][] array_N, double n_points, ArrayList<String> colnames,
                                                  byte[][] grid_mask){

        String[] prefs = new String[]{"struct_", "R_", "G_", "B_", "N_"};
        ArrayList<Double> out = new ArrayList<>();
        ArrayList<Double> out_struct = new ArrayList<>();
        ArrayList<Double> out_R = new ArrayList<>();
        ArrayList<Double> out_G = new ArrayList<>();
        ArrayList<Double> out_B = new ArrayList<>();
        ArrayList<Double> out_N = new ArrayList<>();

        for(int z=0 ; z<array[0][0].length ; z++){


            for(int x=0 ; x<array.length ; x++){
                for(int y=0 ; y<array[0].length ; y++){

                    if(grid_mask[x][y] == 1 || square) {
                        if (array[x][y][z] > 0) {
                            int surrounding_points = 1;

                            for (int x_ = x - 1; x_ <= x + 1; x_++) {
                                for (int y_ = y - 1; y_ <= y + 1; y_++) {
                                    for (int z_ = z - 1; z_ <= z + 1; z_++) {

                                        if (x_ >= 0 && y_ >= 0 && z_ >= 0 && x_ < x_dim_ &&
                                                y_ < y_dim_ && z_ < z_dim_) {
                                            surrounding_points += array[x_][y_][z_];

                                        }
                                    }
                                }
                            }

                            //if(surrounding_points > 1)
                            //    System.out.println("what is this: " + array[x][y][z] / surrounding_points + " " + array[x][y][z] + " " + surrounding_points);

                            //out_struct.add(array[x][y][z] / surrounding_points);
                            out_struct.add(array[x][y][z]);

                            out_R.add(array_R[x][y][z] / array[x][y][z]);
                            out_G.add(array_G[x][y][z] / array[x][y][z]);
                            out_B.add(array_B[x][y][z] / array[x][y][z]);
                            out_N.add(array_N[x][y][z] / array[x][y][z]);

                            //out.add(array[x][y][z] / surrounding_points);
                            out.add(array[x][y][z]);
                            out.add(array_R[x][y][z] / array[x][y][z]);
                            out.add(array_G[x][y][z] / array[x][y][z]);
                            out.add(array_B[x][y][z] / array[x][y][z]);
                            out.add(array_N[x][y][z] / array[x][y][z]);


                        } else {
                            out_struct.add(0.0);
                            out_R.add(0.0);
                            out_G.add(0.0);
                            out_B.add(0.0);
                            out_N.add(0.0);

                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);
                            out.add(0.0);

                        }
                    }else{
                        out_struct.add(0.0);
                        out_R.add(0.0);
                        out_G.add(0.0);
                        out_B.add(0.0);
                        out_N.add(0.0);

                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);
                        out.add(0.0);

                    }
                }
            }


/*
            out.addAll(out_R);
            out.addAll(out_G);
            out.addAll(out_B);
            out.addAll(out_N);

            out_R.clear();
            out_G.clear();
            out_B.clear();
            out_N.clear();
*/
        }

        //out.addAll(out_struct);
        //out.addAll(out_R);
        //out.addAll(out_G);
       // out.addAll(out_B);
        //out.addAll(out_N);

        for(int z=0 ; z<array[0][0].length ; z++) {


            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[0].length; y++) {

                    array[x][y][z] = 0;
                    array_R[x][y][z] = 0;
                    array_G[x][y][z] = 0;
                    array_B[x][y][z] = 0;
                    array_N[x][y][z] = 0;
                }

            }

        }

            for(int z=0 ; z<array[0][0].length ; z++){

                for(int x=0 ; x<array.length ; x++){
                        for (int y = 0; y < array[0].length; y++) {
                            //out.add((double)grid[i][j][k] / n_points);
                            //if(grid_mask[x][y] == 1) {
                            for(int pref = 0; pref < 5; pref++)
                                colnames.add(prefs[pref] + x + "_" + y + "_" + z);
                            //}
                            //grid[i][j][k] = 0;
                        }

                }
        }

        return out;
    }

    public double bearing(double a1, double a2, double b1, double b2) {

        // if (a1 = b1 and a2 = b2) throw an error
        double theta = FastMath.atan2(b1 - a1, a2 - b2);

        if (theta < 0.0)
            theta += TWOPI;

        return RAD2DEG * theta;

    }

    public void mirrorPoints(double[] origin, ArrayList<double[]> p){
        for(double[] p_ : p){

            mirrorPoint(origin, p_);

        }
    }
    public void rotatePoints(double[] origin, double angle, ArrayList<double[]> p){

        for(double[] p_ : p){

            rotate_point(origin, p_, angle);

        }
    }

    public void mirrorPoint(double[] origin, double[] p){

        if(p[1] < origin[1]){

            p[1] += (Math.abs(origin[1]-p[1]) * 2);
        }else if(p[1] > origin[1]){
            p[1] -= (Math.abs(origin[1]-p[1]) * 2);
        }else{

        }

    }
    public void rotate_point(double[] origin, double[] p, double angle) {

        double s = Math.sin(angle);
        double c = Math.cos(angle);

        // translate point back to origin:
        p[0] -= origin[0];
        p[1] -= origin[1];

        // rotate point
        double xnew = p[0] * c - p[1] * s;
        double ynew = p[0] * s + p[1] * c;

        // translate point back:
        p[0] = xnew + origin[0];
        p[1] = ynew + origin[1];
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    /**
     * Currently way unoptimized, can do a lot better i'm sure.
     *
     * @param z
     * @param intensity
     * @param sum_z
     * @param sum_i
     * @param suffix
     * @param colnames
     * @return
     */
    public ArrayList<Double> calc(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames){

        ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);
            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);
            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }
            return output;
        }
        //concurrentSort(z, z, intensity);

        //Collections.sort(z);
        //Collections.sort(intensity);


        ArrayList<Double> z_above_threshold = new ArrayList<>();
        ArrayList<Integer> i_above_threshold = new ArrayList<>();

        TreeSet<Integer> intensity_median_set = new TreeSet<>();

        double percentile_step_z = percentile_step_orig;
        double percentile_step_i = percentile_step_orig;

        double[] densitiesOutput = new double[densities.length];



        double z_val;
        int i_val;

        double z_sum_run = 0.0d, i_sum_run = 0.0d;
/*
        System.out.println(percentile_step_i);
        System.out.println((int)(1.0 / percentile_step_z));
*/

        double sd_z = 0;
        double sd_i = 0;

        double mean_z = sum_z / (double)z.size();
        double mean_i = sum_i / (double)intensity.size();

        sum_z = 0;
        sum_i = 0;

        double n_above_t_hold = 0;

        double max_z = Double.NEGATIVE_INFINITY;
        int max_i = Integer.MIN_VALUE;

        double min_z = Double.POSITIVE_INFINITY;
        int min_i = Integer.MAX_VALUE;

        for(int i = 0; i < z.size(); i++){

            z_val = z.get(i);
            i_val = intensity.get(i);

            for(int j = densities.length-1 ; j >= 0; j--){

                if(z_val <= densities[j]){
                    densitiesOutput[j]++;

                }

            }

            if(z_val < cutoff) {
                continue;
            }

            sum_z += z_val;
            sum_i += i_val;

            z_above_threshold.add(z_val);
            i_above_threshold.add(i_val);

            n_above_t_hold++;

            if(z_val > max_z)
                max_z = z_val;
            if(z_val < min_z)
                min_z = z_val;

            if(i_val > max_i)
                max_i = i_val;
            if(i_val < min_i)
                min_i = i_val;

        }

        if(z_above_threshold.size() < cutoff_n_points){
            /* Should we have insufficient number of points to calculate metrics,
              then we just output NaN for all metrics. This check is done here
              again because we only just now check for point that are within
              the legal boundaries.
             */
            //if(z.size() < cutoff_n_points){

                int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

                colnames.add("max_z" + suffix);
                output.add(Double.NaN);
                colnames.add("min_z" + suffix);
                output.add(Double.NaN);
                colnames.add("sd_z" + suffix);
                output.add(Double.NaN);
                colnames.add("median_z" + suffix);
                output.add(Double.NaN);
                colnames.add("mean_z" + suffix);
                output.add(Double.NaN);
                colnames.add("skewness_z" + suffix);
                output.add(Double.NaN);
                colnames.add("kurtosis_z" + suffix);
                output.add(Double.NaN);
                colnames.add("max_i" + suffix);
                output.add(Double.NaN);
                colnames.add("min_i" + suffix);
                output.add(Double.NaN);
                colnames.add("sd_i" + suffix);
                output.add(Double.NaN);
                colnames.add("median_i" + suffix);
                output.add(Double.NaN);
                colnames.add("mean_i" + suffix);
                output.add(Double.NaN);
                colnames.add("skewness_i" + suffix);
                output.add(Double.NaN);
                colnames.add("kurtosis_i" + suffix);
                output.add(Double.NaN);
                int counter22 = 0;

                double[] percentiles_names = new double[p_size];
                double percentile_step_z2 = percentile_step_orig;
                for(int i = 0; i < p_size; i++){

                    percentiles_names[i] = percentile_step_z2;
                    percentile_step_z2 += percentile_step_orig;

                }

                for(int i = 0 ; i < p_size; i++){

                    colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                    output.add(Double.NaN);

                }
                counter22 = 0;
                for(int i = 0 ; i < p_size; i++){

                    colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                    output.add(Double.NaN);
                }
                for(int i = 0; i < densities.length; i++) {

                    colnames.add("d_" + densities[i] + "_z" + suffix);
                    output.add(Double.NaN);
                }
                return output;
            //}
        }

        mean_z = sum_z / (double)z_above_threshold.size();
        mean_i = sum_i / (double)i_above_threshold.size();

        double median_z = -1, median_i = -1;

        double z_skewness_v = 0;
        double i_skewness_v = 0;

        double z_kurtosis_v = 0;
        double z_kurtosis_v_2 = 0;
        double i_kurtosis_v = 0;
        double i_kurtosis_v_2 = 0;

        int densities_counter = 0;


        double sum_z_above = 0.0;
        double sum_i_above = 0.0;

        for(int i = 0; i < z_above_threshold.size(); i++){

            z_val = z_above_threshold.get(i);
            i_val = i_above_threshold.get(i);

            z_sum_run += z_val;
            i_sum_run += i_val;

            sd_z += ((z_val - mean_z) * (z_val - mean_z));
            sd_i += ((i_val - mean_i) * (i_val - mean_i));

            z_skewness_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) );
            i_skewness_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) );

            z_kurtosis_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z));
            z_kurtosis_v_2 += ((z_val - mean_z) * (z_val - mean_z));
            i_kurtosis_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) *  (i_val - mean_i));
            i_kurtosis_v_2 += ((i_val - mean_i) * (i_val - mean_i));

            if(z_val < 0)
                z_val = 0;
            if(i_val < 0)
                i_val = 0;

        }

        sd_z = sd_z / (n_above_t_hold-1);
        sd_i = sd_i / (n_above_t_hold-1);

        sd_z = Math.sqrt(sd_z);
        sd_i = Math.sqrt(sd_i);

        double skewness_z = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * z_skewness_v / (sd_z*sd_z*sd_z);
        double skewness_i = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * i_skewness_v / (sd_i*sd_i*sd_i);


/*
        double kurtosis_z = z_kurtosis_v / (double)z.size() / (sd_z*sd_z*sd_z*sd_z) - 3.0;
        double kurtosis_i = i_kurtosis_v / (double)intensity.size() / (sd_i*sd_i*sd_i*sd_i) - 3.0;

 */
        double kurtosis_z = z_kurtosis_v / n_above_t_hold / ( ( z_kurtosis_v_2 / n_above_t_hold ) * ( z_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_i = i_kurtosis_v / n_above_t_hold / ( ( i_kurtosis_v_2 / n_above_t_hold ) * ( i_kurtosis_v_2 / n_above_t_hold ));


        Collections.sort(i_above_threshold);
        Collections.sort(z_above_threshold);

        if (z_above_threshold.size() % 2 == 0) {
            //System.out.println(z_above_threshold.size());
            median_z = ( (z_above_threshold.get(z_above_threshold.size() / 2) + z_above_threshold.get(z_above_threshold.size() / 2 - 1)) / 2.0);

            median_i = ( ((double) i_above_threshold.get(i_above_threshold.size() / 2) + (double) i_above_threshold.get(i_above_threshold.size() / 2 - 1)) / 2.0);
        }
        else {
            median_z = z_above_threshold.get(z_above_threshold.size() / 2);
            median_i = (double) i_above_threshold.get(i_above_threshold.size() / 2);
        }


        //skewness_z = 1.0 / (sd_z*sd_z*sd_z) * (z_skewness_v / (double)z.size());

        //percentiles_z[percentiles_z.length-1] = z.get(z.size()-1);
        //percentiles_i[percentiles_i.length-1] = intensity.get(intensity.size()-1);

        for(int j = 0 ; j < densities.length; j++){

            densitiesOutput[j] /= z.size();

        }
/*
        System.out.println(Arrays.toString(percentiles_z) + " " + z.get(z.size()-1));
        System.out.println(Arrays.toString(densitiesOutput));
        System.out.println(sd_z + " " + sd_i + " " + skewness_z + " " + kurtosis_z);
*/
        colnames.clear();

        int counter = 0;

        double[] z_array = ArrayUtils.toPrimitive(z_above_threshold.toArray(new Double[z_above_threshold.size()]));
        double[] i_array = new double[i_above_threshold.size()];

        for(int i = 0; i < i_above_threshold.size(); i++){
            i_array[i] = (double)i_above_threshold.get(i);
        }


        Percentile p_z = new Percentile(this.percentile_step_orig);
        Percentile p_i = new Percentile(this.percentile_step_orig);

        p_z.setData(z_array);
        p_i.setData(i_array);

        int pers_size = (int)Math.ceil((0.95 / percentile_step_z));

        double[] percentiles_z = new double[pers_size];
        double[] percentiles_z_names = new double[pers_size];
        double[] percentiles_i = new double[pers_size];
        double[] percentiles_i_names = new double[pers_size];

        for(int i = 0; i < percentiles_z.length; i++){

            percentiles_z[i] = p_z.evaluate(percentile_step_z * 100.0);
            percentiles_z_names[i] = percentile_step_z;
            percentiles_i[i] = p_i.evaluate(percentile_step_z * 100.0);
            percentiles_i_names[i] = percentile_step_z;
            percentile_step_z += percentile_step_orig;

        }

        //System.out.println("end: ");



        colnames.add("max_z" + suffix);
        output.add(max_z);
        colnames.add("min_z" + suffix);
        output.add(min_z);
        colnames.add("sd_z" + suffix);
        output.add(sd_z);
        colnames.add("median_z" + suffix);
        output.add(median_z);
        colnames.add("mean_z" + suffix);
        output.add(mean_z);
        colnames.add("skewness_z" + suffix);
        output.add(skewness_z);
        colnames.add("kurtosis_z" + suffix);
        output.add(kurtosis_z);
        colnames.add("max_i" + suffix);
        output.add((double)max_i);
        colnames.add("min_i" + suffix);
        output.add((double)min_i);
        colnames.add("sd_i" + suffix);
        output.add(sd_i);
        colnames.add("median_i" + suffix);
        output.add((median_i));
        colnames.add("mean_i" + suffix);
        output.add(mean_i);
        colnames.add("skewness_i" + suffix);
        output.add(skewness_i);
        colnames.add("kurtosis_i" + suffix);
        output.add(kurtosis_i);
        counter = 0;
        for(int i = 0 ; i < percentiles_z.length; i++){

            colnames.add("p_" + (Math.round(percentiles_z_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
            output.add(percentiles_z[counter++]);

        }
        counter = 0;
        for(int i = 0 ; i < percentiles_i.length; i++){

            colnames.add("p_" + (Math.round(percentiles_i_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
            output.add(percentiles_i[counter++]);
        }
        for(int i = 0; i < densities.length; i++){

            colnames.add("d_" + densities[i] + "_z" + suffix);
            output.add(densitiesOutput[i]);

        }

/*
        System.out.println(Arrays.toString(colnames.toArray()));
        System.out.println(Arrays.toString(output.toArray()));
*/
        return output;
    }

    public ArrayList<Double> calc_with_RGB(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames,
                                           ArrayList<int[]> RGB){

        ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);

            colnames.add("max_N" + suffix);
            output.add(Double.NaN);
            colnames.add("min_N" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_N" + suffix);
            output.add(Double.NaN);
            colnames.add("median_N" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_N" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_N" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_N" + suffix);
            output.add(Double.NaN);


            return output;
        }
        //concurrentSort(z, z, intensity);

        //Collections.sort(z);
        //Collections.sort(intensity);


        ArrayList<Double> z_above_threshold = new ArrayList<>();
        ArrayList<Double> R_above_threshold = new ArrayList<>();
        ArrayList<Double> G_above_threshold = new ArrayList<>();
        ArrayList<Double> B_above_threshold = new ArrayList<>();
        ArrayList<Double> N_above_threshold = new ArrayList<>();
        ArrayList<Integer> i_above_threshold = new ArrayList<>();

        TreeSet<Integer> intensity_median_set = new TreeSet<>();

        double percentile_step_z = percentile_step_orig;
        double percentile_step_i = percentile_step_orig;

        double[] densitiesOutput = new double[densities.length];



        double z_val;
        double R_val;
        double G_val;
        double B_val;
        double N_val;

        int i_val;

        double z_sum_run = 0.0d, i_sum_run = 0.0d;
        double R_sum_run = 0.0d;
        double G_sum_run = 0.0d;
        double B_sum_run = 0.0d;
        double N_sum_run = 0.0d;
/*
        System.out.println(percentile_step_i);
        System.out.println((int)(1.0 / percentile_step_z));
*/

        double sd_z = 0;
        double sd_R = 0;
        double sd_G = 0;
        double sd_B = 0;
        double sd_N = 0;

        double sd_i = 0;

        double mean_z = sum_z / (double)z.size();
        double mean_R = 0;
        double mean_G = 0;
        double mean_B = 0;
        double mean_N = 0;

        double sum_R = 0;
        double sum_G = 0;
        double sum_B = 0;
        double sum_N = 0;

        double mean_i = sum_i / (double)intensity.size();

        sum_z = 0;
        sum_i = 0;

        double n_above_t_hold = 0;

        double max_z = Double.NEGATIVE_INFINITY;

        double max_R = Double.NEGATIVE_INFINITY;
        double max_G = Double.NEGATIVE_INFINITY;
        double max_B = Double.NEGATIVE_INFINITY;
        double max_N = Double.NEGATIVE_INFINITY;

        int max_i = Integer.MIN_VALUE;

        double min_z = Double.POSITIVE_INFINITY;
        double min_R = Double.POSITIVE_INFINITY;
        double min_G = Double.POSITIVE_INFINITY;
        double min_B = Double.POSITIVE_INFINITY;
        double min_N = Double.POSITIVE_INFINITY;

        int min_i = Integer.MAX_VALUE;

        for(int i = 0; i < z.size(); i++){

            z_val = z.get(i);

            R_val = RGB.get(i)[0];
            G_val = RGB.get(i)[1];
            B_val = RGB.get(i)[2];
            N_val = RGB.get(i)[3];

            i_val = intensity.get(i);

            for(int j = densities.length-1 ; j >= 0; j--){

                if(z_val <= densities[j]){
                    densitiesOutput[j]++;

                }

            }

            // MADE A CHANGE HERE //
            if(z_val < cutoff) {
                continue;
            }

            sum_z += z_val;
            sum_i += i_val;

            sum_R += R_val;
            sum_G += G_val;
            sum_B += B_val;
            sum_N += N_val;

            z_above_threshold.add(z_val);
            R_above_threshold.add(R_val);
            G_above_threshold.add(G_val);
            B_above_threshold.add(B_val);
            N_above_threshold.add(N_val);
            i_above_threshold.add(i_val);

            n_above_t_hold++;

            if(z_val > max_z)
                max_z = z_val;
            if(z_val < min_z)
                min_z = z_val;

            if(R_val > max_R)
                max_R = R_val;
            if(R_val < min_R)
                min_R = R_val;

            if(G_val > max_G)
                max_G = G_val;
            if(G_val < min_G)
                min_G = G_val;

            if(B_val > max_B)
                max_B = B_val;
            if(B_val < min_B)
                min_B = B_val;

            if(N_val > max_N)
                max_N = N_val;
            if(N_val < min_N)
                min_N = N_val;

            if(i_val > max_i)
                max_i = i_val;
            if(i_val < min_i)
                min_i = i_val;

        }

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z_above_threshold.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);

            colnames.add("max_N" + suffix);
            output.add(Double.NaN);
            colnames.add("min_N" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_N" + suffix);
            output.add(Double.NaN);
            colnames.add("median_N" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_N" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_N" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_N" + suffix);
            output.add(Double.NaN);



            return output;
        }

        mean_z = sum_z / (double)z_above_threshold.size();

        mean_R = sum_R / (double)z_above_threshold.size();
        mean_G = sum_G / (double)z_above_threshold.size();
        mean_B = sum_B / (double)z_above_threshold.size();
        mean_N = sum_N / (double)z_above_threshold.size();

        mean_i = sum_i / (double)i_above_threshold.size();


        double median_z = -1, median_i = -1;
        double median_R = -1;
        double median_G = -1;
        double median_B = -1;
        double median_N = -1;

        double z_skewness_v = 0;
        double R_skewness_v = 0;
        double G_skewness_v = 0;
        double B_skewness_v = 0;
        double N_skewness_v = 0;
        double i_skewness_v = 0;

        double z_kurtosis_v = 0;
        double R_kurtosis_v = 0;
        double G_kurtosis_v = 0;
        double B_kurtosis_v = 0;
        double N_kurtosis_v = 0;
        double z_kurtosis_v_2 = 0;
        double R_kurtosis_v_2 = 0;
        double G_kurtosis_v_2 = 0;
        double B_kurtosis_v_2 = 0;
        double N_kurtosis_v_2 = 0;

        double i_kurtosis_v = 0;
        double i_kurtosis_v_2 = 0;

        int densities_counter = 0;


        double sum_z_above = 0.0;
        double sum_i_above = 0.0;

        for(int i = 0; i < z_above_threshold.size(); i++){

            z_val = z_above_threshold.get(i);
            R_val = R_above_threshold.get(i);
            G_val = G_above_threshold.get(i);
            B_val = B_above_threshold.get(i);
            N_val = N_above_threshold.get(i);

            i_val = i_above_threshold.get(i);

            z_sum_run += z_val;
            R_sum_run += R_val;
            G_sum_run += G_val;
            B_sum_run += B_val;
            N_sum_run += N_val;

            i_sum_run += i_val;

            sd_z += ((z_val - mean_z) * (z_val - mean_z));
            sd_R += ((R_val - mean_R) * (R_val - mean_R));
            sd_G += ((G_val - mean_G) * (G_val - mean_G));
            sd_B += ((B_val - mean_B) * (B_val - mean_B));
            sd_N += ((N_val - mean_N) * (N_val - mean_N));

            sd_i += ((i_val - mean_i) * (i_val - mean_i));

            z_skewness_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) );
            R_skewness_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) );
            G_skewness_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) );
            B_skewness_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) );
            N_skewness_v += ((N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N) );

            i_skewness_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) );

            z_kurtosis_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z));
            R_kurtosis_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B));
            N_kurtosis_v += ((N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N));

            z_kurtosis_v_2 += ((z_val - mean_z) * (z_val - mean_z));
            R_kurtosis_v_2 += ((R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v_2 += ((G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v_2 += ((B_val - mean_B) * (B_val - mean_B));
            N_kurtosis_v_2 += ((N_val - mean_N) * (N_val - mean_N));

            i_kurtosis_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) *  (i_val - mean_i));
            i_kurtosis_v_2 += ((i_val - mean_i) * (i_val - mean_i));

            if(z_val < 0)
                z_val = 0;
            if(i_val < 0)
                i_val = 0;

        }

        sd_z = sd_z / (n_above_t_hold-1);
        sd_R = sd_R / (n_above_t_hold-1);
        sd_G = sd_G / (n_above_t_hold-1);
        sd_B = sd_B / (n_above_t_hold-1);
        sd_N = sd_N / (n_above_t_hold-1);

        sd_i = sd_i / (n_above_t_hold-1);

        sd_z = Math.sqrt(sd_z);
        sd_R = Math.sqrt(sd_R);
        sd_G = Math.sqrt(sd_G);
        sd_B = Math.sqrt(sd_B);
        sd_N = Math.sqrt(sd_N);

        sd_i = Math.sqrt(sd_i);

        double skewness_z = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * z_skewness_v / (sd_z*sd_z*sd_z);
        double skewness_R = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * R_skewness_v / (sd_R*sd_R*sd_R);
        double skewness_G = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * G_skewness_v / (sd_G*sd_G*sd_G);
        double skewness_B = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * B_skewness_v / (sd_B*sd_B*sd_B);
        double skewness_N = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * N_skewness_v / (sd_N*sd_N*sd_N);

        double skewness_i = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * i_skewness_v / (sd_i*sd_i*sd_i);


/*
        double kurtosis_z = z_kurtosis_v / (double)z.size() / (sd_z*sd_z*sd_z*sd_z) - 3.0;
        double kurtosis_i = i_kurtosis_v / (double)intensity.size() / (sd_i*sd_i*sd_i*sd_i) - 3.0;

 */
        double kurtosis_z = z_kurtosis_v / n_above_t_hold / ( ( z_kurtosis_v_2 / n_above_t_hold ) * ( z_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_R = R_kurtosis_v / n_above_t_hold / ( ( R_kurtosis_v_2 / n_above_t_hold ) * ( R_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_G = G_kurtosis_v / n_above_t_hold / ( ( G_kurtosis_v_2 / n_above_t_hold ) * ( G_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_B = B_kurtosis_v / n_above_t_hold / ( ( B_kurtosis_v_2 / n_above_t_hold ) * ( B_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_N = N_kurtosis_v / n_above_t_hold / ( ( N_kurtosis_v_2 / n_above_t_hold ) * ( N_kurtosis_v_2 / n_above_t_hold )) ;

        double kurtosis_i = i_kurtosis_v / n_above_t_hold / ( ( i_kurtosis_v_2 / n_above_t_hold ) * ( i_kurtosis_v_2 / n_above_t_hold ));


        Collections.sort(i_above_threshold);
        Collections.sort(z_above_threshold);
        Collections.sort(R_above_threshold);
        Collections.sort(G_above_threshold);
        Collections.sort(B_above_threshold);
        Collections.sort(N_above_threshold);

        if (z_above_threshold.size() % 2 == 0) {
            median_z = ( (z_above_threshold.get(z_above_threshold.size() / 2) + z_above_threshold.get(z_above_threshold.size() / 2 - 1)) / 2.0);
            median_R = ( (R_above_threshold.get(R_above_threshold.size() / 2) + R_above_threshold.get(R_above_threshold.size() / 2 - 1)) / 2.0);
            median_G = ( (G_above_threshold.get(G_above_threshold.size() / 2) + G_above_threshold.get(G_above_threshold.size() / 2 - 1)) / 2.0);
            median_B = ( (B_above_threshold.get(B_above_threshold.size() / 2) + B_above_threshold.get(B_above_threshold.size() / 2 - 1)) / 2.0);
            median_N = ( (N_above_threshold.get(N_above_threshold.size() / 2) + N_above_threshold.get(N_above_threshold.size() / 2 - 1)) / 2.0);

            median_i = ( ((double) i_above_threshold.get(i_above_threshold.size() / 2) + (double) i_above_threshold.get(i_above_threshold.size() / 2 - 1)) / 2.0);
        }
        else {
            median_z = z_above_threshold.get(z_above_threshold.size() / 2);
            median_R = R_above_threshold.get(R_above_threshold.size() / 2);
            median_G = G_above_threshold.get(G_above_threshold.size() / 2);
            median_B = B_above_threshold.get(B_above_threshold.size() / 2);
            median_N = N_above_threshold.get(N_above_threshold.size() / 2);

            median_i = (double) i_above_threshold.get(i_above_threshold.size() / 2);
        }


        //skewness_z = 1.0 / (sd_z*sd_z*sd_z) * (z_skewness_v / (double)z.size());

        //percentiles_z[percentiles_z.length-1] = z.get(z.size()-1);
        //percentiles_i[percentiles_i.length-1] = intensity.get(intensity.size()-1);

        for(int j = 0 ; j < densities.length; j++){

            densitiesOutput[j] /= z.size();

        }
/*
        System.out.println(Arrays.toString(percentiles_z) + " " + z.get(z.size()-1));
        System.out.println(Arrays.toString(densitiesOutput));
        System.out.println(sd_z + " " + sd_i + " " + skewness_z + " " + kurtosis_z);
*/
        colnames.clear();

        int counter = 0;

        double[] z_array = ArrayUtils.toPrimitive(z_above_threshold.toArray(new Double[z_above_threshold.size()]));
        double[] i_array = new double[i_above_threshold.size()];

        for(int i = 0; i < i_above_threshold.size(); i++){
            i_array[i] = (double)i_above_threshold.get(i);
        }


        Percentile p_z = new Percentile(this.percentile_step_orig);
        Percentile p_i = new Percentile(this.percentile_step_orig);

        p_z.setData(z_array);
        p_i.setData(i_array);

        int pers_size = (int)Math.ceil((0.95 / percentile_step_z));

        double[] percentiles_z = new double[pers_size];
        double[] percentiles_z_names = new double[pers_size];
        double[] percentiles_i = new double[pers_size];
        double[] percentiles_i_names = new double[pers_size];

        for(int i = 0; i < percentiles_z.length; i++){

            percentiles_z[i] = p_z.evaluate(percentile_step_z * 100.0);
            percentiles_z_names[i] = percentile_step_z;
            percentiles_i[i] = p_i.evaluate(percentile_step_z * 100.0);
            percentiles_i_names[i] = percentile_step_z;
            percentile_step_z += percentile_step_orig;

        }

        //System.out.println("end: ");



        colnames.add("max_z" + suffix);
        output.add(max_z);
        colnames.add("min_z" + suffix);
        output.add(min_z);
        colnames.add("sd_z" + suffix);
        output.add(sd_z);
        colnames.add("median_z" + suffix);
        output.add(median_z);
        colnames.add("mean_z" + suffix);
        output.add(mean_z);
        colnames.add("skewness_z" + suffix);
        output.add(skewness_z);
        colnames.add("kurtosis_z" + suffix);
        output.add(kurtosis_z);
        colnames.add("max_i" + suffix);
        output.add((double)max_i);
        colnames.add("min_i" + suffix);
        output.add((double)min_i);
        colnames.add("sd_i" + suffix);
        output.add(sd_i);
        colnames.add("median_i" + suffix);
        output.add((median_i));
        colnames.add("mean_i" + suffix);
        output.add(mean_i);
        colnames.add("skewness_i" + suffix);
        output.add(skewness_i);
        colnames.add("kurtosis_i" + suffix);
        output.add(kurtosis_i);

        counter = 0;
        for(int i = 0 ; i < percentiles_z.length; i++){

            colnames.add("p_" + (Math.round(percentiles_z_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
            output.add(percentiles_z[counter++]);

        }
        counter = 0;
        for(int i = 0 ; i < percentiles_i.length; i++){

            colnames.add("p_" + (Math.round(percentiles_i_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
            output.add(percentiles_i[counter++]);
        }
        for(int i = 0; i < densities.length; i++){

            colnames.add("d_" + densities[i] + "_z" + suffix);
            output.add(densitiesOutput[i]);

        }

        colnames.add("max_R" + suffix);
        output.add(max_R);
        colnames.add("min_R" + suffix);
        output.add(min_R);
        colnames.add("sd_R" + suffix);
        output.add(sd_R);
        colnames.add("median_R" + suffix);
        output.add(median_R);
        colnames.add("mean_R" + suffix);
        output.add(mean_R);
        colnames.add("skewness_R" + suffix);
        output.add(skewness_R);
        colnames.add("kurtosis_R" + suffix);
        output.add(kurtosis_R);

        colnames.add("max_G" + suffix);
        output.add(max_G);
        colnames.add("min_G" + suffix);
        output.add(min_G);
        colnames.add("sd_G" + suffix);
        output.add(sd_G);
        colnames.add("median_G" + suffix);
        output.add(median_G);
        colnames.add("mean_G" + suffix);
        output.add(mean_G);
        colnames.add("skewness_G" + suffix);
        output.add(skewness_G);
        colnames.add("kurtosis_G" + suffix);
        output.add(kurtosis_G);

        colnames.add("max_B" + suffix);
        output.add(max_B);
        colnames.add("min_B" + suffix);
        output.add(min_B);
        colnames.add("sd_B" + suffix);
        output.add(sd_B);
        colnames.add("median_B" + suffix);
        output.add(median_B);
        colnames.add("mean_B" + suffix);
        output.add(mean_B);
        colnames.add("skewness_B" + suffix);
        output.add(skewness_B);
        colnames.add("kurtosis_B" + suffix);
        output.add(kurtosis_B);

        colnames.add("max_N" + suffix);
        output.add(max_N);
        colnames.add("min_N" + suffix);
        output.add(min_N);
        colnames.add("sd_N" + suffix);
        output.add(sd_N);
        colnames.add("median_N" + suffix);
        output.add(median_N);
        colnames.add("mean_N" + suffix);
        output.add(mean_N);
        colnames.add("skewness_N" + suffix);
        output.add(skewness_N);
        colnames.add("kurtosis_N" + suffix);
        output.add(kurtosis_N);


/*
        System.out.println(Arrays.toString(colnames.toArray()));
        System.out.println(Arrays.toString(output.toArray()));
*/
        return output;
    }

    public ArrayList<Double> calc_with_RGB_only_sunny(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames,
                                           ArrayList<int[]> RGB){

        ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);

            colnames.add("max_N" + suffix);
            output.add(Double.NaN);
            colnames.add("min_N" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_N" + suffix);
            output.add(Double.NaN);
            colnames.add("median_N" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_N" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_N" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_N" + suffix);
            output.add(Double.NaN);


            return output;
        }
        //concurrentSort(z, z, intensity);

        //Collections.sort(z);
        //Collections.sort(intensity);


        ArrayList<Double> z_above_threshold = new ArrayList<>();
        ArrayList<Double> R_above_threshold = new ArrayList<>();
        ArrayList<Double> G_above_threshold = new ArrayList<>();
        ArrayList<Double> B_above_threshold = new ArrayList<>();
        ArrayList<Double> N_above_threshold = new ArrayList<>();
        ArrayList<Integer> i_above_threshold = new ArrayList<>();

        TreeSet<Integer> intensity_median_set = new TreeSet<>();

        double percentile_step_z = percentile_step_orig;
        double percentile_step_i = percentile_step_orig;

        double[] densitiesOutput = new double[densities.length];



        double z_val;
        double R_val;
        double G_val;
        double B_val;
        double N_val;

        int i_val;

        double z_sum_run = 0.0d, i_sum_run = 0.0d;
        double R_sum_run = 0.0d;
        double G_sum_run = 0.0d;
        double B_sum_run = 0.0d;
        double N_sum_run = 0.0d;
/*
        System.out.println(percentile_step_i);
        System.out.println((int)(1.0 / percentile_step_z));
*/

        double sd_z = 0;
        double sd_R = 0;
        double sd_G = 0;
        double sd_B = 0;
        double sd_N = 0;

        double sd_i = 0;

        double mean_z = sum_z / (double)z.size();
        double mean_R = 0;
        double mean_G = 0;
        double mean_B = 0;
        double mean_N = 0;

        double sum_R = 0;
        double sum_G = 0;
        double sum_B = 0;
        double sum_N = 0;

        double mean_i = sum_i / (double)intensity.size();

        sum_z = 0;
        sum_i = 0;

        double n_above_t_hold = 0;

        double max_z = Double.NEGATIVE_INFINITY;

        double max_R = Double.NEGATIVE_INFINITY;
        double max_G = Double.NEGATIVE_INFINITY;
        double max_B = Double.NEGATIVE_INFINITY;
        double max_N = Double.NEGATIVE_INFINITY;

        int max_i = Integer.MIN_VALUE;

        double min_z = Double.POSITIVE_INFINITY;
        double min_R = Double.POSITIVE_INFINITY;
        double min_G = Double.POSITIVE_INFINITY;
        double min_B = Double.POSITIVE_INFINITY;
        double min_N = Double.POSITIVE_INFINITY;

        int min_i = Integer.MAX_VALUE;

        System.out.println("RGB SIZE: " + RGB.size());

        for(int i = 0; i < RGB.size(); i++){

            R_val = RGB.get(i)[0];
            G_val = RGB.get(i)[1];
            B_val = RGB.get(i)[2];
            N_val = RGB.get(i)[3];

            sum_R += R_val;
            sum_G += G_val;
            sum_B += B_val;
            sum_N += N_val;

            R_above_threshold.add(R_val);
            G_above_threshold.add(G_val);
            B_above_threshold.add(B_val);
            N_above_threshold.add(N_val);


            if(R_val > max_R)
                max_R = R_val;
            if(R_val < min_R)
                min_R = R_val;

            if(G_val > max_G)
                max_G = G_val;
            if(G_val < min_G)
                min_G = G_val;

            if(B_val > max_B)
                max_B = B_val;
            if(B_val < min_B)
                min_B = B_val;

            if(N_val > max_N)
                max_N = N_val;
            if(N_val < min_N)
                min_N = N_val;


        }

        for(int i = 0; i < z.size(); i++){

            z_val = z.get(i);
            i_val = intensity.get(i);

            for(int j = densities.length-1 ; j >= 0; j--){

                if(z_val <= densities[j]){
                    densitiesOutput[j]++;

                }

            }

            if(z_val < cutoff) {
                continue;
            }

            sum_z += z_val;
            sum_i += i_val;



            z_above_threshold.add(z_val);

            i_above_threshold.add(i_val);

            n_above_t_hold++;

            if(z_val > max_z)
                max_z = z_val;
            if(z_val < min_z)
                min_z = z_val;



            if(i_val > max_i)
                max_i = i_val;
            if(i_val < min_i)
                min_i = i_val;

        }

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z_above_threshold.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);

            colnames.add("max_N" + suffix);
            output.add(Double.NaN);
            colnames.add("min_N" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_N" + suffix);
            output.add(Double.NaN);
            colnames.add("median_N" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_N" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_N" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_N" + suffix);
            output.add(Double.NaN);



            return output;
        }

        mean_z = sum_z / (double)z_above_threshold.size();

        mean_R = sum_R / (double)R_above_threshold.size();
        mean_G = sum_G / (double)G_above_threshold.size();
        mean_B = sum_B / (double)B_above_threshold.size();
        mean_N = sum_N / (double)N_above_threshold.size();

        mean_i = sum_i / (double)i_above_threshold.size();


        double median_z = -1, median_i = -1;
        double median_R = -1;
        double median_G = -1;
        double median_B = -1;
        double median_N = -1;

        double z_skewness_v = 0;
        double R_skewness_v = 0;
        double G_skewness_v = 0;
        double B_skewness_v = 0;
        double N_skewness_v = 0;
        double i_skewness_v = 0;

        double z_kurtosis_v = 0;
        double R_kurtosis_v = 0;
        double G_kurtosis_v = 0;
        double B_kurtosis_v = 0;
        double N_kurtosis_v = 0;
        double z_kurtosis_v_2 = 0;
        double R_kurtosis_v_2 = 0;
        double G_kurtosis_v_2 = 0;
        double B_kurtosis_v_2 = 0;
        double N_kurtosis_v_2 = 0;

        double i_kurtosis_v = 0;
        double i_kurtosis_v_2 = 0;

        int densities_counter = 0;


        double sum_z_above = 0.0;
        double sum_i_above = 0.0;

        for(int i = 0; i < R_above_threshold.size(); i++){

            R_val = R_above_threshold.get(i);
            G_val = G_above_threshold.get(i);
            B_val = B_above_threshold.get(i);
            N_val = N_above_threshold.get(i);

            R_sum_run += R_val;
            G_sum_run += G_val;
            B_sum_run += B_val;
            N_sum_run += N_val;

            sd_R += ((R_val - mean_R) * (R_val - mean_R));
            sd_G += ((G_val - mean_G) * (G_val - mean_G));
            sd_B += ((B_val - mean_B) * (B_val - mean_B));
            sd_N += ((N_val - mean_N) * (N_val - mean_N));

            R_skewness_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) );
            G_skewness_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) );
            B_skewness_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) );
            N_skewness_v += ((N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N) );

            R_kurtosis_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B));
            N_kurtosis_v += ((N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N) * (N_val - mean_N));

            R_kurtosis_v_2 += ((R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v_2 += ((G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v_2 += ((B_val - mean_B) * (B_val - mean_B));
            N_kurtosis_v_2 += ((N_val - mean_N) * (N_val - mean_N));

        }

        for(int i = 0; i < z_above_threshold.size(); i++){

            z_val = z_above_threshold.get(i);


            i_val = i_above_threshold.get(i);

            z_sum_run += z_val;


            i_sum_run += i_val;

            sd_z += ((z_val - mean_z) * (z_val - mean_z));


            sd_i += ((i_val - mean_i) * (i_val - mean_i));

            z_skewness_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) );


            i_skewness_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) );

            z_kurtosis_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z));


            z_kurtosis_v_2 += ((z_val - mean_z) * (z_val - mean_z));


            i_kurtosis_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) *  (i_val - mean_i));
            i_kurtosis_v_2 += ((i_val - mean_i) * (i_val - mean_i));

            if(z_val < 0)
                z_val = 0;
            if(i_val < 0)
                i_val = 0;

        }

        sd_z = sd_z / (n_above_t_hold-1);
        sd_R = sd_R / (R_above_threshold.size()-1);
        sd_G = sd_G / (R_above_threshold.size()-1);
        sd_B = sd_B / (R_above_threshold.size()-1);
        sd_N = sd_N / (R_above_threshold.size()-1);

        sd_i = sd_i / (n_above_t_hold-1);


        int n_above_t_hold_spectral = R_above_threshold.size();

        sd_z = Math.sqrt(sd_z);
        sd_R = Math.sqrt(sd_R);
        sd_G = Math.sqrt(sd_G);
        sd_B = Math.sqrt(sd_B);
        sd_N = Math.sqrt(sd_N);

        sd_i = Math.sqrt(sd_i);

        double skewness_z = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * z_skewness_v / (sd_z*sd_z*sd_z);
        double skewness_R = (n_above_t_hold_spectral / ((n_above_t_hold_spectral -1.0) * (n_above_t_hold_spectral - 2.0)) ) * R_skewness_v / (sd_R*sd_R*sd_R);
        double skewness_G = (n_above_t_hold_spectral / ((n_above_t_hold_spectral -1.0) * (n_above_t_hold_spectral - 2.0)) ) * G_skewness_v / (sd_G*sd_G*sd_G);
        double skewness_B = (n_above_t_hold_spectral / ((n_above_t_hold_spectral -1.0) * (n_above_t_hold_spectral - 2.0)) ) * B_skewness_v / (sd_B*sd_B*sd_B);
        double skewness_N = (n_above_t_hold_spectral / ((n_above_t_hold_spectral -1.0) * (n_above_t_hold_spectral - 2.0)) ) * N_skewness_v / (sd_N*sd_N*sd_N);

        double skewness_i = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * i_skewness_v / (sd_i*sd_i*sd_i);


/*
        double kurtosis_z = z_kurtosis_v / (double)z.size() / (sd_z*sd_z*sd_z*sd_z) - 3.0;
        double kurtosis_i = i_kurtosis_v / (double)intensity.size() / (sd_i*sd_i*sd_i*sd_i) - 3.0;

 */
        double kurtosis_z = z_kurtosis_v / n_above_t_hold / ( ( z_kurtosis_v_2 / n_above_t_hold ) * ( z_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_R = R_kurtosis_v / n_above_t_hold_spectral / ( ( R_kurtosis_v_2 / n_above_t_hold_spectral ) * ( R_kurtosis_v_2 / n_above_t_hold_spectral )) ;
        double kurtosis_G = G_kurtosis_v / n_above_t_hold_spectral / ( ( G_kurtosis_v_2 / n_above_t_hold_spectral ) * ( G_kurtosis_v_2 / n_above_t_hold_spectral )) ;
        double kurtosis_B = B_kurtosis_v / n_above_t_hold_spectral / ( ( B_kurtosis_v_2 / n_above_t_hold_spectral ) * ( B_kurtosis_v_2 / n_above_t_hold_spectral )) ;
        double kurtosis_N = N_kurtosis_v / n_above_t_hold_spectral / ( ( N_kurtosis_v_2 / n_above_t_hold_spectral ) * ( N_kurtosis_v_2 / n_above_t_hold_spectral )) ;

        double kurtosis_i = i_kurtosis_v / n_above_t_hold / ( ( i_kurtosis_v_2 / n_above_t_hold ) * ( i_kurtosis_v_2 / n_above_t_hold ));


        Collections.sort(i_above_threshold);
        Collections.sort(z_above_threshold);
        Collections.sort(R_above_threshold);
        Collections.sort(G_above_threshold);
        Collections.sort(B_above_threshold);
        Collections.sort(N_above_threshold);

        try {
            if (n_above_t_hold_spectral % 2 == 0) {
                median_R = ((R_above_threshold.get(R_above_threshold.size() / 2) + R_above_threshold.get(R_above_threshold.size() / 2 - 1)) / 2.0);
                median_G = ((G_above_threshold.get(G_above_threshold.size() / 2) + G_above_threshold.get(G_above_threshold.size() / 2 - 1)) / 2.0);
                median_B = ((B_above_threshold.get(B_above_threshold.size() / 2) + B_above_threshold.get(B_above_threshold.size() / 2 - 1)) / 2.0);
                median_N = ((N_above_threshold.get(N_above_threshold.size() / 2) + N_above_threshold.get(N_above_threshold.size() / 2 - 1)) / 2.0);
            } else {
                median_R = R_above_threshold.get(R_above_threshold.size() / 2);
                median_G = G_above_threshold.get(G_above_threshold.size() / 2);
                median_B = B_above_threshold.get(B_above_threshold.size() / 2);
                median_N = N_above_threshold.get(N_above_threshold.size() / 2);

            }
        }catch (Exception e){
            System.out.println("Plot without any sunny points");
        }


        if (z_above_threshold.size() % 2 == 0) {
            median_z = ( (z_above_threshold.get(z_above_threshold.size() / 2) + z_above_threshold.get(z_above_threshold.size() / 2 - 1)) / 2.0);


            median_i = ( ((double) i_above_threshold.get(i_above_threshold.size() / 2) + (double) i_above_threshold.get(i_above_threshold.size() / 2 - 1)) / 2.0);
        }
        else {
            median_z = z_above_threshold.get(z_above_threshold.size() / 2);

            median_i = (double) i_above_threshold.get(i_above_threshold.size() / 2);
        }


        //skewness_z = 1.0 / (sd_z*sd_z*sd_z) * (z_skewness_v / (double)z.size());

        //percentiles_z[percentiles_z.length-1] = z.get(z.size()-1);
        //percentiles_i[percentiles_i.length-1] = intensity.get(intensity.size()-1);

        for(int j = 0 ; j < densities.length; j++){

            densitiesOutput[j] /= z.size();

        }
/*
        System.out.println(Arrays.toString(percentiles_z) + " " + z.get(z.size()-1));
        System.out.println(Arrays.toString(densitiesOutput));
        System.out.println(sd_z + " " + sd_i + " " + skewness_z + " " + kurtosis_z);
*/
        colnames.clear();

        int counter = 0;

        double[] z_array = ArrayUtils.toPrimitive(z_above_threshold.toArray(new Double[z_above_threshold.size()]));
        double[] i_array = new double[i_above_threshold.size()];

        for(int i = 0; i < i_above_threshold.size(); i++){
            i_array[i] = (double)i_above_threshold.get(i);
        }


        Percentile p_z = new Percentile(this.percentile_step_orig);
        Percentile p_i = new Percentile(this.percentile_step_orig);

        p_z.setData(z_array);
        p_i.setData(i_array);

        int pers_size = (int)Math.ceil((0.95 / percentile_step_z));

        double[] percentiles_z = new double[pers_size];
        double[] percentiles_z_names = new double[pers_size];
        double[] percentiles_i = new double[pers_size];
        double[] percentiles_i_names = new double[pers_size];

        for(int i = 0; i < percentiles_z.length; i++){

            percentiles_z[i] = p_z.evaluate(percentile_step_z * 100.0);
            percentiles_z_names[i] = percentile_step_z;
            percentiles_i[i] = p_i.evaluate(percentile_step_z * 100.0);
            percentiles_i_names[i] = percentile_step_z;
            percentile_step_z += percentile_step_orig;

        }

        //System.out.println("end: ");



        colnames.add("max_z" + suffix);
        output.add(max_z);
        colnames.add("min_z" + suffix);
        output.add(min_z);
        colnames.add("sd_z" + suffix);
        output.add(sd_z);
        colnames.add("median_z" + suffix);
        output.add(median_z);
        colnames.add("mean_z" + suffix);
        output.add(mean_z);
        colnames.add("skewness_z" + suffix);
        output.add(skewness_z);
        colnames.add("kurtosis_z" + suffix);
        output.add(kurtosis_z);
        colnames.add("max_i" + suffix);
        output.add((double)max_i);
        colnames.add("min_i" + suffix);
        output.add((double)min_i);
        colnames.add("sd_i" + suffix);
        output.add(sd_i);
        colnames.add("median_i" + suffix);
        output.add((median_i));
        colnames.add("mean_i" + suffix);
        output.add(mean_i);
        colnames.add("skewness_i" + suffix);
        output.add(skewness_i);
        colnames.add("kurtosis_i" + suffix);
        output.add(kurtosis_i);

        counter = 0;
        for(int i = 0 ; i < percentiles_z.length; i++){

            colnames.add("p_" + (Math.round(percentiles_z_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
            output.add(percentiles_z[counter++]);

        }
        counter = 0;
        for(int i = 0 ; i < percentiles_i.length; i++){

            colnames.add("p_" + (Math.round(percentiles_i_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
            output.add(percentiles_i[counter++]);
        }
        for(int i = 0; i < densities.length; i++){

            colnames.add("d_" + densities[i] + "_z" + suffix);
            output.add(densitiesOutput[i]);

        }

        colnames.add("max_R" + suffix);
        output.add(max_R);
        colnames.add("min_R" + suffix);
        output.add(min_R);
        colnames.add("sd_R" + suffix);
        output.add(sd_R);
        colnames.add("median_R" + suffix);
        output.add(median_R);
        colnames.add("mean_R" + suffix);
        output.add(mean_R);
        colnames.add("skewness_R" + suffix);
        output.add(skewness_R);
        colnames.add("kurtosis_R" + suffix);
        output.add(kurtosis_R);

        colnames.add("max_G" + suffix);
        output.add(max_G);
        colnames.add("min_G" + suffix);
        output.add(min_G);
        colnames.add("sd_G" + suffix);
        output.add(sd_G);
        colnames.add("median_G" + suffix);
        output.add(median_G);
        colnames.add("mean_G" + suffix);
        output.add(mean_G);
        colnames.add("skewness_G" + suffix);
        output.add(skewness_G);
        colnames.add("kurtosis_G" + suffix);
        output.add(kurtosis_G);

        colnames.add("max_B" + suffix);
        output.add(max_B);
        colnames.add("min_B" + suffix);
        output.add(min_B);
        colnames.add("sd_B" + suffix);
        output.add(sd_B);
        colnames.add("median_B" + suffix);
        output.add(median_B);
        colnames.add("mean_B" + suffix);
        output.add(mean_B);
        colnames.add("skewness_B" + suffix);
        output.add(skewness_B);
        colnames.add("kurtosis_B" + suffix);
        output.add(kurtosis_B);

        colnames.add("max_N" + suffix);
        output.add(max_N);
        colnames.add("min_N" + suffix);
        output.add(min_N);
        colnames.add("sd_N" + suffix);
        output.add(sd_N);
        colnames.add("median_N" + suffix);
        output.add(median_N);
        colnames.add("mean_N" + suffix);
        output.add(mean_N);
        colnames.add("skewness_N" + suffix);
        output.add(skewness_N);
        colnames.add("kurtosis_N" + suffix);
        output.add(kurtosis_N);


/*
        System.out.println(Arrays.toString(colnames.toArray()));
        System.out.println(Arrays.toString(output.toArray()));
*/
        return output;
    }

    public static <T extends Comparable<T>> void concurrentSort(
            final List<T> key, List<?>... lists){
        // Create a List of indices
        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < key.size(); i++)
            indices.add(i);

        // Sort the indices list based on the key
        Collections.sort(indices, new Comparator<Integer>(){
            @Override public int compare(Integer i, Integer j) {
                return key.get(i).compareTo(key.get(j));
            }
        });

        // Create a mapping that allows sorting of the List by N swaps.
        // Only swaps can be used since we do not know the type of the lists
        Map<Integer,Integer> swapMap = new HashMap<Integer, Integer>(indices.size());
        List<Integer> swapFrom = new ArrayList<Integer>(indices.size()),
                swapTo   = new ArrayList<Integer>(indices.size());
        for(int i = 0; i < key.size(); i++){
            int k = indices.get(i);
            while(i != k && swapMap.containsKey(k))
                k = swapMap.get(k);

            swapFrom.add(i);
            swapTo.add(k);
            swapMap.put(i, k);
        }

        // use the swap order to sort each list by swapping elements
        for(List<?> list : lists)
            for(int i = 0; i < list.size(); i++)
                Collections.swap(list, swapFrom.get(i), swapTo.get(i));
    }

}
