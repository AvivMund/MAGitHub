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

public class WCFileContentServlet extends HttpServlet {
    public class FileInfo {
        private String content;
        private boolean isDirectory;

        public FileInfo(String content, boolean isDirectory) {
            this.content = content;
            this.isDirectory = isDirectory;
        }

        public String getContent() {
            return content;
        }

        public boolean isDirectory() {
            return isDirectory;
        }
    }
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //returning JSON objects, not HTML
        response.setContentType("application/json");
        Gson gson = new Gson();
        String usernameFromSession = SessionUtils.getUsername(request);
        Engine engine = ServletUtils.getEngine(getServletContext());

        if (usernameFromSession == null) {
            response.setStatus(404);
            return;
        }
        String filePath = request.getParameter("file");
        boolean isDirectory = false;

        //"ERROR: Failed to load file content";
        //"ERROR: This file is a directory not a text file!";
        //"ERROR: This file does not exist";


        String content = "";
        if (filePath != null) {
            content = engine.getWCFileContent(usernameFromSession, filePath);
            if (content.startsWith("ERROR")) {
                System.out.println(content);
                content = "";
                isDirectory = true;
            }
        }

        FileInfo fileInfo = new FileInfo(content, isDirectory);

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(fileInfo);
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
