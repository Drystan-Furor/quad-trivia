package quad.solutions.trivia.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(GET, "/", "/questions", "/css/**", "/js/**").permitAll()
						.requestMatchers(POST, "/questions", "/checkanswers").permitAll()
						.anyRequest().denyAll())
				.csrf(Customizer.withDefaults());

		return http.build();
	}

}
