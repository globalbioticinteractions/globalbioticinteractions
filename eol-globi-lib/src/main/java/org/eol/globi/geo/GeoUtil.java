package org.eol.globi.geo;

import com.jillesvangurp.geo.GeoHashUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.lang.StringUtils;

public class GeoUtil {
    public static Point getPoint(double lat, double lng) {
        return new GeometryFactory().createPoint(new Coordinate(lng, lat));
    }

    public static Coordinate getCoordinate(double lat, double lng) {
        return new Coordinate(lng, lat);
    }

    public static String createGeoHashPrefixes(Double latitude, Double longitude, int numberOfCharacters) {
        String fullHash = GeoHashUtils.encode(latitude, longitude, numberOfCharacters);
        String[] prefixes = new String[fullHash.length()];
        for (int i=0; i < prefixes.length; i++) {
            prefixes[i] = StringUtils.substring(fullHash, 0, i+1);
        }

        return StringUtils.join(prefixes, " ");
    }
}
