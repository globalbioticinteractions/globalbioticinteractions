package org.trophic.graph.factory;

import org.trophic.graph.dao.LocationDao;
import org.trophic.graph.dao.LocationDaoImpl;
import org.trophic.graph.db.Neo4JGraphDb;
import org.trophic.graph.service.LocationService;
import org.trophic.graph.service.LocationServiceImpl;

public abstract class LocationFactory {

    private static LocationDao locationDao;
    private static LocationService locationService;

    public static LocationDao getLocationDao(){
        if (locationDao == null){
            locationDao = new LocationDaoImpl();
            ((LocationDaoImpl)locationDao).setGraph(Neo4JGraphDb.getGraph());
        }
        return locationDao;
    }

    public static LocationService getLocationService(){
        if (locationService == null){
            locationService = new LocationServiceImpl();
            ((LocationServiceImpl)locationService).setLocationDao(getLocationDao());
        }
        return locationService;
    }

}
