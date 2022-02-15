import LASio.LASReader;
import tools.ToShp;
import utils.argumentReader;
import utils.fileDistributor;
import utils.gz_tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import static utils.miscProcessing.printProcessingTime;

public class janne_gz {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);

        aR.noLas = true;

        ArrayList<File> inputFiles = prepareData(aR, "gz");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{

            gz_tools gz = new gz_tools(aR);

            for (int i = 0; i < inputFiles.size(); i++) {
                try {
                    gz.process(aR.inputFiles.get(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

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

            gz_tools gz = new gz_tools(aR);

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                try {
                    gz.process(f);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
