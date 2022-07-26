package org.mskcc.limsrest.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents iLab request form instances.
 * It's used in promoteBanked.java to parse iLab forms and retrieve its information to input into Teamwork cards and
 * in UpdateFromILabsTask.java to pull iLab data into LIMS.
 * @author Fahimeh Mirhaj
 */
public class CustomForm {
    private String id;
    private String name;
    private String note;
    private Map<String, String> fields;

    public CustomForm() {

    }

    public CustomForm(String id, String name, String note) {
        this.id = id;
        this.name = name;
        this.note = note;
        this.fields = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void addField(String key, String val) {
        this.fields.put(key, val);
    }
}
