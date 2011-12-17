package org.trophic.graph.dao;

import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public interface SpecimenDao {



    List<SpecimenDto> getSpecimens();

    List<SpecimenDto> getAllSpecimens();

    List<SpecimenDto> getSpecimensByLocation(String latitude, String longitude);

    void updateSpecimenWithThumbnail(SpecimenDto specimenDto);

    SpecimenDto getSpecimenById(Long id);

}