package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class KarttaLehtiJako {
    double sideLength = 6000;
    public double minX = 0;
    public double minY = 0;
    public double maxX = 0;
    public double maxY = 0;

    public HashSet<String> mapSheetNames = new HashSet<String>();
    int xDim = 0;
    int yDim = 0;

    HashSet<String> configMapSheets = new HashSet<String>();
    public ArrayList<double[]> configMapSheetExtents = new ArrayList<double[]>();
    public ArrayList<String> configMapSheetNames = new ArrayList<String>();
    public HashMap<Integer, String> index_to_map_name = new HashMap<Integer, String>();
    public KarttaLehtiJako(){

    }

    public void create(){

    }

    public String getIndexToMapName(int x, int y){

        int index = x + y * xDim;

        return index_to_map_name.get(index);

    }

    public void readFromFile(File inputFile) throws Exception {

        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("ETRS89_TM35FIN_karttalehtijako.txt");

        BufferedReader br = new BufferedReader(new InputStreamReader(in));


        String line = "";
        String cvsSplitBy = "\t";
        String [] lineArray;
        line = br.readLine();
        lineArray = line.split(cvsSplitBy);

        double minY = Double.MAX_VALUE;
        double minX = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxX = Double.MIN_VALUE;

        while((line = br.readLine()) != null){

            lineArray = line.split(cvsSplitBy);

            double x = Double.parseDouble(lineArray[2]);
            double x_max = Double.parseDouble(lineArray[3]);
            double y = Double.parseDouble(lineArray[4]);
            double y_max = Double.parseDouble(lineArray[5]);
            mapSheetNames.add(lineArray[6]);

            if (x < minX)
                minX = x;
            if (x_max > maxX)
                maxX = x_max;
            if (y < minY)
                minY = y;
            if (y_max > maxY)
                maxY = y_max;
        }

        this.maxX = maxX;
        this.maxY = maxY;
        this.minX = minX;
        this.minY = minY;

/*
        System.out.println("minX: " + minX);
        System.out.println("minY: " + minY);
        System.out.println("maxX: " + maxX);
        System.out.println("maxY: " + maxY);

        System.out.println("x_dim: " + (maxX - minX)/sideLength);
        System.out.println("y_dim: " + (maxY - minY)/sideLength);
*/
        //System.exit(1);
        int xDim = (int)((maxX - minX)/sideLength);
        int yDim = (int)((maxY - minY)/sideLength);

        this.xDim = xDim;
        this.yDim = yDim;

        int[][] lehdet = new int[xDim][yDim];

        this.index_to_map_name = new HashMap<Integer, String>();

        in = this.getClass().getClassLoader()
                .getResourceAsStream("ETRS89_TM35FIN_karttalehtijako.txt");
        br = new BufferedReader(new InputStreamReader(in));

        br.readLine();

        while((line = br.readLine()) != null){

            lineArray = line.split(cvsSplitBy);

            double x = Double.parseDouble(lineArray[2]);
            double y = Double.parseDouble(lineArray[4]);

            int x_index = (int)((x - minX)/sideLength);
            x_index = Math.min(x_index, xDim - 1);
            int y_index = (int)((maxY - y )/sideLength);
            y_index = Math.min(y_index, yDim - 1);

            //System.out.println(x_index + " " + y_index);
            int index = x_index + y_index * xDim;


            lehdet[x_index][y_index] = index;
            index_to_map_name.put(index, lineArray[6]);

        }
        //System.exit(1);

        //for(int i : index_to_map_name.keySet())
        //    System.out.println(i + " " + index_to_map_name.get(i));


        //System.exit(1);

        // read inputFile line by line


    }

    public void readFromFile(File inputFile, HashSet<String> mapSheetsToConsider) throws Exception {

        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("ETRS89_TM35FIN_karttalehtijako.txt");

        BufferedReader br = new BufferedReader(new InputStreamReader(in));


        String line = "";
        String cvsSplitBy = "\t";
        String [] lineArray;
        line = br.readLine();
        lineArray = line.split(cvsSplitBy);

        while((line = br.readLine()) != null){

            lineArray = line.split(cvsSplitBy);

            String mapSheetName = lineArray[6];

            if(mapSheetsToConsider.contains(mapSheetName)){

                configMapSheetExtents.add(new double[]{Double.parseDouble(lineArray[2]), Double.parseDouble(lineArray[3]), Double.parseDouble(lineArray[4]), Double.parseDouble(lineArray[5])});
                configMapSheetNames.add(mapSheetName);

            }
        }


    }

    public boolean isGarbageName(String name){

        if(mapSheetNames.contains(name))
            return true;
        else
            return false;
    }




   public void readFromObject(File objectFile){


   }
}

