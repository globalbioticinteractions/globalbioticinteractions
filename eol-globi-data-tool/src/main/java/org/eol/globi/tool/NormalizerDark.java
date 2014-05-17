package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForFWDP;

import java.util.ArrayList;

public class NormalizerDark {

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new Normalizer().normalize(new ArrayList<Class>() {{
            add(StudyImporterForFWDP.class);
        }});
    }

}