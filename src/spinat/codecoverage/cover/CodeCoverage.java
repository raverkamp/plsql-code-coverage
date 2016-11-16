package spinat.codecoverage.cover;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import spinat.plsqlparser.Range;

public class CodeCoverage {

    final Connection connection;

    final String owner;
    final static String package_name = "AAA_COVERAGE_TOOL";
    final static String tab_master = "AAA_COVERAGE";
    final static String tab_detail = "AAA_COVERAGE_STATEMENTS";
    final static String sequence = "AAA_COVERAGE_SEQ";

    public CodeCoverage(Connection con) {
        this.connection = con;

        try (Statement stm = connection.createStatement();
                ResultSet rs = stm.executeQuery("select user from dual")) {
            rs.next();
            this.owner = rs.getString(1);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() {
        try {
            this.connection.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    final static String query = " select uo.object_name as package_name,id, "
            + " start_date, end_date,uo.status,nvl(c.is_covered,0),"
            + " (select count(*) from aaa_coverage_statements s where s.cvr_id = c.id) as total_statement_count,"
            + " (select count(*) from aaa_coverage_statements s where s.cvr_id = c.id and s.hit>0) as covered_statement_count"
            + " from user_objects uo "
            + " left join aaa_coverage c on uo.object_name = c.package_name "
            + " where  uo.OBJECT_TYPE = 'PACKAGE BODY' "
            // no sql injection problem: package_name is final static string!
            + " and uo.object_name !='" + package_name + "'";

    PackInfo packInfoFromRes(ResultSet rs) throws SQLException {
        PackInfo pi = new PackInfo(rs.getInt(2),
                owner,
                rs.getString(1),
                false,
                rs.getTimestamp(3),
                rs.getTimestamp(4),
                rs.getString(5).equals("VALID"),
                rs.getInt(7),
                rs.getInt(8));
        pi.isCovered = rs.getInt(6) == 1;
        return pi;
    }

    public ArrayList<PackInfo> getCCInfo() throws SQLException {
        ArrayList<PackInfo> res = new ArrayList<>();
        try (CallableStatement stm = connection.prepareCall(query + " order by 1")) {
            stm.setFetchSize(10000);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    res.add(packInfoFromRes(rs));
                }
            }
        }
        return res;
    }

    public PackInfo getPackInfo(String packname) throws SQLException {
        try (CallableStatement stm = connection.prepareCall(query + " and uo.object_name = ? ")) {
            stm.setString(1, packname);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    PackInfo pi = packInfoFromRes(rs);
                    return pi;
                } else {
                    return null;
                }
            }
        }
    }

    public abstract static class StartCoverageResult {
    };

    public static class StartCoverageSuccess extends StartCoverageResult {

        public BigInteger id;
    }

    public static class StartCoverageFailure extends StartCoverageResult {

        public String errormsg;
    }

    public StartCoverageResult startCoverage(String packName) throws Exception {
        try {
            if (!isPackageValid(packName)) {
                throw new RuntimeException("It is not possible to do code coverage on an invalid object.");
            }
            // the id of the coverage
            BigInteger id;
            try (CallableStatement stm
                    = connection.prepareCall("begin aaa_coverage_tool.start_coverage(?,?); end;")) {
                stm.setString(1, packName);
                stm.registerOutParameter(2, Types.BIGINT);
                stm.execute();
                id = stm.getBigDecimal(2).toBigInteger();
            }
            String bodysource = getObjectSource(this.owner, "PACKAGE BODY", packName);
            String specsource = getObjectSource(this.owner, "PACKAGE", packName);

            CodeInstrumenter code_instr = new CodeInstrumenter();
            CodeInstrumenter.InstrumentResult res = code_instr.instrument(specsource, bodysource, id);

            String newSrc = res.instrumentedSource;
            byte[] hash = md5ForString(newSrc);

            try (CallableStatement stm = connection.prepareCall("begin aaa_coverage_tool.add_line(?,?,?, ?,?,?); end;")) {
                for (CodeInstrumenter.InstrumentedStatement ir : res.statementRanges) {
                    Range r = ir.range;
                    stm.setBigDecimal(1, new BigDecimal(id));
                    stm.setInt(2, ir.no + 1);
                    stm.setInt(3, r.start);
                    stm.setInt(4, r.end);
                    stm.setString(5, bodysource.substring(r.start, Math.min(r.end, r.start + 50)));
                    stm.setString(6, bodysource.substring(Math.max(r.start, r.end - 50), r.end));
                    stm.addBatch();
                    
                }
                stm.executeBatch();
            }
            try (PreparedStatement stm = connection.prepareStatement(
                    "update aaa_coverage set hash_of_covered_code =? where id = ?")) {
                stm.setBytes(1, hash);
                stm.setBigDecimal(2, new BigDecimal(id));
                stm.execute();
            }
            try (Statement stm = connection.createStatement()) {
                stm.execute("create or replace " + newSrc);
            }
            // not necessary, ddl commits before executing 
            connection.commit();
            // if the package is not valid anymore return to the old code
            if (!isPackageValid(packName)) {
                try (Statement stm = connection.createStatement()) {
                    stm.execute("create or replace " + bodysource);
                }
                try (PreparedStatement stm = connection.prepareStatement(
                        "begin aaa_coverage_tool.abort_coverage(?); end;")) {
                    stm.setBigDecimal(1, new BigDecimal(id));
                    stm.execute();
                    connection.commit();
                }
                StartCoverageFailure fres = new StartCoverageFailure();
                fres.errormsg = "Error when creating instrument code";
                return fres;
            } else {
                connection.commit();
                StartCoverageSuccess res2 = new StartCoverageSuccess();
                res2.id = id;
                return res2;
            }
        } finally {
            connection.rollback();
        }
    }

    static private boolean bytesEqual(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean stopCoverage(String packName, boolean force) throws Exception {
        final String originalsource;
        byte[] digest = computeMD5ForSource(owner, "PACKAGE BODY", packName);
        try (PreparedStatement stm = connection.prepareStatement(
                "select ORIGINAL_body_SOURCE, hash_of_covered_code from aaa_coverage c "
                + " where c.package_name = ?")) {
            stm.setString(1, packName);
            try (ResultSet rs = stm.executeQuery()) {
                rs.next();
                originalsource = rs.getString(1);
                byte[] hash = rs.getBytes(2);
                if (!force && (hash == null || !bytesEqual(hash, digest))) {
                    return false;
                }
            }
        }
        try (CallableStatement stm = connection.prepareCall("begin aaa_coverage_tool.end_coverage(?); end;")) {
            stm.setString(1, packName);
            stm.execute();
        }
        try (Statement stm = connection.createStatement()) {
            stm.execute("create or replace " + originalsource);
        }
        connection.commit();
        return true;
    }

    public void stopCoverageNoSourceReset(String packName) throws SQLException {
        try (CallableStatement stm = connection.prepareCall("begin aaa_coverage_tool.end_coverage(?); end;")) {
            stm.setString(1, packName);
            stm.execute();
            connection.commit();
        }
    }

    public CoverageInfo getCoverInfo(int id) throws SQLException {
        final String specSource;
        final String bodySource;
        try (CallableStatement stm
                = connection.prepareCall(
                        "select original_spec_source,original_body_source "
                        + " from aaa_coverage where id =?")) {
                    stm.setInt(1, id);
                    try (ResultSet rs = stm.executeQuery()) {
                        if (rs.next()) {
                            specSource = rs.getString(1);
                            bodySource = rs.getString(2);
                        } else {
                            throw new RuntimeException("no code coverage found for id:" + id);
                        }
                    }
                }
                try (CallableStatement stm = connection.prepareCall(
                        "select stm_no, line_no, hit, start_, end_ "
                        + " from aaa_coverage_statements where cvr_id = ?")) {
                    stm.setInt(1, id);
                    stm.setFetchSize(10000);
                    ArrayList<CoveredStatement> l;
                    try (ResultSet rs = stm.executeQuery()) {
                        l = new ArrayList<>();
                        while (rs.next()) {
                            CoveredStatement e = new CoveredStatement(rs.getInt(4), rs.getInt(5), rs.getInt(3) > 0);
                            l.add(e);
                        }
                    }
                    return new CoverageInfo(specSource, bodySource, l);
                }
    }

    public boolean isPackageValid(String name) throws SQLException {
        try (CallableStatement stm = connection.prepareCall(
                "select count(*) from user_objects \n"
                + " where object_name = ? \n"
                + " and object_type in ('PACKAGE','PACKAGE BODY') \n"
                + " and status = 'VALID' \n")) {
            stm.setString(1, name);
            try (ResultSet rs = stm.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 2;
            }
        }
    }

    public byte[] md5ForString(String s) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DigestOutputStream dis = new DigestOutputStream(os, digest);
        Writer w = new OutputStreamWriter(dis, Charset.forName("UTF-8"));
        try {
            w.write(s);
            w.flush();
            w.close();
            dis.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return dis.getMessageDigest().digest();
    }

    public byte[] computeMD5ForSource(String owner, String type, String name) throws SQLException, IOException {
        String s = getObjectSource(owner, type, name);
        return md5ForString(s);
    }

    String getObjectSource(String owner, String object_type, String object_name) throws SQLException {
        // this fetch the code from user_source
        try (PreparedStatement stm = this.connection.prepareStatement("select text from all_source where owner = ? and type=? and name = ? order by line")) {
            stm.setFetchSize(10000);
            stm.setString(1, owner);
            stm.setString(2, object_type);
            stm.setString(3, object_name);
            StringBuilder b = new StringBuilder();
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    b.append(rs.getString(1));
                }
            }
            return b.toString();
        }
    }

    public String getPackageBodySource(String packageName) throws SQLException {
        return this.getObjectSource(this.owner, "PACKAGE BODY", packageName);
    }

    public String getPackageSpecSource(String packageName) throws SQLException {
        return this.getObjectSource(this.owner, "PACKAGE", packageName);
    }

    public List<String> possibleCoveredPackages() throws SQLException {
        try (PreparedStatement pst = this.connection.prepareStatement(
                "select distinct name from user_source "
                + "where text like '%\"$log\"%' "
                + " and type like 'PACKAGE BODY' order by 1")) {
            pst.setFetchSize(10000);
            try (ResultSet rs = pst.executeQuery()) {
                ArrayList<String> a = new ArrayList<>();
                while (rs.next()) {
                    a.add(rs.getString(1));
                }
                return a;
            }
        }
    }
}
