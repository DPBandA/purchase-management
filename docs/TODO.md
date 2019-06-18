### Initial Implementation
- Add a "Order Detail" tab that allows entry of information for the PO:
  * Add new PO fields to ALL databases.
  * Add PO fields in the PR class to the PR form "Order Details" tab.
- For PO form get the terms and conditions from the printed form and place after the page break in the jasper form.
- Use "Regular" and "Urgent" as priority codes.
- Allow only the PO to cancel a PR once it is saved.
- Add a default of 2 weeks to the date when the PR is fully approved (2 or 3 approvals) to arrive at the default "Expected date of completion". Allow changing this default period in fin options.
- Set the limit in fin options when a PR needs to go to procurement. 1.5M is the current limit. Show an alert when the total cost exceeds this limit. Put a note in the status note section stating that the limit was exceeded.
- Send automatic email to persons that can approve the PR based on the given criteria. Note only persons in the originator's division should get this email.
- Add the total amount to the PR emails templates.
- Send automatic PR emails to admin assistant of a department/division.
- Implement selecting the currency in the costcomponent. Implement "Currency"/ entity class to facilitate conversion between currencies using static methods. Currency class should have the ISO symbol and abbreviation.
- Implement adding links to attachments or uploading to server.
- The "on hand now" field to the PR should link to inventory.
- Link to Accpac and do budget allocation.Canceling a PR/PO should reverse Accpac budget allocation.
- Create report to show Accpac budget allocations.
- Get suppliers from Accpac.
- Only allow one of the set positions to approve otherwise an approval date will not be shown. Add system option that sets the positions that can approve PR.
- Indicate the number of approvals in the email template?
- Add the supplier address to the PR and PO form.
- Do proposal to get cost codes, budgets and suppliers from Accpac.
- Open the purchase requisition tab by default for now but allow user to choose which tab to open by default for financial admin module.
- Add purchase requisition forms to the Form templates tab in Business entities tab and stop using system options to get the files.
- Fix up PR jasper form by removing the overlapping line and put in missing borders.
- Values that are 0 set them n/a in the PR form and change the corresponding parameter types to String.
- Make sure that more than one person with the same position cannot approve PR.
- Take the approvers section out of the page footer of the PR form and put it at the end of the report.
- Make the PO box the same width as the Rate and Cost column heading width combined.
- Right justify the total cost in the PR form.
- Centre "Suggested Supplier:" static text with the text field. It is not centred when exported to PDF on Linux. See if it is the same on Windows.
- Remove the default values from all parameters.
- Implement feature to send PO to supplier.
- Implement editing cost components in the table in the PR dialog?
- Add "CANACCESSPROCUREMENTUNIT" to the Privilege class to control access to PM. 
  This is temp cause the Module along with the Privilege class will be used in 
  future to control module access.
- Let add supplier privilege be true by default?
- Parish/State/Province autocomplete does not seem to work. Fix.

### UI Design
- Look where the help button or menu is out in the PrimeFaces demos.
- Create currency class and add it to the CostComponent class. Let Currency class
implement Asset class

### Database Update
- costcomponent table:
- privilege table: CANACCESSPROCUREMENTUNIT
- purchaserequisition table: DELIVERYDATEREQUIRED, IMPORTLICENCEDATE, IMPORTLICENCENUM
- Add currency table by running BEL code.


