package utils;

public class cloudPoint implements Comparable<cloudPoint> {

    final double x,y,z,t, dz;
    double x_rot = 0,y_rot = 0,z_rot = 0;
    public int id, trunk_id;
    public double prev_yaw = -1, prev_pitch = -1, prev_roll = -1, prev_x = -1, prev_y = -1, prev_z = -1;

    public cloudPoint(double x, double y, double z, double dz, double t, int id, int trunk_id){

        this.x = x;
        this.y = y;
        this.z = z;
        this.t = t;
        this.id = id;
        this.trunk_id = trunk_id;
        this.dz = dz;

        this.x_rot = x;
        this.y_rot = y;
        this.z_rot = z;

    }

    public int compareTo(cloudPoint other) {
        //multiplied to -1 as the author need descending sort order

        return 1 * Double.valueOf(this.z).compareTo(other.z);
    }

    double getX(){
        return this.x;

    }
    double getY(){
        return this.y;
    }
    double getZ(){
        return this.z;
    }
    double getT(){
        return this.t;
    }
    double getdz(){ return this.dz; }
    int getid(){ return this.id; }
    int get_trunkid(){ return this.trunk_id; }

    public void print(){

        System.out.println(x + " " + y + " " + z + " " + t);
        System.out.println(x_rot + " " + y_rot + " " + z_rot + " " + t);

    }

    public cloudPoint copy(){

        return new cloudPoint(this.getX(), this.getY(), this.getZ(), this.getdz(), this.getT(), this.getid(), this.get_trunkid());

    }

}
