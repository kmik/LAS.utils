package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import utils.KdTree;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class lasScaler {


    public double factor = 15.0;
    public double maxDistance = 1.5;

    public double minDistance = 1.5;

    public String method = "knn";

    public String neigborhood_statistic = "mean";

    public int k = 2;

    argumentReader aR;


    public lasScaler(argumentReader aR) {

        this.aR = aR;

    }

    public void setFactor(double factor) {
        this.factor = factor;
    }
    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public void setNeigborhoodStatistic(String neigborhood_statistic) {
        this.neigborhood_statistic = neigborhood_statistic;
    }

    public void setK(int k) {
        this.k = k;
    }

    public void setAargumentReader(argumentReader aR) {
        this.aR = aR;
    }

    public void scale(LASReader in, argumentReader aR) throws Exception {
        // Implement the scaling logic here

        KdTree kdTree = new KdTree();

        LasPoint tempPoint = new LasPoint();

        File outFile = aR.createOutputFile(in);


        int thread_n = aR.pfac.addReadThread(in);

        //aR.add_extra_bytes(6, "ITC_id", "ID for an ITC segment");

        //aR.add_extra_bytes(6, "z_order", "just a running id for points");
        //aR.add_extra_bytes(6, "point_id_2", "just a running id for points_2");
        //aR.add_extra_bytes(6, "tree_id", "ITC id");
        //aR.add_extra_bytes(4);
        //tempPoint.addExraByte(aR.create_extra_byte_vlr_n_bytes);
        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, in, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);


        int counter = 0;

        for(long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

            //for(long i = 0; i < in.getNumberOfPointRecords(); i++) {

            int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                in.readFromBuffer(tempPoint);
                KdTree.XYZPoint tempPoint_ = new KdTree.XYZPoint(tempPoint.x, tempPoint.y, tempPoint.z);
                tempPoint_.setIndex(j);
                tempPoint_.setExtra_value1(0);
                kdTree.add(tempPoint_);

            }
        }

        KdTree.XYZPoint nearest = new KdTree.XYZPoint(0, 0, 0);
        List<KdTree.XYZPoint> nearestPoints = null;

        for(int k = 0; k < this.factor; k++) {

            Iterator<KdTree.XYZPoint> iterator = kdTree.iterator();

            //for (long i = 0; i < in.getNumberOfPointRecords(); i += 20000) {

                //for(long i = 0; i < in.getNumberOfPointRecords(); i++) {

                //int maxi = (int) Math.min(20000, Math.abs(in.getNumberOfPointRecords() - i));

               // aR.pfac.prepareBuffer(thread_n, i, maxi);

                //for (int j = 0; j < maxi; j++) {
            while (iterator.hasNext()) {

                    KdTree.XYZPoint item = iterator.next();

                //System.out.println("Item: " + item.getX() + ", " + item.getY() + ", " + item.getZ() + ", index: " + item.getIndex());

                    in.readRecord((long)item.getIndex(), tempPoint);
                    //in.readFromBuffer(tempPoint);

                    nearest.setX(tempPoint.x);
                    nearest.setY(tempPoint.y);
                    nearest.setZ(tempPoint.z);

                    nearestPoints = (List<KdTree.XYZPoint>) kdTree.nearestNeighbourSearch(k + 10, nearest);

                    //System.out.println("Nearest points: " + nearestPoints.size() + " for point: " + tempPoint.x + ", " + tempPoint.y + ", " + tempPoint.z);

                    double newPointX = tempPoint.x;
                    double newPointY = tempPoint.y;
                    double newPointZ = tempPoint.z;

                    double count_points = 1;

                    for (int p = 1; p < nearestPoints.size(); p++) {

                        KdTree.XYZPoint point = nearestPoints.get(p);

                        //System.out.println("distance: " + point.euclideanDistance(nearest));

                        if(point.euclideanDistance(nearest) < this.minDistance || (int)point.getExtra_value1() == (k + 1) ) {
                            continue;
                        }

                        newPointX += point.getX();
                        newPointY += point.getY();
                        newPointZ += point.getZ();
                        count_points++;

                        if(count_points == this.k)
                            break;
                        //System.out.println("Point: " + point.getX() + ", " + point.getY() + ", " + point.getZ());
                    }

                    if(count_points < this.k) {
                        continue;
                    }

                    newPointX /= count_points;
                    newPointY /= count_points;
                    newPointZ /= count_points;

                    //if(k < this.factor - 1) {

                        KdTree.XYZPoint newPoint = new KdTree.XYZPoint(newPointX, newPointY, newPointZ);
                        newPoint.setIndex(item.getIndex());
                        newPoint.setExtra_value1(k + 1);

                        double distanceToOriginal = newPoint.euclideanDistance(nearest);

                        if(distanceToOriginal < 0.5) {

                            //System.out.println("Distance to original: " + distanceToOriginal + ", maxDistance: " + this.maxDistance);

                        }
                        kdTree.add(newPoint);

                    //}

                    //System.out.println("New point: " + newPointX + ", " + newPointY + ", " + newPointZ);
                }

        }

        Iterator<KdTree.XYZPoint> iterator = kdTree.iterator();
        int counter2 = 0;

        int countWritten = 0;
        while (iterator.hasNext()) {

            KdTree.XYZPoint item = iterator.next();

            //System.out.println("Item: " + item.getX() + ", " + item.getY() + ", " + item.getZ() + ", index: " + item.getIndex());

            in.readRecord((long)item.getIndex(), tempPoint);

            tempPoint.x = item.getX();
            tempPoint.y = item.getY();
            tempPoint.z = item.getZ();

            tempPoint.classification = ((int)item.getExtra_value1());

            //System.out.println(tempPoint.classification);
            try {

                aR.pfac.writePoint(tempPoint, counter2++, thread_n);
                countWritten++;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }

            // do something with item
        }

        kdTree = null;


        System.out.println(countWritten + " " + in.getNumberOfPointRecords());

        aR.pfac.closeThread(thread_n);


        // This is a placeholder for the actual scaling logic
        System.out.println("Scaling with factor: " + factor);
        System.out.println("Max distance: " + maxDistance);
        System.out.println("Method: " + method);
        System.out.println("Neighborhood statistic: " + neigborhood_statistic);
        System.out.println("K: " + this.k);
    }

    public void releaseMemory() {
        // Implement memory release logic here

    }
}
