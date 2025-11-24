package com.example.demo.user;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /* =================== USERS =================== */
                        // public
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()                      // register
                        .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login/facebook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/photo/**").permitAll()
                        // authenticated
                        .requestMatchers(HttpMethod.POST, "/users/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/logoutAll").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/users").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/users/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH,"/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH,"/users/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/users/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/users/me").authenticated()

                        /* =================== CINEMAS =================== */
                        // authenticated (admin)
                        .requestMatchers(HttpMethod.POST,   "/cinemas").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/cinemas/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/cinemas/*").authenticated()
                        // public
                        .requestMatchers("/cinemas/usermodeling/**").permitAll()
                        .requestMatchers("/cinemas/photo/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cinemas", "/cinemas/*").permitAll()

                        /* =================== FOODS =================== */
                        // authenticated (admin)
                        .requestMatchers(HttpMethod.POST,   "/foods").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/foods/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/foods/*").authenticated()
                        // public
                        .requestMatchers(HttpMethod.GET, "/foods", "/foods/*").permitAll()

                        /* =================== MOVIES =================== */
                        // authenticated (admin)
                        .requestMatchers(HttpMethod.POST,   "/movies").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/movies/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/movies/*").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/movies/photo/*").authenticated()
                        // public
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/*", "/movies/usermodeling/**").permitAll()

                        /* =================== SHOWTIMES =================== */
                        // authenticated (admin)
                        .requestMatchers(HttpMethod.POST,   "/showtimes").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/showtimes/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/showtimes/*").authenticated()
                        // public
                        .requestMatchers(HttpMethod.GET,
                                "/showtimes",
                                "/showtimes/*",
                                "/showtimes/by-movie/**",
                                "/showtimes/by_movie/**"
                        ).permitAll()

                        /* =================== RESERVATIONS =================== */
                        // authenticated (user)
                        .requestMatchers(HttpMethod.POST, "/reservations").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/reservations").authenticated()
                        .requestMatchers(HttpMethod.PATCH,"/reservations/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE,"/reservations/*").authenticated()
                        // public
                        .requestMatchers(HttpMethod.GET,
                                "/reservations/occupied",
                                "/reservations/checkin/*",
                                "/reservations/usermodeling/**",
                                "/reservations/*" // (GET by id)
                        ).permitAll()

                        /* =================== NEWS =================== */
                        // authenticated (admin)
                        .requestMatchers(HttpMethod.POST, "/news").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/news/all").authenticated()
                        .requestMatchers(HttpMethod.PATCH,"/news/*").authenticated()
                        // public
                        .requestMatchers(HttpMethod.GET, "/news/public", "/news/visible", "/news/visible/**").permitAll()

                        /* =================== INVITATIONS =================== */
                        // authenticated (user)
                        .requestMatchers(HttpMethod.POST, "/invitations").authenticated()

                        /* =================== DEFAULT =================== */
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        // Chỉ gắn JwtAuthFilter nếu có bean (tránh lỗi nếu bạn chưa khai báo filter)
        if (jwtAuthFilter != null) {
            http.addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager() { return authentication -> authentication; }

    /* ================= JWT ================= */

    @Component
    public static class JwtService {
        private final SecretKey key;

        public JwtService(
                @Value("${jwt.secret}") String secret
        ) {
            if (secret == null || secret.isBlank()) throw new IllegalArgumentException("Missing jwt.secret");
            byte[] kb = secret.startsWith("base64:")
                    ? Decoders.BASE64.decode(secret.substring(7))
                    : secret.getBytes(StandardCharsets.UTF_8);
            if (kb.length < 32) throw new IllegalArgumentException("jwt.secret must be >= 32 bytes (HS256).");
            this.key = Keys.hmacShaKeyFor(kb);
        }

        // === THAY ĐỔI 1: ký với claim "_id" + "iat" (giống Node), không dùng "sub" ===
        public String sign(String userId) {
            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .claim("_id", userId)
                    .setIssuedAt(new Date(now)) // để có "iat"
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        }

        // === THAY ĐỔI 2: verify & lấy userId từ claim "_id" ===
        public String verifyAndGetUserId(String token) {
            Claims c = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            Object id = c.get("_id");
            if (id == null) throw new JwtException("Missing _id");
            return String.valueOf(id);
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final UserRepository userRepo;

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                chain.doFilter(req, res);
                return;
            }
            String token = auth.substring(7);
            try {
                String userId = jwtService.verifyAndGetUserId(token);
                Optional<User> ou = userRepo.findById(userId);
                if (ou.isEmpty() || !ou.get().getTokens().contains(token)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Please authenticate.\"}");
                    return;
                }
                User u = ou.get();
                req.setAttribute("currentUser", u);
                req.setAttribute("currentToken", token);

                Collection<GrantedAuthority> auths =
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase(Locale.ROOT)));
                var up = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        u.getId(), null, auths);
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(up);
            } catch (JwtException | IllegalArgumentException ex) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Please authenticate.\"}");
                return;
            }
            chain.doFilter(req, res);
        }
    }
}
