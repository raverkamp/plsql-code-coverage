package spinat.codecoverage;

import java.lang.reflect.Method;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

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

        spinat.oraclejdbcclassloading.ClassLoaderFactory f
                = new spinat.oraclejdbcclassloading.ClassLoaderFactory("spinat.codecoverage");
        ClassLoader cl = f.mkClassLoader(Main.class.getClassLoader());
        Class realMainClass = cl.loadClass("spinat.codecoverage.RealMain");
        Method m = realMainClass.getMethod("main", new String[0].getClass());
        m.invoke(null, (Object) args);

    }

}
