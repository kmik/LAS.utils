import utils.MicMacOri;
import utils.argumentReader;
import utils.fileDistributor;
import utils.parseBundler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class makeMicMacOri {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");

        MicMacOri mMOri = new MicMacOri(aR);

        mMOri.setOrientationFile(aR.exterior);
        mMOri.parseOrientationFile();
        mMOri.writeOrientationFile(aR.output);

        aR.cleanup();

    }

}
