package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Branch;
import magit.engine.Engine;
import magit.engine.Repository;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ActiveRepoInfoServlet extends HttpServlet {
    public class ActiveRepoInfo {
        private String myUsername;
        private String repoName;
        private String remoteName;
        private String remoteOwner;
        private BranchInfo head;
        private Collection<BranchInfo> branches;

        public ActiveRepoInfo(String myUsername, String repoName, String remoteName, String remoteOwner, BranchInfo head, Collection<BranchInfo> branches) {
            this.myUsername = myUsername;
            this.repoName = repoName;
            this.remoteName = remoteName;
            this.remoteOwner = remoteOwner;
            this.head = head;
            this.branches = branches;
        }

        public String getRepoName() {
            return repoName;
        }

        public String getRemoteName() {
            return remoteName;
        }

        public String getRemoteOwner() {
            return remoteOwner;
        }

        public BranchInfo getHead() {
            return head;
        }

        public Collection<BranchInfo> getBranches() {
            return branches;
        }

        public String getMyUsername() {
            return myUsername;
        }
    }

    public class BranchInfo {
        private String name;
        private boolean isRTB;

        public BranchInfo(String name, boolean isRTB) {
            this.name = name;
            this.isRTB = isRTB;
        }

        public String getName() {
            return name;
        }

        public boolean isRTB() {
            return isRTB;
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

        String repoName = SessionUtils.getActiveRepoName(request);
        Engine engine = ServletUtils.getEngine(getServletContext());
        Repository repo = engine.getRepoManagerByUsername(usernameFromSession).getCurrentRepo();
        Map<String, Branch> branchMap = repo.getBranches();
        String remoteName = repo.getRemoteRepoName();
        String headName = repo.getHead().getName();
        BranchInfo head = new BranchInfo(headName, branchMap.containsKey(remoteName + File.separator + headName));
        List<BranchInfo> branches = new ArrayList<>();
        for (Branch b: branchMap.values()) {
            if (!b.getName().equals(headName)) {
                boolean isRTB = false;
                if (b.getName().indexOf('\\') < 0) {
                    if (remoteName != null && branchMap.containsKey(remoteName + File.separator + b.getName())) {
                        isRTB = true;
                    }
                }
                branches.add(new BranchInfo(b.getName(), isRTB));
            }
        }
        remoteName = "";
        String remoteOwner = "";
        if (repo.getRemoteRepoName() != null){
            remoteName = repo.getRemoteRepoName();
            remoteOwner = repo.getRemoteRepoLocation().split("\\\\")[2];
        }
        ActiveRepoInfo activeRepoInfo = new ActiveRepoInfo(usernameFromSession, repoName, remoteName, remoteOwner, head, branches);

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(activeRepoInfo);
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
