import LASio.LASReader;
import tools.ToShp;
import tools.stemDetector;
import tools.stemDetector_filtered;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class las2trunk {


    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "stemDetector");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{
            for (int i = 0; i < inputFiles.size(); i++) {
                LASReader temp = new LASReader(aR.inputFiles.get(i));
                try {
                    stemDetector_filtered sd = new stemDetector_filtered(temp, 0.1, 0.5, 1, aR);
                    sd.setUpOutputFiles(temp);
                    sd.detect(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                LASReader temp = null;
                try {
                    temp = new LASReader(f);
                    stemDetector sd = new stemDetector(temp, 0.1, 0.5, nCore, aR);
                    sd.setUpOutputFiles(temp);
                    sd.detect(false);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
