package com.dalvi.auth;

import com.dalvi.WebVoiceChatPlugin;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AuthStatusServlet extends HttpServlet {
    private final WebVoiceChatPlugin plugin;

    public AuthStatusServlet(WebVoiceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println("{\"authRequired\":" + plugin.isAuthRequired() + "}");
    }
}
