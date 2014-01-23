package org.eol.globi.service;

import uk.me.jstott.jcoord.LatLng;

import java.io.IOException;

public interface GeoNamesService {

    boolean hasPositionForLocality(String spireLocality);

    LatLng findPointForLocality(String spireLocality) throws IOException;

    LatLng findLatLng(Long id) throws IOException;
}
