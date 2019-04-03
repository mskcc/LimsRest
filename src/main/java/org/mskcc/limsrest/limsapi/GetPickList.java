package org.mskcc.limsrest.limsapi;


import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.util.VeloxConstants;
import org.mskcc.util.email.EmailNotificator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A queued task that takes a pick list name and returns the possible values
 *
 * @author Aaron Gabow
 */
public class GetPickList extends LimsTask {
    private Log log = LogFactory.getLog(GetPickList.class);

    private String pickListName;
    private boolean notifyOfChanges;

    private List<String> currentRecipes;

    private EmailNotificator emailNotificator;

    private PickListRetriever pickListRetriever;

    public GetPickList(
            PickListRetriever pickListRetriever,
            EmailNotificator emailNotificator,
            List<String> currentRecipes) {
        this.pickListRetriever = pickListRetriever;
        this.emailNotificator = emailNotificator;
        this.currentRecipes = currentRecipes;
    }

    public void init(String picklist, boolean notifyOfChanges) {
        this.pickListName = picklist;
        this.notifyOfChanges = notifyOfChanges;
    }

    @Override
    public Object execute(VeloxConnection conn) {
        List<String> values = pickListRetriever.retrieve(pickListName, dataMgmtServer, user);

        if (notifyOfChanges && VeloxConstants.RECIPE.equals(pickListName)) {
            List<String> valuesCopy = new ArrayList<>(values);
            log.info(String.format("Current recipes in enum (%d): %s", currentRecipes.size(), currentRecipes));

            valuesCopy.removeAll(currentRecipes);
            if (valuesCopy.size() > 0) {
                log.info(String.format("Found %d new values forGePi pick list %s: %s", valuesCopy.size(), pickListName,
                        valuesCopy));
                try {
                    emailNotificator.notifyMessage(pickListName, StringUtils.join(valuesCopy, ","));
                } catch (Exception e) {
                    log.warn("Exception while sending email notification about new recipes in LIMS", e);
                }
            } else {
                log.info(String.format("No new values were added to pick list %s", pickListName));
            }
        }

        if (!values.equals("Exemplar Sample Type")) {
            return values;
        } else {
            String[] blacklist = {"cDNA", "cDNA Library", "Plasma"};
            values.removeAll(Arrays.asList(blacklist));
            return values;
        }
    }
}