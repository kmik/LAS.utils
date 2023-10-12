package utils;

public class Tree {

    public tools.ConcaveHull.Point point = new tools.ConcaveHull.Point(0d,0d);
    public float height, diameter;
    public double x_coordinate_machine, y_coordinate_machine, z_coordinate_machine;
    public double x_coordinate_estimated, y_coordinate_estimated, z_coordinate_estimated;
    public double x_coordinate_optimized, y_coordinate_optimized, z_coordinate_optimized;
    public int species, key, id, global_id;

    public double volume = 0, volume_kuitu = 0, volume_tukki = 0, volume_energia = 0, volume_pikkutukki = 0, volume_unknown = 0;

    public double distanceToStandBorder = -1;
    public int standId = -1;
    public boolean trulyInStand = true;
    //
    public double boomAngle, boomPosition, boomExtension, machineBearing;

    public double[] stemCurveX, stemCurveY;
    public String HPR_FILE_NAME = "";

    public Tree(){

    }

    public Tree(float height, float diameter, int species){

        this.height = height;
        this.species = species;
        this.species = species;

    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getDiameter() {
        return diameter;
    }

    public void setDiameter(float diameter) {
        this.diameter = diameter;
    }

    public double getX_coordinate_machine() {
        return x_coordinate_machine;
    }

    public void setX_coordinate_machine(double x_coordinate_machine) {
        this.x_coordinate_machine = x_coordinate_machine;
    }

    public double getY_coordinate_machine() {
        return y_coordinate_machine;
    }

    public void setY_coordinate_machine(double y_coordinate_machine) {
        this.y_coordinate_machine = y_coordinate_machine;
    }

    public void setPoint(){
        this.point = new tools.ConcaveHull.Point(x_coordinate_machine, y_coordinate_machine);
        //System.out.println("point: " + this.point.toString());
    }

    public tools.ConcaveHull.Point getPoint(){
        return this.point;
    }

    public double getZ_coordinate_machine() {
        return z_coordinate_machine;
    }

    public void setZ_coordinate_machine(double z_coordinate_machine) {
        this.z_coordinate_machine = z_coordinate_machine;
    }

    public int getSpecies() {
        return species;
    }

    public void setSpecies(int species) {
        this.species = species;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX_coordinate_estimated() {
        return x_coordinate_estimated;
    }

    public void setX_coordinate_estimated(double x_coordinate_estimated) {
        this.x_coordinate_estimated = x_coordinate_estimated;
        this.x_coordinate_optimized = x_coordinate_estimated;
    }

    public double getY_coordinate_estimated() {
        return y_coordinate_estimated;
    }

    public void setY_coordinate_estimated(double y_coordinate_estimated) {
        this.y_coordinate_estimated = y_coordinate_estimated;
        this.y_coordinate_optimized = y_coordinate_estimated;

    }

    public double getZ_coordinate_estimated() {
        return z_coordinate_estimated;
    }

    public void setZ_coordinate_estimated(double z_coordinate_estimated) {
        this.z_coordinate_estimated = z_coordinate_estimated;
        this.z_coordinate_optimized = z_coordinate_estimated;
    }

    public void setGlobal_id(int id){

        this.global_id = id;
    }

    public int getGlobal_id(){
        return this.global_id;
    }

    public double getBoomAngle() {
        return boomAngle;
    }

    public void setBoomAngle(double boomAngle) {
        this.boomAngle = boomAngle;
    }

    public double getBoomPosition() {
        return boomPosition;
    }

    public void setBoomPosition(double boomPosition) {
        this.boomPosition = boomPosition;
    }

    public double getBoomExtension() {
        return boomExtension;
    }

    public void setBoomExtension(double boomExtension) {
        this.boomExtension = boomExtension;
    }

    public double getMachineBearing() {
        return machineBearing;
    }

    public void setMachineBearing(double machineBearing) {
        this.machineBearing = machineBearing;
    }

    public KdTree.XYZPoint toXYZPoint(){
        return new KdTree.XYZPoint(this.x_coordinate_machine, this.y_coordinate_machine, this.z_coordinate_machine);

    }

    public KdTree.XYZPoint toXYZPoint_estimatedCoords(){
        return new KdTree.XYZPoint(this.x_coordinate_estimated, this.y_coordinate_estimated, this.z_coordinate_estimated);

    }

    public KdTree.XYZPoint toXYZPoint_estimatedCoords(int id){
        return new KdTree.XYZPoint(this.x_coordinate_estimated, this.y_coordinate_estimated, this.z_coordinate_estimated, id);

    }

    public double[] getStemCurveX() {
        return stemCurveX;
    }

    public void setStemCurveX(double[] stemCurveX) {
        this.stemCurveX = stemCurveX;
    }

    public double[] getStemCurveY() {
        return stemCurveY;
    }

    public void setStemCurveY(double[] stemCurveY) {
        this.stemCurveY = stemCurveY;
    }

    public double euclideanDistanceTo(Tree target){

        double x1 = this.getX_coordinate_machine();
        double y1 = this.getY_coordinate_machine();
        double z1 = this.getZ_coordinate_machine();

        double x2 = target.getX_coordinate_machine();
        double y2 = target.getY_coordinate_machine();
        double z2 = target.getZ_coordinate_machine();

        double distance = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));

        return distance;

    }

    public void estimateHeight(curveFitting fitter){

        this.setHeight((float)fitter.findIntersection(this.stemCurveX, this.stemCurveY, 2));

        if(this.getHeight() <= 0){

            this.setHeight(this.diameter * 1.0f);

        }
    }
    public Tree clone(){

        Tree tree = new Tree();

        tree.setHeight(this.height);
        tree.setDiameter(this.diameter);
        tree.setX_coordinate_machine(this.x_coordinate_machine);
        tree.setY_coordinate_machine(this.y_coordinate_machine);
        tree.setZ_coordinate_machine(this.z_coordinate_machine);
        tree.setX_coordinate_estimated(this.x_coordinate_estimated);
        tree.setY_coordinate_estimated(this.y_coordinate_estimated);
        tree.setZ_coordinate_estimated(this.z_coordinate_estimated);
        tree.setSpecies(this.species);
        tree.setKey(this.key);
        tree.setId(this.id);
        tree.setBoomAngle(this.boomAngle);
        tree.setBoomPosition(this.boomPosition);
        tree.setBoomExtension(this.boomExtension);
        tree.setMachineBearing(this.machineBearing);

        return tree;
    }



    @Override
    public String toString() {
        return "Tree{" +
                "height=" + height +
                ", diameter=" + diameter +
                ", x_coordinate_machine=" + x_coordinate_machine +
                ", y_coordinate_machine=" + y_coordinate_machine +
                ", z_coordinate_machine=" + z_coordinate_machine +
                ", x_coordinate_estimated=" + x_coordinate_estimated +
                ", y_coordinate_estimated=" + y_coordinate_estimated +
                ", z_coordinate_estimated=" + z_coordinate_estimated +
                ", species=" + species +
                ", key=" + key +
                ", id=" + id +
                ", boomAngle=" + boomAngle +
                ", boomPosition=" + boomPosition +
                ", boomExtension=" + boomExtension +
                ", machineBearing=" + machineBearing +
                '}';
    }
};
