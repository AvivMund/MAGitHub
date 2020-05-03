package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Commit;
import magit.engine.Engine;
import magit.engine.RepoManager;
import magit.webapp.pr.PullRequest;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PrInfoServlet extends HttpServlet {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

    public class PrInfo {
        private String myUsername;
        private Collection<CommitInfoPR> commits;

        public PrInfo(String myUsername, Collection<CommitInfoPR> commits) {
            this.myUsername = myUsername;
            this.commits = commits;
        }

        public String getMyUsername() {
            return myUsername;
        }

        public Collection<CommitInfoPR> getCommits() {
            return commits;
        }
    }

    public class CommitInfoPR {
        private String sha1;
        private String message;
        private String date;
        private String author;
        private String prevCommitSha1;

        public CommitInfoPR(String sha1, String message, String date, String author, String prevCommitSha1) {
            this.sha1 = sha1;
            this.message = message;
            this.date = date;
            this.author = author;
            this.prevCommitSha1 = prevCommitSha1;
        }

        public String getSha1() {
            return sha1;
        }

        public String getMessage() {
            return message;
        }

        public String getDate() {
            return date;
        }

        public String getAuthor() {
            return author;
        }

        public String getPrevCommitSha1() {
            return prevCommitSha1;
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

        Integer prId = SessionUtils.getPrId(request);
        Engine engine = ServletUtils.getEngine(getServletContext());
        PullRequest pr = engine.getPrManager().getPrsByUsername(usernameFromSession).get(prId);
        String targetBranchName = pr.getTargetBranch();
        String baseBranchName = pr.getBaseBranch();
        RepoManager repoManager = engine.getRepoManagerByUsername(usernameFromSession);
        String commitTargetSha1 = repoManager.getCurrentRepo().getBranches().get(targetBranchName).getCommitSha1();
        String commitBaseSha1 = repoManager.getCurrentRepo().getBranches().get(baseBranchName).getCommitSha1();

        Collection<Commit> commits = engine.getPrevCommits(usernameFromSession, commitTargetSha1);

        List<CommitInfoPR> commitsForJson = new ArrayList<>();
        for (Commit c : commits) {
            String sha1 = c.calculateSha1();
            if (sha1.equals(commitBaseSha1)) {
                break;
            }
            CommitInfoPR ci = new CommitInfoPR(sha1, c.getMessage(), c.getDate(), c.getAuthor(), c.getPreviousCommit1Sha1());
            commitsForJson.add(ci);
        }

        PrInfo prInfo = new PrInfo(usernameFromSession, commitsForJson);

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(prInfo);
            out.println(json);
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
