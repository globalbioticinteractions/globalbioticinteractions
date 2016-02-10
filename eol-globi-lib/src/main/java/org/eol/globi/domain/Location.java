package org.eol.globi.domain;

public interface Location  {
    Double getAltitude();

    Double getLongitude();

    Double getLatitude();

    String getFootprintWKT();
}