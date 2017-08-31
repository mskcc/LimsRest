package org.mskcc.limsrest.limsapi.integrationtest;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxConnectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.BankedSample;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.PromoteBanked;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.SimpleStringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.StringToSampleCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.CellLineCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.formatter.PatientCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.*;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class PromoteBankedTest {
    private static final Log LOG = LogFactory.getLog(PromoteBankedTest.class);
    private static String connectionFile = "src/integration-test/resources/Connection-test.txt";

    private StringToSampleCmoIdConverter stringToCmoSampleConverter = new SimpleStringToSampleCmoIdConverter();
    private PatientSampleCountRetriever patientSampleCountRetriever = new PatientSampleCountRetriever(stringToCmoSampleConverter);
    private CmoSampleIdResolver patientCmoResolver = new PatientCmoSampleIdResolver(patientSampleCountRetriever);
    private PatientCmoSampleIdFormatter patientCmoSampleIdFormatter = new PatientCmoSampleIdFormatter();
    private CellLineCmoSampleIdFormatter cellLineCmoSampleIdFormatter = new CellLineCmoSampleIdFormatter();
    private CmoSampleIdResolver cellLineSmoSampleResolver = new CellLineCmoSampleIdResolver();

    private CmoSampleIdRetriever cellLineCmoSampleRetriever = new CmoSampleIdRetrieverByBankedSample(cellLineSmoSampleResolver, cellLineCmoSampleIdFormatter);
    private CmoSampleIdRetriever patientCmoSampleRetriever = new CmoSampleIdRetrieverByBankedSample(patientCmoResolver, patientCmoSampleIdFormatter);

    private PromoteBanked promoteBanked = new PromoteBanked(patientCmoSampleRetriever, cellLineCmoSampleRetriever);
    private VeloxConnection connection;
    private DataRecordManager dataRecordManager;
    private User user;
    private DataRecord sampleRecord;

    @Before
    public void setUp() throws Exception {
        connection = new VeloxConnection(connectionFile);
        promoteBanked.setVeloxConnection(connection);
        reopenConnection();
        addBankedSampleRecord();
    }

    private void addBankedSampleRecord() throws IoError, NotFound, AlreadyExists, InvalidValue, RemoteException, ServerException {
        sampleRecord = dataRecordManager.addDataRecord(BankedSample.DATA_TYPE_NAME, user);
        dataRecordManager.storeAndCommit(String.format("Added banked sample record: %s", sampleRecord.getRecordId()), user);
        LOG.info(String.format("Added banked sample record: %s", sampleRecord.getRecordId()));
    }

    @After
    public void tearDown() throws Exception {
        deleteBankedSampleRecord();
        connection.close();
        LOG.info("Closed LIMS connection");
    }

    private void deleteBankedSampleRecord() throws ServerException, RemoteException {
        dataRecordManager.deleteDataRecords(Arrays.asList(sampleRecord), null, true, user);
        LOG.info(String.format("Deleted banked sample record: %s", sampleRecord.getRecordId()));
        dataRecordManager.storeAndCommit(String.format("Deleted banked sample record: %s", sampleRecord.getRecordId()), user);
    }

    @Test
    public void whenSampleIsCellLine_shouldSetOtherSampleIdInBankedSampleRecordToCellLineCmoSampleId() throws Exception {
        //given
        String userSampleId = "g748";
        String requestId = "req3";

        Map<String, Object> fields = new HashMap<>();
        fields.put(BankedSample.SPECIMEN_TYPE, SpecimenType.CELLLINE.getValue());
        fields.put(BankedSample.REQUEST_ID, requestId);
        fields.put(BankedSample.USER_SAMPLE_ID, userSampleId);

        sampleRecord.setFields(fields, user);

        String[] bankedIds = new String[1];
        bankedIds[0] = String.valueOf(sampleRecord.getRecordId());

        promoteBanked.init(bankedIds, "NULL", "NULL", "IGO-00248235", "rezae", "false");

        //when
        promoteBanked.call();

        reopenConnection();

        //then
        DataRecord bankedSampleRecord = dataRecordManager.querySystemForRecordId(Long.parseLong(bankedIds[0]), user);

        assertEquals(bankedSampleRecord.getStringVal(BankedSample.OTHER_SAMPLE_ID, user), String.format("%s-%s", userSampleId, requestId));
    }

    private void reopenConnection() throws VeloxConnectionException {
        if (!connection.isConnected()) {
            connection.open();
        }

        dataRecordManager = connection.getDataRecordManager();
        user = connection.getUser();
    }

}