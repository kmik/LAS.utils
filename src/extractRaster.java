import LASio.LASReader;
import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;
import tools.lasRasterTools;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;
import utils.lasClipMetricOfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class extractRaster {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);


        if(aR.inputFiles.get(0).getName().contains(".tif")){
            extractFromRaster(aR);
            System.exit(1);
        }

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{

            process_las2las tooli = new process_las2las(1);


            for (int i = 0; i < inputFiles.size(); i++) {
                LASReader temp = new LASReader(aR.inputFiles.get(i));

                try {

                    tooli.convert(temp, aR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    public static void extractFromRaster(argumentReader aR){

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        lasRasterTools lRT = new lasRasterTools();

        // start time
        long startTime = System.currentTimeMillis();

        try {
            lRT.zonalStatistics_exportSurface(aR, new lasClipMetricOfile(aR));
        }catch (Exception e){
            e.printStackTrace();
        }

        // end time
        long endTime = System.currentTimeMillis();

        // total time in hours, minutes, seconds
        long totalTime = endTime - startTime;
        long totalTimeSec = totalTime / 1000;
        long totalTimeMin = totalTimeSec / 60;
        long totalTimeHour = totalTimeMin / 60;
        totalTimeSec = totalTimeSec % 60;
        totalTimeMin = totalTimeMin % 60;

        System.out.println("Total time: " + totalTimeHour + " hours " + totalTimeMin + " minutes " + totalTimeSec + " seconds");

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
                LASReader temp = null;
                try {
                    temp = new LASReader(f);
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    tooli.convert(temp, aR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
