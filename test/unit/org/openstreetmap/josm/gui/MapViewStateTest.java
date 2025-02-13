// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;

/**
 * Test {@link MapViewState}
 * @author Michael Zangl
 */
class MapViewStateTest {

    private static final int WIDTH = 301;
    private static final int HEIGHT = 200;
    private MapViewState state;

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Create the default state.
     */
    @BeforeEach
    public void setUp() {
        state = MapViewState.createDefaultState(WIDTH, HEIGHT);
    }

    private void doTestGetCenter(Function<MapViewState, MapViewPoint> getter, Function<Integer, Double> divider) {
        MapViewPoint center = getter.apply(state);
        assertHasViewCoords(divider.apply(WIDTH), divider.apply(HEIGHT), center);

        MapViewState newState = state.movedTo(center, new EastNorth(3, 4));

        // state should not change, but new state should.
        center = getter.apply(state);
        assertHasViewCoords(divider.apply(WIDTH), divider.apply(HEIGHT), center);

        center = getter.apply(newState);
        assertEquals(3, center.getEastNorth().east(), 0.01, "east");
        assertEquals(4, center.getEastNorth().north(), 0.01, "north");
    }

    /**
     * Test {@link MapViewState#getCenter()} returns map view center.
     */
    @Test
    void testGetCenter() {
        doTestGetCenter(MapViewState::getCenter, t -> t / 2d);
    }

    private static void assertHasViewCoords(double x, double y, MapViewPoint center) {
        assertEquals(x, center.getInViewX(), 0.01, "x");
        assertEquals(y, center.getInViewY(), 0.01, "y");
        assertEquals(x, center.getInView().getX(), 0.01, "x");
        assertEquals(y, center.getInView().getY(), 0.01, "y");
    }

    /**
     * Test {@link MapViewState#getForView(double, double)}
     */
    @Test
    void testGetForView() {
        MapViewPoint corner = state.getForView(0, 0);
        assertHasViewCoords(0, 0, corner);

        MapViewPoint middle = state.getForView(120, 130);
        assertHasViewCoords(120, 130, middle);

        MapViewPoint fraction = state.getForView(0.12, 0.7);
        assertHasViewCoords(0.12, 0.7, fraction);

        MapViewPoint negative = state.getForView(-17, -30);
        assertHasViewCoords(-17, -30, negative);
    }

    /**
     * Test {@link MapViewState#getViewWidth()} and {@link MapViewState#getViewHeight()}
     */
    @Test
    void testGetViewSize() {
        assertEquals(WIDTH, state.getViewWidth(), 0.01);
        assertEquals(HEIGHT, state.getViewHeight(), 0.01);
    }

    /**
     * Tests that all coordinate conversions for the point work.
     */
    @Test
    void testPointConversions() {
        MapViewPoint p = state.getForView(WIDTH / 2d, HEIGHT / 2d);
        assertHasViewCoords(WIDTH / 2d, HEIGHT / 2d, p);

        EastNorth eastnorth = p.getEastNorth();
        LatLon shouldLatLon = ProjectionRegistry.getProjection().getWorldBoundsLatLon().getCenter();
        EastNorth shouldEastNorth = ProjectionRegistry.getProjection().latlon2eastNorth(shouldLatLon);
        assertEquals(shouldEastNorth.east(), eastnorth.east(), 0.01, "east");
        assertEquals(shouldEastNorth.north(), eastnorth.north(), 0.01, "north");
        MapViewPoint reversed = state.getPointFor(shouldEastNorth);
        assertHasViewCoords(WIDTH / 2d, HEIGHT / 2d, reversed);

        LatLon latlon = p.getLatLon();
        assertEquals(shouldLatLon.lat(), latlon.lat(), 0.01, "lat");
        assertEquals(shouldLatLon.lon(), latlon.lon(), 0.01, "lon");

        MapViewPoint p2 = state.getPointFor(new EastNorth(2, 3));
        assertEquals(2, p2.getEastNorth().east(), 0.01, "east");
        assertEquals(3, p2.getEastNorth().north(), 0.01, "north");
    }

    /**
     * Test {@link MapViewState#getAffineTransform()}
     */
    @Test
    void testGetAffineTransform() {
        for (EastNorth en : Arrays.asList(new EastNorth(100, 100), new EastNorth(0, 0), new EastNorth(300, 200),
                new EastNorth(-1, -2.5))) {
            MapViewPoint should = state.getPointFor(en);
            AffineTransform transform = state.getAffineTransform();
            Point2D result = transform.transform(new Point2D.Double(en.getX(), en.getY()), null);

            assertEquals(should.getInViewX(), result.getX(), 0.01, "x");
            assertEquals(should.getInViewY(), result.getY(), 0.01, "y");
        }
    }

    /**
     * Test {@link MapViewState#OUTSIDE_BOTTOM} and similar constants.
     */
    @Test
    void testOutsideFlags() {
        assertEquals(1, Integer.bitCount(MapViewState.OUTSIDE_BOTTOM));
        assertEquals(1, Integer.bitCount(MapViewState.OUTSIDE_TOP));
        assertEquals(1, Integer.bitCount(MapViewState.OUTSIDE_LEFT));
        assertEquals(1, Integer.bitCount(MapViewState.OUTSIDE_RIGHT));
        assertEquals(4, Integer.bitCount(MapViewState.OUTSIDE_BOTTOM | MapViewState.OUTSIDE_TOP
                | MapViewState.OUTSIDE_LEFT | MapViewState.OUTSIDE_RIGHT));
    }

    /**
     * Test {@link MapViewPoint#getOutsideRectangleFlags(MapViewRectangle)}
     */
    @Test
    void testPointGetOutsideRectangleFlags() {
        MapViewRectangle rect = state.getForView(0, 0).rectTo(state.getForView(10, 10));
        assertEquals(0, state.getForView(1, 1).getOutsideRectangleFlags(rect));
        assertEquals(0, state.getForView(1, 5).getOutsideRectangleFlags(rect));
        assertEquals(0, state.getForView(9, 1).getOutsideRectangleFlags(rect));
        assertEquals(0, state.getForView(10 - 1e-10, 1e-10).getOutsideRectangleFlags(rect));
        assertEquals(0, state.getForView(10 - 1e-10, 10 - 1e-10).getOutsideRectangleFlags(rect));


        assertEquals(MapViewState.OUTSIDE_TOP, state.getForView(1, -11).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_TOP, state.getForView(1, -1e20).getOutsideRectangleFlags(rect));

        assertEquals(MapViewState.OUTSIDE_BOTTOM, state.getForView(1, 11).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_BOTTOM, state.getForView(1, 1e20).getOutsideRectangleFlags(rect));

        assertEquals(MapViewState.OUTSIDE_LEFT, state.getForView(-11, 1).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_LEFT, state.getForView(-1e20, 1).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_RIGHT, state.getForView(11, 1).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_RIGHT, state.getForView(1e20, 1).getOutsideRectangleFlags(rect));

        assertEquals(MapViewState.OUTSIDE_RIGHT | MapViewState.OUTSIDE_TOP, state.getForView(11, -11).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_RIGHT | MapViewState.OUTSIDE_BOTTOM, state.getForView(11, 11).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_LEFT | MapViewState.OUTSIDE_TOP, state.getForView(-11, -11).getOutsideRectangleFlags(rect));
        assertEquals(MapViewState.OUTSIDE_LEFT | MapViewState.OUTSIDE_BOTTOM, state.getForView(-11, 11).getOutsideRectangleFlags(rect));
    }

    /**
     * Test {@link MapViewPoint#oneNormInView(MapViewPoint)}
     */
    @Test
    void testPointOneNormInView() {
        MapViewPoint p = state.getForView(5, 15);
        assertEquals(0, p.oneNormInView(p), 1e-10);
        assertEquals(6, p.oneNormInView(state.getForView(-1, 15)), 1e-10);
        assertEquals(5, p.oneNormInView(state.getForView(5, 20)), 1e-10);
        assertEquals(22, p.oneNormInView(state.getForView(-1, -1)), 1e-10);
        assertEquals(40, p.oneNormInView(state.getForView(30, 30)), 1e-10);
    }

    /**
     * Test {@link MapViewState.MapViewViewPoint#toString()} and {@link MapViewState.MapViewEastNorthPoint#toString()}
     */
    @Test
    void testToString() {
        assertEquals("MapViewViewPoint [x=1.0, y=2.0]",
                state.getForView(1, 2).toString());
        assertEquals("MapViewEastNorthPoint [eastNorth=EastNorth[e=0.0, n=0.0]]",
                state.getPointFor(new EastNorth(0, 0)).toString());
    }
}
