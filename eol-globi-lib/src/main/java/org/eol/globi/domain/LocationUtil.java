package org.eol.globi.domain;

public class LocationUtil {
    public static Location fromLocation(Location fromLocation) {
        final LocationImpl toLocation = new LocationImpl(fromLocation.getLatitude(),
                fromLocation.getLongitude(),
                fromLocation.getAltitude(),
                fromLocation.getFootprintWKT());
        toLocation.setLocality(fromLocation.getLocality());
        toLocation.setLocalityId(fromLocation.getLocalityId());
        return toLocation;
    }
}
