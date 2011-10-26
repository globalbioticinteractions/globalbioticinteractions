package org.trophic.graph.factory;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.trophic.graph.dao.LocationDao;
import org.trophic.graph.dao.LocationDaoGroovy;
import org.trophic.graph.dao.LocationDaoJava;
import org.trophic.graph.service.LocationService;
import org.trophic.graph.service.LocationServiceImpl;

public abstract class LocationFactory {

    private static LocationDao locationDao;
    private static LocationService locationService;

    public static LocationDao getLocationDao() {
        if (locationDao == null){
            createLocationDaoJava();
        }
        return locationDao;
    }

    private static void createLocationDaoGroovy(){
        try{
            GroovyClassLoader gcl = new GroovyClassLoader();
            Class clazz = gcl.parseClass("src/main/java/org/trophic/graph/dao/LocationDaoRepo.groovy");
            GroovyObject groovyObject = (GroovyObject) clazz.newInstance();
            LocationDaoGroovy impl = new LocationDaoGroovy();
            impl.setLocationRepo(groovyObject);
            locationDao = impl;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void createLocationDaoJava(){
        try{
            LocationDaoJava dao = new LocationDaoJava();
            locationDao = dao;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static LocationService getLocationService() throws Exception {
        if (locationService == null){
            locationService = new LocationServiceImpl();
            ((LocationServiceImpl)locationService).setLocationDao(getLocationDao());
        }
        return locationService;
    }



}