package org.trophic.graph.service;

import org.trophic.graph.dao.SpecimenDao;
import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public class SpecimenServiceImpl implements SpecimenService {

    private SpecimenDao specimenDao;
    private List<SpecimenDto> specimens;
	
	@Override
	public List<SpecimenDto> getSpecimens() {
        if (specimens == null)
            specimens = specimenDao.getSpecimens(null);
		return specimens;
	}

    @Override
    public List<SpecimenDto> getSpecimensByLocation(String latitude, String longitude) {
        return specimenDao.getSpecimensByLocation(latitude, longitude);
    }

    public void setSpecimenDao(SpecimenDao specimenDao) {
        this.specimenDao = specimenDao;
    }

}