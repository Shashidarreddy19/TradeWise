-- Build hs_regulatory_coverage_audit for ALL countries
-- Maps each HS code to its BEST regulation match (first found via chapter)
-- Avoids duplicates by using a subquery to pick one regulation per HS code

TRUNCATE TABLE hs_regulatory_coverage_audit;

INSERT INTO hs_regulatory_coverage_audit 
  (country, hs_code, hs2, hs4, hs6, national_code, product_description,
   regulation_found, regulation_id, regulation_title, authority,
   mapping_method, confidence_score, coverage_status)
SELECT 
  h.country,
  h.national_code,
  LEFT(h.national_code, 2),
  LEFT(h.national_code, 4),
  LEFT(h.national_code, 6),
  h.national_code,
  LEFT(h.official_description, 255),
  CASE WHEN best.reg_id IS NOT NULL THEN 1 ELSE 0 END,
  best.reg_id,
  best.reg_title,
  best.reg_authority,
  CASE WHEN best.reg_id IS NOT NULL THEN 'HS2_CHAPTER' ELSE 'NOT_FOUND' END,
  CASE WHEN best.reg_id IS NOT NULL THEN 0.50 ELSE 0.0 END,
  CASE WHEN best.reg_id IS NOT NULL THEN 'HS2_MATCH' ELSE 'NOT_FOUND' END
FROM hs_master h
LEFT JOIN (
  SELECT rm.country, rhm.chapter, 
         MIN(rm.id) as reg_id,
         MIN(rm.title) as reg_title,
         MIN(rm.authority) as reg_authority
  FROM regulation_master rm
  JOIN regulation_hs_mapping rhm ON rhm.regulation_id = rm.id
  WHERE rhm.chapter IS NOT NULL
  GROUP BY rm.country, rhm.chapter
) best ON best.country = h.country AND best.chapter = LEFT(h.national_code, 2)
WHERE h.is_current = 1;

SELECT country, COUNT(*) total,
  SUM(CASE WHEN coverage_status='HS2_MATCH' THEN 1 ELSE 0 END) hs2_match,
  SUM(CASE WHEN coverage_status='NOT_FOUND' THEN 1 ELSE 0 END) not_found,
  ROUND(SUM(CASE WHEN coverage_status='HS2_MATCH' THEN 1 ELSE 0 END)*100.0/COUNT(*),1) coverage_pct
FROM hs_regulatory_coverage_audit 
GROUP BY country ORDER BY country;
