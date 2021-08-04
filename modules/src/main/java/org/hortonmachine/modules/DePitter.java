/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_KEYWORDS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_LABEL;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_LICENSE;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_NAME;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_STATUS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_outFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter.OMSDEPITTER_outPit_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSDEPITTER_DESCRIPTION)
@Author(name = OMSDEPITTER_AUTHORNAMES, contact = OMSDEPITTER_AUTHORCONTACTS)
@Keywords(OMSDEPITTER_KEYWORDS)
@Label(OMSDEPITTER_LABEL)
@Name("_" + OMSDEPITTER_NAME)
@Status(OMSDEPITTER_STATUS)
@License(OMSDEPITTER_LICENSE)
public class DePitter extends HMModel {
    @Description(OMSDEPITTER_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev;

    @Description(OMSDEPITTER_outPit_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPit = null;

    @Description(OMSDEPITTER_outFlow_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFlow = null;

    public boolean doParallel = true;

    @Execute
    public void process() throws Exception {
        OmsDePitter pitfiller = new OmsDePitter();
        pitfiller.inElev = getRaster(inElev);
        pitfiller.pm = pm;
        pitfiller.doProcess = doProcess;
        pitfiller.doReset = doReset;
        pitfiller.doParallel = doParallel;
        pitfiller.doFlow = outFlow != null;
        pitfiller.process();
        if (outPit != null)
            dumpRaster(pitfiller.outPit, outPit);
        if (outFlow != null)
            dumpRaster(pitfiller.outFlow, outFlow);
    }

    public static void main( String[] args ) throws Exception {
        DePitter d = new DePitter();
        d.inElev = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/DEM_gura.tif";
        d.outPit = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/depit_gura.tif";
        d.outFlow = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/deflow_gura.tif";
        d.doParallel = true;
        d.process();
    }
}
