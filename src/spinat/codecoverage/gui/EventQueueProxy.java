package spinat.codecoverage.gui;

import java.awt.AWTEvent;
import java.awt.EventQueue;

public class EventQueueProxy extends EventQueue {

    @Override
    protected void dispatchEvent(AWTEvent newEvent) {
        try {
            super.dispatchEvent(newEvent);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            String message = t.getMessage();
            if (message == null || message.length() == 0) {
                message = "Fatal: " + t.getClass();
            }
            t.printStackTrace(System.err);
            ErrorDialog.show(null, "Unhandled Exception", message, t);
        }
    }
}
