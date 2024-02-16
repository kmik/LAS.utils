package tools;

import java.util.ArrayList;
import java.util.Scanner;

public class QuickHull {
    public ArrayList<ConcaveHull.Point> quickHull(ArrayList<ConcaveHull.Point> points) {
        ArrayList<ConcaveHull.Point> convexHull = new ArrayList<ConcaveHull.Point>();
        if (points.size() < 3)
            return (ArrayList) points.clone();

        int minPoint = -1, maxPoint = -1;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).x < minX) {
                minX = points.get(i).getX();
                minPoint = i;
            }
            if (points.get(i).x > maxX) {
                maxX = points.get(i).getX();
                maxPoint = i;
            }
        }
        ConcaveHull.Point A = points.get(minPoint);
        ConcaveHull.Point B = points.get(maxPoint);
        convexHull.add(A);
        convexHull.add(B);
        points.remove(A);
        points.remove(B);

        ArrayList<ConcaveHull.Point> leftSet = new ArrayList<ConcaveHull.Point>();
        ArrayList<ConcaveHull.Point> rightSet = new ArrayList<ConcaveHull.Point>();

        for (int i = 0; i < points.size(); i++) {
            ConcaveHull.Point p = points.get(i);
            if (pointLocation(A, B, p) == -1)
                leftSet.add(p);
            else if (pointLocation(A, B, p) == 1)
                rightSet.add(p);
        }
        hullSet(A, B, rightSet, convexHull);
        hullSet(B, A, leftSet, convexHull);

        return convexHull;
    }

    public double distance(ConcaveHull.Point A, ConcaveHull.Point B, ConcaveHull.Point C) {
        double ABx = B.getX() - A.getX();
        double ABy = B.getY() - A.getY();
        double num = ABx * (A.getY() - C.getY()) - ABy * (A.getX() - C.getX());
        if (num < 0)
            num = -num;
        return num;
    }

    public void hullSet(ConcaveHull.Point A, ConcaveHull.Point B, ArrayList<ConcaveHull.Point> set,
                        ArrayList<ConcaveHull.Point> hull) {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0)
            return;
        if (set.size() == 1) {
            ConcaveHull.Point p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.NEGATIVE_INFINITY;
        int furthestPoint = -1;
        for (int i = 0; i < set.size(); i++) {
            ConcaveHull.Point p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist) {
                dist = distance;
                furthestPoint = i;
            }
        }
        ConcaveHull.Point P = set.get(furthestPoint);
        set.remove(furthestPoint);
        hull.add(insertPosition, P);

        // Determine who's to the left of AP
        ArrayList<ConcaveHull.Point> leftSetAP = new ArrayList<ConcaveHull.Point>();
        for (int i = 0; i < set.size(); i++) {
            ConcaveHull.Point M = set.get(i);
            if (pointLocation(A, P, M) == 1) {
                leftSetAP.add(M);
            }
        }

        // Determine who's to the left of PB
        ArrayList<ConcaveHull.Point> leftSetPB = new ArrayList<ConcaveHull.Point>();
        for (int i = 0; i < set.size(); i++) {
            ConcaveHull.Point M = set.get(i);
            if (pointLocation(P, B, M) == 1) {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);

    }


    public int pointLocation(ConcaveHull.Point A, ConcaveHull.Point B, ConcaveHull.Point P)
    {
        double cp1 = (B.getX() - A.getX()) * (P.getY() - A.getY()) - (B.getY() - A.getY()) * (P.getX() - A.getX());
        if (cp1 > 0)
            return 1;
        else if (cp1 == 0)
            return 0;
        else
            return -1;
    }
}