package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForDunne extends StudyImporterNodesAndLinks {


    public StudyImporterForDunne(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    Study createStudy() throws NodeFactoryException {
        return nodeFactory.getOrCreateStudy(new StudyImpl(getNamespace(), getSourceCitationLastAccessed(), getSourceDOI(), null));
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Study study = createStudy();
        try {
            LabeledCSVParser nodes = parserFactory.createParser(getNodesResourceName(), CharsetConstant.UTF8);
            nodes.changeDelimiter(getDelimiter());

            Map<Integer, Taxon> taxonForNode = new HashMap<Integer, Taxon>();


            while (nodes.getLine() != null) {
                Integer nodeId = getNodeId(nodes);
                if (nodeId != null) {
                    final String tsn = nodes.getValueByLabel("TSN");
                    taxonForNode.put(nodeId, new TaxonImpl(nodes.getValueByLabel("Name"), TaxonomyProvider.ID_PREFIX_ITIS + tsn));
                }
            }

            LabeledCSVParser links = parserFactory.createParser(getLinksResourceName(), CharsetConstant.UTF8);
            links.changeDelimiter(getDelimiter());

            while (links.getLine() != null) {
                List<Location> locations = new ArrayList<>();
                if (getLocation() != null) {
                    Location loc = nodeFactory.getOrCreateLocation(new LocationImpl(getLocation().getLat(), getLocation().getLng(), null, null));
                    if (loc != null) {
                        locations.add(loc);
                    }
                }

                for (Location location : locations) {
                    addLink(study, taxonForNode, links, location);
                }
            }

        } catch (IOException e) {
            throw new StudyImporterException("failed to find data file(s)", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create nodes", e);
        }
    }

    protected Integer getNodeId(LabeledCSVParser nodes) {
        String nodeID = nodes.getValueByLabel("ID");
        return nodeID == null ? null : Integer.parseInt(nodeID);
    }

    private void addLink(Study study, Map<Integer, Taxon> taxonForNode, LabeledCSVParser links, Location location) throws StudyImporterException {
        Integer consumerNodeID = Integer.parseInt(links.getValueByLabel("Consumer"));
        Integer resourceNodeID = Integer.parseInt(links.getValueByLabel("Resource"));
        Specimen consumer = nodeFactory.createSpecimen(study, taxonForNode.get(consumerNodeID));
        consumer.setExternalId(getNamespace() + ":NodeID:" + consumerNodeID);
        consumer.caughtIn(location);
        Specimen resource = nodeFactory.createSpecimen(study, taxonForNode.get(resourceNodeID));
        resource.setExternalId(getNamespace() + ":NodeID:" + resourceNodeID);
        resource.caughtIn(location);
        consumer.interactsWith(resource, InteractType.ATE);
    }


}
