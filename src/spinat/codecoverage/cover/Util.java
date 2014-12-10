package spinat.codecoverage.cover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public static String getAsciiResource(Class cl, String name) {
        try (InputStream s = cl.getResourceAsStream(name)) {
            if (s == null) {
                throw new RuntimeException("can not find resource: " + name);
            }
            try (InputStreamReader ir = new InputStreamReader(s, "US-ASCII")) {
                StringBuilder b = new StringBuilder();
                while (true) {
                    int c = ir.read();
                    if (c < 0) {
                        break;
                    }
                    b.append((char) c);
                }
                return b.toString();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<String> decomposeBySemiColon(String s) {
        BufferedReader rdr = new BufferedReader(new StringReader(s));
        List<String> code = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        try {
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                if (line.endsWith(";")) {
                    code.add(b.toString() + line.substring(0, line.length() - 1));
                    b = new StringBuilder();
                } else {
                    b.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            throw new Error("IO Ex eption when reading from StringReader", ex);
        }
        return code;
    }

    public static List<String> decomposeBySlash(String s) {
        BufferedReader rdr = new BufferedReader(new StringReader(s));
        List<String> code = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        try {
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                if (line.startsWith("/") && !line.startsWith("/*")) {
                    code.add(b.toString());
                    b = new StringBuilder();
                } else {
                    b.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            throw new Error("IO Ex eption when reading from StringReader", ex);
        }
        // no need to add contents of b here, there was no ";"
        return code;
    }
}
