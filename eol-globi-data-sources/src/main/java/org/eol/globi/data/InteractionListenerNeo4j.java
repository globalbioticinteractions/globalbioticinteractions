package org.eol.globi.data;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.InvalidLocationException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import static org.eol.globi.data.StudyImporterForTSV.*;

class InteractionListenerNeo4j implements InteractionListener {

    private final NodeFactory nodeFactory;
    private final GeoNamesService geoNamesService;

    private final ImportLogger logger;

    public InteractionListenerNeo4j(NodeFactory nodeFactory, GeoNamesService geoNamesService, ImportLogger logger) {
        this.nodeFactory = nodeFactory;
        this.geoNamesService = geoNamesService;
        this.logger = logger;
    }

    @Override
    public void newLink(Map<String, String> properties) throws StudyImporterException  {
        try {
            if (properties != null) {
                importLink(properties);
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import: " + properties, e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to import: " + properties, e);
        }
    }

    private void importLink(Map<String, String> link) throws NodeFactoryException, IOException {
        String sourceTaxonName = link.get(SOURCE_TAXON_NAME);
        String sourceTaxonId = link.get(SOURCE_TAXON_ID);
        String targetTaxonName = link.get(TARGET_TAXON_NAME);
        String targetTaxonId = link.get(TARGET_TAXON_ID);
        if ((StringUtils.isNotBlank(sourceTaxonName) || StringUtils.isNotBlank(sourceTaxonId))
                && (StringUtils.isNotBlank(targetTaxonName) || StringUtils.isNotBlank(targetTaxonId))) {
            String interactionTypeId = link.get(INTERACTION_TYPE_ID);
            InteractType type = InteractType.typeOf(interactionTypeId);
            String referenceCitation = link.get(REFERENCE_CITATION);
            Study study = nodeFactory.getOrCreateStudy(link.get(REFERENCE_ID), link.get(STUDY_SOURCE_CITATION), link.get(REFERENCE_DOI), referenceCitation);
            if (StringUtils.isBlank(study.getCitation())) {
                study.setCitationWithTx(referenceCitation);
            }
            if (type == null) {
                study.appendLogMessage("unsupported interaction type id [" + interactionTypeId + "]", Level.WARNING);
            } else {
                Specimen source = nodeFactory.createSpecimen(study, sourceTaxonName, sourceTaxonId);
                Specimen target = nodeFactory.createSpecimen(study, targetTaxonName, targetTaxonId);
                source.interactsWith(target, type, getOrCreateLocation(study, link));
            }
        }
    }

    private Location getOrCreateLocation(Study study, Map<String, String> link) throws IOException, NodeFactoryException {
        LatLng centroid = null;
        String latitude = link.get(DECIMAL_LATITUDE);
        String longitude = link.get(DECIMAL_LONGITUDE);
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                centroid = LocationUtil.parseLatLng(latitude, longitude);
            } catch (InvalidLocationException e) {
                getLogger().warn(study, "found invalid location: [" + e.getMessage() + "]");
            }
        }
        if (centroid == null) {
            String localityId = link.get(LOCALITY_ID);
            if (StringUtils.isNotBlank(localityId)) {
                centroid = getGeoNamesService().findLatLng(localityId);
            }
        }
        return centroid == null ? null : nodeFactory.getOrCreateLocation(centroid.getLat(), centroid.getLng(), null);
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }

    public ImportLogger getLogger() {
            return logger;
        }


}
