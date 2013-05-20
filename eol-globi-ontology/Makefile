

examples/%.owl: examples/%.ttl
	ln -s $*.ttl $@

# @Deprecated
examples/merged-%.owl: examples/%.owl globi.owl
	owltools globi.owl $< --merge-support-ontologies -o -f ttl file://`pwd`/$@
