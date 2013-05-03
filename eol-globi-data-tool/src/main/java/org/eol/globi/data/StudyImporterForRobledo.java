package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForRobledo extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForRobledo.class);

    public StudyImporterForRobledo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.createStudy("García-Robledo et al 2013"
                , "Carlos García-Robledo",
                "Department of Botany, National Museum of Natural History, Smithsonian Institution, Washington, D.C., United States of America; Department of Entomology, National Museum of Natural History, Smithsonian Institution, Washington, D.C., United States of America",
                "Oct 2008- Feb 2011",
                "<a href=\"http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0052967\">Tropical Plant–Herbivore Networks: Reconstructing Species Interactions Using DNA Barcodes</a>", "2013");

        Map<String, String> abrLookup = buildPlantLookup();

        // spatial location from: http://www.ots.ac.cr/index.php?option=com_content&task=view&id=163&Itemid=348
        Double latitude = LocationUtil.parseDegrees("10°26'N");
        Double longitude = LocationUtil.parseDegrees("83°59'W");
        Location location = nodeFactory.getOrCreateLocation(latitude, longitude, 35.0);

        // TODO: need to map date range of collections
        String studyResource = "robledo/table_s1_extract.csv";
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String beetleName = parser.getValueByLabel("Herbivore species");
                String beetleScientificName = completeBeetleName(beetleName);
                Specimen predator = nodeFactory.createSpecimen(beetleScientificName);
                predator.caughtIn(location);
                study.collected(predator);
                for (String plantAbbreviation : abrLookup.keySet()) {
                    String plantScientificName = abrLookup.get(plantAbbreviation);
                    String valueByLabel = parser.getValueByLabel(plantAbbreviation);
                    try {
                        int interactionCode = Integer.parseInt(valueByLabel);
                        if (interactionCode > 0) {
                            predator.ate(nodeFactory.createSpecimen(plantScientificName));
                        }
                    } catch (NumberFormatException ex) {
                        LOG.warn("malformed or no value [" + valueByLabel + "] found for [" + plantScientificName + "(" + plantAbbreviation + ")" + "] and beetle [" + beetleScientificName + "] could be found in [" + studyResource + ":" + parser.lastLineNumber() + "]");
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem reading [" + studyResource + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("cannot create specimens from [" + studyResource + "]", e);
        }


        return study;
    }

    private Map<String, String> buildPlantLookup() {
        Map<String, String> abrLookup = new HashMap<String, String>();
        abrLookup.put("Hir", "Heliconia imbricata");
        abrLookup.put("Him", "Heliconia irrasa");
        abrLookup.put("Hla", "Heliconia latispatha");
        abrLookup.put("Hmr", "Heliconia mariae");
        abrLookup.put("Hmt", "Heliconia mathiasiae");
        abrLookup.put("Hpo", "Heliconia pogonantha");
        abrLookup.put("Hpo", "Heliconia pogonantha");
        abrLookup.put("Hum", "Heliconia umbrophila");
        abrLookup.put("Hwa", "Heliconia wagneriana");
        abrLookup.put("Ral", "Renealmia alpinia");
        abrLookup.put("Rce", "Renealmia cernua");
        abrLookup.put("Rpl", "Renealmia pluriplicata");
        abrLookup.put("Cbr", "Costus bracteatus");
        abrLookup.put("Clae", "Costus laevis");
        abrLookup.put("Clim", "Costus lima");
        abrLookup.put("Cmal", "Costus malortieanus");
        abrLookup.put("Cpu", "Costus pulverulentus");
        abrLookup.put("Ccl", "Costus cleistantha");
        abrLookup.put("Ccr", "Costus crotalifera");
        abrLookup.put("Cgy", "Costus gymnocarpa");
        abrLookup.put("Cha", "Costus hammelii");
        abrLookup.put("Cin", "Costus inocephala");
        abrLookup.put("Clas", "Costus lasiostachya");
        abrLookup.put("Cle", "Costus leucostachys");
        abrLookup.put("Clu", "Costus lutea");
        abrLookup.put("Cma", "Costus marantifolia");
        abrLookup.put("Csi", "Costus similis");
        abrLookup.put("Cve", "Costus venusta");
        abrLookup.put("Cwa", "Costus warscewiczii");
        abrLookup.put("Iel", "Ischnosiphon elegans");
        abrLookup.put("Iin", "Ischnosiphon inflatus");
        abrLookup.put("Ppr", "Pleiostachya pruinosa");
        abrLookup.put("Ctu", "Canna tuerckheimii");
        return abrLookup;
    }

    private String completeBeetleName(final String name) throws StudyImporterException {
        String completedName = name.replaceAll("C\\.", "Cephaloleia");
        completedName = completedName.replaceAll("Ch\\.", "Chelobasis");
        if (completedName.contains(".")) {
            throw new StudyImporterException("failed to complete [" + name + "]: came up with [" + completedName + "]");
        }
        return completedName;
    }

}
