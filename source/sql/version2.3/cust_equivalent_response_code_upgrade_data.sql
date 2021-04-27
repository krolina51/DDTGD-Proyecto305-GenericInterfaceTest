UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0046'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0060'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0198'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0288'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0341'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1', [process] = '1' WHERE [code_isc] = '0092'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1' WHERE [code_isc] = '1047'
UPDATE [dbo].[cust_equivalent_response_codes] SET [active] = '1' WHERE [code_isc] = '3819'
UPDATE [dbo].[cust_equivalent_response_codes] SET [process] = '1' WHERE [code_isc] = '9260'
UPDATE [dbo].[cust_equivalent_response_codes] SET [process] = '1' WHERE [code_isc] = '1006'
UPDATE [dbo].[cust_equivalent_response_codes] SET [code_iso] = '02', [code_b24] = '02' WHERE [code_isc] ='2731'
INSERT [dbo].[cust_equivalent_response_codes] ([code_isc], [description_isc], [code_iso], [code_b24], [date], [commision_value], [active], [process]) VALUES (N'0000', N'Transaccion Exitosa', N'00', N'00', GETDATE(), CAST(0 AS Numeric(18, 0)), 1, 1)
INSERT [dbo].[cust_equivalent_response_codes] ([code_isc], [description_isc], [code_iso], [code_b24], [date], [commision_value], [active], [process]) VALUES (N'2983', N'TRANS.SUPERA LIMITE PARA CORRESPONSAL BANCARI', N'65', N'65', GETDATE(), CAST(0 AS Numeric(18, 0)), 0, 0)
