package org.eol.globi.service;

import org.eol.globi.geo.LatLng;

import java.io.IOException;

public interface GeoNamesService {

    boolean hasTermForLocale(String locality);

    LatLng findLatLng(String locality) throws IOException;

}
