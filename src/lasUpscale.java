import LASio.LASReader;
import tools.lasScaler;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class lasUpscale {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            threadTool(aR, fD);
        }else{

            //process_las2las tooli = new process_las2las(1);


            for (int i = 0; i < inputFiles.size(); i++) {

                LASReader temp = new LASReader(aR.inputFiles.get(i));


                lasScaler tooli = new lasScaler(aR);

                tooli.setFactor(4);
                tooli.setMaxDistance(1.5);
                tooli.setMinDistance(0.75);
                tooli.setMethod("knn");
                tooli.setNeigborhoodStatistic("mean");
                tooli.k = 3;

                try {
                    tooli.scale(temp, aR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                temp.releaseMemory();
                temp = null;
                //System.out.println("DBSCAN COMPLETE! " + results.length);
                if( i % 100 == 0){
                    System.gc();
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
