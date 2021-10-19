package tools;
import LASio.*;
import org.ejml.data.DMatrixRMaj;
import utils.*;

import java.io.*;
import java.util.*;

public class stemAligner {

    LASReader pointCloud;
    LASReader pointCloud_thin;
    argumentReader aR;
    TreeMap<Double,double[]> trajectory = new TreeMap<>();
    double deltaT;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
    org.tinfour.interpolation.TriangularFacetInterpolator polator;// = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();


    public stemAligner(){

    }

    public stemAligner(LASReader temp, argumentReader aR){

        this.pointCloud = temp;
        this.aR = aR;

    }

    public void readTrajectoryFile(String fileName){

        File trajFile = new File(fileName);

        String[] tokens = null;
        double tempCopy = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(trajFile))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {

                tokens = line.split(" ");
                double[] temp = new double[]{Double.parseDouble( tokens[1]),Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])
                        ,Double.parseDouble( tokens[4]),Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6])};
                //System.out.println(Arrays.toString(temp));

                tempCopy = temp[1];
                temp[1] = temp[0];
                temp[0] = tempCopy;
                //System.out.println(Arrays.toString(temp));

                //System.out.println("---------------");
                trajectory.put((Double.parseDouble(tokens[0])), temp);

            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void createTin(int groundClassification) throws IOException{

        LasPoint tempPoint = new LasPoint();

        long n = pointCloud.getNumberOfPointRecords();

        for(int i = 0; i < n; i++){

            pointCloud.readRecord(i, tempPoint);

            if(tempPoint.classification == groundClassification)
                tin.add(new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z));


        }

        polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
    }

    public void thinPointCloud(LASReader pointCloud) throws IOException{

        aR.step = 0.03;
        //System.out.println(this.y_interval);
        aR.few = 1;
        aR.thin3d = true;
        aR.cores = 1;
        aR.p_update = new progressUpdater(aR);

        Thinner thin = new Thinner(pointCloud, aR.step, aR, 1);

        this.pointCloud_thin = new LASReader(thin.outputFile);
    }

    public void align() throws Exception{

        readTrajectoryFile(aR.trajectory);

        pointWriterMultiThread pw = new pointWriterMultiThread(pointCloud.getFile(), pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
        int maxi = 0;

        LasPoint tempPoint = new LasPoint();

        TreeMap<Integer, HashSet<Integer>> flightLineIds = new TreeMap<>();
        TreeMap<Integer, double[]> flightLineTimeStamps = new TreeMap<>();
        TreeMap<Integer, double[]> flightLineTimeStamps_full_clouds = new TreeMap<>();

        TreeMap<Short, HashMap<Integer, ArrayList<cloudPoint>>> stemWise = new TreeMap<>();

        double time_min = Double.POSITIVE_INFINITY;
        double time_max = Double.NEGATIVE_INFINITY;

        //TreeMap<Integer, double[]> flightLineTimes = new TreeMap<>();

        createTin(2);

        double dz = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.classification == 4 && tempPoint.pointSourceId > 0) {

                    dz = tempPoint.z - polator.interpolate(tempPoint.x, tempPoint.y, valuator); //= tempPoint.z - raster_dz[xCoord][yCoord];

                    //System.out.println("dz: " + dz);
                    //System.out.println("dz2: " + polator.interpolate(tempPoint.x, tempPoint.y, valuator));

                    if(stemWise.containsKey(tempPoint.pointSourceId)){



                        if(stemWise.get(tempPoint.pointSourceId).containsKey(tempPoint.userData)){

                            cloudPoint tempCloudPoint = new cloudPoint(tempPoint.x, tempPoint.y, tempPoint.z, dz, tempPoint.gpsTime, tempPoint.userData, tempPoint.pointSourceId);
                            stemWise.get(tempPoint.pointSourceId).get(tempPoint.userData).add(tempCloudPoint);

                        }else{
                            stemWise.get(tempPoint.pointSourceId).put(tempPoint.userData, new ArrayList<>());
                            cloudPoint tempCloudPoint = new cloudPoint(tempPoint.x, tempPoint.y, tempPoint.z, dz, tempPoint.gpsTime, tempPoint.userData, tempPoint.pointSourceId);

                            stemWise.get(tempPoint.pointSourceId).get(tempPoint.userData).add(tempCloudPoint);
                        }

                    }else{
                        stemWise.put(tempPoint.pointSourceId, new HashMap<>());
                        stemWise.get(tempPoint.pointSourceId).put(tempPoint.userData, new ArrayList<>());

                        cloudPoint tempCloudPoint = new cloudPoint(tempPoint.x, tempPoint.y, tempPoint.z, dz, tempPoint.gpsTime, tempPoint.userData, tempPoint.pointSourceId);

                        stemWise.get(tempPoint.pointSourceId).get(tempPoint.userData).add(tempCloudPoint);
                    }

                    if (flightLineIds.containsKey(tempPoint.userData)) {
                        flightLineIds.get(tempPoint.userData).add(i + j);

                        if(tempPoint.gpsTime < flightLineTimeStamps.get(tempPoint.userData)[0]){
                            flightLineTimeStamps.get(tempPoint.userData)[0] = tempPoint.getGpsTime();
                        }
                        if(tempPoint.gpsTime > flightLineTimeStamps.get(tempPoint.userData)[1]){
                            flightLineTimeStamps.get(tempPoint.userData)[1] = tempPoint.getGpsTime();
                        }

                    } else {
                        flightLineIds.put(tempPoint.userData, new HashSet<Integer>());
                        flightLineIds.get(tempPoint.userData).add(i + j);

                        flightLineTimeStamps.put(tempPoint.userData, new double[]{tempPoint.getGpsTime(), tempPoint.getGpsTime()});
                    }


                }

                if(tempPoint.gpsTime < time_min)
                    time_min = tempPoint.gpsTime;
                if(tempPoint.gpsTime > time_max)
                    time_max = tempPoint.gpsTime;

                if (flightLineTimeStamps_full_clouds.containsKey(tempPoint.userData)) {
                    if (tempPoint.gpsTime < flightLineTimeStamps_full_clouds.get(tempPoint.userData)[0]) {
                        flightLineTimeStamps_full_clouds.get(tempPoint.userData)[0] = tempPoint.getGpsTime();
                    }
                    if (tempPoint.gpsTime > flightLineTimeStamps_full_clouds.get(tempPoint.userData)[1]) {
                        flightLineTimeStamps_full_clouds.get(tempPoint.userData)[1] = tempPoint.getGpsTime();
                    }
                }else{
                    flightLineTimeStamps_full_clouds.put(tempPoint.userData, new double[]{tempPoint.getGpsTime(), tempPoint.getGpsTime()});
                }

            }
        }

/*

        for (Map.Entry<Integer, HashSet<Integer>> entry : flightLineIds.entrySet()) {
            //System.out.println(entry.getKey() + " n_points: " + entry.getValue().size());
        }

        for (Map.Entry<Short, HashMap<Integer, ArrayList<cloudPoint>>> entry : stemWise.entrySet()) {
            //System.out.println("Trunk id: " + entry.getKey() + " n_flightLines: " + entry.getValue().size());
        }
*/

        TreeMap<Integer, double[]> flightLinePivotPoints = new TreeMap<>();
        Map.Entry<Double, double[]> hehe;
        Map.Entry<Double, double[]> hehe2;
        double x_coord,y_coord, z_coord;
        deltaT = 1.0;
        ArrayList<Integer> flightLine_n_segments = new ArrayList<>();

        ArrayList<Integer> flightlineidlist = new ArrayList<>();
        ArrayList<Integer> flightLineParamsStartFrom = new ArrayList<>();
        flightLineParamsStartFrom.add(0);

        TreeMap<Integer, strip> stripInformation = new TreeMap<>();

        if(false)
        for(Map.Entry<Integer, double[]> entry : flightLineTimeStamps.entrySet()){

            System.out.println(entry.getValue()[0] + " " + entry.getValue()[1]);

            flightlineidlist.add(entry.getKey());

            int numberOfSegments = (int)Math.ceil((entry.getValue()[1] - entry.getValue()[0]) / deltaT);
            System.out.println("n_seg: " + numberOfSegments);

            flightLine_n_segments.add(numberOfSegments);

            if(flightLine_n_segments.size() > 1){
                flightLineParamsStartFrom.add(flightLineParamsStartFrom.get(flightLineParamsStartFrom.size()-1) + flightLine_n_segments.get(flightLine_n_segments.size()-2));
            }

            stripInformation.put(entry.getKey(), new strip( entry.getKey(),
                                                            flightLine_n_segments.get(flightLine_n_segments.size()-1),
                                                            flightLineParamsStartFrom.get(flightLineParamsStartFrom.size()-1),
                                                            entry.getValue()[0],
                                                            entry.getValue()[1]));

            hehe = trajectory.ceilingEntry(entry.getValue()[0]);
            hehe2 = trajectory.ceilingEntry(entry.getValue()[1]);
            x_coord = (hehe.getValue()[0] + hehe2.getValue()[0]) / 2.0;
            y_coord = (hehe.getValue()[1] + hehe2.getValue()[1]) / 2.0;
            z_coord = (hehe.getValue()[2] + hehe2.getValue()[2]) / 2.0;
            //System.out.println(entry.getKey() + " " + x_coord + " " + y_coord + " " + z_coord);

        }

        if(true)
        for(Map.Entry<Integer, double[]> entry : flightLineTimeStamps_full_clouds.entrySet()){

            flightlineidlist.add(entry.getKey());

            int numberOfSegments = (int)Math.ceil((entry.getValue()[1] - entry.getValue()[0]) / deltaT);

            flightLine_n_segments.add(numberOfSegments);

            if(flightLine_n_segments.size() > 1){
                flightLineParamsStartFrom.add(flightLineParamsStartFrom.get(flightLineParamsStartFrom.size()-1) + flightLine_n_segments.get(flightLine_n_segments.size()-2));
            }

            stripInformation.put(entry.getKey(), new strip( entry.getKey(),
                    flightLine_n_segments.get(flightLine_n_segments.size()-1),
                    Math.max(0,flightLineParamsStartFrom.get(flightLineParamsStartFrom.size()-1) - 1),
                    entry.getValue()[0],
                    entry.getValue()[1]));

            System.out.println((int)entry.getValue()[0] + " " + (int)entry.getValue()[1]);
            hehe = trajectory.ceilingEntry(entry.getValue()[0]);
            hehe2 = trajectory.ceilingEntry(entry.getValue()[1]);
            x_coord = (hehe.getValue()[0] + hehe2.getValue()[0]) / 2.0;
            y_coord = (hehe.getValue()[1] + hehe2.getValue()[1]) / 2.0;
            z_coord = (hehe.getValue()[2] + hehe2.getValue()[2]) / 2.0;
            //System.out.println(entry.getKey() + " " + x_coord + " " + y_coord + " " + z_coord);

        }

        int sum_of_segments = flightLine_n_segments.stream()
                .mapToInt(a -> a)
                .sum();

        for(int i = 1; i < flightLineParamsStartFrom.size(); i++){

            flightLineParamsStartFrom.set(i, flightLineParamsStartFrom.get(i)-1);

        }

        System.out.println(time_min + " " + time_max);
        System.out.println(sum_of_segments);

        System.out.println(Arrays.toString(flightLine_n_segments.toArray()));
        System.out.println(Arrays.toString(flightLineParamsStartFrom.toArray()));


        //System.exit(1);
        LevenbergMarquardt lm = new LevenbergMarquardt(1);

        residFunctionStemAlign res = new residFunctionStemAlign(deltaT, stripInformation);

        res.setTrajectory(trajectory);

        res.setStartTime(time_min);
        res.setEndTime(time_max);
        res.setTrunkPoints(stemWise);

        System.out.println(time_max + " " + time_min);
        int numberOfSegments = (int)Math.ceil((time_max - time_min) / deltaT);

        DMatrixRMaj param = null;
        DMatrixRMaj residuals = null;
        param = new DMatrixRMaj(6 * sum_of_segments, 1);
        double[] paramsDouble = new double[6 * sum_of_segments];
        //param = new DMatrixRMaj(flightLineIds.size() * 6, 1);
        residuals = new DMatrixRMaj(1, 1);
        residuals.set(0,0,0);
        System.out.println(deltaT);
        System.out.println(numberOfSegments);

        int counti = 0;

        for(int g = 0 ; g < param.numRows; g++) {
            param.set(g, 0, 0);

            //if(counti == 6){
             //   param.set(g, 0, 1);
            //    counti = -1;
            //}
            counti++;
        }


        System.out.println( "nFlightLines: " + (param.numRows/6.0));

/*
        FunctionNtoM func = new functionCircleFit(deltaT, stripInformation);

        ((functionCircleFit) func).setNumFunctions(paramsDouble.length);

        ((functionCircleFit) func).setTrajectory(trajectory);

        ((functionCircleFit) func).setStartTime(time_min);
        ((functionCircleFit) func).setEndTime(time_max);
        ((functionCircleFit) func).setTrunkPoints(stemWise);

        ConfigLevenbergMarquardt c = new ConfigLevenbergMarquardt();

        c.dampeningInitial = 1;


        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(c, true);

        //UnconstrainedLeastSquaresSchur<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardtSchur(true, null);


        // Send to standard out progress information
        optimizer.setVerbose(System.out,0);

        // if no jacobian is specified it will be computed numerically
        optimizer.setFunction(func,null);

        double[] resid = new double[]{0};

        // provide it an extremely crude initial estimate of the line equation
        optimizer.initialize(paramsDouble,1e-12,1e-12);

        System.out.println(" INITIAL ERROR : " + Arrays.toString(((functionCircleFit) func).calculateCost(paramsDouble, resid, false)));

        UtilOptimize.process(optimizer,10);

        double found[] = optimizer.getParameters();
        System.out.println(" FINAL ERROR : " + Arrays.toString(((functionCircleFit) func).calculateCost(found, resid, false)));

        // see how accurately it found the solution
        System.out.println("Final Error = "+optimizer.getFunctionValue());
*/


        res.prep(param);
        //System.exit(1);
        //double initialCost[] = res.calculateCost(param, residuals, false);
        System.out.println("Initial cost: " + res.thisCosts[0] + " & " + res.thisCosts[1] + " from " + res.numberOfCircles + " circles");
        System.out.println("mean radius: " + res.meanRadius);
        lm.optimize(res, param);

        //res.initialized = true;
        //double optimizedCost[] = res.calculateCost(param, residuals, false);
        res.compute(param, lm.residuals);
        System.out.println("Final cost: " + res.thisCosts[0] + " & " + res.thisCosts[1] + " from " + res.numberOfCircles + " circles");
        System.out.println("mean radius: " + res.meanRadius);


        for(Map.Entry<Integer, strip> entry : stripInformation.entrySet()){
            System.out.println("Strip: " + entry.getKey() + " " + entry.getValue().observed_slices);
        }

        File outputFile = aR.createOutputFile(pointCloud);

        //String outputfile = "stemAligned.las";
        //File outputFile = new File("stemAligned.las");

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        LASraf outputLAS = new LASraf(outputFile);
        PointInclusionRule rule = new PointInclusionRule();
        LASwrite.writeHeader(outputLAS, "stemAlign", pointCloud.versionMajor, pointCloud.versionMinor,
                pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

        //res.prep(param);
        int pointCount = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                //System.out.println("pre_x: " + tempPoint.x + " pre_y: " + tempPoint.y + " pre_z: " + tempPoint.z);
                res.rotateLasPoint(tempPoint);
                //System.out.println("aft_x: " + tempPoint.x + " aft_y: " + tempPoint.y + " aft_z: " + tempPoint.z);

                if(outputLAS.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                        pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, i ))
                    pointCount++;
            }


        }

        outputLAS.writeBuffer2();
        outputLAS.updateHeader2();

        System.out.println("DONE!");

    }

}

