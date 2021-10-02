import java.io.IOException;

import tools.*;

public class lasStripTest {



    public static void main(String[] args) throws IOException {

        /*
        File lasFile1 = new File("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/project/uav_lidar/plot_10_uav_lidar_images/lidar/corr/gc/10_L2.las");

        File lasFile2 = new File("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/project/uav_lidar/plot_10_uav_lidar_images/lidar/corr/gc/10_L3.las");
        LASReader las1 = new LASReader(lasFile1);
        LASReader las2 = new LASReader(lasFile2);
        */
        lasStrip stip = new lasStrip();

        stip.align();

    }
}