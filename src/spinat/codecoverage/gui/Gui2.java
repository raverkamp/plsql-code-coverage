package spinat.codecoverage.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import oracle.jdbc.OracleConnection;
import spinat.codecoverage.cover.CodeCoverage;
import spinat.codecoverage.cover.CoverageInfo;
import spinat.codecoverage.cover.CoveredStatement;
import spinat.codecoverage.cover.DBObjectsInstallation;
import spinat.codecoverage.cover.PackInfo;
import spinat.codecoverage.cover.ProcedureAndRange;
import spinat.oraclelogin.OraConnectionDesc;
import spinat.oraclelogin.OracleLogin;

public class Gui2 {

    private CodeCoverage codeCoverage;
    public final JFrame frame;

    private OraConnectionDesc connectionDesc;
    private OracleConnection connection;

    private final DefaultListModel<PackInfo> packModel;
    private final DefaultListModel<ProcedureAndRange> procedureModel;

    private final JLabel current_package;
    private final JList<PackInfo> packList;
    private final JList<ProcedureAndRange> procList;

    private final JLabel lblPackinfo;
    private final JLabel lblProcedures;

    private final CodeDisplay codeDisplay;

    private PackInfo currentPackinfo = null;

    public Gui2() {
        this.codeCoverage = null;
        this.connection = null;
        this.connectionDesc = null;

        frame = new JFrame();
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFrameIconFromResource(frame, "/cc-bild.png");
        frame.setPreferredSize(new Dimension(800, 600));

        frame.setLayout(new BorderLayout());
        JPanel left = new JPanel();
        installMenues();
        frame.add(left, BorderLayout.WEST);
        left.setLayout(new GridBagLayout());
        left.setPreferredSize(new Dimension(350, 100));
        //left.setBackground(Color.red);

        {
            JLabel lblPackages = new JLabel("Packages");
            lblPackages.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.PAGE_START;
            left.add(lblPackages, c);
        }
        this.packList = new JList<>();
        this.packModel = new DefaultListModel<>();
        packList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        packList.setModel(this.packModel);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.PAGE_START;
            JScrollPane jsp = new JScrollPane(packList,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            left.add(jsp, c);
        }
        packList.setCellRenderer(new PackageCellRenderer());
        packList.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        packageSelectionChange(e);
                    }
                });

        {
            lblProcedures = new JLabel("Procs/Funs");
            lblProcedures.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            left.add(lblProcedures, c);
        }

        this.procList = new JList<>();
        this.procedureModel = new DefaultListModel<>();
        procList.setModel(procedureModel);
        {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.PAGE_START;
            JScrollPane jsp = new JScrollPane(procList,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            left.add(jsp, c);
        }
        procList.setCellRenderer(proc_cellrenderer);

        procList.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        procedureSelectionChanged(e);
                    }
                });
        //this.procedureModel.add(0, new ProcedureInfo("Test"));
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        frame.add(right, BorderLayout.CENTER);
        JPanel top = new JPanel();
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JLabel l = new JLabel("Package");
        top.add(l);

        this.current_package = new JLabel();
        this.current_package.setOpaque(true);
        this.current_package.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        this.current_package.setBackground(Color.white);
        top.add(current_package);

        Dimension d = l.getPreferredSize();

        Dimension d3 = new Dimension(current_package.getFont().getSize() * 30, d.height + 6);
        current_package.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        current_package.setPreferredSize(d3);

        JButton b1 = new JButton(startCoverageAction);
        top.add(b1);
        startCoverageAction.setEnabled(false);

        JButton b2 = new JButton(stopCoverageAction);
        top.add(b2);
        stopCoverageAction.setEnabled(false);

        lblPackinfo = new JLabel();

        top.add(lblPackinfo);
        right.add(top, BorderLayout.NORTH);

        this.codeDisplay = new CodeDisplay();

        right.add(codeDisplay.getComponent(), BorderLayout.CENTER);
    }

    // there are two methods to startup the GUI,
    //   without a connection string
    public void start() {
        try {
            this.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            this.frame.setVisible(true);
            tryConnect();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    // with a connection String
    public void start(String connectionDesc) {
        try {
            this.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            this.frame.setVisible(true);
            OraConnectionDesc od = OraConnectionDesc.fromString(connectionDesc);
            OracleConnection c = od.getConnection();
            if (!setConnection(od, c)) {
                c.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void setFrameIconFromResource(JFrame frame, String resourceName) {
        try {
            InputStream ins = Gui2.class.getResourceAsStream(resourceName);
            if (ins != null) {
                BufferedImage img = ImageIO.read(ins);
                frame.setIconImage(img);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void tryConnect() throws SQLException {
        OracleLogin lo = new OracleLogin("Login", this.getClass().toString());
        OracleLogin.OracleLoginResult res = lo.doLogin();
        if (res == null) {
            return;
        }
        if (!setConnection(res.connectionDesc, res.connection)) {
            res.connection.close();
        }
    }

    public boolean setConnection(OraConnectionDesc cd, OracleConnection connection)
            throws SQLException {
        connection.setAutoCommit(false);
        DBObjectsInstallation inst = new DBObjectsInstallation(connection);
        final boolean dbOk;
        switch (inst.checkDBObjects()) {
            case NOTHING:
                Object[] options = {"Yes",
                    "No"};
                int x = JOptionPane.showOptionDialog(null,
                        "The database objects needed to run code coverage do not exist.\n"
                        + "Do you want to create them now?",
                        "Recreate DB objects?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                if (x == JOptionPane.YES_OPTION) {
                    inst.createDBOBjects();
                    dbOk = true;
                } else {
                    dbOk = false;
                }
                break;
            case MIXUP:
                if (askUserForRecreation()) {
                    inst.dropCodeCoverageDBObjects();
                    inst.createDBOBjects();
                    dbOk = true;
                } else {
                    dbOk = false;
                }
                break;
            case OK:
                dbOk = true;
                break;
            default:
                throw new Error("BUG");
        }
        if (!dbOk) {
            return false;
        }
        this.connection = connection;
        this.connectionDesc = cd;
        frame.setTitle("Code Coverage: " + this.connectionDesc.display());
        this.codeCoverage = new CodeCoverage(this.connection);
        this.refresh();
        return true;
    }

    public void refresh() {
        if (this.connection == null) {
            this.procedureModel.clear();
            this.packModel.clear();
            this.currentPackinfo = null;
            return;
        }
        try {
            PackInfo ci = this.currentPackinfo;
            ArrayList<PackInfo> pis = this.codeCoverage.getCCInfo();
            this.packModel.clear();
            int i = 0;
            int j = -1;
            for (PackInfo pi : pis) {
                this.packModel.add(i, pi);
                if (ci != null && pi.name.equals(ci.name)) {
                    j = i;
                }
                i++;
            }
            if (j >= 0) {
                this.packList.getSelectionModel().setSelectionInterval(j, j);
                setNewPackInfo(this.packList.getModel().getElementAt(j));
            } else {
                setNewPackInfo(null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final DefaultListCellRenderer proc_cellrenderer
            = new javax.swing.DefaultListCellRenderer() {

                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(list, value,
                            index, isSelected, cellHasFocus);
                    if (value != null) {
                        ProcedureAndRange pi = (ProcedureAndRange) value;
                        c.setText(pi.name);
                    } else {
                        c.setText("?");
                    }
                    return c;
                }

            };

    private void packageSelectionChange(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            PackInfo pi = (PackInfo) this.packList.getSelectedValue();
            setNewPackInfo(pi);
        }
    }

    private void procedureSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            ProcedureAndRange par = this.procList.getSelectedValue();
            if (par != null) {
                this.codeDisplay.gotoTextPosition(par.range.start);
            }
        }
    }

    void setNewPackInfo(PackInfo pi) {
        try {
            currentPackinfo = pi;
            this.lblPackinfo.setText("" + pi);
            if (pi != null) {
                String source;
                this.current_package.setText(pi.name);
                if (pi.id > 0) {
                    CoverageInfo ci = this.codeCoverage.getCoverInfo(pi.id);
                    source = ci.source;
                    this.codeDisplay.setText(source);
                    this.codeDisplay.setCoverageStyles(ci.entries);

                } else {
                    String s = this.codeCoverage.getPackageBodySource(pi.name);

                    source = s;
                    this.codeDisplay.setText(source);

                }
                List<ProcedureAndRange> prl
                        = this.codeCoverage.getProcedureRanges(source);
                this.procedureModel.clear();
                this.lblProcedures.setText("Procs/Funcs in " + pi.name);
                int i = 0;
                for (ProcedureAndRange pr : prl) {
                    this.procedureModel.add(i, pr);
                    i++;
                }
                if (pi.isValid && !pi.isCovered) {
                    this.startCoverageAction.setEnabled(false);
                } else {
                    this.startCoverageAction.setEnabled(false);
                }

                if (pi.isCovered) {
                    this.stopCoverageAction.setEnabled(true);
                } else {
                    this.stopCoverageAction.setEnabled(false);
                }
                if (!pi.isCovered && pi.isValid) {
                    this.startCoverageAction.setEnabled(true);
                } else {
                    this.startCoverageAction.setEnabled(false);
                }
            } else {
                this.startCoverageAction.setEnabled(false);
                this.stopCoverageAction.setEnabled(false);
                this.procedureModel.clear();
                this.lblProcedures.setText("Procs/Funcs");
                this.codeDisplay.setText("");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    static java.util.Comparator<CoveredStatement> cmpEntry
            = new java.util.Comparator<CoveredStatement>() {
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

    // once the selected package is changed:
// fetch the packinfo from the database, update the list entry
// fetch the original source from the database
// fetch the coverage info from the database
// fetch the procedures from the database
//  fill the procedure info list
//  fill the source text control
//  at least the database access should not be done on the EDT.
    void startCoverage() {
        try {
            PackInfo pi = currentPackinfo;
            if (pi != null) {
                if (pi.start == null || pi.end != null) {

                    CodeCoverage.StartCoverageResult res
                            = codeCoverage.startCoverage(pi.name);
                    if (res instanceof CodeCoverage.StartCoverageSuccess) {
                        // OK, nothing todo
                    } else if (res instanceof CodeCoverage.StartCoverageFailure) {
                        CodeCoverage.StartCoverageFailure fres
                                = (CodeCoverage.StartCoverageFailure) res;
                        String msg = fres.errormsg;
                        JOptionPane.showMessageDialog(frame.getRootPane(),
                                msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    PackInfo pi2 = codeCoverage.getPackInfo(pi.name);
                    DefaultListModel<PackInfo> lm = Gui2.this.packModel;
                    for (int i = 0; i < lm.size(); i++) {
                        if (lm.getElementAt(i).name.equals(pi2.name)) {
                            lm.setElementAt(pi2, i);
                            setNewPackInfo(pi2);
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    Action startCoverageAction = new AbstractAction("Start Coverage") {
        @Override
        public void actionPerformed(ActionEvent e) {
            startCoverage();
        }
    };

    void stopCoverage() {
        try {
            PackInfo pi = currentPackinfo;

            boolean ok = codeCoverage.stopCoverage(pi.name, false);
            if (!ok) {
                Object[] options = {"Yes do it anyway!",
                    "Leave it as it is.",
                    "Just mark it as stopped"};
                int n = JOptionPane.showOptionDialog(frame,
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
            PackInfo pi2 = codeCoverage.getPackInfo(pi.name);
            DefaultListModel<PackInfo> lm = Gui2.this.packModel;
            for (int i = 0; i < lm.size(); i++) {
                if (lm.getElementAt(i).name.equals(pi2.name)) {
                    lm.setElementAt(pi2, i);
                    setNewPackInfo(pi2);
                    break;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    Action stopCoverageAction = new AbstractAction("Stop Coverage") {

        @Override
        public void actionPerformed(ActionEvent e) {
            stopCoverage();
        }
    };

    public static boolean askUserForRecreation() {

        Object[] options = {"Yes",
            "No"};
        int x = JOptionPane.showOptionDialog(null,
                "The state of the db objects for code coverage in your DB is messy.\n"
                + "Recreate them?",
                "Recreate DB objects?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        return x == JOptionPane.YES_OPTION;
    }

    final void installMenues() {
        JMenuBar mb = new JMenuBar();
        this.frame.setJMenuBar(mb);
        JMenu m = new JMenu("File");
        mb.add(m);

        m.add(new AbstractAction("Connect") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    tryConnect();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        m.add(new AbstractAction("Refresh") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    refresh();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        m.add(this.dropCCObjects);

        m.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    final Action dropCCObjects = new AbstractAction("Drop CodeCoverage Objects") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Gui2.this.dropDBObjects();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    void dropDBObjects() throws SQLException {
        DBObjectsInstallation inst = new DBObjectsInstallation(this.connection);

        inst.dropCodeCoverageDBObjects();
        closeConnection();
    }

    void closeConnection() throws SQLException {
        if (this.codeCoverage != null) {
            this.codeCoverage.close();
            this.codeCoverage = null;
            this.connectionDesc = null;
            this.connection = null;
            this.refresh();
        }
    }

}
