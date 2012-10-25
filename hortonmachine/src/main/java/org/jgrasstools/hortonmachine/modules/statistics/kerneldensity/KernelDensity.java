/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.statistics.kerneldensity;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.KernelJAI;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Kernel Density Estimator (based on the Jaitools project).")
@Author(name = "Andrea Antonello", contact = "http://jaitools.org, www.hydrologis.com")
@Keywords("Kernel Density, Raster")
@Name("kerneldenisty")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class KernelDensity extends JGTModel {

    @Description("The input map.")
    @In
    public GridCoverage2D inMap = null;

    @Description("The kernel to use (binary=0, cosine=1, distance=2, epanechnikov=3 (default), gaussian=4, inverse_distance=5, quartic=6, triangular=7, triweight=8).")
    @In
    public int pKernel = 3;

    @Description("The kernel radius to use (default = 10).")
    @In
    public int pRadius = 10;

    @Description("Use a constant value for the existing input map values instead of the real map value (default = false).")
    @In
    public boolean doConstant = false;

    @Description("The kernel density estimation.")
    @Out
    public GridCoverage2D outDensity = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        ValueType type = KernelFactory.ValueType.EPANECHNIKOV;
        switch( pKernel ) {
        case 0:
            type = KernelFactory.ValueType.BINARY;
            break;
        case 1:
            type = KernelFactory.ValueType.COSINE;
            break;
        case 2:
            type = KernelFactory.ValueType.DISTANCE;
            break;
        case 4:
            type = KernelFactory.ValueType.GAUSSIAN;
            break;
        case 5:
            type = KernelFactory.ValueType.INVERSE_DISTANCE;
            break;
        case 6:
            type = KernelFactory.ValueType.QUARTIC;
            break;
        case 7:
            type = KernelFactory.ValueType.TRIANGULAR;
            break;
        case 8:
            type = KernelFactory.ValueType.TRIWEIGHT;
            break;
        }

        KernelJAI kernel = KernelFactory.createCircle(pRadius, type);

        RenderedImage inImg = inMap.getRenderedImage();
        RandomIter inIter = RandomIterFactory.create(inImg, null);

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        float[] kernelData = kernel.getKernelData();

        pm.beginTask("Estimating kernel density...", cols - 2 * pRadius);
        for( int c = pRadius; c < cols - pRadius; c++ ) {
            for( int r = pRadius; r < rows - pRadius; r++ ) {
                double inputValue = inIter.getSampleDouble(c, r, 0);
                if (isNovalue(inputValue)) {
                    continue;
                }

                if (doConstant)
                    inputValue = 1.0;

                int k = 0;
                for( int kc = -pRadius; kc <= pRadius; kc++ ) {
                    for( int kr = -pRadius; kr <= pRadius; kr++, k++ ) {
                        // data[gridCoords.y + j][gridCoords.x + i] += cdata[k] * centreValue;
                        double outputValue = outIter.getSampleDouble(c + kc, r + kr, 0);
                        if (isNovalue(outputValue)) {
                            outputValue = 0;
                        }
                        outputValue = outputValue + kernelData[k] * inputValue;
                        outIter.setSample(c + kc, r + kr, 0, outputValue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Finalizing...", cols);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double outputValue = outIter.getSampleDouble(c, r, 0);
                if (isNovalue(outputValue)) {
                    outIter.setSample(c, r, 0, 0.0);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outDensity = CoverageUtilities.buildCoverage("kerneldensity", outWR, regionMap, inMap.getCoordinateReferenceSystem());
    }

}
