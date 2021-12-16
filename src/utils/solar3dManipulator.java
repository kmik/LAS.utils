package utils;

public class solar3dManipulator {


    float[][][] space;
    int x_dim, y_dim, z_dim;

    int counter = 0;

    public solar3dManipulator(int x_dim, int y_dim, int z_dim, float[][][] data){

        this.y_dim = y_dim;
        this.x_dim = x_dim;
        this.z_dim = z_dim;

        this.space = data;

    }

    public void setValue(int x, int y, int z, float value){

        this.space[y][x][z] = value;

    }

    public float getValue(int x, int y, int z){

        return space[y][x][z];

    }
}
