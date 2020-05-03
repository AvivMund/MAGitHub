package magit.engine;

import magit.engine.xml.XmlLoader;
import magit.webapp.notifications.Notification;
import magit.webapp.notifications.NotificationManager;
import magit.webapp.pr.PRManager;
import magit.webapp.pr.PullRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static magit.engine.Constants.MAGIT_PATH;
import static magit.engine.Constants.SYSTEM_USERS_PATH;

public class Engine {
    private Map<String, RepoManager> usersRepos = new HashMap<>();
    private PRManager prManager = new PRManager();
    private NotificationManager notificationManager = new NotificationManager();

    public RepoManager getRepoManagerByUsername(String username) {
        return usersRepos.get(username);
    }

    public void addUser(String username) {
        if (!usersRepos.containsKey(username)) {
            usersRepos.put(username, new RepoManager(username));
        }
    }

    public PRManager getPrManager() {
        return prManager;
    }

    public String getCurrRepoLocation(String username) {
        return usersRepos.get(username).getCurrRepoLocation();
    }

    public Map<ChangeType, List<Item>> getWCStatus(String username) {
        return usersRepos.get(username).getWCStatus();
    }

    public Map<String, Branch> getBranches(String username) {
        return usersRepos.get(username).getBranches();
    }

    public Branch getHeadBranch(String username) {
        return usersRepos.get(username).getHeadBranch();
    }

    public String switchCurrentRepo(String username, String fullPath) {
        String message = isTheRepoIsLegal(fullPath);
        if (message == null) {
            message = "ERROR: there is no repository in this location";
        } else if (message.equals("This location has already repository")) {
            if (usersRepos.get(username).getCurrRepoLocation() != null &&  usersRepos.get(username).getCurrRepoLocation().equals(fullPath)) {
                message = "The path you entered is already the active repository!";
            } else { //Repository is exist and not the active repo
                usersRepos.get(username).setCurrentRepo(fullPath);
                message = "The active repository switched successfully!";
            }
        }

        return message;
    }

    public String createBranch(String username, String branchName) {
        if (usersRepos.get(username).getCurrRepoName().equals("None")) {
            return "ERROR: There is no active repository";
        }

        return usersRepos.get(username).createBranch(branchName);
    }

    public String createBranch(String username, String branchName, String commitSha1) {
        if (usersRepos.get(username).getCurrRepoName().equals("None")) {
            return "ERROR: There is no active repository";
        }

        return usersRepos.get(username).createBranch(branchName, commitSha1);
    }

    public String deleteBranch(String username, String branchName) {
        String message;
        BranchExistence status = usersRepos.get(username).deleteBranch(branchName);
        if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            message = "Branch - " + branchName + " deleted successfully!";
        } else { //(status == BranchExistence.HEAD)
            message = "ERROR: Branch - '" + branchName + "' is the head branch and you can not delete it!";
        }
//        else { // status == BranchExistence.NOT_EXIST
//            message = "Unfortunately, Branch - " + branchName + " is not exist :(";
//        }

        return message;
    }

    public String beforeCheckout(String username, String branchName) {
        BranchExistence status = usersRepos.get(username).checkBranchExistence(branchName);
        String message = null;
        if (status == BranchExistence.HEAD) {
            message = "ERROR: Branch - " + branchName + " is already the head branch!";
        } else if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            if (!usersRepos.get(username).isWCEmpty()) {
                message = "ERROR: The current WC is with open changes, you are going to lose information!";
            }
        }

        return message;
    }

    public void checkout(String username, String branchName) {
        usersRepos.get(username).checkout(branchName);
    }

    public List<Commit> getActiveBranchCommitsHistory(String username) {
        return usersRepos.get(username).getActiveBranchCommitsHistory();
    }

    public Folder getHeadTree(String username) {
        return usersRepos.get(username).getHeadTree();
    }

    public String loadRepoFromXml(String username, XmlLoader xmlLoader, boolean isContentFilesToDelete) {
        usersRepos.get(username).loadXmlRepo(xmlLoader, isContentFilesToDelete);
        return "Repository loaded from xml successfully";
    }

    public Map<String, Commit> getCommits(String username) {
        return usersRepos.get(username).getCommits();
    }

    public List<Commit> getPrevCommits(String username, String commitSha1) {
        return usersRepos.get(username).getPrevCommits(commitSha1);
    }

    public Folder getCommitTree(String username, Commit selectedCommit) {
        return usersRepos.get(username).getCommitTree(selectedCommit);
    }

    public Map<ChangeType, List<Item>> getDelta(String username, Folder treeRoot, String commit1Sha1) {
        return usersRepos.get(username).getDelta(treeRoot, commit1Sha1);
    }

    public Map<ChangeType, List<Item>> getDelta(String username, String oldCommitSha1, String newCommitSha1) {
        return usersRepos.get(username).getDelta(oldCommitSha1, newCommitSha1);
    }

    public MergeData mergePartA(String username, String selectedBranch) {
        return usersRepos.get(username).mergePartA(usersRepos.get(username).getHeadBranch().getName(), selectedBranch);
    }

    public boolean isWCEmpty(String username) {
        return usersRepos.get(username).isWCEmpty();
    }

    public void mergePartB(String username, String commitMessage) {
        usersRepos.get(username).mergePartB(commitMessage);
    }

    public String getFileContent(String username, String sha1) {
        try {
            return Blob.getContent(usersRepos.get(username).getCurrRepoLocation(), sha1);
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed getting Content";
        }
    }

    public String getWCFileContent(String username, String fileRelativePath) {
        String reposLocation = getUserReposLocation(username);
        String fullPath = reposLocation + fileRelativePath;
        File f = new File(fullPath);
        if (f.isDirectory()) {
            return "ERROR: This file is a directory not a text file!";
        }
        if (!f.exists()) {
            return "ERROR: This file does not exist";
        }
        return Blob.getContent(fullPath);
    }

    public boolean resolve(String username, String path, String content) {
        return usersRepos.get(username).resolve(path, content);
    }

    public String saveFileToWC(String username, String fileRelativePath, String content) {
        String fullPath = getUserReposLocation(username) + fileRelativePath;
        return usersRepos.get(username).saveFileToWC(fullPath, content);
    }

    public String deleteFile(String username, String fileRelativePath){
        String fullPath = getUserReposLocation(username) + fileRelativePath;
        if (fullPath.equals(usersRepos.get(username).getCurrRepoLocation())) {
            return "ERROR: You cannot delete the repository root folder!";
        }

        return usersRepos.get(username).deleteFile(fullPath);
    }

    public String createFile (String username, String fileRelativePath) {
        if (fileRelativePath.indexOf("\\..\\") >= 0) {
            return "ERROR: illegal file path";
        }
        if (!fileRelativePath.startsWith("\\" + usersRepos.get(username).getCurrRepoName() + "\\")) {
            return "ERROR: path must start with repository name";
        }
        String fullPath = getUserReposLocation(username) + fileRelativePath;
        return Utils.writeToTextFile(fullPath, "");
    }

    public String clone(String username, String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation) {
        if(remoteRepoName.equals("")) {
            return "ERROR: there is no selected repository! ";
        }
        File dir = new File(localRepoLocation);
        try {
            Files.createDirectories(Paths.get(localRepoLocation));
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: problem with create repository root directory";
        }
        if (dir.list().length == 0) {
            if (Files.exists(Paths.get(remoteRepoLocation + MAGIT_PATH))) {
                return usersRepos.get(username).clone(remoteRepoName, remoteRepoLocation, localRepName, localRepoLocation);

            } else {
                return "ERROR: The remote directory is not a repository";
            }
        } else {
            return "ERROR: The local directory is not an empty directory";
        }
    }

    public String fetch(String username) {
        return usersRepos.get(username).fetch();
    }

    public String reset(String username, String commitSha1) {
        return usersRepos.get(username).reset(commitSha1);
    }

    public String pull(String username) {
        return usersRepos.get(username).pull();
    }

    public String push(String username) {
        return usersRepos.get(username).push();
    }

    public Branch getRBPointingCommit(String username, String commitSha1) {
        return usersRepos.get(username).getRBPointingCommit(commitSha1);
    }

    public String getCurrRepoName(String username) {
        return usersRepos.get(username).getCurrRepoName();
    }

    public String createRepo(String username, String repoName, String repoPath) {
        String message = isTheRepoIsLegal(repoPath);

        // repo can created
        if (message == null) {
            message = "Repository is created successfully!";
            usersRepos.get(username).createNewRepo(repoName, repoPath);
        }

        return message;
    }

    private String isTheRepoIsLegal(String locationPath) {
//        Path path = Paths.get(locationPath);
//        if (!Files.exists(path)) {
//            return "Unfortunately, this location is not exist";
//        }
//        if (!Files.isDirectory(path)) {
//            return "Unfortunately, this location is not directory";
//        }

//        if (!(Paths.get(locationPath).isAbsolute())) {
//            return "ERROR, you should enter full path of the repository";
//        }

        String temp = locationPath + "\\.magit";
        Path magitPath = Paths.get(temp);
        if (Files.exists(magitPath)) {
            return "This location has already repository";
        }

        return null; // The repository can created
    }

    public String createCommit(String username, String commitMessage) {
        if (usersRepos.get(username).getCurrRepoName().equals("none")) {
            return "ERROR: There is no active repository in the system";
        }

         return usersRepos.get(username).createCommit(commitMessage);
    }

    static public String getUserReposLocation(String username) {
        return SYSTEM_USERS_PATH + File.separator + username;
    }


    public String loadRepoFromXmlStream(String username, InputStream xmlInputStream, boolean force) throws IOException {
        XmlLoader xmlLoader = new XmlLoader(xmlInputStream);
        if (!xmlLoader.getErrorMessage().equals("") || !xmlLoader.isValid()) {
            return xmlLoader.getErrorMessage();
        }
        String repoLocation = getUserReposLocation(username) + File.separator + xmlLoader.getRepo().getName();
        xmlLoader.getRepo().setLocation(repoLocation);
        Path repoPath = Paths.get(repoLocation);
        Files.createDirectories(repoPath);
        if (Files.exists(Paths.get(repoLocation + MAGIT_PATH)) && !force) {
            return "There is already another repository in the xml repo location. Please check the '<i>Force</i>' checkbox and retry.";
        }
        return loadRepoFromXml(username, xmlLoader, force);
    }

    public NotificationManager getNotifaicationManager() {
        return notificationManager;
    }

    public String fork(String username, String selectedUser, String selectedRepo) {
        String remoteRepoName = selectedRepo;
        String remoteRepoLocation = getUserReposLocation(selectedUser) + File.separator + selectedRepo;
        String localRepName = remoteRepoName;
        String localRepoLocation = getUserReposLocation(username) + File.separator + selectedRepo;
        String msg = clone(username, remoteRepoName, remoteRepoLocation, localRepName, localRepoLocation);
        if (msg.equals("Clone Create successfully!")) {
            notificationManager.addNotificationToUser(selectedUser,new Notification(selectedUser, localRepName, username));
        }
        return msg;
    }

    public void setActiveRepo(String username, Repository repository) {
        usersRepos.get(username).setCurrentRepo(repository);
    }

    public List<String> getFilesListByCommitSha1(String username, String commitSha1) {
        return usersRepos.get(username).getFilesListByCommitSha1(username, commitSha1);
    }

    public String pullRequest(String username, String targetBranch, String baseBranch, String message) {
        if (usersRepos.get(username).getCurrentRepo().getRemoteRepoLocation() == null) {
            return "ERROR: there is no remote repository!";
        }
        if (targetBranch.equals(baseBranch)) {
            return "ERROR: can not pull branch to itself!";
        }
        Branch target = usersRepos.get(username).getBranches().get(targetBranch);
        Branch base = usersRepos.get(username).getBranches().get(baseBranch);
        if (base.getCommitSha1().equals(target.getCommitSha1())) {
            return "ERROR: both branches point on the same commit, nothing to do!";
        }
        String remoteUsername = usersRepos.get(username).getActiveRepoRemoteOwner();
        String repoName = usersRepos.get(username).getCurrRepoName();
        prManager.addPrByUsername(remoteUsername, new PullRequest(repoName, targetBranch, baseBranch, username, message));
        notificationManager.addNotificationToUser(remoteUsername, new Notification(remoteUsername, repoName, username, message, targetBranch, baseBranch));

        return "Pull Request sent successfully!";
    }

    public void rejectPR(String rejectReason, String username, int id) {
        PullRequest pr = prManager.rejectPR(rejectReason, username, id);
        notificationManager.addNotificationToUser(pr.getRequestUsername(), new Notification(pr, username));
    }

    public void acceptPR(String username, int id) {
        PullRequest pr = prManager.acceptPR(username, id);
        notificationManager.addNotificationToUser(pr.getRequestUsername(), new Notification(pr, username));
        //performMerge
        usersRepos.get(username).mergePartA(pr.getBaseBranch(), pr.getTargetBranch());
        usersRepos.get(username).mergePartB(pr.getMessage());
    }

    public String getFileSha1ByCommitAndFileName(String username, String filePath, String commitSha1) {
        String fileFullPath = getUserReposLocation(username) + filePath;
        return usersRepos.get(username).getFileSha1ByCommitAndFileName(fileFullPath, commitSha1);
    }

    public List<String> getWCFilesList(String username) {
        return usersRepos.get(username).getWCFilesList(username);
    }
}
