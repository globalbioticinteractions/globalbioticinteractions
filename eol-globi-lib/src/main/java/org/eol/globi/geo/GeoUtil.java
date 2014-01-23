package org.eol.globi.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GeoUtil {
    public static Point getPoint(double lat, double lng) {
        return new GeometryFactory().createPoint(new Coordinate(lng, lat));
    }

    public static Coordinate getCoordinate(double lat, double lng) {
        return new Coordinate(lng, lat);
    }
}
