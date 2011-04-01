package edu.ualberta.med.biobank.server.logging;

import edu.ualberta.med.biobank.common.peer.LogPeer;
import edu.ualberta.med.biobank.model.Log;

public class MessageGenerator {

    public static String generateStringMessage(String action, String site,
        String patientNumber, String inventoryID, String locationLabel,
        String details, String type) {
        StringBuffer sb = new StringBuffer();
        append(sb, LogPeer.ACTION.getName(), action);
        append(sb, LogPeer.CENTER.getName(), site);
        append(sb, LogPeer.PATIENT_NUMBER.getName(), patientNumber);
        append(sb, LogPeer.INVENTORY_ID.getName(), inventoryID);
        append(sb, LogPeer.LOCATION_LABEL.getName(), locationLabel);
        append(sb, LogPeer.DETAILS.getName(), details);
        append(sb, LogPeer.TYPE.getName(), type);
        return sb.toString();
    }

    public static String generateStringMessage(Log log) {
        return generateStringMessage(log.action, log.center, log.patientNumber,
            log.inventoryId, log.locationLabel, log.details, log.type);
    }

    private static void append(StringBuffer sb, String property, String value) {
        if (value != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(property).append("=").append(value);
        }
    }
}
