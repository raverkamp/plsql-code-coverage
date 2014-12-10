package spinat.oraclelogin;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.*;
import oracle.jdbc.OracleConnection;

/**
 * a class to pop up a dialog to get a connection description from the user
 *
 * @author rav
 */
public final class OracleLogin {

    final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static class OracleLoginResult {

        public final OracleConnection connection;
        public final OraConnectionDesc connectionDesc;

        private OracleLoginResult(OracleConnection c, OraConnectionDesc d) {
            connection = c;
            connectionDesc = d;
        }
    }
    OracleLoginResult res = null;
    final JTextField fUser = new JTextField();
    final JTextField fPwd = new JPasswordField(30);
    final JTextField fHost = new JTextField();
    final JTextField fPort = new JTextField();
    final JTextField fService = new JTextField();
    final JRadioButton rbThin = new JRadioButton();
    final JRadioButton rbFat = new JRadioButton();
    final JTextField fTns = new JTextField();
    final JComboBox<StringAndValue> histbox = new JComboBox<StringAndValue>();
    final JCheckBox cbSavePwd = new JCheckBox("Save Password");
    final JButton btnClearHist = new JButton("Clear History");
    JDialog dialog = new JDialog();
    final String title;
    final String preferencesKey;

    /**
     * create a OracleLogin object, input can be gotten from the user by calling
     * doLogin
     *
     * @param title the title of the dialog
     * @param preferencesKey the key under which to store the connection history
     */
    public OracleLogin(String title, String preferencesKey) {
        this.title = title;
        this.preferencesKey = preferencesKey;
    }

    private void adJustControlProps() {
        boolean b = rbThin.isSelected();
        fTns.setEnabled(!b);
        fHost.setEnabled(b);
        fPort.setEnabled(b);
        fService.setEnabled(b);
    }

    /**
     * get a oracle connection from the user
     *
     * @return a OracleLoginresult, which contains a connection description and
     * a oracle connection
     */
    public OracleLoginResult doLogin() {

        JPanel p = new JPanel();

        dialog.setContentPane(p);
        dialog.setModal(true);
        dialog.setTitle(title);

        JLabel lbUser = new JLabel("User");
        JLabel lbPwd = new JLabel("Password");
        JLabel lbTns = new JLabel("Tns-Name");
        JLabel lbThin = new JLabel("Host:Port:Service");
        JLabel lbHist = new JLabel("Past Logins");
        JLabel lbKindOfConnection = new JLabel("Kind of Connection");

        fTns.setEnabled(false);

        setPrefferedWidth(fPort, 50);
        setMaxSize(fPort);

        rbThin.setText("Thin");
        rbFat.setText("Fat");
        ButtonGroup group = new ButtonGroup();
        group.add(rbThin);
        group.add(rbFat);

        group.setSelected(rbThin.getModel(), true);

        ActionListener li = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adJustControlProps();
            }
        };
        rbFat.addActionListener(li);
        rbThin.addActionListener(li);

        JButton btnOk = new JButton();
        btnOk.setText("Ok");
        JButton btnCancel = new JButton();
        btnCancel.setText("Cancel");

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryLogin();
            }
        });

        //setPrefferedWidth(histbox,100);
        p.setLayout(new BorderLayout());
        p.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
        Box b = Box.createHorizontalBox();
        //b.add(histbox);
        b.add(Box.createHorizontalGlue());
        b.add(Box.createVerticalStrut(btnOk.getPreferredSize().height + 2 * 20));
        b.add(btnOk);
        b.add(Box.createHorizontalStrut(20));
        b.add(btnCancel);
        b.add(Box.createHorizontalStrut(20));
        p.add(b, BorderLayout.SOUTH);

        JPanel gp = new JPanel();
        p.add(gp, BorderLayout.CENTER);
        gp.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        //top, left, bottom, right
        //Border lb = BorderFactory.createEmptyBorder(0,0,0,10);
        Insets ins = new Insets(4, 4, 4, 4);

        c.anchor = GridBagConstraints.LINE_END;
        c.insets = ins;
        c.gridx = 0;
        c.weightx = 0;

        c.gridy = 0;
        gp.add(lbHist, c);

        c.gridy = 2;
        gp.add(lbUser, c);

        c.gridy = 3;
        gp.add(lbPwd, c);

        c.gridy = 4;
        Box b2 = Box.createHorizontalBox();
        b2.add(rbFat);
        b2.add(rbThin);
        b2.add(Box.createHorizontalGlue());

        {
            GridBagConstraints c2 = (GridBagConstraints) c.clone();
            c2.gridx = 1;
            c2.anchor = GridBagConstraints.LINE_START;
            gp.add(b2, c2);
        }
        //c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        gp.add(lbKindOfConnection, c);

        c.anchor = GridBagConstraints.LINE_END;

        c.gridwidth = 1;
        c.gridy = 5;
        gp.add(lbTns, c);

        c.gridy = 6;

        gp.add(lbThin, c);

        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 1;
        c.gridy = 0;

        gp.add(histbox, c);

        c.gridy = 1;
        Box bh = Box.createHorizontalBox();
        bh.add(cbSavePwd);
        bh.add(btnClearHist);

        btnClearHist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearHistory();
                loadHistory();

            }
        });

        cbSavePwd.setSelected(getSavePwd());
        cbSavePwd.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean b = cbSavePwd.isSelected();
                setSavePwd(b);
            }
        });

        gp.add(bh, c);

        c.gridy = 2;
        setPrefferedWidth(fUser, 100);
        gp.add(fUser, c);
        c.gridy = 3;
        gp.add(fPwd, c);
        c.gridy = 5;
        gp.add(fTns, c);

        c.gridy = 6;

        Box b3 = Box.createHorizontalBox();

        b3.add(fHost);
        b3.add(new JLabel(":"));
        b3.add(fPort);
        b3.add(new JLabel(":"));
        b3.add(fService);
        b3.add(Box.createHorizontalGlue());
        gp.add(b3, c);

        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = 4;
        gp.add(Box.createGlue(), c);

        setUpHistBox();

        dialog.getRootPane().setDefaultButton(btnOk);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        fUser.requestFocusInWindow();
        dialog.setVisible(true);

        return res;

    }

    Preferences getNode() {
        Preferences p = Preferences.userRoot();
        Preferences p2 = p.node(preferencesKey);
        return p2;
    }
    static final String histKey = "HIST";
    static final String savePwdKey = "SAVE_PWD";

    void saveToHist(OraConnectionDesc d0, boolean savePwd) {
        OraConnectionDesc d;
        if (savePwd) {
            d = d0;
        } else {
            if (d0 instanceof OciConnectionDesc) {
                d = new OciConnectionDesc(d0.user, "", ((OciConnectionDesc) d0).tnsname);
            } else if (d0 instanceof ThinConnectionDesc) {
                ThinConnectionDesc y = (ThinConnectionDesc) d0;
                d = new ThinConnectionDesc(y.user, "", y.host, y.port, y.service);
            } else {
                throw new RuntimeException("unknown OraConnectionDesc subclass");
            }
        }

        Preferences p2 = getNode();
        String hs = p2.get(histKey, "");
        OraConnectionDesc[] h = decodeHist(hs);
        ArrayList<OraConnectionDesc> l = new ArrayList<OraConnectionDesc>();
        l.add(d);

        for (int i = 0; i < h.length; i++) {
            if (l.size() >= 10) {
                break;
            }
            if (d.display().equals(h[i].display())) {
                continue;
            }
            l.add(h[i]);
        }
        h = l.toArray(h);
        String s = encodeHist(h);
        p2.put(histKey, s);
        try {
            p2.flush();
        } catch (BackingStoreException e) {
            logger.log(Level.SEVERE, "error", e);
        }
    }

    void clearHistory() {
        Preferences p = getNode();
        p.put(histKey, "");
        try {
            p.flush();
        } catch (BackingStoreException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "error", ex);
        }
    }

    void setSavePwd(boolean x) {
        Preferences n = getNode();
        try {
            n.putBoolean(savePwdKey, x);
            n.flush();
        } catch (BackingStoreException ex) {
            logger.log(Level.SEVERE, "error", ex);
        }
    }

    boolean getSavePwd() {
        Preferences n = getNode();
        return n.getBoolean(savePwdKey, false);
    }

    final void loadHistory() {
        Preferences p2 = getNode();
        String hs = p2.get(histKey, "");
        OraConnectionDesc[] h = decodeHist(hs);
        StringAndValue[] sv = new StringAndValue[h.length + 1];
        sv[0] = new StringAndValue("-", null);
        for (int i = 0; i < h.length; i++) {
            sv[i + 1] = new StringAndValue(h[i].display(), h[i]);
        }
        histbox.setModel(new DefaultComboBoxModel<StringAndValue>(sv));
    }

    final void setUpHistBox() {

        loadHistory();
        histbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object o = e.getItem();
                    if (o instanceof StringAndValue) {
                        if (((StringAndValue) o).value == null) {
                            return;
                        }
                        OraConnectionDesc ocd = (OraConnectionDesc) ((StringAndValue) o).value;
                        fUser.setText(ocd.user);
                        fPwd.setText(ocd.pwd);

                        if (ocd instanceof OciConnectionDesc) {
                            rbFat.setSelected(true);
                            fTns.setText(((OciConnectionDesc) ocd).tnsname);
                        } else if (ocd instanceof ThinConnectionDesc) {
                            rbThin.setSelected(true);
                            ThinConnectionDesc td = (ThinConnectionDesc) ocd;
                            fHost.setText(td.host);
                            fPort.setText("" + td.port);
                            fService.setText(td.service);
                        }
                        adJustControlProps();
                    }
                }
            }
        });
    }

    void tryLogin() {
        try {
            String user = fUser.getText();
            String pwd = fPwd.getText();
            OraConnectionDesc desc;
            if (rbThin.isSelected()) {
                String host = fHost.getText();
                String port = fPort.getText();
                int portInt = Integer.parseInt(port);
                String service = fService.getText();
                desc = new ThinConnectionDesc(user, pwd, host, portInt, service);
            } else {
                String tns = fTns.getText();
                desc = new OciConnectionDesc(user, pwd, tns);
            }
            OracleConnection con = desc.getConnection();

            res = new OracleLoginResult(con, desc);
            saveToHist(desc, this.cbSavePwd.isSelected());
            dialog.setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (UnsatisfiedLinkError er) {
            JOptionPane.showMessageDialog(dialog, "Use a thin connection: " + er.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    static void setPrefferedWidth(JComponent c, int width) {
        c.setPreferredSize(new Dimension(width, c.getPreferredSize().height));
    }

    static void setMinSize(JComponent c) {
        c.setMinimumSize(new Dimension(c
                .getPreferredSize().width - 1,
                c.getPreferredSize().height));
    }

    static void setMaxSize(JComponent c) {
        c.setMaximumSize(new Dimension(c
                .getPreferredSize().width - 1,
                c.getPreferredSize().height));
    }

    static String encode(OraConnectionDesc c) {
        StringBuilder b = new StringBuilder();
        if (c instanceof OciConnectionDesc) {
            OciConnectionDesc f = (OciConnectionDesc) c;
            b.append(f.user).append(";")
                    .append(f.pwd).append(";")
                    .append(f.tnsname);
        } else if (c instanceof ThinConnectionDesc) {
            ThinConnectionDesc f = (ThinConnectionDesc) c;
            b.append(f.user).append(";")
                    .append(f.pwd).append(";")
                    .append(f.host).append(";")
                    .append("" + f.port).append(";")
                    .append(f.service);
        } else {
            throw new RuntimeException("unknown desctype");
        }
        return b.toString();
    }

    static String encodeHist(OraConnectionDesc[] cons) {
        StringBuilder b = new StringBuilder();
        for (OraConnectionDesc c : cons) {
            b.append(encode(c));

            b.append("|");
        }
        return b.toString();
    }

    static OraConnectionDesc[] decodeHist(String s) {
        String[] a = s.split("\\|");
        ArrayList<OraConnectionDesc> al = new ArrayList<OraConnectionDesc>();
        for (String w : a) {
            if (w == null || w.equals("")) {
                continue;
            }
            String[] b = w.split(";");
            if (b.length == 3) {
                al.add(new OciConnectionDesc(b[0], b[1], b[2]));
            } else if (b.length == 5) {
                al.add(new ThinConnectionDesc(b[0], b[1], b[2], Integer.parseInt(b[3]), b[4]));
            } else {
                //throw new RuntimeException("aua");
            }
        }
        return al.toArray(new OraConnectionDesc[0]);
    }

    static class StringAndValue {

        String string;
        Object value;

        public StringAndValue(String s, Object v) {
            string = s;
            value = v;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
