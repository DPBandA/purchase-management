### Initial Implementation
- Add pr-email-template email template and see what is causing the exception 
  "Error processing PR actions: java.lang.NullPointerException". 
  Calling processPurchaseReqActions() in Thread() seems to be the problem when getUser() is called. 
  Find a workaround.
- Add a default of 2 weeks to the date when the PR is fully approved (2 or 3 approvals) 
  to arrive at the default "Expected date of completion". Allow changing this default 
  period in fin options:
  * Add system option for the number of approvals required (requiredPRApprovals). 
    Show the number of required approvals in the growl message.  
  * Implement adding the "daysAfterPRApprovalForEDOC" to the final approval date to 
    arrive at the EDOC.
  * Add system option for the number of days after to be added to the approval date
    to arrive at the "Expected date of completion". Use daysAfterPRApprovalForEDOC.
- Set the limit in fin options when a PR needs to go to procurement. 
  1.5M is the current limit. Show an alert when the total cost exceeds this limit. 
  Put a note in the status note section stating that the limit was exceeded.
- Send automatic email to persons that can approve the PR based on the given criteria. 
  Note only persons in the originator's division should get this email.
- Add the total amount to the PR emails templates.
- Send automatic PR emails to admin assistant of a department/division.
- Create Currency class and add it to the CostComponent class. Let Currency class
implement Asset class. Add symbol (eg $) and code (eg JMD) fields to the class.
- Create CurrencyConversion for storing currency conversions such as USD to JMD.
- Implement selecting the currency in the costcomponent. Implement "Currency"/ entity class 
  to facilitate conversion between currencies using static methods. 
  Currency class should have the ISO symbol and abbreviation.

### Notes for deployment and email to RA etc.
- The user interface is similar to the JMTS to ease the learning curve.
- Mention know the change from department to branch and team leader to manager. 
- Re-enable authentication for RA, DM and MA.

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
- Add attachment table by running BEL code.
- costcomponent table: CURRENCY_ID
- privilege table: CANACCESSPROCUREMENTUNIT
- purchaserequisition table: DELIVERYDATEREQUIRED, IMPORTLICENCEDATE, IMPORTLICENCENUM,
  SHIPPINGINSTRUCTIONS (VARCHAR: 1024), PLEASESUPPLYNOTE (VARCHAR: 1024)
- Add currency table by running BEL code.
- Add "purchReqUploadFolder" system option. Add "/" to purchReqUploadFolder system option.
- Add new PO fields to ALL databases.
- Add prPriorityCodes (Regular;Urgent)system option.
- Add daysAfterPRApprovalForEDOC system option.
- Add requiredPRApprovals system option.


### Training
- How can the order date be set automatically?

