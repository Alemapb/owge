package com.kevinguanchedarias.owgejava.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.FilterEventHandler;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationProvider;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationSuccessHandler;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.RestAuthenticationEntryPoint;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.owgejava.event.ResourceAutoUpdateEventHandler;
import com.kevinguanchedarias.owgejava.filter.BootJwtAuthenticationFilter;
import com.kevinguanchedarias.owgejava.security.AdminTokenConfigLoader;
import com.kevinguanchedarias.owgejava.security.DevelopmentSgtTokenConfigLoader;

@Configuration
@Order(101)
class SecurityBeansConfiguration extends WebSecurityConfigurerAdapter {

	private AuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint();
	private BootJwtAuthenticationFilter adminBootJwtAuthenticationFilter = new BootJwtAuthenticationFilter("/admin/**");
	private BootJwtAuthenticationFilter gameBootJwtAuthenticationFilter = new BootJwtAuthenticationFilter("/game/**");
	private JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider();

	@Bean
	public AuthenticationEntryPoint restAuthenticationEntryPoint() {
		return authenticationEntryPoint;
	}

	@Bean
	public AuthenticationSuccessHandler jwtAuthenticationSuccessHandler() {
		return new JwtAuthenticationSuccessHandler();
	}

	@Bean
	public TokenConfigLoader adminOwgeTokenConfigLoader() {
		return new AdminTokenConfigLoader();
	}

	@Bean
	public TokenConfigLoader gameOwgeTokenConfigLoader() {
		return new DevelopmentSgtTokenConfigLoader();
	}

	@Bean
	public FilterEventHandler owgeResourceAutoUpdateEventHandler() {
		return new ResourceAutoUpdateEventHandler();
	}

	@Bean
	public BootJwtAuthenticationFilter gameBootJwtAuthenticationFilter(
			AuthenticationSuccessHandler authenticationSuccessHandler,
			@Qualifier("gameOwgeTokenConfigLoader") TokenConfigLoader tokenConfigLoader,
			FilterEventHandler filterEventHandler) throws Exception {
		gameBootJwtAuthenticationFilter.setAuthenticationManager(authenticationManager());
		gameBootJwtAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
		gameBootJwtAuthenticationFilter.setTokenConfigLoader(tokenConfigLoader);
		gameBootJwtAuthenticationFilter.setConvertExceptionToJson(true);
		gameBootJwtAuthenticationFilter.setFilterEventHandler(filterEventHandler);
		gameBootJwtAuthenticationFilter
				.setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/game/**")));
		return gameBootJwtAuthenticationFilter;
	}

	@Bean
	public BootJwtAuthenticationFilter adminBootJwtAuthenticationFilter(
			AuthenticationSuccessHandler authenticationSuccessHandler,
			@Qualifier("adminOwgeTokenConfigLoader") TokenConfigLoader tokenConfigLoader) throws Exception {
		adminBootJwtAuthenticationFilter.setAuthenticationManager(authenticationManager());
		adminBootJwtAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
		adminBootJwtAuthenticationFilter.setTokenConfigLoader(tokenConfigLoader);
		adminBootJwtAuthenticationFilter.setConvertExceptionToJson(true);
		adminBootJwtAuthenticationFilter
				.setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/admin/**")));
		return adminBootJwtAuthenticationFilter;
	}

	@Bean
	public FilterRegistrationBean<BootJwtAuthenticationFilter> runLastGameAuth(
			@Qualifier("gameBootJwtAuthenticationFilter") BootJwtAuthenticationFilter filter) {
		FilterRegistrationBean<BootJwtAuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>(
				filter);
		filterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);
		return filterRegistrationBean;
	}

	@Bean
	public FilterRegistrationBean<BootJwtAuthenticationFilter> disableAdmin(
			@Qualifier("adminBootJwtAuthenticationFilter") BootJwtAuthenticationFilter filter) {
		FilterRegistrationBean<BootJwtAuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>(
				filter);
		filterRegistrationBean.setEnabled(false);
		return filterRegistrationBean;
	}

	public AuthenticationEntryPoint getAuthenticationEntryPoint() {
		return authenticationEntryPoint;
	}

	public BootJwtAuthenticationFilter getAdminBootJwtAuthenticationFilter() {
		return adminBootJwtAuthenticationFilter;
	}

	public BootJwtAuthenticationFilter getGameBootJwtAuthenticationFilter() {
		return gameBootJwtAuthenticationFilter;
	}

	public JwtAuthenticationProvider getJwtAuthenticationProvider() {
		return jwtAuthenticationProvider;
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(jwtAuthenticationProvider);
	}
}