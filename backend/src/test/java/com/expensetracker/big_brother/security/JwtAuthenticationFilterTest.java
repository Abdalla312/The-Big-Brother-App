package com.expensetracker.big_brother.security;

import com.expensetracker.big_brother.auth.JwtService;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    @Mock
    JwtService service;
    @Mock
    CustomUserDetailsService userDetailsService;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;
    @InjectMocks
    JwtAuthenticationFilter filter;

    private SecurityContext securityContext;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setup() {
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);

        User u = new User();
        u.setEmail("user@example.com");
        u.setRole(Role.USER);
        u.setTokenVersion(1);
        u.setUserVerified(true);
        userDetails = new CustomUserDetails(u);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void seedExistingAuthentication() {
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("oldPrincipal", null));
    }

    @Test
    void doFilterInternal_NoAuthHeader_PassesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verify(service, never()).extractUserName(anyString());
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_BearerPrefix_PassesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(service.extractUserName("valid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(service.isTokenValid(eq("valid-token"), any(CustomUserDetails.class))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getPrincipal()).isSameAs(userDetails);
        assertThat(securityContext.getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void doFilterInternal_InvalidToken_ClearsContext() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(service.extractUserName("token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(service.isTokenValid(eq("token"), any(CustomUserDetails.class))).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_TokenValidationThrowsException_ClearsContext() throws ServletException, IOException {
        seedExistingAuthentication();
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(service.extractUserName("token")).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_NullEmailFromToken_PassesThrough() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(service.extractUserName("token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertThat(securityContext.getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_AlreadyAuthenticated_DoesNotOverride() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken("existingPrincipal", null);
        securityContext.setAuthentication(existingAuth);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(service.extractUserName("token")).thenReturn("user@example.com");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(service, never()).isTokenValid(anyString(), any(CustomUserDetails.class));
        assertThat(securityContext.getAuthentication()).isSameAs(existingAuth);
    }

    @Test
    void doFilterInternal_UserDetailsLoadFails_ClearsContext() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(service.extractUserName("token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(service, never()).isTokenValid(anyString(), any(CustomUserDetails.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    }
}
