INSERT INTO [dbo].[cust_equivalent_response_codes] ([code_isc], [description_isc], [code_iso],[code_b24], [date], [commision_value],[active],[process]) VALUES ('5270','HORA LIMITE EXCEDIDA','85','02',GETDATE(),0,'1','1')
INSERT INTO [dbo].[cust_equivalent_response_codes] ([code_isc], [description_isc], [code_iso],[code_b24], [date], [commision_value],[active],[process]) VALUES ('3159','TRANSAC. 0306 NO PERMITIDA EN PROD.','57','95',GETDATE(),0,'1','1')
UPDATE [dbo].[cust_equivalent_response_codes] SET [process] = '1' WHERE [code_iso] = '79'
UPDATE [dbo].[cust_equivalent_response_codes] SET [process] = '1' WHERE [code_iso] = '81' AND [code_isc] = '10002'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '9017'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1' WHERE [code_isc] = '9507'
