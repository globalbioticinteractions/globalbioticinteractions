package org.eol.globi.service;

import org.eol.globi.geo.LatLng;

import java.io.IOException;

public interface GeoNamesService {

    boolean hasPositionForLocality(String locality);

    LatLng findPointForLocality(String locality) throws IOException;

}
