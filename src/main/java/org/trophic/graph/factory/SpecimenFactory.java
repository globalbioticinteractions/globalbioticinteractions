package org.trophic.graph.factory;

import org.trophic.graph.dao.SpecimenDao;
import org.trophic.graph.dao.SpecimentDaoJava;
import org.trophic.graph.service.SpecimenService;
import org.trophic.graph.service.SpecimenServiceImpl;

public abstract class SpecimenFactory {

    private static SpecimenDao specimenDao;
    private static SpecimenService specimentService;

    public static SpecimenDao getSpecimenDao() {
        if (specimenDao == null){
            createLocationDaoJava();
        }
        return specimenDao;
    }

    private static void createLocationDaoJava(){
        try{
            SpecimentDaoJava dao = new SpecimentDaoJava();
            specimenDao = dao;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static SpecimenService getSpecimenService() throws Exception {
        if (specimentService == null){
            specimentService = new SpecimenServiceImpl();
            ((SpecimenServiceImpl) specimentService).setSpecimenDao(getSpecimenDao());
        }
        return specimentService;
    }

}