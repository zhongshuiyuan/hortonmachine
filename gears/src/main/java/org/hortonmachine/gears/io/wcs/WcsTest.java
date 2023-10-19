package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;

public class WcsTest {

    public static void main(String[] args) throws Exception {

            /**
     * <p>
     * example 2.0.1 URL
     * http://earthserver.pml.ac.uk/rasdaman/ows?
     * &SERVICE=WCS
     * &VERSION=2.0.1
     * &REQUEST=GetCoverage
     * &COVERAGEID=V2_monthly_CCI_chlor_a_insitu_test
     * &SUBSET=Lat(40,50)
     * &SUBSET=Long(-10,0)
     * &SUBSET=ansi(144883,145000)
     * &FORMAT=application/netcdf
     *
     * @throws Exception
     */

        // http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs?
        //     service=WCS
        //     &version=2.0.1
        //     &CoverageId=OneGDev__AegeanLevantineSeas-MCol
        //     &request=GetCoverage
        //     &format=image/png&

        // http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs?service=WCS
        //     &version=2.0.1
        //     &CoverageId=OneGDev__AegeanLevantineSeas-MCol
        //     &request=GetCoverage
        //     &format=image/png
        //     &subset=Lat(34.54889,37.31744)
        //     &subset=Long(26.51071,29.45505)

        String outFolder = "/home/hydrologis/TMP/KLAB/WCS/DUMPS/";

        // String SERVICE_URL = "https://geoservices9.civis.bz.it/geoserver/ows"; // ?service=WCS&version=2.0.1&request=GetCapabilities";
        String SERVICE_URL = "http://ogcdev.bgs.ac.uk/geoserver/OneGDev/wcs";

        IWebCoverageService service = IWebCoverageService.getServiceForVersion(SERVICE_URL, null);

        System.out.println( service.getVersion());

        // print list of coverage ids
        System.out.println("Coverage ids:");
        List<String> coverageIds = service.getCoverageIds();
        for (String coverageId : coverageIds) {
            System.out.println("\t" + coverageId);
        }

        // print supported srids
        System.out.println("Supported srids:");
        int[] supportedSrids = service.getSupportedSrids();
        for (int srid : supportedSrids) {
            System.out.println("\t" + srid);
        }

        // print supported formats
        System.out.println("Supported formats:");
        List<String> supportedFormats = service.getSupportedFormats();
        for (String format : supportedFormats) {
            System.out.println("\t" + format);
        }

        

        // get coverage
        String coverageId = "OneGDev__AegeanLevantineSeas-MCol";
        ReferencedEnvelope env = new ReferencedEnvelope(26.51071, 29.45505, 34.54889, 37.31744, DefaultGeographicCRS.WGS84);

        CoverageReaderParameters parameters = new CoverageReaderParameters(service, coverageId);
        parameters.format("image/tiff");
        parameters.bbox(env);
        
        File file = new File(outFolder, coverageId + "_bbox_parameter.tiff");
        service.getCoverage(file.getAbsolutePath(), parameters, null);

        
        // WebCoverageService201 wcs = new WebCoverageService201(SERVICE_URL, "2.0.1");
        // String defaultVersion = wcs.getVersion();

        // System.out.println( wcs.getCapabilitiesUrl());
        // WcsCapabilities capabilities = wcs.getCapabilities();

        // List<String> coverageIds = capabilities.getCoverageIds();

        // String coverageId = coverageIds.get(0);
        // CoverageSummary coverageSummary =
        // capabilities.getCoverageSummaryById(coverageId);
        // DescribeCoverage describeCoverage = wcs.getDescribeCoverage(coverageSummary);

        // System.out.println(describeCoverage);

    }
}
