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

    public static boolean isSameLocation(Location location, Location foundLocation) {
        return sameLatitude(location, foundLocation)
                && sameLongitude(location, foundLocation)
                && sameAltitude(location, foundLocation)
                && sameFootprintWKT(location, foundLocation)
                && sameLocality(location, foundLocation)
                && sameLocalityId(location, foundLocation);
    }

    private static boolean sameLocalityId(Location location, Location foundLocation) {
        return foundLocation.getLocalityId() == null && location.getLocalityId() == null
                || location.getLocalityId() != null && location.getLocalityId().equals(foundLocation.getLocalityId());
    }

    private static boolean sameLocality(Location location, Location foundLocation) {
        return foundLocation.getLocality() == null && location.getLocality() == null
                || location.getLocality() != null && location.getLocality().equals(foundLocation.getLocality());
    }

    private static boolean sameFootprintWKT(Location location, Location foundLocation) {
        return foundLocation.getFootprintWKT() == null && location.getFootprintWKT() == null
                || location.getFootprintWKT() != null && location.getFootprintWKT().equals(foundLocation.getFootprintWKT());
    }

    private static boolean sameAltitude(Location location, Location foundLocation) {
        return foundLocation.getAltitude() == null && location.getAltitude() == null
                || location.getAltitude() != null && location.getAltitude().equals(foundLocation.getAltitude());
    }

    private static boolean sameLatitude(Location location, Location foundLocation) {
        return foundLocation.getLatitude() == null && location.getLatitude() == null
                || location.getLatitude() != null && location.getLatitude().equals(foundLocation.getLatitude());
    }

    private static boolean sameLongitude(Location location, Location foundLocation) {
        return foundLocation.getLongitude() == null && location.getLongitude() == null
                || location.getLongitude() != null && location.getLongitude().equals(foundLocation.getLongitude());
    }

    public static boolean hasLatLng(Location location) {
        return location.getLatitude() != null && location.getLongitude() != null;
    }
}
