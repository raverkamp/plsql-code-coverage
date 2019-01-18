package spinat.codecoverage.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import spinat.codecoverage.cover.CoveredStatement;
import static spinat.codecoverage.gui.Gui2.cmpEntry;

public class CodeDisplay {

    private final JTextPane sourceTextPane;
    private final JScrollPane scrollPane;

    public JComponent getComponent() {
        return scrollPane;
    }

    public CodeDisplay(int fontSize) {
        sourceTextPane = new JTextPane();
        // must be here, otherwise too late
        sourceTextPane.setEditorKit(new SourceEditorKit(fontSize+8));
        initStyles(fontSize);

        scrollPane = new JScrollPane(sourceTextPane);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(250, 145));
        scrollPane.setMinimumSize(new Dimension(10, 10));
        sourceTextPane.setEditable(false);
    }

    // the text Styles for the source view
    Style defStyle;
    Style hotStyle;
    Style greenStyle;

    final void initStyles(int fontSize) {
        Style defaultStyle = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        defStyle = StyleContext.getDefaultStyleContext().addStyle("hot", defaultStyle);
        StyleConstants.setFontFamily(defStyle, Font.MONOSPACED);
        StyleConstants.setFontSize(defStyle, fontSize);
        StyleConstants.setForeground(defStyle, Color.black);
        hotStyle = StyleContext.getDefaultStyleContext().addStyle("hot", defStyle);
        StyleConstants.setBackground(hotStyle, new Color(255, 200, 200));
        greenStyle = StyleContext.getDefaultStyleContext().addStyle("green", defStyle);
        StyleConstants.setBackground(greenStyle, new Color(200, 255, 200));
    }

    public void gotoTextPosition(int pos) {
        this.sourceTextPane.setCaretPosition(pos);
    }

    public void setText(String txt) {
        try {
            StyledDocument sd = this.sourceTextPane.getStyledDocument();
            sd.remove(0, sd.getLength());
            sd.insertString(0, txt, this.defStyle);
            this.sourceTextPane.setCaretPosition(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCoverageStyles(List<CoveredStatement> statements) {
        new Styler().setStyles(this.sourceTextPane.getStyledDocument(), statements);
    }

    private class Styler {

        private StyledDocument doc;
        private ArrayList<CoveredStatement> sortedList;

        public void setStyles(StyledDocument doc, List<CoveredStatement> statements) {
            this.doc = doc;
            sortedList = new ArrayList<>();
            sortedList.addAll(statements);
            Collections.sort(sortedList, cmpEntry);

            int pos = 0;
            while (pos < sortedList.size()) {
                pos = setStyles(pos);
            }
        }

        private void setStyle(int from, int to, Style style) {
            if (from == to) {
                return;
            }
            this.doc.setCharacterAttributes(from, to - from, style, true);
        }

        private int setStyles(final int startPos) {
            // einstieg mit pos
            final CoveredStatement cs = sortedList.get(startPos);
            final Style myStyle;
            if (cs.hit) {
                myStyle = CodeDisplay.this.greenStyle;
            } else {
                myStyle = CodeDisplay.this.hotStyle;
            }
            int last_end = cs.start;
            int pos = startPos + 1;
            while (true) {
                if (pos >= sortedList.size()) {
                    // last entry no contained elements set your style and return pos+1
                    setStyle(last_end, cs.end, myStyle);
                    return pos;
                }
                CoveredStatement next_cs = sortedList.get(pos);
                if (next_cs.start <= cs.end) {
                    // contained
                    setStyle(last_end, next_cs.start, myStyle);
                    last_end = next_cs.end;
                    pos = setStyles(pos);
                } else {
                    setStyle(last_end, cs.end, myStyle);
                    return pos;
                }
            }
        }
    }

}
