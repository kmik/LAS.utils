import LASio.LASReader;
import tools.heatMap;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class treeTopHeatMap {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){

        }else{


            for (int i = 0; i < inputFiles.size(); i++) {

                //LASReader temp = new LASReader(aR.inputFiles.get(i));



                try {

                    heatMap hm = new heatMap(aR);

                    hm.readMeasuredTrees(aR.measured_trees);
                    hm.readFieldPlots(new File(aR.poly));
                    hm.createHeatMap(25, 5, 1, 35);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        aR.cleanup();

    }
}
