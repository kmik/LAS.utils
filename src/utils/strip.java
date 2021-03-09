package utils;

public class strip {

    public int id, n_segments, flightLineParamsStartFrom;
    double start_t, end_t;

    public int observed_slices = 0;

    public strip(int id, int n_segments, int flightLineParamsStartFrom, double start_t, double end_t) {

        this.start_t = start_t;
        this.end_t = end_t;
        this.id = id;
        this.n_segments = n_segments;
        this.flightLineParamsStartFrom = flightLineParamsStartFrom;

    }

}
