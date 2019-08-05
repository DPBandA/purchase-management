### Initial Implementation
- Add a "Order Detail" tab that allows entry of information for the PO:
  * Add PO fields in the PR class to the PR form "Order Details" tab.
- For PO form get the terms and conditions from the printed form and place after 
  the page break in the jasper form. See the pho
- Use "Regular" and "Urgent" as priority codes.
- Allow only the PO to cancel a PR once it is saved.
- Add a default of 2 weeks to the date when the P
R is fully approved (2 or 3 approvals) to arrive at the default "Expected date of completion". Allow changing this default period in fin options.
- Set the limit in fin options when a PR needs to go to procurement. 1.5M is the current limit. Show an alert when the total cost exceeds this limit. Put a note in the status note section stating that the limit was exceeded.
- Send automatic email to persons that can approve the PR based on the given criteria. Note only persons in the originator's division should get this email.
- Add the total amount to the PR emails templates.
- Send automatic PR emails to admin assistant of a department/division.
- Create Currency class and add it to the CostComponent class. Let Currency class
implement Asset class. Add symbol (eg $) and code (eg JMD) fields to the class.
- Create CurrencyConversion for storing currency conversions such as USD to JMD.
- Implement selecting the currency in the costcomponent. Implement "Currency"/ entity class 
  to facilitate conversion between currencies using static methods. 
  Currency class should have the ISO symbol and abbreviation.
- Only allow one of the set positions to approve otherwise an approval date will not be shown. Add system option that sets the positions that can approve PR.
- Indicate the number of approvals in the email template?
- Add the supplier address to the PR and PO form.
- Open the purchase requisition tab by default for now but allow user to choose which tab to open by default for financial admin module.
- Add purchase requisition forms to the Form templates tab in Business entities tab and stop using system options to get the files.
- Fix up PR jasper form by removing the overlapping line and put in missing borders.
- Values that are 0 set them n/a in the PR form and change the corresponding parameter types to String.
- Make sure that more than one person with the same position cannot approve PR.
- Move PurchaseManager to PM project until development is done then move it to PM lib project.

### UI Design
- Put "Help" and "About" menu items in the "User" menu.
PR/PO form:
- Take the approvers section out of the page footer of the PR form and put it at the end of the report.
- Make the PO box the same width as the Rate and Cost column heading width combined.
- Right justify the total cost in the PR form.
- Centre "Suggested Supplier:" static text with the text field. It is not centred. 
  when exported to PDF on Linux. See if it is the same on Windows.

### Database Update
- Run new PR(); to create the database tables for the attachment field in the PR class.
- Add attachment table by running BEL code.
- costcomponent table: CURRENCY_ID
- privilege table: CANACCESSPROCUREMENTUNIT
- purchaserequisition table: DELIVERYDATEREQUIRED, IMPORTLICENCEDATE, IMPORTLICENCENUM
- Add currency table by running BEL code.
- Add "purchReqUploadFolder" system option.
- Add new PO fields to ALL databases.


### Training
- How can the order date be set automatically?

