package org.mskcc.limsrest.limsapi.cmoinfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.apache.log4j.Logger;
import org.mskcc.domain.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.PatientSamplesRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetrieverFactory;
import org.mskcc.util.CommonUtils;
import org.mskcc.util.notificator.Notificator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.mskcc.domain.sample.SpecimenType.CELLLINE;

public class SampleTypeCorrectedCmoSampleIdGenerator implements CorrectedCmoSampleIdGenerator {
    private static final Logger LOGGER = Logger.getLogger(SampleTypeCorrectedCmoSampleIdGenerator.class);

    private final CmoSampleIdRetrieverFactory cmoSampleIdRetrieverFactory;
    private final Multimap<String, CorrectedCmoSampleView> generatedSamples = HashMultimap.create();
    private final PatientSamplesRetriever patientSamplesRetriever;
    private final Notificator notificator;

    public SampleTypeCorrectedCmoSampleIdGenerator(CmoSampleIdRetrieverFactory cmoSampleIdRetrieverFactory,
                                                   PatientSamplesRetriever patientSamplesRetriever,
                                                   Notificator notificator) {
        this.cmoSampleIdRetrieverFactory = cmoSampleIdRetrieverFactory;
        this.patientSamplesRetriever = patientSamplesRetriever;
        this.notificator = notificator;
    }

    @Override
    public synchronized String generate(CorrectedCmoSampleView correctedCmoSampleView, String requestId,
                                        DataRecordManager dataRecordManager, User user) throws LimsException {
        try {
            String patientId = correctedCmoSampleView.getPatientId();
            CommonUtils.requireNonNullNorEmpty(patientId, String.format("Patient id is not set for sample: %s",
                    correctedCmoSampleView.getId()));

            List<CorrectedCmoSampleView> cmoSampleViews = patientSamplesRetriever.retrieve(patientId,
                    dataRecordManager, user);

            CmoSampleIdRetriever cmoSampleIdRetriever = cmoSampleIdRetrieverFactory.getCmoSampleIdRetriever
                    (correctedCmoSampleView);

            cmoSampleViews.addAll(generatedSamples.get(patientId));
            String cmoSampleId = cmoSampleIdRetriever.retrieve(correctedCmoSampleView, getAllNonCellLineViews
                    (cmoSampleViews), requestId);

            correctedCmoSampleView.setCorrectedCmoId(cmoSampleId);
            generatedSamples.put(patientId, correctedCmoSampleView);

            return cmoSampleId;
        } catch (LimsException e) {
            notifyAboutCorrectedCmoIdFailure(correctedCmoSampleView, requestId, e);
            throw e;
        } catch (Exception e) {
            notifyAboutCorrectedCmoIdFailure(correctedCmoSampleView, requestId, e);
            throw new LimsException(e.getMessage(), e);
        }
    }

    private List<CorrectedCmoSampleView> getAllNonCellLineViews(List<CorrectedCmoSampleView> cmoSampleViews) {
        return cmoSampleViews.stream()
                .filter(v -> v.getSpecimenType() != CELLLINE)
                .collect(Collectors.toList());
    }

    private void notifyAboutCorrectedCmoIdFailure(CorrectedCmoSampleView correctedCmoSampleView, String requestId,
                                                  Exception exception) {
        String message = String.format(":-1: Corrected cmo id autogeneration failed for sample: *%s (%s)* :-1: \n " +
                        "Cause: %s",
                correctedCmoSampleView.getSampleId(), correctedCmoSampleView.getId(), getStackTraceAsString(exception));

        try {
            notificator.notifyMessage(requestId, message);
        } catch (Exception e) {
            LOGGER.warn(String.format("Sending notification about failure to autogenerate corrected cmo id failed for" +
                    " sample: %s", correctedCmoSampleView.getId()));
        }
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
