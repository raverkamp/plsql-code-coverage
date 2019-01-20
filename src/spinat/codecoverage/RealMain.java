package spinat.codecoverage;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Enumeration;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import spinat.codecoverage.gui.EventQueueProxy;
import spinat.codecoverage.gui.Gui2;
import spinat.codecoverage.gui.KwArgs;

public class RealMain {

    public static void main(String[] args) throws Exception {
        
        KwArgs kwa = KwArgs.parse(args);
        final double scale;
        if (kwa.kwargs.containsKey("scale")) {
            scale = Double.parseDouble(kwa.kwargs.get("scale"));
        } else {
            scale = 1.0;
        }
            
        
        MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        javax.swing.plaf.metal.MetalLookAndFeel lf = new javax.swing.plaf.metal.MetalLookAndFeel();
        UIManager.setLookAndFeel(lf);
        UIDefaults d =  UIManager.getDefaults();
        
        
        Font f1 = d.getFont("Label.font");
        int defaultSize = f1.getSize();
        Enumeration<Object> ks = d.keys();
        
        while(ks.hasMoreElements()) {
            Object o = ks.nextElement();
            Font f = d.getFont(o);
            if (f!=null) {
                Font f2 = new Font(f.getName(), f.getStyle(), (int)(f.getSize()*scale));
                d.put(o, f2);
            }
            System.out.println("" + o + " = " + d.get(o));
        }
       

        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        queue.push(new EventQueueProxy());

        final Gui2 g = new Gui2(defaultSize, scale);

        // there is at most one argument, a connection string
        // forget about the rest
        if (kwa.fixedArgs.length >= 1) {
            final String s = kwa.fixedArgs[0];
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
