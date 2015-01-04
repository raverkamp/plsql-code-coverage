package spinat.codecoverage.cover;

public class ProcedureAndRange {

    public final String name;
    public final Range range;
    public final boolean publik;

    public ProcedureAndRange(String name, boolean publik, Range range) {
        this.name = name;
        this.publik = publik;
        this.range = range;
    }

}
