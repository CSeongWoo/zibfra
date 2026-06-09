package com.example.zipfra.security;

import com.example.zipfra.exception.ErrorResponse;
import com.example.zipfra.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        String exception = (String) request.getAttribute("exception");
        int status = HttpServletResponse.SC_UNAUTHORIZED;
        String errorCode = "TOKEN_MISSING";
        String message = "Authorization header is missing or invalid";

        if ("TOKEN_EXPIRED".equals(exception)) {
            status = HttpServletResponse.SC_UNAUTHORIZED;
            errorCode = "TOKEN_EXPIRED";
            message = "Access Token has expired";
        } else if ("TOKEN_INVALID".equals(exception)) {
            status = HttpServletResponse.SC_FORBIDDEN;
            errorCode = "TOKEN_INVALID";
            message = "Signature is invalid or token is malformed";
        } else if ("TOKEN_BLACKLISTED".equals(exception)) {
            status = HttpServletResponse.SC_UNAUTHORIZED;
            errorCode = "TOKEN_BLACKLISTED";
            message = "Token has been blacklisted (logged out)";
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Api-Version", "1");

        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, Instant.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
