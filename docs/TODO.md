# General
- See what other dates that can be created from the PO form.
- Create JobPosition class with, salary, description, minApprovalLimit and
  maxApprovalLimit etc.
  * Impl assigning multiple JobPosition to employee class.
  * Postion names: Team Leader, Divisional Manager, Divisional Director, 
    Finance Manager, Executive Director, Senior Engineer, Analyst, 
    Network Administrator, System Administrator etc
- Design and include the dashboard and main tabs 
- Explore asynchronous for long service tasks by using activiti:async="true"
  in the service task.
- Remove "Module Access" from preference dialog.
- Populate PurchaseOrder with PR if possible. 
- Impl automatic generation of PR #. Initialize with the latest number used.
- Ensure that the requisition date is the that orginator signed the PR.
- Create Supplier class. Supplier/Suggested Supplier are the same in PR.
- The date the PR is completed should be the PO date.
- Impl email messaging to the originator and other PR stakeholders.
- The content of the PO should be generated from the PR. The PO should be a field
  in the PR.
- PO/PR should remain in "draft" stage until all issues are sorted out.
- Get approval limits from Finance.
- The orinator's department should be uneditable by the orginator unless they are
  the dept. head or system admin.

# Optional/Future
- Deploy and configure activiti in gf3 on local PC for testing .
- Design and include the purchasing process.
- Pull cost codes from Accpac.
- The "on hand now" field to the PR should link to inventory.
- Link to Accpac and do budget allocation.Canceling a PR/PO should reverse 
  Accpac budget allocation.
- Create report to show Accpac budget allocations.
- Get suppliers from Accpac.
- Impl supplier evaluation.


