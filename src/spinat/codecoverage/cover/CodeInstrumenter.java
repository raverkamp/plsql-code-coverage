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

        String[] msg = new String[1];
        final StatementExtractor stex
                = StatementExtractor.create(spec_src, body_src, msg);
        if (stex == null) {
            throw new RuntimeException(msg[0]);
        }

        List<String> a = stex.extractRestrictReferences();
        Set<String> excludedProcs = new HashSet<>(a);

        StatementExtractor.ExtractionResult extres = stex.extract(excludedProcs);

        int firstProc = extres.firstProcedurePosition;
        ArrayList<Patch> patches = new ArrayList<>();

        Class cl = this.getClass();
        final String logstufff = Util.getAsciiResource(cl, "/otherstuff/logstuff.txt");

        patches.add(new Patch(firstProc, Patch.Position.TRAILING,
                " \"sql%rowcount\" integer := null;\n"
                + " \"sql%found\" boolean := null;\n"
                + " \"sql%notfound\" boolean := null;\n"
                + " \"sql%isopen\" boolean := null;\n"
                + " \"sql%bulk_rowcount\" integer := null;\n"));

        patches.add(new Patch(firstProc, Patch.Position.LEADING, logstufff.replace("$id", "" + id)));
        ArrayList<InstrumentedStatement> is = new ArrayList<>();

        int i = 0;

        for (Range r : extres.statementRanges) {
            i++;
            is.add(new InstrumentedStatement(r, i));
            patches.add(new Patch(r.start, Patch.Position.LEADING,
                    "\"$log\"(" + (i + 1) + ");"));
        }
        for (Range r : extres.sqlAttrChangers) {
            // there is problem: range for a statement DOES NOT include the ;
            //  so we insert between the statement and the ;
            patches.add(new Patch(r.end, Patch.Position.LEADING,
                    ";\n \"sql%rowcount\"  := sql%rowcount;\n"
                    + " \"sql%found\"  := sql%found;\n"
                    + " \"sql%notfound\"  := sql%notfound;\n"
                    + " \"sql%isopen\"  := sql%isopen"));
        }
        for (StatementExtractor.SqlAttrExprAndRange sr : extres.sqlAttrExpressions) {
            final String repl;
          
            switch (sr.attribute) {
                // FOUND, ISOPEN, NOTFOUND, ROWCOUNT
                case "ROWCOUNT":
                    repl = "\"sql%rowcount\"";
                    break;
                case "FOUND":
                    repl = "\"sql%found\"";
                    break;
                case "NOTFOUND":
                    repl = "\"sql%notfound\"";
                    break;
                case "ISOPEN":
                    repl = "\"sql%isopen\"";
                    break;
                default:
                    throw new RuntimeException("unexpected SQL attribute: " + sr.attribute);
            }

            patches.add(new Patch(sr.range.start, sr.range.end, repl));
        }

        String newSrc = Patch.applyPatches(body_src, patches);
        System.out.println(newSrc);
        return new InstrumentResult(newSrc, is);
    }
}
