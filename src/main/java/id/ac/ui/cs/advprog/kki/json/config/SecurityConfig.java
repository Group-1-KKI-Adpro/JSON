package id.ac.ui.cs.advprog.kki.json.config;

import id.ac.ui.cs.advprog.kki.json.auth.security.JwtAuthFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .securityContext(sc ->
                        sc.requireExplicitSave(false)
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * AUTH
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        /*
                         * PUBLIC API
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/**"
                        ).permitAll()

                        /*
                         * H2 CONSOLE
                         */
                        .requestMatchers(
                                "/h2-console/**"
                        ).permitAll()

                        /*
                         * VOUCHERS
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vouchers/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/vouchers/validate"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/vouchers/use"
                        ).authenticated()

                        /*
                         * ADMIN
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/vouchers"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/admin/wallet/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        /*
                         * ORDERS
                         */
                        .requestMatchers(
                                "/api/orders/**"
                        ).permitAll()

                        /*
                         * STATIC PAGES
                         */
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/favicon.ico",
                                "/*.html"
                        ).permitAll()

                        /*
                         * THYMELEAF PAGES
                         */
                        .requestMatchers(
                                "/Transaction/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/wallet",
                                "/transactions"
                        ).permitAll()

                        /*
                         * STATIC ASSETS
                         */
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        /*
                         * ERROR PAGE
                         */
                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        /*
                         * EVERYTHING ELSE
                         */
                        .anyRequest().authenticated()
                )

                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                .formLogin(form ->
                        form.disable()
                );

        /*
         * JWT FILTER
         */
        http.addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
    }
}