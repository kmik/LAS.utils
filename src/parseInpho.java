import LASio.LASReader;
import err.toolException;
import tools.process_las2las;
import utils.argumentReader;
import utils.fileDistributor;
import utils.inpho;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class parseInpho {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if(aR.path == null)
            throw new toolException("No path specified");

        for(int i = 0; i < aR.inputFiles.size(); i++){

            inpho file_ = new inpho(aR);


            file_.setInphoFile(aR.inputFiles.get(i));

            file_.parseInphoFile();
            file_.writePhotosAndCameras();

            //System.exit(1);
            //System.exit(1);

        }

        aR.cleanup();

    }


}
