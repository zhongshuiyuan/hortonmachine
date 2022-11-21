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
package org.hortonmachine.gears.modules.r.rasternull;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LABEL;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LICENSE;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_NAME;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSRASTERNULLFILLER_DESCRIPTION)
@Documentation(OMSRASTERNULLFILLER_DOCUMENTATION)
@Author(name = OMSRASTERNULLFILLER_AUTHORNAMES, contact = OMSRASTERNULLFILLER_AUTHORCONTACTS)
@Keywords(OMSRASTERNULLFILLER_KEYWORDS)
@Label(OMSRASTERNULLFILLER_LABEL)
@Name(OMSRASTERNULLFILLER_NAME)
@Status(OMSRASTERNULLFILLER_STATUS)
@License(OMSRASTERNULLFILLER_LICENSE)
public class OmsRasterMissingValuesFiller extends HMModel {

    @Description(OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULLFILLER_pValidCellsBuffer_DESCRIPTION)
    @In
    public int pValidCellsBuffer = 10;

    @Description(OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERNULLFILLER_DESCRIPTION = "Modules that fills missing values in a raster by interpolation of border values.";
    public static final String OMSRASTERNULLFILLER_DOCUMENTATION = "";
    public static final String OMSRASTERNULLFILLER_KEYWORDS = "Null, Missing values, Raster";
    public static final String OMSRASTERNULLFILLER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERNULLFILLER_NAME = "rmissingfiller";
    public static final int OMSRASTERNULLFILLER_STATUS = 40;
    public static final String OMSRASTERNULLFILLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERNULLFILLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERNULLFILLER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION = "The raster to modify.";
    public static final String OMSRASTERNULLFILLER_pValidCellsBuffer_DESCRIPTION = "Number of max cells in distance to consider for the interpolation.";
    public static final String OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION = "The new raster.";

    

    @SuppressWarnings("unchecked")
    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster); HMRaster outData = HMRaster.writableFromTemplate(inRaster, true)) {
            List<Coordinate> novaluePoints = new ArrayList<>();
            inData.process(pm, "Identifing holes...", ( col, row, value, tcols, trows ) -> {
                if (inData.isNovalue(value)) {
                    novaluePoints.add(new Coordinate(col, row));
                }
            });

            // now find touching points with values
            STRtree touchingPointsTreetree = new STRtree();
            for( Coordinate noValuePoint : novaluePoints ) {
                Direction[] orderedDirs = Direction.getOrderedDirs();
                for( int i = 0; i < orderedDirs.length; i++ ) {
                    Direction direction = orderedDirs[i];
                    int newCol = (int) (noValuePoint.x + direction.col);
                    int newRow = (int) (noValuePoint.y + direction.row);
                    if (inData.isContained(newCol, newRow)) {
                        double value = inData.getValue(newCol, newRow);
                        if (!inData.isNovalue(value)) {
                            Coordinate coordinate = new Coordinate(newCol, newRow, value);
                            Point p = gf.createPoint(coordinate);
                            touchingPointsTreetree.insert(p.getEnvelopeInternal(), coordinate);
                        }
                    }
                }
            }
            touchingPointsTreetree.build();

            pm.beginTask("Filling holes...", novaluePoints.size());
            IDWInterpolator interpolator = new IDWInterpolator(pValidCellsBuffer);
            for( Coordinate noValuePoint : novaluePoints ) {
                // get points with values in range
                Envelope env = new Envelope(new Coordinate(noValuePoint.x, noValuePoint.y));
                env.expandBy(pValidCellsBuffer);
                List<Coordinate> result = touchingPointsTreetree.query(env);
                if (result.size() > 3) {
                    double value = interpolator.getValue(result, noValuePoint);
                    outData.setValue((int) noValuePoint.x, (int) noValuePoint.y, value);
                }

                pm.worked(1);
            }
            pm.done();

            outRaster = outData.buildCoverage("nulled");
        }
    }

}
