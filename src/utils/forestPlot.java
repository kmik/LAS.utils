package utils;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import tools.plotAugmentator;

import java.io.*;
import java.util.*;

import org.gdal.ogr.*;
import org.gdal.osr.*;

import static org.gdal.ogr.ogrConstants.*;

public class forestPlot {

    public double train_area = 254.469;

    public double totalVolue = 0;
    LASReader plotLASReader;
    public File plotLASFile;
    plotAugmentator pA;
    int id;
    double[][] plotBounds;

    double[][] smallerBounds;
    boolean hasSmallerBounds = false;
    double[] plotBounds_extent;
    ArrayList<double[][]> plotBounds_simulated = new ArrayList<double[][]>();

    int simulationOffsetX = 0;
    int simulationOffsetY = 0;
    int plotID;
    double[][] plotITCTreeBounds;

    public File simulationOutputFile;
    public File simulationReportFile;
    public File simulationPlotForestFile;
    public File simulationPlotFile;
    public File simulationTreeTopFile;

    public File simulationPlotShapeFile;

    double[] speciesSpecificVolume = new double[3];
    double[] speciesSpecificVolume_simulated = new double[3];
    double area, smallArea;
    public HashMap<Integer, forestTree> trees = new HashMap<Integer, forestTree>();
    HashMap<Integer, forestTree> trees_unique_id = new HashMap<Integer, forestTree>();
    HashMap<Integer, forestTree> trees_unique_id_for_simulation = new HashMap<Integer, forestTree>();

    public HashMap<Integer, forestTree> simulated_plot = new HashMap<Integer, forestTree>();
    HashSet<Integer> treeHasITC = new HashSet<Integer>();
    HashSet<Integer> ITCWithoutMatch = new HashSet<Integer>();

    int n_simulations = 0;
    int simulationLocationCountX = 0;
    int simulationLocationCountY = 1;

    boolean locked = false;
    HashSet<Integer> treeWithoutITC = new HashSet<Integer>();
    public forestPlot(){
    }

    public forestPlot(int id){

        this.id = id;
    }

    public synchronized void lockPlot(){
        System.out.println("Locking plot " + this.id);
        this.locked = true;
    }

    public synchronized void unlock(){

        this.locked = false;
    }

    public synchronized boolean isLocked(){
        return this.locked;
    }

    public forestPlot( int id,  double[][] plotBounds, plotAugmentator pA){
        this.id = id;
        this.pA = pA;
        this.plotBounds = plotBounds;
        this.plotBounds_simulated.add( clone2DArray(this.plotBounds));
        this.plotBounds_extent = this.getExtent(plotBounds);



        this.simulationOffsetX = (int)(this.plotBounds_extent[2] - this.plotBounds_extent[0]) * 2;
        this.simulationOffsetY = (int)(this.plotBounds_extent[3] - this.plotBounds_extent[1]) * 2;

    }

    public void addTree(forestTree tree){
        trees.put(tree.getTreeID(), tree);
        trees_unique_id.put(tree.getTreeID_unique(), tree);
        tree.setPlotID(this.id);
        this.totalVolue += tree.getTreeVolume();

    }

    public int treesOverDBH(double dbhLimit){
        int nTrees = 0;
        for(forestTree tree : trees.values()){
            if(tree.getTreeDBH() >= dbhLimit)
                nTrees++;
        }
        return nTrees;


    }

    public double[][] getSmallerBounds() {
        return smallerBounds;
    }

    public void setSmallerBounds(double[][] smallerBounds) {
        this.smallerBounds =  clone2DArray(smallerBounds);
        this.hasSmallerBounds = true;
    }

    public void hasITCid(int treeID){
        treeHasITC.add(treeID);
        //System.out.println(pA.getTreeBounds().get(treeID)[0][0]);
    }

    // print 2d array

    public void preparePlot(){

        try {
            plotLASReader = new LASReader(this.plotLASFile);
            //System.out.println("Reading LAS file: " + this.plotLASFile.getName());
        }catch (Exception e){
            System.out.println("Error reading LAS file");
        }

        for(int treeID : trees.keySet()){

            if(!trees.get(treeID).hasCrown){

                int target = underWhere(treeID);


                //System.out.println("Tree is under segment: " + target + " " + treeHasITC.size());

                if(target != -1){

                    trees.get(target).volume_total[trees.get(treeID).getTreeSpecies()] += trees.get(treeID).getTreeVolume();
                    trees.get(target).addTreeBeneath(trees.get(treeID));
                    trees.get(treeID).beneathCrownId = target;
                    //System.out.println(Arrays.toString(trees.get(target).volume_total));

                }else{

                    //System.out.println("UNDER ITC WITHOUT MATCH: " + treeID);
                }
            }

            this.speciesSpecificVolume[trees.get(treeID).getTreeSpecies()] += trees.get(treeID).getTreeVolume();

        }


        for(int treeID : trees.keySet()){

            if(trees.get(treeID).hasCrown){
/*
                if(trees.get(treeID).getTreeITCid() == 6922){
                    System.out.println("CONSIDERED! " + pointInPlot(new double[]{trees.get(treeID).getTreeX_ITC(), trees.get(treeID).getTreeY_ITC()}));
                    System.out.println(Arrays.toString(new double[]{trees.get(treeID).getTreeX_ITC(), trees.get(treeID).getTreeY_ITC()}));
                    System.exit(1);
                }
*/
                //if(pointInPlot(new double[]{trees.get(treeID).getTreeX(), trees.get(treeID).getTreeY()})) {
                if(pointInPlot(new double[]{trees.get(treeID).getTreeX_ITC(), trees.get(treeID).getTreeY_ITC()})) {

                    trees_unique_id_for_simulation.put(trees.get(treeID).getTreeID_unique(), trees.get(treeID));

                    trees.get(treeID).belongsToOptimization = true;
                    //System.out.println(trees.get(treeID).getTreeID_unique() + " == " + trees.get(treeID).getTreeITCid());
                    for(int i = 0; i < trees.get(treeID).treesBeneath.size(); i++){

                        if( !trees.containsKey(trees.get(treeID).treesBeneath.get(i).getTreeID())) {
                            System.out.println(trees.get(treeID).getTreeITCid() + " " + trees.get(treeID).getTreeID());
                            throw new toolException("Tree " + trees.get(treeID).treesBeneath.get(i) + " is not in the plot " + this.id);
                        }
                        trees_unique_id_for_simulation.put(trees.get(treeID).treesBeneath.get(i).getTreeID_unique(), trees.get(treeID).treesBeneath.get(i));
                        trees.get(treeID).treesBeneath.get(i).belongsToOptimization = true;
                    }

                }

            }else if(trees.get(treeID).beneathCrownId == -1){

                // NOT UNDER ANY SEGMENT

                if(pointInPlot(new double[]{trees.get(treeID).getTreeX(), trees.get(treeID).getTreeY()})) {

                    trees_unique_id_for_simulation.put(trees.get(treeID).getTreeID_unique(), trees.get(treeID));

                    trees.get(treeID).belongsToOptimization = true;
                }

            }else if(trees.get(treeID).beneathCrownId != -1){
/*
                if(pointInPlot(new double[]{ trees.get(trees.get(treeID).beneathCrownId).getTreeX_ITC(), trees.get(trees.get(treeID).beneathCrownId).getTreeY_ITC()})){
                    trees_unique_id_for_simulation.put(trees.get(treeID).getTreeID_unique(), trees.get(treeID));

                }

 */
            }else{
                throw new toolException("SHOULD NOT BE HERE!!");
            }

        }

        double frac = 10000 / this.area;

        for(int i = 0 ; i < speciesSpecificVolume.length; i++){
            speciesSpecificVolume[i] *= frac;
            speciesSpecificVolume[i] /= 1000;
        }

        System.out.println("Plot " + this.id + " orig trees: " + trees.size() + " trees for simulation: " + trees_unique_id_for_simulation.size());
        //System.out.println("Plot " + this.id + " species specific volume: " + Arrays.toString(this.speciesSpecificVolume));
    }

    public int underWhere(int treeId){

        for(int treeID : treeHasITC){

            if(treeID == treeId)
                continue;

            if(trees.get(treeID).containsCoordinate(trees.get(treeId).getTreeX(), trees.get(treeId).getTreeY()))
                return treeID;

            //System.out.println("tried tree: " + treeID);
        }

        return -1;
    }

    public int underWhere_ITC_ID(int treeId){

        for(int treeID : trees.keySet()){

            int unique_id = trees.get(treeID).getTreeID_unique();
            if(treeID == treeId)
                continue;

            if(pA.ITChasNoMatchBoundaries.containsKey(unique_id))
            if( containsCoordinate(trees.get(treeId).getTreeX(), trees.get(treeId).getTreeY(), pA.ITChasNoMatchBoundaries.get(unique_id)) ) {
                return trees.get(treeID).getTreeID_unique();
            }
        }

        return -1;
    }


    public void ITCHasNoMatch(int ITCid){
        ITCWithoutMatch.add(ITCid);
        //System.out.println("plot " + this.id + " " + treeWithoutITC.size());
    }
    public forestTree getTree(int treeID){

        if(!trees.containsKey(treeID))
            throw new toolException("Tree " + treeID + " not found in plot " + this.id);

        return trees.get(treeID);
    }

    public boolean pointInPlot(double[] point ){


        if(pointInPolygon(point, this.plotBounds))
            return true;

        return false;
    }

    public boolean pointInPlot(double[] point, double[][] bounds){


        return pointInPolygon(point, bounds);

    }


    public void findOmittedTrees(){
        for(int treeID : trees.keySet()){
            if(!treeHasITC.contains(treeID)){


            }
        }
    }

    public double[][] getPlotBounds() {
        return plotBounds;
    }

    public void setPlotBounds(double[][] plotBounds) {
        this.plotBounds = plotBounds;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public void setSmallerArea(double area) {
        this.smallArea = area;
    }

    public double getSmallerArea() {
        return this.smallArea;
    }


    public static boolean pointInPolygon(double[] point, double[][] poly) {


        int numPolyCorners = poly.length;
        int j = numPolyCorners - 1;
        boolean isInside = false;

        for (int i = 0; i < numPolyCorners; i++) {
            if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                    isInside = !isInside;
                }
            }
            j = i;
        }
        return isInside;
    }

    public File getPlotLASFile() {
        return plotLASFile;
    }

    public void setPlotLASFile(File plotLASFile) {
        this.plotLASFile = plotLASFile;
    }

    public void prepareSimulation(){
        this.speciesSpecificVolume_simulated = speciesSpecificVolume.clone();
    }

    public void createSimulationFile(String simulation){

        String simul2 = simulation;
        simulation += ".las";

        try {
            this.simulationOutputFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation);
        }catch (Exception e){
            System.out.println("Error creating simulation las file");
        }

        this.createSimulationReport(simul2);
        this.createSimulatedPlotFile(simul2);
        this.createSimulatedPlotShapeFile(simul2);
        this.createSimulationPlotForestFile(simul2);
    }
    public void createSimulationFile(String simulation, String odir){

        String simul2 = simulation;
        simulation += ".las";

        try {
            this.simulationOutputFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating simulation las file");
        }

        this.createSimulationReport(simul2, odir);
        this.createSimulatedPlotFile(simul2, odir);
        this.createSimulatedPlotShapeFile(simul2, odir);
        this.createSimulationPlotForestFile(simul2, odir);
        this.createTreeTopFile(simul2, odir);
    }

    public void createTreeTopFile(String simulation, String odir){
        String simul2 = simulation;
        simulation += "_treeTops.shp";

        try {
            this.simulationTreeTopFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating treeTop file");
        }
    }

    public void createSimulationReport(String simulation){

        simulation += ".simul";

        try {
            this.simulationReportFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation);
        }catch (Exception e){
            System.out.println("Error creating simulation report file");
        }
    }
    public void createSimulationReport(String simulation, String odir){

        simulation += ".simul";

        try {
            this.simulationReportFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating simulation report file");
        }
    }
    public void createSimulationPlotForestFile(String simulation){

        simulation += "_volumes.txt";

        try {
            this.simulationPlotForestFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation);
        }catch (Exception e){
            System.out.println("Error creating simulation report file");
        }
    }
    public void createSimulationPlotForestFile(String simulation, String odir){

        simulation += "_volumes.txt";

        try {
            this.simulationPlotForestFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating simulation report file");
        }
    }
    public void createSimulatedPlotFile(String simulation){

        simulation += ".txt";

        try {
            this.simulationPlotFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation);
        }catch (Exception e){
            System.out.println("Error creating simulation plot file");
        }
    }

    public void createSimulatedPlotFile(String simulation, String odir){

        simulation += ".txt";

        try {
            this.simulationPlotFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating simulation plot file");
        }
    }

    public void createSimulatedPlotShapeFile(String simulation){

        simulation += "_plotBoundary.shp";

        try {
            this.simulationPlotShapeFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation);
        }catch (Exception e){
            System.out.println("Error creating simulation plot file");
        }
    }
    public void createSimulatedPlotShapeFile(String simulation, String odir){

        simulation += "_plotBoundary.shp";

        try {
            this.simulationPlotShapeFile = pA.aR.createOutputFileWithExtension(this.plotLASFile, "_" + simulation, odir);
        }catch (Exception e){
            System.out.println("Error creating simulation plot file");
        }
    }
    public void writeSimulationFile(plotSimulator ps, HashSet<Integer> remove, TreeMap<Integer, Integer> add, TreeSet<Integer> plots,
                                    boolean fill, boolean original, int simulationID, HashSet<Integer> keep) throws Exception {

        simulationReportFile.createNewFile();


        writePlotShapeFile(this.simulationPlotShapeFile, simulationID, ps);

        int thread_n = pA.aR.pfac.addReadThread(plotLASReader);

        LASReader tmpReader = new LASReader(new File(plotLASFile.getAbsolutePath()));
        pointWriterMultiThread pw = new pointWriterMultiThread(this.simulationOutputFile, tmpReader, "las2las", pA.aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        pA.aR.pfac.addWriteThread(thread_n, pw, buf);

        LasPoint tempPoint = new LasPoint();
        int tree_id = -1;
        try {
            tree_id = tmpReader.extraBytes_names.get("ITC_id");
        }catch (Exception e){
            throw new toolException("Cannot find ITC_id extra byte VLR.");
        }

        double[] pointxy = new double[2];

        for(long i = 0; i < tmpReader.getNumberOfPointRecords(); i++) {

            tmpReader.readRecord(i, tempPoint);

            int itc_id = tempPoint.getExtraByteInt(tree_id);

            if(!pA.aR.inclusionRule.ask(tempPoint, i, true)){
                continue;
            }

            tempPoint.x += this.simulationOffsetX * simulationLocationCountX;
            tempPoint.y -= this.simulationOffsetY * simulationLocationCountY;

            if(tempPoint.z < pA.aR.z_cutoff) {
                //System.out.println("HERE!! YAY!!");
                pA.aR.pfac.writePoint(tempPoint, i, thread_n);
            }

            if(keep.contains(itc_id)){
                pA.aR.pfac.writePoint(tempPoint, i, thread_n);
            }

            pointxy[0] = tempPoint.x;
            pointxy[1] = tempPoint.y;

            // Setting this to false wil make the results worse!
            if(true)
            if (pointInPlot(pointxy, this.plotBounds_simulated.get(this.plotBounds_simulated.size()-1))) {

                if(itc_id <= 0){
                    pA.aR.pfac.writePoint(tempPoint, i, thread_n);
                }
            }
            /*

                if(tempPoint.z < pA.aR.z_cutoff) {
                    pA.aR.pfac.writePoint(tempPoint, i, thread_n);

                }else if( remove.contains(tempPoint.getExtraByteInt(tree_id)) ){

                    if(original)
                        pA.aR.pfac.writePoint(tempPoint, i, thread_n);
                }else{
                    pA.aR.pfac.writePoint(tempPoint, i, thread_n);
                }

             */
        }

        tmpReader.close();
/*
        for(int i : add.keySet())
            System.out.print(i + " ");
        System.out.println();
        for(int i : plots)
            System.out.print(i + " ");
        System.out.println();
*/
        TreeSet<Integer> dones = new TreeSet<>();
        TreeSet<Integer> dones_2 = new TreeSet<>();

        if(fill)
        for(int p_ : plots){

            LASReader tmpReader2 = new LASReader(new File(pA.plots.get(p_).plotLASFile.getAbsolutePath()));

            for(long i = 0; i < tmpReader2.getNumberOfPointRecords(); i++) {

                tmpReader2.readRecord(i, tempPoint);
                dones_2.add(pA.plots.get(p_).id);

                if (!pA.aR.inclusionRule.ask(tempPoint, i, true)) {
                    continue;
                }
                int itc_id = tempPoint.getExtraByteInt(tree_id);

                //System.out.println(itc_id);


                if (add.containsKey(itc_id)) {

                    dones.add(itc_id);

                    if(tempPoint.z > pA.aR.z_cutoff) {


                        //tempPoint.x += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[0] - tempPoint.x) +
                        //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[0] - tempPoint.x);
                        tempPoint.x += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeX_ITC() - tempPoint.x) +
                                (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeX_ITC()- tempPoint.x);
                        //tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[1] - tempPoint.y) +
                        //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[1] - tempPoint.y);
                        tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeY_ITC() - tempPoint.y) +
                                (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeY_ITC() - tempPoint.y);

                        tempPoint.x += this.simulationOffsetX * simulationLocationCountX;
                        tempPoint.y -= this.simulationOffsetY * simulationLocationCountY;
                        //System.out.println(Arrays.toString((ps.augmentator.ITC_id_to_tree_id.get(itc_id).geometricCenter)));
                        pA.aR.pfac.writePoint(tempPoint, i, thread_n);

                        //System.out.println(this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).getPlotID());
                        /*
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationX1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[0]);
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationX2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[0]);
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationY1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[1]);
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationY2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[1]);
*/
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationX1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeX_ITC());
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationX2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeX_ITC());
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationY1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeY_ITC());
                        this.simulated_plot.get(ps.augmentator.ITC_id_to_tree_id.get(itc_id).getTreeID_unique()).setSimulationTranslationY2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeY_ITC());


                        if(ps.augmentator.ITC_id_to_tree_id.get(itc_id).treesBeneath.size() > 0){
                            for(forestTree tree : ps.augmentator.ITC_id_to_tree_id.get(itc_id).treesBeneath){

                                tree.setSimulationTranslationX1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeX_ITC());
                                tree.setSimulationTranslationX2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeX_ITC());
                                tree.setSimulationTranslationY1(ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeY_ITC());
                                tree.setSimulationTranslationY2(ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeY_ITC());

                            }
                        }

                    }

                }
            }
            tmpReader2.close();
        }
/*
        for(int i : dones)
            System.out.print(i + " ");
        System.out.println();
        for(int i : dones_2)
            System.out.print(i + " ");
        System.out.println();
*/
        writeSimilatedPlotFile(this.simulationPlotFile, simulationID);
        writeTreeTopFile(this.simulationTreeTopFile, simulationID, ps, add);
        pA.aR.pfac.closeThread(thread_n);

    }

    public void writeTreeTopFile(File in, int simulationID, plotSimulator ps, TreeMap<Integer, Integer> add){

        ogr.RegisterAll();
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        driver.Register();

        if(in.exists())
            in.delete();
        // Create the data source
        DataSource dataSource = driver.CreateDataSource(in.getPath(), null);

        if(dataSource == null)
            throw new toolException("Could not create shapefile: " + in.getPath());

        SpatialReference sr = new SpatialReference();
        sr.ImportFromEPSG(3067);

        System.out.println(in.exists());
        // Create a layer


        Layer outShpLayer = dataSource.CreateLayer("points", sr, wkbPoint, null);
        FieldDefn layerFieldDef = new FieldDefn("z",2);
        FieldDefn layerFieldDef_2 = new FieldDefn("id",2);
        outShpLayer.CreateField(layerFieldDef);
        outShpLayer.CreateField(layerFieldDef_2);
        FeatureDefn outShpFeatDefn=outShpLayer.GetLayerDefn();
        Feature outShpFeat = new Feature(outShpFeatDefn);

        for(int i : this.simulated_plot.keySet()){

            if(this.simulated_plot.get(i).hasCrown){

                System.out.println(simulated_plot.get(i).getTreeX_ITC());

                // AAHHHH THIS IS NULL IF THE simulated_plot.get(i) is already in the plot!
                //System.out.println(add.get(simulated_plot.get(i).getTreeITCid()));
                //System.out.println((ps.augmentator.ITC_id_to_tree_id.get(add.get(simulated_plot.get(i).getTreeITCid())).getTreeX_ITC()));

                double translated_x = simulated_plot.get(i).getTreeX_ITC() + (ps.augmentator.ITC_id_to_tree_id.get(add.get(simulated_plot.get(i).getTreeITCid())).getTreeX_ITC() - simulated_plot.get(i).getTreeX_ITC()) +
                        (ps.augmentator.ITC_id_to_tree_id.get((simulated_plot.get(i).getTreeITCid())).getTreeX_ITC() - simulated_plot.get(i).getTreeX_ITC());
                translated_x += this.simulationOffsetX * simulationLocationCountX;

                double translated_y = simulated_plot.get(i).getTreeY_ITC() + (ps.augmentator.ITC_id_to_tree_id.get(add.get(simulated_plot.get(i).getTreeITCid())).getTreeY_ITC() - simulated_plot.get(i).getTreeY_ITC()) +
                        (ps.augmentator.ITC_id_to_tree_id.get((simulated_plot.get(i).getTreeITCid())).getTreeY_ITC() - simulated_plot.get(i).getTreeY_ITC());
                translated_y -= this.simulationOffsetY * simulationLocationCountY;

                //System.out.println("Translated x: " + translated_x + " Translated y: " + translated_y);

                /*
                empPoint.x += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeX_ITC() - tempPoint.x) +
                                (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeX_ITC()- tempPoint.x);
                        //tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[1] - tempPoint.y) +
                        //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[1] - tempPoint.y);
                        tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeY_ITC() - tempPoint.y) +
                                (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeY_ITC() - tempPoint.y);

                        tempPoint.x += this.simulationOffsetX * simulationLocationCountX;
                        tempPoint.y -= this.simulationOffsetY * simulationLocationCountY;
                 */
                Geometry outShpGeom = new Geometry(1);
                outShpGeom.SetPoint(0, translated_x, translated_y);
                outShpFeat.SetField("z",simulated_plot.get(i).getTreeHeight());
                outShpFeat.SetField("id", simulated_plot.get(i).getTreeITCid());

                outShpFeat.SetGeometryDirectly(outShpGeom);
                outShpLayer.CreateFeature(outShpFeat);

            }

        }

        dataSource.FlushCache();


    }

    public void writePlotShapeFile(File in, int simulationID, plotSimulator ps){

        ogr.RegisterAll();
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");
        driver.Register();

        if(in.exists())
            in.delete();
        // Create the data source
        DataSource dataSource = driver.CreateDataSource(in.getPath(), null);

        if(dataSource == null)
            throw new toolException("Could not create shapefile: " + in.getPath());

        SpatialReference sr = new SpatialReference();
        sr.ImportFromEPSG(3067);

        System.out.println(in.exists());
        // Create a layer


        Layer layer = dataSource.CreateLayer("polygons", sr, wkbMultiPolygon, null);

        FieldDefn fieldDefn = new FieldDefn("plot_id", ogr.OFTInteger);
        fieldDefn.SetWidth(32);
        layer.CreateField(fieldDefn);

        Feature feature = new Feature(layer.GetLayerDefn());
        feature.SetField("plot_id", simulationID);
        Geometry polygon = new Geometry(wkbPolygon);
        Geometry polygon2 = new Geometry(wkbLinearRing);
        double[][] plotBounds_ = null;

        if(!this.hasSmallerBounds)
            plotBounds_ = clone2DArray(this.plotBounds);
        else
            plotBounds_ = clone2DArray(this.smallerBounds);

       // System.out.println("Plot bounds: " + plotBounds_.length);

        for(int i = 0; i < plotBounds_.length; i++){
            plotBounds_[i][0] += this.simulationOffsetX * simulationLocationCountX;
            plotBounds_[i][1] -= this.simulationOffsetY * simulationLocationCountY;
        }
        for(int i = 0; i < plotBounds_.length; i++){
            polygon2.AddPoint_2D(plotBounds_[i][0], plotBounds_[i][1]);
        }

        //System.out.println(polygon2.GetPoints().length);
        this.plotBounds_simulated.add(clone2DArray(plotBounds_));

        plotBounds_ = null;

        polygon.AddGeometry(polygon2);
        feature.SetGeometry(polygon);
        // Add the feature to the layer
        layer.CreateFeature(feature);

        // Clean up
        dataSource.FlushCache();
        feature.delete();
        layer.delete();
        dataSource.delete();

        //System.exit(1);
    }

    public void writeSimilatedPlotFile(File in, int simulationID){

        try{
            this.simulationPlotFile.createNewFile();
        }catch (Exception e){
            System.out.println("Error creating simulation plot file");
        }

        double[] treeSpeciesVolumes = new double[3];
        double volume_pine = 0, volume_spruce = 0, volume_decid = 0;

        try {
            //BufferedReader br = new BufferedReader(new FileReader(in));
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.simulationPlotFile));

            //String line = br.readLine();
            int counter = 0;
            int counter2 = 0;

            for(int i : simulated_plot.keySet()){

                if(false)
                if(this.hasSmallerBounds){

                    double translated_x = simulated_plot.get(i).getTreeX() + (simulated_plot.get(i).simulationTranslationX1 - simulated_plot.get(i).getTreeX()) +
                            (simulated_plot.get(i).simulationTranslationX2 - simulated_plot.get(i).getTreeX());
                    translated_x += this.simulationOffsetX * simulationLocationCountX;

                    double translated_y = simulated_plot.get(i).getTreeY() + (simulated_plot.get(i).simulationTranslationY1 - simulated_plot.get(i).getTreeY()) +
                            (simulated_plot.get(i).simulationTranslationY2 - simulated_plot.get(i).getTreeY());
                    //System.out.println("translated_y: " + translated_y);
                    translated_y -= this.simulationOffsetY * simulationLocationCountY;


                    if(simulated_plot.get(i).hasCrown || simulated_plot.get(i).belongsToSomeITC){
                        if (!pointInPolygon(new double[]{translated_x, translated_y}, this.plotBounds_simulated.get(this.plotBounds_simulated.size() - 1))) {

                            //System.out.println("TREE OUTSIDE SMALLER BOUNDS");
                            continue;
                        }
                    }
                        else {
                            if (!pointInPolygon(new double[]{simulated_plot.get(i).getTreeX(), simulated_plot.get(i).getTreeY()}, this.smallerBounds)) {
                                continue;
                            }
                        }
                }else{

                }

                //if(!pointInPlot(new double[]{simulated_plot.get(i).getTreeX(), simulated_plot.get(i).getTreeY()}))
                  //  continue;

                double tree_sim_x = simulated_plot.get(i).getTreeX();
                double tree_sim_x_ITC = simulated_plot.get(i).getTreeX_ITC();
                double tree_sim_y = simulated_plot.get(i).getTreeY();
                double tree_sim_y_ITC = simulated_plot.get(i).getTreeY_ITC();



                if(simulated_plot.get(i).plotID != this.id){
                    tree_sim_x = simulated_plot.get(i).getTreeX() + (simulated_plot.get(i).simulationTranslationX1 - simulated_plot.get(i).getTreeX()) +
                            (simulated_plot.get(i).simulationTranslationX2 - simulated_plot.get(i).getTreeX());
                    tree_sim_x += this.simulationOffsetX * simulationLocationCountX;

                    tree_sim_y = simulated_plot.get(i).getTreeY() + (simulated_plot.get(i).simulationTranslationY1 - simulated_plot.get(i).getTreeY()) +
                            (simulated_plot.get(i).simulationTranslationY2 - simulated_plot.get(i).getTreeY());
                    tree_sim_y -= this.simulationOffsetY * simulationLocationCountY;
                    //System.out.println("HERE!! " + counter++ + " " + simulated_plot.size() );
                    //System.out.println("from_external_plot: " + tree_sim_x + " " + tree_sim_y + " " + simulated_plot.get(i).hasCrown + " " +
                     //       simulated_plot.get(i).getTreeX() + " " + simulated_plot.get(i).getTreeY());

                }else{
                    tree_sim_x += this.simulationOffsetX * simulationLocationCountX;
                    tree_sim_y -= this.simulationOffsetY * simulationLocationCountY;

                    //System.out.println("from_original_plot: " + tree_sim_x + " " + tree_sim_y + " " + simulated_plot.get(i).hasCrown + " " +
                    //        simulated_plot.get(i).getTreeX() + " " + simulated_plot.get(i).getTreeY());

                    counter++;
                }

                if(simulated_plot.get(i).hasCrown)
                    if(simulated_plot.get(i).plotID != this.id){
                        tree_sim_x_ITC = simulated_plot.get(i).getTreeX_ITC() + (simulated_plot.get(i).simulationTranslationX1 - simulated_plot.get(i).getTreeX_ITC()) +
                                (simulated_plot.get(i).simulationTranslationX2 - simulated_plot.get(i).getTreeX_ITC());
                        tree_sim_x_ITC += this.simulationOffsetX * simulationLocationCountX;

                        tree_sim_y_ITC = simulated_plot.get(i).getTreeY_ITC() + (simulated_plot.get(i).simulationTranslationY1 - simulated_plot.get(i).getTreeY_ITC()) +
                                (simulated_plot.get(i).simulationTranslationY2 - simulated_plot.get(i).getTreeY_ITC());
                        tree_sim_y_ITC -= this.simulationOffsetY * simulationLocationCountY;
                        //System.out.println("HERE!! " + counter++ + " " + simulated_plot.size() );
                        //System.out.println("from_external_plot: " + tree_sim_x + " " + tree_sim_y + " " + simulated_plot.get(i).hasCrown + " " +
                        //       simulated_plot.get(i).getTreeX() + " " + simulated_plot.get(i).getTreeY());

                    }else{
                        tree_sim_x_ITC += this.simulationOffsetX * simulationLocationCountX;
                        tree_sim_y_ITC -= this.simulationOffsetY * simulationLocationCountY;

                        //System.out.println("from_original_plot: " + tree_sim_x + " " + tree_sim_y + " " + simulated_plot.get(i).hasCrown + " " +
                        //        simulated_plot.get(i).getTreeX() + " " + simulated_plot.get(i).getTreeY());

                        counter++;
                    }


                if(!simulated_plot.get(i).hasCrown) {
                    if (!pointInPlot(new double[]{tree_sim_x, tree_sim_y}, this.plotBounds_simulated.get(this.plotBounds_simulated.size() - 1)))
                        continue;
                }
                else{
                    if(!pointInPlot(new double[]{tree_sim_x, tree_sim_y}, this.plotBounds_simulated.get(this.plotBounds_simulated.size() - 1)))
                        continue;
                }
                //System.out.println("here2: " + counter2++);

                treeSpeciesVolumes[simulated_plot.get(i).treeSpecies] += simulated_plot.get(i).getTreeVolume();

                String[] outLine = simulated_plot.get(i).getTreeLineFromFieldData_delimited();

                for(int j = 0; j < outLine.length; j++){

                    // X
                    if(simulated_plot.get(i).plotID != this.id) {
                        if (j == 3 && simulated_plot.get(i).hasCrown) {
                            double translated_x = simulated_plot.get(i).getTreeX() + (simulated_plot.get(i).simulationTranslationX1 - simulated_plot.get(i).getTreeX()) +
                                    (simulated_plot.get(i).simulationTranslationX2 - simulated_plot.get(i).getTreeX());
                            translated_x += this.simulationOffsetX * simulationLocationCountX;
                            //System.out.println("translated_x: " + translated_x) ;
                            bw.write(translated_x + "\t");
                        } else if(j == 4 && !simulated_plot.get(i).hasCrown){

                            bw.write(tree_sim_x + "\t");
                        } else if(j == 2){
                            bw.write((10000 + simulationID) + "\t");
                        }
                        else{
                            bw.write(outLine[j] + "\t");
                        }
                    }else{
                        if (j == 3){
                            bw.write(tree_sim_x + "\t");
                        }
                        if (j == 4){
                            bw.write(tree_sim_y + "\t");
                        }
                        if (j == 2){
                            bw.write((10000 + simulationID) + "\t");
                        }
                        else{
                            bw.write(outLine[j] + "\t");
                        }

                    }

                    // Y
                    if(simulated_plot.get(i).plotID != this.id) {
                        if (j == 4 && simulated_plot.get(i).hasCrown) {
                            double translated_y = simulated_plot.get(i).getTreeY() + (simulated_plot.get(i).simulationTranslationY1 - simulated_plot.get(i).getTreeY()) +
                                    (simulated_plot.get(i).simulationTranslationY2 - simulated_plot.get(i).getTreeY());
                            //System.out.println("translated_y: " + translated_y);
                            translated_y -= this.simulationOffsetY * simulationLocationCountY;
                            bw.write(translated_y + "\t");
                        } else if (j == 4 && !simulated_plot.get(i).hasCrown) {

                            bw.write(tree_sim_y + "\t");
                        } else if (j == 2) {
                            bw.write((10000 + simulationID) + "\t");
                        } else {
                            bw.write(outLine[j] + "\t");
                        }
                    }else{
                        if (j == 3){
                            bw.write(tree_sim_x + "\t");
                        }
                        if (j == 4){
                            bw.write(tree_sim_y + "\t");
                        }
                        if (j == 2){
                            bw.write((10000 + simulationID) + "\t");
                        }
                        else{
                            bw.write(outLine[j] + "\t");
                        }
                    }
                }
                bw.write("\n");
                //bw.write(simulated_plot.get(i).getTreeLineFromFieldData() + "\n");
            }

            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            this.simulationPlotForestFile.createNewFile();
        }catch (Exception e){
            System.out.println("Error creating simulation plot (forest data) file");
        }

        double frac = 10000 / this.area;

        if(this.hasSmallerBounds)
            frac = 10000 / this.getSmallerArea();

        try {
            //BufferedReader br = new BufferedReader(new FileReader(in));
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.simulationPlotForestFile));


            bw.write(treeSpeciesVolumes[0]*frac/1000.0 + "\t" +
                    treeSpeciesVolumes[1]*frac/1000.0 + "\t" +
                    treeSpeciesVolumes[2]*frac/1000.0 + "\n");


            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public static double[][] clone2DArray(double[][] original) {
        int rows = original.length;
        double[][] clone = new double[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new double[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }

    private static boolean intersect(double[] a, double[] b, double[] c, double[] d) {
        double denominator = ((b[0] - a[0]) * (d[1] - c[1])) - ((b[1] - a[1]) * (d[0] - c[0]));
        double numerator1 = ((a[1] - c[1]) * (d[0] - c[0])) - ((a[0] - c[0]) * (d[1] - c[1]));
        double numerator2 = ((a[1] - c[1]) * (b[0] - a[0])) - ((a[0] - c[0]) * (b[1] - a[1]));

        if (denominator == 0) {
            return false;
        }

        double r = numerator1 / denominator;
        double s = numerator2 / denominator;

        return (r >= 0 && r <= 1) && (s >= 0 && s <= 1);
    }

    public boolean doPolygonsIntersect(double[][] polygon1, double[][] polygon2) {
        int n1 = polygon1.length;
        int n2 = polygon2.length;

        for (int i = 0; i < n1; i++) {
            int j = (i + 1) % n1;
            for (int k = 0; k < n2; k++) {
                int l = (k + 1) % n2;
                if (intersect(polygon1[i], polygon1[j], polygon2[k], polygon2[l])) {
                    return true;
                }
            }
        }

        return false;
    }


    public void prepareSimulationOffsets(){

        boolean breakLoop = false;



        while(!breakLoop){

            //double[][] plotBounds_ = (double[][])this.plotBounds.clone();

            double[][] plotBounds_ = clone2DArray(this.plotBounds);

            boolean foundIntersect = false;
            this.simulationLocationCountX++;

            for(int i = 0; i < plotBounds_.length; i++){
                plotBounds_[i][0] += this.simulationOffsetX * this.simulationLocationCountX;
                plotBounds_[i][1] -= this.simulationOffsetY * this.simulationLocationCountY;
            }
            for(int p : pA.plots.keySet()){

                if(pA.plots.get(p).id == this.id){
                    continue;
                }

                //System.out.println(pA.plots.get(p).plotBounds_simulated.get(0)[0][0]);
                for(int i = 0; i < pA.plots.get(p).plotBounds_simulated.size(); i++){
                    if(doPolygonsIntersect(plotBounds_, pA.plots.get(p).plotBounds_simulated.get(i))){
                        foundIntersect = true;
                        break;
                    }
                }
                if(!foundIntersect){
                    breakLoop = true;
                }

            }
        }


    }

    public static double calculateDGM(HashMap<Integer, forestTree> trees) {
        // Sort the diameters in ascending order

        double[] treeDiameters = new double[trees.size()];

        int counter = 0;
        for(int tree : trees.keySet()){
            treeDiameters[counter++] = trees.get(tree).getTreeDBH();
        }

        Arrays.sort(treeDiameters);

        // Calculate the total basal area
        double totalBasalArea = 0.0;
        for (double diameter : treeDiameters) {
            double radius = diameter / 2.0;
            double basalArea = Math.PI * radius * radius;
            totalBasalArea += basalArea;
        }

        // Calculate the basal area per tree
        double basalAreaPerTree = totalBasalArea / treeDiameters.length;

        // Find the diameter of the basal area median tree
        double basalAreaSum = 0.0;
        int i = 0;
        while (basalAreaSum < basalAreaPerTree && i < treeDiameters.length) {
            double radius = treeDiameters[i] / 2.0;
            double basalArea = Math.PI * radius * radius;
            basalAreaSum += basalArea;
            i++;
        }
        // Return the diameter of the basal area median tree
        return 2.0 * treeDiameters[i - 1];
    }

    public static double[] getExtent(double[][] polygon) {
        int n = polygon.length;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            minX = Math.min(minX, polygon[i][0]);
            minY = Math.min(minY, polygon[i][1]);
            maxX = Math.max(maxX, polygon[i][0]);
            maxY = Math.max(maxY, polygon[i][1]);
        }

        return new double[] {minX, minY, maxX, maxY};
    }

    public boolean treeInSmallerPolygon(double x, double y){

        return pointInPolygon(new double[]{x, y}, this.smallerBounds);

    }

    public boolean treeInLargerPolygon(double x, double y){

        return pointInPolygon(new double[]{x, y}, this.plotBounds);

    }

    public boolean containsCoordinate(double x, double y, double[][] polygon) {

        double[] point = new double[2];
        point[0] = x;
        point[1] = y;

        //System.out.println(Arrays.toString(point));
        return pointInPolygon(point, polygon);

    }


}
