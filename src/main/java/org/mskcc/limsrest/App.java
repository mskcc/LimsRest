package org.mskcc.limsrest;

import java.util.List;

import org.apache.catalina.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@PropertySource({"classpath:/connect.txt", "classpath:/app.properties"})
@EnableSwagger2
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

    @Autowired
    private Gateway messagingGateway;

    @Value("${nats.url}")
    private String natsUrl;

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                connector.setProperty("relaxedQueryChars", "|{}[]");
            }
        });
        return factory;
    }

    @Bean
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

        log.info("Creating LIMS connection pool to host: " + host);
        return new ConnectionPoolLIMS(host, port, guid, user1, pass1);
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

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(true)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("IGO LIMS REST API")
                .description("IGO LIMS project, request and sample data & metadata.")
                .contact(new Contact("The IGO Data Team", "https://igo.mskcc.org", "zzPDL_SKI_IGO_DATA@mskcc.org"))
                .build();
    }
}