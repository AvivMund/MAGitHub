package magit.webapp.user;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
Adding and retrieving users is synchronized and in that manner - these actions are thread safe
Note that asking if a user exists (isUserExists) does not participate in the synchronization and it is the responsibility
of the user of this class to handle the synchronization of isUserExists with other methods here on it's own
 */
public class UserManager {

    private final Map<String, User> users;
    //private final Set<String> usersSet;

    public UserManager() {
        users = new HashMap<>();
    }

    public synchronized void addUser(String username) {
        users.put(username, new User(username));
    }

    public synchronized boolean login(String username) {
        User u = users.get(username);
        if (u == null) {
            addUser(username);
            return true;
        }
        if (u.isOnline()) {
            return false;
        }
        u.setOnline(true);
        u.setLastLogin(new Date());
        return true;
    }

    public synchronized boolean logout(String username) {
        User u = users.get(username);
        if (u == null || !u.isOnline()) {
            return false;
        }
        u.setOnline(false);
        u.setLastLogout(new Date());
        return true;
    }

    public synchronized Map<String, User> getUsers() {
        return Collections.unmodifiableMap(users);
    }

    public boolean isUserExists(String username) {
        return users.containsKey(username);
    }

    public User getUser(String username) {
        return users.get(username);
    }
}
