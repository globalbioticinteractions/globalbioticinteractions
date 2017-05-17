package org.eol.globi.domain;

import java.util.ArrayList;
import java.util.List;

public class LocationImpl implements Location {
    private Double altitude;
    private Double longitude;
    private Double latitude;
    private String footprintWKT;
    private String locality;
    private String localityId;
    private final List<Environment> environments = new ArrayList<>();

    public LocationImpl(Double latitude, Double longitude, Double altitude, String footprintWKT) {
        this.altitude = altitude;
        this.longitude = longitude;
        this.latitude = latitude;
        this.footprintWKT = footprintWKT;
        this.locality = null;
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

    @Override
    public String getLocality() {
        return locality;
    }

    @Override
    public void addEnvironment(Environment environment) {
        environments.add(environment);
    }

    @Override
    public List<Environment> getEnvironments() { return environments; }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    @Override
    public String getLocalityId() {
        return localityId;
    }

    public void setLocalityId(String localityId) {
        this.localityId = localityId;
    }
}