package tools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ConvexHull_arraylist {

	private final int n;
	private ArrayList<Point> points;

	public ConvexHull_arraylist(ArrayList<Point> points) {
		this.n = points.size();
		this.points = points;
	}

	private Point getPointWithLowestYCoord() {
		Point lowest_point = points.get(0);
		for (int i = 1; i < n; i++) {
			if (points.get(i).getY() < lowest_point.getY()) {
				lowest_point = points.get(i);
			} else if (points.get(i).getY() < lowest_point.getY()) {
				if (points.get(i).getX() > lowest_point.getX())
					lowest_point = points.get(i);
			}
		}
		return lowest_point;
	}

	public int computeConvexHull() {
		if (n <= 2)
			return n;
		Point point_with_lowest_y_coord = getPointWithLowestYCoord();

		for (int i = 0; i < n; i++) {
			points.get(i).setComparatorPoint(point_with_lowest_y_coord);
		}

		Collections.sort(points);
		ArrayList<Point> mod_points = new ArrayList<Point>(n + 1);
		//System.arraycopy(points, 0, mod_points, 0, n);
		mod_points.addAll(points);
		mod_points.add(points.get(0));
		mod_points.set(n, points.get(0));
		points = mod_points;
		int convex_hull_index = 1;
		int i = 2;
		while (i <= n) {
			// decide if an angle is counterclockwise or not
			// if it is not counterclockwise, do not include it in the
			// convex hull
			while (!Point.isCounterclockwise(points.get(convex_hull_index - 1),
					points.get(convex_hull_index), points.get(i))) {
				if (convex_hull_index > 1)
					convex_hull_index--;
				else if (i == n)
					// all points are collinear
					break;
				else
					i++;
			}
			convex_hull_index++;
			swap(points, convex_hull_index, i);
			i++;
		}
		return convex_hull_index;
	}

	public ArrayList<Point> getConvexHull() {
		int convex_hull_index = computeConvexHull();
		ArrayList<Point> convex_hull_points = new ArrayList<Point>(convex_hull_index);
		//System.arraycopy(points, 0, convex_hull_points, 0, convex_hull_index);
		//System.arraycopy(points, 0, convex_hull_points, 0, convex_hull_index);
		convex_hull_points.addAll(points.subList(0, convex_hull_index));
		System.out.println("convex_hull_index:  " +  convex_hull_index);

		return convex_hull_points;
	}

	private void swap(ArrayList<Point> points, int index1, int index2) {
		assert (index1 < points.size());
		assert (index2 < points.size());

		Point aux = points.get(index1);
		points.set(index1, points.get(index2));
		points.set(index2, aux);
	}
}