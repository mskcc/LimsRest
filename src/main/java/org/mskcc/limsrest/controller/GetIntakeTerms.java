package org.mskcc.limsrest.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.mskcc.limsrest.service.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Queries the SampleIntakeForm table which has a mapping of sample type & headers for a sequencing request type.
 * <BR>
 * Used by the IGO sample submission website.
 */
@RestController
@RequestMapping("/")
public class GetIntakeTerms {
    private static Log log = LogFactory.getLog(GetIntakeTerms.class);
    private final ConnectionLIMS conn;

    public GetIntakeTerms(ConnectionLIMS conn){
        this.conn = conn;
    }

    @GetMapping("/getIntakeTerms")
    public List<List<String>> getContent(@RequestParam(value = "type", defaultValue = "NULL") String type,
                                         @RequestParam(value = "recipe", defaultValue = "NULL") String recipe) {
        List<List<String>> values = new LinkedList<>();
        String decodedRecipe = "";
        try {
            decodedRecipe = URLDecoder.decode(recipe, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {

        }
        if (!Whitelists.textMatches(type)) {
            log.info("FAILURE: type is not using a valid format");
            values.add(Arrays.asList("", "", "FAILURE: type is not using a valid format"));
            return values;
        }
        if (!Whitelists.specialMatches(decodedRecipe)) {
            log.info("FAILURE: recipe is not using a valid format");
            values.add(Arrays.asList("", "", "FAILURE: recipe is not using a valid format"));
            return values;
        }

        log.info("/getIntakeTerms for " + type + " and " + decodedRecipe);
        // database has "Blocks/Slides" client calls endpoint with type="Blocks_PIPI_SLASH_Slides"
        type = type.replaceAll("_PIPI_SLASH_", "/");
        GetIntakeFormDescription task = new GetIntakeFormDescription(type, decodedRecipe, conn);
        return task.execute();
    }
}