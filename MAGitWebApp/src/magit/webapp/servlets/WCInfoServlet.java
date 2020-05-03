package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.engine.ChangeType;
import magit.engine.Engine;
import magit.engine.Item;
import magit.webapp.servlets.viewobjects.ChangesStatusView;
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

public class WCInfoServlet extends HttpServlet {
    public class WCInfo {
        private String myName;
        private ChangesStatusView wcStatus;
        private List<String> wcFiles;
        private String activeRepoName;

        public WCInfo(String myName, ChangesStatusView wcStatus, List<String> wcFiles, String activeRepoName) {
            this.myName = myName;
            this.wcStatus = wcStatus;
            this.wcFiles = wcFiles;
            this.activeRepoName = activeRepoName;
        }

        public String getMyName() {
            return myName;
        }

        public ChangesStatusView getWcStatus() {
            return wcStatus;
        }

        public List<String> getWcFiles() {
            return wcFiles;
        }

        public String getActiveRepoName() {
            return activeRepoName;
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
        String activeRepoName = engine.getRepoManagerByUsername(usernameFromSession).getCurrRepoName();
        List<String> files = engine.getWCFilesList(usernameFromSession);

        Map<ChangeType, List<Item>> changes = engine.getWCStatus(usernameFromSession);
        List<Item> deletedItems = changes.get(ChangeType.DELETED);
        List<Item> updatedItems = changes.get(ChangeType.UPDATED);
        List<Item> createdItems = changes.get(ChangeType.CREATED);
        int beginIndex = Engine.getUserReposLocation(usernameFromSession).length();


        List<String> deleted = convertItemListToStringPathList(beginIndex, deletedItems);
        List<String> updated = convertItemListToStringPathList(beginIndex, updatedItems);
        List<String> created = convertItemListToStringPathList(beginIndex, createdItems);
        ChangesStatusView changesForJson = new ChangesStatusView(deleted, updated, created);

        WCInfo wcInfo = new WCInfo(usernameFromSession, changesForJson, files, activeRepoName);

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(wcInfo);
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

    public static List<String> convertItemListToStringPathList(int beginIndex, List<Item> items) {
        List<String> files = new ArrayList<>();

        for (Item item : items) {
            String relativePath = item.getFullPath().substring(beginIndex);
            files.add(relativePath);
        }

        return files;
    }
}
