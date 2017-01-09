-- lists food items of a specific species > include reference, location, time.
--
-- questions: 
--		a. Is the date / time of the food item / prey interaction available?
--
select f.PreySpecCode as 'prey species code', f.FoodIII as 'food III', f.foodName as 'food item name', s2.genus as 'food item genus', s2.species as 'food item species', 'food item of', s.speccode as 'consumer species code', s.genus as 'consumer genus', s.species as 'consumer species', 
r.author, r.year, r.title, 
f.locality, cref.ISO_code as 'countryCode', cref.centerlat as 'latitude', cref.centerlong as 'longitude' 
FROM fooditems f LEFT JOIN species s ON s.speccode = f.speccode LEFT JOIN species s2 on s2.speccode = f.preyspeccode LEFT JOIN refrens r ON f.foodsRefNo = r.refno LEFT JOIN countref cref ON cref.c_code = f.c_code; 
 -- WHERE r.author <> 'ICES'
; 
