package org.trophic.graph.dao;

import org.junit.Test;
import org.trophic.graph.factory.LocationFactory;

public class LocationDaoTest {

    @Test
    public void init(){
		System.out.println("Start Location DAO Test");
        LocationDao dao = LocationFactory.getLocationDao();
        String hallo = dao.sayHallo();
        assert hallo != null;
        System.out.println(hallo);
         dao.getLocations();
//        assert map != null;
//        assert map.size() > 0;
		System.out.println("Start Location DAO Test");
    }



}
