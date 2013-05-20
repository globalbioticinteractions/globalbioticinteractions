
## Interactions

Interactions are treated as biological processes. We reuse parts of
the Gene Ontology biological process hierarchy for interactions
between organisms.

We make biotic interaction a generalization of GO:0051704 !
multi-organism process. This allows us to accommodate semi-biotic
interactions (not covered in GO).

Note that GO biological processes are typically modeled from the
perspective of one organism. Is this always the instigator?

## Polarity

In OWL it is generally undesirable to have proliferation of
relationship types. However, we can model the polarity as *roles* -
e.g. predator role and prey role.

 predator-prey interaction <
  has_participant some (organism and has_role some predator)
  and
  has_participant some (organism and has_role some prey)

alternately, these could be seen as individual processes

 pp-interaction
  has_part some predation-behavior
  has_part some prey-behavior

predation process SubClassOf instigator process

playing-dead SubClassOf prey-behavior

instigator-in some pp-interaction SubClassOf ...

Two processes coincident in time, each from an individual perspective

## Taxa vs organisms

Instance-level:

 * this-particular-lion instigator-in predator-prey-interaction-001
 * this-particular-impala patient-in predator-prey-interaction-001

Lion SubClassOf capable_of predator-prey-interaction and has_patient some impala

## Sequences of interactions

Ethograms

## Other ontologies

 * NBO
 * GO
 * Uberon
