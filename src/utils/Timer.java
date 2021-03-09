package utils;

import java.util.concurrent.TimeUnit;

public class Timer {

    long start = System.nanoTime();

    public Timer(){}

    public void start(){

        this.start = System.nanoTime();

    }

    public String elapsed(){

        return print(System.nanoTime() - start);
    }

    private String print(Long time){

        long days = TimeUnit.NANOSECONDS
                .toDays(time);
        time -= TimeUnit.DAYS.toNanos(days);

        long hours = TimeUnit.NANOSECONDS
                .toHours(time);
        time -= TimeUnit.HOURS.toNanos(hours);

        long minutes = TimeUnit.NANOSECONDS
                .toMinutes(time);
        time -= TimeUnit.MINUTES.toNanos(minutes);

        long seconds = TimeUnit.NANOSECONDS
                .toSeconds(time);

        time -= TimeUnit.SECONDS.toNanos(seconds);

        long milliseconds = TimeUnit.NANOSECONDS
                .toMillis(time);

        String out = "";

        if(days > 0)
            out += days + " d ";

        if(hours > 0)
            out += hours + " h ";

        if(minutes > 0)
            out += minutes + " m ";

        if(seconds > 0)
            out += seconds + " s ";

        out += milliseconds + " ms ";

        return out;

    }
}
