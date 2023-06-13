package org.mskcc.limsrest;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
@PropertySource({"classpath:/connect.txt", "classpath:/app.properties"})
public class App extends SpringBootServletInitializer {
    private static Log log = LogFactory.getLog(App.class);

    @Autowired
    private Environment env;

    @Value("${dmpRestUrl}")
    private String dmpRestUrl;

    @Value("${oncotreeRestUrl}")
    private String oncotreeRestUrl;

    @Value("#{'${human.recipes}'.split(',')}")
    List<String> humanRecipes;

    private Gateway messagingGateway;
    @Value("${nats.url}")
    private String natsUrl;

    public Gateway messagingGateway() throws Exception {
        messagingGateway.connect(natsUrl);
        log.info("Attempting to connecto to CMO MetaDB NATS server...");
        if (!messagingGateway.isConnected()) {
            log.error("Failed to connect to CMO MetaDB NATS server - messages will not be published");
        } else {
            log.info("CMO MetaDB NATS connection successful");
        }
        return messagingGateway;
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean(destroyMethod = "cleanup")
    public ConnectionPoolLIMS connectionQueue() {
        String host = env.getProperty("lims.host");
        Integer port = Integer.parseInt(env.getProperty("lims.port"));
        String guid = env.getProperty("lims.guid");

        String user1 = env.getProperty("lims.user1");
        String pass1 = env.getProperty("lims.pword1");
        String user2 = env.getProperty("lims.user2");
        String pass2 = env.getProperty("lims.pword2");

        log.info("Creating LIMS connection pool.");
        return new ConnectionPoolLIMS(host, port, guid, user1, pass1, user2, pass2);
    }

    @Bean(destroyMethod = "close")
    public ConnectionLIMS connection() {
        String host = env.getProperty("lims.host");
        Integer port = Integer.parseInt(env.getProperty("lims.port"));
        String guid = env.getProperty("lims.guid");
        String user2 = env.getProperty("lims.user2");
        String pass2 = env.getProperty("lims.pword2");
        log.info("Creating LIMS connection");
        return new ConnectionLIMS(host, port, guid, user2, pass2);
    }
}