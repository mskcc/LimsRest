package org.mskcc.limsrest;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.springframework.beans.factory.config.PropertiesFactoryBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.crypto.password.NoOpPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//
//import java.io.IOException;

//@Configuration
//public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
//    private final Log log = LogFactory.getLog(SecurityConfiguration.class);
//
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(inMemoryUserDetailsManager()).passwordEncoder(passwordEncoder());
//    }
//
//    public PasswordEncoder passwordEncoder() {
//        return NoOpPasswordEncoder.getInstance();
//    }
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        log.info("SecurityConfiguration configure()\n\n");
//        http.authorizeRequests()
//                .antMatchers("/**")
//                .authenticated()
//                .and().httpBasic();
//        http.csrf().disable();
//    }
//
//    @Bean
//    public InMemoryUserDetailsManager inMemoryUserDetailsManager() throws IOException {
//        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
//        propertiesFactoryBean.setLocation(new ClassPathResource("limsrestcredentials.properties"));
//        propertiesFactoryBean.afterPropertiesSet();
//        return new InMemoryUserDetailsManager(propertiesFactoryBean.getObject());
//    }
//}