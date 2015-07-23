package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;

public enum InteractType implements RelType {
    PREYS_UPON("http://purl.obolibrary.org/obo/RO_0002439"),
    PARASITE_OF("http://purl.obolibrary.org/obo/RO_0002444"),
    HAS_HOST("http://purl.obolibrary.org/obo/RO_0002454"),
    INTERACTS_WITH("http://purl.obolibrary.org/obo/RO_0002437"),
    HOST_OF("http://purl.obolibrary.org/obo/RO_0002453"),
    POLLINATES("http://purl.obolibrary.org/obo/RO_0002455"),
    PERCHING_ON(PropertyAndValueDictionary.NO_MATCH),
    ATE("http://purl.obolibrary.org/obo/RO_0002470"),
    SYMBIONT_OF("http://purl.obolibrary.org/obo/RO_0002440"),
    PREYED_UPON_BY("http://purl.obolibrary.org/obo/RO_0002458"),
    POLLINATED_BY("http://purl.obolibrary.org/obo/RO_0002456"),
    EATEN_BY("http://purl.obolibrary.org/obo/RO_0002471"),
    HAS_PARASITE("http://purl.obolibrary.org/obo/RO_0002445"),
    PERCHED_ON_BY(PropertyAndValueDictionary.NO_MATCH),
    HAS_PATHOGEN("http://purl.obolibrary.org/obo/RO_0002557"),
    PATHOGEN_OF("http://purl.obolibrary.org/obo/RO_0002556"),
    HAS_VECTOR("http://purl.obolibrary.org/obo/RO_0002460"),
    VECTOR_OF("http://purl.obolibrary.org/obo/RO_0002459"),
    FLOWERS_VISITED_BY("http://eol.org/schema/terms/FlowersVisitedBy"),
    VISITS_FLOWERS_OF("http://eol.org/schema/terms/VisitsFlowersOf");

    String iri;

    InteractType(String iri) {
        this.iri = iri;
    }

    public static InteractType typeOf(String iri) {
        if (StringUtils.startsWith(iri, "RO:")) {
            iri = StringUtils.replace(iri, "RO:", PropertyAndValueDictionary.RO_NAMESPACE);
        }
        InteractType[] values = values();
        for (InteractType interactType : values) {
            if (StringUtils.equals(iri, interactType.getIRI())) {
                return interactType;
            }
        }
        return null;
    }

    public String getIRI() {
        return iri;
    }
}
