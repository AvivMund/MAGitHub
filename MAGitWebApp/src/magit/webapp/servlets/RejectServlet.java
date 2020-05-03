package magit.webapp.servlets;

import magit.engine.Engine;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class RejectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String usernameFromSession = SessionUtils.getUsername(request);
        Engine engine = ServletUtils.getEngine(getServletContext());

        String reason = request.getParameter("reason");
        int id = Integer.parseInt(request.getParameter("id"));
        String status = "success";
        String message = "Reject sent successfully!";
        if (id == -1) {
            message = "ERROR: there is no selected pull request!";
            status = "danger";
        } else {
            engine.rejectPR(reason, usernameFromSession, id);
        }
        System.out.println("Reject pull request: " + message);

        try (PrintWriter out = response.getWriter()) {
            out.println("{\"status\": \"" + status + "\", \"msg\": \"" + message + "\"}");
            out.flush();
        }
    }
}
