package org.trophic.graph.service;

import org.trophic.graph.dao.LocationDao;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.LocationDto;

import java.util.ArrayList;
import java.util.List;

public class LocationServiceImpl implements LocationService {

    private LocationDao locationDao;
	
	@Override
	public List<Location> getLocations() {
        List<Location> result = locationDao.getLocations();
		return result;
	}
	
	private List<LocationDto> getMockLocations() {
		LocationDto location = new LocationDto();
		location.setName("Location Alpha 1");
		location.setLatitude(59.91908D);
		location.setLongitude(30.26113D);
		location.setId("S44");
		
		LocationDto location2 = new LocationDto();
		location2.setName("Location Alpha 2");
		location2.setLatitude(45.39601D);
		location2.setLongitude(35.79723D);
		location2.setId("S45");
		
		List<LocationDto> locations = new ArrayList<LocationDto>();
		locations.add(location2);
		locations.add(location);
		
		return locations;
	}

    public void setLocationDao(LocationDao locationDao) {
        this.locationDao = locationDao;
    }
}