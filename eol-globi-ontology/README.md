# EOL-Globi-Ontology

This describes the OWL model used for EOL-Globi - globi.owl

Status: Draft

## Goals

To produce an OWL model representing inter-organism or inter-taxon
interactions within Globi to allow

 * publication of EOL-Globi on the linked data cloud
 * use of OWL reasoners to validate and perform complex queries on EOL Globi data

## Contents of this project

 * globi.owl - an OWL ontology modeling various kinds of interactions; made by merging:
  * globi-core.owl - interaction types
  * globi-upper.owl - upper level classes
 * globi-plus.owl - core globi plus extensions for OWL reasoning
 * Makefile - generates globi.owl and merged examples
 * examples/ - examples in turtle format of particular animals engaging in interactions
  * examples/all.ttl - combines all examples together

## Model

### Reference

We are taking Malcolm Storey's documentation on interactions as a reference.

### Ontology Reuse

globi.owl will reuse classes and relations from other bio-ontologies,
in particular:

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

### Sequences of interactions

Ethograms

## Queries

http://lod.globalbioticinteractions.org

prefix i: <http://eol.org/globi/individuals>
prefix g: <http://eol.org/globi/>
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 

SELECT *
WHERE
{
 ?ixn 
     rdf:type g:predator-interaction ;
     g:has-receiver ?prey ;
     g:has-agent ?predator .
 ?prey g:has-taxon ?preyTax .
 ?predator g:has-taxon ?predTax .
 ?preyTax ?preyP ?preyV
}

LIMIT 100
