package org.trophic.graph.service;

import java.util.ArrayList;
import java.util.List;

import org.trophic.graph.domain.LocationPure;

public class LocationServiceImpl implements LocationService {

	private static LocationServiceImpl instance = null;
	
	public static LocationServiceImpl getInstance(){
		if (instance == null)
			instance = new LocationServiceImpl();
		return instance;
	}
	
	@Override
	public List<LocationPure> getStudyLocations() {
		return getMockLocations();
	}
	
	private List<LocationPure> getMockLocations() {
		LocationPure location = new LocationPure();
		location.setLatitude(544D);
		location.setLongitude(23488D);
		location.setId("S44");
		
		LocationPure location2 = new LocationPure();
		location2.setLatitude(543D);
		location2.setLongitude(23418D);
		location2.setId("S45");
		
		List<LocationPure> locations = new ArrayList<LocationPure>();
		locations.add(location2);
		locations.add(location);
		
		return locations;
	}

}