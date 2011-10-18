package org.trophic.graph.controller;

import org.junit.Test;
import org.trophic.graph.data.StudyImporter;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.data.StudyLibrary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class StudyControllerImplTest {
    @Test
    public void populate() throws StudyImporterException {
        StudyControllerImpl controller = new StudyControllerImpl();
        StudyImporter studyImporter = mock(StudyImporter.class);
        controller.setStudyImporter(studyImporter);
        controller.populate(null);
        verify(studyImporter).importStudy(StudyLibrary.MISSISSIPPI_ALABAMA);
        verify(studyImporter).importStudy(StudyLibrary.LAVACA_BAY);
    }
}
