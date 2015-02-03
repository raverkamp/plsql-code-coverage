package spinat.codecoverage;

import java.lang.reflect.Method;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

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

        ClassLoader cl = factoryCL.mkClassLoader(Main.class.getClassLoader());
        Class realMainClass = cl.loadClass("spinat.codecoverage.RealMain");
        Method m = realMainClass.getMethod("main", new String[0].getClass());
        m.invoke(null, (Object) args);

    }

}
