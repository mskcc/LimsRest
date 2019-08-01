package org.mskcc.limsrest;

import org.mskcc.limsrest.connection.ConnectionQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@EnableSwagger2
@SpringBootApplication // replaces @Configuration @EnableAutoConfiguration & @ComponentScan
@PropertySource({"classpath:/connect.txt", "classpath:/application.properties"})
public class App extends SpringBootServletInitializer {

    public static ConnectionQueue connQueue;

    @Autowired
    private Environment env;

    @Value("${slack.webhookUrl}")
    private String webhookUrl;
    @Value("${slack.channel}")
    private String channel;
    @Value("${slack.user}")
    private String user;
    @Value("${slack.icon}")
    private String icon;

    @Value("${dmpRestUrl}")
    private String dmpRestUrl;

    @Value("${oncotreeRestUrl}")
    private String oncotreeRestUrl;

    @Value("#{'${human.recipes}'.split(',')}")
    private List<String> humanRecipes;


    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @PostConstruct
    public void init() {
        connQueue = connectionQueue();
    }

    @Bean(destroyMethod = "cleanup")
    public ConnectionQueue connectionQueue() {
        String host = env.getProperty("lims.host");
        String port = env.getProperty("lims.port");
        String user = env.getProperty("lims.user");
        String pass = env.getProperty("lims.pword");
        String guid = env.getProperty("lims.guid");
        return new ConnectionQueue(host, port, user, pass, guid);
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(true)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.ant("/api/*"))
                .build();
    }

    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("IGO LIMS REST API")
                .description("IGO LIMS project, request and sample data & metadata.")
                .contact(new Contact("The IGO Data Team", "https://igo.mskcc.org", "zzPDL_SKI_IGO_DATA@mskcc.org"))
                .build();
    }

    @Bean
    // redirect / to swagger-ui.html, other ways to to this too
    public FilterRegistrationBean filterRegistrationBean(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        Filter myFilter = new Filter(){
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if (request.getRequestURI().equals("") || request.getRequestURI().equals("/")){
                    response.sendRedirect("/swagger-ui.html");
                    return;
                }
                filterChain.doFilter(servletRequest, servletResponse);
            }
        };
        filterRegistrationBean.setFilter(myFilter);
        return filterRegistrationBean;
    }
}