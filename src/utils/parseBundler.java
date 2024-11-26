package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class parseBundler {

    File bundlerFile;

    int num_cameras;
    int num_points;

    argumentReader aR;

    public parseBundler(argumentReader aR){

        this.aR = aR;

    }

    public void setBundlerFile(File bundlerFile){
        this.bundlerFile = bundlerFile;
    }

    public void parseBundlerFile(){

        File outputFile = new File(aR.output);

        if(!outputFile.exists()){
            try{
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            outputFile.delete();
            try{
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // declare reader

        BufferedReader reader = null;
        BufferedWriter writer = null;

        TreeMap<Integer, Camera> cameras = new TreeMap<Integer, Camera>();

        try {
            reader = new BufferedReader(new FileReader(bundlerFile));
            writer = new BufferedWriter(new FileWriter(outputFile));
            String line = reader.readLine();

            point p = new point();

            while (line != null) {
                // process the line
                line = reader.readLine();

                // Split by space
                String[] parts = line.split(" ");
                num_cameras = Integer.parseInt(parts[0]);
                num_points = Integer.parseInt(parts[1]);

                System.out.println("num_cameras: " + num_cameras);
                System.out.println("num_points: " + num_points);
                int camCount = 0;

                for(int i = 0; i < num_cameras; i++){

                    Camera c = new Camera();
                    c.setFocal_length(17347.8261);
                    c.setPrincipal_point_x(14790 / 2.0 + 29.5);
                    //c.setPrincipal_point_x(14790 / 2.0 + (0.0467 * 0.00158177));
                    c.setPrincipal_point_y(23010 / 2.0 + (-0.5));
                    //c.setPrincipal_point_y(23010 / 2.0 + (0.0008 * 0.00158177));
                    c.setKey(i);
                    cameras.put(i, c);
                    // Five lines per camera, but we don't need these for now
                    for(int j = 0; j < 5; j++)
                        line = reader.readLine();

                }

                for(int i = 0; i < num_points; i++){

                    // First is the 3d position of the point (we can ignore)
                    line = reader.readLine();

                    // Second is the color of the point
                    line = reader.readLine();

                    String[] parts2 = line.split(" ");
                    p.setR(Short.parseShort(parts2[0]));
                    p.setG(Short.parseShort(parts2[1]));
                    p.setB(Short.parseShort(parts2[2]));

                    // Third is the list of views the point is visible in
                    // Split by space
                    line = reader.readLine();
                    parts = line.split(" ");

                    int numViews = Integer.parseInt(parts[0]);

                    for(int j = 0; j < numViews; j++){

                        int cameraKey = Integer.parseInt(parts[j * 4 + 1]);
                        int key = Integer.parseInt(parts[j * 4 + 2]);

                        View view = new View();
                        view.setKeynumber(key);
                        view.setX(Math.abs(Double.parseDouble(parts[j * 4 + 3])));
                        view.setY(Math.abs(Double.parseDouble(parts[j * 4 + 4])));
                        view.setR(p.getR());
                        view.setG(p.getG());
                        view.setB(p.getB());

                        cameras.get(cameraKey).addView(view);

                        //p.setKey(key);
                        if(cameras.containsKey(cameraKey)){
                            cameras.get(cameraKey).addView(view);
                        }else{
                            System.exit(1);
                            Camera c = new Camera();
                            c.addKey(key);
                            cameras.put(cameraKey, c);
                        }

                    }

                    // Write to file
                    //writer.write(p.getX() + " " + p.getY() + " " + p.getZ() + "\n");

                }

                int maxViewKey = 0;

                for(int i : cameras.keySet()){
                    Camera c = cameras.get(i);

                    System.out.println("camera has " + c.numKeys + " keys");

                    for(int j = 0; j < c.numKeys; j++){

                        if(c.views.get(j).getKeynumber() > maxViewKey)
                            maxViewKey = c.views.get(j).getKeynumber();
                        //System.out.println(c.getKey() + " " + c.views.get(j).toString());
                    }

                }

                String separator = " ";
                String separator2 = " ";
                for(int i : cameras.keySet()){
                    Camera c = cameras.get(i);
                    writer.write("random" + separator2 + "random" + separator2 + c.getKey() + separator +
                            "random" + separator2 + "random" + separator2 + "random" + separator2 + "random" + separator2 + "random" + separator2 +
                            c.numKeys + separator2 +
                            "random" + separator2 + "random" + separator2 + "random" + separator2 +
                            c.getPrincipal_point_x() + separator2 +
                            "random" + separator2 + "random" + separator2 +
                            c.getPrincipal_point_y() + separator2 +
                            "random" + separator2 + "random" + separator2 +
                            c.getFocal_length() + separator2 +"\n");

                    for(int j = 0; j < c.numKeys; j++){
                        View v = c.views.get(j);
                        writer.write(v.getKeynumber() + separator + v.getX() + separator + v.getY() + separator + 0 + separator + 0 + separator + v.getR() + separator + v.getG() + separator + v.getB() + "\n");
                    }
                }
                break;
            }

            reader.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public class point{



        double x;
        double y;
        double z;

        double image_x, image_y;
        short R, G, B;

        int key;

        public point(){

        }


        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public short getR() {
            return R;
        }

        public void setR(short r) {
            R = r;
        }

        public short getG() {
            return G;
        }

        public void setG(short g) {
            G = g;
        }

        public short getB() {
            return B;
        }

        public void setB(short b) {
            B = b;
        }

        public int getKey() {
            return key;
        }

        public void setKey(int key) {
            this.key = key;
        }

        public double getImage_x() {
            return image_x;
        }

        public void setImage_x(double image_x) {
            this.image_x = image_x;
        }

        public double getImage_y() {
            return image_y;
        }

        public void setImage_y(double image_y) {
            this.image_y = image_y;
        }
    }

    public class View{

        int keynumber;
        double x, y;
        short R, G, B;

        public View(){

        }

        public int getKeynumber() {
            return keynumber;
        }

        public void setKeynumber(int keynumber) {
            this.keynumber = keynumber;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public short getR() {
            return R;
        }

        public void setR(short r) {
            R = r;
        }

        public short getG() {
            return G;
        }

        public void setG(short g) {
            G = g;
        }

        public short getB() {
            return B;
        }

        public void setB(short b) {
            B = b;
        }
        @Override
        public String toString(){
            return("key: " + keynumber + " x: " + x + " y: " + y + " R: " + R + " G: " + G + " B: " + B);
        }
    }

    public class Camera{


        ArrayList<View> views = new ArrayList<View>();

        HashSet<Integer> keys = new HashSet<Integer>();
        int key;
        double focal_length;
        double principal_point_x;
        double principal_point_y;

        int numKeys = 0;


        public Camera(){

        }

        public int getKey() {
            return key;
        }

        public void setKey(int key) {
            this.key = key;
        }

        public double getFocal_length() {
            return focal_length;
        }

        public void setFocal_length(double focal_length) {
            this.focal_length = focal_length;
        }

        public double getPrincipal_point_x() {
            return principal_point_x;
        }

        public void setPrincipal_point_x(double principal_point_x) {
            this.principal_point_x = principal_point_x;
        }

        public double getPrincipal_point_y() {
            return principal_point_y;
        }

        public void setPrincipal_point_y(double principal_point_y) {
            this.principal_point_y = principal_point_y;
        }

        public void addKey(int key){
            numKeys++;
        }

        public void addView(View v){

            if(!keys.contains(v.getKeynumber())) {
                keys.add(v.getKeynumber());
                views.add(v);
                this.addKey(v.getKeynumber());
            }
        }
    }

}
