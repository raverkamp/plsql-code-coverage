package spinat.oraclelogin;

import java.sql.SQLException;
import oracle.jdbc.OracleConnection;
import java.text.ParseException;

/**
 * @author rav
 * describes an oracle jdbc connection
 */
public abstract class OraConnectionDesc {

    protected String user;
    protected String pwd;

    /**
     *
     * @return a string representation of the connection description, 
     * but without the password
     */
    public abstract String display();
    
    /**
     * does the connection have a password set?
     * @return true if the connection description has password set 
     */
    public boolean hasPwd() {
       return pwd != null;
    }
    
    /**
     * set the password of the connection description
     * @param pwd the password
     */
    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
    
    /**
     * get a oracle jdbc connection for the connection desription
     * @return an oracle jdbc connection
     * @throws SQLException
     */
    public abstract OracleConnection getConnection() throws SQLException;
    // ensure the driver is loaded
    static final oracle.jdbc.driver.OracleDriver d = new oracle.jdbc.driver.OracleDriver();
    
    /**
     * parse a connection description from a string
     * @param conStr
     * @return connection description
     * @throws ParseException
     */
    public static OraConnectionDesc fromString(String conStr) throws ParseException{
        final int p = conStr.indexOf("@");
        if (p <= 0) {
            throw new ParseException("expecting a conenction string in the form \"user[/pwd]@tnsname\" or \"user[/pwd]@host:port:service\"",0);
        }
        final String userPart = conStr.substring(0, p);
        final String rest = conStr.substring(p + 1);
        final int p2 = userPart.indexOf("/");
        final String user;
        final String pwd;
        if (p2 <= 0) {
            user = userPart;
            pwd = null;
        } else {
            user = userPart.substring(0, p2);
            pwd = userPart.substring(p2 + 1);
        }
        
        final int pcolon1 = rest.indexOf(":");
        if (pcolon1 < 0) {
            return new OciConnectionDesc(user, pwd, rest);
        } else {
            final int pcolon2 = rest.indexOf(":", pcolon1 + 1);
            if (pcolon2 >= 0) {
                final String[] a = rest.split(":");
                if (a.length != 3) {
                    throw new ParseException("expecting more",0);
                }
                final int x;
                try {
                    x = Integer.parseInt(a[1]);
                } catch ( java.lang.NumberFormatException ex) {
                    throw new ParseException("port must be an integer >0, not: " + a[1],0);
                }
                return new ThinConnectionDesc(user, pwd, a[0], x, a[2]);
            } else {
                throw new ParseException("expecting a connection string in the form \"user/pwd@host:port:service\"",0);
            }
        }
    }
}
