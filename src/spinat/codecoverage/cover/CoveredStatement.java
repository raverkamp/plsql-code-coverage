package spinat.codecoverage.cover;

public class CoveredStatement {

    public final int start;
    public final int end;
    public final boolean hit;
    //public final boolean atomic;

    public CoveredStatement(int start, int end, boolean hit) {
        this.start = start;
        this.end = end;
        this.hit = hit;
        //  this.atomic = atomic;
    } //  this.atomic = atomic;

    @Override
    public String toString() {
        return "CoveredStatement{"
                + "start=" + start
                + ", end=" + end
                + ", hit=" + hit + "}";
    }
}
