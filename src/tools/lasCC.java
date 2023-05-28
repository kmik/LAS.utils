package tools;

import LASio.LASReader;
import LASio.LasPoint;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import utils.argumentReader;

public class lasCC {

    LASReader pointCloud;
    argumentReader aR;


    public lasCC(LASReader pointCloud, argumentReader aR){

        this.pointCloud = pointCloud;
        this.aR = aR;


    }

    public void canopy_cover_points() throws Exception{

        int points_canopy = 0;
        int points_below_canopy = 0;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        LasPoint tempPoint = new LasPoint();

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if (!aR.inclusionRule.ask(tempPoint, i + j, true)) {
                    continue;
                }

                if(tempPoint.numberOfReturns == tempPoint.returnNumber){

                    if(tempPoint.z > 5.0){
                        points_canopy++;
                    }else{
                        points_below_canopy++;
                    }

                }
            }
        }

        double cc = (double)points_canopy / (double)(points_below_canopy + points_canopy) * 100.0;
        System.out.println("Canopy cover: " + cc);

    }

    public void canopy_cover_chm(Dataset chm){

        int points_canopy = 0;
        int points_below_canopy = 0;

        int number_of_pix_x = chm.getRasterXSize();
        int number_of_pix_y = chm.getRasterYSize();

        Band band = chm.GetRasterBand(1);

        float[] floatArray = new float[number_of_pix_x];

        for(int y = 0; y < number_of_pix_y; y++){

            band.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for(int x = 0; x < number_of_pix_x; x++){

                float value = floatArray[x];

                if(Float.isNaN(value))
                    continue;

                if(value > 5.0f){
                    points_canopy++;

                }else
                    points_below_canopy++;
            }
        }


        double cc = (double)points_canopy / (double)(points_below_canopy + points_canopy) * 100.0;
        System.out.println("Canopy cover (chm)  : " + cc);


    }

}
