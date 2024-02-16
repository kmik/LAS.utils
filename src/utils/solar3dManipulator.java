package utils;

import org.apache.commons.math3.util.FastMath;
import quickhull3d.Vector3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

import utils.amapVox;
import err.toolException;

import org.apache.commons.io.FilenameUtils;

public class solar3dManipulator {

    double angleThreshold = 45;

    static double TWOPI = 6.2831853071795865;
    static double RAD2DEG = 57.2957795130823209;
    float[][][] space;
    float[][][] space_closure;
    int x_dim, y_dim, z_dim;
    int[][] maxValueInChm;
    double resolution;
    byte[][][] contains_points;

    public VoxelNeighborhood[][][] vox_;

    public boolean hasAmapVox = false;
    double amapVox_min_corner_x;
    double amapVox_min_corner_y;
    double amapVox_min_corner_z;
    double amapVox_max_corner_x;
    double amapVox_max_corner_y;
    double amapVox_max_corner_z;

    int amapVox_dim_x, amapVox_dim_y, amapVox_dim_z;
    float[][][] amapVox;

    public amapVox aV;
    int counter = 0;

    public solar3dManipulator(int x_dim, int y_dim, int z_dim, float[][][] data, double resolution){

        this.y_dim = y_dim;
        this.x_dim = x_dim;
        this.z_dim = z_dim;

        this.resolution = resolution;
        space_closure = new float[y_dim][x_dim][z_dim];

        this.space = data;

    }

    public void setContains_points(byte[][][] contains_points){
        this.contains_points = contains_points;
    }

    public void setVox_(VoxelNeighborhood[][][] in){

        this.vox_ = in;

    }
    public solar3dManipulator(int x_dim, int y_dim, int z_dim, float[][][] data, double resolution, int[][] maxValueInChm, double angle){

        this.y_dim = y_dim;
        this.x_dim = x_dim;
        this.z_dim = z_dim;
        this.maxValueInChm = maxValueInChm;
        this.angleThreshold = angle;
        this.resolution = resolution;
        space_closure = new float[y_dim][x_dim][z_dim];

        this.space = data;

    }

    public void setAmapVox(amapVox in){

        this.hasAmapVox = true;
        this.aV = in;

    }

    public void read_amapvox_1_6(File amapVoxFile){

        if(!FilenameUtils.getExtension(amapVoxFile.getAbsoluteFile().getName()).equals("vox")){
            System.out.println("Input is not a .vox file");
            System.exit(1);
        }

        BufferedReader in = null;

        String line = null;
        String[] tokens = null;

        this.aV = new amapVox(amapVoxFile);

        this.aV.readHeader();

        this.aV.readVoxels();

        System.out.println(this.x_dim + " ==? " + aV.x_dim);
        System.out.println(this.y_dim + " ==? " + aV.y_dim);
        System.out.println(this.z_dim + " ==? " + aV.z_dim);

        //System.exit(1);
        this.hasAmapVox = true;

        if(false)
        try {
            in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(amapVoxFile.getAbsolutePath()), StandardCharsets.UTF_8));


            in.close();



        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }


    }


    public void setValue(int x, int y, int z, float value){

        this.space[y][x][z] = value;

    }

    public void setValue_closure(int x, int y, int z, float value){

        this.space_closure[y][x][z] = value;

    }

    public float getValue(int x, int y, int z){

        return space[y][x][z];

    }

    public void get_sky_prop(){

        double prop = 0.0;

        IntStream.range(0, z_dim-1).parallel().forEach(z -> {
            //for(int z = 1; z < z_dim-1; z++){
            for(int x = 1; x < x_dim-1; x++){
                for(int y = 1; y < y_dim-1; y++) {

                    if(contains_points[y][x][z] > 0){

                        int length = (int)(FastMath.tan(FastMath.toRadians(angleThreshold)) * ((z_dim-1) - z));
                        //System.out.println("length: " + (length * resolution) + "m " + (((z_dim-1) - z)*resolution));
                        int count = 0;

                        int min_x = (int)Math.max(0, x - length);
                        int min_y = (int)Math.max(0, y - length);

                        int max_x = (int)Math.min(x_dim-1, x + length);
                        int max_y = (int)Math.min(y_dim-1, y + length);
/*
                        int max_z = Integer.MAX_VALUE;


                        for(int x_ = min_x; x_ < max_x; x_++) {
                            for (int y_ = min_y; y_ < max_y; y_++) {

                                if(maxValueInChm[y_][x_] > max_z)
                                    max_z = maxValueInChm[y_][x_];

                            }
                        }

 */
                        for(int x_ = min_x; x_ < max_x; x_++) {
                            for (int y_ = min_y; y_ < max_y; y_++) {


                                boolean blocked = rayTrace(x, y, z, x_, y_, z_dim-1);
                                //boolean blocked = rayTrace(x, y, z, x_, y_, max_z);

                                if(!blocked)
                                    count++;
                            }
                        }

                        //System.out.println((double)((double)count / (double)(x_dim * y_dim)));
                        //System.out.println("n: " + ((max_x - min_x) * (max_y - min_y)));
                        //System.out.println((count));

                        this.setValue_closure(x, y, z, (float)count / (float)((max_x - min_x) * (max_y - min_y)) * 65535.0f);
                        //space[y][x][z] = (float)();
                    }

                }
            }
        });

        //this.space = space_closure;
        //return prop;

    }




    public boolean rayTrace(int from_x, int from_y, int from_z, int to_x, int to_y, int to_z){

        boolean debug = false;

        if(debug){

            from_x = 15;
            from_y = 15;
            from_z = 5;

            to_x = 30;
            to_y = 30;
            to_z = 10;
        }


        int penetration = 0;

        int minPenetration = 2;


        Vector3d line_quick = new Vector3d(0,0,0);
        Vector3d point_quick = new Vector3d(0,0,0);

        float center_of_pixel_x = (float)from_x + 0.5f;
        float center_of_pixel_y = (float)from_y + 0.5f;
        float center_of_pixel_z = (float)from_z + 0.5f;

        int current_x = from_x;
        int current_y = from_y;
        int current_z = from_z;

        float outside_x = to_x, outside_y = to_y, outside_z = to_z;


        float xy_dist = euclideanDistance(from_x, from_y, to_x, to_y);
        float z_dist = Math.abs(from_z - to_z);
        float xyz_dist =  euclideanDistance3d(from_x, from_y, from_z, to_x, to_y, to_z);

        float zenith_angle = 90.0f - (float)Math.toDegrees(FastMath.atan2(z_dist, xyz_dist));

        float cosAngle2 = cos((FastMath.abs((float)zenith_angle)));
        float sinAngle2 = sin((FastMath.abs((float)zenith_angle)));

        float cosAngle, sinAngle;

        int step_x = -2, step_y = -2;
        float n_points_in_voxel = 0;

        float direction_angle = (float)bearing(center_of_pixel_x, center_of_pixel_y, to_x, to_y);
        int step_z = 1;

        if(debug){

            System.out.println("dir: " + direction_angle);
            System.out.println("zen: " + zenith_angle);
            System.exit(1);

        }
        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            //outside_y = -center_of_pixel_y + sinAngle * 10000;
            //outside_x = center_of_pixel_x + cosAngle * 10000;

            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(center_of_pixel_y), center_of_pixel_x + 10000, (int)(center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), (float) center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z+1), from_x + 10000,  (int)(center_of_pixel_z+1));
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z), from_x + 10000,  (int)(center_of_pixel_z));

            if(y_intersect == null || x_intersect == null || z_intersect == null)
                return true;

            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;
            int counter = 0;
            /* RAY TRACE ITERATIONS */


            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }
                counter++;


                if(current_z >= this.z_dim || current_x >= this.x_dim || current_y >= this.y_dim
                        || current_x < 0 || current_y < 0){
                    return false;
                }



                if(current_z > from_z+1)
                    n_points_in_voxel = contains_points[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

/*
                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);


                    if(distance < distance_threshold){
                        // System.out.println("BLOCKED!");
                        return true;
                    }
 */
                    return true;
                }
            }

        }

        else if(direction_angle < 180.0f){

            // System.out.println("HERE!!");
            step_x = 1;
            step_y = 1;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


            // outside_y = -center_of_pixel_y - cosAngle * 10000;
            //outside_x = center_of_pixel_x + sinAngle * 10000;

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(center_of_pixel_y+1), center_of_pixel_x + 10000, (int)(center_of_pixel_y+1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));

            if(y_intersect == null || x_intersect == null || z_intersect == null)
                return true;

            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                //System.out.println(this.rasterMaxValue + " " + (this.p_cloud_min_z + current_z * resolution));


                if(current_z >= this.z_dim || current_x >= this.x_dim || current_y >= this.y_dim
                        || current_x < 0 || current_y < 0){
                    return false;
                }



                if(current_z > from_z+1)
                    n_points_in_voxel = contains_points[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

/*
                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);


                    if(distance < distance_threshold){
                        // System.out.println("BLOCKED!");
                        return true;
                    }
 */
                    return true;
                }

            }



        }

        else if(direction_angle < 270.0f){

            step_x = -1;
            step_y = 1;

            //cosAngle = (float)Math.cos(Math.toRadians(270.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(270.0 - direction_angle));

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

            //outside_y = -center_of_pixel_y - sinAngle * 10000;
            //outside_x = center_of_pixel_x - cosAngle * 10000;

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);

            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 10000, (int)(center_of_pixel_y+1), from_x + 10000, (int)(center_of_pixel_y+1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x-1), center_of_pixel_y + 10000, (int)(center_of_pixel_x-1), center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            if(y_intersect == null || x_intersect == null || z_intersect == null)
                return true;

            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if(current_z >= this.z_dim || current_x >= this.x_dim || current_y >= this.y_dim
                        || current_x < 0 || current_y < 0){
                    return false;
                }



                if(current_z > from_z+1)
                    n_points_in_voxel = contains_points[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){
/*
                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);


                    if(distance < distance_threshold){
                        // System.out.println("BLOCKED!");
                        return true;
                    }
 */
                    return true;
                }

            }



        }

        else if(direction_angle < 360.0f){

            step_x = -1;
            step_y = -1;

            //cosAngle = (float)Math.cos(Math.toRadians(360.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(360.0 - direction_angle));

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

            //outside_y = -center_of_pixel_y + cosAngle * 10000;
            //outside_x = center_of_pixel_x - sinAngle * 10000;


            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            // Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, from_x - 10000, (int)(center_of_pixel_y), from_x + 10000, (int)(center_of_pixel_y));
            //float[] y_intersect_z = lineIntersect(-center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, z - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x-1), center_of_pixel_y + 10000, (int)(center_of_pixel_x-1), center_of_pixel_y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000,  (int)(center_of_pixel_z+1), from_x + 10000, (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, from_x - 10000, (int)(center_of_pixel_z), from_x + 10000,  (int)(center_of_pixel_z));


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            if(y_intersect == null || x_intersect == null || z_intersect == null)
                return true;

            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }


                if(current_z >= this.z_dim || current_x >= this.x_dim || current_y >= this.y_dim
                        || current_x < 0 || current_y < 0){
                    return false;
                }



                if(current_z > from_z+1)
                    n_points_in_voxel = contains_points[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){
/*
                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);


                    if(distance < distance_threshold){
                        // System.out.println("BLOCKED!");
                        return true;
                    }
 */
                    return true;
                }

            }
        }

        return false;

    }

    public float[] IDW(double x, double y, double z){

        int x_ = (int)x; int y_ = (int)y; int z_ = (int)z;

        //if(space[y_][x_][z_] <= 0){
        if(vox_[x_][y_][z_] == null){
            return new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        }



        int min_x = Math.max(0, x_ - 1); int min_y = Math.max(0, y_ - 1); int min_z = Math.max(0, z_ - 1);

        int max_x = Math.min(this.x_dim-1, x_ + 1); int max_y = Math.min(this.y_dim-1, y_ + 1);
        int max_z = Math.min(this.z_dim-1, z_ + 1);

        float denominator = 0.0f, numerator = 0.0f, numerator_2 = 0.0f, numerator_3 = 0.0f, numerator_4 = 0.0f;

        for(int x__ = min_x; x__ <= max_x; x__++){
            for(int y__ = min_y; y__ <= max_y; y__++) {
                for (int z__ = min_z; z__ <= max_z; z__++) {

                    // Can't include normal and flatness here because space[y__][x__][z__] > 0
                    // means voxels that are illuminated, not voxels that have points.
                    //if( space[y__][x__][z__] > 0){
                    if( vox_[x__][y__][z__] != null )
                    if( !vox_[x__][y__][z__].garbage ){

                        double distance = euclideanDistance3d(x, y, z, x__ + 0.5, y__ + 0.5, z__ + 0.5);
                        denominator += 1.0 / distance;
                        numerator += space[y__][x__][z__] / distance;
                        numerator_2 += space_closure[y__][x__][z__] / distance;
                        numerator_3 += vox_[x__][y__][z__].normal / distance;
                        numerator_4 += vox_[x__][y__][z__].flatness / distance;

                    }
                }
            }
        }
        return  new float[]{numerator / denominator, numerator_2 / denominator, numerator_3 / denominator, numerator_4 / denominator};
    }



    static final int precision = 100; // gradations per degree, adjust to suit

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; // lookup table
    static final float[] cos = new float[modulus]; // lookup table
    static {
        // a static initializer fills the table
        // in this implementation, units are in degrees
        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }

        for (int i = 0; i<cos.length; i++) {
            cos[i]=(float)Math.cos((i*Math.PI)/(precision*180));
        }

    }
    // Private function for table lookup
    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }
    // Private function for table lookup
    private static float cosLookup(int a) {
        return a>=0 ? cos[a%(modulus)] : -cos[-a%(modulus)];
    }

    // These are your working functions:
    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }


    public float euclideanDistance(float x1, float y1, float x2, float y2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) );

    }

    public float euclideanDistance3d(float x1, float y1, float z1, float x2, float y2, float z2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2) );

    }

    public double euclideanDistance3d(double x1, double y1, double z1, double x2, double y2, double z2){


        return FastMath.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2) );

    }

    public double bearing(double a1, double a2, double b1, double b2) {

        // if (a1 = b1 and a2 = b2) throw an error
        double theta = FastMath.atan2(b1 - a1, a2 - b2);

        if (theta < 0.0)
            theta += TWOPI;

        return RAD2DEG * theta;

    }

    public static float[] lineIntersect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new float[]{(float) (x1 + ua*(x2 - x1)),  (float) (y1 + ua*(y2 - y1))};
        }

        return null;
    }

    public static float[] lineIntersect_debug(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }


        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;

        System.out.println("DEBUG!!: ua: " + ua + " ub: " + ub);

        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new float[]{(float) (x1 + ua*(x2 - x1)),  (float) (y1 + ua*(y2 - y1))};
        }

        return null;
    }

}