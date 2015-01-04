package spinat.codecoverage.cover;

import java.util.ArrayList;

public class CoverageInfo {
    public final String bodySource;
    public final String specSource;
    public final ArrayList<CoveredStatement> entries;

    public CoverageInfo(String specSource,String bodySource, ArrayList<CoveredStatement> entries) {
        this.specSource = specSource;
        this.bodySource = bodySource;
        this.entries = entries;
    }
    
}
