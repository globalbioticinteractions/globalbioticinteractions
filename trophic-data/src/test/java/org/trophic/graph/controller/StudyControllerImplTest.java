package org.trophic.graph.controller;

import org.junit.Test;
import org.trophic.graph.data.StudyImporter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class StudyControllerImplTest {
    @Test
    public void populate() throws Exception {
        StudyControllerImpl controller = new StudyControllerImpl();
        StudyImporter studyImporter = mock(StudyImporter.class);
        controller.setStudyImporter(studyImporter);
        controller.populate(null);
        verify(studyImporter).importStudy();
    }
}
