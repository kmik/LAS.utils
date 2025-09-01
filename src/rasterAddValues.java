import LASio.LASReader;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.ogr;
import tools.lasRasterTools;
import tools.process_las2las;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import static utils.miscProcessing.printProcessingTime;

public class rasterAddValues {

    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        int numAddedBands = 1;

        if(aR.cores < 0){

        }else{
            rasterCollection auxRasters = new rasterCollection(1);

            rasterCollection rasters = new rasterCollection(1);

            for (int i = 0; i < inputFiles.size(); i++) {

                try {

                    Dataset raster = gdal.Open(inputFiles.get(i).getAbsolutePath(), gdalconst.GA_Update);

                    rasters.addRaster(new gdalRaster(raster.GetDescription(), i, gdalconst.GA_Update));

                    raster.delete();

                    System.out.println("Raster " + (i + 1) + " of " + inputFiles.size() + " loaded.");


                }catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                catch (Error e) {
                    e.printStackTrace();
                    System.exit(1);
                }



            }

            System.out.println(aR.aux_files.size() + " auxiliary rasters found.");

            // Print aux files

            for (File auxFile : aR.aux_files) {
                System.out.println("Auxiliary raster: " + auxFile.getAbsolutePath());
            }

            for (int i = 0; i < aR.aux_files.size(); i++) {

                try {

                    String full = aR.aux_files.get(i).getName();
                    String firstPart = full.split("_")[0];

                    System.out.println("Loading auxiliary raster: " + aR.aux_files.get(i).getAbsolutePath() + " with first part: " + firstPart);
                    Dataset raster = gdal.Open(aR.aux_files.get(i).getAbsolutePath(), gdalconst.GA_Update);

                    System.out.println("Raster " + (i + 1) + " of " + aR.aux_files.size() + " loaded.");
                    auxRasters.addRaster(new gdalRaster(raster.GetDescription(), i, gdalconst.GA_Update), firstPart);

                    raster.delete();

                }catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                catch (Error e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            if( aR.dtm != null){
                Dataset raster = gdal.Open(aR.dtm.getAbsolutePath(), gdalconst.GA_Update);

                //System.out.println("Raster " + (i + 1) + " of " + aR.aux_files.size() + " loaded.");
                auxRasters.addRaster(new gdalRaster(raster.GetDescription(), auxRasters.rasters.size(), gdalconst.GA_Update), "dtm");
            }

            for(int i = 0; i < rasters.rasters.size(); i++){

                gdalRaster raster = rasters.rasters.get(i);
                raster.open();
                ArrayList<Float> list = new ArrayList<>(Arrays.asList(5f));

                raster.addBands(1,list);

            }


            File nfiPlots = aR.measured_trees;
            KdTree kdtree = new KdTree();

            if(nfiPlots != null) {
                System.out.println("Adding NFI plots to rasters.");

                // Read nfiPlots line by line and add to kdtree

                BufferedReader br = new BufferedReader(new java.io.FileReader(nfiPlots));


                String line;

                HashMap<String, Integer> colnametoIndex = new HashMap<>();
                boolean colnamesRead = false;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\s");

                    if (!colnamesRead) {
                        for (int i = 0; i < parts.length; i++) {
                            String cleaned = parts[i].trim().replaceAll("^\"|\"$", ""); // removes surrounding quotes
                            colnametoIndex.put(cleaned, i);
                        }

                        //System.out.println("Parsed header columns with lengths:");
                        for (String key : colnametoIndex.keySet()) {
                        //    System.out.println(">> '" + key + "' (length " + key.length() + ")");
                        }
                        //System.out.println("Trying to get index for: 'x12'");

                        // Print colnames
                        //System.out.println("Column names: " + colnametoIndex.keySet());
                        //System.out.println(colnametoIndex.get("x12"));
                        //System.exit(1);
                        colnamesRead = true;
                        continue;
                    }
                    if (parts.length < 3) {
                        System.out.println("Skipping line: " + line);
                        continue;
                    }
                    try {
                        //System.out.println(line + " " + parts.length);
                        double x = Double.parseDouble(parts[colnametoIndex.get("x12")]);
                        double y = Double.parseDouble(parts[colnametoIndex.get("y12")]);
                        double value = Double.parseDouble(parts[colnametoIndex.get("ls_12")]);

                        KdTree.XYZPoint point = new KdTree.XYZPoint(x, y, value);
                        kdtree.add(point);

                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format in line: " + line);
                    }
                }
            }

            modValuesForCloudProject(rasters, auxRasters, kdtree);

        }



        aR.cleanup();
        printProcessingTime();

    }

    public static void modValuesForCloudProject(rasterCollection raster, rasterCollection auxRasters, KdTree nfiplots){

        System.out.println("here!!! " + raster.rasters.size());
        KdTree.XYZPoint point = new KdTree.XYZPoint(0, 0, 0);

        for(int i = 0; i < raster.rasters.size(); i++){

            gdalRaster ras = raster.rasters.get(i);

            ArrayList<Float> valuesToBeAdded = new ArrayList<>();

            double rasterX = (ras.rasterExtent()[0] + ras.rasterExtent()[2]) / 2.0;
            double rasterY = (ras.rasterExtent()[1] + ras.rasterExtent()[3]) / 2.0;

            //System.out.println(Arrays.toString(ras.rasterExtent));
            //System.exit(1);

            point.setX(rasterX);
            point.setY(rasterY);
            point.setZ(0);

            ArrayList<KdTree.XYZPoint> nearestPoints = (ArrayList<KdTree.XYZPoint>)nfiplots.nearestNeighbourSearch2d(3, point);

            double averageValue = 0;

            for(KdTree.XYZPoint p : nearestPoints){
                averageValue += p.getZ();
            }

            averageValue /= nearestPoints.size();

            gdalRaster dtm = auxRasters.getRasterByName("dtm");

            double[] coordinatesInPixelSpaceDtm = dtm.realWorldCoordinateToPixelCoordinate(rasterX, rasterY);

            float dtmValue = dtm.readValue((int)coordinatesInPixelSpaceDtm[0], (int)coordinatesInPixelSpaceDtm[1]);

            //System.out.println(Arrays.toString(coordinatesInPixelSpaceDtm) + " for raster " + (i + 1) + " of " + raster.rasters.size() + " at coordinates: " + rasterX + ", " + rasterY);
            //System.out.println(dtmValue + " for raster " + (i + 1) + " of " + raster.rasters.size() + " at coordinates: " + rasterX + ", " + rasterY);
            //System.out.println("Average value for raster " + (i + 1) + " of " + raster.rasters.size() + ": " + averageValue);

            // d.d.
            valuesToBeAdded.add((float)averageValue);

            valuesToBeAdded.add((float)dtmValue);

            // THIS IS GROWTH INDEX
            //valuesToBeAdded.add(348.924f * 1.5f);
            //valuesToBeAdded.add(1046.772f);

            // THIS IS "LEAF"
            valuesToBeAdded.add(2f);

            // This is deltaGS
            valuesToBeAdded.add(3.040781647f * 1.0f);
            //valuesToBeAdded.add(6);

            gdalRaster maaluokka = auxRasters.getRasterByName("maaluokka");
            double[] coordinatesInPixelSpacemaaluokka = maaluokka.realWorldCoordinateToPixelCoordinate(rasterX, rasterY);


            gdalRaster paatyyppi = auxRasters.getRasterByName("paatyyppi");
            double[] coordinatesInPixelSpacepaatyyppi = paatyyppi.realWorldCoordinateToPixelCoordinate(rasterX, rasterY);

            gdalRaster kasvupaikka = auxRasters.getRasterByName("kasvupaikka");
            double[] coordinatesInPixelSpacekasvupaikka = kasvupaikka.realWorldCoordinateToPixelCoordinate(rasterX, rasterY);

            float maaluokkaValue = maaluokka.readValue((int)coordinatesInPixelSpacemaaluokka[0], (int)coordinatesInPixelSpacemaaluokka[1]);
            float paatyyppiValue = paatyyppi.readValue((int)coordinatesInPixelSpacepaatyyppi[0], (int)coordinatesInPixelSpacepaatyyppi[1]);
            float kasvupaikkaValue = kasvupaikka.readValue((int)coordinatesInPixelSpacekasvupaikka[0], (int)coordinatesInPixelSpacekasvupaikka[1]);

            if(paatyyppiValue <= 2){
                paatyyppiValue = 1;
            }else if(paatyyppiValue == 3){
                paatyyppiValue = 2;
            }else if(paatyyppiValue == 4){
                paatyyppiValue = 3;
            }else{
                paatyyppiValue = 4;
            }

            kasvupaikkaValue = Math.min((int)kasvupaikkaValue, 5);

            if(kasvupaikkaValue <= 2){
                kasvupaikkaValue = 1;
            }else{
                //kasvupaikkaValue = kasvupaikkaValue - 1;
            }

            if(maaluokkaValue > 2)
                maaluokkaValue = 2;


            valuesToBeAdded.add(maaluokkaValue);
            valuesToBeAdded.add(paatyyppiValue);
            valuesToBeAdded.add(kasvupaikkaValue);


            ras.addBands(valuesToBeAdded.size(), valuesToBeAdded);
            ras.flush();
            ras.close();

        }

    }

    private static void threadTool(argumentReader aR, fileDistributor fD) {

        proge.setEnd(aR.inputFiles.size());

        if (aR.cores > aR.inputFiles.size())
            aR.cores = aR.inputFiles.size();

        ArrayList<Thread> threadList = new ArrayList<Thread>();

        for (int ii = 1; ii <= aR.cores; ii++) {

            proge.addThread();
            Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
            threadList.add(temp);
            temp.start();

        }

        for (int i = 0; i < threadList.size(); i++) {

            try {
                threadList.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Just a class to divide workers for multithreaded tools.
     */
    static class multiThreadTool implements Runnable {

        argumentReader aR;
        int nCores;
        int nCore;
        fileDistributor fD;

        public multiThreadTool(argumentReader aR, int nCores, int nCore, fileDistributor fD) {

            this.aR = aR;
            this.nCores = nCores;
            this.nCore = nCore;
            this.fD = fD;

        }

        public void run() {

            process_las2las tooli = new process_las2las(nCore);

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                lasRasterTools tool = new lasRasterTools(aR);

                try {

                    if(aR.metadatafile != null)
                        tool.readMetadata(aR.metadatafile);

                    LASReader temp = new LASReader(f);
                    tool.rasterize(temp, aR.res);

                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(2);
                }
                catch (Error e) {
                    e.printStackTrace();
                    System.exit(2);
                }
            }
        }
    }
}
