package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForHechinger extends StudyImporterNodesAndLinks {

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
        put("concomitant predation on symbionts", InteractType.PREYS_UPON);
        put("predation", InteractType.PREYS_UPON);
        put("social predation", InteractType.PREYS_UPON);
        put("micropredation", InteractType.INTERACTS_WITH);
        put("parasitic castration", InteractType.PARASITE_OF);
        put("pathogen infection", InteractType.PATHOGEN_OF);
        put("macroparasitism", InteractType.PARASITE_OF);
        put("pollination", InteractType.POLLINATES);
        put("parasitoid infection", InteractType.PARASITE_OF);
        put("commensalism", InteractType.SYMBIONT_OF);
        put("trophically transmitted parasitic castration", InteractType.PARASITE_OF);
        put("trophically transmitted pathogen infection", InteractType.PATHOGEN_OF);
        put("trophically transmitted parasitism", InteractType.PARASITE_OF);
        put("trophically transmitted commensalism", InteractType.INTERACTS_WITH);
        put("concomitant predation on symbionts", InteractType.SYMBIONT_OF);
        put("trophic transmission", InteractType.INTERACTS_WITH);
        put("predation on free-living non-feeding stages", InteractType.PREYS_UPON);
        put("predation on commensal non-feeding stages", InteractType.PREYS_UPON);
        put("detritivory", InteractType.ATE);
        put("parasite intraguild trophic interaction", InteractType.INTERACTS_WITH);
        put("intimate and durable mutualism", InteractType.SYMBIONT_OF);
        put("facultative micropredation", InteractType.PREYS_UPON);

    }};

    public StudyImporterForHechinger(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = createStudy();
        try {
            LabeledCSVParser nodes = parserFactory.createParser(getNodeResource(), CharsetConstant.UTF8);

            nodes.changeDelimiter(getDelimiter());

            Map<Integer, Term> stageForNode = new HashMap<Integer, Term>();
            Map<Integer, String> taxonForNode = new HashMap<Integer, String>();


            while (nodes.getLine() != null) {
                Integer nodeId = getNodeId(nodes);
                if (nodeId != null) {
                    String name = parseMostGranularTaxonName(nodes);
                    if (StringUtils.isBlank(name)) {
                        name = nodes.getValueByLabel("WorkingName");
                        if (StringUtils.isBlank(name)) {
                            getLogger().warn(study, "failed to findNamespaces name for node on line [" + nodes.lastLineNumber() + "]");
                        }
                    }

                    if (StringUtils.isNotBlank(name)) {
                        try {
                            taxonForNode.put(nodeId, name);
                            String stage = nodes.getValueByLabel("Stage");
                            stageForNode.put(nodeId, nodeFactory.getOrCreateLifeStage(getNamespace() + ":" + stage, stage));
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to reader line [" + nodes.lastLineNumber() + "]", e);
                        }
                    }
                }
            }

            LabeledCSVParser links = parserFactory.createParser(getLinkResource(), CharsetConstant.UTF8);
            links.changeDelimiter(getDelimiter());
            while (links.getLine() != null) {
                List<LocationNode> locations = new ArrayList<LocationNode>();
                if (getLocation() != null) {
                    LocationNode loc = nodeFactory.getOrCreateLocation(getLocation().getLat(), getLocation().getLng(), null);
                    if (loc != null) {
                        locations.add(loc);
                    }
                }

                if (StringUtils.equals("1", links.getValueByLabel("PresentAtCSM"))) {
                    locations.add(nodeFactory.getOrCreateLocation(34.403511, -119.537873, null));
                }

                if (StringUtils.equals("1", links.getValueByLabel("PresentAtEPB"))) {
                    locations.add(nodeFactory.getOrCreateLocation(31.748606, -116.626854, null));
                }

                if (StringUtils.equals("1", links.getValueByLabel("PresentAtBSQ"))) {
                    locations.add(nodeFactory.getOrCreateLocation(30.378207, -115.938835, null));
                }

                for (LocationNode location : locations) {
                    addLink(study, stageForNode, taxonForNode, links, location);
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to findNamespaces data file(s)", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create nodes", e);
        }


        return study;
    }

    protected Integer getNodeId(LabeledCSVParser nodes) {
        String nodeID = nodes.getValueByLabel("NodeID");
        if (StringUtils.isBlank(nodeID)) {
            nodeID = nodes.getValueByLabel("Node ID");
        }
        return nodeID == null ? null : Integer.parseInt(nodeID);
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

    private void addLink(Study study, Map<Integer, Term> stageForNode, Map<Integer, String> taxonForNode, LabeledCSVParser links, LocationNode location) throws StudyImporterException, NodeFactoryException {
        Integer consumerNodeID = Integer.parseInt(links.getValueByLabel("ConsumerNodeID"));
        Integer resourceNodeID = Integer.parseInt(links.getValueByLabel("ResourceNodeID"));
        String linkType = links.getValueByLabel("LinkType");
        InteractType interactType = interactionMapping.get(StringUtils.trim(StringUtils.lowerCase(linkType)));
        if (interactType == null) {
            throw new StudyImporterException("failed to map interaction type [" + linkType + "] in line [" + links.lastLineNumber() + "]");
        }
        Specimen consumer = nodeFactory.createSpecimen(study, taxonForNode.get(consumerNodeID));
        consumer.setLifeStage(stageForNode.get(consumerNodeID));
        consumer.setExternalId(getNamespace() + ":NodeID:" + consumerNodeID);
        consumer.caughtIn(location);
        String resourceName = taxonForNode.get(resourceNodeID);
        Specimen resource = nodeFactory.createSpecimen(study, resourceName);
        resource.setLifeStage(stageForNode.get(resourceNodeID));
        resource.setExternalId(getNamespace() + ":NodeID:" + resourceNodeID);
        resource.caughtIn(location);
        consumer.interactsWith(resource, interactType);
    }

}
