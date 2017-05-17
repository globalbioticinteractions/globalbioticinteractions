package org.eol.globi.domain;

import java.util.List;

public interface Location  {
    Double getAltitude();

    Double getLongitude();

    Double getLatitude();

    String getFootprintWKT();

    // see http://rs.tdwg.org/dwc/terms/#locality
    String getLocality();

    String getLocalityId();

    void addEnvironment(Environment environment);

    List<Environment> getEnvironments();
}