package magit.webapp.utils;

import magit.engine.Engine;
import magit.webapp.notifications.NotificationManager;
import magit.webapp.pr.PRManager;
import magit.webapp.user.UserManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static magit.webapp.constants.Constants.INT_PARAMETER_ERROR;

public class ServletUtils {

    private static final String USER_MANAGER_ATTRIBUTE_NAME = "userManager";
    private static final String CHAT_MANAGER_ATTRIBUTE_NAME = "chatManager";
    private static final String ENGINE_ATTRIBUTE_NAME = "engine";

    /*
    Note how the synchronization is done only on the question and\or creation of the relevant managers and once they exists -
    the actual fetch of them is remained un-synchronized for performance POV
     */
    private static final Object userManagerLock = new Object();
    private static final Object engineLock = new Object();
    private static final Object chatManagerLock = new Object();

    public static UserManager getUserManager(ServletContext servletContext) {

        synchronized (userManagerLock) {
            if (servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new UserManager());
            }
        }
        return (UserManager) servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
    }

    public static NotificationManager getNotificationManager(ServletContext servletContext) {
        return getEngine(servletContext).getNotifaicationManager();
    }

    public static PRManager getPrManager(ServletContext servletContext) {
        return getEngine(servletContext).getPrManager();
    }

//    public static ChatManager getChatManager(ServletContext servletContext) {
//        synchronized (chatManagerLock) {
//            if (servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME) == null) {
//                servletContext.setAttribute(CHAT_MANAGER_ATTRIBUTE_NAME, new ChatManager());
//            }
//        }
//        return (ChatManager) servletContext.getAttribute(CHAT_MANAGER_ATTRIBUTE_NAME);
//    }

    public static int getIntParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return INT_PARAMETER_ERROR;
    }

    public static Engine getEngine(ServletContext servletContext) {
        synchronized (engineLock) {
            if (servletContext.getAttribute(ENGINE_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(ENGINE_ATTRIBUTE_NAME, new Engine());
            }
        }
        return (Engine) servletContext.getAttribute(ENGINE_ATTRIBUTE_NAME);
    }
}
