package spinat.codecoverage;


import javax.swing.SwingUtilities;
import spinat.oraclelogin.OracleLogin;

public class MainTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        OracleLogin lo = new OracleLogin("Login","roland");
                        lo.doLogin();
                    }
                });
    };
}
