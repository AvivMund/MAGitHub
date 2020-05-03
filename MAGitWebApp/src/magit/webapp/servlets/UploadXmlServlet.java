package magit.webapp.servlets;

import magit.engine.Engine;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;

//@WebServlet("/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 , maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class UploadXmlServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String usernameFromSession = SessionUtils.getUsername(request);
        Engine engine = ServletUtils.getEngine(getServletContext());

        Part part = request.getPart("xml_file");
        boolean force = null != request.getParameter("force");
        String message = engine.loadRepoFromXmlStream(usernameFromSession, part.getInputStream(), force);
        String status = "success";
        System.out.println("LoadFromXml: " + message);
        if (!"Repository loaded from xml successfully".equals(message)) {
            status = "danger";
        }
        try (PrintWriter out = response.getWriter()) {
            out.println("{\"status\": \"" + status + "\", \"msg\": \"" + message + "\"}");
            out.flush();
        }
    }
}
