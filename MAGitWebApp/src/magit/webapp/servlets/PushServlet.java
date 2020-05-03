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

public class PushServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //returning JSON objects, not HTML

        response.setContentType("application/json");
        String usernameFromSession = SessionUtils.getUsername(request);
        if (usernameFromSession == null) {
            response.setStatus(404);
            return;
        }

        Engine engine = ServletUtils.getEngine(getServletContext());
        String msg = engine.push(usernameFromSession);


        String status = "success";
        System.out.println("Push: " + msg);
        if (!"Push done successfully!".equals(msg)) {
            status = "danger";
        } else {
            String remoteOwner = engine.getRepoManagerByUsername(usernameFromSession).getCurrentRepo().getRemoteRepoLocation().split("\\\\")[2];
            engine.getRepoManagerByUsername(remoteOwner).reloadRepository(engine.getRepoManagerByUsername(usernameFromSession).getCurrRepoName());
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
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
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
