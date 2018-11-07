/*
Job Management & Tracking System (JMTS) 
Copyright (C) 2017  D P Bennett & Associates Limited

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
package jm.com.dpbennett.purchasing;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.AccPacCustomer;
import jm.com.dpbennett.business.entity.BusinessEntity;
import jm.com.dpbennett.business.entity.BusinessOffice;
import jm.com.dpbennett.business.entity.Classification;
import jm.com.dpbennett.business.entity.Client;
import jm.com.dpbennett.business.entity.Contact;
import jm.com.dpbennett.business.entity.Country;
import jm.com.dpbennett.business.entity.DatePeriod;
import jm.com.dpbennett.business.entity.Department;
import jm.com.dpbennett.business.entity.DepartmentUnit;
import jm.com.dpbennett.business.entity.Distributor;
import jm.com.dpbennett.business.entity.DocumentType;
import jm.com.dpbennett.business.entity.Employee;
import jm.com.dpbennett.business.entity.Job;
import jm.com.dpbennett.business.entity.JobCategory;
import jm.com.dpbennett.business.entity.JobManagerUser;
import jm.com.dpbennett.business.entity.JobSample;
import jm.com.dpbennett.business.entity.JobSubCategory;
import jm.com.dpbennett.business.entity.Laboratory;
import jm.com.dpbennett.business.entity.Manufacturer;
import jm.com.dpbennett.business.entity.PetrolCompany;
import jm.com.dpbennett.business.entity.Preference;
import jm.com.dpbennett.business.entity.Sector;
import jm.com.dpbennett.business.entity.Service;
import jm.com.dpbennett.business.entity.SystemOption;
import jm.com.dpbennett.business.entity.fileutils.PropertiesFile;
import jm.com.dpbennett.business.entity.utils.BusinessEntityUtils;
import jm.com.dpbennett.business.entity.utils.SearchParameters;
import jm.com.dpbennett.business.entity.utils.Security;
import jm.com.dpbennett.wal.utils.DataItem;
import jm.com.dpbennett.wal.utils.DateUtils;
import jm.com.dpbennett.wal.utils.FinancialUtils;
import jm.com.dpbennett.wal.utils.ReportUtils;
import jm.com.dpbennett.wal.utils.SortableSelectItem;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;

/**
 *
 * @author Desmond Bennett
 */
@Named(value = "App")
@ApplicationScoped
@Singleton
public class PurchasingApplication implements Serializable {

    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    @PersistenceUnit(unitName = "AccPacPU")
    private EntityManagerFactory EMF2;
    private final Map<String, String> themes = new TreeMap<>();
    private final List<Job> openedJobs;

    /**
     * Creates a new instance of Application
     */
    public PurchasingApplication() {
        openedJobs = new ArrayList<>();

        // init primefaces themes
        themes.put("Aristo", "aristo");
        themes.put("Black-Tie", "black-tie");
        themes.put("Redmond", "redmond");
        themes.put("Dark Hive", "dark-hive");
    }

    public List<String> completeStrategicPriority(String query) {

        List<String> priorities = (List<String>) SystemOption.
                getOptionValueObject(getEntityManager1(), "StrategicPriorities");
        List<String> matchedPriority = new ArrayList<>();

        for (String priority : priorities) {
            if (priority.contains(query)) {
                matchedPriority.add(priority);
            }
        }

        return matchedPriority;

    }

    public synchronized List<Job> getOpenedJobs() {
        return openedJobs;
    }

    public synchronized void addOpenedJob(Job job) {
        getOpenedJobs().add(job);
    }

    public synchronized void removeOpenedJob(Job job) {
        getOpenedJobs().remove(job);
    }

    public synchronized Job findOpenedJob(Long jobId) {
        for (Job job : openedJobs) {
            if (Objects.equals(job.getId(), jobId)) {
                return job;
            }
        }

        return null;
    }

    public List<String> completeDocumentTypeName(String query) {

        try {
            List<DocumentType> types = DocumentType.findDocumentTypesByName(getEntityManager1(), query);
            List<String> suggestions = new ArrayList<>();
            if (types != null) {
                if (!types.isEmpty()) {
                    for (DocumentType type : types) {
                        suggestions.add(type.getName());
                    }
                }
            }

            return suggestions;
        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<String> completeDocumentTypeCode(String query) {

        try {
            List<DocumentType> types = DocumentType.findDocumentTypesByCode(getEntityManager1(), query);
            List<String> suggestions = new ArrayList<>();
            if (types != null) {
                if (!types.isEmpty()) {
                    for (DocumentType type : types) {
                        suggestions.add(type.getCode());
                    }
                }
            }

            return suggestions;
        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List getClassificationCategories() {
        return ReportUtils.getCategories(); //Classification.getCategories();
    }

    public List getDocumentStatuses() {
        ArrayList statuses = new ArrayList();

        statuses.add(new SelectItem("--", "--"));
        statuses.add(new SelectItem("Clarification required", "Clarification required"));
        statuses.add(new SelectItem("Completed", "Completed"));
        statuses.add(new SelectItem("On target", "On target"));
        statuses.add(new SelectItem("Transferred to Ministry", "Transferred to Ministry"));

        return statuses;
    }

    public List getPriorityLevels() {
        ArrayList levels = new ArrayList();

        levels.add(new SelectItem("--", "--"));
        levels.add(new SelectItem("High", "High"));
        levels.add(new SelectItem("Medium", "Medium"));
        levels.add(new SelectItem("Low", "Low"));
        levels.add(new SelectItem("Emergency", "Emergency"));

        return levels;
    }

    public List getDocumentForms() {
        ArrayList forms = new ArrayList();

        forms.add(new SelectItem("E", "Electronic"));
        forms.add(new SelectItem("H", "Hard copy"));
        forms.add(new SelectItem("V", "Verbal"));

        return forms;
    }

    // tk getStringListAsSelectItems
    public List<SelectItem> getWorkProgressList() {

        return PurchasingApplication.getStringListAsSelectItems(getEntityManager1(),
                "workProgressList");

    }

    public List getCostCodeList() {
        return FinancialUtils.getCostCodeList();
    }

    public Map<String, String> getThemes() {
        return themes;
    }

    public EntityManager getEntityManager1() {
        return EMF1.createEntityManager();
    }

    public EntityManager getEntityManager2() {
        return EMF2.createEntityManager();
    }

    public List getDateSearchFields() {
         return  DateUtils.getDateSearchFields("All");
    }

    public List getContactTypes() {

        return PurchasingApplication.getStringListAsSelectItems(getEntityManager1(), "personalContactTypes");

    }

    public List<SelectItem> getJamaicaParishes() {

        return PurchasingApplication.getStringListAsSelectItems(getEntityManager1(), "jamaicaParishes");

    }

    public List getSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("General", "General"));
        searchTypes.add(new SelectItem("My jobs", "My jobs"));
        searchTypes.add(new SelectItem("My department's jobs", "My department's jobs"));

        return searchTypes;
    }

    public List getLegalDocumentSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("General", "General"));

        return searchTypes;
    }

    /**
     * Returns the discount type that can be applied to a payment/amount
     *
     * @return
     */
    public List getDiscountTypes() {

        return FinancialUtils.getDiscountTypes();
    }

    /**
     *
     * @return
     */
    public List getPaymentTypes() {
        return FinancialUtils.getPaymentTypes();
    }

    /**
     *
     * @return
     */
    public List getPaymentPurposes() {
        return FinancialUtils.getPaymentPurposes();
    }

    /*
     * NB: Methods to be put in database and not hard coded.
     */
    public List getMethodsOfDisposal() {
        ArrayList methods = new ArrayList();

        // tk to be replaced by the user's organization
        String sysOption
                = (String) SystemOption.getOptionValueObject(getEntityManager1(),
                        "organizationName");

        methods.add(new SelectItem("1", "Collected by the client within 30 days"));
        if (sysOption != null) {
            methods.add(new SelectItem("2", "Disposed of by " + sysOption));
        } else {
            methods.add(new SelectItem("2", "Disposed of by us"));
        }
        methods.add(new SelectItem("3", "To be determined"));

        return methods;
    }

    /*
     * NB: Methods to be put in database and not hard coded
     */
    public List getJobSampleTypes() {
        ArrayList dateFields = new ArrayList();

        dateFields.add(new SelectItem("1", "Food"));
        dateFields.add(new SelectItem("2", "Electrical"));
        dateFields.add(new SelectItem("3", "Mechanical"));

        return dateFields;
    }

    public List<SelectItem> getDatePeriods() {
        ArrayList<SelectItem> datePeriods = new ArrayList<>();

        for (String name : DatePeriod.getDatePeriodNames()) {
            datePeriods.add(new SelectItem(name, name));
        }

        return datePeriods;
    }

    public List<String> completeDatePeriods(String query) {

        return DatePeriod.getDatePeriodNames();
    }

    public String getLogonUser() {
        String userName = System.getProperty("user.name");

        return userName;
    }

    // tk to be obtained from database
    public List<SelectItem> getTestMeasures() {

        return PurchasingApplication.getStringListAsSelectItems(getEntityManager1(), "petrolTestMeasures");

    }

    public List<SelectItem> getEquipmentWorkingStatus() {
        return PurchasingApplication.getStringListAsSelectItems(getEntityManager1(), "equipmentWorkingStatusList");
    }

    public Boolean findJobSample(List<JobSample> samples, String reference) {
        for (JobSample jobSample : samples) {
            if (jobSample.getReference().equals(reference)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> T findBean(String beanName) {
        FacesContext context = FacesContext.getCurrentInstance();
        return (T) context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", Object.class);
    }

    public List<BusinessOffice> completeBusinessOffice(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<BusinessOffice> offices = BusinessOffice.findActiveBusinessOfficesByName(em, query);

            return offices;
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<Department> completeActiveDepartment(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Department> departments = Department.findActiveDepartmentsByName(em, query);

            return departments;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<DocumentType> completeDocumentType(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<DocumentType> documentTypes = DocumentType.findDocumentTypesByName(em, query);

            return documentTypes;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> completeServiceName(String query) {
        List<String> serviceNames = new ArrayList<>();

        try {
            List<Service> services = Service.findServicesByName(getEntityManager1(), query);
            for (Service service : services) {
                serviceNames.add(service.getName());
            }

            return serviceNames;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<Service> completeService(String query) {

        try {
            return Service.findServicesByName(getEntityManager1(), query);
        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<Manufacturer> completeManufacturer(String query) {
        return Manufacturer.findManufacturersBySearchPattern(getEntityManager1(), query);
    }

    public List<Employee> completeActiveEmployee(String query) {
        EntityManager em;

        try {

            em = getEntityManager1();
            List<Employee> employees = Employee.findActiveEmployeesByName(em, query);

            if (employees != null) {
                return employees;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<Client> completeActiveClient(String query) {
        try {
            return Client.findActiveClientsByAnyPartOfName(getEntityManager1(), query);

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    // remove as it is implemented in ClientManager 
    public List<String> completeClientName(String query) {
        try {
            return Client.findActiveClientNames(getEntityManager1(), query);

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    private Employee getEmployeeByName(EntityManager em, String name) {
        String names[] = name.split(",");
        if (names.length == 2) {
            return Employee.findEmployeeByName(em,
                    names[1].trim(),
                    names[0].trim());
        } else {
            return null;
        }
    }

    public static String getSearchResultsTableHeader(SearchParameters searchParameters, List searchResultsList) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

        if (searchParameters != null) {
            if (searchParameters.getDatePeriod().getStartDate() != null
                    && searchParameters.getDatePeriod().getEndDate() != null) {
                return "Period: " + formatter.format(searchParameters.getDatePeriod().getStartDate()) + " to "
                        + formatter.format(searchParameters.getDatePeriod().getEndDate()) + " (found: "
                        + searchResultsList.size() + ")";
            } else {
                return "Search Results";
            }
        } else {
            return "Search Results";
        }
    }

    public static String getSearchResultsTableHeader(DatePeriod datePeriod, List searchResultsList) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

        if (datePeriod.getStartDate() != null
                && datePeriod.getEndDate() != null) {
            return "Period: " + formatter.format(datePeriod.getStartDate()) + " to "
                    + formatter.format(datePeriod.getEndDate()) + " (found: "
                    + searchResultsList.size() + ")";
        } else {
            return "Search Results";
        }

    }

    public static String getSearchResultsTableHeader(List searchResultsList) {
        return "Search Results (found: " + searchResultsList.size() + ")";
    }

    public List<Country> getCountries() {
        EntityManager em = getEntityManager1();

        List<Country> countries = Country.findAllCountries(em);

        return countries;
    }

    /**
     * Get LDAP attributes
     *
     * @param em
     * @param username
     * @param ctx
     * @return
     */
    public static Boolean checkForLDAPUser(EntityManager em, String username, javax.naming.ldap.LdapContext ctx) {

        try {
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String[] attrIDs = {"displayName"};

            constraints.setReturningAttributes(attrIDs);

            String name = (String) SystemOption.getOptionValueObject(em, "ldapContextName");
            NamingEnumeration answer = ctx.search(name, "SAMAccountName=" + username, constraints);

            if (!answer.hasMore()) { // Assuming only one match
                // LDAP user not found!
                return Boolean.FALSE;
            }
        } catch (NamingException ex) {
            System.out.println(ex);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public static JobManagerUser createNewUser(EntityManager em) {
        JobManagerUser user = new JobManagerUser();

        user.setUsername("");
        // init employee
        user.setEmployee(getDefaultEmployee(em, "--", "--"));

        // default privileges
        user.getPrivilege().setCanEditOwnJob(Boolean.TRUE);
        user.getPrivilege().setCanEditDepartmentJob(Boolean.TRUE);
        user.getPrivilege().setCanEnterJob(Boolean.TRUE);

        return user;
    }

    public static Employee getDefaultEmployee(EntityManager em, String firstName, String lastName) {
        Employee employee = Employee.findEmployeeByName(em, firstName, lastName);

        // create employee if it does not exist
        if (employee == null) {
            employee = new Employee();
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            //employee.setBusinessOffice(getDefaultBusinessOffice(em, "--"));
            //employee.setDepartment(getDefaultDepartment(em, "--"));
            // save
            em.getTransaction().begin();
            BusinessEntityUtils.saveBusinessEntity(em, employee);
            em.getTransaction().commit();
        }

        return employee;
    }

    public static BusinessOffice getDefaultBusinessOffice(EntityManager em, String name) {
        BusinessOffice office = BusinessOffice.findBusinessOfficeByName(em, name);

        if (office == null) {
            em.getTransaction().begin();
            office = new BusinessOffice();
            office.setName(name);
            BusinessEntityUtils.saveBusinessEntity(em, office);
            em.getTransaction().commit();
        }

        return office;
    }

    public static Department getDefaultDepartment(EntityManager em,
            String name) {
        Department department = Department.findDepartmentByName(em, name);

        if (department == null) {
            department = new Department();

            em.getTransaction().begin();
            department.setName(name);
            BusinessEntityUtils.saveBusinessEntity(em, department);
            em.getTransaction().commit();
        }

        return department;
    }

    public static Manufacturer getDefaultManufacturer(EntityManager em,
            String name) {
        Manufacturer manufacturer = Manufacturer.findManufacturerByName(em, name);

        if (manufacturer == null) {
            manufacturer = new Manufacturer();
            em.getTransaction().begin();
            manufacturer.setName(name);
            BusinessEntityUtils.saveBusinessEntity(em, manufacturer);
            em.getTransaction().commit();
        }

        return manufacturer;
    }

    /**
     * Get the default distributor by name and creates one if it does not exist.
     *
     * @param em
     * @param name
     * @return
     */
    public static Distributor getDefaultDistributor(EntityManager em,
            String name) {
        Distributor distributor = Distributor.findDistributorByName(em, name);

        if (distributor == null) {
            distributor = new Distributor();
            em.getTransaction().begin();
            distributor.setName(name);
            BusinessEntityUtils.saveBusinessEntity(em, distributor);
            em.getTransaction().commit();
        }

        return distributor;
    }

    public static PetrolCompany getDefaultPetrolCompany(EntityManager em,
            String name) {
        PetrolCompany petrolCompany = PetrolCompany.findPetrolCompanyByName(em, name);

        if (petrolCompany == null) {
            petrolCompany = new PetrolCompany();

            em.getTransaction().begin();
            petrolCompany.setName(name);
            BusinessEntityUtils.saveBusinessEntity(em, petrolCompany);
            em.getTransaction().commit();
        }

        return petrolCompany;
    }

    public static Contact getDefaultContact(EntityManager em, String firstName, String lastName) {

        Contact contact = Contact.findContactByName(em, firstName, lastName);

        // create employee if it does not exist
        if (contact == null) {
            contact = new Contact();
            contact.setFirstName(firstName);
            contact.setLastName(lastName);
            // save
            em.getTransaction().begin();
            BusinessEntityUtils.saveBusinessEntity(em, contact);
            em.getTransaction().commit();
        }

        return contact;
    }

    public static Manufacturer getValidManufacturer(EntityManager em, String name) {

        if ((name == null) || (name.equals(""))) {
            return null;
        } else if (!BusinessEntityUtils.validateName(name)) {
            return null;
        } else {
            Manufacturer currentManufacturer = Manufacturer.findManufacturerByName(em, name);
            if (currentManufacturer == null) {
                // create new
                currentManufacturer = new Manufacturer();
                // replace double quotes with two single quotes to avoid query issues
                currentManufacturer.setName(name.replaceAll("\"", "''"));

                return currentManufacturer;
            } else { // create and return new manufacturer
                return currentManufacturer;
            }
        }
    }

    public static List<SelectItem> getStringListAsSelectItems(EntityManager em,
            String systemOption) {

        ArrayList list = new ArrayList();

        List<String> stringList = (List<String>) SystemOption.getOptionValueObject(em, systemOption);

        for (String name : stringList) {
            list.add(new SelectItem(name, name));
        }

        return list;
    }

    public static List<SortableSelectItem> getStringListAsSortableSelectItems(EntityManager em,
            String systemOption) {

        ArrayList list = new ArrayList();

        List<String> stringList = (List<String>) SystemOption.getOptionValueObject(em, systemOption);

        for (String name : stringList) {
            list.add(new SortableSelectItem(name, name));
        }

        return list;
    }

    public static void main(String[] args) {
        try {

            PropertiesFile propertiesFile = new PropertiesFile("LabelPrint.properties");
            propertiesFile.load();
            HashMap prop = new HashMap();

            ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                    .setJdbcUrl(propertiesFile.getProperty("ConnectionURL"))
                    .setJdbcUsername(propertiesFile.getProperty("ConnectionUserName"))
                    .setJdbcPassword(Security.decrypt(propertiesFile.
                            getProperty("ConnectionPassword")))
                    .setJdbcDriver(propertiesFile.getProperty("ConnectionDriverName"))
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);

            ProcessEngine processEngine = cfg.buildProcessEngine();
            String pName = processEngine.getName();
            String ver = ProcessEngine.VERSION;
            System.out.println("ProcessEngine [" + pName + "] Version: [" + ver + "]");
//
//            prop.put("javax.persistence.jdbc.user",
//                    propertiesFile.getProperty("ConnectionUserName"));
//            prop.put("javax.persistence.jdbc.password",
//                    Security.decrypt(propertiesFile.getProperty("ConnectionPassword")));
//            prop.put("javax.persistence.jdbc.url",
//                    propertiesFile.getProperty("ConnectionURL"));
//            prop.put("javax.persistence.jdbc.driver",
//                    propertiesFile.getProperty("ConnectionDriverName"));

//            if (setupDatabaseConnection("PU", prop)) {
//                EntityManager em = EMF.createEntityManager();
//                List<BusinessEntity> list
//                        = EnergyConsumptionAndEfficiency.findAllByProductType(em, "Refrigerator");
//                System.out.println("List: " + list);
//                EnergyConsumptionAndEfficiency ecaf
//                        = EnergyConsumptionAndEfficiency.findById(em, Long.parseLong("1"));
//                System.out.println("ecaf: " + ecaf);
//            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public synchronized static void saveBusinessEntity(EntityManager em, BusinessEntity entity) {
        try {

            em.getTransaction().begin();
            BusinessEntityUtils.saveBusinessEntity(em, entity);
            em.getTransaction().commit();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static List<DataItem> getStringListAsSortableDataItems(EntityManager em,
            String systemOption) {
        ArrayList list = new ArrayList();

        List<String> stringList = (List<String>) SystemOption.getOptionValueObject(em, systemOption);

        for (String name : stringList) {
            list.add(new DataItem(name, name));
        }

        return list;
    }

    public List<Classification> completeClassification(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Classification> classifications = Classification.findActiveClassificationsByName(em, query);

            return classifications;
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<Classification> completeJobClassification(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Classification> classifications = Classification.findActiveClassificationsByNameAndCategory(em, query, "Job");

            return classifications;
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public ArrayList<String> completeCountry(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            ArrayList<Country> countries = new ArrayList<>(Country.findCountriesByName(em, query));
            ArrayList<String> countriesList = (ArrayList<String>) (ArrayList<?>) countries;

            return countriesList;
        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<AccPacCustomer> completeAccPacClient(String query) {
        EntityManager em2;

        try {
            em2 = getEntityManager2();

            return AccPacCustomer.findAccPacCustomersByName(em2, query);
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<String> completePreferenceValue(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<String> preferenceValues = Preference.findAllPreferenceValues(em, query);

            return preferenceValues;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<String> getJobTableViews() {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<String> preferenceValues = Preference.findAllPreferenceValues(em, "");

            return preferenceValues;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<DepartmentUnit> completeDepartmentUnit(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<DepartmentUnit> departmentUnits = DepartmentUnit.findDepartmentUnitsByName(em, query);

            return departmentUnits;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<Laboratory> completeLaboratory(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Laboratory> laboratories = Laboratory.findLaboratoriesByName(em, query);

            return laboratories;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    /**
     * NB: query parameter currently not used to filter sectors.
     *
     * @param query
     * @return
     */
    public List<Sector> completeActiveSectors(String query) {
        EntityManager em = getEntityManager1();

        List<Sector> sectors = Sector.findAllActiveSectors(em);

        return sectors;
    }

    /**
     * NB: query not used to filter
     *
     * @param query
     * @return
     */
    public List<JobCategory> completeActiveJobCategories(String query) {
        EntityManager em = getEntityManager1();

        List<JobCategory> categories = JobCategory.findAllActiveJobCategories(em);

        return categories;
    }

    public List<JobSubCategory> completeActiveJobSubCategories(String query) {
        EntityManager em = getEntityManager1();

        List<JobSubCategory> subCategories = JobSubCategory.findAllActiveJobSubCategories(em);

        return subCategories;
    }
}
