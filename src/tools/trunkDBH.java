package tools;

import LASio.LASReader;
import utils.argumentReader;

public class trunkDBH {

    LASReader pointCloud;
    argumentReader aR;

    public trunkDBH(LASReader temp, argumentReader aR){

        this.pointCloud = temp;
        this.aR = aR;

    }
}
