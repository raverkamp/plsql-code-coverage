package spinat.codecoverage;

import spinat.codecoverage.cover.DBObjectsInstallation;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import oracle.jdbc.OracleConnection;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import spinat.oraclelogin.OraConnectionDesc;

/**
 *
 * @author rav
 */
public class DBObjectsInstallationTest {

    public DBObjectsInstallationTest() {
    }

    DBObjectsInstallation inst;

    OracleConnection con;

    @Before
    public void setUp() throws SQLException, ParseException {
        OraConnectionDesc cd = OraConnectionDesc.fromString("roland/tiger@xe");
        this.con = cd.getConnection();
        this.inst = new DBObjectsInstallation(con);
    }

    @Test
    public void testObjectExists() throws Exception {
        this.inst.dropCodeCoverageDBObjects();
        assert (!this.inst.objectExists("PACKAGE", "AAA_COVERAGE_TOOL"));
        this.inst.createDBOBjects();
        assert (this.inst.objectExists("PACKAGE", "AAA_COVERAGE_TOOL"));
    }

    @Test
    public void testCheckDBObjects() throws Exception {
        this.inst.dropCodeCoverageDBObjects();
        assert (this.inst.checkDBObjects() == DBObjectsInstallation.DBObjectState.NOTHING);
        this.inst.createDBOBjects();
        assert (this.inst.checkDBObjects() == DBObjectsInstallation.DBObjectState.OK);
        try (Statement stm = this.con.createStatement()) {
            stm.execute("drop table AAA_COVERAGE_STATEMENTS");
        }
        assert (this.inst.checkDBObjects() == DBObjectsInstallation.DBObjectState.MIXUP);
    }

}
