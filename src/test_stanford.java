import LASio.LASReader;
import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

        rasterCollection rasters = new rasterCollection();

        if(aR.ref.size() > 0){

            for(int i = 0; i < aR.ref.size(); i++){

                rasters.addRaster(new gdalRaster(aR.ref.get(i).getAbsolutePath()));

            }
        }

        System.out.println("number of rasters: " + rasters.numRasters() + " " + rasters.numRasterExtents());
        stanford.setRasters(rasters);

        //System.exit(1);

        for (int i = 0; i < inputFiles.size(); i++) {

            //stanford.setXMLfile(new File("/home/koomikko/Documents/customer_work/metsahallitus/example.hpr"));
            stanford.setXMLfile((inputFiles.get(i)));

            stanford.parse();

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