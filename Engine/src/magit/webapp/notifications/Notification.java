package magit.webapp.notifications;

import magit.webapp.pr.PRStatus;
import magit.webapp.pr.PullRequest;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Notification {
    protected String username;
    protected String date;
    protected String message;

    //pull request c'tor
    public Notification(String username, String repoName, String fromUsername,
                        String prMessage, String targetBranchName, String baseBranchName) {
        this.username = username;
        SimpleDateFormat formatDateOfCreation = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        this.date = formatDateOfCreation.format(new Date());
        this.message = "New Pull Request:" +
                " By: " + fromUsername +
                ", Repository: " + repoName +
                ", Message: " + prMessage +
                ", From: " + targetBranchName +
                ", To: " + baseBranchName;

    }

    //pull request status c'tor
    public Notification(PullRequest pr, String fromUsername) {
        this.username = pr.getRequestUsername();
        SimpleDateFormat formatDateOfCreation = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        this.date = formatDateOfCreation.format(new Date());
        this.message = "Your Pull Request " + (pr.getStatus() == PRStatus.ACCEPTED ? "Accepted!\n" : "Rejected!\nRejected Reason: " + pr.getRejectMessage() + "\n") +
                "Username: " + fromUsername + "\n" +
                "Repository name: " + pr.getRepoName() + "\n" +
                "Pull request message: " + pr.getMessage() + "\n" +
                "Target branch name: " + pr.getTargetBranch() + "\n" +
                "Base branch name: " + pr.getBaseBranch() + "\n";
    }

    //fork c'tor
    public Notification(String username, String repoName, String fromUsername) {
        SimpleDateFormat formatDateOfCreation = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        this.date = formatDateOfCreation.format(new Date());
        message = fromUsername + " forked " + repoName;
    }

    public String getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }
}
