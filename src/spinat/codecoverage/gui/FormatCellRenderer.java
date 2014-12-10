package spinat.codecoverage.gui;

import java.awt.Component;
import java.text.Format;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class FormatCellRenderer extends DefaultTableCellRenderer {
    
    final Format f;
    final int textAlign;
    public FormatCellRenderer(Format f,int textalign) {
        super();
        this.f = f;
        this.textAlign = textalign;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        this.setHorizontalTextPosition(textAlign);
        return res;
    }
    
    @Override 
    public void setValue(Object o) {
        if (null ==o) {
            super.setValue("");
        } else {
         super.setValue(f.format(o));
        }
    }
    
}
