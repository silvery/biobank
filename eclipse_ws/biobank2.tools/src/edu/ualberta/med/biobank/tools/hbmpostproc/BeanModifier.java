package edu.ualberta.med.biobank.tools.hbmpostproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class BeanModifier {

    private static final Logger LOGGER = Logger.getLogger(HbmModifier.class
        .getName());

    private static Pattern BEAN_SERIAL_VERSION_DECL = Pattern
        .compile("private static final long serialVersionUID");

    private static final String LAST_UPDATE_DECL = "        private int version;";

    private static BeanModifier instance = null;

    private BeanModifier() {

    }

    public static BeanModifier getInstance() {
        if (instance == null) {
            instance = new BeanModifier();
        }
        return instance;
    }

    public void alterBean(String filename, String className) throws Exception {
        if (!filename.contains(className)) {
            throw new Exception(
                "Bean file name does not contain class name: filename "
                    + filename + ", classname " + className);
        }

        File outFile = File.createTempFile(className, ".java");

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

        String line = reader.readLine();
        boolean documentChanged = false;

        while (line != null) {
            String alteredLine = new String(line);

            Matcher declMatcher = BEAN_SERIAL_VERSION_DECL.matcher(line);

            if (!documentChanged && declMatcher.find()) {
                alteredLine = new StringBuffer(alteredLine).append("\n\n")
                    .append(LAST_UPDATE_DECL).toString();
            }

            documentChanged |= !line.equals(alteredLine);

            writer.write(alteredLine);
            writer.newLine();
            line = reader.readLine();
        }

        reader.close();
        writer.flush();
        writer.close();

        if (documentChanged) {
            FileUtils.copyFile(outFile, new File(filename));
            if (HbmPostProcess.getInstance().getVerbose()) {
                LOGGER.info("Bean Modified: " + filename);
            }
        }

    }
}