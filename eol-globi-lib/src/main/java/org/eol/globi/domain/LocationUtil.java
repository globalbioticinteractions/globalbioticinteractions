package org.eol.globi.domain;

public class LocationUtil {
    public static Location fromLocation(Location location) {
        return new LocationImpl(location.getLatitude(),
                location.getLongitude(),
                location.getAltitude(),
                location.getFootprintWKT());
    }
}
