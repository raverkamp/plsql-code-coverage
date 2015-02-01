
package spinat.codecoverage;

import java.awt.EventQueue;
import java.awt.Toolkit;
import spinat.codecoverage.gui.EventQueueProxy;
import spinat.codecoverage.gui.Gui2;


public class RealMain {
      
      public static void main(String[] args) throws Exception {
        
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
                    try {
                       g.start(s);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        } else {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        g.start();
                    } catch (Exception ex) {
                         throw new RuntimeException(ex);
                    }
                }
            });
        }
    }

}
