import utils.argumentReader;
import utils.fileDistributor;
import utils.inpho;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class inpho2micmac {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        //if(aR.path == null)
        //    throw new toolException("No path specified");

        for(int i = 0; i < aR.inputFiles.size(); i++){

            inpho file_ = new inpho(aR);


            file_.setInphoFile(aR.inputFiles.get(i));

            if(aR.include != null)
                file_.setIncludedImages(aR.include);

            file_.findControlPoints();
            file_.findPhotos();
            file_.writeMicMacControlPointFiles(aR.odir);

            //System.exit(1);
            //System.exit(1);

        }

        aR.cleanup();

    }


}
