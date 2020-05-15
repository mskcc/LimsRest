package org.mskcc.limsrest.util;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.NucleicAcid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Utils {
    private final static Log LOGGER = LogFactory.getLog(Utils.class);

    public static void runAndCatchNpe(Runnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException var2) {
        }
    }

    public static String requireNonNullNorEmpty(String string, String message) {
        if (string != null && !"".equals(string)) {
            return string;
        } else {
            throw new RuntimeException(message);
        }
    }

    public static Optional<NucleicAcid> getOptionalNucleicAcid(String nucleicAcid, String sampleId) {
        try {
            return Optional.of(NucleicAcid.fromValue(nucleicAcid));
        } catch (Exception e) {
            LOGGER.warn(String.format("Nucleic acid for sample %s is empty. For some sample types cmo sample " +
                    "id won't be able to be generated", sampleId));

            return Optional.empty();
        }
    }

    public static <T> ResponseEntity<T> getResponseEntity(T input, HttpStatus status) {
        ResponseEntity<T> resp = new ResponseEntity<T>(input, status);
        return resp;
    }

    /**
     * Method to check if Sequencing for a sample is complete based on presence of SeqAnalysisSampleQC as child record and status of SeqAnalysisSampleQC as Passed.
     * @param sample
     * @return
     */
    public static Boolean isSequencingComplete(DataRecord sample, User user){
        try {
            List<DataRecord> seqAnalysisRecords = Arrays.asList(sample.getChildrenOfType("SeqAnalysisSampleQC", user));
            if (seqAnalysisRecords.size()>0) {
                Object sequencingStatus = seqAnalysisRecords.get(0).getValue("SeqQCStatus", user);
                if (sequencingStatus != null && (sequencingStatus.toString().equalsIgnoreCase("passed") || sequencingStatus.toString().equalsIgnoreCase("failed"))){
                    return true;
                }
            }
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Get a DataField value from a DataRecord.
     *
     * @param record
     * @param fieldName
     * @param fieldType
     * @return Object
     * @throws NotFound
     * @throws RemoteException
     */
    public static Object getValueFromDataRecord(DataRecord record, String fieldName, String fieldType, User user) throws NotFound, RemoteException {
        if (record == null) {
            return "";
        }
        if (record.getValue(fieldName, user) != null) {
            if (fieldType.equals("String")) {
                return record.getStringVal(fieldName, user);
            }
            if (fieldType.equals("Integer")) {
                return record.getIntegerVal(fieldName, user);
            }
            if (fieldType.equals("Long")) {
                return record.getLongVal(fieldName, user);
            }
            if (fieldType.equals("Double")) {
                return record.getDoubleVal(fieldName, user);
            }
            if (fieldType.equals("Date")) {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy");
                return dateFormatter.format(new Date(record.getDateVal(fieldName, user)));
            }
        }
        return "";
    }
}