package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Engine;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class CreatePullRequestServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //returning JSON objects, not HTML
        response.setContentType("application/json");
        Gson gson = new Gson();
        String usernameFromSession = SessionUtils.getUsername(request);
        if (usernameFromSession == null) {
            response.setStatus(404);
            return;
        }
        String targetBranch = request.getParameter("targetbranch");
        String baseBranch = request.getParameter("basebranch");
        String prMessage= request.getParameter("prmessage");
        Engine engine = ServletUtils.getEngine(getServletContext());
        String msg = engine.pullRequest(usernameFromSession, targetBranch, baseBranch, prMessage);

        String status = "success";
        System.out.println("Pull Request: " + msg);
        if (!"Pull Request sent successfully!".equals(msg)) {
            status = "danger";
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("{\"status\": \"" + status + "\", \"msg\": \"" + msg + "\"}");
            out.flush();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
