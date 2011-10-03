package org.trophic.graph.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.trophic.graph.data.ParserFactoryImpl;
import org.trophic.graph.data.StudyImporter;
import org.trophic.graph.data.StudyImporterImpl;

import java.io.IOException;

@Controller
public class StudyControllerImpl implements StudyController {

    @Autowired
    private StudyImporter studyImporter;

    @Override
    public String populate(Model model) {
        try {
            studyImporter.importStudy();
        } catch (IOException e) {
            throw new RuntimeException("failed to populate", e);
        }

        return "/study/list";
    }

    public void setStudyImporter(StudyImporter studyImporter) {
        this.studyImporter = studyImporter;
    }


}
