package spinat.codecoverage;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import oracle.jdbc.OracleConnection;
import spinat.codecoverage.gui.EventQueueProxy;
import spinat.codecoverage.gui.Gui2;
import spinat.oraclelogin.OraConnectionDesc;

public class Main {

    public static void main(String[] args) throws Exception {

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        queue.push(new EventQueueProxy());

        final Gui2 g = new Gui2();
        boolean conSuccess = false;
        // there is at most one argument, a connection string
        if (args.length == 1) {
            String s = args[0];
            final OraConnectionDesc od = OraConnectionDesc.fromString(s);
            final OracleConnection c = od.getConnection();
            conSuccess = true;
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        g.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                        g.frame.setVisible(true);
                        if (!g.setConnection(od, c)) {
                            c.close();
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        if (!conSuccess) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        g.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                        g.frame.setVisible(true);
                        g.tryConnect();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
    }

}
