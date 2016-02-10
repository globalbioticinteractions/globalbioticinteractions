package org.eol.globi.domain;

public class LocationImpl implements Location {
    private Double altitude;
    private Double longitude;
    private Double latitude;
    private String footprintWKT;

    public LocationImpl(Double latitude, Double longitude, Double altitude, String footprintWKT) {
        this.altitude = altitude;
        this.longitude = longitude;
        this.latitude = latitude;
        this.footprintWKT = footprintWKT;
    }


    @Override
    public Double getAltitude() {
        return altitude;
    }

    @Override
    public Double getLongitude() {
        return longitude;
    }

    @Override
    public Double getLatitude() {
        return latitude;
    }

    @Override
    public String getFootprintWKT() {
        return footprintWKT;
    }
}