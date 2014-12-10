/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spinat.codecoverage.gui;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rav
 */
public class ErrorDialogTest {
    
    public ErrorDialogTest() {
    }
    
    @Before
    public void setUp() {
    }

    @Test
    public void testShowErrorDialog() {
    }
    @Test
    public void test1() {
        spinat.codecoverage.gui.ErrorDialog.show(null, "title", "msg",new Exception());    
    }
    
}
