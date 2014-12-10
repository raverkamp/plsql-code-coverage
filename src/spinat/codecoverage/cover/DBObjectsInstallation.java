package spinat.codecoverage.cover;

import java.io.*;
import java.sql.*;
import java.util.List;
import oracle.jdbc.OracleConnection;

public class DBObjectsInstallation {

    public final static String package_name = "AAA_COVERAGE_TOOL";
    public final static String tab_master = "AAA_COVERAGE";
    public final static String tab_detail = "AAA_COVERAGE_STATEMENTS";
    public final static String sequence = "AAA_COVERAGE_SEQ";

    final OracleConnection connection;

    public DBObjectsInstallation(OracleConnection connection) {
        this.connection = connection;
    }

    public static enum DBObjectState {

        OK, NOTHING, MIXUP
    }

    public boolean objectExists(String type, String name) throws SQLException {
        try (PreparedStatement stm = connection.prepareStatement("select count(*) from user_objects "
                + " where object_name = ? and object_type = ? ")) {
            stm.setString(1, name);
            stm.setString(2, type);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) {
                    throw new Error("BUG");
                }
                int count = rs.getInt(1);
                if (rs.wasNull()) {
                    throw new Error("BUG");
                }
                if (count == 0) {
                    return false;
                } else if (count == 1) {
                    return true;
                } else {
                    throw new Error("BUG");
                }
            }
        }
    }

    // first check if the database is up to date
    // possibilities :
    //   nothing exists => ask and then create objects
    //   everything exists and is up to date => nothing to do
    //   only parts exists => stop and tell user that the state is inconclusive
    public DBObjectState checkDBObjects() throws SQLException {
        PreparedStatement stm = connection.prepareStatement(
                "select count(*) from user_objects "
                + " where object_name = ? and object_type ='PACKAGE' "
                + " or object_name = ? and object_type ='TABLE' "
                + " or object_name = ? and object_type = 'TABLE' "
                + " or object_name = ? and object_type = 'SEQUENCE'");
        stm.setString(1, package_name);
        stm.setString(2, tab_master);
        stm.setString(3, tab_detail);
        stm.setString(4, sequence);
        ResultSet rs = stm.executeQuery();
        if (!rs.next()) {
            throw new Error("BUG");
        }
        int count = rs.getInt(1);
        if (rs.wasNull()) {
            throw new Error("BUG");
        }
        if (count == 4) {
            return DBObjectState.OK;
        } else if (count == 0) {
            return DBObjectState.NOTHING;
        } else {
            return DBObjectState.MIXUP;
        }
    }

    public void dropCodeCoverageDBObjects() throws SQLException {
        try (Statement stm = this.connection.createStatement()) {
            if (objectExists("SEQUENCE", sequence)) {
                boolean b = stm.execute("drop sequence " + sequence);
            }
            if (objectExists("TABLE", tab_detail)) {
                boolean b = stm.execute("drop  table " + tab_detail);
            }
            if (objectExists("TABLE", tab_master)) {
                boolean b = stm.execute("drop  table " + tab_master);
            }
            if (objectExists("PACKAGE BODY", package_name)) {
                boolean b = stm.execute("drop  package body " + package_name);
            }
            if (objectExists("PACKAGE", package_name)) {
                boolean b = stm.execute("drop  package " + package_name);
            }
        }
    }

    public void createDBOBjects() throws SQLException {
        String sqlsources = Util.getAsciiResource(this.getClass(), "aaa_tables_sequences.sql");
        List<String> sql_components = Util.decomposeBySemiColon(sqlsources);
        try (Statement stm = this.connection.createStatement()) {
            for (String s : sql_components) {
                try {
                    boolean b = stm.execute(s);
                    // fixme: what to do with b?
                } catch (SQLException ex) {
                    System.out.println(s);
                    throw new RuntimeException("aua",ex);
                }
            }
        }
        String packagefile = Util.getAsciiResource(this.getClass(), "aaa_coverage_tool.pck");
        List<String> codes = Util.decomposeBySlash(packagefile);
        try (Statement stm = this.connection.createStatement()) {
            for (String s : codes) {
                try {
                    boolean b = stm.execute(s);
                    // fixme: what to do with b?
                } catch (SQLException ex) {
                    System.out.println(s);
                    throw new RuntimeException("aua",ex);
                }
            }
        }
    }
}
