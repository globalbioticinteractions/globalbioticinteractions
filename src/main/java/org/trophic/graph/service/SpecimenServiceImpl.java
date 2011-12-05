package org.trophic.graph.service;

import org.trophic.graph.dao.SpecimenDao;
import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public class SpecimenServiceImpl implements SpecimenService {

    private SpecimenDao specimenDao;
	
	@Override
	public List<SpecimenDto> getSpecimens() {
        List<SpecimenDto> result = specimenDao.getSpecimens(null);
		return result;
	}

    @Override
    public List<SpecimenDto> getSpecimensByLocation(String latitude, String longitude) {
        return specimenDao.getSpecimensByLocation(latitude, longitude);
    }

    public void setSpecimenDao(SpecimenDao specimenDao) {
        this.specimenDao = specimenDao;
    }

}