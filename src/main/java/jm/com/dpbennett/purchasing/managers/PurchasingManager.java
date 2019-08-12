/*
Purchase Management
Copyright (C) 2019  D P Bennett & Associates Limited

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Email: info@dpbennett.com.jm
 */
package jm.com.dpbennett.purchasing.managers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.Address;
import jm.com.dpbennett.business.entity.Attachment;
import jm.com.dpbennett.business.entity.BusinessEntity;
import jm.com.dpbennett.business.entity.Contact;
import jm.com.dpbennett.business.entity.CostComponent;
import jm.com.dpbennett.business.entity.DatePeriod;
import jm.com.dpbennett.business.entity.Department;
import jm.com.dpbennett.business.entity.Email;
import jm.com.dpbennett.business.entity.Employee;
import jm.com.dpbennett.business.entity.EmployeePosition;
import jm.com.dpbennett.business.entity.Internet;
import jm.com.dpbennett.business.entity.JobManagerUser;
import jm.com.dpbennett.business.entity.PurchaseRequisition;
import jm.com.dpbennett.business.entity.Supplier;
import jm.com.dpbennett.business.entity.SystemOption;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.StreamedContent;
import jm.com.dpbennett.business.entity.utils.BusinessEntityUtils;
import jm.com.dpbennett.business.entity.utils.ReturnMessage;
import jm.com.dpbennett.wal.Authentication;
import jm.com.dpbennett.wal.managers.FileUploadManager;
import jm.com.dpbennett.wal.managers.SystemManager;
import jm.com.dpbennett.wal.managers.SystemManager.LoginActionListener;
import jm.com.dpbennett.wal.managers.SystemManager.SearchActionListener;
import jm.com.dpbennett.wal.utils.BeanUtils;
import jm.com.dpbennett.wal.utils.FinancialUtils;
import jm.com.dpbennett.wal.utils.MainTabView;
import jm.com.dpbennett.wal.utils.PrimeFacesUtils;
import jm.com.dpbennett.wal.utils.Utils;
import jm.com.dpbennett.wal.validator.AddressValidator;
import jm.com.dpbennett.wal.validator.ContactValidator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Desmond Bennett
 */
public class PurchasingManager implements Serializable,
        SearchActionListener, LoginActionListener {

    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    @PersistenceUnit(unitName = "AccPacPU")
    private EntityManagerFactory EMF2;
    private CostComponent selectedCostComponent;
    private PurchaseRequisition selectedPurchaseRequisition;
    private Employee selectedApprover;
    private Boolean edit;
    private String searchText;
    private String purchaseReqSearchText;
    private List<PurchaseRequisition> foundPurchaseReqs;
    private String searchType;
    private DatePeriod dateSearchPeriod;
    private Long searchDepartmentId;
    private List<Employee> toEmployees;
    private String purchaseReqEmailSubject;
    private String purchaseReqEmailContent;
    private Supplier selectedSupplier;
    private Contact selectedSupplierContact;
    private Address selectedSupplierAddress;
    private String supplierSearchText;
    private Boolean isActiveSuppliersOnly;
    private List<Supplier> foundSuppliers;
    private Attachment selectedAttachment;
    private UploadedFile uploadedFile;

    /**
     * Creates a new instance of PurchasingManager
     */
    public PurchasingManager() {
        init();
    }
    
    public void onAttachmentCellEdit(CellEditEvent event) {
        getSelectedPurchaseRequisition().getAllSortedAttachments().
                get(event.getRowIndex()).setIsDirty(true);
        updatePurchaseReq(null);        
    }

    public StreamedContent getFileAttachment(Attachment attachment) {
        return new DefaultStreamedContent(attachment.getFileInputStream(), 
                attachment.getContentType(),
                attachment.getSourceURL());
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            OutputStream outputStream;

            // Save file
            String uploadedFilePath = SystemOption.getOptionValueObject(getEntityManager1(),
                    "purchReqUploadFolder")
                    + event.getFile().getFileName();
            File fileToSave
                    = new File(uploadedFilePath);
            outputStream = new FileOutputStream(fileToSave);
            outputStream.write(event.getFile().getContents());
            outputStream.close();

            // Create attachment and save PR.            
            getSelectedPurchaseRequisition().getAttachments().
                    add(new Attachment(event.getFile().getFileName(),
                            event.getFile().getFileName(),
                            uploadedFilePath,
                            event.getFile().getContentType()));

            updatePurchaseReq(null);
            
            PrimeFacesUtils.addMessage("Succesful", event.getFile().getFileName() + " was uploaded.", FacesMessage.SEVERITY_INFO);

            if (getSelectedPurchaseRequisition().getId() != null) {
                saveSelectedPurchaseRequisition();
            }            

        } catch (IOException ex) {
            Logger.getLogger(FileUploadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the title of the application which may be saved in a database.
     *
     * @return
     */
    public String getTitle() {
        return "Purchase Management";
    }

    /**
     * Gets the general search text.
     *
     * @return
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Sets the general search text.
     *
     * @param searchText
     */
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    /**
     * Gets the supplier's search text.
     *
     * @return
     */
    public String getSupplierSearchText() {
        return supplierSearchText;
    }

    /**
     * Sets the supplier's search text.
     *
     * @param supplierSearchText
     */
    public void setSupplierSearchText(String supplierSearchText) {
        this.supplierSearchText = supplierSearchText;
    }

    /**
     * Gets the selected supplier.
     *
     * @return
     */
    public Supplier getSelectedSupplier() {
        if (selectedSupplier == null) {
            return new Supplier("");
        }
        return selectedSupplier;
    }

    /**
     * Sets the selected supplier.
     *
     * @param selectedSupplier
     */
    public void setSelectedSupplier(Supplier selectedSupplier) {
        this.selectedSupplier = selectedSupplier;
    }

    public Address getSupplierCurrentAddress() {
        return getSelectedSupplier().getDefaultAddress();
    }

    public Contact getSupplierCurrentContact() {
        return getSelectedSupplier().getDefaultContact();
    }

    public void editSupplierCurrentAddress() {
        selectedSupplierAddress = getSupplierCurrentAddress();
        setEdit(true);
    }

    public void createNewSupplier() {
        selectedSupplier = new Supplier("", true);
    }

    public void addNewSupplier() {
        selectedSupplier = new Supplier("", true);

        openSuppliersTab();

        editSelectedSupplier();
    }

    public Boolean getIsNewSupplier() {
        return getSelectedSupplier().getId() == null;
    }

    public void cancelSupplierEdit(ActionEvent actionEvent) {

        getSelectedSupplier().setIsDirty(false);

        PrimeFaces.current().dialog().closeDynamic(null);
    }

    public void okSupplier() {
        Boolean hasValidAddress = false;
        Boolean hasValidContact = false;

        try {

            // Validate 
            // Check for a valid address
            for (Address address : selectedSupplier.getAddresses()) {
                hasValidAddress = hasValidAddress || AddressValidator.validate(address);
            }
            if (!hasValidAddress) {
                PrimeFacesUtils.addMessage("Address Required",
                        "A valid address was not entered for this supplier",
                        FacesMessage.SEVERITY_ERROR);

                return;
            }

            // Check for a valid contact
            for (Contact contact : selectedSupplier.getContacts()) {
                hasValidContact = hasValidContact || ContactValidator.validate(contact);
            }
            if (!hasValidContact) {
                PrimeFacesUtils.addMessage("Contact Required",
                        "A valid contact was not entered for this supplier",
                        FacesMessage.SEVERITY_ERROR);

                return;
            }

            // Update tracking
            if (getIsNewSupplier()) {
                getSelectedSupplier().setDateEntered(new Date());
                getSelectedSupplier().setDateEdited(new Date());
                if (getUser() != null) {
                    selectedSupplier.setEnteredBy(getUser().getEmployee());
                    selectedSupplier.setEditedBy(getUser().getEmployee());
                }
            }

            // Do save
            if (getSelectedSupplier().getIsDirty()) {
                getSelectedSupplier().setDateEdited(new Date());
                if (getUser() != null) {
                    selectedSupplier.setEditedBy(getUser().getEmployee());
                }
                selectedSupplier.save(getEntityManager1());
                getSelectedSupplier().setIsDirty(false);
            }

            PrimeFaces.current().dialog().closeDynamic(null);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String getRenderDateSearchFields() {
        switch (searchType) {
            case "Suppliers":
                return "false";
            default:
                return "true";
        }
    }

    public ArrayList getDateSearchFields() {
        ArrayList dateSearchFields = new ArrayList();

        switch (searchType) {
            case "Suppliers":
                dateSearchFields.add(new SelectItem("dateEntered", "Date entered"));
                dateSearchFields.add(new SelectItem("dateEdited", "Date edited"));
                break;
            case "Purchase requisitions":
                dateSearchFields.add(new SelectItem("requisitionDate", "Requisition date"));
                dateSearchFields.add(new SelectItem("dateOfCompletion", "Date completed"));
                dateSearchFields.add(new SelectItem("dateEdited", "Date edited"));
                dateSearchFields.add(new SelectItem("expectedDateOfCompletion", "Exp'ted date of completion"));
                dateSearchFields.add(new SelectItem("dateRequired", "Date required"));
                dateSearchFields.add(new SelectItem("purchaseOrderDate", "Purchase order date"));
                dateSearchFields.add(new SelectItem("teamLeaderApprovalDate", "Team Leader approval date"));
                dateSearchFields.add(new SelectItem("divisionalManagerApprovalDate", "Divisional Manager approval date"));
                dateSearchFields.add(new SelectItem("divisionalDirectorApprovalDate", "Divisional Director approval date"));
                dateSearchFields.add(new SelectItem("financeManagerApprovalDate", "Finance Manager approval date"));
                dateSearchFields.add(new SelectItem("executiveDirectorApprovalDate", "Executive Director approval date"));
                break;
            default:
                break;
        }

        return dateSearchFields;
    }

    public ArrayList getSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("Purchase requisitions", "Purchase requisitions"));
        searchTypes.add(new SelectItem("Suppliers", "Suppliers"));

        return searchTypes;
    }

    public void updateSupplier() {
        getSelectedSupplier().setIsDirty(true);
    }

    public void removeSupplierContact() {
        getSelectedSupplier().getContacts().remove(selectedSupplierContact);
        getSelectedSupplier().setIsDirty(true);
        selectedSupplierContact = null;
    }

    public Boolean getIsNewSupplierAddress() {
        return getSelectedSupplierAddress().getId() == null && !getEdit();
    }

    public void okSupplierAddress() {

        selectedSupplierAddress = selectedSupplierAddress.prepare();

        if (getIsNewSupplierAddress()) {
            getSelectedSupplier().getAddresses().add(selectedSupplierAddress);
        }

        PrimeFaces.current().executeScript("PF('addressFormDialog').hide();");

    }

    public void updateSupplierAddress() {
        getSelectedSupplierAddress().setIsDirty(true);
        getSelectedSupplier().setIsDirty(true);
    }

    public List<Address> getSupplierAddressesModel() {
        return getSelectedSupplier().getAddresses();
    }

    public List<Contact> getSupplierContactsModel() {
        return getSelectedSupplier().getContacts();
    }

    public void createNewSupplierAddress() {
        selectedSupplierAddress = null;

        // Find an existing invalid or blank address and use it as the neww address
        for (Address address : getSelectedSupplier().getAddresses()) {
            if (address.getAddressLine1().trim().isEmpty()) {
                selectedSupplierAddress = address;
                break;
            }
        }

        // No existing blank or invalid address found so creating new one.
        if (selectedSupplierAddress == null) {
            selectedSupplierAddress = new Address("", "Billing");
        }

        setEdit(false);

        getSelectedSupplier().setIsDirty(false);
    }

    public void okContact() {

        selectedSupplierContact = selectedSupplierContact.prepare();

        if (getIsNewSupplierContact()) {
            getSelectedSupplier().getContacts().add(selectedSupplierContact);
        }

        PrimeFaces.current().executeScript("PF('contactFormDialog').hide();");

    }

    public void updateSupplierContact() {
        getSelectedSupplierContact().setIsDirty(true);
        getSelectedSupplier().setIsDirty(true);
    }

    public void createNewSupplierContact() {
        selectedSupplierContact = null;

        for (Contact contact : getSelectedSupplier().getContacts()) {
            if (contact.getFirstName().trim().isEmpty()) {
                selectedSupplierContact = contact;
                break;
            }
        }

        if (selectedSupplierContact == null) {
            selectedSupplierContact = new Contact("", "", "Main");
            selectedSupplierContact.setInternet(new Internet());
        }

        setEdit(false);

        getSelectedSupplier().setIsDirty(false);
    }

    public void updateSupplierName(AjaxBehaviorEvent event) {
        selectedSupplier.setName(selectedSupplier.getName().trim());

        getSelectedSupplier().setIsDirty(true);
    }

    public void onSupplierCellEdit(CellEditEvent event) {
        BusinessEntityUtils.saveBusinessEntityInTransaction(getEntityManager1(),
                getFoundSuppliers().get(event.getRowIndex()));
    }

    public int getNumOfSuppliersFound() {
        return getFoundSuppliers().size();
    }

    public void editSelectedSupplier() {

        getSelectedSupplier().setIsNameAndIdEditable(getUser().getPrivilege().getCanAddSupplier());

        PrimeFacesUtils.openDialog(null, "supplierDialog", true, true, true, 450, 700);
    }

    public Boolean getIsActiveSuppliersOnly() {
        if (isActiveSuppliersOnly == null) {
            isActiveSuppliersOnly = true;
        }
        return isActiveSuppliersOnly;
    }

    public List<Supplier> getFoundSuppliers() {
        return foundSuppliers;
    }

    public void setFoundSuppliers(List<Supplier> foundSuppliers) {
        this.foundSuppliers = foundSuppliers;
    }

    public void setIsActiveSuppliersOnly(Boolean isActiveSuppliersOnly) {
        this.isActiveSuppliersOnly = isActiveSuppliersOnly;
    }

    public void doSupplierSearch() {
        doSupplierSearch(supplierSearchText);
    }

    public void doSupplierSearch(String supplierSearchText) {
        this.supplierSearchText = supplierSearchText;

        if (getIsActiveSuppliersOnly()) {
            foundSuppliers = Supplier.findActiveSuppliersByFirstPartOfName(getEntityManager1(), supplierSearchText);
        } else {
            foundSuppliers = Supplier.findSuppliersByFirstPartOfName(getEntityManager1(), supplierSearchText);
        }

    }

    public void doSearch() {

        switch (searchType) {
            case "Purchase requisitions":
                doPurchaseReqSearch(dateSearchPeriod, searchType, searchText, null);
                openPurchaseReqsTab();
                break;
            case "Suppliers":
                doSupplierSearch(searchText);
                openSuppliersTab();
                break;
            default:
                break;
        }

    }

    public Boolean getIsNewSupplierContact() {
        return getSelectedSupplierContact().getId() == null && !getEdit();
    }

    public Contact getSelectedSupplierContact() {
        return selectedSupplierContact;
    }

    public void setSelectedSupplierContact(Contact selectedSupplierContact) {
        this.selectedSupplierContact = selectedSupplierContact;

        setEdit(true);
    }

    public Address getSelectedSupplierAddress() {
        return selectedSupplierAddress;
    }

    public void setSelectedSupplierAddress(Address selectedSupplierAddress) {
        this.selectedSupplierAddress = selectedSupplierAddress;

        setEdit(true);
    }

    public void removeSupplierAddress() {
        getSelectedSupplier().getAddresses().remove(selectedSupplierAddress);
        getSelectedSupplier().setIsDirty(true);
        selectedSupplierAddress = null;
    }

    public List<Supplier> completeActiveSupplier(String query) {
        try {
            return Supplier.findActiveSuppliersByAnyPartOfName(getEntityManager1(), query);

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public void openSuppliersTab() {
        getSystemManager().getMainTabView().openTab("Suppliers");
    }

    public Boolean getCanExportPurchaseReqForm() {
        return getIsSelectedPurchaseReqIsValid();
    }

    public Boolean getCanExportPurchaseOrderForm() {
        return getIsProcurementOfficer() && getIsSelectedPurchaseReqIsValid();
    }

    public Boolean getIsProcurementOfficer() {
        return getUser().getEmployee().isProcurementOfficer();
    }

    public String getPurchaseReqEmailContent() {
        return purchaseReqEmailContent;
    }

    public void setPurchaseReqEmailContent(String purchaseReqEmailContent) {
        this.purchaseReqEmailContent = purchaseReqEmailContent;
    }

    public String getPurchaseReqEmailSubject() {
        return purchaseReqEmailSubject;
    }

    public void setPurchaseReqEmailSubject(String purchaseReqEmailSubject) {
        this.purchaseReqEmailSubject = purchaseReqEmailSubject;
    }

    public List<Employee> getToEmployees() {
        if (toEmployees == null) {
            toEmployees = new ArrayList<>();
        }
        return toEmployees;
    }

    public void setToEmployees(List<Employee> toEmployees) {
        this.toEmployees = toEmployees;
    }

    public void updateDateSearchField() {
        //doSearch();
    }

    public DatePeriod getDateSearchPeriod() {
        return dateSearchPeriod;
    }

    public void setDateSearchPeriod(DatePeriod dateSearchPeriod) {
        this.dateSearchPeriod = dateSearchPeriod;
    }

    public String getPRApprovalDate(List<EmployeePosition> positions) {
        for (EmployeePosition position : positions) {
            switch (position.getTitle()) {
                case "Team Leader":
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getTeamLeaderApprovalDate());
                case "Divisional Manager":
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getDivisionalManagerApprovalDate());
                case "Divisional Director":
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getDivisionalDirectorApprovalDate());
                case "Finance Manager":
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getFinanceManagerApprovalDate());
                case "Executive Director":
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getExecutiveDirectorApprovalDate());
                default:
                    return BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getApprovalDate());
            }
        }

        return "";
    }

    public Employee getSelectedApprover() {
        return selectedApprover;
    }

    public void setSelectedApprover(Employee selectedApprover) {
        this.selectedApprover = selectedApprover;
    }

    public Boolean checkPRWorkProgressReadinessToBeChanged() {
        EntityManager em = getEntityManager1();

        // Find the currently stored PR and check it's work status
        if (getSelectedPurchaseRequisition().getId() != null) {

            PurchaseRequisition savedPurchaseRequisition
                    = PurchaseRequisition.findById(em, getSelectedPurchaseRequisition().getId());

            if (savedPurchaseRequisition != null) {
                if (!getUser().getEmployee().isProcurementOfficer()
                        && !getSelectedPurchaseRequisition().getWorkProgress().equals("Completed")
                        && savedPurchaseRequisition.getWorkProgress().equals("Completed")) {
                    PrimeFacesUtils.addMessage("Procurement Officer Required",
                            "You are not a procurement officer so you cannot change the completion status of this purchase requisition.",
                            FacesMessage.SEVERITY_WARN);

                    return false;
                }
            }

            // Procurement officer is required to approve PRs
            if (!getUser().getEmployee().isProcurementOfficer()
                    && getSelectedPurchaseRequisition().getWorkProgress().equals("Completed")) {

                PrimeFacesUtils.addMessage("Procurement Officer Required",
                        "You are not a procurement officer so you cannot mark this purchase requisition as completed.",
                        FacesMessage.SEVERITY_WARN);

                return false;
            }

            // Do not allow flagging PR as completed unless it is approved             
            if (!getSelectedPurchaseRequisition().isApproved(2) // tk required num. to be made system option
                    && getSelectedPurchaseRequisition().getWorkProgress().equals("Completed")) {

                PrimeFacesUtils.addMessage("Purchase Requisition Not Approved",
                        "This purchase requisition is NOT approved so it cannot be marked as completed.",
                        FacesMessage.SEVERITY_WARN);

                return false;
            }

        } else {

            PrimeFacesUtils.addMessage("Purchase Requisition Work Progress Cannot be Changed",
                    "This purchase requisition's work progress cannot be changed until it is saved.",
                    FacesMessage.SEVERITY_WARN);
            return false;
        }

        return true;
    }

    public void updateWorkProgress() {

        if (checkPRWorkProgressReadinessToBeChanged()) {
            if (!getSelectedPurchaseRequisition().getWorkProgress().equals("Completed")) {

                selectedPurchaseRequisition.setPurchasingDepartment(
                        Department.findDefaultDepartment(getEntityManager1(),
                                "--"));
                selectedPurchaseRequisition.setProcurementOfficer(
                        Employee.findDefaultEmployee(getEntityManager1(),
                                "--", "--", false));
                getSelectedPurchaseRequisition().setDateOfCompletion(null);

                getSelectedPurchaseRequisition().setPurchaseOrderDate(null);

            } else if (getSelectedPurchaseRequisition().getWorkProgress().equals("Completed")) {

                getSelectedPurchaseRequisition().setDateOfCompletion(new Date());

                getSelectedPurchaseRequisition().setPurchaseOrderDate(new Date());

                // Set the procurement officer and their department
                getSelectedPurchaseRequisition().
                        setProcurementOfficer(getUser().getEmployee());

                getSelectedPurchaseRequisition().
                        setPurchasingDepartment(getUser().getEmployee().getDepartment());

                updatePurchaseReq(null);

                getSelectedPurchaseRequisition().addAction(BusinessEntity.Action.COMPLETE);
            }

        } else {
            if (getSelectedPurchaseRequisition().getId() != null) {
                // Reset work progress to the currently saved state
                PurchaseRequisition foundPR = PurchaseRequisition.findById(getEntityManager1(),
                        getSelectedPurchaseRequisition().getId());
                if (foundPR != null) {
                    getSelectedPurchaseRequisition().setWorkProgress(foundPR.getWorkProgress());
                } else {
                    getSelectedPurchaseRequisition().setWorkProgress("Ongoing");
                }
            } else {
                getSelectedPurchaseRequisition().setWorkProgress("Ongoing");
            }
        }

    }

    public void deleteCostComponent() {
        deleteCostComponentByName(selectedCostComponent.getName());
    }

    public void deleteAttachment() {
        deleteAttachmentByName(selectedAttachment.getName());
    }

    public void deleteSelectedPRApprover() {
        deleteApproverByName(selectedApprover.getName());
    }

    public void deleteApproverByName(String approverName) {

        List<Employee> employees = getSelectedPurchaseRequisition().getApprovers();
        int index = 0;
        for (Employee employee : employees) {
            if (employee.getName().equals(approverName)) {
                employees.remove(index);
                removePRApprovalDate(employee.getPositions());
                getSelectedPurchaseRequisition().setIsDirty(true);
                getSelectedPurchaseRequisition().addAction(BusinessEntity.Action.EDIT);

                break;
            }
            ++index;
        }
    }

    public void okCostingComponent() {
        if (selectedCostComponent.getId() == null && !getEdit()) {
            getSelectedPurchaseRequisition().getCostComponents().add(selectedCostComponent);
        }
        setEdit(false);

        if (getSelectedCostComponent().getIsDirty()) {
            updatePurchaseReq(null);
        }

        PrimeFaces.current().executeScript("PF('purchreqCostingCompDialog').hide();");
    }

    public void openPurchaseReqsTab() {
        getMainTabView().openTab("Purchase Requisitions");
    }

    public void editPurchReqGeneralEmail() {
        PrimeFacesUtils.openDialog(null, "purchaseReqEmailDialog", true, true, true, false, 500, 625);
    }

    public void openRequestApprovalEmailDialog() {
        EntityManager em = getEntityManager1();
        Email email = Email.findActiveEmailByName(em, "pr-gen-email-template");

        String prNum = getSelectedPurchaseRequisition().generateNumber();
        String JMTSURL = (String) SystemOption.getOptionValueObject(em, "appURL");
        String originator = getSelectedPurchaseRequisition().getOriginator().getFirstName()
                + " " + getSelectedPurchaseRequisition().getOriginator().getLastName();
        String department = getSelectedPurchaseRequisition().getOriginatingDepartment().getName();
        String requisitionDate = BusinessEntityUtils.
                getDateInMediumDateFormat(getSelectedPurchaseRequisition().getRequisitionDate());
        String description = getSelectedPurchaseRequisition().getDescription();
        String sender = getUser().getEmployee().getFirstName() + " "
                + getUser().getEmployee().getLastName();

        getToEmployees().clear();
        setPurchaseReqEmailSubject(
                email.getSubject().replace("{purchaseRequisitionNumber}", prNum));
        setPurchaseReqEmailContent(
                email.getContent("/correspondences/").
                        replace("{JMTSURL}", JMTSURL).
                        replace("{purchaseRequisitionNumber}", prNum).
                        replace("{originator}", originator).
                        replace("{department}", department).
                        replace("{requisitionDate}", requisitionDate).
                        replace("{action}", "approve").
                        replace("{description}", description).
                        replace("{sender}", sender));

        editPurchReqGeneralEmail();
    }

    public void openSendEmailDialog() {
        getToEmployees().clear();
        setPurchaseReqEmailSubject("");
        setPurchaseReqEmailContent("");

        editPurchReqGeneralEmail();
    }

    public void sendGeneralPurchaseReqEmail() {

        new Thread() {
            @Override
            public void run() {
                try {
                    EntityManager em = getEntityManager1();
                    Email email = Email.findActiveEmailByName(em, "pr-gen-email-template");

                    for (Employee toEmployee : getToEmployees()) {
                        JobManagerUser toEmployeeUser
                                = JobManagerUser.findActiveJobManagerUserByEmployeeId(
                                        em, toEmployee.getId());

                        Utils.postMail(null,
                                getUser().getEmployee(),
                                toEmployeeUser,
                                getPurchaseReqEmailSubject(),
                                getPurchaseReqEmailContent().
                                        replace("{title}",
                                                getUser().getEmployee().getTitle()).
                                        replace("{surname}",
                                                getUser().getEmployee().getLastName()).
                                        replace("{role}", toEmployee.getPositions().get(0).getTitle()),
                                email.getContentType(),
                                em);
                    }
                } catch (Exception e) {
                    System.out.println("Error sending PR email(s): " + e);
                }
            }

        }.start();

        closeDialog();

    }

    public List getCostTypeList() {
        return FinancialUtils.getCostTypeList();
    }

    public Boolean getIsSupplierNameValid() {
        return BusinessEntityUtils.validateName(selectedPurchaseRequisition.getSupplier().getName());
    }

    public StreamedContent getPurchaseReqFile() {
        StreamedContent streamedContent = null;

        try {

            // tk impl get PR form
            streamedContent = getPurchaseReqStreamContent(getEntityManager1());

        } catch (Exception e) {
            System.out.println(e);
        }

        return streamedContent;
    }

    public StreamedContent getPurchaseReqStreamContent(EntityManager em) {

        HashMap parameters = new HashMap();

        try {
            parameters.put("prId", getSelectedPurchaseRequisition().getId());

            parameters.put("purchReqNo", getSelectedPurchaseRequisition().getNumber());
            parameters.put("purchaseOrderNo", getSelectedPurchaseRequisition().getPurchaseOrderNumber());
            parameters.put("addressLine1", getSelectedPurchaseRequisition()
                    .getSupplier().getDefaultAddress().getAddressLine1());
            parameters.put("addressLine2", getSelectedPurchaseRequisition()
                    .getSupplier().getDefaultAddress().getAddressLine2());
            parameters.put("purposeOfOrder", getSelectedPurchaseRequisition().getDescription());
            parameters.put("suggestedSupplier", getSelectedPurchaseRequisition()
                    .getSupplier().getName());
            parameters.put("originator", getSelectedPurchaseRequisition()
                    .getOriginator().getFirstName() + " "
                    + getSelectedPurchaseRequisition()
                            .getOriginator().getLastName());
            parameters.put("priorityCode", getSelectedPurchaseRequisition().getPriorityCode());
            parameters.put("requisitionDate",
                    BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getRequisitionDate()));
            parameters.put("originatorSignature", getSelectedPurchaseRequisition()
                    .getOriginator().getFirstName() + " "
                    + getSelectedPurchaseRequisition()
                            .getOriginator().getLastName());
            // Set approvals
            Employee approver = getSelectedPurchaseRequisition().
                    getFirstApproverByPositionTitle("Team Leader");
            if (approver != null) {
                parameters.put("teamLeaderApproval",
                        approver.getFirstName() + " " + approver.getLastName());
                parameters.put("teamLeaderApprovalDate",
                        BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                                getTeamLeaderApprovalDate()));

            }
            approver = getSelectedPurchaseRequisition().
                    getFirstApproverByPositionTitle("Divisional Manager");
            if (approver != null) {
                parameters.put("divisionalManagerApproval",
                        approver.getFirstName() + " " + approver.getLastName());
                parameters.put("divisionalManagerApprovalDate",
                        BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                                getDivisionalManagerApprovalDate()));

            }
            approver = getSelectedPurchaseRequisition().
                    getFirstApproverByPositionTitle("Divisional Director");
            if (approver != null) {
                parameters.put("divisionalDirectorApproval",
                        approver.getFirstName() + " " + approver.getLastName());
                parameters.put("divisionalDirectorApprovalDate",
                        BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                                getDivisionalDirectorApprovalDate()));

            }
            approver = getSelectedPurchaseRequisition().
                    getFirstApproverByPositionTitle("Finance Manager");
            if (approver != null) {
                parameters.put("financeManagerApproval",
                        approver.getFirstName() + " " + approver.getLastName());
                parameters.put("financeManagerApprovalDate",
                        BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                                getFinanceManagerApprovalDate()));

            }
            approver = getSelectedPurchaseRequisition().
                    getFirstApproverByPositionTitle("Executive Director");
            if (approver != null) {
                parameters.put("executiveDirectorApproval",
                        approver.getFirstName() + " " + approver.getLastName());
                parameters.put("executiveDirectorApprovalDate",
                        BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                                getExecutiveDirectorApprovalDate()));

            }
            parameters.put("procurementOfficer", getSelectedPurchaseRequisition()
                    .getProcurementOfficer().getFirstName() + " "
                    + getSelectedPurchaseRequisition()
                            .getProcurementOfficer().getLastName());
            parameters.put("totalCost", getSelectedPurchaseRequisition().getTotalCost());

            Connection con = BusinessEntityUtils.establishConnection(
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseDriver"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseURL"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseUsername"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabasePassword"));

            if (con != null) {
                try {
                    StreamedContent streamedContent;

                    JasperReport jasperReport = JasperCompileManager
                            .compileReport((String) SystemOption.getOptionValueObject(em, "purchaseRequisition"));

                    JasperPrint print = JasperFillManager.fillReport(
                            jasperReport,
                            parameters,
                            con);

                    byte[] fileBytes = JasperExportManager.exportReportToPdf(print);

                    streamedContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes),
                            "application/pdf", "Purchase Requisition - " + getSelectedPurchaseRequisition().getNumber() + ".pdf");

                    return streamedContent;

                } catch (JRException e) {
                    System.out.println("Error compiling purchase requisition: " + e);
                }
            }

            return null;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

    }

    public StreamedContent getPurchaseOrderFile() {
        StreamedContent streamContent = null;

        try {

            streamContent = getPurchaseOrderStreamContent(getEntityManager1());
            setLongProcessProgress(100);

        } catch (Exception e) {
            System.out.println(e);
            setLongProcessProgress(0);
        }

        return streamContent;
    }

    public StreamedContent getPurchaseOrderStreamContent(EntityManager em) {

        HashMap parameters = new HashMap();

        try {
            parameters.put("prId", getSelectedPurchaseRequisition().getId());

            parameters.put("purchReqNo", getSelectedPurchaseRequisition().getNumber());
            parameters.put("purchaseOrderNo", getSelectedPurchaseRequisition().getPurchaseOrderNumber());
            parameters.put("addressLine1", getSelectedPurchaseRequisition()
                    .getSupplier().getDefaultAddress().getAddressLine1());
            parameters.put("addressLine2", getSelectedPurchaseRequisition()
                    .getSupplier().getDefaultAddress().getAddressLine2());
            parameters.put("suggestedSupplier", getSelectedPurchaseRequisition()
                    .getSupplier().getName());

            parameters.put("shippingInstructions", "");
            parameters.put("terms", "CREDIT");
            parameters.put("originatingDeptCode",
                    getSelectedPurchaseRequisition().getOriginatingDepartment().getCode());
            parameters.put("importLicenceNo", "");
            parameters.put("deliveryDateRequired", "");
            parameters.put("pleaseSupply", "");
            parameters.put("importLicenseDate", "");
            parameters.put("quotation Number", "");

            parameters.put("requisitionDate",
                    BusinessEntityUtils.getDateInMediumDateFormat(getSelectedPurchaseRequisition().
                            getRequisitionDate()));

            parameters.put("procurementOfficer", getSelectedPurchaseRequisition()
                    .getProcurementOfficer().getFirstName() + " "
                    + getSelectedPurchaseRequisition()
                            .getProcurementOfficer().getLastName());
            parameters.put("totalCost", getSelectedPurchaseRequisition().getTotalCost());

            Connection con = BusinessEntityUtils.establishConnection(
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseDriver"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseURL"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseUsername"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabasePassword"));

            if (con != null) {
                try {
                    StreamedContent streamedContent;

                    JasperReport jasperReport = JasperCompileManager
                            .compileReport((String) SystemOption.getOptionValueObject(em, "purchaseOrder"));

                    JasperPrint print = JasperFillManager.fillReport(
                            jasperReport,
                            parameters,
                            con);

                    byte[] fileBytes = JasperExportManager.exportReportToPdf(print);

                    streamedContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes),
                            "application/pdf", "Purchase Order - " + getSelectedPurchaseRequisition().getPurchaseOrderNumber() + ".pdf");

                    setLongProcessProgress(100);

                    return streamedContent;

                } catch (JRException e) {
                    System.out.println("Error compiling purchase order: " + e);
                }
            }

            return null;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

    }

    public void updatePurchaseReq(AjaxBehaviorEvent event) {
        getSelectedPurchaseRequisition().setIsDirty(true);
        getSelectedPurchaseRequisition().setEditStatus("(edited)");

        getSelectedPurchaseRequisition().addAction(BusinessEntity.Action.EDIT);

    }

    public void updateAutoGeneratePRNumber() {

        if (getSelectedPurchaseRequisition().getAutoGenerateNumber()) {
            getSelectedPurchaseRequisition().generateNumber();
        }

        updatePurchaseReq(null);
    }

    public void closeDialog() {
        PrimeFacesUtils.closeDialog(null);
    }

    public void closePurchaseReqDialog() {
        PrimeFacesUtils.closeDialog(null);
    }

    public Boolean getIsSelectedPurchaseReqIsValid() {
        return getSelectedPurchaseRequisition().getId() != null
                && !getSelectedPurchaseRequisition().getIsDirty();
    }

    public PurchaseRequisition getSelectedPurchaseRequisition() {
        if (selectedPurchaseRequisition == null) {
            selectedPurchaseRequisition = new PurchaseRequisition();
        }
        return selectedPurchaseRequisition;
    }

    public void setSelectedPurchaseRequisition(PurchaseRequisition selectedPurchaseRequisition) {
        this.selectedPurchaseRequisition = selectedPurchaseRequisition;
    }

    public Attachment getSelectedAttachment() {
        if (selectedAttachment == null) {
            selectedAttachment = new Attachment();
        }
        return selectedAttachment;
    }

    public void setSelectedAttachment(Attachment selectedAttachment) {
        this.selectedAttachment = selectedAttachment;
    }

    public void saveSelectedPurchaseRequisition() {

        if (getSelectedPurchaseRequisition().getIsDirty()) {
            ReturnMessage returnMessage;

            returnMessage = getSelectedPurchaseRequisition().prepareAndSave(getEntityManager1(), getUser());

            if (returnMessage.isSuccess()) {
                PrimeFacesUtils.addMessage("Saved!", "Purchase requisition was saved", FacesMessage.SEVERITY_INFO);
                getSelectedPurchaseRequisition().setEditStatus(" ");

            } else {
                PrimeFacesUtils.addMessage(returnMessage.getHeader(),
                        returnMessage.getMessage(),
                        FacesMessage.SEVERITY_ERROR);

                Utils.sendErrorEmail("An error occurred while saving a purchase requisition! - "
                        + returnMessage.getHeader(),
                        "Purchase requisition number: " + getSelectedPurchaseRequisition().getNumber()
                        + "\nJMTS User: " + getUser().getUsername()
                        + "\nDate/time: " + new Date()
                        + "\nDetail: " + returnMessage.getDetail(),
                        getEntityManager1());
            }
        } else {
            PrimeFacesUtils.addMessage("Already Saved",
                    "This purchase requisition was not saved because it was not modified or it was recently saved.",
                    FacesMessage.SEVERITY_INFO);
        }

    }

    private void emailProcurementOfficers(String action) {
        EntityManager em = getEntityManager1();

        List<Employee> procurementOfficers = Employee.
                findActiveEmployeesByPosition(em,
                        "Procurement Officer");

        for (Employee procurementOfficer : procurementOfficers) {
            JobManagerUser procurementOfficerUser
                    = JobManagerUser.findActiveJobManagerUserByEmployeeId(
                            em, procurementOfficer.getId());

            if (!getUser().equals(procurementOfficerUser)) {
                sendPurchaseReqEmail(em, procurementOfficerUser,
                        "a procurement officer", action);
            }

        }
    }

    private void emailPurchaseReqApprovers(String action) {
        EntityManager em = getEntityManager1();

        for (Employee approver : getSelectedPurchaseRequisition().getApprovers()) {

            JobManagerUser approverUser
                    = JobManagerUser.findActiveJobManagerUserByEmployeeId(
                            em, approver.getId());

            if (!getUser().equals(approverUser)) {
                sendPurchaseReqEmail(em, approverUser,
                        "an approver", action);
            }

        }
    }

    private void emailDepartmentRepresentatives(String action) {
        EntityManager em = getEntityManager1();

        JobManagerUser originatorUser = JobManagerUser.
                findActiveJobManagerUserByEmployeeId(em,
                        getSelectedPurchaseRequisition().getOriginator().getId());
        Employee head = getSelectedPurchaseRequisition().getOriginatingDepartment().getHead();
        Employee actingHead = getSelectedPurchaseRequisition().getOriginatingDepartment().getActingHead();
        JobManagerUser headUser = JobManagerUser.findActiveJobManagerUserByEmployeeId(em, head.getId());
        JobManagerUser actingHeadUser = JobManagerUser.findActiveJobManagerUserByEmployeeId(em, actingHead.getId());

        // Send to originator
        if (!getUser().equals(originatorUser)) {
            sendPurchaseReqEmail(em, originatorUser, "the orginator", action);
        }

        // Send to department head
        if (!getUser().equals(headUser)) {
            sendPurchaseReqEmail(em, headUser, "a department head", action);
        }

        // Send to acting head if active.
        if (!getUser().equals(actingHeadUser)) {
            if (getSelectedPurchaseRequisition().getOriginatingDepartment().getActingHeadActive()) {
                sendPurchaseReqEmail(em, actingHeadUser, "an acting department head", action);
            }
        }
    }

    private void sendPurchaseReqEmail(
            EntityManager em,
            JobManagerUser user,
            String role,
            String action) {

        Email email = Email.findActiveEmailByName(em, "pr-email-template");

        String prNum = getSelectedPurchaseRequisition().getNumber();
        String department = getSelectedPurchaseRequisition().
                getOriginatingDepartment().getName();
        String JMTSURL = (String) SystemOption.getOptionValueObject(em, "appURL");
        String originator = getSelectedPurchaseRequisition().getOriginator().getFirstName()
                + " " + getSelectedPurchaseRequisition().getOriginator().getLastName();
        String requisitionDate = BusinessEntityUtils.
                getDateInMediumDateFormat(getSelectedPurchaseRequisition().getRequisitionDate());
        String description = getSelectedPurchaseRequisition().getDescription();

        Utils.postMail(null, null,
                user,
                email.getSubject().
                        replace("{action}", action).
                        replace("{purchaseRequisitionNumber}", prNum),
                email.getContent("/correspondences/").
                        replace("{title}",
                                user.getEmployee().getTitle()).
                        replace("{surname}",
                                user.getEmployee().getLastName()).
                        replace("{JMTSURL}", JMTSURL).
                        replace("{purchaseRequisitionNumber}", prNum).
                        replace("{originator}", originator).
                        replace("{department}", department).
                        replace("{requisitionDate}", requisitionDate).
                        replace("{role}", role).
                        replace("{action}", action).
                        replace("{description}", description),
                email.getContentType(),
                em);
    }

    private synchronized void processPurchaseReqActions() {

        for (BusinessEntity.Action action : getSelectedPurchaseRequisition().getActions()) {
            switch (action) {
                case CREATE:
                    emailProcurementOfficers("created");
                    emailDepartmentRepresentatives("created");
                    emailPurchaseReqApprovers("created");
                    break;
                case EDIT:
                    emailProcurementOfficers("edited");
                    emailDepartmentRepresentatives("edited");
                    emailPurchaseReqApprovers("edited");
                    break;
                case APPROVE:
                    emailProcurementOfficers("approved");
                    emailDepartmentRepresentatives("approved");
                    emailPurchaseReqApprovers("approved");
                    break;
                case COMPLETE:
                    emailProcurementOfficers("completed");
                    emailDepartmentRepresentatives("completed");
                    emailPurchaseReqApprovers("completed");
                    break;
                default:
                    break;
            }
        }

        getSelectedPurchaseRequisition().getActions().clear();

    }

    public void onPurchaseReqCellEdit(CellEditEvent event) {
        BusinessEntityUtils.saveBusinessEntityInTransaction(getEntityManager1(),
                getFoundPurchaseReqs().get(event.getRowIndex()));
    }

    public int getNumOfPurchaseReqsFound() {
        return getFoundPurchaseReqs().size();
    }

    public String getPurchaseReqsTableHeader() {
        if (getUser().getPrivilege().getCanBeFinancialAdministrator()) {
            return "Search Results (found: " + getNumOfPurchaseReqsFound() + ")";
        } else {
            return "Search Results (found: " + getNumOfPurchaseReqsFound() + " for "
                    + getUser().getEmployee().getDepartment() + ")";
        }
    }

    public void editPurhaseReqSuppier() {
        setSelectedSupplier(getSelectedPurchaseRequisition().getSupplier());

        editSelectedSupplier();
    }

    public void purchaseReqSupplierDialogReturn() {
        if (getSelectedSupplier().getId() != null) {
            getSelectedPurchaseRequisition().setSupplier(getSelectedSupplier());

        }
    }

    public void purchaseReqDialogReturn() {
        if (getSelectedPurchaseRequisition().getIsDirty()) {
            PrimeFacesUtils.addMessage("Purchase requisition NOT saved",
                    "The recently edited purchase requisition was not saved",
                    FacesMessage.SEVERITY_WARN);
            PrimeFaces.current().ajax().update("headerForm:growl3");
            getSelectedPurchaseRequisition().setIsDirty(false);
        } else {
            doPurchaseReqSearch();
            // Process actions performed during the editing of the saved PR.
            if (getSelectedPurchaseRequisition().getId() != null) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            processPurchaseReqActions();
                        } catch (Exception e) {
                            System.out.println("Error processing PR actions: " + e);
                        }
                    }

                }.start();
            }
        }
    }

    public void createNewPurhaseReqSupplier() {
        createNewSupplier();

        editSelectedSupplier();
    }

    public void editSelectedPurchaseReq() {

        PrimeFacesUtils.openDialog(null, "purchreqDialog", true, true, true, false, 625, 700);
    }

    public List<PurchaseRequisition> getFoundPurchaseReqs() {
        if (foundPurchaseReqs == null) {
            doPurchaseReqSearch();
        }
        return foundPurchaseReqs;
    }

    public void setFoundPurchaseReqs(List<PurchaseRequisition> foundPurchaseReqs) {
        this.foundPurchaseReqs = foundPurchaseReqs;
    }

    public void doPurchaseReqSearch() {

        EntityManager em = getEntityManager1();

        if (!purchaseReqSearchText.isEmpty()) {
            foundPurchaseReqs = PurchaseRequisition.findByDateSearchField(em,
                    dateSearchPeriod.getDateField(), searchType, purchaseReqSearchText.trim(),
                    dateSearchPeriod.getStartDate(), dateSearchPeriod.getEndDate(),
                    searchDepartmentId);
        } else {
            foundPurchaseReqs = PurchaseRequisition.findByDateSearchField(em,
                    dateSearchPeriod.getDateField(), searchType, "",
                    dateSearchPeriod.getStartDate(), dateSearchPeriod.getEndDate(),
                    searchDepartmentId);
        }
    }

    public void doPurchaseReqSearch(DatePeriod dateSearchPeriod,
            String searchType, String searchText, Long searchDepartmentId) {

        this.dateSearchPeriod = dateSearchPeriod;
        this.searchType = searchType;
        this.purchaseReqSearchText = searchText;
        this.searchDepartmentId = searchDepartmentId;

        doPurchaseReqSearch();

    }

    public String getPurchaseReqSearchText() {
        return purchaseReqSearchText;
    }

    public void setPurchaseReqSearchText(String purchaseReqSearchText) {
        this.purchaseReqSearchText = purchaseReqSearchText;
    }

    public void createNewPurchaseReq() {
        selectedPurchaseRequisition = new PurchaseRequisition();
        selectedPurchaseRequisition.setPurchasingDepartment(Department.findDefaultDepartment(getEntityManager1(),
                "--"));
        selectedPurchaseRequisition.setProcurementOfficer(Employee.findDefaultEmployee(getEntityManager1(),
                "--", "--", false));
        selectedPurchaseRequisition.setSupplier(new Supplier("", true));
        selectedPurchaseRequisition.
                setOriginatingDepartment(getUser().getEmployee().getDepartment());
        selectedPurchaseRequisition.setOriginator(getUser().getEmployee());
        selectedPurchaseRequisition.setRequisitionDate(new Date());
        selectedPurchaseRequisition.generateNumber();
        selectedPurchaseRequisition.addAction(BusinessEntity.Action.CREATE);

        doPurchaseReqSearch(dateSearchPeriod,
                searchType,
                purchaseReqSearchText,
                getUser().getEmployee().getDepartment().getId());

        openPurchaseReqsTab();

        editSelectedPurchaseReq();
    }

    public void cancelDialogEdit(ActionEvent actionEvent) {
        PrimeFaces.current().dialog().closeDynamic(null);
    }

    /**
     * Gets the SystemManager object as a session bean.
     *
     * @return
     */
    public SystemManager getSystemManager() {
        return BeanUtils.findBean("systemManager");
    }

    public MainTabView getMainTabView() {

        return getSystemManager().getMainTabView();
    }

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    private void init() {
        selectedCostComponent = null;
        searchType = "Purchase requisitions";
        dateSearchPeriod = new DatePeriod("This year", "year",
                "requisitionDate", null, null, null, false, false, false);
        dateSearchPeriod.initDatePeriod();
        purchaseReqSearchText = "";
        foundPurchaseReqs = null;
        toEmployees = new ArrayList<>();
        supplierSearchText = "";
        searchText = "";
        foundSuppliers = new ArrayList<>();

        getSystemManager().addSingleLoginActionListener(this);
    }

    public void reset() {
        init();
    }

    public EntityManager getEntityManager1() {
        return EMF1.createEntityManager();
    }

    /**
     * Gets the SessionScoped bean that deals with user authentication.
     *
     * @return
     */
    public Authentication getAuthentication() {

        return BeanUtils.findBean("authentication");
    }

    public JobManagerUser getUser() {
        return getAuthentication().getUser();
    }

    public void onCostComponentSelect(SelectEvent event) {
        selectedCostComponent = (CostComponent) event.getObject();
    }

    public CostComponent getSelectedCostComponent() {
        return selectedCostComponent;
    }

    public void setSelectedCostComponent(CostComponent selectedCostComponent) {
        this.selectedCostComponent = selectedCostComponent;
    }

    public EntityManager getEntityManager2() {
        return EMF2.createEntityManager();
    }

    public void updateSelectedCostComponent() {
        getSelectedCostComponent().setIsDirty(true);
    }

    public void updateCostType() {
        switch (selectedCostComponent.getType()) {
            case "FIXED":
                selectedCostComponent.setIsFixedCost(true);
                selectedCostComponent.setIsHeading(false);
                selectedCostComponent.setHours(0.0);
                selectedCostComponent.setHoursOrQuantity(0.0);
                selectedCostComponent.setRate(0.0);
                break;
            case "HEADING":
                selectedCostComponent.setIsFixedCost(false);
                selectedCostComponent.setIsHeading(true);
                selectedCostComponent.setHours(0.0);
                selectedCostComponent.setHoursOrQuantity(0.0);
                selectedCostComponent.setRate(0.0);
                selectedCostComponent.setCost(0.0);
                selectedCostComponent.setUnit("");
                break;
            case "VARIABLE":
                selectedCostComponent.setIsFixedCost(false);
                selectedCostComponent.setIsHeading(false);
                break;
            default:
                selectedCostComponent.setIsFixedCost(false);
                selectedCostComponent.setIsHeading(false);
                break;
        }

        updateSelectedCostComponent();

    }

    public Boolean getAllowCostEdit() {
        if (selectedCostComponent != null) {
            if (null == selectedCostComponent.getType()) {
                return true;
            } else {
                switch (selectedCostComponent.getType()) {
                    case "--":
                        return true;
                    default:
                        return false;
                }
            }
        } else {
            return true;
        }
    }

    public void updateIsCostComponentHeading() {

    }

    public void updateIsCostComponentFixedCost() {

        if (getSelectedCostComponent().getIsFixedCost()) {

        }
    }

    public void deleteSelectedCostComponent() {
        deleteCostComponentByName(selectedCostComponent.getName());
    }

    public void deleteCostComponentByName(String componentName) {

        List<CostComponent> components = getSelectedPurchaseRequisition().getAllSortedCostComponents();
        int index = 0;
        for (CostComponent costComponent : components) {
            if (costComponent.getName().equals(componentName)) {
                components.remove(index);
                updatePurchaseReq(null);

                break;
            }
            ++index;
        }
    }

    public void deleteAttachmentByName(String attachmentName) {

        List<Attachment> attachments = getSelectedPurchaseRequisition().getAttachments();
        int index = 0;
        for (Attachment attachment : attachments) {
            if (attachment.getName().equals(attachmentName)) {
                attachments.remove(index);
                attachment.deleteFile();
                updatePurchaseReq(null);
                saveSelectedPurchaseRequisition();

                break;
            }
            ++index;
        }
    }

    public void editCostComponent(ActionEvent event) {
        setEdit(true);
    }

    public void createNewCostComponent(ActionEvent event) {
        selectedCostComponent = new CostComponent();
        setEdit(false);
    }

    public void addNewAttachment(ActionEvent event) {
        System.out.println("Adding new attachment"); // tk

        addAttachment(); // tk
    }

    // tk
    public void addAttachment() {

        PrimeFacesUtils.openDialog(null, "/common/attachmentDialog", true, true, true, 450, 700);
    }

    public void approveSelectedPurchaseRequisition(ActionEvent event) {

        // Check if the approver is already in the list of approvers
        if (BusinessEntityUtils.isBusinessEntityList(
                getSelectedPurchaseRequisition().getApprovers(),
                getUser().getEmployee().getId())) {

            PrimeFacesUtils.addMessage("Already Approved",
                    "You already approved this purchase requisition.",
                    FacesMessage.SEVERITY_INFO);

            return;

        }

        // Do not allow originator to approve
        if (getSelectedPurchaseRequisition().getOriginator().
                equals(getUser().getEmployee())) {

            PrimeFacesUtils.addMessage("Cannot Approve",
                    "The originator cannot approve this purchase requisition.",
                    FacesMessage.SEVERITY_WARN);

            return;

        }

        // Check if total cost is within the approver's limit
        if (isPRCostWithinApprovalLimit(getUser().getEmployee().getPositions())) {
            getSelectedPurchaseRequisition().getApprovers().add(getUser().getEmployee());
            setPRApprovalDate(getUser().getEmployee().getPositions());

            updatePurchaseReq(null);
            getSelectedPurchaseRequisition().addAction(BusinessEntity.Action.APPROVE);

        } else {

            PrimeFacesUtils.addMessage("Cannot Approve",
                    "You cannot approve this purchase requisition because the Total Cost is greater than your approval limit.",
                    FacesMessage.SEVERITY_WARN);

        }
    }

    private Boolean isPRCostWithinApprovalLimit(List<EmployeePosition> positions) {
        for (EmployeePosition position : positions) {
            if (position.getUpperApprovalLevel() >= getSelectedPurchaseRequisition().getTotalCost()) {
                return true;
            }
        }

        return false;
    }

    private void setPRApprovalDate(List<EmployeePosition> positions) {
        for (EmployeePosition position : positions) {
            switch (position.getTitle()) {
                case "Team Leader":
                    getSelectedPurchaseRequisition().setTeamLeaderApprovalDate(new Date());
                    return;
                case "Divisional Manager":
                    getSelectedPurchaseRequisition().setDivisionalManagerApprovalDate(new Date());
                    return;
                case "Divisional Director":
                    getSelectedPurchaseRequisition().setDivisionalDirectorApprovalDate(new Date());
                    return;
                case "Finance Manager":
                    getSelectedPurchaseRequisition().setFinanceManagerApprovalDate(new Date());
                    return;
                case "Executive Director":
                    getSelectedPurchaseRequisition().setExecutiveDirectorApprovalDate(new Date());
                    return;
                default:
                    getSelectedPurchaseRequisition().setApprovalDate(new Date());
                    return;
            }
        }
    }

    private void removePRApprovalDate(List<EmployeePosition> positions) {
        for (EmployeePosition position : positions) {
            switch (position.getTitle()) {
                case "Team Leader":
                    getSelectedPurchaseRequisition().setTeamLeaderApprovalDate(null);
                    break;
                case "Divisional Manager":
                    getSelectedPurchaseRequisition().setDivisionalManagerApprovalDate(null);
                    break;
                case "Divisional Director":
                    getSelectedPurchaseRequisition().setDivisionalDirectorApprovalDate(null);
                    break;
                case "Finance Manager":
                    getSelectedPurchaseRequisition().setFinanceManagerApprovalDate(null);
                    break;
                case "Executive Director":
                    getSelectedPurchaseRequisition().setExecutiveDirectorApprovalDate(null);
                    break;
                default:
                    break;
            }
        }
    }

    public void cancelCostComponentEdit() {
        selectedCostComponent.setIsDirty(false);
    }

    private EntityManagerFactory getEMF2() {
        return EMF2;
    }

    public List<CostComponent> copyCostComponents(List<CostComponent> srcCostComponents) {
        ArrayList<CostComponent> newCostComponents = new ArrayList<>();

        for (CostComponent costComponent : srcCostComponents) {
            CostComponent newCostComponent = new CostComponent(costComponent);
            newCostComponents.add(newCostComponent);
        }

        return newCostComponents;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public List getPriorityCodes() {
        ArrayList codes = new ArrayList();

        codes.add(new SelectItem("", ""));
        codes.add(new SelectItem("A", "A"));
        codes.add(new SelectItem("B", "B"));
        codes.add(new SelectItem("C", "C"));

        return codes;
    }

    @Override
    public void doDefaultSearch() {
        doSearch();
    }

    @Override
    public void doLogin() {
        getSystemManager().addSingleSearchActionListener(this);
    }

}
