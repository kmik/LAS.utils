import LASio.LASReader;
import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import org.nd4j.common.loader.FileBatch;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static utils.miscProcessing.prepareData;

public class test_stanford {


    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        KarttaLehtiJako kj = new KarttaLehtiJako();

        try {
            kj.readFromFile(new File(""));

        }
        catch (Exception e){
            e.printStackTrace();
        }



        //System.exit(1);

        Stanford2010 stanford = new Stanford2010(aR);
        stanford.readExcludedStandsFromFile(aR.exclude);
        stanford.declareLogFile();
        stanford.readShapeFiles(aR.poly);

        rasterCollection rasters = new rasterCollection(aR.cores);

        if(aR.ref.size() > 0){

            for(int i = 0; i < aR.ref.size(); i++){

                rasters.addRaster(new gdalRaster(aR.ref.get(i).getAbsolutePath()));

            }
        }

        System.out.println("number of rasters: " + rasters.numRasters() + " " + rasters.numRasterExtents());
        stanford.setRasters(rasters);

        //System.exit(1);


        HashMap<Integer, ArrayList<File>> files = stanford.getOverlappingHPRFiles(inputFiles);

        stanford.createProcessingBatches(files);
        stanford.parseBatches();

        if(false)
        for (int i = 0; i < inputFiles.size(); i++) {

            //stanford.setXMLfile(new File("/home/koomikko/Documents/customer_work/metsahallitus/example.hpr"));
            stanford.setXMLfile((inputFiles.get(i)));

            stanford.parseBatches();

            //System.exit(1);
            try {

                //tooli.convert(temp, aR);
            } catch (Exception e) {
                e.printStackTrace();
            }

            rasters.printCurrentSelectionFileNames();
            System.gc();
            System.gc();
            System.gc();
            System.gc();
            System.gc();

        }



        stanford.printFailedFiles();
        stanford.finalizeMergedShapefile();
        stanford.closeLogFile();
        stanford.mergeTreeFiles();

        System.out.println("SUCCESS!");
    }
}
