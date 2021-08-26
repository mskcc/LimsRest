package org.mskcc.limsrest.service.integrationtest;

import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.RequestSample;
import org.mskcc.limsrest.model.RequestSampleList;
import org.mskcc.limsrest.service.GetRequestSamplesTask;
import org.mskcc.limsrest.service.GetSampleManifestTask;
import org.mskcc.limsrest.model.SampleManifest;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AllWholeExome {
    private static final String connectionFile = "/lims-tango-dev.properties";
    private static final String [] requests = {"04540_J","04835_I","04835_J","04969_M","05257_BX","05257_BY","05395_J","05457_T","05469_AR","05469_AT","05469_AV","05500_GD","05500_GE","05500_GF","05500_GG","05667_CL","05740_L","05816_BV","05816_BY","05841_J","05943_N","05971_Z","06000_FS","06095_C","06230_G","06260_R","06260_S","06287_AO","06287_AP","06287_AR","06287_AU","06960_AM","06960_AO","06960_AQ","07008_CK","07008_CM","07008_CN","07008_CO","07008_CP","07008_CQ","07008_CT","07008_CU","07008_CV","07008_CW","07008_CY","07058_L","07224_AL","07250_AY","07250_BB","07250_BC","07336_P","07336_Q","07366_C","07428_BE","07557_K","07615_AS","07871_T","07973_DA","07973_DD","07973_DF","07973_DH","07973_DL","07973_DN","07973_DP","07973_DR","07973_DU","08015_C","08099_P","08106_E","08106_F","08269_D","08795_D","08822_FB","08822_FC","08822_FD","08822_FE","08822_FF","08822_FG","08822_FH","08822_FI","08822_FJ","08858_F","09088_B","09198_P","09198_Q","09221_D","09242_C","09317_E","09317_F","09335_G","09401_E","09401_F","09401_G","09413_H","09443_R","09443_S","09443_T","09443_U","09443_V","09443_W","09443_X","09455_M","09455_N","09455_O","09483_J","09483_K","09483_M","09483_O","09525_J","09525_K","09525_L","09525_M","09530_D","09537_F","09537_G","09543_D","09546_P","09546_Q","09602_H","09612_F","09625_H","09652_K","09659","09659_B","09670_D","09670_F","09687_E","09687_G","09687_I","09743_C","09743_D","09769_B","09775_D","09791_D","09792_I","09808_B","09841_C","09843_B","09866_D","09868_I","09868_J","09868_K","09906_B","09917_D","09929","09929_B","09955_F","09991_B","09991_C","10011_C","10047","10052_B","10057_B","10058_C","10059_D","10060_B","10065_D","10071","10075","10080_B","10081_B","10091","10093","10094","10096","10097","10099","10099_B","10100_B","10104","10105","10105_B","10106","10108","10110_B","10115","10115_B","10118_B","10119","10120","10120_B","10121_B","10122","10123","10128","10129","10131_D","10132","10137","10139","10142","10150","10151_B","10154","10156","10157","10159","10160","10162","10163","10164","10167","10170","10171_C","10171_D","10171_H","10176","10177","10178","10179_B","10180","10181","10182","10183","10187","10188","10189_B","10194","10195","10196_B","10199_B","10202","10203","10204","10205","10206","10208","10209","10210","10211","10212","10215","10217","10218","10222","10224","10228","10230","10234","10235","10238","10239","10241_B","10241_C","10242","10243","10244","10245","10246","10247","10250","10251","10252","10253","10254","10256","10265","10267","10272","10275","10277","10278","10280","10281","10283","10286","10291","10292","10293","10295","10298","10299","10317","10327"};
    public static class WES {
        public String requestID;
        public Integer samplesIgoComplete = 0;
        public Integer fastqFiles = 0;

        @Override
        public String toString() {
            return "'" + requestID + '\'' +
                    ", " + samplesIgoComplete +
                    ", " + fastqFiles;
        }
    }

    public static void main(String [] args) throws Exception {
        Properties properties = new Properties();
        properties.load(new FileReader(QcStatusAwareProcessAssignerTest.class.getResource(connectionFile).getPath()));
        String host = (String) properties.get("lims.host");
        Integer port = Integer.parseInt((String) properties.get("lims.port"));
        String guid = (String) properties.get("lims.guid");
        String user = (String) properties.get("lims.realuser");
        String pass = (String) properties.get("lims.realpass");
        ConnectionLIMS conn = new ConnectionLIMS(host, port, guid, user, pass);

        try {
            ConcurrentHashMap<String, WES> results = new ConcurrentHashMap<>();

            ExecutorService executor = Executors.newFixedThreadPool(12);
            List<Future> futures = new ArrayList<>();
            for (String request : requests) {
                Future f = executor.submit(new Handler(conn, results, request));
                futures.add(f);
            }

            boolean allDone = false;
            while(!allDone) {
                System.out.println("Checking if all done.");
                for (Future future : futures) {
                    allDone = true;
                    if (!future.isDone())
                        allDone = false;
                }
                for (WES w : results.values()) {
                    System.out.println(w);
                }
                Thread.sleep(20000);
            }

            System.out.println("ALL DONE.");
            for (WES w : results.values()) {
                System.out.println(w);
            }
        } finally {
            System.out.println("Closing connection.");
            conn.close();
        }
    }

    public static class Handler implements Runnable {
        ConnectionLIMS conn;
        ConcurrentHashMap<String, WES> results;
        String request;

        Handler(ConnectionLIMS conn,
                ConcurrentHashMap<String, WES> results,
                String request) {
            this.conn = conn;
            this.results = results;
            this.request = request;
        }

        public void run() {
            getSampleData(conn, results, request);
        }
    }

    protected static void getSampleData(ConnectionLIMS conn,
                                        ConcurrentHashMap<String, WES> results,
                                        String request) {
        System.out.println("Getting Data for request: " + request);
        GetRequestSamplesTask requestSamples = new GetRequestSamplesTask(request, conn);
        RequestSampleList sampleList = requestSamples.execute();

        WES wes = new WES();
        wes.requestID = request;
        //results.put(request, wes);

        for (RequestSample sample : sampleList.getSamples()) {
            if (sample.isIgoComplete()) {
                wes.samplesIgoComplete++;
                String[] igoIds = {sample.getIgoSampleId()};
                GetSampleManifestTask smt = new GetSampleManifestTask(igoIds, conn);
                GetSampleManifestTask.SampleManifestResult smResult = smt.execute();
                if (smResult == null) {

                } else {
                    SampleManifest sm = smResult.smList.get(0);
                    for (SampleManifest.Library library : sm.getLibraries()) {
                        for (SampleManifest.Run run : library.getRuns()) {
                            for (String fastq : run.getFastqs())
                                if (results.containsKey(fastq)) {
                                    System.err.println("DUPLICATE FASTQ: " + fastq + " Sample" + sample);
                                    System.exit(1);
                                } else
                                    results.put(fastq, wes);
                        }
                    }
                }
            }
        }
    }
}