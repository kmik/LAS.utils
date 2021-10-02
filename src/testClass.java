import utils.Data;
import utils.EigenSet;
import utils.Matrix;

public class testClass {

    public static void main(String[] args) {

        double[][] data = {{3.2, 3.1, 3.3, 3.1}, {3, 6, 6, 3},
                {3, 3, 6, 6}};
        System.out.println("Raw data:");
        Matrix.print(data);
        Data dat = new Data(data);
        dat.center();
        double[][] cov = dat.covarianceMatrix();
        System.out.println("Covariance matrix:");
        Matrix.print(cov);
        EigenSet eigen = dat.getCovarianceEigenSet();
        double[][] vals = {eigen.values};
        System.out.println("Eigenvalues:");
        Matrix.print(vals);
        System.out.println("Corresponding eigenvectors:");
        Matrix.print(eigen.vectors);

        System.out.println(eigen.vectors[2][0]);
        System.out.println(eigen.vectors[2][1]);
        System.out.println(eigen.vectors[2][2]);

        System.out.println("z angle: " + Math.toDegrees(Math.acos(eigen.vectors[2][2]/1.0)));

        System.out.println("Two principal components:");
        Matrix.print(dat.buildPrincipalComponents(1, eigen));
        System.out.println("Principal component transformation:");
        Matrix.print(Data.principalComponentAnalysis(data, 1));

    }
}
