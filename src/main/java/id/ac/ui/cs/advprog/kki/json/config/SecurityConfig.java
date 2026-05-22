package id.ac.ui.cs.advprog.kki.json.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // Public pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/catalog",
                                "/catalog.html",
                                "/catalog/new"
                        ).permitAll()

                        // Catalog API
                        .requestMatchers(HttpMethod.GET, "/api/catalog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/catalog").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/**").authenticated()

                        .requestMatchers(HttpMethod.PUT, "/api/catalog/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/catalog/**").authenticated()

                        // Everything else
                        .anyRequest().authenticated()
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}