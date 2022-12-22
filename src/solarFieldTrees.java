import LASio.LASReader;
import tools.solar2trees;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class solarFieldTrees {

    public static void main(String[] args) throws Exception {


        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "solar_trees");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        solar2trees s2t = new solar2trees(aR, 1);

        s2t.readMeasuredTrees(aR.measured_trees);
        //s2t.readMeasuredTrees_2(aR.measured_trees_2);

        for (int i = 0; i < inputFiles.size(); i++) {

            LASReader temp = new LASReader(aR.inputFiles.get(i));

            s2t.processPointCloud(temp);
        }

        s2t.closeOutputStream();
    }

}
