package spinat.codecoverage.gui;

import java.util.ArrayList;
import java.util.Date;
import javax.swing.table.AbstractTableModel;
import spinat.codecoverage.cover.PackInfo;

class CoverageModel extends AbstractTableModel {
    ArrayList<PackInfo> data = new ArrayList<>();

    public void clear() {
        data.clear();
        this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case posName:
                return String.class;
            case posCovered:
                return Boolean.class;
            case posStart:
                return Date.class;
            case posEnd:
                return Date.class;
            case posStartStop:
                return String.class;
            case posShow:
                return String.class;
            case posValid:
                return Boolean.class;
            default:
                return Object.class;
        }
    }

    public static final int posName =0;
    public static final int posCovered =1;
    public static final int posStart =2;
    public static final int posEnd =3;
    public static final int posStartStop =4;
    public static final int posShow =5;
    public static final int posValid =6;
    
    
    @Override
    public Object getValueAt(int row, int column) {
        PackInfo pi = data.get(row);
        switch (column) {
            case posName:
                return pi.name;
            case posCovered:
                return pi.start != null && pi.end == null;
            case posStart:
                return pi.start;
            case posEnd:
                return pi.end;
            case posStartStop:
                if (pi.start == null || pi.end != null) {
                    return "Start";
                } else {
                    return "Stop";
                }
            case posShow:
                return "Show";
            case posValid:
                return pi.isValid;
            default:
                throw new RuntimeException("aua");
        }
    }

    public void addPack(PackInfo pa) {
        data.add(pa);
        this.fireTableDataChanged();
    }

    public void setPack(int row, PackInfo pa) {
        data.set(row, pa);
        this.fireTableRowsUpdated(row, row);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == posStartStop) {
            PackInfo  pi = data.get(rowIndex);
            if (pi.isCovered) {
              return true;  
            } else {
                // it is a start
                return pi.isValid;
            }
        }
        if (columnIndex == posShow) {
            return data.get(rowIndex).start != null;
        }
        return false;
    }
    
}
