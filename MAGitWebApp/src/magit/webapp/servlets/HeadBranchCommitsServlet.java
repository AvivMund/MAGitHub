package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.Branch;
import magit.engine.Commit;
import magit.engine.Engine;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HeadBranchCommitsServlet extends HttpServlet {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

    public class CommitInfo {
        private String sha1;
        private String message;
        private String date;
        private String author;
        private Collection<String> branches = new ArrayList<>();

        public CommitInfo(String sha1, String message, String date, String author) {
            this.sha1 = sha1;
            this.message = message;
            this.date = date;
            this.author = author;
        }

        public void addBranch(String branch) {
            this.branches.add(branch);
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

        public Collection<String> getBranches() {
            return branches;
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
        Collection<Commit> commits = engine.getActiveBranchCommitsHistory(usernameFromSession);
        Map<String, CommitInfo> commitsBySha1 = new HashMap<>();
        for (Commit c : commits) {
            String sha1 = c.calculateSha1();
            commitsBySha1.put(sha1, new CommitInfo(sha1, c.getMessage(), c.getDate(), c.getAuthor()));
        }
        Map<String, Branch> branches = engine.getBranches(usernameFromSession);
        for (Branch b : branches.values()) {
            CommitInfo commitInfo = commitsBySha1.get(b.getCommitSha1());
            if (commitInfo != null) {
                commitInfo.addBranch(b.getName());
            }
        }
        List<CommitInfo> commitsForJson = new ArrayList<>();
        for (CommitInfo ci : commitsBySha1.values()) {
            commitsForJson.add(ci);
        }

        commitsForJson.sort((o1, o2) -> {
            try {
                return dateFormat.parse(o2.getDate()).compareTo(dateFormat.parse(o1.getDate()));
            } catch (ParseException e) {
                e.printStackTrace();
                System.out.println("problem with commit date");
                return 0;
            }
        });


        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(commitsForJson);
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
