import LASio.LASReader;
import org.gdal.ogr.ogr;
import tools.lasRasterTools;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import static utils.miscProcessing.printProcessingTime;

public class las2raster {

    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{

            for (int i = 0; i < inputFiles.size(); i++) {

                lasRasterTools tool = new lasRasterTools(aR);

                if(aR.metadatafile != null)
                    tool.readMetadata(aR.metadatafile);

                LASReader temp = new LASReader(aR.inputFiles.get(i));

                tool.rasterize(temp, aR.res);

            }

        }

        aR.cleanup();
        printProcessingTime();

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
                }
            }
        }
    }
}
