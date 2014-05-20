-- lists diet items of a species. 
--
-- questions:
--
-- 	a) On missing diet.yearEnd, does that mean that yearStart = yearEnd ?
--  b) Can diet locality be easy translated into lat/lng shape?
--
select d.dietcode as 'prey code', s2.genus 'prey genus', s2.species 'prey species', 'diet of ', s.speccode as 'predator code', s.genus as 'predator genus', s.species as 'predator species', d.yearStart, d.yearEnd, d.locality, r.author, r.year, r.title from diet d JOIN species s ON s.speccode = d.speccode JOIN species s2 on s2.speccode = d.dietcode JOIN refrens r ON d.dietRefNo = r.refno;
