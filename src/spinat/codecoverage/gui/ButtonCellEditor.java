package spinat.codecoverage.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ButtonCellEditor extends AbstractCellEditor
        implements TableCellEditor {

    Object currentValue;
    JButton button;
    protected static final String EDIT = "edit";

    ButtonCellListener listener;

    public ButtonCellEditor(ButtonCellListener l) {
        button = new JButton();
        button.setBorderPainted(false);
        listener = l;
    }

    final public void edit() {
        Object oldVal = currentValue;
        if (oldVal == null) {
        }
        boolean editSuccess = doEdit();
        if (!editSuccess) {
            currentValue = oldVal;
        }
        fireEditingStopped();
    }

    protected String mkButtonText() {
        if (currentValue == null) {
            return "";
        } else {
            return currentValue.toString();
        }
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject changeEvent) {
        return true;
    }

    protected boolean doEdit() {
        return true;
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    @Override
    final public Object getCellEditorValue() {
        return currentValue;
    }

    //Implement the one method defined by TableCellEditor.
    @Override
    final public Component getTableCellEditorComponent(JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
        currentValue = value;
        button.setText(mkButtonText());
        removeActionListeners(button);
        final int r = row;
        final int col = column;
        final Object o = value;
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                listener.call(r, col, o);
                fireEditingStopped();

            }
        });
        return button;
    }

    static void removeActionListeners(JButton b) {
        ActionListener[] l = b.getActionListeners();
        for (ActionListener al : l) {
            b.removeActionListener(al);
        }

    }
}
