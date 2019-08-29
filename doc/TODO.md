### Initial Implementation
- Fix attachment feature:
  * It seems the file to be uploaded is being looked for on the server machine and 
    and not from where it is selected. Check out the fileToSave variable. The
    argument to new File() should not be uploadedFilePath
- See paper for changes to be implemented
- Deploy, activate authentication and test entry, editing and form export:
  * Check that default search still works.
- Do invoice.

### Notes for deployment and email to RA etc.
- The user interface is similar to the JMTS to ease the learning curve.
- Mention know the change from department to branch and team leader to manager. 
- Re-enable authentication for GG, RA, DM and MA.
- Further update cannot be done until the JMTS maintenance program is initiated.
- PM to activated and tested in the JMTS.

### Later Issues
- Create CurrencyConversion entity class for storing currency conversions such as USD to JMD:
  * Fields: from, to, rate
  * Multiple from by rate to get to.
  * Look at ratec conversion in GnuCash
  * Implement selecting the currency in the costcomponent.
  * Add system option for the default currency by symbol or name.
- Note that a PR can use multiple currencies. Impl conversion of all currencies to
  the base currencies.

### UI Design
- Address PO/PR forms as follows:
  * Quotation Number label not in line with quotation number. Fix for other labels.
    Note that this may be the case only on Linux.
  * Fix Order Date to show "August 08, 2019" and not "August 8, 2019" for example.
  * Values that are 0 set them n/a in the PR form and change the corresponding parameter types to String.
  * Fix up PR jasper form by removing the overlapping line and put in missing borders.
  * Add purchase requisition forms to the Form templates tab in FinAdmin. 
    tab and stop using system options to get the files.
- Put "Help" and "About" menu items in the "User" menu.
PR/PO form:
- Take the approvers section out of the page footer of the PR form and put it at the end of the report.
- Make the PO box the same width as the Rate and Cost column heading width combined.
- Right justify the total cost in the PR form.
- Centre "Suggested Supplier:" static text with the text field. It is not centred. 
  when exported to PDF on Linux. See if it is the same on Windows.
- Make "Category" in system option read only for all except sysadmin.

### Database Update
- Run new PR(); to create the database tables for the attachment field in the PR class.
- Add attachment, currency tables by running BEL code.
- costcomponent table: CURRENCY_ID (BIGINT, index)
- privilege table: CANACCESSPROCUREMENTUNIT
- purchaserequisition table: DELIVERYDATEREQUIRED, IMPORTLICENCEDATE, IMPORTLICENCENUM,
  SHIPPINGINSTRUCTIONS (VARCHAR: 1024), PLEASESUPPLYNOTE (VARCHAR: 1024),
  CURRENCY_ID (BIGINT, index)
- modules table: PURCHASEMANAGEMENTMODULE
- Add currency table by running BEL code.
- Add "purchReqUploadFolder" system option. Add "/" to purchReqUploadFolder system option.
- Add new PO fields to ALL databases.
- Add prPriorityCodes (Regular;Urgent)system option.
- Add daysAfterPRApprovalForEDOC system option.
- Add requiredPRApprovals system option.
- Add maxAmountForPRProcurement system option.


### Training
- How can the order date be set automatically?

