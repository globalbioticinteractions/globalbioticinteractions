package org.trophic.graph.dao;

import org.junit.Test;
import org.trophic.graph.domain.Location;
import org.trophic.graph.factory.LocationFactory;

import java.util.List;

public class LocationDaoTest {

    @Test
    public void test(){
		System.out.println("Start Location DAO Test");
        LocationDao dao = LocationFactory.getLocationDao();
        List<Location> locations = dao.getLocations();
        assert locations != null;
        assert locations.size() > 0;
        for (Location location : locations){
            System.out.println("Location: " + location.toString());
        }
		System.out.println("Start Location DAO Test");
    }

}
