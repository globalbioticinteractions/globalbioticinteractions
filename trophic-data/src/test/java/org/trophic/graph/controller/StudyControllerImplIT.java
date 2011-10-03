package org.trophic.graph.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.data.StudyImporter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class StudyControllerImplIT {
    @Autowired
    StudyController studyController;

    @Test
    public void emptyTestForWiring() throws Exception {

    }
}
