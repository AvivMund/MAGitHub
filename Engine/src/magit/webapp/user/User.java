package magit.webapp.user;

import java.util.Date;

public class User {
    private String name;
    private boolean isOnline;
    private Date lastLogin;
    private Date lastLogout;

    public User(String username) {
        name = username;
        isOnline = true;
        lastLogin = new Date();
        lastLogout = null;
    }

    public String getName() {
        return name;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(Date lastLogout) {
        this.lastLogout = lastLogout;
    }
}
