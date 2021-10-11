package runners;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.util.HashMap;

import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
//import org.gdal.ogr.Driver;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdalconst.*;

//import javafx.scene.paint.Color;
import LASio.*;
//import Tinfour.*; 

import tools.*;


class ITDtest{


    /**
     * @deprecated
     * This method is no longer being used anywhere.
     * @param pointClouds
     * @return
     * @throws IOException
     */
    public static boolean calibrateToSentinel(ArrayList<LASReader> pointClouds) throws IOException{
        
        int[] temp_pixel = new int[1];


        String image = "/media/koomikko/B8C80A93C80A4FD41/UAV_TREE_SPECIES/sentinel_2/mosaic/test_plots/resampled/rgb_test.tif";
        String image2 = "/media/koomikko/B8C80A93C80A4FD41/UAV_TREE_SPECIES/sentinel_2/mosaic/test_plots/resampled/nir_test.tif";
        
        String outputDirectory = "/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/all_point_clouds_collinear/ground_classified/dz/clipped/noisefree/rgb_normalized/";


        ArrayList<String> ofiles = new ArrayList<String>();

        /* Declare outputFiles */
        for(int i = 0; i < pointClouds.size(); i++){

            String fileName = outputDirectory + pointClouds.get(i).getFile().getName();
            ofiles.add(fileName);

            File temp = new File(fileName);

            if(temp.exists())
                temp.delete();
                    
            temp.createNewFile();
            LASraf raTemp = new LASraf(temp);
            LASwrite.writeHeader(raTemp, "lasTile", pointClouds.get(i).versionMajor, pointClouds.get(i).versionMinor,
                    pointClouds.get(i).pointDataRecordFormat, pointClouds.get(i).pointDataRecordLength,
                    pointClouds.get(i).headerSize, pointClouds.get(i).offsetToPointData, pointClouds.get(i).numberVariableLengthRecords,
                    pointClouds.get(i).fileSourceID, pointClouds.get(i).globalEncoding,
                    pointClouds.get(i).xScaleFactor, pointClouds.get(i).yScaleFactor, pointClouds.get(i).zScaleFactor,
                    pointClouds.get(i).xOffset, pointClouds.get(i).yOffset, pointClouds.get(i).zOffset);

            raTemp.close();
        }


        Dataset rgb_test = gdal.Open(image, gdalconstConstants.GA_ReadOnly);
        Dataset nir_test = gdal.Open(image2, gdalconstConstants.GA_ReadOnly);

        Band R = rgb_test.GetRasterBand(1);
        Band G = rgb_test.GetRasterBand(2);
        Band B = rgb_test.GetRasterBand(3);

        Band nir = nir_test.GetRasterBand(1);

        double[] dimensions = rgb_test.GetGeoTransform();

        int xSize = R.getXSize();
        int ySize = R.getYSize();

        System.out.println(Arrays.toString(dimensions));

        LasPoint tempPoint = new LasPoint();

        double xCoord = 0.0;
        double yCoord = 0.0;

        double minX = dimensions[0];
        double maxY = dimensions[3];

        int x = 0;
        int y = 0;

        long sumR = 0L;
        long sumG = 0L;
        long sumB = 0L;

        long sumR_p = 0L;
        long sumG_p = 0L;
        long sumB_p = 0L;

        long sumR_p2 = 0L;
        long sumG_p2 = 0L;
        long sumB_p2 = 0L;

        double minXcloud = 0.0;
        double maxXcloud = 0.0;

        double minYcloud = 0.0;
        double maxYcloud = 0.0;

        PointInclusionRule rule = new PointInclusionRule(true);

        for(int o = 0; o < pointClouds.size(); o++){

            LASReader reader = pointClouds.get(o);

            System.out.println("POINT CLOUD NAME: " + reader.getFile().getName());
            minXcloud = reader.minX;
            maxXcloud = reader.maxX;

            minYcloud = reader.minY;
            maxYcloud = reader.maxY;

            LASraf writer = new LASraf(new File(ofiles.get(o)));

            long n = reader.getNumberOfPointRecords();

            sumR = 0L;
            sumG = 0L;
            sumB = 0L;

            sumR_p = 0L;
            sumG_p = 0L;
            sumB_p = 0L;

            //HashMap<Int, Int> bank = new HashMap<Int, Int>();

            long count = 0L;

            int correctionR = -1;
            int correctionG = -1;
            int correctionB = -1;


            double sq_sumR = 0.0;
            double sq_sumG = 0.0;
            double sq_sumB = 0.0;

            double sq_sumR_p = 0.0;
            double sq_sumG_p = 0.0;
            double sq_sumB_p = 0.0;

            for(long i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

                y = Math.max((int)Math.floor((maxY - tempPoint.y) / 0.5), 0) ;
                x = Math.max((int)Math.floor((tempPoint.x - minX) / 0.5), 0) ;

                //System.out.println("XY: " + x + " " + y);
                yCoord = maxY - (y * 0.5 - 0.25);
                xCoord = minX + (x * 0.5 - 0.25);

                sumG_p += tempPoint.G;
                sumR_p += tempPoint.R;
                sumB_p += tempPoint.B;

                sq_sumR_p += tempPoint.R * tempPoint.R;
                sq_sumG_p += tempPoint.G * tempPoint.G;
                sq_sumB_p += tempPoint.B * tempPoint.B;

                R.ReadRaster(x, y, 1, 1, temp_pixel);
                
                if(temp_pixel[0] > 0){
                    sumR += temp_pixel[0];
                    sq_sumR += (temp_pixel[0] * temp_pixel[0]);
                    count++;
                }
                
                G.ReadRaster(x, y, 1, 1, temp_pixel);
                
                if(temp_pixel[0] > 0){
                    sumG += temp_pixel[0];
                    sq_sumG += (temp_pixel[0] * temp_pixel[0]);
                }
                
                B.ReadRaster(x, y, 1, 1, temp_pixel);
                
                if(temp_pixel[0] > 0){
                    sumB += temp_pixel[0];
                    sq_sumB += (temp_pixel[0] * temp_pixel[0]);
                }
                
            }



            sumR = sumR / count;
            sumG = sumG / count;
            sumB = sumB / count;

            double varianceR = sq_sumR / count - sumR * sumR;
            double varianceG = sq_sumG / count - sumG * sumG;
            double varianceB = sq_sumB / count - sumB * sumB;

            double stdR = Math.sqrt(varianceR);
            double stdG = Math.sqrt(varianceG);
            double stdB = Math.sqrt(varianceB);

            sumR_p = sumR_p / n;
            sumG_p = sumG_p / n;
            sumB_p = sumB_p / n;

            double varianceR_p = sq_sumR_p / n - sumR_p * sumR_p;
            double varianceG_p = sq_sumG_p / n - sumG_p * sumG_p;
            double varianceB_p = sq_sumB_p / n - sumB_p * sumB_p;

            double stdR_p = Math.sqrt(varianceR_p);
            double stdG_p = Math.sqrt(varianceG_p);
            double stdB_p = Math.sqrt(varianceB_p);

            //System.out.println(sumR + " - " + sumR_p);

            correctionR = (int)(sumR - sumR_p);
            correctionG = (int)(sumG - sumG_p);
            correctionB = (int)(sumB - sumB_p);

            sumR_p2 = 0L;
            sumG_p2 = 0L;
            sumB_p2 = 0L;

            //yi = m2 + (xi - m1) * (s2/s1)

            for(int i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

                y = (int)Math.floor((maxY - tempPoint.y) / 0.5) ;
                x = (int)Math.floor((tempPoint.x - minX) / 0.5) ;

                //nir.ReadRaster​(x, y, 1, 1, temp_pixel);

                /*
                tempPoint.R += correctionR;
                tempPoint.G += correctionG;
                tempPoint.B += correctionB;
                */
                //System.out.println((short)(sumR + (tempPoint.R - sumR_p) * (stdR / stdR_p)));
                /*
                tempPoint.R = (short)(temp_pixel[0]);
                tempPoint.G = (short)(sumR + (tempPoint.R - sumR_p) * (stdR / stdR_p));
                tempPoint.B = (short)(sumG + (tempPoint.G - sumG_p) * (stdG / stdG_p));
                */
                
                tempPoint.R = (short)(sumR + (tempPoint.R - sumR_p) * (stdR / stdR_p));
                tempPoint.G = (short)(sumG + (tempPoint.G - sumG_p) * (stdG / stdG_p));
                //tempPoint.B = (short)(sumB + (tempPoint.B - sumB_p) * (stdB / stdB_p));
                /*
                if( (tempPoint.G + tempPoint.R - tempPoint.B) != 0)
                    tempPoint.B = (short)((tempPoint.G - tempPoint.R) / (tempPoint.G + tempPoint.R - tempPoint.B) * 10);
                else
                    tempPoint.B = 0;
                */
                sumR_p2 += tempPoint.R;
                sumG_p2 += tempPoint.G;
                sumB_p2 += tempPoint.B;

                writer.writePoint( tempPoint, rule, reader.xScaleFactor, reader.yScaleFactor, reader.zScaleFactor,
                        reader.xOffset, reader.yOffset, reader.zOffset, reader.pointDataRecordFormat, i);

            }

            sumR_p2 = sumR_p2 / n;
            sumG_p2 = sumG_p2 / n;
            sumB_p2 = sumB_p2 / n;

            //System.out.println("after: " + sumR_p2);

            writer.writeBuffer2();
            writer.updateHeader2();

            System.out.println(o);
        }

        return false;
    }

    public static void main(String[] args) throws IOException   {

        /*
        Thread asd = new Thread() {
                @Override
                public void run() {
                    javafx.application.Application.launch(StartUpTest.class);
                }
            };
            
            asd.start();
            StartUpTest startUpTest = StartUpTest.waitForStartUpTest();
            startUpTest.printSomething();
            

        startUpTest.updateStage();

        */
        /*
        File file = new File("tree_table_grid.csv");

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
        
                if(lineCount != 0)
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0)
                        treeCount++;

                lineCount++;

            }
        }

        System.out.println("Trees: " + treeCount);

        double[][] trees = new double[treeCount][9];

        treeCount = 0;
        lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            
            while ((line = br.readLine()) != null) {
        
                if(lineCount != 0){
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0){

                        trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);
                        trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);
                        trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);
                        trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);
                        trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);
                        trees[treeCount][5] = 0.0;
                        trees[treeCount][6] = Double.parseDouble(line.split(",")[5]);
                        trees[treeCount][7] = Double.parseDouble(line.split(",")[2]);
                        
                        treeCount++;

                }

                lineCount++;
            }
        }
           */
        File file = new File("testi_1.las");

        ITDstatistics stats = new ITDstatistics();

        stats.readMeasuredTrees(new File("tree_table_grid.csv"));

        //stats.setTreeBank(trees);


        stats.setOutput(new File("srcNN/asdi.txt"));

        int currentId = -1;

        int previousId = -1;    

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        LasPoint tempPoint = new LasPoint();

        ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();

        ArrayList<String> filesList = new ArrayList<String>();

        //filesList = runners.MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/all_point_clouds_collinear/ground_classified/dz/clipped/noisefree/clipped/", ".las");
        filesList = MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/all_point_clouds_collinear/ground_classified/dz/clipped/noisefree/", "_ITD.las");
        
        

        for(int i = 0; i < filesList.size(); i++){
            //System.out.println(filesList.get(i));
            pointClouds.add(new LASReader(new File(filesList.get(i))));

        }

        //calibrateToSentinel("asd", pointClouds);

        filesList = MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/all_point_clouds_collinear/ground_classified/dz/clipped/noisefree/rgb_normalized/", "_ITD.las");
        
        pointClouds.clear();

        for(int i = 0; i < filesList.size(); i++){
            //System.out.println(filesList.get(i));
            pointClouds.add(new LASReader(new File(filesList.get(i))));

        }

        File fout = null;

        DataSource ds = ogr.Open("/media/koomikko/B8C80A93C80A4FD41/artikkeli_3/GridPlots.shp");
        System.out.println("Layer count: " + ds.GetLayerCount());
        Layer layeri = ds.GetLayer(0);
        System.out.println("Feature count: " + layeri.GetFeatureCount());

          try{

            fout = new File("tempWKT.csv");

            if(fout.exists())
              fout.delete();

            fout.createNewFile();

            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write("WKT,plot_id");
            bw.newLine();

            //System.out.println("Feature count: " + layeri.GetFeatureCount());

            for(long i = 0; i < layeri.GetFeatureCount(); i++ ){

              Feature tempF = layeri.GetFeature(i);
              Geometry tempG = tempF.GetGeometryRef();
              String id = "";

              if(tempF.GetFieldCount() > 0)
                id = tempF.GetFieldAsString(0);
              else
                id = String.valueOf(i);

              //System.out.println(tempG.ExportToWkt());

              String out = "\"" + tempG.ExportToWkt() + "\"," + id;

              //System.out.println();
              bw.write(out);
              bw.newLine();


            }

            bw.close();
          }catch(IOException e){
          }

    

        ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
        ArrayList<Double> plotID1 = new ArrayList<Double>();
        //String tiedosto_coord = "plotsTEST30.csv";
        //String tiedosto_coord = "plotsTEST15.csv";
        String tiedosto_coord = "tempWKT.csv";
        String line1 = "";

          File tiedostoCoord = new File(tiedosto_coord);   
          tiedostoCoord.setReadable(true);
          
          try{
            BufferedReader sc = new BufferedReader( new FileReader(tiedostoCoord));
            sc.readLine();
            while((line1 = sc.readLine())!= null){
            
              //System.out.println(line1);

              String[] tokens =  line1.split(",");

              if(Double.parseDouble(tokens[tokens.length - 1]) != -999){

                plotID1.add(Double.parseDouble(tokens[tokens.length - 1]));

                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){
                  //System.out.println(tokens[i]);

                  if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                    plotID1.remove(plotID1.size() - 1);
                    breikki = true;
                    break;
                  }

                  tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                  tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                  //System.out.println(Arrays.toString(tempPoly[counteri]));
                  counteri++;
                }


                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                  polyBank1.add(tempPoly);
              }

            }

              
          }catch( IOException ioException ) {} 

          System.out.println(polyBank1.size());

        stats.setPolygon(polyBank1, plotID1);

        for(int o = 0; o < pointClouds.size(); o++){

            stats.setPointCloud(pointClouds.get(o));

            System.out.println(o + "/" + pointClouds.size());

            ArrayList<LasPoint> lista = new ArrayList<LasPoint>();

            LASReader reader = pointClouds.get(o);

            long n = reader.getNumberOfPointRecords();

            HashSet<Integer> tsekkaus = new HashSet<Integer>();

            HashMap<Short, Integer> bank = new HashMap<Short, Integer>();

            HashMap<Short, ArrayList<LasPoint>> pointBank = new HashMap<Short, ArrayList<LasPoint>>();

            for(long i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

                if(!bank.containsKey(tempPoint.pointSourceId))
                    bank.put(tempPoint.pointSourceId, 1);
                else
                    bank.put(tempPoint.pointSourceId, bank.get(tempPoint.pointSourceId) + 1);

            }

            for(long i = 0; i < n; i++){

                reader.readRecord(i, tempPoint);

                if(!pointBank.containsKey(tempPoint.pointSourceId)){
                    pointBank.put(tempPoint.pointSourceId, new ArrayList<LasPoint>());
                    pointBank.get(tempPoint.pointSourceId).add(new LasPoint(tempPoint));
                }
                else
                    pointBank.get(tempPoint.pointSourceId).add(new LasPoint(tempPoint));

                if(pointBank.get(tempPoint.pointSourceId).size() == bank.get(tempPoint.pointSourceId)){

                    stats.setData(pointBank.get(tempPoint.pointSourceId));
                    //stats.profile();
                    pointBank.get(tempPoint.pointSourceId).clear();

                }               

            }

        }
        stats.printOutput();
        /*
        for(long i = 0; i < n; i++){

            reader.readRecord(i, tempPoint);

            currentId = tempPoint.pointSourceId;

            if(i != 0 && currentId == previousId){

                lista.add(new LasPoint(tempPoint));

            }
            else if(lista.size() > 10){

                for(int j = 0; j < lista.size(); j++)
                    tsekkaus.add((int)lista.get(j).pointSourceId);

                System.out.println("unique: " + lista.size());
                stats.setData(lista);
                stats.profile();
                lista.clear();
                lista.add(new LasPoint(tempPoint));
            }

            previousId = currentId;
        }
        */

        stats.closeFile();

    }

}