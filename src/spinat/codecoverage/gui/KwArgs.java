package spinat.codecoverage.gui;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// very simple command line parser
// there are leaving normal arguments then there are keyword arguments, 
// there is a keyword starting with "-" and the value 
// the normal arguments are packed into fixedArgs and the keyword arguments are 
// packed into kwargs the "-" is stripped
// FUTURE?: one could thing about rest args which come after some "--"
public class KwArgs {
    

    public String fixedArgs[];
    public final Map<String, String> kwargs;

    private KwArgs(String[] fixedArgs, Map<String, String> kwargs) {
        this.fixedArgs = fixedArgs;
        this.kwargs = kwargs;
    }

    public static KwArgs parse(String[] args) throws ParseException {
        int p = 0;
        ArrayList<String> fix = new ArrayList<>();
        while (p < args.length) {
            if (args[p].startsWith("-")) {
                break;
            }
            fix.add(args[p]);
            p++;
        }
        HashMap<String, String> mp = new HashMap<>();
        while (p < args.length) {
            String s = args[p];
            if (s.startsWith("-")) {
                if (p + 1 < args.length) {
                    mp.put(args[p].substring(1), args[p + 1]);
                    p += 2;
                } else {
                    throw new ParseException("can not parse commandline, arg for keyword is missing: " + s, 0);
                }
            } else {
                throw new ParseException("can not parse commandline, not keyword arg: " + s, 0);
            }
        }
        return new KwArgs(fix.toArray(new String[0]), mp);
    }
}
