package org.trophic.graph.dao;

import org.junit.Test;
import org.trophic.graph.dto.SpecimenDto;
import org.trophic.graph.factory.SpecimenFactory;

import java.util.List;

public class SpecimenDaoTest {

    @Test
    public void test(){
		System.out.println("Start Location DAO Test");
        SpecimenDao dao = SpecimenFactory.getSpecimenDao();
        List<SpecimenDto> specimens = dao.getSpecimens(null);
        assert specimens != null;
        assert specimens.size() > 0;
        for (SpecimenDto specimenDto : specimens){
            System.out.println("Location: " + specimenDto.toString());
        }
		System.out.println("Start Location DAO Test");
    }

}
