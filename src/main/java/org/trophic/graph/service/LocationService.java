package org.trophic.graph.service;

import org.trophic.graph.domain.LocationDto;

import java.util.List;

public interface LocationService {
	
	List<LocationDto> getStudyLocations();

}