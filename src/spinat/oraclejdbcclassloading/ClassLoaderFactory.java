package spinat.oraclejdbcclassloading;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;


/* create a classLoader that is able to load the Oracle
 * Jdbc classes
 */
public class ClassLoaderFactory {

    private static boolean tryLoadClass(ClassLoader ccl, String className) {
        try {
            Class classs = ccl.loadClass(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static boolean checkClassLoaderForOracleJDBCClasses(ClassLoader ccl) {
        return tryLoadClass(ccl, "oracle.jdbc.driver.OracleDriver")
                && tryLoadClass(ccl, "oracle.jdbc.OracleConnection")
                && tryLoadClass(ccl, "oracle.jdbc.OracleResultSet");
    }

    final String preferencesKey;

    // the key under whcih to store the name of the oracle JDBC file 
    // in the preferences
    final static String prefKey = "ORCLE-JDBC-FILE";

    JFileChooser fileChooser;

    public ClassLoaderFactory(String preferencesKey) {
        this.preferencesKey = preferencesKey;
        this.fileChooser = null;
    }

    Preferences getNode() {
        Preferences p = Preferences.userRoot();
        Preferences p2 = p.node(preferencesKey);
        return p2;
    }

    public File getFile() {
        if (fileChooser == null) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setCurrentDirectory(null);
            this.fileChooser = fc;
        }
        this.fileChooser.setSelectedFile(null);
        this.fileChooser.showOpenDialog(null);
        File file = this.fileChooser.getSelectedFile();
        return file;
    }

    private static ClassLoader extendWithURL(URLClassLoader clu, URL url) {
        ClassLoader pa = clu.getParent();
        URL[] urls = clu.getURLs();
        URL[] urls2 = new URL[urls.length + 1];
        System.arraycopy(urls, 0, urls2, 0, urls.length);
        urls2[urls.length] = url;
        return new URLClassLoader(urls2, pa);
    }

    private static ClassLoader extendWithFile(URLClassLoader cl, File f) {
        final java.net.URL url;
        try {
            url = f.toURI().toURL();
        } catch (java.net.MalformedURLException ex) {
            // this must always work, so throw an error if it does not
            throw new Error("java could not convert file to url", ex);
        }
        return extendWithURL(cl, url);
    }

    public ClassLoader mkClassLoader(ClassLoader parent) {
        if (checkClassLoaderForOracleJDBCClasses(parent)) {
            return parent;
        }
        if (! (parent instanceof URLClassLoader)) {
            JOptionPane.showMessageDialog(null,"<html>The Oracle JDBC classes where not found. You have to add the it to the classpath.</html>",
                    "Oracle JDBC classes not found.",JOptionPane.ERROR_MESSAGE);
        }
        URLClassLoader baseURLClassLoader = (URLClassLoader) parent; 
        
      

        Preferences p = getNode();
        String s = p.get(prefKey, "");
        if (s != null && !s.equals("")) {
            ClassLoader cl = extendWithFile(baseURLClassLoader, new File(s));
            if (checkClassLoaderForOracleJDBCClasses(cl)) {
                return cl;
            }
        }

        JOptionPane.showMessageDialog(null,
                "<html><p>This program needs the Oracle JDBC classes in order run."
                + "These classes where not found on the classpath.</p>"
                + "<p><b>In the next step you have to locate the JDBC jar file "
                + " from Oracle on your harddisk.</b></p>"
                + "<p>The JDBC file is located somewhere in the Oracle installation"
                + " directory and named for example like <tt>ojdbc6.jar</tt>.</p>"
                + "<p>The name of the file will be stored in the configuration."
                + "The next time you run this program you will not be asked.</p>"
                + "<p>If there are problems with Oracle classloading you can extend "
                + " the classpath on the command line.</p></html>", "Oracle JDBC not found",
                JOptionPane.INFORMATION_MESSAGE);

        while (true) {
            File f = getFile();
            if (f != null) {
                ClassLoader cl = extendWithFile(baseURLClassLoader, f);
                if (checkClassLoaderForOracleJDBCClasses(cl)) {
                    p.put(prefKey, f.getAbsolutePath());
                    return cl;
                } else {
                    JOptionPane.showMessageDialog(null,
                            "<html>This file did not contain the Oracle JDBC classes. Please try again.</html>",
                            "Oracle JDBC classes not found", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "<html>No Oracle JDBC jar file was supplied.<br>"
                        + "<b>This program aborts.</b></html>",
                        "Oracle JDBC file not found.", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }
}
