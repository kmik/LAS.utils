package tools;

/** Java 8 CPU backend. All distances and optical coefficients use metres. */
public final class SolarCpuTracer {
    private SolarCpuTracer() { }
    public static final byte CLEAR=0, OPAQUE=1, ATTENUATING=2;
    private static final int SHIFT=3, BLOCK=1<<SHIFT;

    /** Adapter avoids imposing a float[][][] versus double[][][] type on the existing loader. */
    public interface Transmission { double get(int x,int y,int z); }

    /** Shared by all receivers using the same precomputed solar sample. */
    public static final class Sun {
        final double dx,dy,dz,ddx,ddy,ddz,gx,gy,gz,resolution;
        final int sx,sy,sz;
        public final double irradiance;
        public final boolean daylight;
        public Sun(double zenith,double azimuth,double resolution) {
            if (!(resolution>0) || !Double.isFinite(resolution))
                throw new IllegalArgumentException("Invalid voxel size");
            double[] d=SolarRayTracer.sunDirection(zenith,azimuth);
            this.resolution=resolution;
            dx=d[0];dy=d[1];dz=d[2];sx=sign(dx);sy=sign(dy);sz=sign(dz);
            ddx=delta(dx,resolution);ddy=delta(dy,resolution);ddz=delta(dz,resolution);
            gx=dx/resolution;gy=dy/resolution;gz=dz/resolution;
            daylight=zenith<90;
            // Deliberately match the previous multiplication and cosine expression.
            irradiance=1366.0*Math.max(0,Math.cos(Math.toRadians(zenith)));
        }
    }

    /** Coordinates and the ground adjustment are prepared once per receiver. */
    public static final class Receiver {
        final double ox,oy,oz;
        final int x,y,z,excludeX,excludeY,excludeZ;
        public Receiver(double ox,double oy,double oz,int ex,int ey,int ez) {
            if (!Double.isFinite(ox)||!Double.isFinite(oy)||!Double.isFinite(oz))
                throw new IllegalArgumentException("Invalid ray origin");
            this.ox=ox;this.oy=oy;this.oz=oz;
            x=(int)Math.floor(ox);y=(int)Math.floor(oy);z=(int)Math.floor(oz);
            excludeX=ex;excludeY=ey;excludeZ=ez;
        }
    }

    /** Build once, then publish read-only to the workers. Rows avoid one giant Java array. */
    public static final class Grid {
        final int nx,ny,nz,bx,bz;
        final byte[][] material,occupiedBlocks;
        final double[][] terrainMaximum,blockMaximum;
        final SolarRayTracer.Terrain terrain;
        final double extinction,resolution;
        final Transmission amap;
        final double[][] coefficient;
        final boolean opticalDepth,skipBlocks;

        public Grid(int nx,int ny,int nz,double resolution,double extinction,
                SolarRayTracer.Terrain terrain,Transmission amap,boolean opticalDepth,boolean skipBlocks) {
            if(nx<1||ny<1||nz<1||!(resolution>0)||!Double.isFinite(resolution)
                    ||extinction<0||!Double.isFinite(extinction))
                throw new IllegalArgumentException("Invalid optical grid");
            this.nx=nx;this.ny=ny;this.nz=nz;this.resolution=resolution;
            this.extinction=extinction;this.terrain=terrain;this.amap=amap;
            this.opticalDepth=opticalDepth;this.skipBlocks=skipBlocks;
            bx=(nx-1)/BLOCK+1;bz=(nz-1)/BLOCK+1;
            int by=(ny-1)/BLOCK+1;
            material=new byte[ny][Math.multiplyExact(nx,nz)];
            occupiedBlocks=new byte[by][Math.multiplyExact(bx,bz)];
            terrainMaximum=terrain==null?null:new double[ny][nx];
            blockMaximum=terrain==null?null:new double[by][bx];
            coefficient=amap!=null&&opticalDepth?new double[ny][Math.multiplyExact(nx,nz)]:null;
            if(terrain!=null) {
                for(double[] row:blockMaximum) java.util.Arrays.fill(row,Double.NEGATIVE_INFINITY);
                for(int y=0;y<ny;y++) for(int x=0;x<nx;x++) {
                    double h=terrain.maximum(x,y);
                    terrainMaximum[y][x]=h;
                    blockMaximum[y>>SHIFT][x>>SHIFT]=Math.max(blockMaximum[y>>SHIFT][x>>SHIFT],h);
                }
            }
            if(amap!=null) for(int y=0;y<ny;y++) for(int x=0;x<nx;x++) for(int z=0;z<nz;z++) {
                double t=amap.get(x,y,z);
                if(!Double.isFinite(t)||t<0||t>1)
                    throw new IllegalStateException("AMAPVox transmission must be in [0,1] at "+x+","+y+","+z+": "+t);
                set(x,y,z,t==0?OPAQUE:t==1?CLEAR:ATTENUATING);
                if(coefficient!=null&&t>0&&t<1) coefficient[y][x*nz+z]=-Math.log(t)/resolution;
            }
        }
        public void set(int x,int y,int z,byte value) {
            if(value<CLEAR||value>ATTENUATING) throw new IllegalArgumentException("Unknown material");
            material[y][x*nz+z]=value;
            // Conservative if a caller changes a material back to CLEAR during construction.
            if(value!=CLEAR) occupiedBlocks[y>>SHIFT][(x>>SHIFT)*bz+(z>>SHIFT)]=1;
        }
    }

    public static double trace(Receiver r,Sun s,Grid g) {
        if(!s.daylight) return 0;
        if(s.resolution!=g.resolution) throw new IllegalArgumentException("Solar/grid resolution mismatch");
        int x=r.x,y=r.y,z=r.z;
        double tx=first(r.ox,x,s.dx,g.resolution),ty=first(r.oy,y,s.dy,g.resolution),tz=first(r.oz,z,s.dz,g.resolution);
        double t=0,transmission=1,depth=0;
        long limit=(long)g.nx+g.ny+g.nz+6;
        for(long iteration=0;iteration<limit;iteration++) {
            if(x<0||x>=g.nx||y<0||y>=g.ny||z<0||z>=g.nz)
                return g.opticalDepth?Math.exp(-depth):transmission;

            if(g.skipBlocks&&g.occupiedBlocks[y>>SHIFT][(x>>SHIFT)*g.bz+(z>>SHIFT)]==0
                    &&(g.terrain==null||above(r.oz+s.gz*t,g.blockMaximum[y>>SHIFT][x>>SHIFT]))) {
                // Use repeated additions, not n*delta: this retains the reference DDA's
                // rounding at subsequent occupied cells, including nearly tied crossings.
                double ex=blockExit(tx,s.ddx,x,g.nx,s.sx),ey=blockExit(ty,s.ddy,y,g.ny,s.sy),
                        ez=blockExit(tz,s.ddz,z,g.nz,s.sz);
                double next=Math.min(ex,Math.min(ey,ez));
                if(!Double.isFinite(next)||next<t) throw new IllegalStateException("Invalid block traversal");
                // A fine-grid crossing just before the nominal block exit may advance
                // an exit axis in the reference's tie tolerance. Retain that earlier t.
                double exit=next;
                next=Math.min(next,earlierTiedCrossing(tx,s.ddx,exit));
                next=Math.min(next,earlierTiedCrossing(ty,s.ddy,exit));
                next=Math.min(next,earlierTiedCrossing(tz,s.ddz,exit));
                double tolerance=8*Math.ulp(Math.max(1.0,next));
                while(tx<=next+tolerance) {x+=s.sx;tx+=s.ddx;}
                while(ty<=next+tolerance) {y+=s.sy;ty+=s.ddy;}
                while(tz<=next+tolerance) {z+=s.sz;tz+=s.ddz;}
                t=next;
                continue;
            }
            double next=Math.min(tx,Math.min(ty,tz));
            if(!Double.isFinite(next)||next<t) throw new IllegalStateException("Non-monotonic ray traversal");
            if(next>t) {
                if(g.terrain!=null&&!above(r.oz+s.gz*t,g.terrainMaximum[y][x])
                        &&g.terrain.blocks(x,y,r.ox,r.oy,r.oz,s.gx,s.gy,s.gz,t,next)) return 0;
                if(x!=r.excludeX||y!=r.excludeY||z!=r.excludeZ) {
                    byte material=g.material[y][x*g.nz+z];
                    if(material==OPAQUE) return 0;
                    if(material==ATTENUATING) {
                        double length=next-t;
                        if(g.opticalDepth) depth+=(g.coefficient==null?g.extinction:g.coefficient[y][x*g.nz+z])*length;
                        else {
                            // Compatibility mode intentionally retains each original exp/pow and product.
                            transmission*=g.amap==null?Math.exp(-g.extinction*length):Math.pow(g.amap.get(x,y,z),length/g.resolution);
                            if(transmission==0) return 0;
                        }
                    }
                }
            }
            double tolerance=8*Math.ulp(Math.max(1.0,next));
            if(tx<=next+tolerance) {x+=s.sx;tx+=s.ddx;}
            if(ty<=next+tolerance) {y+=s.sy;ty+=s.ddy;}
            if(tz<=next+tolerance) {z+=s.sz;tz+=s.ddz;}
            t=next;
        }
        throw new IllegalStateException("Ray exceeded its grid traversal bound");
    }
    private static boolean above(double height,double maximum) {
        // Conservative clearance for floating-point evaluation near cell/block edges.
        return maximum==Double.NEGATIVE_INFINITY||height>maximum+1e-5*Math.max(1,Math.abs(maximum));
    }
    private static double blockExit(double first,double delta,int cell,int size,int sign) {
        if(sign==0) return Double.POSITIVE_INFINITY;
        int steps=sign>0?Math.min(size,((cell>>SHIFT)+1)*BLOCK)-cell:cell-(cell>>SHIFT)*BLOCK+1;
        for(int i=1;i<steps;i++) first+=delta;
        return first;
    }
    private static double earlierTiedCrossing(double first,double delta,double exit) {
        double last=first;
        while(last+delta<exit) last+=delta;
        return last<exit&&exit<=last+8*Math.ulp(Math.max(1.0,last))?last:exit;
    }
    private static int sign(double d) {return d>0?1:d<0?-1:0;}
    private static double delta(double d,double resolution) {return d==0?Double.POSITIVE_INFINITY:resolution/Math.abs(d);}
    private static double first(double q,int cell,double d,double resolution) {
        return d==0?Double.POSITIVE_INFINITY:Math.max(0,((d>0?cell+1.0:cell)-q)*resolution/d);
    }
}
