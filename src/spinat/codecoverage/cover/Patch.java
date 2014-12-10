package spinat.codecoverage.cover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class Patch {

    public final int start;
    public final int end;
    public final String txt;

    public Patch(int start, int end, String txt) {
        this.start = start;
        this.end = end;
        this.txt = txt;
    }

    static int intCompare(int x, int y) {
        if (x < y) {
            return -1;
        }
        if (x > y) {
            return 1;
        }
        return 0;
    }

    static String applyPatches(String s, ArrayList<Patch> patches) {
        ArrayList<Patch> a = new ArrayList<>();
        a.addAll(patches);
        Collections.sort(a, new Comparator<Patch>() {
            @Override
            public int compare(Patch p1, Patch p2) {
                return intCompare(p1.start, p2.end);
            }
        });
        StringBuilder b = new StringBuilder();
        int pos = 0;
        for (Patch patch : a) {
            b.append(s.substring(pos, patch.start));
            b.append(patch.txt);
            pos = patch.end;
        }
        b.append(s.substring(pos));
        return b.toString();
    }
}
