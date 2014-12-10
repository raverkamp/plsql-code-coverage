package spinat.codecoverage.cover;

import java.util.ArrayList;

public class CoverageInfo {
    public final String source;
    public final ArrayList<CoveredStatement> entries;

    public CoverageInfo(String source, ArrayList<CoveredStatement> entries) {
        this.source = source;
        this.entries = entries;
    }
    
}
