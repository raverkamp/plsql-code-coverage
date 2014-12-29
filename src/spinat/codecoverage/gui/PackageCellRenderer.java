package spinat.codecoverage.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import spinat.codecoverage.cover.PackInfo;

// the class to draw the cells for the package list
// DefaultListCellRenderer itself extends JLabel
// and the method getListCellRendererComponent returns itself (this)
// properly configured
// we want to display
// if it is covered, state of coverage  percentage
// C if its is covered, I for invalid, V for Valid
// green/red box for coverage in percentage
// what font to choose?
class PackageCellRenderer extends DefaultListCellRenderer {

    int border = 4;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value != null) {
            PackInfo pi = (PackInfo) value;
            c.setText(pi.name);
            char state;
            if (pi.isCovered) {
                state = 'C';
            } else if (pi.isValid) {
                state = 'V';
            } else {
                state = 'I';
            }
            int p;
            if (pi.totalStatementCount > 0) {
                p = 100 * pi.coveredStatementCount / pi.totalStatementCount;
            } else {
                p = -1;
            }
            c.setIcon(new PackageIcon(c.getFont(), state, p));
        } else {
            c.setText("?");
        }
        return c;
    }

    private static class PackageIcon implements Icon {

        private char covered_or_valid_or_invalid;
        private int coverageState; // 0 , 25, 50, 75, 100 or -1 as not available
        private Font font;

        public PackageIcon(Font font, char covered_or_valid_or_invalid, int coverageState) {
            this.covered_or_valid_or_invalid = covered_or_valid_or_invalid;
            this.coverageState = coverageState;
            this.font = font;
        }

        @Override
        public void paintIcon(Component c, Graphics gr, int x, int y) {
            final Graphics2D g = (Graphics2D) gr.create();
            g.setFont(this.font);
            g.drawChars(new char[]{this.covered_or_valid_or_invalid}, 0, 1, x, y + getIconHeight());
            if (coverageState == -1) {
                g.setColor(Color.gray);
                g.fillRect(x + getIconHeight(), y, getIconHeight(), getIconHeight());
            } else {
                int h = getIconHeight() * coverageState / 100;
                g.setColor(Color.green);
                g.fillRect(x + getIconHeight(), y, getIconHeight(), h);
                g.setColor(Color.red);
                g.fillRect(x + getIconHeight(), y + h, getIconHeight(), getIconHeight() - h);
            }
        }

        @Override
        public int getIconWidth() {
            return getIconHeight() * 2;
        }

        @Override
        public int getIconHeight() {
            return this.font.getSize();
        }

    }

}
