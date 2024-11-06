import LASio.LASReader;
import com.drew.metadata.Directory;
import org.gdal.ogr.ogr;
import tools.createCHM;
import utils.argumentReader;
import utils.fileDistributor;
import utils.gdalRaster;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class SyncALSRasters {

    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        long tStart = System.currentTimeMillis();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2dsm");
        //fileDistributor fD = new fileDistributor(aR.inputFiles);

        File inputDirectory = new File(aR.idir);

        ArrayList<File> directoriesToProcess = new ArrayList<File>();

        if(inputDirectory.isDirectory()){
            for(File f : inputDirectory.listFiles()){
                if(f.isDirectory()){
                    directoriesToProcess.add(f);
                }
            }
        }else{
            throw new IOException("Input directory is not a directory.");
        }

        if(aR.cores > 1){
            //threadTool(aR, fD);
        }else{

            for (int i = 0; i < directoriesToProcess.size(); i++) {

                gdalRaster oldestRaster = new gdalRaster(directoriesToProcess.get(i) + aR.pathSep + "oldest/surface.tif");
                gdalRaster newestRaster = new gdalRaster(directoriesToProcess.get(i) + aR.pathSep + "newest/surface.tif");



                File outputDirectoryOldest = new File(directoriesToProcess.get(i) + aR.pathSep + "oldest" + aR.pathSep + "threshold_" + aR.step + aR.pathSep);
                File outputDirectoryNewest = new File(directoriesToProcess.get(i) + aR.pathSep + "newest" + aR.pathSep + "threshold_" + aR.step + aR.pathSep);

                if(!outputDirectoryOldest.exists()){
                    outputDirectoryOldest.mkdirs();
                }

                oldestRaster.open();


                //oldestRaster.copyRasterToFile(outputDirectoryOldest + aR.pathSep + "surface.tif");

                oldestRaster.syncWithAnotherChm(newestRaster, (float)aR.step, outputDirectoryOldest + aR.pathSep + "surface.tif");

                gdalRaster syncedRaster = new gdalRaster(outputDirectoryOldest + aR.pathSep + "surface.tif");
                syncedRaster.open();
                syncedRaster.toTxt();

                System.out.println("Processing: " + directoriesToProcess.get(i).getName());
                //System.out.println(Arrays.toString(oldestRaster.rasterExtent));
                //System.out.println(Arrays.toString(newestRaster.rasterExtent));
                //System.exit(1);

                oldestRaster.close();
                oldestRaster = null;

                newestRaster.close();
                newestRaster = null;

                syncedRaster.close();
                syncedRaster = null;


            }
        }
        aR.cleanup();
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

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                LASReader temp = null;
                try {
                    temp = new LASReader(f);
                    createCHM testi_c = new createCHM();
                    createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, nCore);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
