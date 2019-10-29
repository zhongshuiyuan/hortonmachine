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
package org.hortonmachine.gears.io.vectorwriter;

import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_DESCRIPTION;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_DOCUMENTATION;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_KEYWORDS;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_LABEL;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_LICENSE;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_NAME;
import static org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter.OMSVECTORWRITER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.FEATUREWRITER;

import java.io.File;
import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSVECTORWRITER_DESCRIPTION)
@Documentation(OMSVECTORWRITER_DOCUMENTATION)
@Author(name = OMSVECTORWRITER_AUTHORNAMES, contact = OMSVECTORWRITER_AUTHORCONTACTS)
@Keywords(OMSVECTORWRITER_KEYWORDS)
@Label(OMSVECTORWRITER_LABEL)
@Name(OMSVECTORWRITER_NAME)
@Status(OMSVECTORWRITER_STATUS)
@License(OMSVECTORWRITER_LICENSE)
public class OmsVectorWriter extends HMModel {

    @Description(OMSVECTORWRITER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSVECTORWRITER_P_TYPE_DESCRIPTION)
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description(OMSVECTORWRITER_TABLE_DESCRIPTION)
    @In
    public String table = null;

    @Description(OMSVECTORWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String file = null;

    // PARAM NAMES START
    public static final String OMSVECTORWRITER_DESCRIPTION = "Vectors features writer to file module.";
    public static final String OMSVECTORWRITER_DOCUMENTATION = "OmsVectorWriter.html";
    public static final String OMSVECTORWRITER_KEYWORDS = "IO, Shapefile, Feature, Vector, Writing";
    public static final String OMSVECTORWRITER_LABEL = FEATUREWRITER;
    public static final String OMSVECTORWRITER_NAME = "vectorwriter";
    public static final int OMSVECTORWRITER_STATUS = 40;
    public static final String OMSVECTORWRITER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORWRITER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORWRITER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORWRITER_IN_VECTOR_DESCRIPTION = "The read feature collection.";
    public static final String OMSVECTORWRITER_P_TYPE_DESCRIPTION = "The vector type to write (Supported is: shp).";
    public static final String OMSVECTORWRITER_TABLE_DESCRIPTION = "The table to write to (where applicable).";
    public static final String OMSVECTORWRITER_FILE_DESCRIPTION = "The vector file to write.";
    // PARAM NAMES STOP

    @Execute
    public void process() throws Exception {
        checkNull(file);

        File vectorFile = new File(file);
        if (inVector.size() == 0) {
            pm.message("Warning, not writing an empty vector to file: " + vectorFile.getName());
            return;
        }
        String name = vectorFile.getName();
        if (name.toLowerCase().endsWith(HMConstants.SHP) || (pType != null && pType.equals(HMConstants.SHP))) {
            OmsShapefileFeatureWriter.writeShapefile(vectorFile.getAbsolutePath(), inVector, pm);
        } else if (name.toLowerCase().endsWith(HMConstants.GPKG) || (pType != null && pType.equals(HMConstants.GPKG))) {
            File outFile = new File(file);
            if (table == null || table.length() == 0) {
                table = FileUtilities.getNameWithoutExtention(outFile);
            }
            try (GeopackageDb db = (GeopackageDb) EDb.GEOPACKAGE.getSpatialDb()) {
                db.open(file);
                db.initSpatialMetadata(null);

                CoordinateReferenceSystem crs = inVector.getBounds().getCoordinateReferenceSystem();
                int srid = CrsUtilities.getSrid(crs);
                db.addCRS("EPSG", srid, crs.toWKT());
                SpatialDbsImportUtils.createTableFromSchema(db, inVector.getSchema(), table, null, false);
                SpatialDbsImportUtils.importFeatureCollection(db, inVector, table, -1, pm);
            }
        } else {
            throw new IOException("Format is currently not supported for file: " + name);
        }
    }

    /**
     * Fast write access mode. 
     * 
     * @param path the vector file path.
     * @param featureCollection the {@link FeatureCollection} to write.
     * @throws IOException
     */
    public static void writeVector( String path, SimpleFeatureCollection featureCollection ) throws Exception {
        OmsVectorWriter writer = new OmsVectorWriter();
        writer.file = path;
        writer.inVector = featureCollection;
        writer.process();
    }

    public static void writeVector( String path, String table, SimpleFeatureCollection featureCollection ) throws Exception {
        OmsVectorWriter writer = new OmsVectorWriter();
        writer.file = path;
        writer.table = table;
        writer.inVector = featureCollection;
        writer.process();
    }

}
