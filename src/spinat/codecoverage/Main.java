package spinat.codecoverage;

import java.awt.EventQueue;
import java.awt.Toolkit;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import spinat.codecoverage.gui.EventQueueProxy;
import spinat.codecoverage.gui.Gui2;


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
      
        // there is at most one argument, a connection string
        // forget about the rest
        if (args.length >= 1) {
            final String s = args[0];
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    g.start(s);
                }
            });
        } else {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    g.start();
                }
            });
        }
    }

}
