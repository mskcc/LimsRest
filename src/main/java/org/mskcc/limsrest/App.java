package org.mskcc.limsrest;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.Sample;
import org.mskcc.limsrest.connection.ConnectionQueue;
import org.mskcc.limsrest.limsapi.*;
import org.mskcc.limsrest.limsapi.assignedprocess.AssignedProcessCreator;
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatusAwareProcessAssigner;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.SampleTypeCorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.BankedSampleToCorrectedCmoSampleIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.FormatAwareCorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.SampleToCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.*;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.limsrest.web.*;
import org.mskcc.util.notificator.Notificator;
import org.mskcc.util.notificator.SlackNotificator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

//@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@PropertySource({"classpath:/connect.txt", "classpath:/slack.properties"})
public class App extends SpringBootServletInitializer {
    @Autowired
    private Environment env;

    @Value("${webhookUrl}")
    private String webhookUrl;

    @Value("${channel}")
    private String channel;

    @Value("${user}")
    private String user;

    @Value("${icon}")
    private String icon;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean(destroyMethod = "cleanup")
    public ConnectionQueue connectionQueue() {
        String host = env.getProperty("lims.host");
        String port = env.getProperty("lims.port");
        String user = env.getProperty("lims.user");
        String pword = env.getProperty("lims.pword");
        String guid = env.getProperty("lims.guid");
        return new ConnectionQueue(host, port, user, pword, guid);
    }

    @Bean
    @Scope("request")
    public ListStudies listStudies() {
        return new ListStudies();
    }

    @Bean
    @Scope("request")
    public GetAllStudies getAllStudies() {
        return new GetAllStudies(connectionQueue(), listStudies());
    }

    @Bean
    public SetRequestStatus setRequestStatus() {
        return new SetRequestStatus();
    }

    @Bean
    public SetStatuses setStatuses() {
        return new SetStatuses(connectionQueue(), setRequestStatus());
    }

    @Bean
    @Scope("request")
    public GetSampleQc getQc() {
        return new GetSampleQc();
    }

    @Bean
    @Scope("request")
    public GetProjectQc projectQc() {
        return new GetProjectQc(connectionQueue(), getQc());
    }

    @Bean
    @Scope("request")
    public GetSamples getSamples() {
        return new GetSamples();
    }

    @Bean
    @Scope("request")
    public GetProjectDetails getProjectDetails() {
        return new GetProjectDetails();
    }

    @Bean
    @Scope("request")
    public GetProject getProject() {
        return new GetProject(connectionQueue(), getSamples());
    }

    @Bean
    @Scope("request")
    public GetProjectDetailed getProjectDetailed() {
        return new GetProjectDetailed(connectionQueue(), getProjectDetails());
    }

    @Bean
    @Scope("request")
    public GetPickList getPickList() {
        return new GetPickList();
    }

    @Bean
    @Scope("request")
    public GetPickListValues getPickListValues() {
        return new GetPickListValues(connectionQueue(), getPickList());
    }

    @Bean
    public GetBarcodeInfo getBarcodeInfo() {
        return new GetBarcodeInfo();
    }

    @Bean
    @Scope("request")
    public GetBarcodeList getBarcodeList() {
        return new GetBarcodeList(connectionQueue(), getBarcodeInfo());
    }

    @Bean
    @Scope("request")
    public FindBarcodeSequence findBarcodeSequence() {
        return new FindBarcodeSequence();
    }

    @Bean
    @Scope("request")
    public GetBarcodeSequence getBarcodeSequence() {
        return new GetBarcodeSequence(connectionQueue(), findBarcodeSequence());
    }

    @Bean
    @Scope("request")
    public GetHiseq getHiseq() {
        return new GetHiseq();
    }

    @Bean
    @Scope("request")
    public GetBillingReport getBillingReport() {
        return new GetBillingReport();
    }

    @Bean
    @Scope("request")
    public GetReadyForIllumina getReadyForIllumina() {
        return new GetReadyForIllumina();
    }

    @Bean
    @Scope("request")
    public Report getReport() {
        return new Report(connectionQueue(), getHiseq(), getReadyForIllumina());
    }

    @Bean
    @Scope("request")
    public BillingReport billingReport() {
        return new BillingReport(connectionQueue(), getBillingReport());
    }

    @Bean
    public GetProcessNames getProcessNames() {
        return new GetProcessNames();
    }

    @Bean
    @Scope("request")
    public GetProcesses getProcesses() {
        return new GetProcesses(connectionQueue(), getProcessNames());
    }

    @Bean
    @Scope("request")
    public ToggleSampleQcStatus toggleSampleQcStatus() {
        return new ToggleSampleQcStatus();
    }

    @Bean
    @Scope("request")
    public SetQcStatus setQcStatus() {
        return new SetQcStatus(connectionQueue(), toggleSampleQcStatus());
    }

    @Bean
    @Scope("request")
    public SetOrCreateBanked setOrCreateBanked() {
        return new SetOrCreateBanked();
    }

    @Bean
    @Scope("request")
    public SetBankedSample setBankedSample() {
        return new SetBankedSample(connectionQueue(), setOrCreateBanked());
    }

    @Bean
    @Scope("request")
    public GetBanked getBanked() {
        return new GetBanked();
    }

    @Bean
    @Scope("request")
    public GetBankedSamples getBankedSamples() {
        return new GetBankedSamples(connectionQueue(), getBanked());
    }

    @Bean
    @Scope("request")
    public PromoteBanked promoteBanked() {
        return new PromoteBanked(bankedSampleToCorrectedCmoSampleIdConverter(),
                sampleTypeCorrectedCmoSampleIdGenerator());
    }

    @Bean
    @Scope("request")
    public Notificator slackNotificator() {
        return new SlackNotificator(webhookUrl, channel, user, icon);
    }

    @Bean
    @Scope("request")
    public CorrectedCmoIdConverter<BankedSample> bankedSampleToCorrectedCmoSampleIdConverter() {
        return new BankedSampleToCorrectedCmoSampleIdConverter();
    }

    @Bean
    @Scope("request")
    @Qualifier("patientCmoSampleIdRetriever")
    public CmoSampleIdRetriever patientCmoSampleIdRetriever() {
        return new FormattedCmoSampleIdRetriever(patientCmoSampleIdResolver(), patientCmoSampleIdFormatter());
    }

    @Bean
    @Scope("request")
    @Qualifier("cellLineCmoSampleIdRetriever")
    public CmoSampleIdRetriever cellLineSampleIdRetriever() {
        return new FormattedCmoSampleIdRetriever(cellLineCmoSampleIdResolver(), cellLineCmoSampleIdFormatter());
    }

    @Bean
    @Scope("request")
    public CellLineCmoSampleIdFormatter cellLineCmoSampleIdFormatter() {
        return new CellLineCmoSampleIdFormatter();
    }

    @Bean
    @Scope("request")
    public SampleCounterRetriever patientSampleCountRetriever() {
        return new IncrementalSampleCounterRetriever(stringToPatientCmoIdConverterFactory());
    }

    @Bean
    @Scope("request")
    public FormatAwareCorrectedCmoIdConverterFactory stringToPatientCmoIdConverterFactory() {
        return new FormatAwareCorrectedCmoIdConverterFactory(sampleTypeAbbreviationRetriever());
    }

    @Bean
    @Scope("request")
    public CellLineCmoSampleIdResolver cellLineCmoSampleIdResolver() {
        return new CellLineCmoSampleIdResolver();
    }

    @Bean
    @Scope("request")
    public PatientCmoSampleIdFormatter patientCmoSampleIdFormatter() {
        return new PatientCmoSampleIdFormatter();
    }

    @Bean
    @Scope("request")
    public PatientCmoSampleIdResolver patientCmoSampleIdResolver() {
        return new PatientCmoSampleIdResolver(patientSampleCountRetriever(), sampleTypeAbbreviationRetriever());
    }

    @Bean
    @Scope("request")
    public SampleAbbreviationRetriever sampleTypeAbbreviationRetriever() {
        return new CspaceSampleAbbreviationRetriever();
    }

    @Bean
    @Scope("request")
    public CorrectedCmoSampleIdGenerator sampleTypeCorrectedCmoSampleIdGenerator() {
        return new SampleTypeCorrectedCmoSampleIdGenerator(cmoSampleIdRetrieverFactory(), patientSamplesRetriever(),
                slackNotificator());
    }

    @Bean
    @Scope("request")
    public PatientSamplesRetriever patientSamplesRetriever() {
        return new PatientSamplesWithCmoInfoRetriever(sampleToCorrectedCmoIdConverter(),
                sampleRecordToSampleConverter());
    }

    @Bean
    @Scope("request")
    public CmoSampleIdRetrieverFactory cmoSampleIdRetrieverFactory() {
        return new CmoSampleIdRetrieverFactory(patientCmoSampleIdRetriever(), cellLineSampleIdRetriever());
    }

    @Bean
    @Scope("request")
    public PromoteBankedSample promoteBankedSample() {
        return new PromoteBankedSample(connectionQueue(), promoteBanked());
    }

    @Bean
    @Scope("request")
    public ToggleAutorunnable toggleAutorunnable() {
        return new ToggleAutorunnable();
    }

    @Bean
    @Scope("request")
    public SetAutorunnable setAutorunnable() {
        return new SetAutorunnable(connectionQueue(), toggleAutorunnable());
    }

    @Bean
    @Scope("request")
    public AddOrCreateSet addOrCreateSet() {
        return new AddOrCreateSet();
    }

    @Bean
    @Scope("request")
    public AddSampleSet addSampleSet() {
        return new AddSampleSet(connectionQueue(), addOrCreateSet());
    }

    @Bean
    @Scope("request")
    public GetSet getSet() {
        return new GetSet();
    }

    @Bean
    @Scope("request")
    public GetSampleSet getSampleSet() {
        return new GetSampleSet(connectionQueue(), getSet());
    }

    @Bean
    @Scope("request")
    public AddPoolToLane addPoolToLane() {
        return new AddPoolToLane();
    }

    @Bean
    @Scope("request")
    public AddPoolToFlowcellLane addPoolToFlowcellLane() {
        return new AddPoolToFlowcellLane(connectionQueue(), addPoolToLane());
    }

    @Bean
    @Scope("request")
    public GetProjectHistory getProjectHistory() {
        return new GetProjectHistory();
    }

    @Bean
    @Scope("request")
    public GetTimelines getTimelines() {
        return new GetTimelines(connectionQueue(), getProjectHistory());
    }

    @Bean
    @Scope("request")
    public AddChildSample addChildSample() {
        return new AddChildSample();
    }

    @Bean
    @Scope("request")
    public AddChildAliquotToSample addChildAliquotToSample() {
        return new AddChildAliquotToSample(connectionQueue(), addChildSample());
    }

    @Bean
    @Scope("request")
    public GetPassingSamples getPassingSamples() {
        return new GetPassingSamples();
    }

    @Bean
    @Scope("request")
    public GetPassingSamplesForProject getPassingSamplesForProject() {
        return new GetPassingSamplesForProject(connectionQueue(), getPassingSamples());
    }

    @Bean
    @Scope("request")
    public SetPairing setPairing() {
        return new SetPairing();
    }

    @Bean
    @Scope("request")
    public PairingInfo pairingInfo() {
        return new PairingInfo(connectionQueue(), setPairing());
    }

    @Bean
    @Scope("request")
    public GetCorrectedSampleCmoId getCorrectedSampleCmoId() {
        return new GetCorrectedSampleCmoId(connectionQueue(), generateSampleCmoIdTask());
    }

    @Bean
    @Scope("request")
    public GenerateSampleCmoIdTask generateSampleCmoIdTask() {
        return new GenerateSampleCmoIdTask(sampleTypeCorrectedCmoSampleIdGenerator(), sampleToCorrectedCmoIdConverter
                (), sampleRecordToSampleConverter());
    }

    @Bean
    @Scope("request")
    public SampleRecordToSampleConverter sampleRecordToSampleConverter() {
        return new SampleRecordToSampleConverter();
    }

    @Bean
    @Scope("request")
    public CorrectedCmoIdConverter<Sample> sampleToCorrectedCmoIdConverter() {
        return new SampleToCorrectedCmoIdConverter();
    }

    @Bean
    @Scope("request")
    public GetSetOrReqPairs getSetOrReqPairs() {
        return new GetSetOrReqPairs();
    }

    @Bean
    @Scope("request")
    public GetPairingInfo getPairingInfo() {
        return new GetPairingInfo(connectionQueue(), getSetOrReqPairs());
    }

    @Bean
    @Scope("request")
    public RenameSample renameSample() {
        return new RenameSample();
    }

    @Bean
    @Scope("request")
    public SetSampleName setSampleName() {
        return new SetSampleName(connectionQueue(), renameSample());
    }

    @Bean
    @Scope("request")
    public SetSampleStatus setSampleStatus() {
        return new SetSampleStatus();
    }

    @Bean
    @Scope("request")
    public FixSampleStatus fixSampleStatus() {
        return new FixSampleStatus(connectionQueue(), setSampleStatus());
    }

    @Bean
    @Scope("request")
    public GetIntakeFormDescription getIntakeFormDescription() {
        return new GetIntakeFormDescription();
    }

    @Bean
    @Scope("request")
    public GetIntakeTerms getIntakeTerms() {
        return new GetIntakeTerms(connectionQueue(), getIntakeFormDescription());
    }

    @Bean
    @Scope("request")
    public SetRequest setRequest() {
        return new SetRequest();
    }

    @Bean
    @Scope("request")
    public GetRequest getRequest() {
        return new GetRequest();
    }

    @Bean
    @Scope("request")
    public LimsRequest limsRequest() {
        return new LimsRequest(connectionQueue(), setRequest(), getRequest());
    }

    @Bean
    @Scope("request")
    public GetDelivered getDelivered() {
        return new GetDelivered();
    }

    @Bean
    @Scope("request")
    public GetRecentDeliveries getRecentDeliveries() {
        return new GetRecentDeliveries(connectionQueue(), getDelivered());
    }

    @Bean
    @Scope("request")
    public AddSampleToPool addSampleToPool() {
        return new AddSampleToPool();
    }

    @Bean
    @Scope("request")
    public SwapPools swapPools() {
        return new SwapPools(connectionQueue(), addSampleToPool());
    }

    @Bean
    @Scope("request")
    public DeleteBanked deleteBanked() {
        return new DeleteBanked();
    }

    @Bean
    @Scope("request")
    public DeleteBankedSample deleteBankedSample() {
        return new DeleteBankedSample(connectionQueue(), deleteBanked());
    }

    @Bean
    @Scope("request")
    public QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner() {
        return new QcStatusAwareProcessAssigner(processAssignerFactory(), assignedProcessCreator());
    }

    @Bean
    @Scope("request")
    public AssignedProcessConfigFactory processAssignerFactory() {
        return new AssignedProcessConfigFactory();
    }

    @Bean
    @Scope("request")
    public AssignedProcessCreator assignedProcessCreator() {
        return new AssignedProcessCreator();
    }

    @Bean
    public SecurityConfiguration getSecurityConfiguration() {
        return new SecurityConfiguration();
    }

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        return builder;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(App.class);
    }
}
