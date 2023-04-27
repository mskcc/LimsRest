package org.mskcc.limsrest.service.ilabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.mskcc.limsrest.service.CustomForm;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

// CHANGES TO BE MADE FOR DELIVERY UPDATE:
// FASTQ field to Analysis_Type: FastQ only (Raw Data), Analysis by Bioinformatics core (bic), Analysis by Computational Services Core
// all emails => qc_access, data_access
public class GetGeneralInfo {
    private static final Log log = LogFactory.getLog(GetGeneralInfo.class);
    private static final String baseUrl = "https://api.ilabsolutions.com/v1/cores";
    private RestTemplate restTemplateIGO;
    private RestTemplate restTemplateCMO;
    private String core_id_igo;
    private String core_id_cmo;
    private static final String PROJECT_NOT_IN_ILABS = "PROJECT NOT IN ILABS";
    private static final String FIELD_NOT_IN_ILABS = "FIELD NOT IN ILABS";
    private static String[] HEADER = {"CC", "FUND", "PHONE", "FASTQ", "ILAB_SERVICE_REQUEST_ID", "ANALYSIS_TYPE", "INVEST", "SUMMARY", "INVESTEMAIL", "ALLEMAILS", "DATA_ACCESS_EMAILS", "QC_ACCESS_EMAILS", "PIEMAIL", "PROJ", "PI", "COUNT", "FAX", "ROOM"};
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

            formValues.put("ILAB_SERVICE_REQUEST_ID", serviceRequestId);
            System.out.println("iLab service request id has been published! " + serviceRequestId);

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
//                boolean tenXForm = false;
//                if (customForm.getName().contains("Single Cell Library Prep + Sequencing (Human or Mouse)")) {
//                    tenXForm = true;
//                }

                for (Map.Entry<String, String> entry : customForm.getFields().entrySet()) {
                    String lcFormName = entry.getKey().trim();
                    String lcFormValue = entry.getValue();

                    //if (tenXForm) {
                        if (lcFormName.trim().toLowerCase().contains("barcoded antibodies")) {
                            formValues.put("BARCODED_ANTIBODIES", lcFormValue);
                        }
                        else if (lcFormName.trim().toLowerCase().contains("total seq c antibodies")) {
                            formValues.put("SEQC_ANTIBODIES", lcFormValue);
                        }
                        else if (lcFormName.trim().toLowerCase().contains("treatment")) {
                            formValues.put("TREATMENT", lcFormValue);
                        }
                        else if (lcFormName.trim().toLowerCase().contains("additional vdj")) {
                            formValues.put("ADDITIONAL_VDJ", lcFormValue);
                        }
                        else if (lcFormName.trim().toLowerCase().contains("types of cells")) {
                            formValues.put("CELL_TYPES", lcFormValue);
                        }
                    //}

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
}