package utils;


// Copyright (c) 2005-2007, Luc Maisonobe
// All rights reserved.
//
// Redistribution and use in source and binary forms, with
// or without modification, are permitted provided that
// the following conditions are met:
//
//    Redistributions of source code must retain the
//    above copyright notice, this list of conditions and
//    the following disclaimer.
//    Redistributions in binary form must reproduce the
//    above copyright notice, this list of conditions and
//    the following disclaimer in the documentation
//    and/or other materials provided with the
//    distribution.
//    Neither the names of spaceroots.org, spaceroots.comt
//    nor the names of their contributors may be used to
//    endorse or promote products derived from this
//    software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
// CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
// THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
// USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
// IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.


import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.simple.SimpleBase;
import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
//import org.geotools.renderer.style.PointStyle2D;
import org.nd4j.linalg.eigen.Eigen;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.awt.geom.Point2D;
import org.ejml.data.*;
import org.ejml.interfaces.decomposition.*;

/** Class fitting a circle to a set of points.
 * <p>This class implements the fitting algorithms described in the
 * paper <a
 * href="http://www.spaceroots.org/documents/circle/circle-fitting.pdf">
 * Finding the circle that best fits a set of points</a></p>
 * @author Luc Maisonobe
 */
@SuppressWarnings("FieldCanBeLocal")
public class RANSAC_circleFitter {

    /** Test program entry point.
     * @param args command line arguments
     */

    private double ftol = 1e-12;

    boolean[] inliers = new boolean[1];

    boolean RANSAC = false;
    ArrayList<Integer> indexes;
    int ransac_n;
    double ransac_d;
    ArrayList<Point2D.Double> maybeInliers;
    ArrayList<Point2D.Double> alsoInliers;
    ArrayList<Point2D.Double> trash;

    ArrayList<Point2D.Double> bestObservations;
    ArrayList<Point2D.Double> points;
    Point2D.Double[] initial_candidates;
    ArrayList<Point2D.Double> initial_candidates_list;

    ArrayList<Point2D.Double> ransac_inlier_points;

    ArrayList<cloudPoint> maybeInliers_c;
    ArrayList<double[]> maybeInliers_a;
    ArrayList<cloudPoint> alsoInliers_c;
    ArrayList<double[]> alsoInliers_a;
    ArrayList<cloudPoint> trash_c;

    ArrayList<cloudPoint> bestObservations_c;
    ArrayList<cloudPoint> points_c;
    Point2D.Double[] initial_candidates_c;
    ArrayList<cloudPoint> initial_candidates_list_c;
    ArrayList<double[]> initial_candidates_list_a;

    ArrayList<cloudPoint> ransac_inlier_points_c;
    public ArrayList<double[]> ransac_inlier_points_a;
    ArrayList<Integer> ransac_inlier_points_indexes;

    DMatrixRMaj Z, M, H, H_1;
    SimpleMatrix Zs, Ms, Hs, H_1s;

    int ransac_max_iter = 1000;
    int n, d;
    //double threshold = 10.0;

    Point2D.Double[] maybeinlierpoints;
    Point2D.Double[] bestPoints;

    public double cost_all = 0.0;
    public double radius_ransac = 0.0;
    public double radius = 0.0;
    public double minCost = Double.POSITIVE_INFINITY;
    public double center_x, center_y;
    CircleFitter fitter = new CircleFitter();
    double cost = 0;

    public boolean ransacFailed = false;

    double maxRadius = 0.25;
    double minRadius = 0.025;

    public ArrayList<Double> costList = new ArrayList<>();

    /** Build a new instance with a default current circle.
     */
    public RANSAC_circleFitter() {

        center = new Point2D.Double(0.0, 0.0);
        rHat   = 1.0;
        points = null;
        points_list = null;

        maybeInliers = new ArrayList<>();
        alsoInliers = new ArrayList<>();
        initial_candidates_list = new ArrayList<>();
        trash = new ArrayList<>();

        maybeInliers_c = new ArrayList<>();
        maybeInliers_a = new ArrayList<>();
        alsoInliers_a = new ArrayList<>();
        alsoInliers_c = new ArrayList<>();
        initial_candidates_list_a = new ArrayList<>();
        initial_candidates_list_c = new ArrayList<>();
        trash_c = new ArrayList<>();
    }

    public void setMaxRadius(double maxRad){

        this.maxRadius = maxRad;

    }

    public void setMinRadius(double minRad){
        this.minRadius = minRad;
    }

    /** Initialize an approximate circle based on all triplets.
     * @param points circular ring sample points
     * @exception LocalException if all points are aligned
     */
    public void initialize(Point2D.Double[] points, int n, int d, double dist, int ransac_max_iter)
            throws LocalException {

        this.ransac_max_iter = ransac_max_iter;

        this.points = new ArrayList<>();
        double mean_x = 0.0d, mean_y = 0.0d;

        for(int i = 0; i < points.length; i++){
            this.points.add(points[i]);
            mean_x += points[i].x;
            mean_y += points[i].y;
        }

        mean_x /= (double)points.length;
        mean_y /= (double)points.length;

        int i,iter,IterMAX=99;

        double Xi,Yi,Zi;
        double Mz = 0,Mxy = 0,Mxx = 0,Myy = 0,Mxz = 0,Myz = 0,Mzz = 0,Cov_xy = 0,Var_z = 0;
        double A0 = 0,A1 = 0,A2 = 0,A22 = 0;
        double Dy = 0,xnew = 0,x = 0,ynew = 0,y = 0;
        double DET = 0,Xcenter = 0,Ycenter = 0;

        for (i=0; i<this.points.size(); i++)
        {
            Xi = this.points.get(i).x - mean_x;   //  centered x-coordinates
            Yi = this.points.get(i).y - mean_y;   //  centered y-coordinates
            Zi = Xi*Xi + Yi*Yi;

            Mxy += Xi*Yi;
            Mxx += Xi*Xi;
            Myy += Yi*Yi;
            Mxz += Xi*Zi;
            Myz += Yi*Zi;
            Mzz += Zi*Zi;
        }

        Mxx /= (double)this.points.size();
        Myy /= (double)this.points.size();
        Mxy /= (double)this.points.size();
        Mxz /= (double)this.points.size();
        Myz /= (double)this.points.size();
        Mzz /= (double)this.points.size();

        Mz = Mxx + Myy;
        Cov_xy = Mxx*Myy - Mxy*Mxy;
        Var_z = Mzz - Mz*Mz;

        A2 = 4.0*Cov_xy - 3.0*Mz*Mz - Mzz;
        A1 = Var_z*Mz + 4.0*Cov_xy*Mz - Mxz*Mxz - Myz*Myz;
        A0 = Mxz*(Mxz*Myy - Myz*Mxy) + Myz*(Myz*Mxx - Mxz*Mxy) - Var_z*Cov_xy;
        A22 = A2 + A2;

        for (x=0.,y=A0,iter=0; iter<IterMAX; iter++)  // usually, 4-6 iterations are enough
        {
            Dy = A1 + x*(A22 + 16.*x*x);
            xnew = x - y/Dy;
            if ((xnew == x)||(!Double.isInfinite(xnew))) break;
            ynew = A0 + xnew*(A1 + xnew*(A2 + 4.0*xnew*xnew));
            if (Math.abs(ynew)>=Math.abs(y))  break;
            x = xnew;  y = ynew;
        }


        DET = x*x - x*Mz + Cov_xy;
        Xcenter = (Mxz*(Myy - x) - Myz*Mxy)/DET/2.0;
        Ycenter = (Myz*(Mxx - x) - Mxz*Mxy)/DET/2.0;

//       assembling the output

        this.center_x = Xcenter + mean_x;
        this.center_y = Ycenter + mean_y;
        this.radius = Math.sqrt(Xcenter*Xcenter + Ycenter*Ycenter + Mz - x - x);
        this.cost = 0;
        double distance = 0;

        for(i = 0; i < this.points.size(); i++){

            distance = Math.abs(euclideanDistance(this.points.get(i).x, this.points.get(i).y, this.center_x, this.center_y) - this.radius);
            cost += distance;

        }

        this.cost /= this.points.size();


    }

    public void initialize_arraylist(ArrayList<Point2D.Double> points, int n, int d, double dist, int ransac_max_iter)
            throws LocalException {

        this.ransac_max_iter = ransac_max_iter;

        //points = points;

        double mean_x = 0.0d, mean_y = 0.0d;

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i).x;
            mean_y += points.get(i).y;
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        int i,iter,IterMAX=99;

        double Xi,Yi,Zi;
        double Mz = 0,Mxy = 0,Mxx = 0,Myy = 0,Mxz = 0,Myz = 0,Mzz = 0,Cov_xy = 0,Var_z = 0;
        double A0 = 0,A1 = 0,A2 = 0,A22 = 0;
        double Dy = 0,xnew = 0,x = 0,ynew = 0,y = 0;
        double DET = 0,Xcenter = 0,Ycenter = 0;

        for (i=0; i<points.size(); i++)
        {
            Xi = points.get(i).x - mean_x;   //  centered x-coordinates
            Yi = points.get(i).y - mean_y;   //  centered y-coordinates
            Zi = Xi*Xi + Yi*Yi;

            Mxy += Xi*Yi;
            Mxx += Xi*Xi;
            Myy += Yi*Yi;
            Mxz += Xi*Zi;
            Myz += Yi*Zi;
            Mzz += Zi*Zi;
        }

        Mxx /= (double)points.size();
        Myy /= (double)points.size();
        Mxy /= (double)points.size();
        Mxz /= (double)points.size();
        Myz /= (double)points.size();
        Mzz /= (double)points.size();

        Mz = Mxx + Myy;
        Cov_xy = Mxx*Myy - Mxy*Mxy;
        Var_z = Mzz - Mz*Mz;

        A2 = 4.0*Cov_xy - 3.0*Mz*Mz - Mzz;
        A1 = Var_z*Mz + 4.0*Cov_xy*Mz - Mxz*Mxz - Myz*Myz;
        A0 = Mxz*(Mxz*Myy - Myz*Mxy) + Myz*(Myz*Mxx - Mxz*Mxy) - Var_z*Cov_xy;
        A22 = A2 + A2;

        for (x=0.,y=A0,iter=0; iter<IterMAX; iter++)  // usually, 4-6 iterations are enough
        {
            Dy = A1 + x*(A22 + 16.*x*x);
            xnew = x - y/Dy;
            if ((xnew == x)||(!Double.isInfinite(xnew))) break;
            ynew = A0 + xnew*(A1 + xnew*(A2 + 4.0*xnew*xnew));
            if (Math.abs(ynew)>=Math.abs(y))  break;
            x = xnew;  y = ynew;
        }


        DET = x*x - x*Mz + Cov_xy;
        Xcenter = (Mxz*(Myy - x) - Myz*Mxy)/DET/2.0;
        Ycenter = (Myz*(Mxx - x) - Mxz*Mxy)/DET/2.0;

//       assembling the output

        this.center_x = Xcenter + mean_x;
        this.center_y = Ycenter + mean_y;
        this.radius = Math.sqrt(Xcenter*Xcenter + Ycenter*Ycenter + Mz - x - x);
        this.cost = 0;
        double distance = 0;

        for(i = 0; i < points.size(); i++){

            distance = Math.abs(euclideanDistance(points.get(i).x, points.get(i).y, this.center_x, this.center_y) - this.radius);

            //System.out.println("distance in: " + distance);
            this.cost += distance;

        }

        this.cost /= points.size();

        //System.out.println("this.cost: " + this.cost);
    }

    public void initialize_arraylist_c(ArrayList<cloudPoint> points, int n, int d, double dist, int ransac_max_iter)
            throws LocalException {


        this.costList.clear();
        this.ransac_max_iter = ransac_max_iter;

        //points = points;

        double mean_x = 0.0d, mean_y = 0.0d;

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i).x_rot;
            mean_y += points.get(i).y_rot;
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        int i,iter,IterMAX=99;

        double Xi,Yi,Zi;
        double Mz = 0,Mxy = 0,Mxx = 0,Myy = 0,Mxz = 0,Myz = 0,Mzz = 0,Cov_xy = 0,Var_z = 0;
        double A0 = 0,A1 = 0,A2 = 0,A22 = 0;
        double Dy = 0,xnew = 0,x = 0,ynew = 0,y = 0;
        double DET = 0,Xcenter = 0,Ycenter = 0;

        for (i=0; i<points.size(); i++)
        {
            Xi = points.get(i).x_rot - mean_x;   //  centered x-coordinates
            Yi = points.get(i).y_rot - mean_y;   //  centered y-coordinates
            Zi = Xi*Xi + Yi*Yi;

            Mxy += Xi*Yi;
            Mxx += Xi*Xi;
            Myy += Yi*Yi;
            Mxz += Xi*Zi;
            Myz += Yi*Zi;
            Mzz += Zi*Zi;
        }

        Mxx /= (double)points.size();
        Myy /= (double)points.size();
        Mxy /= (double)points.size();
        Mxz /= (double)points.size();
        Myz /= (double)points.size();
        Mzz /= (double)points.size();

        Mz = Mxx + Myy;
        Cov_xy = Mxx*Myy - Mxy*Mxy;
        Var_z = Mzz - Mz*Mz;

        A2 = 4.0*Cov_xy - 3.0*Mz*Mz - Mzz;
        A1 = Var_z*Mz + 4.0*Cov_xy*Mz - Mxz*Mxz - Myz*Myz;
        A0 = Mxz*(Mxz*Myy - Myz*Mxy) + Myz*(Myz*Mxx - Mxz*Mxy) - Var_z*Cov_xy;
        A22 = A2 + A2;

        for (x=0.,y=A0,iter=0; iter<IterMAX; iter++)  // usually, 4-6 iterations are enough
        {
            Dy = A1 + x*(A22 + 16.*x*x);
            xnew = x - y/Dy;
            if ((xnew == x)||(!Double.isInfinite(xnew))) break;
            ynew = A0 + xnew*(A1 + xnew*(A2 + 4.0*xnew*xnew));
            if (Math.abs(ynew)>=Math.abs(y))  break;
            x = xnew;  y = ynew;
        }


        DET = x*x - x*Mz + Cov_xy;
        Xcenter = (Mxz*(Myy - x) - Myz*Mxy)/DET/2.0;
        Ycenter = (Myz*(Mxx - x) - Mxz*Mxy)/DET/2.0;

//       assembling the output

        this.center_x = Xcenter + mean_x;
        this.center_y = Ycenter + mean_y;
        this.radius = Math.sqrt(Xcenter*Xcenter + Ycenter*Ycenter + Mz - x - x);
        this.cost = 0;
        double distance = 0;

        for(i = 0; i < points.size(); i++){

            distance = Math.abs(euclideanDistance(points.get(i).x_rot, points.get(i).y_rot, this.center_x, this.center_y) - this.radius);

            //System.out.println("distance in: " + distance);
            this.cost += distance;
            costList.add(distance);

        }

        this.cost /= points.size();

        //System.out.println("this.cost: " + this.cost);
    }


    public void initialize_arraylist_a(ArrayList<double[]> points, int n, int d, double dist, int ransac_max_iter)
            throws LocalException {


        this.costList.clear();
        this.ransac_max_iter = ransac_max_iter;

        //points = points;

        double mean_x = 0.0d, mean_y = 0.0d;

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        int i,iter,IterMAX=99;

        double Xi,Yi,Zi;
        double Mz = 0,Mxy = 0,Mxx = 0,Myy = 0,Mxz = 0,Myz = 0,Mzz = 0,Cov_xy = 0,Var_z = 0;
        double A0 = 0,A1 = 0,A2 = 0,A22 = 0;
        double Dy = 0,xnew = 0,x = 0,ynew = 0,y = 0;
        double DET = 0,Xcenter = 0,Ycenter = 0;

        for (i=0; i<points.size(); i++)
        {
            Xi = points.get(i)[0] - mean_x;   //  centered x-coordinates
            Yi = points.get(i)[1] - mean_y;   //  centered y-coordinates
            Zi = Xi*Xi + Yi*Yi;

            Mxy += Xi*Yi;
            Mxx += Xi*Xi;
            Myy += Yi*Yi;
            Mxz += Xi*Zi;
            Myz += Yi*Zi;
            Mzz += Zi*Zi;
        }

        Mxx /= (double)points.size();
        Myy /= (double)points.size();
        Mxy /= (double)points.size();
        Mxz /= (double)points.size();
        Myz /= (double)points.size();
        Mzz /= (double)points.size();

        Mz = Mxx + Myy;
        Cov_xy = Mxx*Myy - Mxy*Mxy;
        Var_z = Mzz - Mz*Mz;

        A2 = 4.0*Cov_xy - 3.0*Mz*Mz - Mzz;
        A1 = Var_z*Mz + 4.0*Cov_xy*Mz - Mxz*Mxz - Myz*Myz;
        A0 = Mxz*(Mxz*Myy - Myz*Mxy) + Myz*(Myz*Mxx - Mxz*Mxy) - Var_z*Cov_xy;
        A22 = A2 + A2;

        for (x=0.,y=A0,iter=0; iter<IterMAX; iter++)  // usually, 4-6 iterations are enough
        {
            Dy = A1 + x*(A22 + 16.*x*x);
            xnew = x - y/Dy;
            if ((xnew == x)||(!Double.isInfinite(xnew))) break;
            ynew = A0 + xnew*(A1 + xnew*(A2 + 4.0*xnew*xnew));
            if (Math.abs(ynew)>=Math.abs(y))  break;
            x = xnew;  y = ynew;
        }


        DET = x*x - x*Mz + Cov_xy;
        Xcenter = (Mxz*(Myy - x) - Myz*Mxy)/DET/2.0;
        Ycenter = (Myz*(Mxx - x) - Mxz*Mxy)/DET/2.0;

//       assembling the output

        this.center_x = Xcenter + mean_x;
        this.center_y = Ycenter + mean_y;
        this.radius = Math.sqrt(Xcenter*Xcenter + Ycenter*Ycenter + Mz - x - x);
        this.cost = 0;
        double distance = 0;

        for(i = 0; i < points.size(); i++){

            distance = Math.abs(euclideanDistance(points.get(i)[0], points.get(i)[1], this.center_x, this.center_y) - this.radius);

            //System.out.println("distance in: " + distance);
            this.cost += distance;
            costList.add(distance);

        }

        this.cost /= (double)points.size();

        //System.out.println("this.cost: " + this.cost);
    }


    public void initialize_arraylist_custom(ArrayList<double[]> points, int n, int d, double dist, int ransac_max_iter)
            throws LocalException {


        this.costList.clear();
        this.ransac_max_iter = ransac_max_iter;

        //points = points;

        double mean_x = 0.0d, mean_y = 0.0d;

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        int i,iter,IterMAX=99;

        double Xi,Yi,Zi;
        double Mz = 0,Mxy = 0,Mxx = 0,Myy = 0,Mxz = 0,Myz = 0,Mzz = 0,Cov_xy = 0,Var_z = 0;
        double A0 = 0,A1 = 0,A2 = 0,A22 = 0;
        double Dy = 0,xnew = 0,x = 0,ynew = 0,y = 0;
        double DET = 0,Xcenter = 0,Ycenter = 0;

        for (i=0; i<points.size(); i++)
        {
            Xi = points.get(i)[0] - mean_x;   //  centered x-coordinates
            Yi = points.get(i)[1] - mean_y;   //  centered y-coordinates
            Zi = Xi*Xi + Yi*Yi;

            Mxy += Xi*Yi;
            Mxx += Xi*Xi;
            Myy += Yi*Yi;
            Mxz += Xi*Zi;
            Myz += Yi*Zi;
            Mzz += Zi*Zi;
        }

        Mxx /= (double)points.size();
        Myy /= (double)points.size();
        Mxy /= (double)points.size();
        Mxz /= (double)points.size();
        Myz /= (double)points.size();
        Mzz /= (double)points.size();

        Mz = Mxx + Myy;
        Cov_xy = Mxx*Myy - Mxy*Mxy;
        Var_z = Mzz - Mz*Mz;

        A2 = 4.0*Cov_xy - 3.0*Mz*Mz - Mzz;
        A1 = Var_z*Mz + 4.0*Cov_xy*Mz - Mxz*Mxz - Myz*Myz;
        A0 = Mxz*(Mxz*Myy - Myz*Mxy) + Myz*(Myz*Mxx - Mxz*Mxy) - Var_z*Cov_xy;
        A22 = A2 + A2;

        for (x=0.,y=A0,iter=0; iter<IterMAX; iter++)  // usually, 4-6 iterations are enough
        {
            Dy = A1 + x*(A22 + 16.*x*x);
            xnew = x - y/Dy;
            if ((xnew == x)||(!Double.isInfinite(xnew))) break;
            ynew = A0 + xnew*(A1 + xnew*(A2 + 4.0*xnew*xnew));
            if (Math.abs(ynew)>=Math.abs(y))  break;
            x = xnew;  y = ynew;
        }


        DET = x*x - x*Mz + Cov_xy;
        Xcenter = (Mxz*(Myy - x) - Myz*Mxy)/DET/2.0;
        Ycenter = (Myz*(Mxx - x) - Mxz*Mxy)/DET/2.0;

//       assembling the output

        this.center_x = Xcenter + mean_x;
        this.center_y = Ycenter + mean_y;
        this.radius = Math.sqrt(Xcenter*Xcenter + Ycenter*Ycenter + Mz - x - x);
        this.cost = 0;
        double distance = 0;

        for(i = 0; i < points.size(); i++){

            distance = Math.abs(euclideanDistance(points.get(i)[0], points.get(i)[1], this.center_x, this.center_y) - this.radius);

            //System.out.println("distance in: " + distance);
            this.cost += distance;
            costList.add(distance);

        }

        this.cost /= points.size();

        //System.out.println("this.cost: " + this.cost);
    }

    public void initialize2(ArrayList<Point2D.Double> points, int n, int d, double threshold, int ransac_max_iter, int round){

        double distance = 0.0;

        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        double center_x_ransac = 0, center_y_ransac = 0;

        ArrayList<Point2D.Double> points_orig = (ArrayList<Point2D.Double>)points.clone();



        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i).x;
            mean_y += points.get(i).y;
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();


        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers.clear();
            alsoInliers.clear();
            initial_candidates_list.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list.add(pullRandomElement(points_orig,rand));
            }

            try {
                this.initialize_arraylist(initial_candidates_list, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            for(int j = 0; j < points_orig.size(); j++){

                distance = Math.abs(euclideanDistance(points_orig.get(j).x, points_orig.get(j).y, this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );
                if(distance < threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers.add(points_orig.get(j));
                }

            }

            points_orig.addAll(initial_candidates_list);
            //System.out.println(points_orig.size() + " ?==? " + count);


            if(alsoInliers.size() > d){

                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers.addAll(alsoInliers);
                maybeInliers.addAll(initial_candidates_list);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist(maybeInliers, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);
                if(cost < minCost){
                   // boolean converged = ftol*minCost >= minCost-cost;

                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points = (ArrayList<Point2D.Double>) maybeInliers.clone();

                    //if(converged)
                      //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < points.size(); j++){

                distance = Math.abs(euclideanDistance(points.get(j).x, points.get(j).y, this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)points.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            //System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }

    public void initialize3(ArrayList<cloudPoint> points, int n, int d, double threshold, int ransac_max_iter, int round){

        double distance = 0.0;
        ransac_inlier_points_c = new ArrayList<>();
        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        double center_x_ransac = 0, center_y_ransac = 0;

        ArrayList<cloudPoint> points_orig = (ArrayList<cloudPoint>)points.clone();



        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i).x_rot;
            mean_y += points.get(i).y_rot;
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();


        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers_c.clear();
            alsoInliers_c.clear();
            initial_candidates_list_c.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list_c.add(pullRandomElement_c(points_orig,rand));
            }

            try {
                this.initialize_arraylist_c(initial_candidates_list_c, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            for(int j = 0; j < points_orig.size(); j++){

                distance = Math.abs(euclideanDistance(points_orig.get(j).x_rot, points_orig.get(j).y_rot, this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );
                if(distance < threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers_c.add(points_orig.get(j));
                }

            }

            points_orig.addAll(initial_candidates_list_c);
            //System.out.println(points_orig.size() + " ?==? " + count);


            if(alsoInliers_c.size() > d){

                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers_c.addAll(alsoInliers_c);
                maybeInliers_c.addAll(initial_candidates_list_c);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist_c(maybeInliers_c, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);



                if(cost < minCost && radius > minRadius && radius < maxRadius){
                    // boolean converged = ftol*minCost >= minCost-cost;

                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points_c = (ArrayList<cloudPoint>) maybeInliers_c.clone();

                    //if(converged)
                    //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < points.size(); j++){

                distance = Math.abs(euclideanDistance(points.get(j).x_rot, points.get(j).y_rot, this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)points.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist_c(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }

    public void initialize6(ArrayList<double[]> points, int n, int d, double threshold, int ransac_max_iter, int round, int seed){

        double distance = 0.0;
        ransac_inlier_points_a = new ArrayList<>();
        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        int max_d = -1;
        double minimumDiameter = Double.POSITIVE_INFINITY;
        double center_x_ransac = 0, center_y_ransac = 0;

        ArrayList<double[]> points_orig = (ArrayList<double[]>)points.clone();

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        double fraction_of_too_many_inside = 0.2;


        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();

        if(seed != -1)
            rand.setSeed(seed);

        ransacFailed = false;

        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers_a.clear();
            alsoInliers_a.clear();
            initial_candidates_list_a.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list_a.add(pullRandomElement_a(points_orig,rand));
            }

            try {
                this.initialize_arraylist_a(initial_candidates_list_a, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            boolean inside = false;
            int count_inside = 0;
            int count_all = 0;

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            for(int j = 0; j < points_orig.size(); j++){

                distance = (euclideanDistance(points_orig.get(j)[0], points_orig.get(j)[1], this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );

                if(distance < 0) {
                    inside = true;
                    distance *= -1.0;
                }else
                    inside = false;

                if(distance <= threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers_a.add(points_orig.get(j));
                }else{
                    if(inside){
                        count_inside++;

                    }
                    count_all++;
                }

            }

            double fractionInside = (double)count_inside / (double)count_all;

            points_orig.addAll(initial_candidates_list_a);
            //System.out.println(points_orig.size() + " ?==? " + count);


            if(alsoInliers_a.size() > d && fractionInside <= fraction_of_too_many_inside){

                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers_a.addAll(alsoInliers_a);
                maybeInliers_a.addAll(initial_candidates_list_a);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist_a(maybeInliers_a, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);
                /*
                if(maybeInliers_a.size() >= max_d){
                    max_d = maybeInliers_a.size();
                }
                else{
                    continue;
                }


*/
                //cost = cost * (1.0 - (double)maybeInliers_a.size() / (double)points.size());

                //if( (cost < minCost && this.radius < minimumDiameter) || this.radius < minimumDiameter){
                if(cost <= minCost && radius > minRadius && radius < maxRadius){
                    minimumDiameter = this.radius;
                    // boolean converged = ftol*minCost >= minCost-cost;

                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points_a = (ArrayList<double[]>) maybeInliers_a.clone();

                    //if(converged)
                    //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < ransac_inlier_points_a.size(); j++){

                distance = Math.abs(euclideanDistance(ransac_inlier_points_a.get(j)[0], ransac_inlier_points_a.get(j)[1], this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)ransac_inlier_points_a.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            //System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist_a(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.ransacFailed = true;
            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }

    public void initialize4(ArrayList<double[]> points, int n, int d, double threshold, int ransac_max_iter, int round, int seed){

        double distance = 0.0;
        ransac_inlier_points_a = new ArrayList<>();
        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        int max_d = -1;
        double minimumDiameter = Double.POSITIVE_INFINITY;
        double center_x_ransac = 0, center_y_ransac = 0;

        ArrayList<double[]> points_orig = (ArrayList<double[]>)points.clone();

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();

        if(seed != -1)
            rand.setSeed(seed);

        ransacFailed = false;

        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers_a.clear();
            alsoInliers_a.clear();
            initial_candidates_list_a.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list_a.add(pullRandomElement_a(points_orig,rand));
            }

            try {
                this.initialize_arraylist_a(initial_candidates_list_a, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            for(int j = 0; j < points_orig.size(); j++){

                distance = Math.abs(euclideanDistance(points_orig.get(j)[0], points_orig.get(j)[1], this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );
                if(distance <= threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers_a.add(points_orig.get(j));
                }

            }

            points_orig.addAll(initial_candidates_list_a);
            //System.out.println(points_orig.size() + " ?==? " + count);


            if(alsoInliers_a.size() > d){

                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers_a.addAll(alsoInliers_a);
                maybeInliers_a.addAll(initial_candidates_list_a);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist_a(maybeInliers_a, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);
                /*
                if(maybeInliers_a.size() >= max_d){
                    max_d = maybeInliers_a.size();
                }
                else{
                    continue;
                }


*/
                //cost = cost * (1.0 - (double)maybeInliers_a.size() / (double)points.size());

                if( (cost < minCost && this.radius < minimumDiameter) || this.radius < minimumDiameter && radius > minRadius && radius < maxRadius){
                //if(cost <= minCost){
                    minimumDiameter = this.radius;
                    // boolean converged = ftol*minCost >= minCost-cost;

                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points_a = (ArrayList<double[]>) maybeInliers_a.clone();

                    //if(converged)
                    //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < ransac_inlier_points_a.size(); j++){

                distance = Math.abs(euclideanDistance(ransac_inlier_points_a.get(j)[0], ransac_inlier_points_a.get(j)[1], this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)ransac_inlier_points_a.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            //System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist_a(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.ransacFailed = true;
            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }

    public void initialize5(ArrayList<double[]> points, int n, int d, double threshold, int ransac_max_iter, int round, int seed){

        double distance = 0.0;
        ransac_inlier_points_a = new ArrayList<>();
        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        inliers = new boolean[points.size()];

        int n_inliers = 0;

        int max_d = -1;
        double minimumDiameter = Double.POSITIVE_INFINITY;
        double center_x_ransac = 0, center_y_ransac = 0;

        double fraction_of_too_many_inside = 0.33;

        ArrayList<double[]> points_orig = (ArrayList<double[]>)points.clone();

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();

        if(seed != -1)
            rand.setSeed(seed);

        ransacFailed = false;

        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers_a.clear();
            alsoInliers_a.clear();
            initial_candidates_list_a.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list_a.add(pullRandomElement_a(points_orig,rand));
            }

            try {
                this.initialize_arraylist_a(initial_candidates_list_a, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            boolean inside = false;
            int count_inside = 0;
            int count_all = 0;

            for(int j = 0; j < points_orig.size(); j++){

                distance = (euclideanDistance(points_orig.get(j)[0], points_orig.get(j)[1], this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );

                if(distance < 0) {
                    inside = true;
                    distance *= -1.33;
                }else
                    inside = false;

                if(distance <= threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers_a.add(points_orig.get(j));
                }else{
                    if(inside){
                        count_inside++;

                    }
                    count_all++;
                }

            }

            double fractionInside = (double)count_inside / (double)count_all;

            points_orig.addAll(initial_candidates_list_a);
            //System.out.println(points_orig.size() + " ?==? " + count);

            //System.out.println("fraction: " + fractionInside);

            if(alsoInliers_a.size() > d && fractionInside <= fraction_of_too_many_inside){
                //System.out.println("fractionInside: " + fractionInside);
                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers_a.addAll(alsoInliers_a);
                maybeInliers_a.addAll(initial_candidates_list_a);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist_a(maybeInliers_a, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);
                /*
                if(maybeInliers_a.size() >= max_d){
                    max_d = maybeInliers_a.size();
                }
                else{
                    continue;
                }


*/
                //cost = cost * (1.0 - (double)maybeInliers_a.size() / (double)points.size());

                //if( (cost < minCost && this.radius < minimumDiameter) || this.radius < minimumDiameter){
                if(maybeInliers_a.size() >= n_inliers && radius > minRadius && radius < maxRadius){

                    if(maybeInliers_a.size() == n_inliers)
                        if(cost >minCost)
                            continue;

                    n_inliers = maybeInliers_a.size();
                    minimumDiameter = this.radius;
                    // boolean converged = ftol*minCost >= minCost-cost;

                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points_a.clear();
                    this.ransac_inlier_points_a.addAll(maybeInliers_a);

                    //if(converged)
                    //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < ransac_inlier_points_a.size(); j++){

                distance = Math.abs(euclideanDistance(ransac_inlier_points_a.get(j)[0], ransac_inlier_points_a.get(j)[1], this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)ransac_inlier_points_a.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            //System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist_a(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.ransacFailed = true;
            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }

    public void initialize7(ArrayList<double[]> points, int n, int d, double threshold, int ransac_max_iter, int round, int seed){

        double distance = 0.0;
        ransac_inlier_points_a = new ArrayList<>();
        double min_radius = 0;
        double mean_x = 0.0d, mean_y = 0.0d;

        inliers = new boolean[points.size()];

        int n_inliers = 0;

        int max_d = -1;
        double minimumDiameter = Double.POSITIVE_INFINITY;
        double center_x_ransac = 0, center_y_ransac = 0;

        int minPointsInsideCircle = Integer.MAX_VALUE;


        ArrayList<double[]> points_orig = (ArrayList<double[]>)points.clone();

        for(int i = 0; i < points.size(); i++){
            //this.points.add(points[i]);
            mean_x += points.get(i)[0];
            mean_y += points.get(i)[1];
        }

        mean_x /= (double)points.size();
        mean_y /= (double)points.size();

        minCost = Double.POSITIVE_INFINITY;
        boolean solution_found = false;

        Random rand = new Random();

        if(seed != -1)
            rand.setSeed(seed);

        ransacFailed = false;

        for(int i = 0; i < ransac_max_iter; i++) {

            //Collections.shuffle(points);

            maybeInliers_a.clear();
            alsoInliers_a.clear();
            initial_candidates_list_a.clear();
            //maybeInliers.clear();
            //maybeGoodIndexes = new boolean[points.size()];

            for(int j = 0; j < n; j++){
                //initial_candidates[j] = points.get(j);
                //initial_candidates_list.add(points.get(j));

                initial_candidates_list_a.add(pullRandomElement_a(points_orig,rand));
            }

            try {
                this.initialize_arraylist_a(initial_candidates_list_a, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){

                e.printStackTrace();
                continue;
            }

            //System.out.println(initial_candidates_list.size() + " " + this.radius);
            int count = 0;
            for(int j = 0; j < points_orig.size(); j++){

                distance = (euclideanDistance(points_orig.get(j)[0], points_orig.get(j)[1], this.center_x, this.center_y) - this.radius);
                //System.out.println("GOT HERE!! " + distance );

                if(distance < 0)
                    distance *= -1.0;

                if(distance <= threshold) {
                    //alsoInliers.add(points.get(j));
                    count++;
                    alsoInliers_a.add(points_orig.get(j));
                }

            }

            points_orig.addAll(initial_candidates_list_a);
            //System.out.println(points_orig.size() + " ?==? " + count);


            if(alsoInliers_a.size() > d){

                //System.out.println(alsoInliers.size() + " " + points.size());
                maybeInliers_a.addAll(alsoInliers_a);
                maybeInliers_a.addAll(initial_candidates_list_a);

                //maybeinlierpoints = new Point2D.Double[maybeInliers.size()];
/*
                for(int in = 0; in < maybeinlierpoints.length; in++){

                    maybeinlierpoints[in] = maybeInliers.get(in);

                }
*/

                try {
                    this.initialize_arraylist_a(maybeInliers_a, n, d, threshold, ransac_max_iter);


                }catch (Exception e){
                    //e.printStackTrace();
                }

                double cost = this.cost;
                //System.out.println("cost: " + cost);
                /*
                if(maybeInliers_a.size() >= max_d){
                    max_d = maybeInliers_a.size();
                }
                else{
                    continue;
                }



*/
                //cost = cost * (1.0 - (double)maybeInliers_a.size() / (double)points.size());
                int n_inside = 0;

                for(int j = 0; j < maybeInliers_a.size(); j++){

                    distance = (euclideanDistance(maybeInliers_a.get(j)[0], maybeInliers_a.get(j)[1], this.center_x, this.center_y) - this.radius);
                    //System.out.println("GOT HERE!! " + distance );

                    if(distance < 0)
                        n_inside++;

                }
                //System.out.println("n_inside: " + n_inside);
                //if( (cost < minCost && this.radius < minimumDiameter) || this.radius < minimumDiameter){
                if(n_inside <= minPointsInsideCircle && radius > minRadius && radius < maxRadius){

                    if(n_inside == minPointsInsideCircle)
                        if(cost > minCost)
                            continue;

                    n_inliers = maybeInliers_a.size();
                    minimumDiameter = this.radius;
                    // boolean converged = ftol*minCost >= minCost-cost;
                    minPointsInsideCircle = n_inside;
                    solution_found = true;
                    //this.bestPoints = maybeinlierpoints.clone();
                    minCost = cost;
                    //System.out.println(minCost);
                    min_radius = this.radius;
                    center_x_ransac = this.center_x;
                    center_y_ransac = this.center_y;
                    this.ransac_inlier_points_a.clear();
                    this.ransac_inlier_points_a.addAll(maybeInliers_a);

                    //if(converged)
                    //  break;
                }
            }
        }

        //System.out.println("rad : " + min_radius);
        if(solution_found) {

            this.center_x = center_x_ransac;
            this.center_y = center_y_ransac;

            this.radius_ransac = min_radius;

            double distanceSum = 0.0;

            for(int j = 0; j < ransac_inlier_points_a.size(); j++){

                distance = Math.abs(euclideanDistance(ransac_inlier_points_a.get(j)[0], ransac_inlier_points_a.get(j)[1], this.center_x, this.center_y) - this.radius_ransac);
                //System.out.println("GOT HERE!! " + distance );
                distanceSum += distance;

            }

            distanceSum /= (double)ransac_inlier_points_a.size();

            cost_all = distanceSum;
            //System.out.println("cost_all: " + cost_all);
/*
            if(cost_all > 10.0){
                System.out.println("cost_all: " + cost_all);
                System.out.println(this.center_x + " " + this.center_y + " " + this.radius + " " + this.radius_ransac);
            }

 */
/*
            System.out.println("Cost ransac: " + this.minCost + " rad: " + this.radius_ransac + " cost_all: " + this.cost_all);
            try {
                this.initialize_arraylist(points, n, d, threshold, ransac_max_iter, mean_x, mean_y);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("Cost regular: " + this.cost + " rad: " + this.radius);
            System.out.println("-----------------");
*/
        }/*
        else if(round < 3) {
            round++;
            threshold *= 0.8;
            this.initialize2(points, n, d, threshold, ransac_max_iter, round);
        }
        */
        else{
            //System.out.println("NOT FOUND!! USING REGULAR");
            try {
                this.initialize_arraylist_a(points, n, d, threshold, ransac_max_iter);
                //fitter.minimize(100, 0.1, 1.0e-12);
            }catch (Exception e){
                e.printStackTrace();
            }

            this.ransacFailed = true;
            this.radius_ransac = this.radius;
            this.cost_all = this.cost;
        }
    }


    public Point2D.Double pullRandomElement(List<Point2D.Double> list, Random random) {

        if(list.size() == 1){
            Point2D.Double result = list.get(0);
            list.remove(0);
            return result;

        }
        // select a random list index
        int size = list.size();
        int index = random.nextInt(size-1);
        Point2D.Double result = list.get(index);
        // move last entry to selected index
        list.set(index, list.remove(size - 1));
        return result;
    }

    public cloudPoint pullRandomElement_c(List<cloudPoint> list, Random random) {

        if(list.size() == 1){
            cloudPoint result = list.get(0);
            list.remove(0);
            return result;

        }
        // select a random list index
        int size = list.size();
        int index = random.nextInt(size-1);
        cloudPoint result = list.get(index);
        // move last entry to selected index
        list.set(index, list.remove(size - 1));
        return result;
    }

    public double[] pullRandomElement_a(List<double[]> list, Random random) {

        if(list.size() == 1){
            double[] result = list.get(0);
            list.remove(0);
            return result;

        }
        // select a random list index
        int size = list.size();
        int index = random.nextInt(size-1);
        double[] result = list.get(index);
        // move last entry to selected index
        list.set(index, list.remove(size - 1));
        return result;
    }

    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    /** Update the circle radius.
     */
    private void updateRadius() {


    }

    /** Local exception class for algorithm errors. */
    public static class LocalException extends Exception {
        /** Build a new instance with the supplied message.
         * @param message error message
         */
        public LocalException(String message) {
            super(message);
        }
    }

    /** Current circle center. */

    public Point2D.Double center;

    /** Current circle radius. */
    public double rHat;

    /** Circular ring sample points. */
    //private Point2D.Double[] points;
    private ArrayList<Point2D.Double> points_list;
    /** Current cost function value. */
    public double J;

    /** Current cost function gradient. */
    private double dJdx;
    private double dJdy;

}
