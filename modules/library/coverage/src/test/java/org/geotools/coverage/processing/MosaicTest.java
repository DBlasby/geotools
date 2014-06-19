/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverage.processing;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.geotools.TestData;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.operation.Mosaic;
import org.geotools.coverage.processing.operation.Mosaic.GridGeometryPolicy;
import org.geotools.data.WorldFileReader;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.resources.image.ImageUtilities;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * This class tests the {@link Mosaic} operation. The tests ensures that the final {@link GridCoverage2D} created contains the union of the input
 * bounding box or is equal to that provided by the external {@link GridGeometry2D}. Also the tests check if the output {@link GridCoverage2D}
 * resolution is equal to that imposed in input with the {@link GridGeometryPolicy}.
 * 
 * 
 * @author Nicola Lagomarsini GesoSolutions S.A.S.
 * 
 */
public class MosaicTest extends GridProcessingTestBase {

    /** Tolerance value for the double comparison */
    private static final double TOLERANCE = 0.01d;

    /**
     * WKT used for testing that the operation throws an exception when the input {@link GridCoverage2D}s does not have the same
     * {@link CoordinateReferenceSystem}.
     */
    private final static String GOOGLE_MERCATOR_WKT = "PROJCS[\"WGS 84 / Pseudo-Mercator\","
            + "GEOGCS[\"Popular Visualisation CRS\"," + "DATUM[\"Popular_Visualisation_Datum\","
            + "SPHEROID[\"Popular Visualisation Sphere\",6378137,0,"
            + "AUTHORITY[\"EPSG\",\"7059\"]]," + "TOWGS84[0,0,0,0,0,0,0],"
            + "AUTHORITY[\"EPSG\",\"6055\"]]," + "PRIMEM[\"Greenwich\",0,"
            + "AUTHORITY[\"EPSG\",\"8901\"]]," + "UNIT[\"degree\",0.01745329251994328,"
            + "AUTHORITY[\"EPSG\",\"9122\"]]," + "AUTHORITY[\"EPSG\",\"4055\"]],"
            + "UNIT[\"metre\",1," + "AUTHORITY[\"EPSG\",\"9001\"]],"
            + "PROJECTION[\"Mercator_1SP\"]," + "PARAMETER[\"central_meridian\",0],"
            + "PARAMETER[\"scale_factor\",1]," + "PARAMETER[\"false_easting\",0],"
            + "PARAMETER[\"false_northing\",0]," + "AUTHORITY[\"EPSG\",\"3785\"],"
            + "AXIS[\"X\",EAST]," + "AXIS[\"Y\",NORTH]]";

    /** First Coverage used */
    private static GridCoverage2D coverage1;

    /** Second Coverage used. It is equal to the first one but translated on the X axis. */
    private static GridCoverage2D coverage2;

    /** Third Coverage used. It is equal to the first one but contains an alpha band */
    private static GridCoverage2D coverage3;

    /** Fourth Coverage used. It is equal to the second one but contains an alpha band */
    private static GridCoverage2D coverage4;

    /**
     * The processor to be used for all tests.
     */
    private static final CoverageProcessor processor = CoverageProcessor.getInstance(GeoTools
            .getDefaultHints());

    // Static method used for preparing the input data.
    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        TestData.unzipFile(MosaicTest.class, "sampleData.zip");
        coverage1 = readInputFile("sampleData");
        coverage2 = readInputFile("sampleData2");
        coverage3 = readInputFile("sampleData3");
        coverage4 = readInputFile("sampleData4");
    }

    /**
     * Private method for reading the input file.
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static GridCoverage2D readInputFile(String filename) throws FileNotFoundException,
            IOException {
        final File tiff = TestData.file(MosaicTest.class, filename + ".tif");
        final File tfw = TestData.file(MosaicTest.class, filename + ".tfw");

        final TIFFImageReader reader = (it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(ImageIO.createImageInputStream(tiff));
        final BufferedImage image = reader.read(0);
        reader.dispose();

        final MathTransform transform = new WorldFileReader(tfw).getTransform();
        final GridCoverage2D coverage2D = CoverageFactoryFinder.getGridCoverageFactory(null)
                .create("coverage" + filename,
                        image,
                        new GridGeometry2D(new GridEnvelope2D(PlanarImage.wrapRenderedImage(image)
                                .getBounds()), transform, DefaultGeographicCRS.WGS84), null, null,
                        null);
        return coverage2D;
    }

    // Simple test which mosaics two input coverages without any additional parameter
    @Test
    public void testMosaicSimple() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);
        sources.add(coverage2);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        // Mosaic operation
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final GridCoverage BoundingBox is equal to the union of the separate coverages bounding box
        Envelope2D expected = coverage1.getEnvelope2D();
        expected.include(coverage2.getEnvelope2D());
        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the first coverage
        double initialRes = calculateResolution(coverage1);
        double finalRes = calculateResolution(mosaic);
        double percentual = Math.abs(initialRes - finalRes) / initialRes;
        Assert.assertTrue(percentual < TOLERANCE);

        // Check that on the center of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getCenterX(), actual.getCenterY());
        double nodata = CoverageUtilities.getBackgroundValues(coverage1)[0];
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        disposeCoveragePlanarImage(mosaic);
    }

    // Simple test which mosaics two input coverages with a different value for the output nodata
    @Test
    public void testMosaicWithAnotherNoData() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);
        sources.add(coverage2);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        // Setting of the nodata
        double nodata = -9999;
        param.parameter(Mosaic.OUTNODATA_NAME).setValue(new double[] { nodata });
        // Mosaic
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final GridCoverage BoundingBox is equal to the union of the separate coverages bounding box
        Envelope2D expected = coverage1.getEnvelope2D();
        expected.include(coverage2.getEnvelope2D());
        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the first coverage
        double initialRes = calculateResolution(coverage1);
        double finalRes = calculateResolution(mosaic);
        double percentual = Math.abs(initialRes - finalRes) / initialRes;
        Assert.assertTrue(percentual < TOLERANCE);

        // Check that on the center of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getCenterX(), actual.getCenterY());
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        disposeCoveragePlanarImage(mosaic);
    }

    // Test which mosaics two input coverages with alpha band
    @Test
    public void testMosaicWithAlpha() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage3);
        sources.add(coverage4);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        // Mosaic
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final Coverage Bands are 2
        Assert.assertEquals(2, mosaic.getNumSampleDimensions());

        // Check that the final GridCoverage BoundingBox is equal to the union of the separate coverages bounding box
        Envelope2D expected = coverage3.getEnvelope2D();
        expected.include(coverage4.getEnvelope2D());
        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the first coverage
        double initialRes = calculateResolution(coverage3);
        double finalRes = calculateResolution(mosaic);
        double percentual = Math.abs(initialRes - finalRes) / initialRes;
        Assert.assertTrue(percentual < TOLERANCE);

        // Check that on the center of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getCenterX(), actual.getCenterY());
        double nodata = CoverageUtilities.getBackgroundValues(coverage1)[0];
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        disposeCoveragePlanarImage(mosaic);
    }

    // Test which mosaics two input coverages and resamples them by using the resolution of an external GridGeometry2D
    @Test
    public void testMosaicExternalGeometry() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);
        sources.add(coverage2);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);

        // Initial Bounding box
        Envelope2D startBBOX = coverage1.getEnvelope2D();
        startBBOX.include(coverage2.getEnvelope2D());
        Envelope2D expected = new Envelope2D(startBBOX);
        Point2D pt = new Point2D.Double(startBBOX.getMaxX() + 1, startBBOX.getMaxY() + 1);
        expected.add(pt);
        // External GridGeometry
        GridGeometry2D ggStart = new GridGeometry2D(PixelInCell.CELL_CORNER, coverage1
                .getGridGeometry().getGridToCRS2D(PixelOrientation.UPPER_LEFT), expected,
                GeoTools.getDefaultHints());

        param.parameter("geometry").setValue(ggStart);
        // Mosaic
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final GridCoverage BoundingBox is equal to the bounding box provided in input

        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the first coverage
        double initialRes = calculateResolution(coverage1);
        double finalRes = calculateResolution(mosaic);
        Assert.assertEquals(initialRes, finalRes, TOLERANCE);

        // Check that on the Upper Right pixel of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getMinX() + finalRes, actual.getMaxY() - finalRes);
        double nodata = CoverageUtilities.getBackgroundValues(coverage1)[0];
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        disposeCoveragePlanarImage(mosaic);
    }

    // Test which mosaics two input coverages and tries to impose a null GridGeometry. An exception will be thrown
    @Test(expected = CoverageProcessingException.class)
    public void testMosaicNoExternalGeometry() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);
        sources.add(coverage2);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        param.parameter(Mosaic.POLICY).setValue("external");
        // Mosaic
        processor.doOperation(param);
    }

    // Test which mosaics two input coverages and creates a final GridCoverage with the worst resolution between those of the input GridCoverages
    @Test
    public void testMosaicCoarseResolution() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);

        // Resampling of the second Coverage to an higher resolution
        ParameterValueGroup paramResampling = processor.getOperation("resample").getParameters();
        paramResampling.parameter("Source").setValue(coverage2);
        GridEnvelope2D gridRange = coverage2.getGridGeometry().getGridRange2D();
        gridRange.add(gridRange.getMaxX() + 100, gridRange.getMaxY() + 100);
        GridGeometry2D ggNew = new GridGeometry2D(gridRange, coverage2.getEnvelope());
        paramResampling.parameter("GridGeometry").setValue(ggNew);
        GridCoverage2D resampled = (GridCoverage2D) processor.doOperation(paramResampling);

        sources.add(resampled);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        param.parameter(Mosaic.POLICY).setValue("coarse");
        // Mosaic
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final GridCoverage BoundingBox is equal to the union of the separate coverages bounding box
        Envelope2D expected = coverage1.getEnvelope2D();
        expected.include(resampled.getEnvelope2D());
        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the first coverage
        double initialRes = calculateResolution(coverage1);
        double finalRes = calculateResolution(mosaic);
        double percentual = Math.abs(initialRes - finalRes) / initialRes;
        Assert.assertTrue(percentual < TOLERANCE);

        // Check that on the center of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getCenterX(), actual.getCenterY());
        double nodata = CoverageUtilities.getBackgroundValues(coverage1)[0];
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        resampled.dispose(true);
        disposeCoveragePlanarImage(mosaic);
        disposeCoveragePlanarImage(resampled);
    }

    // Test which mosaics two input coverages and creates a final GridCoverage with the best resolution between those of the input GridCoverages
    @Test
    public void testMosaicFineResolution() {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);

        // Resampling of the second Coverage to an higher resolution
        ParameterValueGroup paramResampling = processor.getOperation("resample").getParameters();
        paramResampling.parameter("Source").setValue(coverage2);
        GridEnvelope2D gridRange = coverage2.getGridGeometry().getGridRange2D();
        gridRange.add(gridRange.getMaxX() + 100, gridRange.getMaxY() + 100);
        GridGeometry2D ggNew = new GridGeometry2D(gridRange, coverage2.getEnvelope());
        paramResampling.parameter("GridGeometry").setValue(ggNew);
        GridCoverage2D resampled = (GridCoverage2D) processor.doOperation(paramResampling);

        sources.add(resampled);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        param.parameter(Mosaic.POLICY).setValue("fine");
        // Mosaic
        GridCoverage2D mosaic = (GridCoverage2D) processor.doOperation(param);

        // Check that the final GridCoverage BoundingBox is equal to the union of the separate coverages bounding box
        Envelope2D expected = coverage1.getEnvelope2D();
        expected.include(resampled.getEnvelope2D());
        // Mosaic Envelope
        Envelope2D actual = mosaic.getEnvelope2D();

        // Check the same Bounding Box
        assertEqualBBOX(expected, actual);

        // Check that the final Coverage resolution is equal to that of the second coverage
        double initialRes = calculateResolution(resampled);
        double finalRes = calculateResolution(mosaic);
        double percentual = Math.abs(initialRes - finalRes) / initialRes;
        Assert.assertTrue(percentual < TOLERANCE);

        // Check that on the center of the image there are nodata
        DirectPosition point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(),
                actual.getCenterX(), actual.getCenterY());
        double nodata = CoverageUtilities.getBackgroundValues(coverage1)[0];
        double result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertEquals(nodata, result, TOLERANCE);

        // Check that on the Upper Left border pixel there is valid data
        point = new DirectPosition2D(mosaic.getCoordinateReferenceSystem(), actual.getMinX()
                + finalRes, actual.getMinY() + finalRes);
        result = ((int[]) mosaic.evaluate(point))[0];
        Assert.assertNotEquals(nodata, result, TOLERANCE);

        // Coverage and RenderedImage disposal
        mosaic.dispose(true);
        resampled.dispose(true);
        disposeCoveragePlanarImage(mosaic);
        disposeCoveragePlanarImage(resampled);
    }

    // Test which mosaics two input coverages with different CRS. An exception will be thrown
    @Test(expected = CoverageProcessingException.class)
    public void testWrongCRS() throws InvalidParameterValueException, ParameterNotFoundException,
            FactoryException {
        /*
         * Do the crop without conserving the envelope.
         */
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();

        // Creation of a List of the input Sources
        List<GridCoverage2D> sources = new ArrayList<GridCoverage2D>(2);
        sources.add(coverage1);

        // Reprojection of the second Coverage
        ParameterValueGroup paramReprojection = processor.getOperation("resample").getParameters();
        paramReprojection.parameter("Source").setValue(coverage2);
        paramReprojection.parameter("CoordinateReferenceSystem").setValue(
                CRS.parseWKT(GOOGLE_MERCATOR_WKT));
        GridCoverage2D resampled = (GridCoverage2D) processor.doOperation(paramReprojection);

        sources.add(resampled);
        // Setting of the sources
        param.parameter("Sources").setValue(sources);
        // Mosaic
        processor.doOperation(param);
    }

    @AfterClass
    public static void finalStep() {
        // Coverage and RenderedImage disposal
        coverage1.dispose(true);
        coverage2.dispose(true);
        disposeCoveragePlanarImage(coverage1);
        disposeCoveragePlanarImage(coverage2);
    }

    /**
     * Method for disposing the {@link RenderedImage} chain of the {@link GridCoverage2D}
     * 
     * @param coverage
     */
    private static void disposeCoveragePlanarImage(GridCoverage2D coverage) {
        ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(coverage
                .getRenderedImage()));
    }

    /**
     * Method for calculating the resolution of the input {@link GridCoverage2D}
     * 
     * @param coverage
     * @return
     */
    private static double calculateResolution(GridCoverage2D coverage) {
        GridGeometry2D gg2D = coverage.getGridGeometry();
        double envW = gg2D.getEnvelope2D().width;
        double gridW = gg2D.getGridRange2D().width;
        double res = envW / gridW;
        return res;
    }

    /**
     * Method which ensures that the two {@link Envelope2D} objects are equals.
     * 
     * @param expected
     * @param actual
     */
    private void assertEqualBBOX(Envelope2D expected, Envelope2D actual) {
        Assert.assertEquals(expected.getX(), actual.getX(), TOLERANCE);
        Assert.assertEquals(expected.getY(), actual.getY(), TOLERANCE);
        Assert.assertEquals(expected.getHeight(), actual.getHeight(), TOLERANCE);
        Assert.assertEquals(expected.getWidth(), actual.getWidth(), TOLERANCE);
    }
}