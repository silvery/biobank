package edu.ualberta.med.biobank.reporting;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.AutoText;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.Transparency;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.common.util.Holder;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;

public class ReportingUtils {

    private static final String FILE_URI = "file://"; //$NON-NLS-1$

    private static final String CSV_EXTENSION = ".csv"; //$NON-NLS-1$

    private static final String PDF_EXTENSION = ".pdf"; //$NON-NLS-1$

    public static final String SANSSERIF_TXT = "SansSerif"; //$NON-NLS-1$

    public static Font sansSerif = new Font(Font.MEDIUM, SANSSERIF_TXT, false);

    public static Font sansSerifBold = new Font(Font.MEDIUM, SANSSERIF_TXT,
        true);

    public static final String JASPER_FILE_NAME = "BasicReport.jrxml"; //$NON-NLS-1$

    public static final String JASPER_FILE_EXTENSION = ".jrxml"; //$NON-NLS-1$

    public static PrinterData data;

    /**
     * if userIntegerProperties is set to true, then the map contained inside
     * 'list' should be contain [{0=value}, {1=value}...] instead of
     * [{name=value}...] (see issue #1312)
     */
    public static JasperPrint createDynamicReport(String reportName,
        List<String> description, List<String> columnInfo, List<?> list,
        boolean useIntegerProperties) throws Exception {

        FastReportBuilder drb = new FastReportBuilder();
        for (int i = 0; i < columnInfo.size(); i++) {
            String title = columnInfo.get(i);
            String property = title;
            if (useIntegerProperties)
                property = String.valueOf(i);
            drb.addColumn(title, property, String.class, 40, false)
                .setPrintBackgroundOnOddRows(true).setUseFullPageWidth(true);
        }

        String infos = StringUtils.join(description,
            System.getProperty("line.separator")); //$NON-NLS-1$

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("title", reportName); //$NON-NLS-1$
        fields.put("infos", infos); //$NON-NLS-1$
        URL reportURL = ReportingUtils.class.getResource(JASPER_FILE_NAME);
        if (reportURL == null) {
            throw new Exception(NLS.bind(
                Messages.ReportingUtils_jasperfile_error_msg,
                JASPER_FILE_NAME.replaceAll(JASPER_FILE_EXTENSION, ""))); //$NON-NLS-1$
        }
        drb.setTemplateFile(reportURL.getFile());
        drb.addAutoText(AutoText.AUTOTEXT_PAGE_X_OF_Y,
            AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_RIGHT, 200, 40);
        drb.addAutoText(
            NLS.bind(Messages.ReportingUtils_footer_print_msg,
                DateFormatter.formatAsDateTime(new Date())),
            AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_LEFT, 200);

        Style headerStyle = new Style();
        headerStyle.setFont(ReportingUtils.sansSerifBold);
        // headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        headerStyle.setBorderBottom(Border.THIN);
        headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
        headerStyle.setBackgroundColor(Color.LIGHT_GRAY);
        headerStyle.setTransparency(Transparency.OPAQUE);
        Style detailStyle = new Style();
        detailStyle.setFont(ReportingUtils.sansSerif);
        drb.setDefaultStyles(null, null, headerStyle, detailStyle);

        JRDataSource ds = new JRBeanCollectionDataSource(list);
        JasperPrint jp = DynamicJasperHelper.generateJasperPrint(drb.build(),
            new ClassicLayoutManager(), ds, fields);
        return jp;
    }

    public static JasperPrint createStandardReport(String reportName,
        Map<String, Object> parameters, List<?> list) throws Exception {
        InputStream reportStream = ReportingUtils.class
            .getResourceAsStream(reportName + JASPER_FILE_EXTENSION);
        if (reportStream == null) {
            throw new Exception(NLS.bind(
                Messages.ReportingUtils_jasperfile_error_msg, reportName));
        }
        JasperDesign jdesign = JRXmlLoader.load(reportStream);
        JasperReport report = JasperCompileManager.compileReport(jdesign);
        return JasperFillManager.fillReport(report, parameters,
            new JRBeanCollectionDataSource(list));
    }

    public static void saveReport(JasperPrint jasperPrint, String path)
        throws Exception {
        if (path == null)
            throw new Exception(Messages.ReportingUtils_cancel_error_msg);

        if (path.startsWith(FILE_URI)) {
            path = path.substring(FILE_URI.length());
        }
        if (path.endsWith(PDF_EXTENSION)) {
            JasperExportManager.exportReportToPdfFile(jasperPrint, path);
        } else if (path.endsWith(CSV_EXTENSION)) {
            JRExporter csvExporter = new JRCsvExporter();
            csvExporter.setParameter(JRExporterParameter.JASPER_PRINT,
                jasperPrint);
            csvExporter.setParameter(JRExporterParameter.OUTPUT_FILE, new File(
                path));
            csvExporter.exportReport();
        } else {
            throw new Exception(Messages.ReportingUtils_extension_error_msg);
        }
    }

    private static PrintService getPrinterService(PrinterData data) {
        // use the standard java method to retrieve print services
        PrintService[] services = PrintServiceLookup.lookupPrintServices(
            DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
        PrintService service = null;
        // try to find the correct PrintService using the Swt PrinterData
        // information
        for (PrintService ps : services) {
            if (ps.getName().equals(data.name)) {
                service = ps;
            }
        }
        return service;
    }

    private static void printViaPrinter(PrinterData data,
        JasperPrint jasperPrint) throws Exception {
        PrintService service = getPrinterService(data);
        if (service != null) {
            JRExporter exporter = new JRPrintServiceExporter();
            exporter
                .setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
            exporter.setParameter(
                JRPrintServiceExporterParameter.PRINT_SERVICE_ATTRIBUTE_SET,
                service.getAttributes());
            exporter.setParameter(
                JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG,
                Boolean.FALSE);
            exporter.setParameter(
                JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG,
                Boolean.FALSE);
            try {
                exporter.exportReport();
            } catch (JRException e) {
                throw new Exception(
                    Messages.ReportingUtils_jasper_printing_error_msg);
            }
        } else {
            throw new Exception(NLS.bind(
                Messages.ReportingUtils_printer_error_msg, data.name));
        }
    }

    private static void printViaFile(PrinterData data, JasperPrint jasperPrint)
        throws Exception {
        String fileName = null;

        // data.fileName is typically "FILE:" instead of null.

        if (data.fileName != null && data.fileName.endsWith(PDF_EXTENSION)) {
            fileName = data.fileName;
        } else {
            // on windows a custom dialog is required to print to file.
            FileDialog fd = new FileDialog(PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell(), SWT.SAVE);
            fd.setOverwrite(true);
            fd.setText(Messages.ReportingUtils_pdf_file_msg);
            String[] filterExt = { "*" + PDF_EXTENSION }; //$NON-NLS-1$
            fd.setFilterExtensions(filterExt);
            fd.setFileName(DateFormatter.formatAsDateTime(new Date()));
            final String path = fd.open();
            fileName = path;
        }

        if (fileName == null) {
            return;
        }

        if (fileName.endsWith(PDF_EXTENSION)) {
            if (fileName.startsWith(FILE_URI)) {
                fileName = fileName.substring(FILE_URI.length());
            }
            JasperExportManager.exportReportToPdfFile(jasperPrint, fileName);
        } else {
            throw new Exception(NLS.bind(
                Messages.ReportingUtils_file_type_error_msg, fileName));
        }
    }

    public static void printReport(final JasperPrint jasperPrint)
        throws Exception {
        // Use SWT PrintDialog instead of the JasperReport method that use java
        // swing gui.
        final Display display = Display.getDefault();
        final Holder<Exception> exception = new Holder<Exception>(null);

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = display.getActiveShell();
                PrintDialog dialog = new PrintDialog(shell, SWT.NONE);
                PrinterData data = dialog.open();

                // if data is null : user cancelled print.

                if (data != null) {
                    try {
                        if (data.printToFile == true) {
                            printViaFile(data, jasperPrint);
                        } else {
                            try {
                                printViaPrinter(data, jasperPrint);
                            } catch (Exception e) {
                                BgcPlugin
                                    .openAsyncError(
                                        Messages.ReportingUtils_printing_error_title,
                                        null,
                                        e,
                                        Messages.ReportingUtils_printing_error_msg);
                                printViaFile(data, jasperPrint);
                            }
                        }
                    } catch (Exception e) {
                        exception.setValue(e);
                    }
                }
            }
        });

        if (exception.getValue() != null) {
            throw exception.getValue();
        }
    }
}
