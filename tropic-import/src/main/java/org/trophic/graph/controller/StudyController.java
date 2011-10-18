package org.trophic.graph.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public interface StudyController {
    @RequestMapping(value = "/study/populate", method = RequestMethod.GET)
    String populate(Model model);
}
