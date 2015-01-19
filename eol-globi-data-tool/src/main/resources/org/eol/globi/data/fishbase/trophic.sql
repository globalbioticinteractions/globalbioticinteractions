-- exports trophic level related data from fb database

USE fbapp_mirror;

(SELECT 'genus','species','speciesCode', 'dietTroph', 'dietSeTroph', 'foodTroph', 'foodSeTroph')
UNION
(SELECT s.genus, s.species, e.SpecCode, IFNULL(e.DietTroph,"NA"), IFNULL(e.DietSeTroph,"NA"), IFNULL(e.FoodTroph, "NA"), IFNULL(e.FoodSeTroph,"NA") 
FROM 
  ecology e 
  LEFT JOIN species s ON s.speccode = e.SpecCode
INTO OUTFILE '/tmp/trophic.csv'
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\r\n');
