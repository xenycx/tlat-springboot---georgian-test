package com.tlat.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class springSecurity {
    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // შესწორება Spring Security 6-სთვის: გამოიყენეთ არაგადავადებული CSRF ტოკენის რეჟიმი
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // null-ს დაყენება უზრუნველყოფს, რომ CSRF ტოკენი აიტვირთოს ყოველ მოთხოვნაზე (არ გადაიდოს)
        requestHandler.setCsrfRequestAttributeName(null);
        
        http
            .authorizeHttpRequests((authorize) -> authorize
                // სტატიკური რესურსები
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/avatars/**").permitAll()
                // საჯარო 
                .requestMatchers("/register/**", "/forgot/**", "/index", "/").permitAll()
                
                // (ADMIN, LECTURER, STUDENT)
                .requestMatchers("/main").hasAnyRole("ADMIN", "LECTURER", "STUDENT")
                .requestMatchers("/lectures").hasAnyRole("ADMIN", "LECTURER", "STUDENT")
                .requestMatchers("/lectures/export/**").hasAnyRole("ADMIN", "LECTURER", "STUDENT")
                .requestMatchers("/resources/download/**").hasAnyRole("ADMIN", "LECTURER", "STUDENT")
                .requestMatchers("/materials/**").hasRole("STUDENT")
                .requestMatchers("/resources/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/rooms").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/rooms/ip", "/rooms/find-by-ip").permitAll()
                .requestMatchers("/lectures/start/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/lectures/stop/**").hasAnyRole("ADMIN", "LECTURER")
                
                // დასწრების სისტემა
                .requestMatchers("/attendance/checkin/**").permitAll()
                .requestMatchers("/attendance/qr-image/**").permitAll()
                .requestMatchers("/attendance/count/**").permitAll()
                .requestMatchers("/attendance/review/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/attendance/manual/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/attendance/regenerate/**").hasAnyRole("ADMIN", "LECTURER")
                
                // მხოლოდ ADMIN-ისთვის
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers("/groups/**").hasRole("ADMIN")
                .requestMatchers("/add/**").hasRole("ADMIN")
                .requestMatchers("/edit/**").hasRole("ADMIN")
                .requestMatchers("/delete/**").hasRole("ADMIN")
                .requestMatchers("/rooms/add/**").hasRole("ADMIN")
                .requestMatchers("/rooms/edit/**").hasRole("ADMIN") 
                .requestMatchers("/rooms/delete/**").hasRole("ADMIN")
                .requestMatchers("/lectures/add/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/lectures/edit/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/lectures/delete/**").hasAnyRole("ADMIN", "LECTURER")
                .requestMatchers("/lectures/import/**").hasAnyRole("ADMIN", "LECTURER")
                
                .anyRequest().authenticated()
            )
            // ლოგინის ფორმა და პარამეტრები
            .formLogin(
                form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/main")
                    .permitAll())
            // ლოგაუთის კონფიგურაცია
            .logout(
                logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .permitAll())
            // CSRF კონფიგურაცია (Cookie-ით, HttpOnly=false რათა ჯავასკრიპტსაც მიუვიდეს)
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler))
            // სესიის მართვა
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .invalidSessionUrl("/login"));
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }
}
