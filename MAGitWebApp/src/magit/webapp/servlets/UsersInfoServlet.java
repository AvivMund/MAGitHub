package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.webapp.user.User;
import magit.webapp.user.UserManager;
import magit.webapp.utils.ServletUtils;
import magit.webapp.utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

public class UsersInfoServlet extends HttpServlet {
    public class UsersInfo {
        private String myUsername;
        private Collection<User> users;

        public UsersInfo(String myUsername, Collection<User> users) {
            this.myUsername = myUsername;
            this.users = users;
        }

        public String getMyUsername() {
            return myUsername;
        }

        public Collection<User> getUsers() {
            return users;
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
        UserManager userManager = ServletUtils.getUserManager(getServletContext());
        User u = userManager.getUser(usernameFromSession);
        if (u == null) {
            response.setStatus(404);
            return;
        }
        try (PrintWriter out = response.getWriter()) {
            Collection<User> users = userManager.getUsers().values();
            UsersInfo usersInfo = new UsersInfo(u.getName(), users);
            String json = gson.toJson(usersInfo);
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
