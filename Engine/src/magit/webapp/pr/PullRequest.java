package magit.webapp.pr;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PullRequest {
    private String repoName;
    private String targetBranch;
    private String baseBranch;
    private String requestUsername;
    private String message;
    private String date;
    private int id;

    private PRStatus status = PRStatus.PENDING;
    private String rejectMessage = null;

    public PullRequest(String repoName, String targetBranch, String baseBranch, String requestUsername, String message) {
        this.repoName = repoName;
        this.targetBranch = targetBranch;
        this.baseBranch = baseBranch;
        this.requestUsername = requestUsername;
        this.message = message;
        SimpleDateFormat formatDateOfCreation = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        this.date = formatDateOfCreation.format(new Date());
    }

    public String getDate() {
        return date;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public String getRequestUsername() {
        return requestUsername;
    }

    public String getMessage() {
        return message;
    }

    public PRStatus getStatus() {
        return status;
    }

    public String getRejectMessage() {
        return rejectMessage;
    }

    public void setStatus(PRStatus status) {
        this.status = status;
    }

    public void setRejectMessage(String rejectMessage) {
        this.rejectMessage = rejectMessage;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
