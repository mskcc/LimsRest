package org.mskcc.limsrest.controller.external;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.model.LeftoverMaterial;
import org.mskcc.limsrest.service.GetLeftoverMaterialTask;
import org.mskcc.limsrest.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

/**
 *
 */
@RestController
@RequestMapping("/")
public class GetLeftoverMaterial {
    private static Log log = LogFactory.getLog(GetLeftoverMaterial.class);
    private ConnectionLIMS conn;

    public GetLeftoverMaterial(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/api/getLeftoverMaterial")
    public LeftoverMaterial getContent(@RequestParam(value = "igoid") String igoid,
                                       HttpServletRequest request) throws IoError, ServerException, RemoteException, NotFound {
        log.info("Starting /getLeftoverMaterial?igoid=" + igoid + " client IP:" + request.getRemoteAddr());

        String baseIgoId = Utils.getBaseSampleId(igoid);
        if (baseIgoId.length() < 5) {
            log.error("FAILURE: invalid IGO ID - " + igoid);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid IGO ID");
        }
        GetLeftoverMaterialTask t = new GetLeftoverMaterialTask(baseIgoId, conn);
        return (LeftoverMaterial) t.execute();
    }
}