package spinat.codecoverage.gui;

import spinat.codecoverage.cover.PackInfo;

import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import spinat.codecoverage.cover.CodeCoverage;
import spinat.codecoverage.cover.CoverageInfo;

public class CoverageTable extends JTable {

    ButtonCellRenderer re = new ButtonCellRenderer(true);

    private final CoverageModel ccmodel;
    private CodeCoverage codeCoverage = null;

    CoverageTable(CoverageModel m) {
        super(m);
        this.ccmodel = m;
        // why is this necessary?
        this.setColumnModel(createDefaultColumnModel());
    }

    public void setCodeCoverage(CodeCoverage codeCoverage) {
        this.codeCoverage = codeCoverage;
    }

    ButtonCellEditor ce = new ButtonCellEditor(new ButtonCellListener() {
        @Override
        public void call(int row, int col, Object value) {
            
            try {
                PackInfo pi = ccmodel.data.get(row);
                if (pi.start == null || pi.end != null) {
                    CodeCoverage.StartCoverageResult res = 
                            codeCoverage.startCoverage(pi.name);
                    if (res instanceof CodeCoverage.StartCoverageSuccess) {
                        // OK, nothing todo
                    } else if (res instanceof CodeCoverage.StartCoverageFailure) {
                        CodeCoverage.StartCoverageFailure fres = (CodeCoverage.StartCoverageFailure) res;
                        String msg = fres.errormsg;
                         JOptionPane.showMessageDialog(getRootPane(),msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    PackInfo pi2 = codeCoverage.getPackInfo(pi.owner, pi.name);
                    ccmodel.setPack(row, pi2);
                }  else {
                    boolean ok = codeCoverage.stopCoverage(pi.name, false);
                    if (!ok) {
                        Object[] options = {"Yes do it anyway!",
                            "Leave it as it is.",
                            "Just mark it as stopped"};
                        int n = JOptionPane.showOptionDialog(CoverageTable.this.getTopLevelAncestor(),
                                "The source has been changed, set old source again?",
                                "Replace Sourcè",
                                JOptionPane.DEFAULT_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null, //do not use a custom Icon
                                options, //the titles of buttons
                                options[1]); //default button title

                        if (n == 0) {
                            boolean dummy = codeCoverage.stopCoverage(pi.name, true);
                        }
                    }
                    PackInfo pi2 = codeCoverage.getPackInfo(pi.owner, pi.name);
                    ccmodel.setPack(row, pi2);
                }
            } catch (Exception ex) {
                ErrorDialog.show(getRootPane(),"Exception",
                        "<html>" +stringToHtml("An exception occured:\n"+ex.toString())
                                +"</html>",ex);
            }
        }
    });

    ButtonCellEditor ce2 = new ButtonCellEditor(new ButtonCellListener() {
        @Override
        public void call(int row, int col, Object value) {
            try {
                int id = ccmodel.data.get(row).id;
                String packname = ccmodel.data.get(row).name;
                CoverageInfo ci = codeCoverage.getCoverInfo(id);
                GenHtml gh = new GenHtml();
                String s = gh.gen(ci.source, ci.entries);
                File temp = File.createTempFile("pattern", ".html");
                temp.deleteOnExit();
                try (Writer w = new FileWriter(temp)) {
                    w.write(s);
                }
                String title = "Package " + packname;
                Gui.showHtml(temp.toURI().toURL().toExternalForm(), 
                        (Window) SwingUtilities.getRoot(CoverageTable.this),title);
                // java.awt.Desktop.getDesktop().open(temp);
            } catch (Exception ex) {
                //ex.printStackTrace(System.err);
                throw new RuntimeException(ex);
            }
        }
    });
    
    FormatCellRenderer dcr = new FormatCellRenderer(new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm:ss"), JLabel.RIGHT);

    @Override
    final protected TableColumnModel createDefaultColumnModel() {
        TableColumnModel tcm = new DefaultTableColumnModel();
        {
            TableColumn tc = new TableColumn(CoverageModel.posName);
            tc.setHeaderValue("Package");
            tc.setMaxWidth(230);
            tcm.addColumn(tc);
        }
        {
            TableColumn tc = new TableColumn(CoverageModel.posCovered);
            tc.setHeaderValue("Covered");
            tc.setMaxWidth(70);
            tcm.addColumn(tc);
        }
        {
            TableColumn tc = new TableColumn(CoverageModel.posValid);
            tc.setHeaderValue("Valid");
            tc.setMaxWidth(70);
            tcm.addColumn(tc);
        }
        {
            TableColumn tc = new TableColumn(CoverageModel.posStart);
            tc.setHeaderValue("Start");
            tc.setMaxWidth(140);

            tc.setCellRenderer(dcr);
            tcm.addColumn(tc);
        }
        {
            TableColumn tc = new TableColumn(CoverageModel.posEnd);
            tc.setHeaderValue("End");
            tc.setMaxWidth(140);
            tc.setCellRenderer(dcr);
            tcm.addColumn(tc);
        }

        {
            TableColumn tc = new TableColumn(CoverageModel.posStartStop);
            tc.setHeaderValue("Action");

            tc.setCellRenderer(re);
            tc.setCellEditor(ce);
            tc.setMaxWidth(100);
            tcm.addColumn(tc);
        }

        {
            TableColumn tc = new TableColumn(CoverageModel.posShow);
            tc.setHeaderValue("Show");
            tc.setCellRenderer(re);
            tc.setCellEditor(ce2);
            tc.setMaxWidth(100);
            tcm.addColumn(tc);
        }

        return tcm;
    }
    
     static String stringToHtml(String s) {
        if (s==null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        for(int i=0;i<s.length();i++) {
            char c = s.charAt( i);
            if (Character.isLetter(c)|| Character.isDigit(c)) {
                b.append(c);
            } else {
                if (c == 10) {
                    b.append("<br>");
                } else {
                    b.append("&#" +((int)c) +";");
                }
            }
        }
        return b.toString();
    }
}
