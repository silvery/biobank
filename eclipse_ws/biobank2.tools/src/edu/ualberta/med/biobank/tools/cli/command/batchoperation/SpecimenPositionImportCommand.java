package edu.ualberta.med.biobank.tools.cli.command.batchoperation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import edu.ualberta.med.biobank.common.action.batchoperation.specimen.position.PositionBatchOpPojo;
import edu.ualberta.med.biobank.common.action.exception.BatchOpErrorsException;
import edu.ualberta.med.biobank.common.action.exception.BatchOpException;
import edu.ualberta.med.biobank.common.batchoperation.ClientBatchOpErrorsException;
import edu.ualberta.med.biobank.common.batchoperation.IBatchOpPojoReader;
import edu.ualberta.med.biobank.common.batchoperation.specimenPosition.SpecimenPositionBatchOpPojoReader;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.server.applicationservice.BiobankApplicationService;
import edu.ualberta.med.biobank.tools.cli.AppServiceUtils;
import edu.ualberta.med.biobank.tools.cli.CliProvider;
import edu.ualberta.med.biobank.tools.cli.CsvUtils;
import edu.ualberta.med.biobank.tools.cli.command.Command;

public class SpecimenPositionImportCommand extends Command {

    private static final Logger LOG =
        LoggerFactory.getLogger(SpecimenPositionImportCommand.class.getName());

    protected static final String NAME = "specimen_position_import_csv";

    protected static final String HELP = "Used to import specimens from a CSV file.";

    protected static final String USAGE =
        NAME
            + " SITE_NAME_SHORT CSV_FILE\n\n"
            + "Used to assign positions to specimens already in the system.\n\n"
            + "SITE_NAME_SHORT is the short name of the working center you wish to use to import the file from.\n\n"
            + BatchOpUsage.USAGE;

    private Map<String, Site> sites;

    private IBatchOpPojoReader<PositionBatchOpPojo> pojoReader;

    public SpecimenPositionImportCommand(CliProvider cliProvider) {
        super(cliProvider, NAME, HELP, USAGE);
    }

    @Override
    public boolean runCommand(String[] argv) {
        if (argv.length != 3) {
            System.out.println(USAGE);
            return false;
        }

        final String siteName = argv[1];
        final String csvFile = argv[2];

        try {
            BiobankApplicationService appService = cliProvider.getAppService();

            ICsvBeanReader reader = new CsvBeanReader(
                new FileReader(csvFile), CsvPreference.EXCEL_PREFERENCE);

            String[] csvHeaders = reader.getCSVHeader(true);

            if (!SpecimenPositionBatchOpPojoReader.isHeaderValid(csvHeaders)) {
                System.out.println("CSV file has invalid headers");
                return false;
            }

            sites = AppServiceUtils.getSitesByNameShort(appService);
            Site site = sites.get(siteName);

            if (site == null) {
                System.out.println("site is invalid: " + siteName);
                return false;
            }

            pojoReader = new SpecimenPositionBatchOpPojoReader(site, csvFile);

            LOG.info("Reading CSV file: {}", csvFile);
            pojoReader.readPojos(reader);

            LOG.info("Sending specimen import to server");
            appService.doAction(pojoReader.getAction()).getId();
            LOG.info("Specimen import done");
            return true;
        } catch (ClientBatchOpErrorsException e) {
            for (BatchOpException<String> error : e.getErrors()) {
                System.out.println("Error: line " + error.getLineNumber() + " :"
                    + error.getMessage());
            }
        } catch (BatchOpErrorsException e) {
            CsvUtils.showErrorsInLog(e);
        } catch (FileNotFoundException e) {
            System.out.println("File does not exist: " + csvFile);
        } catch (IOException e) {
            System.out.println("Could not parse csv file: " + csvFile);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}
