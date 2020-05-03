package magit.webapp.notifications;

import java.util.*;

public class NotificationManager {
    private Map<String, List<Notification>> notifications;


    public NotificationManager() {
        notifications = new HashMap<>();
    }

    public List<Notification> getNotificationsList(String username, int lastReadIdx) {
        List<Notification> n = notifications.get(username);
        return n.subList(lastReadIdx + 1, n.size());
    }

    public void addNotificationToUser(String username, Notification notification) {
        List<Notification> n = notifications.get(username);
        synchronized (n) {
            n.add(notification);
        }
    }

    public void login(String username) {
        if (!notifications.containsKey(username)) {
            notifications.put(username, new ArrayList<>());
        }
    }

    public void logout(String username) {
        notifications.get(username).clear();
    }

    //TODO: handle the notification that not relevant by last connection date
    //TODO: check if the list is empty????
}
