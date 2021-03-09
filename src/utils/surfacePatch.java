package utils;

import java.util.ArrayList;

public class surfacePatch {

    public int id;
    public int numberOfNeighborhoods = 0;

    public ArrayList<VoxelNeighborhood> voxels = new ArrayList<>();

    public double x;
    public double y;
    public double z;

    public double maxZ = Double.NEGATIVE_INFINITY;
    public double minZ = Double.POSITIVE_INFINITY;

    public double maxX = Double.NEGATIVE_INFINITY;
    public double minX = Double.POSITIVE_INFINITY;

    public double maxY = Double.NEGATIVE_INFINITY;
    public double minY = Double.POSITIVE_INFINITY;

    public surfacePatch(int id){

        this.id = id;

    }

    public void setId(int id){
        this.id = id;
    }

    public void addVoxelNeighborhood(){

    }

    public void addVoxel(VoxelNeighborhood vn){

        this.voxels.add(vn);

        if(vn.z < this.minZ)
            this.minZ = vn.z;

        if(vn.z > this.maxZ)
            this.maxZ = vn.z;

        if(vn.y < this.minY)
            this.minY = vn.y;

        if(vn.y > this.maxY)
            this.maxY = vn.y;

        if(vn.x < this.minX)
            this.minX = vn.x;

        if(vn.x > this.maxX)
            this.maxX = vn.x;


    }

    public void calculateMidPoint(){

        double sumx = 0;
        double sumy = 0;
        double sumz = 0;


        for(int i = 0; i < voxels.size(); i++){
            sumx += voxels.get(i).x;
            sumy += voxels.get(i).y;
            sumz += voxels.get(i).z;
        }

        this.x = (sumx / (double)voxels.size());
        this.y = (sumy / (double)voxels.size());
        this.z = (sumz / (double)voxels.size());
    }

    public void merge(surfacePatch patch2){

        //System.out.println("merging: " + this.id + " and " + patch2.id);

        for(int i = 0; i < patch2.voxels.size(); i++) {
            patch2.voxels.get(i).setId(this.id);
            this.addVoxel(patch2.voxels.get(i));
        }

    }

    public void renderUseles(){

        for(int i = 0; i < voxels.size(); i++){
            voxels.get(i).stem = false;
            voxels.get(i).isSurface = false;
        }
    }

    public void label(int id){
        for(int i = 0; i < voxels.size(); i++)
            voxels.get(i).setId(id);

    }
}
