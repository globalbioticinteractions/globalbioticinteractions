package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForHechinger extends BaseStudyImporter {

    private static final Map<String, InteractType> interactionMapping = new HashMap<String, InteractType>() {{
        put("commensalism", InteractType.INTERACTS_WITH);
        put("predation", InteractType.PREYS_UPON);
        put("trophic transmission", InteractType.HOST_OF);
        put("parasitic castration", InteractType.PARASITE_OF);
        put("macroparasitism", InteractType.PARASITE_OF);
        put("trophically transmitted parasitism", InteractType.PARASITE_OF);
        put("predation on free-living non-feeding stage", InteractType.ATE);
        put("parasitoid infection", InteractType.PARASITE_OF);
        put("parasite intraguild antagonism", InteractType.PARASITE_OF);
        put("detritivory", InteractType.ATE);
        put("pathogen infection", InteractType.PATHOGEN_OF);
        put("concurrent predation on symbionts", InteractType.INTERACTS_WITH);
        put("micropredation", InteractType.PREYS_UPON);
    }};

    public StudyImporterForHechinger(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = createStudy();
        try {
            Location carpenteriaSaltMarsh = nodeFactory.getOrCreateLocation(34.403511, -119.537873, null);
            Location esteroDePuntaBanda = nodeFactory.getOrCreateLocation(31.748606, -116.626854, null);
            Location bahiaSanQuintin = nodeFactory.getOrCreateLocation(30.378207, -115.938835, null);

            LabeledCSVParser nodes = parserFactory.createParser("hechinger/Metaweb_Nodes.txt", CharsetConstant.UTF8);
            nodes.changeDelimiter('\t');

            Map<Integer, Term> stageForNode = new HashMap<Integer, Term>();
            Map<Integer, String> taxonForNode = new HashMap<Integer, String>();


            while (nodes.getLine() != null) {
                Integer nodeId = Integer.parseInt(nodes.getValueByLabel("NodeID"));

                String name = parseMostGranularTaxonName(nodes);
                if (StringUtils.isBlank(name)) {
                    name = nodes.getValueByLabel("WorkingName");
                    if (StringUtils.isBlank(name)) {
                        getLogger().warn(study, "failed to find name for node on line [" + nodes.lastLineNumber() + "]");
                    }
                }

                if (StringUtils.isNotBlank(name)) {
                    try {
                        taxonForNode.put(nodeId, name);
                        String stage = nodes.getValueByLabel("Stage");
                        stageForNode.put(nodeId, nodeFactory.getOrCreateLifeStage("hechinger:" + stage, stage));
                    } catch (NodeFactoryException e) {
                        throw new StudyImporterException("failed to reader line [" + nodes.lastLineNumber() + "]", e);
                    }
                }
            }

            LabeledCSVParser links = parserFactory.createParser("hechinger/Metaweb_Links.txt", CharsetConstant.UTF8);
            links.changeDelimiter('\t');
            while (links.getLine() != null) {
                List<Location> locations = new ArrayList<Location>();
                if (StringUtils.equals("1", links.getValueByLabel("PresentAtCSM"))) {
                    locations.add(carpenteriaSaltMarsh);
                }

                if (StringUtils.equals("1", links.getValueByLabel("PresentAtEPB"))) {
                    locations.add(esteroDePuntaBanda);
                }

                if (StringUtils.equals("1", links.getValueByLabel("PresentAtBSQ"))) {
                    locations.add(bahiaSanQuintin);
                }

                for (Location location : locations) {
                    addLink(study, stageForNode, taxonForNode, links, location);
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to find data file(s)", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create nodes", e);
        }


        return study;
    }

    private String parseMostGranularTaxonName(LabeledCSVParser nodes) {
        String name = null;
        String ranks[] = {"Kingdom", "Phylum", "Subphylum", "Superclass", "Class", "Subclass", "Order", "Suborder", "Infraorder", "Superfamily", "Family"};
        String specificEpithet = nodes.getValueByLabel("SpecificEpithet");
        String genus = nodes.getValueByLabel("Genus");
        if (StringUtils.isNotBlank(genus)) {
            name = genus;
            if (StringUtils.isNotBlank(specificEpithet)) {
                name += " " + specificEpithet;
            }
        } else {
            for (int i = ranks.length - 1; i >= 0; i--) {
                name = nodes.getValueByLabel(ranks[i]);
                if (StringUtils.isNotBlank(name)) {
                    break;
                }
            }
        }
        return name;
    }

    private void addLink(Study study, Map<Integer, Term> stageForNode, Map<Integer, String> taxonForNode, LabeledCSVParser links, Location location) throws StudyImporterException, NodeFactoryException {
        Integer consumerNodeID = Integer.parseInt(links.getValueByLabel("ConsumerNodeID"));
        Integer resourceNodeID = Integer.parseInt(links.getValueByLabel("ResourceNodeID"));
        String linkType = links.getValueByLabel("LinkType");
        InteractType interactType = interactionMapping.get(linkType);
        if (interactType == null) {
            throw new StudyImporterException("failed to map interaction type [" + linkType + "] in line [" + links.lastLineNumber() + "]");
        }
        Specimen consumer = nodeFactory.createSpecimen(study, taxonForNode.get(consumerNodeID));
        consumer.setLifeStage(stageForNode.get(consumerNodeID));
        consumer.setExternalId("NodeID:" + consumerNodeID);
        consumer.caughtIn(location);
        Specimen resource = nodeFactory.createSpecimen(study, taxonForNode.get(resourceNodeID));
        resource.setLifeStage(stageForNode.get(resourceNodeID));
        resource.setExternalId("NodeID:" + resourceNodeID);
        resource.caughtIn(location);
        consumer.interactsWith(resource, interactType);
    }

    private Study createStudy() {
        String description = "Ryan F. Hechinger, Kevin D. Lafferty, John P. McLaughlin, Brian L. Fredensborg, Todd C. Huspeni, Julio Lorda, Parwant K. Sandhu, Jenny C. Shaw, Mark E. Torchin, Kathleen L. Whitney, and Armand M. Kuris 2011. Food webs including parasites, biomass, body sizes, and life stages for three California/Baja California estuaries. Ecology 92:791–791. http://dx.doi.org/10.1890/10-1383.1 . ";
        return nodeFactory.getOrCreateStudy("hechinger2011", description, "http://dx.doi.org/10.1890/10-1383.1");
    }
}
