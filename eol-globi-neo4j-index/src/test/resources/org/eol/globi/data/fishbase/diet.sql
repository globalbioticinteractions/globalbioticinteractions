-- lists diets of a specific species > include reference, location, time.
--
-- questions: 
--		a. Is the date / time of the diet / consumer interaction available?
--
SELECT d.dietcode AS 'diet code', s2.genus AS 'diet genus', s2.species AS 'diet species', 'diet of', s.speccode AS 'consumer code', s.genus AS 'consumer genus', s.species AS 'consumer species', 
r.author, r.year, r.title, 
eref.salinity, d.locality, eref.ecosystemname AS 'ecosystemName', eref.ecosystemType AS 'ecosystemType', eref.NorthernLat, eref.NrangeNS, eref.SouthernLat, eref.SrangeNS, eref.WesternLat, eref.WrangeEW, eref.EASternLat, eref.ErangeEW 
FROM diet d LEFT JOIN species s ON s.speccode = d.speccode LEFT JOIN species s2 on s2.speccode = d.dietcode LEFT JOIN refrens r ON d.dietRefNo = r.refno LEFT JOIN ecosystemref eref ON d.e_code = eref.e_code;
 -- WHERE r.author <> 'ICES'
; 
