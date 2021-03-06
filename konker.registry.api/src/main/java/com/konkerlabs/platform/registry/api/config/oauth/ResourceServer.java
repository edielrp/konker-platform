package com.konkerlabs.platform.registry.api.config.oauth;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
@EnableResourceServer
public class ResourceServer extends ResourceServerConfigurerAdapter {
	
	public static final String RESOURCE_ID = "registryapi";
	private static final String[] PUBLIC_RESOURCES = new String[]{
			"/oauth/token",
			"/configuration/ui",
			"/swagger-ui.html",
			"/swagger-resources",
			"/swagger-resources/configuration/ui",
			"/swagger-resources/configuration/security",
			"/api/docs",
			"/v2/api-docs"
	};

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
				.antMatchers(PUBLIC_RESOURCES).permitAll()
				.antMatchers("/").access("#oauth2.hasScope('read')")
				.anyRequest().authenticated();
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources)
			throws Exception {
		resources.resourceId(RESOURCE_ID);
	}

}
