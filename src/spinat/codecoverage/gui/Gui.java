package spinat.codecoverage.gui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import oracle.jdbc.OracleConnection;
import spinat.codecoverage.cover.*;
import spinat.oraclelogin.*;

public class Gui {

    private CodeCoverage codeCoverage;
    public final JFrame frame;
    final CoverageTable coverageTable;
    final JScrollPane scp;
    final CoverageModel model;

    private OraConnectionDesc connectionDesc;
    private OracleConnection connection;

    public Gui() {
        this.codeCoverage = null;
        this.connection = null;
        this.connectionDesc = null;

        frame = new JFrame();
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        try {
            InputStream ins = getClass().getResourceAsStream("/cc-bild.png");
            if (ins != null) {
                BufferedImage img = ImageIO.read(ins);
                frame.setIconImage(img);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        model = new CoverageModel();
        coverageTable = new CoverageTable(model);
        coverageTable.setModel(model);
        scp = new JScrollPane(coverageTable);
        frame.add(scp);
        frame.setPreferredSize(new Dimension(800, 400));
        frame.setSize(new Dimension(800, 400));
        frame.setTitle("Code Coverage not connected");

        JMenuBar mb = new JMenuBar();
        frame.setJMenuBar(mb);
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

        m.add(Gui.this.dropCCObjects);

        m.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

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

    final Action dropCCObjects = new AbstractAction("Drop CodeCoverage Objects") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Gui.this.dropDBObjects();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    public boolean setConnection(OraConnectionDesc cd, OracleConnection connection) throws SQLException {
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
        final String user;
        try (Statement stm = connection.createStatement();
                ResultSet rs = stm.executeQuery("select user from dual")) {
            rs.next();
            user = rs.getString(1);
        }
        this.codeCoverage = new CodeCoverage(this.connection, user);
        this.refresh();
        this.coverageTable.setCodeCoverage(codeCoverage);
        return true;
    }

    public void refresh() throws SQLException {
        model.clear();
        if (this.codeCoverage != null) {
            dropCCObjects.setEnabled(true);
            ArrayList<PackInfo> l = codeCoverage.getCCInfo();
            for (PackInfo l1 : l) {
                model.addPack(l1);
            }
        } else {
            dropCCObjects.setEnabled(false);
        }
    }

    public static void showHtml(String url, Window w, String title) {
        try {
            JEditorPane htmlPane = new JEditorPane(url);
            htmlPane.setEditable(false);
            JDialog d = new JDialog(w);
            d.setTitle(title);
            d.setPreferredSize(new Dimension(600, 800));
            d.setSize(new Dimension(600, 800));
            d.add(new JScrollPane(htmlPane));
            //d.show();
            d.setVisible(true);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

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

    public static boolean askUserForRecreation() {

        Object[] options = {"Yes",
            "No"};
        int x = JOptionPane.showOptionDialog(null,
                "The state of the db objects for code coverage in your DB is messy.\nRecreate them?",
                "Recreate DB objects?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        return x == JOptionPane.YES_OPTION;
    }
}
