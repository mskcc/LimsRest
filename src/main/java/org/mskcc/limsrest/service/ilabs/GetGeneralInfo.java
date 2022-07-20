package org.mskcc.limsrest.service.ilabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

// CHANGES TO BE MADE FOR DELIVERY UPDATE:
// FASTQ field to Analysis_Type: FastQ only (Raw Data), Analysis by Bioinformatics core (bic), Analysis by Computational Services Core
// all emails => qc_access, data_access
public class GetGeneralInfo {
    private static final String baseUrl = "https://api.ilabsolutions.com/v1/cores";
    private RestTemplate restTemplateIGO;
    private RestTemplate restTemplateCMO;
    private String core_id_igo;
    private String core_id_cmo;
    private static final String PROJECT_NOT_IN_ILABS = "PROJECT NOT IN ILABS";
    private static final String FIELD_NOT_IN_ILABS = "FIELD NOT IN ILABS";
    private static String[] HEADER = {"CC", "FUND", "PHONE", "FASTQ", "ANALYSIS_TYPE", "INVEST", "SUMMARY", "INVESTEMAIL", "ALLEMAILS", "DATA_ACCESS_EMAILS", "QC_ACCESS_EMAILS", "PIEMAIL", "PROJ", "PI", "COUNT", "FAX", "ROOM"};
    private static Map<Integer, String> CORRECTED_PIS = new HashMap<Integer, String>() {{
        put(437751, "geissmaf@mskcc.org,Geissmann");
        put(356959, "pentsove@mskcc.org,Pentsova,Elena");
    }};
    private static Map<Integer, String> INVALID_PIS = new HashMap<Integer, String>() {{
        put(9185, "Leiner");
        put(529891, "Hernandez");
        put(297433, "Ngo");
        put(630887, "Achaibar");
        put(19726, "Leung");
    }};

    public GetGeneralInfo() {
    }

    public GetGeneralInfo(String core_id_igo, String token_igo, String core_id_cmo, String token_cmo) {
        this.restTemplateIGO = restTemplate(token_igo);
        this.restTemplateCMO = restTemplate(token_cmo);
        this.core_id_igo = core_id_igo;
        this.core_id_cmo = core_id_cmo;
    }

    private RestTemplate restTemplate(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(getBearerTokenInterceptor(accessToken));
        return restTemplate;
    }

    private static ClientHttpRequestInterceptor getBearerTokenInterceptor(String accessToken) {
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + accessToken);
            return execution.execute(request, body);
        };
        return interceptor;
    }

    public Map<String, String> getServiceRequest(String srName) {
        if (!srName.matches("^[0-9_A-Z]+$")) {
            throw new IllegalArgumentException("Improper project name: " + srName);
        }

        Map<String, String> formValues = new HashMap<>();
        for (String header : HEADER) {
            formValues.put(header, FIELD_NOT_IN_ILABS);
        }
        formValues.put("CC", "FIELD NOT ACCESSIBLE");
        formValues.put("FUND", "FIELD NOT ACCESSIBLE");

        String url = String.format("%s/%s/service_requests.json?name=%s", baseUrl, core_id_igo, srName);

        ObjectNode res = restTemplateIGO.getForObject(url, ObjectNode.class);
        try {
            JsonNode arrayNode = res.get("ilab_response").get("service_requests");
            if (arrayNode == null || arrayNode.size() != 1) {
                System.out.println("total_service_requests=" + (arrayNode == null ? 0 : arrayNode.size()) + " for " + srName);
                Map<String, String> map = new HashMap<>();
                for (String head : HEADER) map.put(head, PROJECT_NOT_IN_ILABS);
                map.put("CC", "FIELD NOT ACCESSIBLE");
                map.put("FUND", "FIELD NOT ACCESSIBLE");
                printMapping(map);
                return map;
            }

            JsonNode serviceRequest = arrayNode.get(0);
            String serviceRequestId = serviceRequest.get("id").asText();
//          used to determine if request was submitted via IGO or CMP iLab core/space
            String coreInfo = serviceRequest.get("actions").get("show_request").get("url").asText();
            String investigatorEmail = serviceRequest.get("owner").get("email").asText();
            String investigatorFirstName = serviceRequest.get("owner").get("first_name").asText();
            String investigatorLastName = serviceRequest.get("owner").get("last_name").asText();

            investigatorFirstName = StringUtils.capitalize(investigatorFirstName);
            investigatorLastName = StringUtils.capitalize(investigatorLastName);

            formValues.put("INVEST", investigatorFirstName + " " + investigatorLastName);
            formValues.put("INVESTEMAIL", investigatorEmail);

            ArrayNode labPis = (ArrayNode) serviceRequest.get("lab").get("principal_investigators");
            if (!ObjectUtils.isEmpty(labPis)) {
                Integer labId = serviceRequest.get("lab").get("id").asInt();
                parsePrincipalInvestigators(labId, labPis).ifPresent(researcher -> {
                    formValues.put("PI", String.join(" ", researcher.getFirstName(), researcher.getLastName()));
                    formValues.put("PIEMAIL", researcher.getEmail());
                });
            }

            String summary = serviceRequest.get("summary").asText();
            formValues.put("SUMMARY", summary);

            boolean hasMilestone = false;
            boolean hasCustomForm = false;
            boolean hasCharge = false;
            JsonNode serviceRows = serviceRequest.get("service_rows");
            if (!ObjectUtils.isEmpty(serviceRows)) {
                Iterator<JsonNode> iterator = serviceRows.iterator();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    String type = node.get("type").asText();
                    if ("CustomForm".equalsIgnoreCase(type)) {
                        hasCustomForm = true;
                    } else if ("Milestone".equalsIgnoreCase(type)) {
                        hasMilestone = true;
                    } else if ("Charge".equalsIgnoreCase(type)) {
                        hasCharge = true;
                    } else {
                        throw new RuntimeException("Unrecognized service_row type, check to see if API changed: " + type);
                    }
                }
            } else {
                throw new RuntimeException("Could not get service_row for service request id, " +
                        "check to see if API changed: " + srName);
            }

            if (hasCustomForm) {
                List<CustomForm> customForms = new ArrayList<>();
                if (coreInfo.contains(core_id_igo)) {
                    System.out.println("IGO Core submissison.");
                    customForms = parseCustomForms(String.format("%s/%s/service_requests/%s/custom_forms.json", baseUrl, core_id_igo, serviceRequestId), restTemplateIGO);
                } else if (coreInfo.contains(core_id_cmo)) {
                    System.out.println("CMO Core submission.");

                    customForms = parseCustomForms(String.format("%s/%s/service_requests/%s/custom_forms.json", baseUrl, core_id_cmo, serviceRequestId), restTemplateCMO);
                }
                CustomForm customForm = customForms.get(0);
                formValues.put("CUSTOM_FORM", customForm.getId() + "-" + customForm.getName());

                for (Map.Entry<String, String> entry : customForm.getFields().entrySet()) {

                    String lcFormName = entry.getKey().trim();
                    String lcFormValue = entry.getValue();
                    if ("Lab Head:".equalsIgnoreCase(lcFormName) ||
                            "Name of Laboratory head:".equalsIgnoreCase(lcFormName)) {
                        System.out.println("lab_head:" + lcFormValue);
                    } else if ("Lab Head E-mail:".equalsIgnoreCase(lcFormName)) {
                        System.out.println("lab_head_email:" + lcFormValue);
                    } else if ("Contact E-mail:".equalsIgnoreCase(lcFormName) ||
                            "investigator email".equalsIgnoreCase(lcFormName)) {
                        System.out.println("invest_email:" + lcFormValue);
                    } else if ("Phone Number:".equalsIgnoreCase(lcFormName) ||
                            "Telephone #:".equalsIgnoreCase(lcFormName)) {
                        formValues.put("PHONE", lcFormValue);
                    } else if ("Fax Number:".equalsIgnoreCase(lcFormName) || "Fax #:".equalsIgnoreCase(lcFormName)) {
                        formValues.put("FAX", lcFormValue);
                    } else if (lcFormName.toLowerCase().startsWith("room number")
                            || "Room #:".equalsIgnoreCase(lcFormName)) {
                        formValues.put("ROOM", lcFormValue);
                    } else if ("Project Name:".equalsIgnoreCase(lcFormName)) {
                        formValues.put("PROJ", lcFormValue);
                    } else if ("Number of Samples".equalsIgnoreCase(lcFormName) ||
                            "Number of Samples:".equalsIgnoreCase(lcFormName) ||
                            "Number of Samples (n=1):".equalsIgnoreCase(lcFormName) ||
                            "Number of individual libraries:".equalsIgnoreCase(lcFormName)) {
                        formValues.put("COUNT", lcFormValue);
                    } else if (lcFormName.startsWith("All contact e-mails")) {
                        lcFormValue = lcFormValue.trim()
                                .replaceAll("\\r\\n", ",")
                                .replaceAll("\\s+", " ")
                                .replaceAll(";", ",")
                                .replaceAll(", ", ",");
                        formValues.put("ALLEMAILS", lcFormValue);
// NEW FIELDS
                    } else if (lcFormName.toLowerCase().startsWith("qc access")) {
                        lcFormValue = lcFormValue.trim()
                                .replaceAll("\\r\\n", ",")
                                .replaceAll("\\s+", " ")
                                .replaceAll(";", ",")
                                .replaceAll(", ", ",");
                        formValues.put("QC_ACCESS_EMAILS", lcFormValue);
                    } else if (lcFormName.toLowerCase().startsWith("data access")) {
                        lcFormValue = lcFormValue.trim()
                                .replaceAll("\\r\\n", ",")
                                .replaceAll("\\s+", " ")
                                .replaceAll(";", ",")
                                .replaceAll(", ", ",");
                        formValues.put("DATA_ACCESS_EMAILS", lcFormValue);
                    } else if (
                            lcFormName.toLowerCase().startsWith("sequencing data analysis method")) {
                        formValues.put("ANALYSIS_TYPE", lcFormValue);

                    } else if (lcFormName.toLowerCase().startsWith("data delivery method") ||
                            lcFormName.toLowerCase().startsWith("analysis type ")) {
                        formValues.put("FASTQ", lcFormValue);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        printMapping(formValues);
        return formValues;
    }

    private Optional<Researcher> parsePrincipalInvestigators(int labId, ArrayNode labPis) {
        for (int i = 0; i < labPis.size(); i++) {
            int piId = labPis.get(i).get("id").asInt();
            if (INVALID_PIS.containsKey(piId)) {
                System.out.println("Removing invalid pi from principal_investigators list, pi id: " + piId);
                labPis.remove(i);
            }
        }

        if (ObjectUtils.isEmpty(labPis)) return Optional.empty();

        JsonNode labPi = labPis.get(0);
        Researcher researcher = new Researcher();
        String piFirstName = labPi.get("first_name").asText();
        String piLastName = labPi.get("last_name").asText();
        String piEmail = labPi.get("email").asText();

        // check if we should override this data
        if (CORRECTED_PIS.containsKey(labId)) {
            String s = CORRECTED_PIS.get(labId);
            String[] temp = s.split(",");
            piEmail = temp[0];
            piLastName = temp[1];
            if (temp.length > 2) {
                piFirstName = temp[2];
            }
        }

        researcher.setFirstName(piFirstName);
        researcher.setLastName(piLastName);
        researcher.setEmail(piEmail);
        return Optional.of(researcher);
    }

    private List<CustomForm> parseCustomForms(String url, RestTemplate restTemplate) {
        ObjectNode customFormsJson = restTemplate.getForObject(url, ObjectNode.class);
        List<CustomForm> parsedCustomForms = new ArrayList<>();
        JsonNode arrayNode = customFormsJson.get("ilab_response").get("custom_forms");
        if (arrayNode == null || arrayNode.size() == 0) {
            throw new RuntimeException("Could not get custom form, check to see if API changed");
        }

        Iterator<JsonNode> iterator = arrayNode.iterator();
        while (iterator.hasNext()) {
            JsonNode customForm = iterator.next();
            if (ObjectUtils.isEmpty(customForm.get("fields"))) continue;
            String id = customForm.get("id").asText();
            String name = customForm.get("name").asText();
            String note = customForm.get("note").asText();
            CustomForm parsedCustomForm = new CustomForm(id, name, note);
            JsonNode fields = customForm.get("fields");
            fields.forEach(field -> {
                if (field.get("value") != null) {
                    String lcFormName = field.get("name").asText();
//                    System.out.println("parsecustom " + lcFormName);
                    String lcFormValue = "";
                    if (field.get("value").isArray()) {
                        List<String> vals = new ArrayList<>();
                        field.get("value").forEach(jsonNode -> vals.add(jsonNode.asText()));
                        lcFormValue = String.join(";", vals);
                    } else {
                        lcFormValue = field.get("value").asText();
                    }
//                    System.out.println(lcFormValue);
                    parsedCustomForm.addField(lcFormName, lcFormValue);
                }
            });
            parsedCustomForms.add(parsedCustomForm);
        }
        return parsedCustomForms;
    }


    public void printMapping(Map<String, String> ilabValues) {
        for (Map.Entry<String, String> entry : ilabValues.entrySet()) {
            System.out.println(entry.getKey() + ":");
            System.out.println("  " + entry.getValue());
        }
        System.out.println();

    }

    @Deprecated
    public HashMap<String, String> getGeneralInfoByApi(String projectName) throws
            IOException, InterruptedException {
//request_name  ilabs_pi_first_name ilabs_pi_last_name  ilabs_pi_email  ilabs_invest_first_name ilabs_invest_last_name  ilabs_invest_email  form_lab_head form_lab_head_email form_invest_email form_phone_number form_fax_number form_room_number  form_project_name form_sample_number
        Runtime r = Runtime.getRuntime();
        if (!projectName.matches("^[0-9_A-Z]+$")) {
            throw new IOException("Improper project name: " + projectName);
        }
        Process p = r.exec("/opt/common/ruby/ruby-2.1.1/bin/ruby  /home/gabow/pipeline_kickoff/trunk/lib/pull_sr_from_ilabs.rb -e /home/gabow/pipeline_kickoff/trunk/data/invalid_pis.txt  -t /home/gabow/pipeline_kickoff/trunk/config/ilabs.yml -d . -p /home/gabow/pipeline_kickoff/trunk/data/corrected_pi.txt -s " + projectName);
        p.waitFor();
        BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String expectedHeader = "request_name\tilabs_pi_first_name\tilabs_pi_last_name\tilabs_pi_email\tilabs_invest_first_name\tilabs_invest_last_name\tilabs_invest_email\tform_lab_head\tform_lab_head_email\tform_invest_email\tform_phone_number\tform_fax_number\tform_room_number\tform_project_name\tform_sample_number\tform_all_emails\tform_fastq_only\tilabs_summary";

        String header = b.readLine();
        System.out.println(header);
        if (!expectedHeader.equals(header)) {
            throw new IOException("Call not returning expected header:" + header);

        }
        String line = b.readLine();
        String[] generalInfo;
        if (line != null) {
            generalInfo = Splitter.on('\t').splitToList(line).toArray(header.split("\t"));
        } else {
            generalInfo = new String[header.split("\t").length];
        }
        HashMap<String, String> key2val = new HashMap<>();
        System.out.println(line);
        System.out.println(generalInfo.length);
        String requestId = generalInfo[0];
        String emptyValue = "FIELD NOT IN ILABS";
        if (line == null) {
            emptyValue = "PROJECT NOT IN ILABS";
        }
        for (int i = 0; i < generalInfo.length; i++) {
            if (generalInfo[i] == null || generalInfo[i].equals("")) {
                generalInfo[i] = emptyValue;
            }
            System.out.println(i + " " + generalInfo[i]);
        }

        String piName = (generalInfo[1] + " " + generalInfo[2]);
        String investigatorName = (generalInfo[4] + " " + generalInfo[5]);

        key2val.put("PI", piName);
        key2val.put("INVEST", investigatorName);
        key2val.put("ROOM", generalInfo[12]);
        key2val.put("PHONE", generalInfo[10]);
        key2val.put("FAX", generalInfo[11]);
        key2val.put("PROJ", generalInfo[13]);
        key2val.put("PIEMAIL", generalInfo[3]);
        key2val.put("INVESTEMAIL", generalInfo[6]);
        key2val.put("CC", "FIELD NOT ACCESSIBLE");
        key2val.put("FUND", "FIELD NOT ACCESSIBLE");
        key2val.put("COUNT", generalInfo[14]);
        key2val.put("ALLEMAILS", generalInfo[15]);
        key2val.put("FASTQ", generalInfo[16]);
        key2val.put("SUMMARY", generalInfo[17]);
        b.close();
        return key2val;
    }
}