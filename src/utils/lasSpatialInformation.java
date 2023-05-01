package utils;

import LASio.LASReader;
import err.toolException;
import org.gdal.ogr.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class lasSpatialInformation {

    ArrayList<String> polyName = new ArrayList<>();
    ArrayList<double[][]> poly = new ArrayList<>();
    argumentReader aR = null;

    public lasSpatialInformation(){

    }

    public lasSpatialInformation(argumentReader aR){
        this.aR = aR;
    }

    public void writeExtentToFile() throws IOException{

        if(aR.output.equals("asd")){
            throw new toolException("No output defined!");
        }

        ArrayList<String> output = new ArrayList<>();

        double min_x = Double.POSITIVE_INFINITY;
        double max_x = Double.NEGATIVE_INFINITY;

        double min_y = Double.POSITIVE_INFINITY;
        double max_y = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < aR.inputFiles.size(); i++){


            LASReader tempReader = new LASReader(aR.inputFiles.get(i));

            if(tempReader.getMinX() < min_x)
                min_x = tempReader.getMinX();

            if(tempReader.getMaxX() > max_x)
                max_x = tempReader.getMaxX();

            if(tempReader.getMinY() < min_y)
                min_y = tempReader.getMinY();

            if(tempReader.getMaxY() > max_y)
                max_y = tempReader.getMaxY();


        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(aR.output));

        String out = min_x + " " + max_y + " " + max_x + " " + min_y;
        writer.write(out);
        writer.close();


    }

    public void getCoverageUtm5() throws Exception{

        ArrayList<String> output = new ArrayList<>();

        double min_x = Double.POSITIVE_INFINITY;
        double max_x = Double.NEGATIVE_INFINITY;

        double min_y = Double.POSITIVE_INFINITY;
        double max_y = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < aR.inputFiles.size(); i++){

            LASReader tempReader = new LASReader(aR.inputFiles.get(i));

            if(tempReader.getMinX() < min_x)
                min_x = tempReader.getMinX();

            if(tempReader.getMaxX() > max_x)
                max_x = tempReader.getMaxX();

            if(tempReader.getMinY() < min_y)
                min_y = tempReader.getMinY();

            if(tempReader.getMaxY() > max_y)
                max_y = tempReader.getMaxY();


        }

        /*
        max_x = (double)Math.ceil(max_x);
        min_x = (double)Math.floor(min_x);

        max_y = (double)Math.ceil(max_y);
        min_y = (double)Math.floor(min_y);
*/
        //System.out.println(max_x + " " + min_x + " " + max_y + " " + min_y);
        this.readShapeFiles(aR.poly);

        //System.out.println(this.polyName.size());

        ArrayList<Integer> overlappingPolygons = findOverlappingPolygons(poly, min_x, min_y, max_x, max_y);

        //System.out.println(overlappingPolygons.size());

        for(int i = 0; i < overlappingPolygons.size(); i++){
            output.add(polyName.get(overlappingPolygons.get(i)));
        }

        if(aR.output.equals("asd")){
            throw new toolException("No output file specified");
        }

        writeStringsToFile(output, aR.output);
    }

    public static void writeStringsToFile(ArrayList<String> strings, String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        for (String str : strings) {
            writer.write(str);
            writer.newLine();
        }
        writer.close();
    }

    public ArrayList<Integer> findOverlappingPolygons(ArrayList<double[][]> polygons, double min_x, double min_y, double max_x, double max_y) {
        ArrayList<Integer> overlappingPolygons = new ArrayList<Integer>();
        int counter = 0;


        for (double[][] polygon : polygons) {

            double polygon_min_x = polygon[0][0];
            double polygon_max_x = polygon[1][0];

            double polygon_max_y = polygon[0][1];
            double polygon_min_y = polygon[2][1];

            boolean overlaps = true;

            if(max_x < polygon_min_x || min_x > polygon_max_x){
                overlaps = false;
            }

            if (max_y < polygon_min_y || min_y > polygon_max_y) {
                overlaps = false;
            }

            if (overlaps) {
                overlappingPolygons.add(counter);
            }
/*
            if(this.polyName.get(counter).equals("P4314F1")){
                print2DArray(polygon);
                System.out.println(polygon_min_x + " " + polygon_min_y + " " + polygon_max_x + " " + polygon_max_y);
                System.out.println(min_x + " " + min_y + " " + max_x + " " + max_y);
                System.out.println("exit " + overlaps);
                System.exit(1);
            }
*/


            counter++;
        }
        return overlappingPolygons;
    }

    public static void print2DArray(double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void readShapeFiles(String shapeFile) throws IOException {

        ogr.RegisterAll();
        DataSource ds = ogr.Open( shapeFile );
        //DataSource ds2 = ogr.Open( shapeFile2 );

        if( ds == null ) {
            System.out.println( "Opening plot file failed." );
            System.exit( 1 );
        }

        Layer shapeFileLayer = ds.GetLayer(0);

        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            if( i % 3 == 0 || true) {
                Feature tempF = shapeFileLayer.GetFeature(i);
                Geometry tempG = tempF.GetGeometryRef();
                Geometry tempG2 = tempG.GetGeometryRef(0);

                if (tempG == null)
                    continue;

                poly.add(clone2DArray(tempG2.GetPoints()));
                polyName.add(tempF.GetFieldAsString(0));

            }
        }


    }

    public static double[][] clone2DArray(double[][] original) {
        int rows = original.length;
        double[][] clone = new double[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new double[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }

}
