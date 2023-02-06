package utils;

import org.cts.CRSFactory;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import tools.createCHM;

import java.io.File;
import java.util.*;


public class Stanford2010 {

    File xml_file;



    public Stanford2010(){

    }

    public void setXMLfile(File file){

        this.xml_file = file;

    }

    public void parse(){

        SAXBuilder builder = new SAXBuilder();

        Document xml = null;
        try {
            xml = builder.build(this.xml_file);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Element root = xml.getRootElement();

        Namespace ns = root.getNamespace();

        System.out.println("Root element of XML document is : " + root.getName());
        System.out.println("Number of books in this XML : " + root.getChildren().size());

        List<Element> lista =  root.getChildren();

        List<Element> machines = root.getChildren("Machine", ns);


        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        src.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        dst.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        CRSFactory crsFactory = new CRSFactory();

        RegistryManager registryManager = crsFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());
        CoordinateReferenceSystem to = null;
        CoordinateReferenceSystem from = null;
        Set<CoordinateOperation> operations;
        CoordinateOperation op = null;

        try {
            to = crsFactory.getCRS("EPSG:3067");
            from = crsFactory.getCRS("EPSG:4326");
            operations = CoordinateOperationFactory
                    .createCoordinateOperations((GeodeticCRS) from, (GeodeticCRS) to);

            Iterator iter = operations.iterator();
            op = (CoordinateOperation) iter.next();
        }catch (Exception e){
            e.printStackTrace();
        }

        double[] transformed = null;


        for(int i = 0; i < machines.size(); i++){

            List<Element> stems = machines.get(i).getChildren("Stem", ns);

            for(int s = 0; s < stems.size(); s++){

                Tree tempTree = new Tree();

                Element stem = stems.get(s);

                List<Element> stem_coordinates = stem.getChildren("StemCoordinates", ns);

                System.out.println(Arrays.toString(stem_coordinates.toArray()));
                double latitude = Double.parseDouble(stem_coordinates.get(0).getChild("Latitude", ns).getValue());
                double longitude = Double.parseDouble(stem_coordinates.get(0).getChild("Longitude", ns).getValue());
                double altitude = Double.parseDouble(stem_coordinates.get(0).getChild("Altitude", ns).getValue());

                System.out.println(latitude + " " + longitude);
                try {
                    transformed = op.transform(new double[]{longitude, latitude});
                }catch (Exception e){
                    e.printStackTrace();
                }

                System.out.println(Arrays.toString(transformed));

                tempTree.setX_coordinate_machine(transformed[0]);
                tempTree.setY_coordinate_machine(transformed[1]);

                Element stemInfo = stem.getChild("Extension", ns).getChild("HPRCMResults", ns).getChild("StemInfo", ns);

                float treeHeight = Float.parseFloat(stemInfo.getChild("TreeHeight",ns).getValue());

                tempTree.setHeight(treeHeight);

                tempTree.setBoomPosition(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomAngle", ns).getValue()));
                tempTree.setBoomExtension(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("BoomExtension", ns).getValue()));
                tempTree.setMachineBearing(Double.parseDouble(stem.getChild("BoomPositioning", ns).getChild("MachineBearing", ns).getValue()));
                tempTree.setDiameter(Float.parseFloat(stem.getChild("SingleTreeProcessedStem", ns).getChild("DBH", ns).getValue()));

                System.out.println(tempTree);
                System.exit(1);
            }
        }

        System.exit(1);

        for(int i = 0; i < lista.size(); i++){

            List<Element> lista_ = lista.get(i).getChildren();

            System.out.println(lista.get(i).getName() + " has " + lista_.size() + " books.");
            System.out.println(Arrays.toString(lista_.toArray()));
            System.out.println(lista_.get(lista_.size()-1).getChildren());
            List<Element> lista__ = lista.get(i).getChildren("Stem", ns);
            System.out.println("Number of stems: " + lista__.size());

        }

    }

}


class Tree {

    public float height, diameter;
    public double x_coordinate_machine, y_coordinate_machine, z_coordinate_machine;
    public double x_coordinate_estimated, y_coordinate_estimated, z_coordinate_estimated;
    public int species, key, id;

    //
    public double boomAngle, boomPosition, boomExtension, machineBearing;
    public Tree(){

    }

    public Tree(float height, float diameter, int species){

        this.height = height;
        this.species = species;
        this.species = species;

    }

    public float getHeight() {
        return height;
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
    }

    public double getY_coordinate_estimated() {
        return y_coordinate_estimated;
    }

    public void setY_coordinate_estimated(double y_coordinate_estimated) {
        this.y_coordinate_estimated = y_coordinate_estimated;
    }

    public double getZ_coordinate_estimated() {
        return z_coordinate_estimated;
    }

    public void setZ_coordinate_estimated(double z_coordinate_estimated) {
        this.z_coordinate_estimated = z_coordinate_estimated;
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

class Stand{

    KdTree forest = new KdTree();
    createCHM.chm canopy_height_model = null;



};

