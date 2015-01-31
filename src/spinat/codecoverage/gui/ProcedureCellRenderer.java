package spinat.codecoverage.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;

public class ProcedureCellRenderer
        extends javax.swing.DefaultListCellRenderer {



    public ProcedureCellRenderer() {

    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel c = (JLabel) super.getListCellRendererComponent(list, value,
                index, isSelected, cellHasFocus);
        if (value != null) {
            ProcedureInfo pi = (ProcedureInfo) value;
            c.setText(pi.procedure.name + (pi.procedure.publik ? "*" : ""));
            if (pi.statmentCount > 0) {
                int frac;
                if (pi.hits == 0) {
                    frac = 0;
                } else if (pi.hits == pi.statmentCount) {
                    frac = 100;
                } else {
                    frac = (int) (100 * ((double) pi.hits) / ((double) pi.statmentCount));
                }

                c.setIcon(new CoverageIcon(this.getFont(), frac));
            } else {
                c.setIcon(new CoverageIcon(this.getFont(), -1));
            }
        } else {
            c.setText("?");
            c.setIcon(new CoverageIcon(this.getFont(), -1));
        }
        return c;
    }

    private static class CoverageIcon implements Icon {

        private final int coverageState; // 0 , 25, 50, 75, 100 or -1 as not available
        private final Font font;

        public CoverageIcon(Font font, int coverageState) {
            this.coverageState = coverageState;
            this.font = font;
        }

        @Override
        public void paintIcon(Component c, Graphics gr, int x, int y) {
            final Graphics2D g = (Graphics2D) gr.create();
            if (coverageState == -1) {
                g.setColor(Color.gray);
                g.fillRect(x, y, getIconHeight(), getIconHeight());
            } else {
                int h = getIconHeight() * coverageState / 100;
                g.setColor(Color.green);
                g.fillRect(x , y, getIconWidth(), h);
                g.setColor(Color.red);
                g.fillRect(x , y + h, getIconWidth(), getIconHeight() - h);
            }
        }

        @Override
        public int getIconWidth() {
            return getIconHeight();
        }

        @Override
        public int getIconHeight() {
            return this.font.getSize();
        }

    }

}
