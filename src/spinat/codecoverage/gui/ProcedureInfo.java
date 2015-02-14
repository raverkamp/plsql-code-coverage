/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spinat.codecoverage.gui;

import spinat.codecoverage.cover.ProcedureAndRange;

/**
 *
 * @author rav
 */
final class ProcedureInfo {

    public final int statmentCount;
    public final int hits;
    public final ProcedureAndRange procedure;

    public ProcedureInfo(ProcedureAndRange procedure, int statmentCount, int hits) {
        this.procedure = procedure;
        this.statmentCount = statmentCount;
        this.hits = hits;
    }

}
