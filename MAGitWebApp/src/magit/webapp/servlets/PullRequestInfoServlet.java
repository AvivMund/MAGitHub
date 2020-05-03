package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Engine;
import magit.engine.RepoManager;
import magit.webapp.pr.PRManager;
import magit.webapp.pr.PullRequest;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PullRequestInfoServlet extends HttpServlet {
    public class PRInfo {
        private String requestUsername;
        private String targetBranch;
        private String baseBranch;
        private String date;
        private String prStatus;
        private int id;

        public PRInfo(String requestUsername, String targetBranch, String baseBranch, String date, String prStatus, int id) {
            this.requestUsername = requestUsername;
            this.targetBranch = targetBranch;
            this.baseBranch = baseBranch;
            this.date = date;
            this.prStatus = prStatus;
            this.id = id;
        }

        public String getRequestUsername() {
            return requestUsername;
        }

        public String getTargetBranch() {
            return targetBranch;
        }

        public String getBaseBranch() {
            return baseBranch;
        }

        public String getDate() {
            return date;
        }

        public String getPrStatus() {
            return prStatus;
        }

        public int getId() {
            return id;
        }
    }

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

        Engine engine = ServletUtils.getEngine(getServletContext());
        RepoManager repoManager = engine.getRepoManagerByUsername(usernameFromSession);
        String activeRepoName = repoManager.getCurrRepoName();
        List<PRInfo> prsForJson = new ArrayList<>();

        PRManager prManager = ServletUtils.getPrManager(getServletContext());
        Map<Integer, PullRequest> prs = prManager.getPrsByUsername(usernameFromSession);
        if(prs != null) {
            for (PullRequest pr : prs.values()) {
                if (pr.getRepoName().equals(activeRepoName)) {
                    PRInfo prInfo = new PRInfo(pr.getRequestUsername(), pr.getTargetBranch(), pr.getBaseBranch(), pr.getDate(), pr.getStatus().toString(), pr.getId());
                    prsForJson.add(prInfo);
                }
            }
        }

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(prsForJson);
            out.println(json);
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
