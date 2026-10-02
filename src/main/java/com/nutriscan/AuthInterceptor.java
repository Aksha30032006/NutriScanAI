package com.nutriscan;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final Jwt jwt;
    private final UserRepo users;

    public AuthInterceptor(Jwt jwt, UserRepo users) { this.jwt = jwt; this.users = users; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if ("OPTIONS".equals(req.getMethod())) return true;
        String path = req.getRequestURI();
        if (path.startsWith("/api/auth/") || ("GET".equals(req.getMethod()) && path.equals("/api/reviews"))) return true;
        try {
            String h = req.getHeader("Authorization");
            String id = jwt.verify(h == null ? "" : h.replace("Bearer ", ""));
            User u = users.findById(id).orElseThrow();
            req.setAttribute("user", u);
            return true;
        } catch (Exception e) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Please log in again\"}");
            return false;
        }
    }
}
