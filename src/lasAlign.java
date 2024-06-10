import LASio.LASReader;
import err.toolException;
import java_cup.runtime.XMLElement;
import tools.groundMatch;
import tools.lasAligner;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;
import utils.tinManupulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class lasAlign{

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        tinManupulator tin = new tinManupulator(aR);

        //if(aR.cores > 1){
            //threadTool(aR, fD);
        //}else{

        try {
            aR.logFile = aR.createFile("alignerLog.log");
            lasAligner tooli = new lasAligner(aR);
            tooli.setTinManupulator(tin);
            tooli.setResolution(5);
            tooli.setMin_points_in_cell(5);
            tooli.readRefs();
            tooli.readTargets();

            tin.maxx = tooli.max_x;
            tin.maxy = tooli.max_y;
            //tin.maxz = tooli.max_z;
            tin.minx = tooli.min_x;
            tin.miny = tooli.min_y;
            //tin.minz = tooli.min_z;

            System.out.println("SUCCESS!");

            tooli.prepareData();

                tooli.processTargets();


                tin.removeOutliers();

                tin.writeTinToFile(tooli.targets.get(0).getFile(), 5);

                tooli.applyCorrection();


            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }catch (Error e){
                e.printStackTrace();
                System.exit(1);
            }



            System.out.println("FINISHED");
        //}

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
                    System.exit(2);
                }catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    System.exit(2);
                }catch (Error e) {
                    e.printStackTrace();
                    System.exit(2);
                }
            }
        }
    }
}


