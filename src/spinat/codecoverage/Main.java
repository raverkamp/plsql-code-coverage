package spinat.codecoverage;

import java.awt.Font;
import java.lang.reflect.Method;
import java.util.Enumeration;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

public class Main {

    static String configKey = "spinat.codecoverage";

    public static void main(String[] args) throws Exception {
        spinat.oraclejdbcclassloading.ClassLoaderFactory factoryCL
                = new spinat.oraclejdbcclassloading.ClassLoaderFactory(configKey);

        if (args.length == 1 && args[0].equals("-clear-jdbc-file")) {
            factoryCL.clearJDBCFileMemory();
            System.out.println("cleared JDBC file memory");
            System.exit(0);
        }

        ClassLoader cl = factoryCL.mkClassLoader(Main.class.getClassLoader());
        Class realMainClass = cl.loadClass("spinat.codecoverage.RealMain");
        Method m = realMainClass.getMethod("main", new String[0].getClass());
        m.invoke(null, (Object) args);

    }

}
