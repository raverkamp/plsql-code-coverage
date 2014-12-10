package spinat.oraclelogin;

import java.sql.DriverManager;
import java.sql.SQLException;
import oracle.jdbc.OracleConnection;

final class ThinConnectionDesc extends OraConnectionDesc {
    String host;
    int port;
    String service;

    public ThinConnectionDesc(String user, String pwd, String host, int port, String service) {
        this.user = user;
        this.pwd = pwd;
        this.host = host;
        this.port = port;
        this.service = service;
    }

    @Override
    public String display() {
        return user + "@" + host + ":" + port + ":" + service;
    }

    @Override
    public OracleConnection getConnection() throws SQLException {
        String s = "jdbc:oracle:thin:@" + host + ":" + port + ":" + service;
        return (OracleConnection) DriverManager.getConnection(s, user, pwd);
    }
    
}
