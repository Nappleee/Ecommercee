package com.ecommerce.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final SecurityProperties securityProperties;
        @Bean
        public SecurityWebFilterChain securityWebFilterChain(
                ServerHttpSecurity http,
                JwtAuthenticationConverter jwtAuthenticationConverter
        ) {
                SecurityProperties.PublicPaths paths = securityProperties.getPublicPaths();

                return http
                        .csrf(ServerHttpSecurity.CsrfSpec::disable)
                        .authorizeExchange(auth -> auth
                                .pathMatchers(paths.getAuth()).permitAll()
                                .pathMatchers(paths.getStorefront()).permitAll()
                                .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(paths.getSwagger()).permitAll()
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .anyExchange().authenticated()
                        )
                        .oauth2ResourceServer(oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                        new ReactiveJwtAuthenticationConverterAdapter(
                                                jwtAuthenticationConverter
                                        )
                                ))
                        )
                        .build();
        }
        @Bean
        public CorsWebFilter corsWebFilter() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(
                        List.of(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
                        )
                );
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return new CorsWebFilter(source);
        }

}
