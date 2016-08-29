# EOL-Globi-Ontology

GloBI uses existing ontologies to represent inter-organism or inter-taxon
interactions to allow:

 * publication of EOL-Globi on the linked data cloud
 * use of OWL reasoners to validate and perform complex queries on EOL Globi data

### Ontology Reuse

 * Interaction processes from the Gene Ontology (GO)
 * Environments and biomes from the environment ontology (ENVO)
 * Taxa from NCBITaxon and other ontologization of taxonomic resources
 * Taxonomic ranks from the OBO taxrank ontology
 * Relations from the OBO Relations Ontology (RO)
 * Life cycle stages from UBERON
 * Body parts from UBERON
 * Observations and speciments from IAO, OBO and OBOE
 * Behaviors from NBO

### Interactions

Interactions are treated as biological processes. We will reuse parts
of the Gene Ontology biological process hierarchy for interactions
between organisms.

We make biotic interaction a generalization of GO:0051704 !
multi-organism process. This allows us to accommodate semi-biotic
interactions (not covered in GO).

Note that GO biological processes are typically modeled from the
perspective of one organism(?). Is this always the instigator?

### Polarity

Interactions may be polarized or unpolarized. In a polarized
interaction, there is at least one participant who is deemed the
"agent", and one who is deemed the "patient". In an unpolarized
interaction, participants are equal in their roles.

An interaction is always represented from the perspective of one
participant.

(note the agent/patient terminology may change)

### Taxa vs organisms

Instance-level:

 * this-particular-lion instigator-in predator-prey-interaction-001
 * this-particular-impala patient-in predator-prey-interaction-001

Lion SubClassOf capable_of predator-prey-interaction and has_patient some impala


## Queries

See http://lod.globalbioticinteraction.org .
