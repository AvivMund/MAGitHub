package magit.webapp.utils;

import magit.webapp.constants.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionUtils {

    public static String getUsername (HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object sessionAttribute = session != null ? session.getAttribute(Constants.USERNAME) : null;
        return sessionAttribute != null ? sessionAttribute.toString() : null;
    }

    public static void clearSession (HttpServletRequest request) {
        request.getSession().invalidate();
    }

    public static void setActiveRepoName(HttpServletRequest request, String repoName) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(Constants.ACTIVE_REPO_NAME, repoName);
        }
    }

    public static String getActiveRepoName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object sessionAttribute = session != null ? session.getAttribute(Constants.ACTIVE_REPO_NAME) : null;
        return sessionAttribute != null ? sessionAttribute.toString() : null;
    }

    public static void setPrId(HttpServletRequest request, Integer prId) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(Constants.PR_ID, prId);
        }
    }

    public static Integer getPrId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object sessionAttribute = session != null ? session.getAttribute(Constants.PR_ID) : null;
        return sessionAttribute != null ? (Integer) sessionAttribute : null;
    }
}
