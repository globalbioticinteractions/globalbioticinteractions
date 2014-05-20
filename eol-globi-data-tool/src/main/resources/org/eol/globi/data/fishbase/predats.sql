-- lists predators of a specific species > include reference, location, time.
--
-- questions: 
--		a. Is the date / time of the predator / prey interaction available?
--		b. Is there a lookup table to relate locality to lat / lng shapes?
--
select p.predatcode as 'pred code', s2.genus as 'predator genus', s2.species as 'predator species', 'predator of', s.speccode as 'prey code', s.genus as 'prey genus', s.species as 'prey species', p.locality, r.author, r.year, r.title from predats p JOIN species s ON s.speccode = p.speccode JOIN species s2 on s2.speccode = p.predatcode JOIN refrens r ON p.predatsRefNo = r.refno 
 -- WHERE r.author <> 'ICES'
; 
