package spinat.codecoverage.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;

import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

// the exception is only shown if a button/something else is shown
// a dialog to show an error message and additionally the exception
// how to do it:
//  1) just offer a static procedure to show, an Extension of JOption Pane?
//  2) or a subclass of JOtion Pane?
public class ErrorDialog {

    static String tString(Throwable t) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            PrintStream s = new PrintStream(bs, false, "UTF-8");
            t.printStackTrace(s);
            s.flush();
            bs.flush();
            bs.close();
            return bs.toString("UTF-8");
        } catch (Exception e) {
            return "Can not retrieve stacktrace\n" + e.toString();
        }
    }

    public static void show(Component parentComponent, String title, String msg, Throwable t) {
        final JComponent jc = new JPanel();
        BoxLayout bl = new BoxLayout(jc, BoxLayout.Y_AXIS);
        jc.setLayout(bl);
        JLabel l = new JLabel();
        l.setText(msg);
        jc.add(l, Box.LEFT_ALIGNMENT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        jc.add(Box.createVerticalStrut(20));
        JButton b = new JButton();
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        jc.add(b);
        jc.add(Box.createVerticalStrut(10));
        JPanel p2 = new JPanel();
        p2.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d2 = new Dimension(500, 10);
        p2.setMinimumSize(d2);
        p2.setMaximumSize(d2);
        p2.setPreferredSize(d2);
        jc.add(p2);
        final JTextArea txt = new JTextArea();

        txt.setText(tString(t));
        txt.setEditable(false);
        final JScrollPane jsp = new JScrollPane(txt);
        jsp.setAlignmentX(Component.LEFT_ALIGNMENT);
        jsp.setMinimumSize(new Dimension(500, 200));
        jsp.setPreferredSize(new Dimension(500, 200));

        jsp.setVisible(false);

        jc.add(jsp);
        b.setAction(new AbstractAction("More") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jsp.isVisible()) {
                    jsp.setVisible(true);

                    Container cd = jc.getTopLevelAncestor();
                    if (cd != null && cd instanceof java.awt.Window) {
                        ((java.awt.Window) cd).pack();
                    }
                    this.putValue(Action.NAME, "Less");
                } else {
                    jsp.setVisible(false);
                    Container cd = jc.getTopLevelAncestor();
                    if (cd != null && cd instanceof java.awt.Window) {
                        ((java.awt.Window) cd).pack();
                    }
                    this.putValue(Action.NAME, "More");
                }
            }
        });
        JOptionPane.showMessageDialog(parentComponent, jc, title, JOptionPane.ERROR_MESSAGE);
    }

}
