
# globi.owl is made by bundling all imports into one owl file
globi.owl: globi-core.owl
	owltools --use-catalog $< --merge-imports-closure --set-ontology-id http://eol.org/ontology/globi.owl -o -f ttl $@

# Protege prefers ".owl" prefix
examples/%.owl: examples/%.ttl
	ln -s $*.ttl $@

# @Deprecated
examples/%-merged.owl: examples/%.owl globi.owl
	owltools --catalog-xml examples/catalog-v001.xml globi.owl $< --merge-support-ontologies --remove-imports-declarations -o -f ttl file://`pwd`/$@

examples/%-mp.owl: examples/%.owl globi-plus.owl
	owltools --catalog-xml examples/catalog-v001.xml globi-plus.owl --merge-imports-closure $< --merge-support-ontologies --merge-imports-closure --remove-imports-declarations -o -f ttl file://`pwd`/$@
