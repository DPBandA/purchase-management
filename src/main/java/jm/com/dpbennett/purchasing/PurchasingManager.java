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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.ldap.InitialLdapContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.AccPacCustomer;
import jm.com.dpbennett.business.entity.DatePeriod;
import jm.com.dpbennett.business.entity.Job;
import jm.com.dpbennett.business.entity.JobManagerUser;
import jm.com.dpbennett.business.entity.SystemOption;
import jm.com.dpbennett.business.entity.management.MessageManagement;
import jm.com.dpbennett.business.entity.management.UserManagement;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.TabCloseEvent;
import org.primefaces.event.ToggleEvent;
import jm.com.dpbennett.business.entity.management.BusinessEntityManagement;
import static jm.com.dpbennett.purchasing.PurchasingApplication.checkForLDAPUser;
import jm.com.dpbennett.wal.utils.Dashboard;
import jm.com.dpbennett.wal.utils.DateUtils;
import jm.com.dpbennett.wal.utils.DialogActionHandler;
import jm.com.dpbennett.wal.utils.Utils;
import jm.com.dpbennett.wal.utils.MainTabView;
import jm.com.dpbennett.wal.utils.PrimeFacesUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.component.tabview.Tab;

/**
 *
 * @author Desmond Bennett
 */
@Named
@SessionScoped
public class PurchasingManager implements Serializable, BusinessEntityManagement,
        DialogActionHandler, UserManagement, MessageManagement {

    private PurchasingApplication application;
    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    @PersistenceUnit(unitName = "AccPacPU")
    private EntityManagerFactory EMF2;
    private Job currentJob;
    private Job selectedJob;
    private Boolean dynamicTabView;
    private Boolean renderSearchComponent;
    @ManagedProperty(value = "Jobs")
    private Integer longProcessProgress;
    private Boolean useAccPacCustomerList;
    private Boolean showJobEntry;
    private List<Job> jobSearchResultList;
    private Integer loginAttempts;
    // Managers/Management
    private ReportManager reportManager;
    private DatePeriod dateSearchPeriod;
    private String searchType;
    private String searchText;
    private String dialogActionHandlerId;
    private String jobsTabTitle;
    private Job[] selectedJobs;
    // Authentication
    private JobManagerUser user;
    private Boolean userLoggedIn;
    private Boolean showLogin;
    private String username;
    private String password;
    private String logonMessage;
    private Boolean westLayoutUnitCollapsed;
    private String invalidFormFieldMessage;
    // tk rid of dialog* and handler and use growl?
    private String dialogMessage;
    private String dialogMessageHeader;
    private String dialogMessageSeverity;
    private Boolean dialogRenderOkButton;
    private Boolean dialogRenderYesButton;
    private Boolean dialogRenderNoButton;
    private Boolean dialogRenderCancelButton;
    private DialogActionHandler dialogActionHandler;
    private Dashboard dashboard;
    private MainTabView mainTabView;
    private AccPacCustomer accPacCustomer;

    /**
     * Creates a new instance of JobManager
     */
    public PurchasingManager() {
        init();
    }

    /**
     * Gets the ApplicationScoped object that is associated with this webapp.
     *
     * @return
     */
    public PurchasingApplication getApplication() {
        if (application == null) {
            application = PurchasingApplication.findBean("App");
        }
        return application;
    }

    /**
     * Finds and Accpac customer by name and updates the Accpac customer field.
     *
     * @param event
     */
    public void updateAccPacCustomer(SelectEvent event) {
        EntityManager em = getEntityManager2();

        accPacCustomer = AccPacCustomer.findByName(em, accPacCustomer.getCustomerName().trim());
        if (accPacCustomer != null) {
            if (accPacCustomer.getId() != null) {
                accPacCustomer.setIsDirty(true);
            }
        }
    }

    /**
     * Gets the Accpac customer field.
     *
     * @return
     */
    public AccPacCustomer getAccPacCustomer() {
        if (accPacCustomer == null) {
            accPacCustomer = new AccPacCustomer();
        }
        return accPacCustomer;
    }

    /**
     * Sets the Accpac customer field.
     *
     * @param accPacCustomer
     */
    public void setAccPacCustomer(AccPacCustomer accPacCustomer) {
        this.accPacCustomer = accPacCustomer;
    }

    /**
     * Handles the initialization of the JobManager session bean.
     *
     */
    private void init() {
        password = "";
        username = "";
        showLogin = true;
        userLoggedIn = false;
        westLayoutUnitCollapsed = true;
        logonMessage = "Please provide your login details below:";
        loginAttempts = 0;
        showJobEntry = false;
        longProcessProgress = 0;
        useAccPacCustomerList = false;
        dynamicTabView = true;
        renderSearchComponent = true;
        jobSearchResultList = new ArrayList<>();
        dashboard = new Dashboard(getUser());
        mainTabView = new MainTabView(getUser());
        // Search fields init
        searchType = "";
        dateSearchPeriod = new DatePeriod("This month", "month",
                "dateAndTimeEntered", null, null, null, false, false, false);
        dateSearchPeriod.initDatePeriod();
    }

    /**
     * Get ReportManager SessionScoped bean.
     *
     * @return
     */
    public ReportManager getReportManager() {
        if (reportManager == null) {
            reportManager = PurchasingApplication.findBean("reportManager");
        }

        return reportManager;
    }

    /**
     * Gets the date search period for jobs.
     *
     * @return
     */
    public DatePeriod getDateSearchPeriod() {
        return dateSearchPeriod;
    }

    public void setDateSearchPeriod(DatePeriod dateSearchPeriod) {
        this.dateSearchPeriod = dateSearchPeriod;
    }

    public void updateDateSearchField() {
        //doSearch();
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public ArrayList getSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("General", "General"));
        searchTypes.add(new SelectItem("My jobs", "My jobs"));
        searchTypes.add(new SelectItem("My department's jobs", "My department's jobs"));
        searchTypes.add(new SelectItem("Parent jobs only", "Parent jobs only"));
        searchTypes.add(new SelectItem("Unapproved job costings", "Unapproved job costings"));
        searchTypes.add(new SelectItem("Incomplete jobs", "Incomplete jobs"));

        return searchTypes;
    }

    public ArrayList getDateSearchFields() {
        return DateUtils.getDateSearchFields();
    }

    public ArrayList getAuthorizedSearchTypes() {

        // Filter list based on user's authorization
        EntityManager em = getEntityManager1();

        if (getUser(em).getPrivilege().getCanEditJob()
                || getUser(em).getPrivilege().getCanEnterJob()
                || getUser(em).getPrivilege().getCanEditInvoicingAndPayment()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEditInvoicingAndPayment()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEditJob()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEnterJob()) {
            return getSearchTypes();
        } else {

            ArrayList newList = new ArrayList();
            for (Object obj : getSearchTypes()) {
                SelectItem item = (SelectItem) obj;
                if (!item.getLabel().equals("General")
                        && !item.getLabel().equals("Unapproved job costings")
                        && !item.getLabel().equals("Incomplete jobs")) {
                    newList.add(item);
                }
            }

            return newList;
        }

    }

    public void reset() {

        userLoggedIn = false;
        showLogin = true;
        password = "";
        username = "";
        logonMessage = "Please provide your login details below:";
        user = new JobManagerUser();
        westLayoutUnitCollapsed = true;
        renderSearchComponent = true;
        jobSearchResultList = new ArrayList<>();

        getReportManager().reset();

        // Unrender all tabs
        dashboard.removeAllTabs();
        dashboard.setRender(false);
        mainTabView.removeAllTabs();
        mainTabView.setRender(false);

        updateAllForms();

        // Return to default theme
        PrimeFaces.current().executeScript(
                "PF('loginDialog').show();"
                + "PF('longProcessDialogVar').hide();"
                + "PrimeFaces.changeTheme('"
                + getUser().getUserInterfaceThemeName() + "');"
                + "PF('layoutVar').toggle('west');");

    }

    public MainTabView getMainTabView() {
        return mainTabView;
    }

    public void setMainTabView(MainTabView mainTabView) {
        this.mainTabView = mainTabView;
    }

    public String getApplicationHeader() {

        return "Purchasing";

    }

    public String getApplicationSubheader() {
        String subHeader = (String) SystemOption.getOptionValueObject(
                getEntityManager1(),
                "applicationSubheader");

        if (subHeader != null) {
            if (subHeader.trim().equals("None")) {
                return getUser().getEmployee().getDepartment().getName();
            }
        } else {
            subHeader = "";
        }

        return subHeader;
    }

    public Boolean getDialogRenderCancelButton() {
        return dialogRenderCancelButton;
    }

    public void setDialogRenderCancelButton(Boolean dialogRenderCancelButton) {
        this.dialogRenderCancelButton = dialogRenderCancelButton;
    }

    public void setDialogActionHandler(DialogActionHandler dialogActionHandler) {
        this.dialogActionHandler = dialogActionHandler;
    }

    public Boolean getDialogRenderOkButton() {
        return dialogRenderOkButton;
    }

    public void setDialogRenderOkButton(Boolean dialogRenderOkButton) {
        this.dialogRenderOkButton = dialogRenderOkButton;
    }

    public Boolean getDialogRenderYesButton() {
        return dialogRenderYesButton;
    }

    public void setDialogRenderYesButton(Boolean dialogRenderYesButton) {
        this.dialogRenderYesButton = dialogRenderYesButton;
    }

    public Boolean getDialogRenderNoButton() {
        return dialogRenderNoButton;
    }

    public void setDialogRenderNoButton(Boolean dialogRenderNoButton) {
        this.dialogRenderNoButton = dialogRenderNoButton;
    }

    public Integer getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(Integer loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public String getDialogMessage() {
        return dialogMessage;
    }

    public void setDialogMessage(String dialogMessage) {
        this.dialogMessage = dialogMessage;
    }

    public String getDialogMessageHeader() {
        return dialogMessageHeader;
    }

    public void setDialogMessageHeader(String dialogMessageHeader) {
        this.dialogMessageHeader = dialogMessageHeader;
    }

    public String getDialogMessageSeverity() {
        return dialogMessageSeverity;
    }

    public void setDialogMessageSeverity(String dialogMessageSeverity) {
        this.dialogMessageSeverity = dialogMessageSeverity;
    }

    public Boolean getWestLayoutUnitCollapsed() {
        return westLayoutUnitCollapsed;
    }

    public void setWestLayoutUnitCollapsed(Boolean westLayoutUnitCollapsed) {
        this.westLayoutUnitCollapsed = westLayoutUnitCollapsed;
    }

    @Override
    public String getLogonMessage() {
        return logonMessage;
    }

    @Override
    public void setLogonMessage(String logonMessage) {
        this.logonMessage = logonMessage;
    }

    @Override
    public Boolean getUserLoggedIn() {
        return userLoggedIn;
    }

    @Override
    public void setUserLoggedIn(Boolean userLoggedIn) {
        this.userLoggedIn = userLoggedIn;
    }

    @Override
    public Boolean getShowLogin() {
        return showLogin;
    }

    @Override
    public void setShowLogin(Boolean showLogin) {
        this.showLogin = showLogin;
    }

    @Override
    public String getUsername() {

        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {

        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    public EntityManager getEntityManager1() {
        return EMF1.createEntityManager();
    }

    @Override
    public void setUser(JobManagerUser user) {
        this.user = user;
    }

    @Override
    public JobManagerUser getUser() {
        if (user == null) {
            return new JobManagerUser();
        }
        return user;
    }

    /**
     * Get user as currently stored in the database
     *
     * @param em
     * @return
     */
    public JobManagerUser getUser(EntityManager em) {
        if (user == null) {
            return new JobManagerUser();
        } else {
            try {
                if (user.getId() != null) {
                    JobManagerUser foundUser = em.find(JobManagerUser.class, user.getId());
                    if (foundUser != null) {
                        em.refresh(foundUser);
                        user = foundUser;
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
                return new JobManagerUser();
            }
        }

        return user;
    }

    public void checkLoginAttemps() {

        ++loginAttempts;
        if (loginAttempts == 2) {
            PrimeFaces.current().executeScript("PF('loginAttemptsDialog').show();");
            try {
                // Send email to system administrator alert if activated
                if ((Boolean) SystemOption.getOptionValueObject(getEntityManager1(),
                        "developerEmailAlertActivated")) {
                    Utils.postMail(null, null, "Failed user login", "Username: " + username + "\nDate/Time: " + new Date());
                }
            } catch (Exception ex) {
                Logger.getLogger(PurchasingManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (loginAttempts > 2) {// tk # attempts to be made option
            PrimeFaces.current().executeScript("PF('loginAttemptsDialog').show();");
        }

        username = "";
        password = "";
    }

    public void login() {

        EntityManager em = getEntityManager1();

        setUserLoggedIn(false);

        try {
            // Find user and determin if authentication is required for this user
            JobManagerUser jobManagerUser = JobManagerUser.findActiveJobManagerUserByUsername(em, getUsername());

            if (jobManagerUser != null) {
                em.refresh(jobManagerUser);
                if (!jobManagerUser.getAuthenticate()) {
                    System.out.println("User will NOT be authenticated.");
                    PrimeFacesUtils.addMessage("NOT Authenticated!",
                            "Authentication is not activated. Please contact your System Administrator",
                            FacesMessage.SEVERITY_WARN);
                    setUser(jobManagerUser);
                    setUserLoggedIn(true);
                } else if (validateAndAssociateUser(em, getUsername(), getPassword())) {
                    System.out.println("User will be authenticated.");
                    setUser(jobManagerUser);
                    setUserLoggedIn(true);
                } else {
                    checkLoginAttemps();
                    logonMessage = "Please enter a valid username.";
                }
            } else {
                logonMessage = "Please enter a registered username.";
                username = "";
                password = "";
            }
            // wrap up
            if (getUserLoggedIn()) {
                user.logActivity("Logged in", getEntityManager1());
                setShowLogin(false);
                username = "";
                password = "";
                loginAttempts = 0;

                user.save(getEntityManager1());

                if (westLayoutUnitCollapsed) {
                    westLayoutUnitCollapsed = false;
                    PrimeFaces.current().executeScript("PF('layoutVar').toggle('west');");
                }

                PrimeFaces.current().executeScript("PF('loginDialog').hide();");

                PrimeFaces.current().executeScript("PrimeFaces.changeTheme('"
                        + getUser().getUserInterfaceThemeName() + "');");

                dashboard.reset(user);
                mainTabView.reset(user);

                logonMessage = "Login error occured! Please try again or contact the System Administrator";
                username = "";
                password = "";
            }

            em.close();

            updateAllForms();
        } catch (Exception e) {
            System.out.println(e);
            logonMessage = "Login error occured! Please try again or contact the System Administrator";
            checkLoginAttemps();
        }
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    private void updateAllForms() {
        PrimeFaces.current().ajax().update("dashboardForm");
        PrimeFaces.current().ajax().update("mainTabViewForm");
        PrimeFaces.current().ajax().update("headerForm");
        PrimeFaces.current().ajax().update("loginForm");
    }

    public void logout() {

        user.logActivity("Logged out", getEntityManager1());

        reset();
    }

    public void handleKeepAlive() {
        getUser().setPollTime(new Date());

        if ((Boolean) SystemOption.getOptionValueObject(getEntityManager1(), "debugMode")) {
            System.out.println("Handling keep alive session: doing polling for Purchasing..." + getUser().getPollTime());
        }
        if (getUser().getId() != null) {
            getUser().save(getEntityManager1());
        }
    }

    public void handleLayoutUnitToggle(ToggleEvent event) {

        if (event.getComponent().getId().equals("dashboard")) {
            westLayoutUnitCollapsed = !event.getVisibility().name().equals("VISIBLE");
        }
    }

    public Boolean renderUserMenu() {
        return getUser().getId() != null;
    }

    @Override
    public String getInvalidFormFieldMessage() {
        return invalidFormFieldMessage;
    }

    @Override
    public void setInvalidFormFieldMessage(String invalidFormFieldMessage) {
        this.invalidFormFieldMessage = invalidFormFieldMessage;
    }

    public void displayCommonMessageDialog(DialogActionHandler dialogActionHandler, String dialogMessage,
            String dialogMessageHeader,
            String dialoMessageSeverity) {

        setDialogActionHandler(dialogActionHandler);

        setDialogRenderOkButton(true);
        setDialogRenderYesButton(false);
        setDialogRenderNoButton(false);

        setDialogMessage(dialogMessage);
        setDialogMessageHeader(dialogMessageHeader);
        setDialogMessageSeverity(dialoMessageSeverity);

        PrimeFaces.current().ajax().update("commonMessageDialogForm");
        PrimeFaces.current().executeScript("PF('commonMessageDialog').show();");
    }

    public void displayCommonConfirmationDialog(DialogActionHandler dialogActionHandler,
            String dialogMessage,
            String dialogMessageHeader,
            String dialoMessageSeverity) {

        setDialogActionHandler(dialogActionHandler);

        setDialogRenderOkButton(false);
        setDialogRenderYesButton(true);
        setDialogRenderNoButton(true);
        setDialogRenderCancelButton(true);

        setDialogMessage(dialogMessage);
        setDialogMessageHeader(dialogMessageHeader);
        setDialogMessageSeverity(dialoMessageSeverity);

        PrimeFaces.current().ajax().update("commonMessageDialogForm");
        PrimeFaces.current().executeScript("PF('commonMessageDialog').show();");
    }

    public void handleDialogOkButtonPressed() {
        if (dialogActionHandler != null) {
            dialogActionHandler.handleDialogOkButtonClick();
        }
    }

    public void handleDialogYesButtonPressed() {
        if (dialogActionHandler != null) {
            dialogActionHandler.handleDialogYesButtonClick();
        }
    }

    public void handleDialogNoButtonPressed() {
        if (dialogActionHandler != null) {
            dialogActionHandler.handleDialogNoButtonClick();
        }
    }

    public void handleDialogCancelButtonPressed() {
        if (dialogActionHandler != null) {
            dialogActionHandler.handleDialogCancelButtonClick();
        }
    }

    /**
     * Validate a user and associate the user with an employee if possible.
     *
     * @param em
     * @param username
     * @param password
     * @return
     */
    @Override
    public Boolean validateAndAssociateUser(EntityManager em, String username, String password) {
        Boolean userValidated = false;
        InitialLdapContext ctx;

        try {
            List<jm.com.dpbennett.business.entity.LdapContext> ctxs = jm.com.dpbennett.business.entity.LdapContext.findAllActiveLdapContexts(em);

            for (jm.com.dpbennett.business.entity.LdapContext ldapContext : ctxs) {
                ctx = ldapContext.getInitialLDAPContext(username, password);

                if (checkForLDAPUser(em, username, ctx)) {
                    // user exists in LDAP                    
                    userValidated = true;
                    break;
                }
            }

            // get the user if one exists
            if (userValidated) {

                System.out.println("User validated.");

                return true;

            } else {
                System.out.println("User NOT validated!");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Problem connecting to directory: " + e);
        }

        return false;
    }

    public void prepareToCloseJobDetail() {
        PrimeFacesUtils.closeDialog(null);
    }

    public void onMainViewTabClose(TabCloseEvent event) {
        String tabId = ((Tab) event.getData()).getId();
        mainTabView.addTab(getEntityManager1(), tabId, false);
    }

    public void onMainViewTabChange(TabChangeEvent event) {
        // Nothing to do yet
        // String tabId = ((MainTab) event.getData()).getId();      
    }

    public void onDashboardTabChange(TabChangeEvent event) {

        getDashboard().setSelectedTabId(((Tab) event.getData()).getId());
    }

    public void updateDashboard(String tabId) {

        PrimeFaces.current().ajax().update("dashboardForm");

    }

    /**
     * Get selected job which is usually displayed in a table.
     *
     * @return
     */
    public Job[] getSelectedJobs() {
        return selectedJobs;
    }

    public void setSelectedJobs(Job[] selectedJobs) {
        this.selectedJobs = selectedJobs;
    }

    public void editPreferences() {
    }

    public void openJobBrowser() {
        // Add the Job Browser tab is 
        mainTabView.addTab(getEntityManager1(), "Job Browser", true);
        mainTabView.select("Job Browser");
    }

    public void openSystemAdministrationTab() {
        mainTabView.addTab(getEntityManager1(), "System Administration", true);
        mainTabView.select("System Administration");
    }

    public void openFinancialAdministrationTab() {
        mainTabView.addTab(getEntityManager1(), "Financial Administration", true);
        mainTabView.select("Financial Administration");
    }

    public String getJobsTabTitle() {
        return jobsTabTitle;
    }

    public void setJobsTabTitle(String jobsTabTitle) {
        this.jobsTabTitle = jobsTabTitle;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public Boolean getShowJobEntry() {
        return showJobEntry;
    }

    public void setShowJobEntry(Boolean showJobEntry) {
        this.showJobEntry = showJobEntry;
    }

    private Boolean isCurrentJobJobAssignedToUser() {
        if (getUser() != null) {
            return currentJob.getAssignedTo().getId().longValue() == getUser().getEmployee().getId().longValue();
        } else {
            return false;
        }
    }

    private Boolean isJobAssignedToUserDepartment() {

        if (getUser() != null) {
            if (currentJob.getDepartment().getId().longValue() == getUser().getEmployee().getDepartment().getId().longValue()) {
                return true;
            } else {
                return currentJob.getSubContractedDepartment().getId().longValue()
                        == getUser().getEmployee().getDepartment().getId().longValue();
            }
        } else {
            return false;
        }
    }

    public Boolean getCanEnterJob() {
        if (getUser() != null) {
            return getUser().getPrivilege().getCanEnterJob();
        } else {
            return false;
        }
    }

    /**
     * Can edit job only if the job is assigned to your department or if you
     * have job entry privilege
     *
     * @return
     */
    public Boolean getCanEditDepartmentalJob() {
        if (getCanEnterJob()) {
            return true;
        }

        if (getUser() != null) {
            return getUser().getPrivilege().getCanEditDepartmentJob() && isJobAssignedToUserDepartment();
        } else {
            return false;
        }
    }

    public Boolean getCanEditOwnJob() {
        if (getCanEnterJob()) {
            return true;
        }

        if (getUser() != null) {
            return getUser().getPrivilege().getCanEditOwnJob();
        } else {
            return false;
        }
    }

    public Integer getLongProcessProgress() {
        if (longProcessProgress == null) {
            longProcessProgress = 0;
        } else {
            if (longProcessProgress < 10) {
                // this is to ensure that this method does not make the progress
                // complete as this is done elsewhere.
                longProcessProgress = longProcessProgress + 1;
            }
        }

        return longProcessProgress;
    }

    public void onLongProcessComplete() {
        longProcessProgress = 0;
    }

    public void setLongProcessProgress(Integer longProcessProgress) {
        this.longProcessProgress = longProcessProgress;
    }

    /**
     * For future implementation if necessary
     *
     * @param query
     * @return
     */
    public List<String> completeSearchText(String query) {
        List<String> suggestions = new ArrayList<>();

        return suggestions;
    }

    public Boolean getRenderSearchComponent() {
        return renderSearchComponent;
    }

    public void setRenderSearchComponent(Boolean renderSearchComponent) {
        this.renderSearchComponent = renderSearchComponent;
    }

    public void updatePreferedJobTableView(SelectEvent event) {
        //doJobViewUpdate((String) event.getObject());
        getUser().save(getEntityManager1());
    }
    
    public void updatePreferences() {
        getUser().save(getEntityManager1());
    }

    public Boolean getDynamicTabView() {
        return dynamicTabView;
    }

    public void setDynamicTabView(Boolean dynamicTabView) {
        this.dynamicTabView = dynamicTabView;
    }

    public EntityManager getEntityManager2() {
        return EMF2.createEntityManager();
    }

    public void updateDashboardTabs(AjaxBehaviorEvent event) {

        switch (event.getComponent().getId()) {
            case "jobManagementAndTrackingUnit":
                dashboard.addTab(getEntityManager1(), "Job Management",
                        getUser().getModules().getJobManagementAndTrackingModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "financialAdminUnit":
                dashboard.addTab(getEntityManager1(), "Financial Administration",
                        getUser().getModules().getFinancialAdminModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "adminUnit":
                dashboard.addTab(getEntityManager1(), "System Administration",
                        getUser().getModules().getAdminModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "complianceUnit":
                dashboard.addTab(getEntityManager1(), "Standards Compliance",
                        getUser().getModules().getComplianceModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "foodsUnit":
                dashboard.addTab(getEntityManager1(), "Foods Inspectorate",
                        getUser().getModules().getFoodsModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "standardsUnit":
                dashboard.addTab(getEntityManager1(), "Standards",
                        getUser().getModules().getStandardsModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "certificationUnit":
                dashboard.addTab(getEntityManager1(), "Certification",
                        getUser().getModules().getCertificationModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "serviceRequestUnit":
                dashboard.addTab(getEntityManager1(), "Service Request",
                        getUser().getModules().getServiceRequestModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "legalOfficeUnit":
                dashboard.addTab(getEntityManager1(), "Document Management",
                        getUser().getModules().getLegalOfficeModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "crmUnit":
                dashboard.addTab(getEntityManager1(), "Client Management",
                        getUser().getModules().getCrmModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            case "legalMetrologyUnit":
                dashboard.addTab(getEntityManager1(), "Legal Metrology",
                        getUser().getModules().getLegalMetrologyModule());
                getUser().getModules().setIsDirty(true);
                getUser().save(getEntityManager1());
                break;
            default:
                break;
        }

    }

    public void closePreferencesDialog2(CloseEvent closeEvent) {
        closePreferencesDialog1(null);
    }

    public void closePreferencesDialog1(ActionEvent actionEvent) {

        PrimeFaces.current().ajax().update("headerForm");
        PrimeFaces.current().executeScript("PF('preferencesDialog').hide();");
    }

    public void sendErrorEmail(String subject, String message) {
        try {
            // send error message to developer's email            
            Utils.postMail(null, null, subject, message);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void doGeneralSearch() {

        switch (getDashboard().getSelectedTabId()) {
            case "Job Management":
                //doJobSearch();
                break;
            case "Job Management3":
                break;
            default:
                break;
        }

    }

    @Override
    public void setIsDirty(Boolean dirty) {
        
    }

    @Override
    public Boolean getIsDirty() {
        return false;
    }

    public EntityManagerFactory setupDatabaseConnection(String PU) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(PU);
            if (emf.isOpen()) {
                return emf;
            } else {
                return null;
            }
        } catch (Exception ex) {
            System.out.println(PU + " Connection failed: " + ex);
            return null;
        }
    }

    public HashMap<String, String> getConnectionProperties(
            String url,
            String driver,
            String username,
            String password) {

        // setup new database connection properties
        HashMap<String, String> prop = new HashMap<>();
        prop.put("javax.persistence.jdbc.user", username);
        prop.put("javax.persistence.jdbc.password", password);
        prop.put("javax.persistence.jdbc.url", url);
        prop.put("javax.persistence.jdbc.driver", driver);

        return prop;
    }

    public Date getCurrentDate() {
        return new Date();
    }

    public void postJobManagerMailToUser(
            Session mailSession,
            JobManagerUser user,
            String subject,
            String message) throws Exception {

        boolean debug = false;
        Message msg;
        EntityManager em = getEntityManager1();

        if (mailSession == null) {
            //Set the host smtp address
            Properties props = new Properties();
            String mailServer = (String) SystemOption.getOptionValueObject(getEntityManager1(), "mail.smtp.host");
            props.put("mail.smtp.host", mailServer);

            // create some properties and get the default Session
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(debug);
            msg = new MimeMessage(session);
        } else {
            msg = new MimeMessage(mailSession);
        }

        // set the from and to address
        String email = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailAddress");
        String name = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailName");
        InternetAddress addressFrom = new InternetAddress(email, name); // option job manager email addres
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[1];
        if (user != null) {
            addressTo[0] = new InternetAddress(user.getUsername(), user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName());
        } else {
            String email1 = (String) SystemOption.getOptionValueObject(em, "administratorEmailAddress");
            String name1 = (String) SystemOption.getOptionValueObject(em, "administratorEmailName");
            addressTo[0] = new InternetAddress(email1, name1);
        }

        msg.setRecipients(Message.RecipientType.TO, addressTo);

        // Setting the Subject and Content Type
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);
    }

    @Override
    public void handleDialogOkButtonClick() {
    }

    @Override
    public void handleDialogYesButtonClick() {

        if (dialogActionHandlerId.equals("unitCostDirty")) {
            PrimeFaces.current().executeScript("PF('unitCostDialog').hide();");
        }

    }

    @Override
    public void handleDialogNoButtonClick() {

        if (dialogActionHandlerId.equals("unitCostDirty")) {
            setIsDirty(false);
            PrimeFaces.current().executeScript("PF('unitCostDialog').hide();");
        }
    }

    @Override
    public DialogActionHandler initDialogActionHandlerId(String id) {
        dialogActionHandlerId = id;
        return this;
    }

    @Override
    public String getDialogActionHandlerId() {
        return dialogActionHandlerId;
    }

    @Override
    public void handleDialogCancelButtonClick() {
    }

    public void openReportsTab() {
        getReportManager().openReportsTab("Job");
    }

    

}
