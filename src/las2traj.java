import LASio.LASReader;
import tools.process_las2las;
import tools.trajectoryEstimator;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class las2traj {


    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.cores > 1){
            //threadTool(aR, fD);
        }else{

            process_las2las tooli = new process_las2las(1);


            for (int i = 0; i < inputFiles.size(); i++) {

                LASReader temp = new LASReader(aR.inputFiles.get(i));

                trajectoryEstimator te = new trajectoryEstimator(temp, aR);



            }
        }

    }


}
