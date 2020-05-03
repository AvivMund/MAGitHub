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
import java.util.List;
import java.util.Map;

import static magit.webapp.servlets.WCInfoServlet.convertItemListToStringPathList;

public class SaveFileContentServlet extends HttpServlet {
    public class SaveInfo {
        private ChangesStatusView wcStatus;
        private String msg;
        private String status;

        public SaveInfo(ChangesStatusView wcStatus, String msg, String status) {
            this.wcStatus = wcStatus;
            this.msg = msg;
            this.status = status;
        }

        public ChangesStatusView getWcStatus() {
            return wcStatus;
        }

        public String getMsg() {
            return msg;
        }

        public String getStatus() {
            return status;
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
        String fileName = request.getParameter("filename");
        String fileContent = request.getParameter("filecontent");

        //save file
        String msg = engine.saveFileToWC(usernameFromSession, fileName, fileContent);

        //wc status
        int beginIndex = Engine.getUserReposLocation(usernameFromSession).length();
        Map<ChangeType, List<Item>> changes = engine.getWCStatus(usernameFromSession);
        List<Item> updatedItems = changes.get(ChangeType.UPDATED);
        List<Item> createdItems = changes.get(ChangeType.CREATED);
        List<Item> deletedItems = changes.get(ChangeType.DELETED);
        List<String> deleted = convertItemListToStringPathList(beginIndex, deletedItems);
        List<String> updated = convertItemListToStringPathList(beginIndex, updatedItems);
        List<String> created = convertItemListToStringPathList(beginIndex, createdItems);
        ChangesStatusView changesForJson = new ChangesStatusView(deleted, updated, created);

        String status = "success";
        System.out.println("Save file: " + msg);
        if (!"File saved successfully!".equals(msg)) {
            status = "danger";
        }

        SaveInfo saveInfo = new SaveInfo(changesForJson, msg, status);

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(saveInfo);
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
