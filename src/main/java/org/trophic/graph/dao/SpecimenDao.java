package org.trophic.graph.dao;

import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

public interface SpecimenDao {

    List<SpecimenDto> getSpecimens(String[] studies);

}