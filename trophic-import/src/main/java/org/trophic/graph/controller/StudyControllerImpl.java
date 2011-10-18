package org.trophic.graph.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.trophic.graph.data.StudyImporter;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.data.StudyLibrary;

@Controller
public class StudyControllerImpl implements StudyController {

    @Autowired
    private StudyImporter studyImporter;

    @Override
    public String populate(Model model) {
        try {
            studyImporter.importStudy(StudyLibrary.MISSISSIPPI_ALABAMA);
            studyImporter.importStudy(StudyLibrary.LAVACA_BAY);
        } catch (StudyImporterException e) {
            throw new RuntimeException("failed to populate", e);
        }

        return "/study/list";
    }

    public void setStudyImporter(StudyImporter studyImporter) {
        this.studyImporter = studyImporter;
    }


}
