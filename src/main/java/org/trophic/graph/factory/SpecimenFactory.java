package org.trophic.graph.factory;

import org.trophic.graph.dao.SpecimenDao;
import org.trophic.graph.dao.SpecimentDaoJava;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.service.SpecimenService;
import org.trophic.graph.service.SpecimenServiceImpl;

public abstract class SpecimenFactory {

    private static SpecimenDao specimenDao;
    private static SpecimenService specimentService;

    public static SpecimenDao getSpecimenDao() {
        if (specimenDao == null){
            createSpecimenDaoJava();
        }
        return specimenDao;
    }

    private static void createSpecimenDaoJava(){
        try{
            SpecimentDaoJava dao = new SpecimentDaoJava(GraphService.getGraphService());
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