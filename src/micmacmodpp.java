import utils.MicMacOri;
import utils.argumentReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class micmacmodpp {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");

        MicMacOri mMOri = new MicMacOri(aR);

        mMOri.modifypp(aR.idir, 29.5, -0.5);

        aR.cleanup();

    }

}
