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

        double pp_x_offset = aR.pp_x_offset;
        double pp_y_offset = aR.pp_y_offset;

        mMOri.modifypp(aR.idir, pp_x_offset, pp_y_offset);

        aR.cleanup();

    }

}
