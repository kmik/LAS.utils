package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class inpho {

    HashSet<String> includedImages = new HashSet<>();
    File inphoFile = null;

    int numControlPoints = 0;

    HashMap<Integer, ControlPoint> controlPoints = new HashMap<>();

    int numPhotos = 0;
    int numCameras = 0;

    ArrayList<String> cameraIds = new ArrayList<>();
    ArrayList<Camera> cameras = new ArrayList<>();

    ArrayList<photo> photos = new ArrayList<>();

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

                    this.photos.add(phot);
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

    public void findControlPoints(){

        int uniqueId = 0;
        // Read inpho file line by line
        // Parse the file and store the data in the class variables
        try {
            BufferedReader sc = new BufferedReader(new FileReader(inphoFile));

            while(sc.ready()){
                String line = sc.readLine();

                if(line.equals("$CONTROL_POINTS")){

                    while(!line.equals("$END_POINTS")){

                        if(line.contains("$ID")){

                            String[] tokens = line.split(" ");

                            System.out.println(Arrays.toString(tokens));
                            String id = tokens[tokens.length-1];


                            ControlPoint newControlPoint = new ControlPoint();
                            newControlPoint.setUniqueId(uniqueId++);

                            newControlPoint.setName(id);

                            line = sc.readLine();

                            //System.out.println(line);
                            while(!line.contains("$ID")){


                                line = sc.readLine();
                                //System.out.println(line);

                                if(line.contains("$END_POINTS"))
                                    break;

                                if(line.contains("$POSITION")){

                                    // Regular expression to match doubles
                                    String regex = "\\d+\\.\\d+";

                                    Pattern pattern = Pattern.compile(regex);
                                    Matcher matcher = pattern.matcher(line);

                                    double[] values = new double[3];
                                    int counter = 0;

                                    while (matcher.find()) {
                                        double value = Double.parseDouble(matcher.group());
                                        values[counter++] = value;

                                        System.out.println(value);
                                    }

                                    newControlPoint.setX(values[0]);
                                    newControlPoint.setY(values[1]);
                                    newControlPoint.setZ(values[2]);

                                }

                                controlPoints.put(newControlPoint.getUniqueId(), newControlPoint);

                            }

                        }

                        //System.out.println(line);
                        if(line.contains("$END_POINTS"))
                            break;

                        line = sc.readLine();
                        //System.out.println(line);

                    }

                    System.out.println("OUT!");

                }


            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void setIncludedImages(String file){

        // Each line of the file contains the name of an image to be excluded
        // Read the file and store the names in the HashSet ignoredImages

        try {
            BufferedReader sc = new BufferedReader(new FileReader(file));

            while (sc.ready()) {
                String line = sc.readLine();
                includedImages.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void findPhotos(){

        String photoname = null;
        boolean include = false;

        // Read inpho file line by line
        // Parse the file and store the data in the class variables
        try {

            BufferedReader sc = new BufferedReader(new FileReader(inphoFile));

            while (sc.ready()) {

                String line = sc.readLine();

                boolean writing = false;

                if (line.contains("$PHOTO_FILE")) {

                    photo p = new photo();
                    String[] split = line.split(": ");
                    String photoPath = split[1];
                    String[] splitPath = photoPath.split("[/\\\\]");


                    photoname = splitPath[splitPath.length - 1];

                    p.setName(photoname);

                    if(includedImages.contains(photoname)){
                        photos.add(p);
                        include = true;
                    }else{
                        include = false;
                    }


                }

                if (line.contains("$PHOTO_POINTS") && include){

                    while(!line.contains("$END_POINTS")){

                        line = sc.readLine();

                        // Regex pattern to match: String (non-whitespace) followed by two doubles
                        String regex = "(\\S+)\\s+([+-]?\\d+\\.\\d+)\\s+([+-]?\\d+\\.\\d+)";

                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(line);

                        if (matcher.find()) {
                            // Extract the values
                            String firstValue = matcher.group(1);  // First string
                            double secondValue = Double.parseDouble(matcher.group(2)); // First double
                            double thirdValue = Double.parseDouble(matcher.group(3));  // Second double

                            if(includedImages.contains(photoname)) {
                                photos.get(photos.size() - 1).photoControlPoints.add(firstValue);
                                photos.get(photos.size() - 1).photControlPointsCoords.add(new double[]{secondValue, thirdValue});
                            }
                            // Output results
                        }


                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(photo p : photos){
            System.out.println(p.getName());

            for(int i = 0; i < p.photoControlPoints.size(); i++){
                //System.out.println(p.photoControlPoints.get(i) + " " + p.photControlPointsCoords.get(i)[0] + " " + p.photControlPointsCoords.get(i)[1]);
            }


            System.out.println( p.photoControlPoints.size());


        }

    }
    public void splitFileToSingleCameras(){

        String outputDirectory = aR.odir;

        if(aR.odir.equals("asd")){
            throw new IllegalArgumentException("Output directory not set, this tool requires an output directory. Please define by setting -odir argument.");
        }

        for(int i = 0; i < this.numCameras; i++) {

            String cameraName = cameras.get(i).getCameraId();

            String outputFileName = outputDirectory + aR.pathSep + cameraName + ".INPHO";

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

                    if(line.contains("$PHOTO_FILE")){

                        String[] split = line.split(": ");
                        String photoPath = split[1];
                        String[] splitPath = photoPath.split("[/\\\\]");

                        String photoName = splitPath[splitPath.length-1];
                        String replacementPath = aR.path + aR.pathSep + photoName;

                        line = line.replace(photoPath, replacementPath);

                    }
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

    public void writePhotosAndCameras() {

        String outputDirectory = aR.odir;

        if (aR.odir.equals("asd")) {
            throw new IllegalArgumentException("Output directory not set, this tool requires an output directory. Please define by setting -odir argument.");
        }

        String outputFileName = outputDirectory + aR.pathSep + "photos.txt";

        File outputFile = new File(outputFileName);

        if (outputFile.exists()) {
            outputFile.delete();
        }

        try {
            outputFile.createNewFile();

            BufferedWriter writer = new BufferedWriter(new BufferedWriter(new FileWriter(outputFile)));

            for (int i = 0; i < this.numPhotos; i++) {

                photo p = new photo();

                p = this.getPhoto(i);

                writer.write(p.getName() + "\t" + p.getCameraId());
                writer.newLine();

            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public photo getPhoto(int i){
        return this.photos.get(i);
    }

    public void writeMicMacControlPointFiles(String odir){

        odir = aR.odir;

        File mesure_appuis = new File(odir + aR.pathSep + "Mesure-Appuix.xml");
        File dico_appuis = new File(odir + aR.pathSep + "Dico-Appuis.xml");

        BufferedWriter mesure_appuis_writer = null;
        BufferedWriter dico_appuis_writer = null;

        try {
            mesure_appuis.createNewFile();
            dico_appuis.createNewFile();

            mesure_appuis_writer = new BufferedWriter(new FileWriter(mesure_appuis));
            dico_appuis_writer = new BufferedWriter(new FileWriter(dico_appuis));


            dico_appuis_writer.write("<?xml version=\"1.0\" ?>");
            dico_appuis_writer.newLine();
            dico_appuis_writer.write("<Global>");
            dico_appuis_writer.newLine();
            dico_appuis_writer.write("\t<DiscoAppuisFlottant>");
            dico_appuis_writer.newLine();
            for (ControlPoint cp : controlPoints.values()) {



                dico_appuis_writer.write("\t\t<OneAppuisDAF>");
                dico_appuis_writer.newLine();
                dico_appuis_writer.write("\t\t\t<Pt>");
                dico_appuis_writer.write(" " + cp.getX() + " " + cp.getY() + " " + cp.getZ() + " </Pt>");
                dico_appuis_writer.newLine();
                dico_appuis_writer.write("\t\t\t<NamePt> " + cp.getName() + " </NamePt>");
                dico_appuis_writer.newLine();
                dico_appuis_writer.write("\t\t\t<Incertitude> " + 0.01 + " " + 0.01 + " " + 0.01 + " </Incertitude>");
                dico_appuis_writer.newLine();
                dico_appuis_writer.write("\t\t</OneAppuisDAF>");
                dico_appuis_writer.newLine();

            }

            dico_appuis_writer.write("\t</DiscoAppuisFlottant>");
            dico_appuis_writer.newLine();
            dico_appuis_writer.write("</Global>");

            mesure_appuis_writer.write("<SetOfMeasureAppuisFlottants>");
            mesure_appuis_writer.newLine();

            for(int i = 0; i < this.photos.size(); i++){

                photo p = photos.get(i);

                if(p.photControlPointsCoords.size() > 0){

                    mesure_appuis_writer.write("\t<MeasureAppuiFlottant1Im>");
                    mesure_appuis_writer.newLine();

                    mesure_appuis_writer.write("\t\t<NameIm>" + p.getName() + "</NameIm>");

                    mesure_appuis_writer.newLine();

                    for(int j = 0; j < p.photControlPointsCoords.size(); j++){

                        mesure_appuis_writer.write("\t\t<OneMeasureAF1I>");
                        mesure_appuis_writer.newLine();
                        mesure_appuis_writer.write("\t\t\t<NamePt> " + p.photoControlPoints.get(j) + " </NamePt>");
                        mesure_appuis_writer.newLine();
                        mesure_appuis_writer.write("\t\t\t<PtIm>" + p.photControlPointsCoords.get(j)[0] + " " + p.photControlPointsCoords.get(j)[1] + " </PtIm>");
                        mesure_appuis_writer.newLine();
                        mesure_appuis_writer.write("\t\t</OneMeasureAF1I>");
                        mesure_appuis_writer.newLine();
                    }


                    mesure_appuis_writer.write("\t</MeasureAppuiFlottant1Im>");
                    mesure_appuis_writer.newLine();
                }


            }


            mesure_appuis_writer.write("</SetOfMeasureAppuisFlottants>");
            mesure_appuis_writer.close();
            dico_appuis_writer.close();

        } catch (Exception e) {
            e.printStackTrace();
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

    ArrayList<String> photoControlPoints = new ArrayList<>();
    ArrayList<double[]> photControlPointsCoords = new ArrayList<>();

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

class ControlPoint{

    String name;

    int uniqueId;

    int type;
    double x, y, z;

    public ControlPoint(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }
}