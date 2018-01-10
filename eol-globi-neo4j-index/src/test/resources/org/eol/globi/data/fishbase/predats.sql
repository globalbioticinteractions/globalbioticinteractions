-- lists predators of a specific species > include reference, location, time.
--
-- questions: 
--		a. Is the date / time of the predator / prey interaction available?
--
SELECT 
p.predatcode AS 'predator code', p.predatorName AS 'predator name', s2.genus AS 'predator genus', s2.species AS 'predator species', 'predator of'
, s.speccode AS 'prey code', s.genus AS 'prey genus', s.species AS 'prey species'
, r.author, r.year, r.title 
, p.locality AS 'locality', cref.iso_code AS 'iso_code', cref.centerlat AS 'latitude', cref.centerlong AS 'longitude'
FROM predats p LEFT JOIN species s ON s.speccode = p.speccode LEFT JOIN species s2 on s2.speccode = p.predatcode LEFT JOIN refrens r ON p.predatsRefNo = r.refno
LEFT JOIN countref cref ON cref.c_code = p.c_code;
 -- WHERE r.author <> 'ICES'
; 
