package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Commit;
import magit.engine.Engine;
import magit.engine.RepoManager;
import magit.engine.Repository;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RepositoriesInfoServlet extends HttpServlet {
    public class RepositoryInfo {
        private String name;
        private String activeBranch;
        private int numberOfBranches;
        private String dateOfLastCommit;
        private String lastCommitMessage;

        public RepositoryInfo(Repository repository) {
            name = repository.getRepoName();
            activeBranch = repository.getHead().getName();
            numberOfBranches = repository.getBranches().size();
            Commit newest = repository.getNewestCommit();
            dateOfLastCommit = newest.getDate();
            lastCommitMessage = newest.getMessage();
        }

        public String getName() {
            return name;
        }

        public String getActiveBranch() {
            return activeBranch;
        }

        public int getNumberOfBranches() {
            return numberOfBranches;
        }

        public String getDateOfLastCommit() {
            return dateOfLastCommit;
        }

        public String getLastCommitMessage() {
            return lastCommitMessage;
        }
    }

    public class Repos {
        Collection<RepositoryInfo> myRepos;
        Collection<RepositoryInfo> selectedUserRepos;

        public Repos(Collection<RepositoryInfo> myRepos, Collection<RepositoryInfo> selectedUserRepos) {
            this.myRepos = myRepos;
            this.selectedUserRepos = selectedUserRepos;
        }

        public Collection<RepositoryInfo> getMyRepos() {
            return myRepos;
        }

        public Collection<RepositoryInfo> getSelectedUserRepos() {
            return selectedUserRepos;
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
        String selectedUser = request.getParameter("name");
        List<RepositoryInfo> selectedUserRepos = new ArrayList<>();
        if (selectedUser != null && !selectedUser.isEmpty()) {
            selectedUserRepos = getUserRepos(engine, selectedUser);
        }
        List<RepositoryInfo> myRepos = getUserRepos(engine, usernameFromSession);

        try (PrintWriter out = response.getWriter()) {
            Repos repos = new Repos(myRepos, selectedUserRepos);
            String json = gson.toJson(repos);
            out.println(json);
            out.flush();
        }
    }

    private List<RepositoryInfo> getUserRepos(Engine engine, String username) {
        RepoManager repoManager = engine.getRepoManagerByUsername(username);
        List<RepositoryInfo> repos = new ArrayList<>();
        for (Repository repo : repoManager.getRepositories()) {
            repos.add(new RepositoryInfo(repo));
        }

        return repos;
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
