package org.mskcc.limsrest.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        // TODO add the three accounts currently in use
    }
     
    @Override
    protected void configure(HttpSecurity http) throws Exception {
       http.authorizeRequests()
       .antMatchers("/**")
       .authenticated()
       .and().httpBasic();
       http.csrf().disable();
    }
}