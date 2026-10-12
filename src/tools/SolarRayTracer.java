package tools;

/** Dependency-free, Java 8 compatible solar-ray geometry. Grid Y increases south. */
public final class SolarRayTracer {
    private SolarRayTracer() { }

    public interface Optics {
        /** Transmission through a positive length in metres; return a value in [0,1]. */
        double transmission(int x, int y, int z, double lengthMetres);
    }

    public static double[] sunDirection(double zenithDegrees, double azimuthDegrees) {
        if (!Double.isFinite(zenithDegrees) || zenithDegrees < 0 || zenithDegrees > 180
                || !Double.isFinite(azimuthDegrees)) {
            throw new IllegalArgumentException("Invalid solar angles");
        }
        double z = Math.toRadians(zenithDegrees);
        double a = Math.toRadians(((azimuthDegrees % 360) + 360) % 360);
        return new double[] {clean(Math.sin(z) * Math.sin(a)),
                clean(-Math.sin(z) * Math.cos(a)), clean(Math.cos(z))};
    }

    private static double clean(double v) { return Math.abs(v) < 1e-15 ? 0 : v; }

    /**
     * Origin is in voxel coordinates; t and segment lengths are in metres.
     * Only the specified receiver voxel is excluded from volume attenuation.
     * Terrain is tested even inside that voxel. Use -1 indices to exclude none.
     */
    public static double trace(double ox, double oy, double oz,
            int receiverX, int receiverY, int receiverZ, int nx, int ny, int nz,
            double resolution, double zenith, double azimuth,
            Terrain terrain, Optics optics) {
        if (!(resolution > 0) || !Double.isFinite(resolution) || nx < 1 || ny < 1 || nz < 1
                || !Double.isFinite(ox) || !Double.isFinite(oy) || !Double.isFinite(oz)) {
            throw new IllegalArgumentException("Invalid grid or ray origin");
        }
        double[] d = sunDirection(zenith, azimuth);
        if (zenith >= 90) return 0;
        int x = (int)Math.floor(ox), y = (int)Math.floor(oy), z = (int)Math.floor(oz);
        int sx = sign(d[0]), sy = sign(d[1]), sz = sign(d[2]);
        double tx = first(ox, x, d[0], resolution);
        double ty = first(oy, y, d[1], resolution);
        double tz = first(oz, z, d[2], resolution);
        double ddx = delta(d[0], resolution), ddy = delta(d[1], resolution);
        double ddz = delta(d[2], resolution);
        double t = 0, transmission = 1;
        long limit = (long)nx + ny + nz + 6;
        for (long iteration = 0; iteration < limit; iteration++) {
            if (x < 0 || x >= nx || y < 0 || y >= ny || z < 0 || z >= nz) return transmission;
            double next = Math.min(tx, Math.min(ty, tz));
            if (!Double.isFinite(next) || next < t) {
                throw new IllegalStateException("Non-monotonic ray traversal");
            }
            if (next > t) {
                if (terrain != null && terrain.blocks(x, y, ox, oy, oz,
                        d[0] / resolution, d[1] / resolution, d[2] / resolution, t, next)) return 0;
                if (x != receiverX || y != receiverY || z != receiverZ) {
                    double cell = optics.transmission(x, y, z, next - t);
                    if (!Double.isFinite(cell) || cell < 0 || cell > 1) {
                        throw new IllegalStateException("Invalid transmission at " + x + "," + y + "," + z);
                    }
                    transmission *= cell;
                    if (transmission == 0) return 0;
                }
            }
            // Advance every tied axis; edge/corner touches have zero optical length.
            double tolerance = 8 * Math.ulp(Math.max(1.0, next));
            if (tx <= next + tolerance) { x += sx; tx += ddx; }
            if (ty <= next + tolerance) { y += sy; ty += ddy; }
            if (tz <= next + tolerance) { z += sz; tz += ddz; }
            t = next;
        }
        throw new IllegalStateException("Ray exceeded its grid traversal bound");
    }

    private static int sign(double d) { return d > 0 ? 1 : d < 0 ? -1 : 0; }
    private static double delta(double d, double s) {
        return d == 0 ? Double.POSITIVE_INFINITY : s / Math.abs(d);
    }
    private static double first(double q, int cell, double d, double s) {
        if (d == 0) return Double.POSITIVE_INFINITY;
        return Math.max(0, ((d > 0 ? cell + 1.0 : cell) - q) * s / d);
    }

    /** Continuous bilinear ground surface, with heights expressed in voxel units. */
    public static final class Terrain {
        private final double[][] nodes;
        public final int width, height;
        private static final double TOLERANCE = 1e-7;

        public Terrain(double[][] nodes) {
            if (nodes == null || nodes.length < 2 || nodes[0].length < 2)
                throw new IllegalArgumentException("Terrain needs at least two rows and columns");
            height = nodes.length - 1;
            width = nodes[0].length - 1;
            this.nodes = new double[nodes.length][];
            for (int y = 0; y < nodes.length; y++) {
                if (nodes[y].length != width + 1) throw new IllegalArgumentException("Ragged terrain");
                this.nodes[y] = nodes[y].clone();
            }
        }

        public boolean covers(int x, int y) {
            return x >= 0 && x < width && y >= 0 && y < height
                    && Double.isFinite(nodes[y][x]) && Double.isFinite(nodes[y][x+1])
                    && Double.isFinite(nodes[y+1][x]) && Double.isFinite(nodes[y+1][x+1]);
        }

        public double heightAt(double x, double y) {
            if (x < 0 || x > width || y < 0 || y > height) return Double.NaN;
            int ix = Math.min(width - 1, (int)Math.floor(x));
            int iy = Math.min(height - 1, (int)Math.floor(y));
            return covers(ix, iy) ? at(ix, iy, x, y) : Double.NaN;
        }

        private double at(int x, int y, double qx, double qy) {
            double u = qx - x, v = qy - y;
            return (1-v)*((1-u)*nodes[y][x] + u*nodes[y][x+1])
                    + v*((1-u)*nodes[y+1][x] + u*nodes[y+1][x+1]);
        }

        boolean blocks(int x, int y, double ox, double oy, double oz,
                double dx, double dy, double dz, double t0, double t1) {
            if (!covers(x, y)) return false; // Unknown terrain is not invented.
            double tm = t0 + (t1-t0)*0.5;
            double g0 = at(x,y,ox+dx*t0,oy+dy*t0) - (oz+dz*t0);
            double gm = at(x,y,ox+dx*tm,oy+dy*tm) - (oz+dz*tm);
            double g1 = at(x,y,ox+dx*t1,oy+dy*t1) - (oz+dz*t1);
            double maximum = Math.max(g0, g1);
            // Bilinear terrain along a straight ray is quadratic. Check its interior maximum.
            double a = 2*(g0+g1-2*gm), b = g1-g0-a;
            if (a < 0) {
                double u = -b/(2*a);
                if (u > 0 && u < 1) maximum = Math.max(maximum, (a*u+b)*u+g0);
            }
            return maximum > TOLERANCE;
        }
    }

    /** Fits local ground planes to populate shared terrain vertices; no external triangulator. */
    public static final class TerrainBuilder {
        private final Moments[][] cells;
        private final int nx, ny;
        private long count;
        public TerrainBuilder(int nx, int ny) {
            this.nx = nx; this.ny = ny; cells = new Moments[ny][nx];
        }
        public long pointCount() { return count; }
        public void add(int x, int y, double gx, double gy, double gz) {
            if (!Double.isFinite(gx) || !Double.isFinite(gy) || !Double.isFinite(gz))
                throw new IllegalArgumentException("Non-finite ground coordinate");
            if (cells[y][x] == null) cells[y][x] = new Moments();
            cells[y][x].add(gx-x, gy-y, gz);
            count++;
        }
        public Terrain build() {
            double[][] nodes = new double[ny+1][nx+1];
            for (int vy = 0; vy <= ny; vy++) for (int vx = 0; vx <= nx; vx++) {
                Moments merged = new Moments();
                for (int y = Math.max(0,vy-1); y <= Math.min(ny-1,vy); y++)
                    for (int x = Math.max(0,vx-1); x <= Math.min(nx-1,vx); x++)
                        if (cells[y][x] != null) merged.merge(cells[y][x], x-vx, y-vy);
                nodes[vy][vx] = merged.heightAtOrigin();
            }
            return new Terrain(nodes);
        }
    }

    private static final class Moments {
        double n, x, y, z, xx, xy, yy, xz, yz;
        void add(double a, double b, double c) {
            n++; x+=a; y+=b; z+=c; xx+=a*a; xy+=a*b; yy+=b*b; xz+=a*c; yz+=b*c;
        }
        void merge(Moments m, double a, double b) {
            n+=m.n; x+=m.x+a*m.n; y+=m.y+b*m.n; z+=m.z;
            xx+=m.xx+2*a*m.x+a*a*m.n; yy+=m.yy+2*b*m.y+b*b*m.n;
            xy+=m.xy+a*m.y+b*m.x+a*b*m.n; xz+=m.xz+a*m.z; yz+=m.yz+b*m.z;
        }
        double heightAtOrigin() {
            if (n == 0) return Double.NaN;
            double a=xx-x*x/n, b=xy-x*y/n, c=yy-y*y/n;
            double u=xz-x*z/n, v=yz-y*z/n, det=a*c-b*b;
            if (n < 3 || det <= 1e-12*Math.max(1,a*c)) return z/n;
            return z/n - ((u*c-v*b)/det)*x/n - ((v*a-u*b)/det)*y/n;
        }
    }
}
