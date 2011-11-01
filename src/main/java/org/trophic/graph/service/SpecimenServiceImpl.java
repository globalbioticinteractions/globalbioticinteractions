package org.trophic.graph.service;

import org.trophic.graph.dao.SpecimenDao;
import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public class SpecimenServiceImpl implements SpecimenService {

    private SpecimenDao locationDao;
	
	@Override
	public List<SpecimenDto> getSpecimens() {
        List<SpecimenDto> result = locationDao.getSpecimens(null);
		return result;
	}

    public void setLocationDao(SpecimenDao locationDao) {
        this.locationDao = locationDao;
    }

}