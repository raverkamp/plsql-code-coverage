package spinat.codecoverage.gui;

import java.io.StringWriter;
import java.util.ArrayList;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.helpers.AttributesImpl;

import spinat.codecoverage.cover.CoveredStatement;

public class GenHtml {

    static java.util.Comparator<CoveredStatement> cmpEntry = new java.util.Comparator<CoveredStatement>() {
        @Override
        public int compare(CoveredStatement o1, CoveredStatement o2) {
            if (o1.start < o2.start) {
                return -1;
            }
            if (o1.start > o2.start) {
                return 1;
            }
            if (o1.end < o2.end) {
                return -1;
            }
            if (o1.end > o2.end) {
                return 1;
            }
            // should not happen unless o1==o2
            return 0;
        }
    };

    void genTree(ArrayList<CoveredStatement> l, TransformerHandler hd, String source) throws Exception {

        java.util.Collections.sort(l, cmpEntry);
        int pos[] = new int[]{0};
        while (!l.isEmpty()) {
            mkTree(l, source, pos, hd);
        }
        addChars(hd, source.substring(pos[0]));

    }

    void addChars(TransformerHandler hd, String s) throws Exception {
        char[] a = s.toCharArray();
        hd.characters(a, 0, a.length);
    }

    // pos is an array of length one, it is a REF containing the current pos in the string
    void mkTree(ArrayList<CoveredStatement> l, String source, int[] pos, TransformerHandler hd) throws Exception {
        if (l.isEmpty()) {
            throw new RuntimeException("aua");
        }
        CoveredStatement e = l.get(0);
        l.remove(0);
        addChars(hd, source.substring(pos[0], e.start));
        AttributesImpl att = e.hit ? hitAtt : noHitAtt;
        hd.startElement("", "", "span", att);
        pos[0] = e.start;
        while (l.size() > 0 && l.get(0).end <= e.end) {
            mkTree(l, source, pos, hd);
        }
        addChars(hd, source.substring(pos[0], e.end));
        pos[0] = e.end;
        hd.endElement("", "", "span");
    }
    AttributesImpl hitAtt;
    AttributesImpl noHitAtt;

    public GenHtml() {
        hitAtt = new AttributesImpl();
        hitAtt.addAttribute("", "", "style", "STRING", "background-color:#CCFFFF;");
        noHitAtt = new AttributesImpl();
        noHitAtt.addAttribute("", "", "style", "STRING", "background-color:#FF8888;");
    }

    public String gen(String source, ArrayList<CoveredStatement> entries) throws Exception {
        java.io.StringWriter sw = new StringWriter();
        //PrintWriter out = new PrintWriter(System.out);
        StreamResult streamResult = new StreamResult(sw);
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        //serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "users.dtd");
        //serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        hd.setResult(streamResult);
        hd.startDocument();
        AttributesImpl atts = new AttributesImpl();
        hd.startElement("", "", "html", atts);
        hd.startElement("", "", "head", atts);
        hd.endElement("", "", "head");
        hd.startElement("", "", "body", atts);
        hd.startElement("", "", "pre", atts);
        genTree(entries, hd, source);
        hd.endElement("", "", "pre");
        hd.endElement("", "", "body");
        hd.endElement("", "", "html");
        hd.endDocument();
        return sw.toString();
    }
}
