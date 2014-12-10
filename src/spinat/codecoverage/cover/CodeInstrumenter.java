package spinat.codecoverage.cover;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeInstrumenter {

    public static class InstrumentedStatement {

        public final Range range;
        public final int no;

        public InstrumentedStatement(Range range, int no) {
            this.range = range;
            this.no = no;
        }
    }

    public static class InstrumentResult {

        public final String instrumentedSource;
        public final List<InstrumentedStatement> statementRanges;

        public InstrumentResult(String newSrc, List<InstrumentedStatement> statements) {
            this.instrumentedSource = newSrc;
            this.statementRanges = statements;
        }
    }

    public CodeInstrumenter() {
    }

    public InstrumentResult instrument(String spec_src, String body_src, BigInteger id) {

        final StatementExtractor stex = new StatementExtractor();

        List<String> a = stex.extractRestrictReferences(spec_src);
        Set<String> excludedProcs = new HashSet<>(a);

        StatementExtractor.ExtractionResult extres = stex.extract(body_src, excludedProcs);
        List<Range> ranges = extres.statementRanges;

        int firstProc = extres.firstProcedurePosition;
        ArrayList<Patch> patches = new ArrayList<>();

        Class cl = this.getClass();
        final String logstufff = Util.getAsciiResource(cl, "/logstuff.txt");

        patches.add(new Patch(firstProc, firstProc, logstufff.replace("$id", "" + id)));
        ArrayList<InstrumentedStatement> is = new ArrayList<>();

        int i = 0;
        for (Range r : ranges) {
            i++;
            is.add(new InstrumentedStatement(r, i));
            patches.add(new Patch(r.start, r.start,
                    "\"$log\"(" + (i + 1) + ");"));
        }

        String newSrc = Patch.applyPatches(body_src, patches);
        return new InstrumentResult(newSrc, is);
    }
}
