UPDATE cust_equivalent_response_codes SET code_b24 = '91', code_iso ='91' WHERE code_isc = '9761'
UPDATE cust_equivalent_response_codes SET active = '0' WHERE code_isc NOT IN ('0000','0309','4016','9290','3046','10002','9240','9244','4042','4027','9246','9761','9238','9245','9956','5280','9260','0416','3874','3154','9296','3153','4008','4061','4023','0498','0001','10001','1994','1995','0852','8038','32256','8014','8054','8055','9523') 
UPDATE cust_equivalent_response_codes SET process = '0'
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso IN ('56','30','55')
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso = '75' AND code_isc ='8038'
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso ='54' AND code_isc ='8054'
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso ='54' AND code_isc ='8054'
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso ='12' AND (code_isc ='1994' OR code_isc='1995')
UPDATE cust_equivalent_response_codes SET process = '1' WHERE code_iso ='91' AND code_isc ='9523'