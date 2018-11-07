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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.Client;
import jm.com.dpbennett.business.entity.DatePeriod;
import jm.com.dpbennett.business.entity.Department;
import jm.com.dpbennett.business.entity.Employee;
import jm.com.dpbennett.business.entity.Job;
import jm.com.dpbennett.business.entity.JobManagerUser;
import jm.com.dpbennett.business.entity.JobReportItem;
import jm.com.dpbennett.business.entity.JobSample;
import jm.com.dpbennett.business.entity.JobSubCategory;
import jm.com.dpbennett.business.entity.Report;
import jm.com.dpbennett.business.entity.Sector;
import jm.com.dpbennett.business.entity.SystemOption;
import jm.com.dpbennett.business.entity.utils.BusinessEntityUtils;
import jm.com.dpbennett.wal.utils.DatePeriodJobReport;
import jm.com.dpbennett.business.entity.utils.DatePeriodJobReportColumnData;
import jm.com.dpbennett.wal.utils.DateUtils;
import jm.com.dpbennett.wal.utils.ReportUtils;
import jm.com.dpbennett.wal.utils.PrimeFacesUtils;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Desmond Bennett
 */
@Named
@SessionScoped
public class ReportManager implements Serializable {

    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    private String columnsToExclude;
    private Integer longProcessProgress;
    private String reportSearchText;
    private List<Report> foundReports;
    private PurchasingManager purchasingManager;
    private Report selectedReport;
    private Report currentReport;
    private Boolean isActiveReportsOnly;
    private String reportCategory;
    private DatePeriod selectedDatePeriod;
    private Boolean edit;

    /**
     * Creates a new instance of JobManagerBean
     */
    public ReportManager() {
        init();
    }

    /**
     * 
     * @return 
     */
    private EntityManager getLocalEntityManager() {
        Map props = new HashMap();
        props.put("javax.persistence.jdbc.driver",
                (String) SystemOption.getOptionValueObject(getEntityManager1(),
                        "defaultDatabaseDriver"));
        props.put("javax.persistence.jdbc.url",
                (String) SystemOption.getOptionValueObject(getEntityManager1(),
                        "defaultDatabaseURL"));
        props.put("javax.persistence.jdbc.user",
                (String) SystemOption.getOptionValueObject(getEntityManager1(),
                        "defaultDatabaseUsername"));
        props.put("javax.persistence.jdbc.password",
                (String) SystemOption.getOptionValueObject(getEntityManager1(),
                        "defaultDatabasePassword"));

        EntityManagerFactory emf = Persistence
                .createEntityManagerFactory("PU", props);

        return emf.createEntityManager();

    }

    /**
     * 
     * @return 
     */
    public List<SelectItem> getDatePeriods() {
        ArrayList<SelectItem> datePeriods = new ArrayList<>();

        for (String name : DatePeriod.getDatePeriodNames()) {
            datePeriods.add(new SelectItem(name, name));
        }

        return datePeriods;
    }

    /**
     * 
     * @return 
     */
    public DatePeriod getReportingDatePeriod1() {
        if (selectedReport.getDatePeriods().isEmpty()) {
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));
        }

        // Ensure that no date period is null
        if (selectedReport.getDatePeriods().get(0).getStartDate() == null) {
            selectedReport.getDatePeriods().get(0).setStartDate(new Date());
        }
        if (selectedReport.getDatePeriods().get(0).getEndDate() == null) {
            selectedReport.getDatePeriods().get(0).setEndDate(new Date());
        }

        return selectedReport.getDatePeriods().get(0);
    }
     
    /**
     * 
     * @param reportingDatePeriod1 
     */
    public void setReportingDatePeriod1(DatePeriod reportingDatePeriod1) {
        selectedReport.getDatePeriods().set(0, reportingDatePeriod1);
    }

    /**
     * 
     * @return 
     */
    public DatePeriod getReportingDatePeriod2() {

        if (selectedReport.getDatePeriods().isEmpty()) {
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().get(1).setShow(false);

        } else if (selectedReport.getDatePeriods().size() == 1) {

            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().get(1).setShow(false);

        }

        // Ensure that no date period if null
        if (selectedReport.getDatePeriods().get(1).getStartDate() == null) {
            selectedReport.getDatePeriods().get(1).setStartDate(new Date());
        }
        if (selectedReport.getDatePeriods().get(1).getEndDate() == null) {
            selectedReport.getDatePeriods().get(1).setEndDate(new Date());
        }

        return selectedReport.getDatePeriods().get(1);
    }

    public void setReportingDatePeriod2(DatePeriod reportingDatePeriod2) {
        selectedReport.getDatePeriods().set(1, reportingDatePeriod2);
    }

    public DatePeriod getReportingDatePeriod3() {

        if (selectedReport.getDatePeriods().isEmpty()) {
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));
            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().get(2).setShow(false);

        } else if (selectedReport.getDatePeriods().size() == 1) {

            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().get(2).setShow(false);

        } else if (selectedReport.getDatePeriods().size() == 2) {

            selectedReport.getDatePeriods().add(
                    new DatePeriod("This month", "month", null, null, null,
                            null, false, false, true));

            selectedReport.getDatePeriods().get(2).setShow(false);

        }

        // Ensure that no date period is null
        if (selectedReport.getDatePeriods().get(2).getStartDate() == null) {
            selectedReport.getDatePeriods().get(2).setStartDate(new Date());
        }
        if (selectedReport.getDatePeriods().get(2).getEndDate() == null) {
            selectedReport.getDatePeriods().get(2).setEndDate(new Date());
        }

        return selectedReport.getDatePeriods().get(2);
    }

    public void setReportingDatePeriod3(DatePeriod reportingDatePeriod3) {
        selectedReport.getDatePeriods().set(2, reportingDatePeriod3);
    }

    public Boolean getIsInvalidReport() {
        return (getSelectedReport().getId() == null);
    }

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    public DatePeriod getSelectedDatePeriod() {
        return selectedDatePeriod;
    }

    public void setSelectedDatePeriod(DatePeriod selectedDatePeriod) {
        this.selectedDatePeriod = selectedDatePeriod;
    }

    public void setDatePeriodToDelete(DatePeriod selectedDatePeriod) {
        this.selectedDatePeriod = selectedDatePeriod;

        deleteDatePeriod();
    }

    public void openReportsTab(String category) {
        setReportCategory(category);
        getPurchasingManager().getMainTabView().addTab(getEntityManager1(), "Reports", true);
        getPurchasingManager().getMainTabView().select("Reports");
    }

    public List<Report> completeReport(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Report> reports = Report.findActiveReportsByName(em, query);

            return reports;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Report> completeReportByCategory(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Report> reports = Report.findActiveReportsByCategoryAndName(em,
                    getReportCategory(), query);

            return reports;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String getReportCategory() {
        return reportCategory;
    }

    public void setReportCategory(String reportCategory) {
        this.reportCategory = reportCategory;
    }

    public PurchasingManager getPurchasingManager() {
        if (purchasingManager == null) {
            purchasingManager = PurchasingApplication.findBean("purchasingManager");
        }
        return purchasingManager;
    }

    public List getReportCategories() {
        return ReportUtils.getCategories();
    }

    public List getReportMimeTypes() {
        return ReportUtils.getMimeTypes();
    }

    public Boolean getIsActiveReportsOnly() {
        if (isActiveReportsOnly == null) {
            isActiveReportsOnly = true;
        }
        return isActiveReportsOnly;
    }

    public void setIsActiveReportsOnly(Boolean isActiveReportsOnly) {
        this.isActiveReportsOnly = isActiveReportsOnly;
    }

    public List<Report> getFoundReports() {
        if (foundReports == null) {
            foundReports = Report.findAllActiveReports(getEntityManager1());
        }

        return foundReports;
    }

    public String getReportSearchText() {
        return reportSearchText;
    }

    public void setReportSearchText(String reportSearchText) {
        this.reportSearchText = reportSearchText;
    }

    public void doReportSearch() {

        if (getIsActiveReportsOnly()) {
            foundReports = Report.findActiveReports(getEntityManager1(), getReportSearchText());
        } else {
            foundReports = Report.findReports(getEntityManager1(), getReportSearchText());
        }

    }

    public void editReport() {
        PrimeFacesUtils.openDialog(null, "reportTemplateDialog", true, true, true, 600, 750);
    }

    public Report getSelectedReport() {
        if (selectedReport == null) {
            selectedReport = new Report();
        }

        return selectedReport;
    }

    public void setSelectedReport(Report selectedReport) {
        this.selectedReport = selectedReport;
    }

    public Report getCurrentReport() {
        if (currentReport == null) {
            currentReport = new Report();
        }

        return currentReport;
    }

    public void setCurrentReport(Report currentReport) {
        this.currentReport = currentReport;
    }

    public void saveCurrentReport() {

        currentReport.save(getEntityManager1());

        PrimeFaces.current().dialog().closeDynamic(null);
    }

    public void okSelectedDatePeriod(ActionEvent actionEvent) {
        getSelectedDatePeriod().setIsDirty(true);

        if (getIsNewDatePeriod()) {
            currentReport.getDatePeriods().add(selectedDatePeriod);
        }

        closeDialog(actionEvent);
    }

    public void cancelSelectedDatePeriod(ActionEvent actionEvent) {
        getSelectedDatePeriod().setIsDirty(false);

        closeDialog(actionEvent);
    }

    public void closeDialog(ActionEvent actionEvent) {
        PrimeFaces.current().dialog().closeDynamic(null);
    }

    public void createNewReport() {

        currentReport = new Report();

        editReport();
    }

    public void createNewDatePeriod() {

        selectedDatePeriod = new DatePeriod("This month", "month",
                "", null, null, null, false, false, false);
        selectedDatePeriod.initDatePeriod();

        setEdit(false);

        PrimeFacesUtils.openDialog(null, "reportDatePeriodDialog", true, true, true, 0, 400);
    }

    public Boolean getIsNewDatePeriod() {
        return getSelectedDatePeriod().getId() == null && !getEdit();
    }

    public void editDatePeriod() {

        setEdit(true);

        PrimeFacesUtils.openDialog(null, "reportDatePeriodDialog", true, true, true, 0, 400);

    }

    public void deleteDatePeriod() {
        EntityManager em = getEntityManager1();

        if (selectedDatePeriod.getId() != null) {
            DatePeriod datePeriod = DatePeriod.findById(em, selectedDatePeriod.getId());
            if (datePeriod != null) {
                currentReport.getDatePeriods().remove(selectedDatePeriod);
                currentReport.save(em);
                em.getTransaction().begin();
                em.remove(datePeriod);
                em.getTransaction().commit();
            }
        } else {
            currentReport.getDatePeriods().remove(selectedDatePeriod);
        }

    }

    private void init() {
        this.reportSearchText = "";
        this.longProcessProgress = 0;
        this.columnsToExclude = "";
        this.reportCategory = "Job";
    }

    public void reset() {
        init();
    }

    public void closeReportsTab() {
        getPurchasingManager().getMainTabView().addTab(getEntityManager1(), "Reports", false);
    }

    public JobManagerUser getUser() {
        return getPurchasingManager().getUser();
    }

    public EntityManager getEntityManager1() {
        return EMF1.createEntityManager();
    }

    public Employee getReportEmployee1() {
        if (selectedReport.getEmployees().isEmpty()) {
            selectedReport.getEmployees().add(getUser().getEmployee());
        }
        return selectedReport.getEmployees().get(0);
    }

    public void setReportEmployee1(Employee reportEmployee1) {
        selectedReport.getEmployees().set(0, reportEmployee1);
    }

    public Department getReportingDepartment1() {
        if (selectedReport.getDepartments().isEmpty()) {
            selectedReport.getDepartments().add(getUser().getEmployee().getDepartment());
        }
        return selectedReport.getDepartments().get(0);
    }

    public Client getReportingClient1() {
        if (selectedReport.getClients().isEmpty()) {
            selectedReport.getClients().add(new Client(""));
        }
        return selectedReport.getClients().get(0);
    }

    public void setReportingDepartment1(Department reportingDepartment1) {
        selectedReport.getDepartments().set(0, reportingDepartment1);
    }

    public JasperPrint getJasperPrint(Connection con,
            HashMap parameters) {
        JasperPrint jasperPrint = null;
        FileInputStream fis;

        switch (selectedReport.getReportFileMimeType()) {
            case "application/jasper":
                if (getSelectedReport().getUsePackagedReportFileTemplate()) {
                    try {
                        fis = new FileInputStream(getClass().getClassLoader().
                                getResource("/reports/" + selectedReport.getReportFileTemplate()).getFile());

                        jasperPrint = JasperFillManager.fillReport(
                                fis,
                                parameters,
                                con);
                    } catch (FileNotFoundException | JRException e) {
                        System.out.println(e);
                    }

                } else {
                    try {
                        jasperPrint = JasperFillManager.fillReport(
                                selectedReport.getReportFileTemplate(),
                                parameters,
                                con);
                    } catch (JRException e) {
                        System.out.println(e);
                    }
                }

                break;

            case "application/jrxml":
                if (getSelectedReport().getUsePackagedReportFileTemplate()) {
                    try {
                        fis = new FileInputStream(getClass().getClassLoader().
                                getResource("/reports/" + selectedReport.getReportFileTemplate()).getFile());

                        JasperReport jasperReport = JasperCompileManager
                                .compileReport(fis);

                        jasperPrint = JasperFillManager.fillReport(
                                jasperReport,
                                parameters,
                                con);
                    } catch (FileNotFoundException | JRException e) {
                        System.out.println(e);
                    }
                } else {
                    try {
                        JasperReport jasperReport = JasperCompileManager
                                .compileReport(selectedReport.getReportFileTemplate());

                        jasperPrint = JasperFillManager.fillReport(
                                jasperReport,
                                parameters,
                                con);
                    } catch (JRException e) {
                        System.out.println(e);
                    }
                }
                break;

            default:
                break;
        }

        return jasperPrint;
    }

    public StreamedContent getReportStreamedContent() {

        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();
        Connection con;
        JasperPrint print;

        try {

            con = BusinessEntityUtils.establishConnection(
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseDriver"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseURL"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabaseUsername"),
                    (String) SystemOption.getOptionValueObject(em, "defaultDatabasePassword"));

            if (con != null) {
                StreamedContent streamContent;
                byte[] fileBytes;
                
                // Provide reoprt parameters
                parameters.put("reportTitle", selectedReport.getName());

                // Provide date parameters if required
                if (selectedReport.getDatePeriodRequired()) {
                    for (int i = 0; i < selectedReport.getDatePeriods().size(); i++) {
                        parameters.put("dateField" + (i + 1),
                                selectedReport.getDatePeriods().get(i).getDateField());
                        parameters.put("startOfPeriod" + (i + 1),
                                selectedReport.getDatePeriods().get(i).initDatePeriod().getStartDate());
                        parameters.put("endOfPeriod" + (i + 1),
                                selectedReport.getDatePeriods().get(i).initDatePeriod().getEndDate());
                    }
                }
                // Provide employee parameters if required
                if (selectedReport.getEmployeeRequired()) {
                    for (int i = 0; i < selectedReport.getEmployees().size(); i++) {
                        parameters.put("employeeId" + (i + 1),
                                selectedReport.getEmployees().get(i).getId());
                    }
                }
                // Provide department parameters if required
                if (selectedReport.getDepartmentRequired()) {
                    for (int i = 0; i < selectedReport.getDepartments().size(); i++) {
                        parameters.put("departmentId" + (i + 1),
                                selectedReport.getDepartments().get(i).getId());
                        parameters.put("departmentName" + (i + 1),
                                selectedReport.getDepartments().get(i).getName());
                    }
                }
                // Provide client parameters if required
                if (selectedReport.getClientRequired()) {
                    for (int i = 0; i < selectedReport.getClients().size(); i++) {
                        parameters.put("clientId" + (i + 1),
                                selectedReport.getClients().get(i).getId());
                        parameters.put("clientName" + (i + 1),
                                selectedReport.getClients().get(i).getName());
                    }
                }

                print = getJasperPrint(con, parameters);

                switch (selectedReport.getReportOutputFileMimeType()) {
                    case "application/pdf":

                        fileBytes = JasperExportManager.exportReportToPdf(print);

                        streamContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes),
                                selectedReport.getReportOutputFileMimeType(),
                                selectedReport.getReportFile());

                        break;

                    case "application/xlsx":
                    case "application/xls":

                        JRXlsExporter exporterXLS = new JRXlsExporter();
                        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                        exporterXLS.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
                        exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, outStream);
                        exporterXLS.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
                        exporterXLS.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
                        exporterXLS.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
                        exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
                        exporterXLS.exportReport();

                        streamContent = new DefaultStreamedContent(new ByteArrayInputStream(outStream.toByteArray()),
                                selectedReport.getReportOutputFileMimeType(),
                                selectedReport.getReportFile());

                        break;

                    default:
                        fileBytes = JasperExportManager.exportReportToPdf(print);

                        streamContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes),
                                selectedReport.getReportOutputFileMimeType(),
                                selectedReport.getReportFile());
                        break;

                }

                setLongProcessProgress(100);

                return streamContent;

            } else {
                return null;
            }

        } catch (JRException e) {
            System.out.println(e);
            setLongProcessProgress(100);

            return null;
        }

    }

    public StreamedContent getReportFile() {

        StreamedContent reportFile = null;

        try {

            switch (getSelectedReport().getReportFileMimeType()) {
                case "application/jasper":
                case "application/jrxml":
                    reportFile = getReportStreamedContent();
                    break;                
                default:
                    reportFile = getReportStreamedContent();
                    break;
            }

            setLongProcessProgress(100);

        } catch (Exception e) {
            System.out.println(e);
            setLongProcessProgress(100);
        }

        return reportFile;
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

    public List<Report> getJobReports() {
        EntityManager em = getEntityManager1();

        List<Report> reports = Report.findAllReports(em);

        return reports;
    }

    public void updateDepartmentReport() {
    }

    public void updateReportCategory() {
        setSelectedReport(new Report(""));
    }

    public void updateReport() {

    }

    /**
     * 
     * @param wb
     * @return 
     */
    HSSFCellStyle getDefaultCellStyle(HSSFWorkbook wb) {
        HSSFCellStyle cellStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Arial");
        cellStyle.setFont(font);
        cellStyle.setFillForegroundColor(HSSFColor.WHITE.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        return cellStyle;
    }

    /**
     * 
     * @param wb
     * @return 
     */
    Font getWingdingsFont(HSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 14);
        font.setFontName("Wingdings");

        return font;
    }

    /**
     * 
     * @param wb
     * @param fontName
     * @param fontsize
     * @return 
     */
    Font getFont(HSSFWorkbook wb, String fontName, short fontsize) {
        Font font = wb.createFont();
        font.setFontHeightInPoints(fontsize);
        font.setFontName(fontName);

        return font;
    }
   
    /**
     * 
     * @return 
     */
    public ArrayList getDateSearchFields() {
        return DateUtils.getDateSearchFields(getSelectedReport().getCategory());
    }
}
