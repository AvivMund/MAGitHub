package magit.webapp.servlets;

import com.google.gson.Gson;
import magit.webapp.notifications.Notification;
import magit.webapp.notifications.NotificationManager;
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

public class NotificationsInfoServlet extends HttpServlet {
    public class NotificationInfo {
        private String date;
        private String message;

        public NotificationInfo(String date, String message) {
            this.date = date;
            this.message = message;
        }

        public String getDate() {
            return date;
        }

        public String getMessage() {
            return message;
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

        NotificationManager notificationManager = ServletUtils.getNotificationManager(getServletContext());
        List<NotificationInfo> notificationsForJson = new ArrayList<>();
        String lastReadIdxParam = request.getParameter("lastReadIdx");
        int lastReadIdx = -1;
        if (lastReadIdxParam != null) {
            lastReadIdx = Integer.parseInt(lastReadIdxParam);
        }

        Collection<Notification> notifications = notificationManager.getNotificationsList(usernameFromSession, lastReadIdx);
        for(Notification n : notifications) {
            notificationsForJson.add(new NotificationInfo(n.getDate(), n.getMessage()));
        }

        try (PrintWriter out = response.getWriter()) {
            String json = gson.toJson(notificationsForJson);
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
