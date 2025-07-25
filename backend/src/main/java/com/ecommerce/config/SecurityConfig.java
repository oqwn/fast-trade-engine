package com.ecommerce.config;

import com.ecommerce.security.JwtAuthenticationFilter;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider, @Lazy UserService userService) {
        return new JwtAuthenticationFilter(tokenProvider, userService);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/verify-email", "/auth/resend-verification").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/forgot-password", "/auth/reset-password").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/files/**").permitAll()
                .requestMatchers("/wishlist-test/**").permitAll()
                
                // Public product browsing (will be added later)
                .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**").permitAll()
                
                // Cart endpoints - mixed authentication (guest and authenticated)
                .requestMatchers(HttpMethod.GET, "/cart", "/cart/count").permitAll()
                .requestMatchers(HttpMethod.POST, "/cart/items").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/cart/items/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/cart/items/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/cart").authenticated()
                .requestMatchers(HttpMethod.POST, "/cart/transfer").authenticated()
                
                // Shipping endpoints
                .requestMatchers(HttpMethod.GET, "/shipping/**").permitAll()
                
                // Store endpoints - specific authenticated endpoints first
                .requestMatchers("/stores/my-store").authenticated()
                .requestMatchers(HttpMethod.POST, "/stores/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/stores/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/stores/**").authenticated()
                // Public store browsing (must come after specific authenticated endpoints)
                .requestMatchers(HttpMethod.GET, "/stores/**").permitAll()
                
                // Admin only endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Seller endpoints
                .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")
                
                // User endpoints (any authenticated user)
                .requestMatchers("/users/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // For H2 Console  
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}