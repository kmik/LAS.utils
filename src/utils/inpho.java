package utils;

import org.datavec.api.split.StringSplit;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class inpho {

    File inphoFile = null;

    int numPhotos = 0;
    int numCameras = 0;

    ArrayList<String> cameraIds = new ArrayList<>();
    ArrayList<Camera> cameras = new ArrayList<>();

    argumentReader aR;


    public inpho(){

    }

    public inpho(File f_){

        this.inphoFile = f_;

    }

    public inpho(argumentReader aR){
        this.aR = aR;
    }

    public void setInphoFile(File f_){

        this.inphoFile = f_;

    }

    public void parseInphoFile(){

        // Read inpho file line by line
        // Parse the file and store the data in the class variables
        try {
            BufferedReader sc = new BufferedReader(new FileReader(inphoFile));

            while(sc.ready()){
                String line = sc.readLine();

                if(line.equals("$PHOTO")){
                    numPhotos++;
                    photo phot = new photo();
                    String photoName = sc.readLine();
                    phot.setName(photoName.split(": ")[1]);
                    String photoPath = sc.readLine();
                    phot.setPath(photoPath.split(": ")[1]);
                    String photoCamera = sc.readLine();
                    phot.setCameraId(photoCamera.split(": ")[1]);

                    //System.out.println(phot);

                }

                if(line.equals("$CAMERA_DEFINITION")){
                    numCameras++;

                    Camera cam = new Camera();


                    String cameraId = sc.readLine();

                    cam.setCameraId(cameraId.split(": ")[1]);
                    cameraIds.add(cam.getCameraId());
                    cameras.add(cam);

                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }

        this.splitFileToSingleCameras();


    }

    public void splitFileToSingleCameras(){

        String outputDirectory = aR.odir;

        if(aR.odir.equals("asd")){
            throw new IllegalArgumentException("Output directory not set, this tool requires an output directory. Please define by setting -odir argument.");
        }

        for(int i = 0; i < this.numCameras; i++) {

            String cameraName = cameras.get(i).getCameraId();

            String outputFileName = outputDirectory + aR.pathSep + cameraName;

            File outputFile = new File(outputFileName);

            if(outputFile.exists()){
                outputFile.delete();
            }



            // Read inpho file line by line
            // Parse the file and store the data in the class variables
            try {
                outputFile.createNewFile();

                BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(outputFile)));

                BufferedReader sc = new BufferedReader(new FileReader(inphoFile));

                while (sc.ready()) {
                    String line = sc.readLine();

                    boolean writing = false;

                    //if(false)
                    if (line.equals("$CAMERA_DEFINITION")) {

                        String cameraId = sc.readLine();

                        if(cameraId.split(": ")[1].equals(cameraName)){
                            writing = true;
                        }

                        if(writing) {
                            writer.write(line);
                            writer.newLine();
                            writer.write(cameraId);
                            writer.newLine();
                        }

                        String line2 = sc.readLine();

                        while(!line2.equals("$END")){

                            if(writing) {
                                writer.write(line2);
                                writer.newLine();
                            }
                            line2 = sc.readLine();
                        }

                        if(writing) {
                            writer.write(line2);
                            writer.newLine();
                        }
                        //String cameraId = sc.readLine();

                        //String currentId = cameraId.split(": ")[1];

                        //if(currentId.equals(cameraName)){

                        //}

                    }else{
                        writing = true;
                    }

                    if(writing) {
                        writer.write(line);
                        writer.newLine();
                    }


                }

                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }
}

class photo{

    public String id;
    public String name;
    public String path;
    public String cameraId;
    public String cameraName;
    public String cameraType;

    public photo(){

    }

    public photo(String id, String name, String path, String cameraId, String cameraName, String cameraType){

        this.id = id;
        this.name = name;
        this.path = path;
        this.cameraId = cameraId;
        this.cameraName = cameraName;
        this.cameraType = cameraType;

    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getCameraType() {
        return cameraType;
    }

    public void setCameraType(String cameraType) {
        this.cameraType = cameraType;
    }

    // toString method
    public String toString(){
        return "Name: " + name + " Path: " + path + " Camera ID: " + cameraId;
    }
}

class Camera{

    String cameraId;

    public Camera(){

    }

    public Camera(String cameraId){
        this.cameraId = cameraId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }



}