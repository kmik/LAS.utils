package tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Flat solar-angle storage and a bounded, shared cache of derived ray constants. */
public final class SolarSchedule {
    public final int samples,hours,days,samplesPerHour;
    public final int[] month,day,hour,minute;
    private final float[][] angles;
    private final double resolution;
    private final int cacheLimit;
    private final ArrayList<String> dates;
    private final LinkedHashMap<Integer,SolarCpuTracer.Sun[]> cache=
            new LinkedHashMap<Integer,SolarCpuTracer.Sun[]>(16,0.75f,true);

    public SolarSchedule(int tiles,int[] months,int year,int[] minutes,double resolution,
            int cacheLimit,boolean textOutput) {
        if(tiles<1||minutes.length<1||cacheLimit<1) throw new IllegalArgumentException("Invalid solar schedule");
        this.resolution=resolution;this.cacheLimit=cacheLimit;samplesPerHour=minutes.length;
        GregorianCalendar calendar=new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
        int nDays=0;
        for(int m:months) {calendar.clear();calendar.set(year,m,1);nDays+=calendar.getActualMaximum(Calendar.DAY_OF_MONTH);}
        if(nDays==0) throw new IllegalArgumentException("No simulation days selected");
        days=nDays;hours=Math.multiplyExact(days,24);samples=Math.multiplyExact(hours,minutes.length);
        month=new int[samples];day=new int[samples];hour=new int[samples];minute=new int[samples];
        dates=textOutput?new ArrayList<String>(samples):null;
        int i=0;
        for(int m:months) {
            calendar.clear();calendar.set(year,m,1);
            for(int d=1;d<=calendar.getActualMaximum(Calendar.DAY_OF_MONTH);d++) for(int h=0;h<24;h++)
                for(int k=0;k<minutes.length;k++,i++) {
                    month[i]=m;day[i]=d;hour[i]=h;minute[i]=minutes[k];
                    // Preserve the existing output labels, including its evenly-spaced minute convention.
                    if(dates!=null) dates.add(String.format(Locale.US,"%04d%02d%02dT%02d%02d",year,m+1,d,h,k*60/minutes.length));
                }
        }
        angles=new float[tiles][Math.multiplyExact(samples,2)];
    }
    /** Fill each tile from a single precomputation task before starting receiver workers. */
    public void set(int tile,int sample,float zenith,float azimuth) {
        angles[tile][2*sample]=zenith;angles[tile][2*sample+1]=azimuth;
    }
    public synchronized SolarCpuTracer.Sun[] prepared(int tile) {
        SolarCpuTracer.Sun[] result=cache.get(tile);
        if(result!=null) return result;
        result=new SolarCpuTracer.Sun[samples];
        float[] source=angles[tile];
        for(int i=0;i<samples;i++) {
            double zenith=source[2*i],azimuth=source[2*i+1];
            if(!Double.isFinite(zenith)||zenith<0||zenith>180||!Double.isFinite(azimuth))
                throw new IllegalArgumentException("Invalid solar angles at tile "+tile+", sample "+i);
            if(zenith<90) result[i]=new SolarCpuTracer.Sun(zenith,azimuth,resolution);
        }
        cache.put(tile,result);
        if(cache.size()>cacheLimit) {
            java.util.Iterator<Map.Entry<Integer,SolarCpuTracer.Sun[]>> it=cache.entrySet().iterator();it.next();it.remove();
        }
        return result;
    }
    public ArrayList<String> copyDates() {
        if(dates==null) throw new IllegalStateException("Text output is disabled");
        return new ArrayList<String>(dates);
    }
    public String describe(int sample) {
        return "month="+(month[sample]+1)+", day="+day[sample]+", hour="+hour[sample]+", minute="+minute[sample];
    }
}
