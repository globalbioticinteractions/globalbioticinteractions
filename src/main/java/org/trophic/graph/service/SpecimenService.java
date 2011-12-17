package org.trophic.graph.service;

import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public interface SpecimenService {
	
	List<SpecimenDto> getSpecimens();

    List<SpecimenDto> getAllSpecimens();

    List<SpecimenDto> getSpecimensByLocation(String latitude, String longitude);

    void updateSpecimenWithThumbnail(SpecimenDto specimenDto);

    SpecimenDto getSpecimenById(Long id);

}