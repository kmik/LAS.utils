package tools;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LASwrite;
import LASio.LasPoint;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import utils.argumentReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class lasLayer {

    double binThresHold_d = 0.00;
    double binThresHold_n = 20;

    argumentReader aR;
    LASReader pointCloud;
    int coreNumber;
    double resolution;

    double theta = 1.0;
    double thetaSqrt;

    double[] gaussKernel;

    PolynomialSplineFunction funk;
    PolynomialSplineFunction funk_first_derivative;
    PolynomialSplineFunction funk_second_derivative;

    PolynomialFunction funk_poly;
    PolynomialSplineFunction funk_s_poly;
    PolynomialFunction funk_first_derivative_poly;
    PolynomialFunction funk_second_derivative_poly;

    //PolynomialFunction funk_poly;

    double delta;

    double minCanopyLength = 1.5;

    int kernelSize;
    SplineInterpolator interpolatori = new SplineInterpolator();

    double searchRadius = 5.0;

    double minz = 0.75;

    double[] x_segments;

    public lasLayer(LASReader pointCloud, argumentReader aR, int coreNumber){

        this.pointCloud = pointCloud;
        this.aR = aR;
        this.coreNumber = coreNumber;

        this.resolution = 2.0;

        this.resolution = aR.dist;



        if(aR.theta == 1.0)
            this.theta = 45;
        else
            this.theta = aR.theta;


        if(aR.delta == 5.0)
            this.delta = 1.0;
        else
            this.delta = aR.delta;

        System.out.println("ker: " + aR.kernel);

        if(aR.kernel == 3) {
            int plus = (int) (7.0 / this.delta) % 2 == 0 ? 1 : 0;
            this.kernelSize = (int) (7.0 / this.delta) + plus;
        }
        else
            this.kernelSize = aR.kernel;

        //this.kernelSize = aR.kernel;

        this.binThresHold_n = (int) (1.0 / this.delta);

        this.thetaSqrt = this.theta * this.theta;

        this.searchRadius = aR.dist * 3.0;

        System.out.println("theta: " + this.theta + " " + delta + " " + kernelSize);

    }

    public void layer() throws IOException {

        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();
        double minZ = pointCloud.getMinZ();
        double maxZ = pointCloud.getMaxZ();

        double numberOfPixelsX = (int) Math.ceil((maxX - minX) / resolution) + 1;
        double numberOfPixelsY = (int) Math.ceil((maxY - minY) / resolution) + 1;
        double numberOfPixelsZ = (int) Math.ceil((maxZ - minZ) / delta) + 1;

        float[][] understoreyHeight = new float[(int)numberOfPixelsX][(int)numberOfPixelsY];

        System.out.println("resolution: " + resolution + " kernelSize: " + this.kernelSize);

        gaussKernel = Kernel1D(this.kernelSize);

        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

        int maxi = 0;

        aR.p_update.threadFile[coreNumber - 1] = "initial pass";

        aR.p_update.threadEnd[coreNumber - 1] = (int) pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber - 1] = 0;

        //pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        int x_index;
        int y_index;

        int pienin;
        int suurin;

        double centerX;
        double centerY;


        File test = new File("testi.txt");

        if(test.exists())
            test.delete();

        test.createNewFile();

        String str = "Hello";
        BufferedWriter writer = new BufferedWriter(new FileWriter("testi.txt"));

        int counter = 0;
/*
        for(int x = 0; x < numberOfPixelsX; x++){
            for(int y = 0; y < numberOfPixelsY; y++) {

                double highestZ = Double.NEGATIVE_INFINITY;
                ArrayList<double[]> gridPoints = new ArrayList<>();

                centerX = minX + resolution * x + resolution / 2.0;
                centerY = minY + resolution * y + resolution / 2.0;

                pointCloud.querySquare(centerX - searchRadius, centerX + searchRadius, centerY - searchRadius, centerY + searchRadius);

                if (pointCloud.queriedIndexes2.size() > 0){

                    for (int u = 0; u < pointCloud.queriedIndexes2.size(); u++) {

                        //System.out.println(pointCloud.queriedIndexes2.get(u).length + " " + pointCloud.queriedIndexes2.size());

                        long n1 = pointCloud.queriedIndexes2.get(u)[1] - pointCloud.queriedIndexes2.get(u)[0];
                        long n2 = pointCloud.queriedIndexes2.get(u)[1];

                        int parts = (int) Math.ceil((double) n1 / 20000.0);
                        int jako = (int) Math.ceil((double) n1 / (double) parts);

                        int ero;

                        for (int c = 1; c <= parts; c++) {

                            if (c != parts) {

                                pienin = (c - 1) * jako;
                                suurin = c * jako;

                            } else {

                                pienin = (c - 1) * jako;
                                suurin = (int) n1;

                            }

                            pienin = pienin + pointCloud.queriedIndexes2.get(u)[0];
                            suurin = suurin + pointCloud.queriedIndexes2.get(u)[0];

                            ero = suurin - pienin;


                            try {
                                pointCloud.readRecord_noRAF(pienin, tempPoint, ero);
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            for (int p = pienin; p < suurin; p++) {

                                pointCloud.readFromBuffer(tempPoint);

                                if(euclideanDistance(centerX, centerY, tempPoint.x, tempPoint.y) < searchRadius && tempPoint.z > 1.3){
                                    gridPoints.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});

                                    if(tempPoint.z > highestZ){
                                        highestZ = tempPoint.z;
                                    }

                                }
                            }

                        }
                    }
                }else{
                    continue;
                }

                if(highestZ == Double.NEGATIVE_INFINITY || highestZ == 0.0)
                    continue;

                int bins = (int)Math.ceil(highestZ / delta) + 1;

                if(bins <= gaussKernel.length)
                    continue;

                double[] yAxis = new double[bins];
                double[] yAxis_filtered = new double[bins];
                double[] xAxis = new double[bins];

                double incr = delta;

                for(int i = 0; i < xAxis.length; i++){

                    xAxis[i] = incr;

                    incr += delta;
                }

                for(int i = 0; i < gridPoints.size(); i++){

                    yAxis[(int)(gridPoints.get(i)[2] / delta)]++;

                }



                //System.out.println(Arrays.toString(yAxis));
                yAxis_filtered = filter(yAxis);

                //System.out.println(Arrays.toString(yAxis));
                //System.out.println("*************");

                funk = interpolatori.interpolate(xAxis, yAxis_filtered);
                funk_first_derivative = funk.polynomialSplineDerivative();
                funk_second_derivative = funk_first_derivative.polynomialSplineDerivative();


                double[] yDerivatives = new double[bins];

                for(int i = 0; i < xAxis.length; i++){
                    //System.out.println(i + " " + funk_second_derivative.value(xAxis[i]));
                    yDerivatives[i] = funk_second_derivative.value(xAxis[i]);
                }

                yDerivatives = filter(yDerivatives);

                if(x == 5 && y == 5){

                    for(int i = 0 ; i < xAxis.length; i++){

                        writer.write("" + yAxis_filtered[i] + "\t" + yAxis[i] + "\t" + yDerivatives[i] + "\n");
                    }
                }

                System.out.println("iter: " + counter++ + " / " + (numberOfPixelsX*numberOfPixelsY));
            }
        }
*/


        int bins = (int)numberOfPixelsZ;

        //double pointDensity = 1 / Math.sqrt((maxX - minX) * (maxY - minY) / (double)pointCloud.getNumberOfPointRecords());

        //System.out.println("point density: " + pointDensity);

        int maxCanopies = 0;

        int[][][] layers = new int[(int)numberOfPixelsX][(int)numberOfPixelsY][(int)numberOfPixelsZ];
        int[][] averagePoints = new int[(int)numberOfPixelsX][(int)numberOfPixelsY];
        int[][][] canopy = new int[(int)numberOfPixelsX][(int)numberOfPixelsY][(int)numberOfPixelsZ];

        for(int x = 0; x < numberOfPixelsX; x++)
            for(int y = 0; y < numberOfPixelsY; y++) {

                understoreyHeight[x][y] = 0f;

                for (int z = 0; z < numberOfPixelsZ; z++)
                    canopy[x][y][z] = -99;
            }

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();//pointCloud.braf.buffer.position(0);
            }

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                pointCloud.readFromBuffer(tempPoint);

                if (tempPoint.z > minz) {
                    int xCoord = (int) ((tempPoint.x - minX) / resolution);
                    int yCoord = (int) ((maxY - tempPoint.y) / resolution);
                    int zCoord = (int) ((tempPoint.z - minZ) / delta);
                    //System.out.println(xCoord + " " + yCoord + " " + zCoord);
                    if(xCoord < numberOfPixelsX && yCoord < numberOfPixelsY && zCoord < numberOfPixelsZ)

                        layers[xCoord][yCoord][zCoord]++;
                }
            }
        }

        boolean printed = false;

        int nPoints = 0;

        int kernelSize_search = 1;

        int counteri = 0;

        int count = 0;

        for(int x = 0; x < layers.length; x++) {
            for (int y = 0; y < layers[0].length; y++) {

                for(int z = 0; z < layers[0][0].length; z++){

                    if(layers[x][y][z] > 0) {
                        averagePoints[x][y] += layers[x][y][z];
                    }
                }
            }
        }

        double average = 0.0;
        double count_average = 0;

        for(int x = 0; x < layers.length; x++) {
            for (int y = 0; y < layers[0].length; y++) {
                if(averagePoints[x][y] > 0){
                    average += averagePoints[x][y];
                    count_average++;
                }
            }
        }

        average /= count_average;

        System.out.println("Average points: " + average);

        for(int x = 0; x < layers.length; x++){
            for(int y = 0; y < layers[0].length; y++){


                nPoints = 0;

                double midPointX = 0.0;
                double midPointY = 0.0;

                int[] xLim = new int[]{Math.max(0,x-kernelSize_search), Math.min(layers.length-1, x+kernelSize_search)};
                int[] yLim = new int[]{Math.max(0,y-kernelSize_search), Math.min(layers[0].length-1, y+kernelSize_search)};


                int dimx = (xLim[1]-xLim[0]) * (yLim[1]-yLim[0]);

                double[] yAxis = new double[layers[0][0].length];
                double[] yAxis_filtered = new double[layers[0][0].length];
                double[] xAxis = new double[layers[0][0].length];

                double incr = delta;

                for(int z = 0; z < layers[0][0].length; z++){

                    xAxis[z] = incr;
                    incr += delta;

                    for(int x1 = xLim[0]; x1 <= xLim[1]; x1++){
                        for(int y1 = yLim[0]; y1 <= yLim[1]; y1++){

                            if(layers[x][y][z] >= 5) {
                                yAxis[z] += layers[x1][y1][z];
                                nPoints += layers[x1][y1][z];
                                //averagePoints[x][y] += layers[x1][y1][z];
                            }
                            //System.out.println(yAxis[z]);
                        }
                    }


                    //System.out.println(layers[x][y][z]);
                   // System.out.println("iter: " + counter++ + " / " + (numberOfPixelsX*numberOfPixelsY*numberOfPixelsZ));
                }


/*
                if(nPoints < average * 9) {
                    continue;
                }
*/

                int[] startEnd = compactArray(yAxis);

                if(startEnd[1] - startEnd[0] < (5.0 / delta)) {
                    continue;
                }

                yAxis = Arrays.copyOfRange(yAxis, startEnd[0], startEnd[1]);
                xAxis = Arrays.copyOfRange(xAxis, startEnd[0], startEnd[1]);

                yAxis = standardize(yAxis);

                double[] checkArray = Arrays.copyOfRange(yAxis, (int)(yAxis.length * (1.0 / 6.0)), (int)(yAxis.length * (2.0 / 4.0)));
                double stDev = std_dev(checkArray, checkArray.length);

                //System.out.println("stdev: " + stDev);

                //if(stDev < 0.05)
                  //  continue;

                //yAxis[0] = 0;
                //yAxis[1] = 0;
                //yAxis[yAxis.length-1] = 0;
                //yAxis[yAxis.length-2] = 0;


                //System.out.println(Arrays.toString(yAxis));

                //funk_poly = new PolynomialFunction(fitPolynomialToData(xAxis, yAxis, 10));

                yAxis_filtered = filter(yAxis);
                //yAxis_filtered = standardize(yAxis_filtered);

                //System.out.println(Arrays.toString(yAxis));
                //System.out.println(Arrays.toString(yAxis_filtered));
                //System.out.println("----------------");
                funk_poly = new PolynomialFunction(fitPolynomialToData(xAxis, yAxis, 5));
                //funk = interpolatori.interpolate(xAxis, yAxis);

                int countOf = 0;

                for(int i = 0; i < xAxis.length; i++){
                    //System.out.println(i + " " + funk_second_derivative.value(xAxis[i]));
                    if(yAxis[i] > 0)
                        countOf++;
                    yAxis_filtered[i] = funk_poly.value(xAxis[i]);
                }

                //if(countOf < 0.5 * (double)xAxis.length)
                  //  continue;


                funk = interpolatori.interpolate(xAxis, yAxis);
                //funk_first_derivative_poly = funk_poly.polynomialDerivative();
                //funk_second_derivative_poly = funk_first_derivative_poly.polynomialDerivative();

                funk_first_derivative = funk.polynomialSplineDerivative();
                funk_second_derivative = funk_first_derivative.polynomialSplineDerivative();

                double[] yDerivatives = new double[yAxis.length];

                for(int i = 0; i < xAxis.length; i++){
                    //System.out.println(i + " " + funk_second_derivative.value(xAxis[i]));
                    yDerivatives[i] = funk_second_derivative.value(xAxis[i]);
                    //yAxis_filtered[i] = funk.value(xAxis[i]);
                }

                //yDerivatives = filter(yDerivatives);

                funk_poly = new PolynomialFunction(fitPolynomialToData(xAxis, yDerivatives, 10));

                //interpolatori = new SplineInterpolator();

                //funk_s_poly = interpolatori.interpolate(xAxis, yDerivatives);

                for(int i = 0; i < xAxis.length; i++){
                    //System.out.println(i + " " + funk_second_derivative.value(xAxis[i]));
                    yDerivatives[i] = funk_poly.value(xAxis[i]);
                    //yDerivatives[i] = funk_s_poly.value(xAxis[i]);

                    //System.out.println(yDerivatives[i] + " == " + funk_s_poly.value(xAxis[i]));
                }

               // System.out.println("vales: " + Arrays.toString(yDerivatives));

                //yDerivatives = standardize(yDerivatives);

                int canopies = 0;

                double cutoff = 1E-10;

                int[] canopyOrNot = new int[yAxis.length];
                for(int i = 0; i < canopyOrNot.length; i++)
                    canopyOrNot[i] = -99;
                int indeksiHere = 0;

                int[] canopyLengths = new int[5];

                ArrayList<Integer> counts = new ArrayList<>();

                int currentCount = 0;

                boolean switchi = false;

                int gap = 0;
/*
                for(int i = 0 ; i < yDerivatives.length; i++){
                    if(yDerivatives[i] > 0)
                        yDerivatives[i] *= -1;
                    else
                        break;
                }
*/

                int consecutive = 0;
                int start = 0;
                int end = 0;

                boolean foundOver = false;
                boolean found = false;
                for(int i = 0; i < yAxis_filtered.length; i++){

                    canopyOrNot[i] = -99;

                    if(yAxis_filtered[i] > 0.2)
                        foundOver = true;

                    if(yAxis_filtered[i] <= 0.10 && foundOver){

                        if(found == true){
                            for(int j = 0; j < yAxis_filtered.length; j++){
                                canopyOrNot[j] = -99;
                            }
                            found = false;
                            break;
                        }

                        if(start == 0){
                            start = i;
                        }

                        consecutive++;
                    }else if(start != 0 && consecutive > 5 && yAxis_filtered[i] > 0.2){

                        end = i;

                        for(int j = 0; j <= start; j++){
                            canopyOrNot[j] = 1;
                        }

                        understoreyHeight[x][y] = startEnd[0] + start;
                        found = true;

                        //break;

                    }
                }

                if(found){
                    //System.out.println(Arrays.toString(canopyOrNot));
                    for (int i = 0; i < xAxis.length; i++) {

                        writer.write("" + (yAxis_filtered[i]) + "\t" + yAxis[i] + "\t" + (yDerivatives[i]*10) + "\t" + canopyOrNot[i] + "\n");


                    }

                    writer.write("" + 9999 + "\t" + 9999 + "\t" + 9999 + "\t" + 9999 + "\n");
                }

                if(false)
                for(int i = 0; i < yDerivatives.length - 1; i++){

                    indeksiHere = i + startEnd[0];

                    if(i == 0 && yDerivatives[i+1] < 0 - cutoff){
                        canopies++;

                        if(canopies >=5)
                            break;
                        canopyOrNot[i] = canopies;
                        switchi = true;
                        currentCount = 0;

                        continue;
                    }

                    /**POSITIVE to NEGATIVE */
                    if( !switchi && yDerivatives[i] > 0 + cutoff && yDerivatives[i+1] < 0 - cutoff) {

                        canopyLengths[canopies] = currentCount;
                        canopies++;
                        if(canopies >=5)
                            break;
                        canopyOrNot[i+1] = canopies;
                        switchi = true;
                        currentCount = 0;

                        continue;
                    }

                    /**POSITIVE*/
                    if(switchi && yDerivatives[i+1] > 0 + cutoff) {

                        canopyOrNot[i] = canopies;
                        canopyOrNot[i+1] = 0;
                        switchi = false;
                        continue;

                    }

                    if(switchi){

                        canopyOrNot[i] = canopies;
                    }else{
                        gap++;
                        currentCount++;
                        canopyOrNot[i] = 0;
                    }


                    if(canopies >=5) {
                        continue;
                    }
                    //if(yAxis[i] == 0)
                      //  yDerivatives[i] = 0;

                    /*
                    if(yDerivatives[i] < 0 - cutoff && (yDerivatives[i-1] > 0 + cutoff || i == 1)){

                        if(canopies == 0) {
                            //canopyLengths[canopies] = i - 1;
                        }
                        else{

                            //canopyLengths[canopies-1] = i - canopyLengths[canopies-1];
                            //canopyLengths[canopies] = i-1;

                            canopyLengths[canopies] = currentCount;
                        }
                        canopies++;
                    }

                    if (yDerivatives[i] < 0 - cutoff) {

                        canopy[x][y][indeksiHere] = canopies;
                        canopyOrNot[i] = canopies;
                        currentCount = 0;

                    }else if(canopies > 0){

                        currentCount++;
                        //System.out.println(currentCount);

                    }
                    */
                }
/*
                if(yDerivatives[yDerivatives.length-1] < 0 - cutoff)
                    canopyOrNot[yDerivatives.length-1] = canopies;
                else if(yDerivatives[yDerivatives.length-1] > 0 + cutoff)
                    canopyOrNot[yDerivatives.length-1] = 0;

 */
                //canopyLengths[canopies-1] = yDerivatives.length - canopyLengths[canopies-1];
                if(false)
                if((canopyLengths[0] > 0 && canopyLengths[1] == 0) || (canopyLengths[0] == 0 && canopyLengths[1] == 0)) {
                    continue;
                }

               // System.out.println(Arrays.toString(canopyOrNot));
/*
                System.out.println("canopies: " + canopies);
                System.out.println("before: " + Arrays.toString(canopyOrNot));
                System.out.println("before: " + Arrays.toString(canopyLengths));
                System.out.println("-------------------------------");
*/
                int largest = 0;


                int topLayerSize = 0;

                int binsBelowThreshold = 0;

                if(false)
                if(canopies > 0){

                    int canopyNumber = 1;
                    boolean skipFirstZeroes = canopyLengths[0] != 0;
                    int canopyNumber_n = 1;

                    int countDown = 0;

                    //System.out.println(canopyNumber);
                    int i = 1;

                    if(skipFirstZeroes){
                        for(int k = 0; k < yDerivatives.length; k++){
                            if(canopyOrNot[k] != 0){
                                break;
                            }
                            i++;
                        }
                    }

                    for(i = i; i < yDerivatives.length; i++) {

                        if(canopyOrNot[i] == 0){

                            //int tempLayerSize = 0;
                            int tempBinsBelowThreshold = 0;

                            for(int j = 0; j < canopyLengths[canopyNumber]+1; j++){

                                if(yAxis[i+j] <= binThresHold_d){
                                    tempBinsBelowThreshold++;
                                }

                                if(j < canopyLengths[canopyNumber] * (1.0 / 4.0)){
                                    canopyOrNot[i+j] = canopyNumber_n;
                                }else{
                                    canopyOrNot[i+j] = canopyNumber_n + 1;
                                }
                            }

                            i += canopyLengths[canopyNumber];

                            canopyNumber_n++;
                            canopyNumber++;
                            largest = canopyNumber;

                            binsBelowThreshold = tempBinsBelowThreshold;

                            if(canopyNumber > (canopies-1)){
                                break;
                            }
                        }

                    }

                }else{
                    continue;
                }

                //System.out.println("below: " + binsBelowThreshold);
                //System.out.println("after: " + Arrays.toString(yAxis));
                if(false)
                if(binsBelowThreshold < binThresHold_n)
                    continue;
/*
                System.out.println("after: " + Arrays.toString(canopyOrNot));
                System.out.println("after: " + Arrays.toString(canopyLengths));
                System.out.println(Arrays.toString(startEnd));
                System.out.println("++++++++++++++++++++++++++++++++++++");
*/
                if(false)
                for(int i = canopyOrNot.length-1; i > 0; i--){
                    if(canopyOrNot[i] == 0) {
                        canopyOrNot[i] = -99;
                        topLayerSize++;



                    }
                    else if(canopyOrNot[i] == largest){
                        topLayerSize++;
                        canopyOrNot[i] = -99;


                    }
                    else {
                        break;
                    }
                }



                if(false)
                if((topLayerSize * delta) < minCanopyLength)
                    if(largest >= 3){

                        //System.out.println("ERRO!! " + (topLayerSize * delta));
                        largest--;

                        topLayerSize = 0;

                        for(int i = canopyOrNot.length-1; i > 0; i--){
                            if(canopyOrNot[i] == -99) {
                                //canopyOrNot[i] = -99;
                                topLayerSize++;
                            }
                            else if(canopyOrNot[i] == (largest)){
                                topLayerSize++;
                                canopyOrNot[i] = -99;

                            }
                            else
                                break;
                        }
                        if((topLayerSize * delta) < minCanopyLength)
                            continue;
                    }else
                        continue;

                if(false)
                    if(canopies < 2)
                        continue;

                if(found) {
                    for (int i = 0; i < canopyOrNot.length; i++) {
                        canopy[x][y][startEnd[0] + i] = canopyOrNot[i];
    /*
                        canopy[x-1][y][startEnd[0] + i] = canopyOrNot[i];
                        canopy[x+1][y][startEnd[0] + i] = canopyOrNot[i];

                        canopy[x-1][y-1][startEnd[0] + i] = canopyOrNot[i];
                        canopy[x][y-1][startEnd[0] + i] = canopyOrNot[i];
                        canopy[x+1][y-1][startEnd[0] + i] = canopyOrNot[i];

                        canopy[x-1][y+1][startEnd[0] + i] = canopyOrNot[i];
                        canopy[x][y+1][startEnd[0] + i] = canopyOrNot[i];
                        canopy[x+1][y+1][startEnd[0] + i] = canopyOrNot[i];
    */
                        //canopy[x][y][i] = 0;
                    }

                    for(int i = 0; i < startEnd[0]; i++){
                        canopy[x][y][i] = 1;
                    }
                }


                //
                if(canopies > maxCanopies)
                    maxCanopies = canopies;

                if(canopies >= 0 && !printed){

                    count++;
                    if(true) {
/*
                        System.out.println("yDerivs: " + Arrays.toString( yDerivatives));
                        System.out.println("yAx: " +Arrays.toString(yAxis));
                        System.out.println("yAxFiltered: " +Arrays.toString(yAxis_filtered));
                        System.out.println("stdev: " + stDev);

 */
                        //System.out.println("below: " + binsBelowThreshold);
                        //System.out.println("after: " + Arrays.toString(yAxis));



                        printed = false;
                    }
                }

                //System.out.println("FINISHED ONE! " + Arrays.toString(canopyOrNot));
                //System.out.println(Arrays.toString(canopy[x][y]));
            }



        }

        System.out.println("max canopies: " + maxCanopies);

        File[] outputFiles = new File[maxCanopies + 1];
        LASraf[] outputraf = new LASraf[maxCanopies + 1];
/*
        File outputFile = aR.createOutputFileWithExtension(pointCloud, "_canopyLayer" + 1 + ".las");
        LASraf outputRaf = new LASraf(outputFile);
        LASwrite.writeHeader(outputRaf, "laslayer", this.pointCloud.versionMajor, this.pointCloud.versionMinor, this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength);
*/
        File outputFile_remainder = aR.createOutputFileWithExtension(pointCloud, "_understorey" + coreNumber + ".las");
        LASraf outputRaf_remainder = new LASraf(outputFile_remainder);
        LASwrite.writeHeader(outputRaf_remainder, "laslayer", this.pointCloud.versionMajor, this.pointCloud.versionMinor,
                this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength,
                this.pointCloud.headerSize, this.pointCloud.offsetToPointData, this.pointCloud.numberVariableLengthRecords,
                this.pointCloud.fileSourceID, this.pointCloud.globalEncoding,
                this.pointCloud.xScaleFactor, this.pointCloud.yScaleFactor, this.pointCloud.zScaleFactor,
                this.pointCloud.xOffset, this.pointCloud.yOffset, this.pointCloud.zOffset);


        File outputFile_overstorey = aR.createOutputFileWithExtension(pointCloud, "_canopy" + coreNumber + ".las");
        LASraf outputRaf_overstorey = new LASraf(outputFile_overstorey);
        LASwrite.writeHeader(outputRaf_overstorey, "laslayer", this.pointCloud.versionMajor, this.pointCloud.versionMinor,
                this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength,
                this.pointCloud.headerSize, this.pointCloud.offsetToPointData, this.pointCloud.numberVariableLengthRecords,
                this.pointCloud.fileSourceID, this.pointCloud.globalEncoding,
                this.pointCloud.xScaleFactor, this.pointCloud.yScaleFactor, this.pointCloud.zScaleFactor,
                this.pointCloud.xOffset, this.pointCloud.yOffset, this.pointCloud.zOffset);

        if(maxCanopies > 5)
            return;
/*
        for(int i = 0; i < outputFiles.length; i++){
            outputFiles[i] = aR.createOutputFileWithExtension(pointCloud, "_canopyLayer" + i + ".las");
            outputraf[i] = new LASraf(outputFiles[i]);
            LASwrite.writeHeader(outputraf[i], "laslayer", this.pointCloud.versionMajor, this.pointCloud.versionMinor, this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength);

        }
*/
        int canopylayer = 0;

        int pointCount = 0;

        meanFilter(understoreyHeight);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();//pointCloud.braf.buffer.position(0);
            }

            for (int j = 0; j < maxi; j++) {
                //Sstem.out.println(j);
                pointCloud.readFromBuffer(tempPoint);
                tempPoint.pointSourceId = 0;
                //if (tempPoint.z > 3.0) {
                    int xCoord = (int) ((tempPoint.x - minX) / resolution);
                    int yCoord = (int) ((maxY - tempPoint.y) / resolution);
                    int zCoord = (int) ((tempPoint.z - minZ) / delta);

                    //if()
                    //canopylayer = canopy[xCoord][yCoord][zCoord];

                    //if(canopylayer == 0)
                      //  canopylayer = maxCanopies;


                    //if(canopylayer == -99) {
                    if(zCoord > (int)understoreyHeight[xCoord][yCoord] && zCoord < numberOfPixelsZ){
                        //tempPoint.pointSourceId = (short) 11;

                        //}else if (tempPoint.z <= 3.0) {
                        //  tempPoint.pointSourceId = (short) 222;
                        //}
                        //if (outputRaf.writePoint(tempPoint, aR.getInclusionRule(), 0.01, 0.01, 0.01, 0, 0, 0, pointCloud.pointDataRecordFormat, i + j))
                          //  pointCount++;
                        if (outputRaf_overstorey.writePoint(tempPoint, aR.getInclusionRule(), pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i + j))
                        pointCount++;
                    }
                        else if(zCoord > 0 && tempPoint.z >= minz) {
                            tempPoint.pointSourceId = (short) 222;


                    }

                if (outputRaf_remainder.writePoint(tempPoint, aR.getInclusionRule(), pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                        pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i + j))
                    pointCount++;

/*
                    if(canopylayer == -99) {
                        if (outputRaf.writePoint(tempPoint, aR.getInclusionRule(), 0.01, 0.01, 0.01, 0, 0, 0, pointCloud.pointDataRecordFormat, i + j))
                            pointCount++;
                    }else
                        if (outputRaf_remainder.writePoint(tempPoint, aR.getInclusionRule(), 0.01, 0.01, 0.01, 0, 0, 0, pointCloud.pointDataRecordFormat, i + j))
                            pointCount++;

 */
               // }
            }
        }
/*
        for(int i = 0; i < outputraf.length; i++){
            outputraf[i].writeBuffer2();
            outputraf[i].updateHeader2();
        }

 */
        outputRaf_remainder.writeBuffer2();
        outputRaf_remainder.updateHeader2();

        outputRaf_overstorey.writeBuffer2();
        outputRaf_overstorey.updateHeader2();

       // outputRaf.writeBuffer2();
        //outputRaf.updateHeader2();

        writer.close();
    }

    public double[] standardize(double[] in){

        double max = Double.NEGATIVE_INFINITY;

        double[] out = new double[in.length];

        for(int i = 0; i < in.length; i++){
            if(in[i] > max)
                max = in[i];
        }

        for(int i = 0; i < in.length; i++)
            out[i] = in[i] / max;

        return out;
    }

    double std_dev(double[] a, int n) {
        if(n == 0)
            return 0.0;
        double sum = 0;
        double sq_sum = 0;
        for(int i = 0; i < n; ++i) {
            sum += a[i];
            sq_sum += a[i] * a[i];
        }
        double mean = sum / n;
        double variance = sq_sum / n - mean * mean;
        return Math.sqrt(variance);
    }

    public void fillArray(int[] in, int value){

        for(int i = 0; i < in.length; i++){
            in[i] = value;
        }
    }

    public double[] fitPolynomialToData(double[] x, double[] y, int degree) {
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < x.length; i++) {
            obs.add(1, x[i], y[i]);
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        double[] coefficients = fitter.fit(obs.toList());

        return coefficients;

    }

    public int[] compactArray(double[] in){

        int start = -1;
        int end = -1;

        int endIndex = in.length-1;

        for(int i = 0; i < in.length; i++){

            if(in[i] != 0 && start == -1){
                start = i;
            }
            if(in[endIndex] != 0 && end == -1){

                end = endIndex+1;
            }

            endIndex--;
        }

        return new int[]{start, end};

    }



    public double[] filter(double[] in){

        double[] output = new double[in.length];

        int minus = gaussKernel.length / 2;

        int index = 0;
        double temp = 0.0;

        for(int i = 0; i < in.length; i++){

            temp = 0.0;
            index = 0;

            int inde = 0;

            double value = 0;

            for(int k = i - minus; k <= i + minus; k++ ){

                inde = k;
/*
                while(inde < 0){
                    inde = in.length - (-1 * inde);
                }

                while(inde >= in.length){
                    inde = 0 + (inde - in.length);
                }
*/
                if(inde < 0) {
                    value = 0;// in[0];
                }
                else if(inde >= in.length){
                    value = 0;//in[in.length-1];
                }else
                    value = in[inde];
//                System.out.println("inde : " + inde + " index: " + index + " " + in.length);
  //              System.out.println(Arrays.toString(in));

                temp += value * gaussKernel[index];
                index++;
             }


            output[i] = temp;

        }

        return output;
    }

    public double[] filterMean(double[] in){

        double[] output = new double[in.length];

        int minus = gaussKernel.length / 2;

        int index = 0;
        double temp = 0.0;
        int counter = 0;

        for(int i = 0; i < in.length; i++){

            temp = 0.0;
            index = 0;

            int inde = 0;

            double value = 0;

            for(int k = i - minus; k <= i + minus; k++ ){

                inde = k;
/*
                while(inde < 0){
                    inde = in.length - (-1 * inde);
                }

                while(inde >= in.length){
                    inde = 0 + (inde - in.length);
                }
*/
                if(inde < 0) {
                    value = 0;// in[0];
                }
                else if(inde >= in.length){
                    value = 0;//in[in.length-1];
                }else
                    value = in[inde];

                temp += value;
                index++;
                counter++;
            }


            output[i] = temp / (double)counter;

        }

        return output;
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }



    public double Function1D(double x){
        return Math.exp( x * x / ( -2.0 * thetaSqrt ) ) / ( Math.sqrt( 2.0 * Math.PI ) * theta );
    }

    public double[] Kernel1D(int size){
        if ( ( ( size % 2 ) == 0 ) || ( size < 3 ) ){
            try {
                throw new Exception("Wrong size");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int r = size / 2;
        // kernel
        double[] kernel = new double[size];

        // compute kernel
        for ( int x = -r, i = 0; i < size; x++, i++ )
        {
            kernel[i] = Function1D( x );
        }

        return kernel;
    }

    public static void meanFilter(float[][] input){

        Statistics stat = new Statistics();

        int x = 0;
        int y = 0;

        int n = 1;

        //float[][] output = new float[input.length - n * 2][input[0].length - n * 2];

        float[][] temppi = arrayCopy(input);

        int height = input[0].length;
        int width = input.length;
        int counter = 0;
        int paatos = (height - n) * (width - n);

        int count3 = 0;

        float[] tempF;

        ArrayList<int[]> leftOvers = new ArrayList<int[]>();

        for(int i = n; i < (height - n); i++){

            for(int j = n; j < (width - n); j++){


                int minX = j - n;
                int maxX = j + n;
                int minY = i - n;
                int maxY = i + n;


                tempF = new float[2];

                for(int h = minX; h <= maxX; h++){
                    for(int u = minY; u <= maxY; u++){

                        x = h;
                        y = u;

                        if(x < 0)
                            x = 0;
                        if(y < 0)
                            y = 0;
                        if(x > (width - 1))
                            x = width - 1;
                        if(y > (height - 1))
                            y = height - 1;

                        if((x != j || y != i) && !Double.isNaN(temppi[x][y]) && temppi[x][y] > 0.0){

                            tempF[0] += temppi[x][y];
                            tempF[1]++;
                            //list.add(temppi[h][u]);
                        }
                    }


                }

                float mean = tempF[0] / tempF[1];

                if(mean > 0)
                    input[j][i] = mean;

                counter++;
            }



        }


    }

    public static float[][] arrayCopy(float[][] in){

        float[][] output = new float[in.length][in[0].length];

        for(int i = 0; i < in.length; i++)
            for(int j = 0; j < in[0].length; j++)
                output[i][j] = in[i][j];

        return output;


    }
/*
    public static double[] getGaussianKernel1D(int radius, double sigma) {
        double[] res = new double[radius * 2 + 1];
        double norm = 1.0 / (Math.sqrt(2 * Math.PI) * sigma);
        double coeff = 2 * sigma * sigma;
        double total = 0;
        for (int x = -radius; x <= radius; x++) {
            double g = norm * Math.exp(-x * x / coeff);
            res[x + radius] = g;
            total += g;
        }
        for (int x = 0; x < res.length; x++) {
            res[x] /= total;
        }
        return res;
    }
*/
}
