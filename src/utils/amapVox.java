package utils;

import LASio.LasPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class amapVox {

    File file;

    public int x_dim, y_dim, z_dim;

    int n_subvoxel, n_record_max, fraction_digits;

    String lad_type, type, build_version;
    public double min_x, min_y, min_z, max_x, max_y, max_z;

    // #res:0.25 #nsubvoxel:8 #nrecordmax:0 #fraction-digits:7 #lad_type:Spherical #type:ALS #max_pad:5.0 #build-version:1.6.4
    double resolution, max_pad;

    ArrayList<String> column_names;

    BufferedReader reader = null;

    int transmittance_column_number;

    public float[][][] data;
    public amapVox(File file){

        this.file = file;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file.getAbsolutePath()), StandardCharsets.UTF_8));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void readHeader(){
        String line;
        String[] tokens;
        String[] tokens_2;
        // VOXEL SPACE

        try {
            line = reader.readLine();

            // min_corner
            line = reader.readLine();

            tokens = line.split(": ");
            tokens = tokens[1].split(" ");

            this.min_x = Double.parseDouble(tokens[0]);
            this.min_y = Double.parseDouble(tokens[1]);
            this.min_z = Double.parseDouble(tokens[2]);

            // max_corner
            line = reader.readLine();

            tokens = line.split(": ");
            tokens = tokens[1].split(" ");

            this.max_x = Double.parseDouble(tokens[0]);
            this.max_y = Double.parseDouble(tokens[1]);
            this.max_z = Double.parseDouble(tokens[2]);

            // dim
            line = reader.readLine();

            tokens = line.split(": ");
            tokens = tokens[1].split(" ");

            this.x_dim = Integer.parseInt(tokens[0]);
            this.y_dim = Integer.parseInt(tokens[1]);
            this.z_dim = Integer.parseInt(tokens[2]);

            this.data = new float[x_dim][y_dim][z_dim];

            for(int x = 0; x < x_dim; x++)
                for(int y = 0; y < y_dim; y++)
                    for(int z = 0; z < x_dim; z++)
                        data[x][y][z] = 1.0f;
            // #res:0.25 #nsubvoxel:8 #nrecordmax:0 #fraction-digits:7 #lad_type:Spherical #type:ALS #max_pad:5.0 #build-version:1.6.4

            line = reader.readLine();

            // example.split("[;:-]");
            tokens = line.split("[: #]");
            //tokens_2 = line.split(":");

            //System.out.println(Arrays.toString(tokens));
            //System.out.println(tokens[2]);
            this.resolution = Double.parseDouble(tokens[2]);
            //System.out.println(tokens[5]);
            this.n_subvoxel = Integer.parseInt(tokens[5]);
            //System.out.println(tokens[8]);
            this.n_record_max = Integer.parseInt(tokens[8]);
            //System.out.println(tokens[11]);
            this.fraction_digits = Integer.parseInt(tokens[11]);
            //System.out.println(tokens[14]);
            this.lad_type = (tokens[14]);

            //System.out.println(tokens[17]);
            this.type = (tokens[17]);
            //System.out.println(tokens[20]);
            this.max_pad = Double.parseDouble(tokens[20]);
            //System.out.println(tokens[23]);
            this.build_version = (tokens[23]);

            line = reader.readLine();

            System.out.println(line);

            tokens = line.split(" ");
            column_names = new ArrayList<>();

            for(int i = 0; i < tokens.length; i++){

                column_names.add(tokens[i]);
                if(tokens[i].equals("transmittance"))
                    transmittance_column_number = i;
            }



        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void readVoxels(){

        double transmittance;
        String line;
        int x, y, z;
        String[] tokens;


        try {
            while ((line = reader.readLine()) != null) {
                tokens = line.split(" ");

                x = Integer.parseInt(tokens[0]);
                y = Integer.parseInt(tokens[1]);
                z = Integer.parseInt(tokens[2]);

                if(!tokens[transmittance_column_number].equals("NaN")) {

                    transmittance = Double.parseDouble(tokens[transmittance_column_number]);
                    data[x][y][z] = (float) transmittance;

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        //System.exit(1);
    }

    public boolean checkPoint(LasPoint tempPoint){

        if(tempPoint.x < this.min_x || tempPoint.x > this.max_x
            || tempPoint.y > this.max_y || tempPoint.y < this.min_y
                || tempPoint.z > this.max_z || tempPoint.y < this.min_z )
            return false;

        return true;

    }
}
