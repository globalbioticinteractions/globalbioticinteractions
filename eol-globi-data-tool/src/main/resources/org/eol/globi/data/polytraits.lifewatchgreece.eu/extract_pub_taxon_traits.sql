SELECT
	 p.id_publication, p.fullref, t.taxon, m.term_identifier, m.definition, tr.term_identifier, tr.definition, tr.fk_mode, r.traitvalue 
INTO 
	OUTFILE "/tmp/pub_tax_trait.csv"
	FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
	LINES TERMINATED BY "\n"
FROM 
	taxa t, modalities m, traits tr, relations r, relation_pub r_p, publications p  
WHERE
	 r.id_relation = r_p.fk_relation AND r_p.fk_publication = p.id_publication AND t.id_taxon = r.fk_taxon AND m.id_modality = r.fk_modality AND  m.fk_trait = tr.id_trait;
