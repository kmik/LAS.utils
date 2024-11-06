package utils;

import org.bytedeco.opencv.presets.opencv_core;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

public class MicMacOri {

    argumentReader aR;

    File inputOrientationFile;

    ArrayList<ImageInfo> images = new ArrayList<ImageInfo>();

    public MicMacOri() {

    }

    public MicMacOri(argumentReader aR) {
        this.aR = aR;
    }

    public void modifypp(String directoryPath, double numberToAdd1, double numberToAdd2) {
        File dir = new File(directoryPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));

        if (files != null) {
            for (File file : files) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));

                    // Pattern to match two doubles separated by whitespace between <PP> and </PP>
                    Pattern pattern = Pattern.compile("<PP>(\\d+(?:\\.\\d+)?)\\s+(\\d+(?:\\.\\d+)?)</PP>");
                    Matcher matcher = pattern.matcher(content);

                    StringBuffer modifiedContent = new StringBuffer();

                    while (matcher.find()) {
                        // Extract the two doubles
                        double firstValue = Double.parseDouble(matcher.group(1));
                        double secondValue = Double.parseDouble(matcher.group(2));

                        // Add the specified number to both values
                        firstValue += numberToAdd1;
                        secondValue += numberToAdd2;
                        System.out.println(firstValue);
                        // Replace the content within <PP> tags with the modified values
                        String replacement = String.format("<PP>%.2f %.2f</PP>", firstValue, secondValue);
                        matcher.appendReplacement(modifiedContent, replacement);
                    }

                    matcher.appendTail(modifiedContent);

                    // Write the modified content back to the file
                    Files.write(file.toPath(), modifiedContent.toString().getBytes());
                    System.out.println("Modified file: " + file.getName());
                } catch (IOException e) {
                    System.out.println("Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No .xml files found in the specified directory.");
        }
    }

    public void setOrientationFile(File inputOrientationFile) {
        this.inputOrientationFile = inputOrientationFile;
    }

    public void setOrientationFile(String inputOrientationFile) {
        this.inputOrientationFile = new File(inputOrientationFile);
    }

    public void parseOrientationFile() {

        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(inputOrientationFile));
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");

                if(values.length < 3){
                    break;
                }
                ImageInfo imageInfo = new ImageInfo();

                imageInfo.setEpsg_code(((aR.EPSG)));
                imageInfo.setImageName(values[0]);
                imageInfo.setX(Double.parseDouble(values[1]));
                imageInfo.setY(Double.parseDouble(values[2]));
                imageInfo.setZ(Double.parseDouble(values[3]));
                imageInfo.setOmega(Double.parseDouble(values[4]));
                imageInfo.setPhi(Double.parseDouble(values[5]));
                imageInfo.setKappa(Double.parseDouble(values[6]));

                imageInfo.convertCoordinatesTo(4326);

                //imageInfo.convertAnglesFromTo("grad", "deg");
                images.add(imageInfo);

                System.out.println(imageInfo.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void writeOrientationFile(String outputOrientationFile) {

        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(outputOrientationFile));

            // First line is # followed by abbreviations of column names
            bw.write("#F=N Y X Z K W P");
            bw.newLine();
            bw.write("#");
            bw.newLine();
            bw.write("#image latitude longitude altitude yaw pitch roll");
            bw.newLine();
            int counter = 0;
            for (ImageInfo image : images) {
                bw.write(image.getImageName() + " " + image.getY() + " " + image.getX() + " " + image.getZ() + " " + image.getKappa() + " " + image.getPhi() + " " + image.getOmega());
                if (counter < images.size() - 1) {
                    bw.newLine();
                }
            }


            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


class ImageInfo{

    int epsg_code;
    String imageName;
    double x, y, z, omega, phi, kappa;

    ImageInfo(){

    }

    public String getImageName() {
        return imageName;
    }

    public void convertCoordinatesTo(int epsg){
        SpatialReference srcSRS = new SpatialReference();
        srcSRS.ImportFromEPSG(epsg_code); // Example source EPSG: 4326 (WGS 84)

        SpatialReference dstSRS = new SpatialReference();
        dstSRS.ImportFromEPSG(epsg); // Example target EPSG: 3857 (Web Mercator)

        // Create the coordinate transformation
        CoordinateTransformation transformation = new CoordinateTransformation(srcSRS, dstSRS);

        // Define your point coordinates in the source EPSG
        double[] point = { x, y }; // Example coordinates

        double[] transformedPoint = transformation.TransformPoint(point[0], point[1]);

        System.out.println("Original X: " + x + " Y: " + y);
        x = transformedPoint[0];
        y = transformedPoint[1];
        System.out.println("Transformed X: " + x + " Y: " + y);

        this.epsg_code = epsg;

    }

    public void convertAnglesFromTo(String from, String to){

        if(from.equals("rad") && to.equals("deg")) {
            omega = Math.toDegrees(omega);
            phi = Math.toDegrees(phi);
            kappa = Math.toDegrees(kappa);
        }
        if(from.equals("deg") && to.equals("rad")) {
            omega = Math.toRadians(omega);
            phi = Math.toRadians(phi);
            kappa = Math.toRadians(kappa);
        }
        if(from.equals("grad") && to.equals("deg")) {
            omega = omega * 0.9;
            phi = phi * 0.9;
            kappa = kappa * 0.9;
        }
        if(from.equals("deg") && to.equals("grad")) {
            omega = omega / 0.9;
            phi = phi / 0.9;
            kappa = kappa / 0.9;
        }

    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
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

    public double getOmega() {
        return omega;
    }

    public void setOmega(double omega) {
        this.omega = omega;
    }

    public double getPhi() {
        return phi;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public void setEpsg_code(int epsg_code) {
        this.epsg_code = epsg_code;
    }

    public int getEpsg_code() {
        return epsg_code;
    }

    public String toString(){
        return imageName + " " + x + " " + y + " " + z + " " + omega + " " + phi + " " + kappa;
    }

}
