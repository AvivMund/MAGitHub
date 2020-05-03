package magit.engine;


import magit.engine.xml.XmlLoader;
import magit.engine.xml.XmlRepoBuilder;
import magit.xml.MagitBlob;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static magit.engine.Constants.*;
import static magit.engine.Utils.readFromTextFile;
import static magit.engine.Utils.writeToTextFile;

public class RepoManager {
    private String username;
    private Repository currentRepo = null;
    private List<Repository> repositories = new ArrayList<>();

    public RepoManager(String username) {
        this.username = username;
        try {
            initRepositories();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERROR: problem with load repositories");
            return;
        }
    }

    public void initRepositories() throws IOException {
        repositories = new ArrayList<>();
        File fileSystem = new File(SYSTEM_USERS_PATH);

        if (!fileSystem.exists()) {
            Files.createDirectories(Paths.get(SYSTEM_USERS_PATH));
        }
        File userFile = new File(SYSTEM_USERS_PATH + File.separator + username);
        if (!userFile.exists()) {
            Files.createDirectories(Paths.get(SYSTEM_USERS_PATH + File.separator + username));
        }
        loadRepositories(userFile);
    }
    //for pr ??????
    public String getRemoteRepoOwner(String repoName) {
        for (Repository r : repositories) {
            if (r.getRepoName().equals(repoName)) {
                String remoteLocation = r.getRemoteRepoLocation();
                if (remoteLocation != null) {
                    return remoteLocation.split("\\\\")[2];
                }
            }
        }

        return null;
    }
    //for pr ??????
    public String getActiveRepoRemoteOwner() {
        String remoteLocation = currentRepo.getRemoteRepoLocation();
        if (remoteLocation != null) {
            return remoteLocation.split("\\\\")[2];
        }

        return null;
    }

    private void loadRepositories(File userFile) {
        for (String repo : userFile.list()) {
            repositories.add(RepoManagerHelper.loadRepo(userFile.getAbsolutePath() + File.separator + repo));
        }
    }

    public void reloadRepository(String repoName) {
        for (int i = 0; i < repositories.size(); ++i) {
            Repository r = repositories.get(i);
            if (r.getRepoName().equals(repoName)) {
                Repository reloaded = RepoManagerHelper.loadRepo(r.getRepoLocation());
                if (r == currentRepo) {
                    currentRepo = reloaded;
                    checkout(currentRepo.getHead().getName());
                } else {
                    Repository temp = currentRepo;
                    currentRepo = reloaded;
                    checkout(currentRepo.getHead().getName());
                    currentRepo = temp;
                }
                repositories.set(i, reloaded);

                break;
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public String getCurrRepoLocation() {
        if (currentRepo == null){
            return "None";
        } else {
            return currentRepo.getRepoLocation();
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRemoteRepoLocation() {
        return currentRepo.getRemoteRepoLocation();
    }

    public void setRemoteRepoLocation(String remoteRepoLocation) {
        currentRepo.setRemoteRepoLocation(remoteRepoLocation);
    }

    public String getRemoteRepoName() {
        return currentRepo.getRemoteRepoName();
    }

    public void setRemoteRepoName(String remoteRepoName) {
        currentRepo.setRemoteRepoName(remoteRepoName);
    }
    protected void setCurrentRepo(String fullPath) {
        currentRepo = RepoManagerHelper.loadRepo(fullPath);
        buildAndInitHeadTree();
    }

    public String getCurrRepoName() {
        return currentRepo == null ? "None" : currentRepo.getRepoName();
    }

    public void createNewRepo(String repoName, String repoPath) {
        currentRepo = new Repository(repoName, repoPath, username);
        createRepoInComputerLibrary(repoPath, repoName);
    }

    private void createRepoInComputerLibrary(String path, String repoName) {
        try {
            Files.createDirectory(Paths.get(path + MAGIT_PATH));
            Files.createDirectory(Paths.get(path + OBJECTS_PATH));
            Files.createDirectory(Paths.get(path + BRANCHES_PATH));

            writeToTextFile(path + HEAD_PATH, "master");
            writeToTextFile(path + MASTER_PATH, "null");
            writeToTextFile(path + REPO_NAME_PATH, repoName);

        } catch (IOException e) {
            System.out.println("Failed to create repo");
        }
    }

    public String createCommit(String commitMessage) {
        if (currentRepo == null) {
            return null;
        }
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        buildAndInitHeadTree();
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);
        if (wcStatus.isEmpty()) {
            return "ERROR: You cant create commit because there is no changes in the WC";
        }
        wcStatus.saveChanges(currentRepo.getRepoLocation() + OBJECTS_PATH);
        Commit commit;
        if (currentRepo.getMerge() == null) {
            String prevCommitSha1 = currentRepo.getHead().getCommitSha1();
            commit = new Commit(commitMessage, wcFullTree.getSha1(), username, prevCommitSha1, null);
        } else {
            String prevCommit1Sha1 = currentRepo.getMerge().getOurs().calculateSha1();
            String prevCommit2Sha1 = currentRepo.getMerge().getTheirs().calculateSha1();
            commit = new Commit(commitMessage, wcFullTree.getSha1(), username, prevCommit1Sha1, prevCommit2Sha1);
        }
        String newCommitSha1 = commit.calculateSha1();
        commit.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
        updateHeadBranch(commit, newCommitSha1);
        buildAndInitHeadTree();

        return "The Commit created successfully!";
    }

    private void updateHeadBranch(Commit commit, String newCommitSha1) {
        Branch branch = currentRepo.getHead();
        branch.setCommit(commit);
        branch.setCommitSha1(newCommitSha1);
        String branchName = branch.getName();
        String fullPath = currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName;
        writeToTextFile(fullPath, newCommitSha1);
    }

    private Item buildWCTree(String currentLocation) {
        if (!Files.isDirectory(Paths.get(currentLocation))) {
            return new Blob(currentLocation, username);
        }

        Folder folder = new Folder(currentLocation, username);
        try (Stream<Path> paths = Files.list(Paths.get(currentLocation + File.separator))){
            if (paths.count() == 0) {
                return null;
            }
            try (Stream<Path> paths2 = Files.list(Paths.get(currentLocation + File.separator))){
                paths2.filter(f -> !f.getFileName().startsWith(".magit")).forEach(curr -> {
                    Item sub = buildWCTree(curr.toString());
                    if (sub != null) {
                        folder.addItemToItemsList(sub);
                    }
                });
                folder.calculateSha1();
            } catch (IOException e) {
                System.out.println("Failed to build working copy");
            }
        } catch (IOException e) {
            System.out.println("Failed to build working copy");
        }
        return folder;
    }

    public Map<ChangeType, List<Item>> getWCStatus() {
        if (currentRepo == null) {
            return null;
        }
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        buildAndInitHeadTree();
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);
        Map<ChangeType, List<Item>> changes = getChangeTypeListMap(wcStatus);
        return changes;
    }

    private Map<ChangeType, List<Item>> getChangeTypeListMap(WCStatus wcStatus) {
        Map<ChangeType, List<Item>> changes = new HashMap<>();
        changes.put(ChangeType.DELETED, wcStatus.getDeleted());
        changes.put(ChangeType.CREATED, wcStatus.getCreated());
        changes.put(ChangeType.UPDATED, wcStatus.getUpdated());
        return changes;
    }

    public Map<String, Branch> getBranches() {
        if (currentRepo == null) {
            return null;
        }
        return currentRepo.getBranches();
    }

    public Branch getHeadBranch() {
        if (currentRepo == null) {
            return null;
        }
        return currentRepo.getHead();
    }
    public String createBranch(String branchName) {
        if (currentRepo.getBranches().get(branchName) != null) {
            return "ERROR: Branch- " + branchName + " is already exist";
        }
        Branch newBranch;
        String commitSha1;

        //remote branch
        if (currentRepo.getRemoteRepoName() != null) {
            String rbName = currentRepo.getRemoteRepoName() + File.separator + branchName;
            Branch rb = currentRepo.getBranches().get(rbName);
            if (rb != null) {
                commitSha1 = rb.getCommitSha1();
                newBranch = new Branch(branchName, rb.getCommit(), commitSha1);
                currentRepo.setNewBranch(newBranch);
                writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName, commitSha1);
                return "Branch- " + branchName + " RTB created successfully!";
            }
        }
        Branch head = currentRepo.getHead();
        commitSha1 = head.getCommitSha1();
        newBranch = new Branch(branchName, head.getCommit(), commitSha1);
        currentRepo.setNewBranch(newBranch);
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName, commitSha1);

        return  "Branch- " + branchName + " created successfully!";
    }

    public String createBranch(String branchName, String commitSha1) {
        if (currentRepo.getBranches().get(branchName) != null) {
            return "ERROR: Branch- " + branchName + " is already exist";
        }

        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
        } catch (Exception e) {
            //e.printStackTrace();
            return "ERROR: the commit sha1 is illegal or not exist";
        }

        Branch newBranch = new Branch(branchName, commit, commitSha1);
        currentRepo.setNewBranch(newBranch);
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName, commitSha1);
        return "Branch- " + branchName + " created successfully!";
    }

    public BranchExistence deleteBranch(String branchName) {
        BranchExistence status = checkBranchExistence(branchName);
        if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            currentRepo.removeBranch(branchName);
            String branchPath = currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName;
            deleteFile(branchPath);
        }

        return status;
    }

    public BranchExistence checkBranchExistence (String branchName) {
        if (currentRepo.getBranches().get(branchName) == null) {
            return BranchExistence.NOT_EXIST;
        }
        if (currentRepo.getHead().getName().equals(branchName)) {
            return BranchExistence.HEAD;
        }

        return BranchExistence.EXIST_AND_NOT_HEAD;
    }

    public boolean isWCEmpty() {
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        buildAndInitHeadTree();
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);

        return wcStatus.isEmpty();
    }

    public void checkout(String branchName) {
        currentRepo.checkout(branchName);
        writeToTextFile(currentRepo.getRepoLocation() + HEAD_PATH, branchName);
        deleteWC(currentRepo.getRepoLocation());
        if (!buildAndInitHeadTree()) return;
        currentRepo.getRepoHeadTree().loadToWC(currentRepo.getRepoLocation());
    }

    private boolean buildAndInitHeadTree() {
        Branch headBranch = currentRepo.getHead();
        Commit headCommit = headBranch.getCommit();
        if (headCommit == null) {
            return false;
        }
        Folder newHeadRoot = loadTreeByCommit(headCommit);
        currentRepo.setHeadRoot(newHeadRoot);
        return true;
    }

    private Folder loadTreeByCommit(Commit headCommit) {
        return loadTreeByCommit(headCommit, currentRepo.getRepoLocation());
    }

    private Folder loadTreeByCommit(Commit headCommit, String repoLocation) {
        String rootFolderSha1 = headCommit.getSha1();
        Folder newHeadRoot = new Folder(repoLocation, headCommit.getAuthor());
        try {
            Folder.loadObject(repoLocation + OBJECTS_PATH, rootFolderSha1, currentRepo.getRepoLocation(), newHeadRoot);
        } catch (Exception e) {
            System.out.println("ERROR: Failed to build and initialize head commit tree");
        }
        newHeadRoot.calculateSha1();
        return newHeadRoot;
    }

    private void deleteWC(String path) {
        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            List<String> toBeDeleted = paths.filter(f -> !f.getFileName().startsWith(".magit")).map(p -> p.toString()).collect(Collectors.toList());
            for (String p : toBeDeleted) {
                deleteFile(p);
            }
        } catch (IOException e) {
            System.out.println("Failed to delete working copy");
        }
    }

    public String deleteFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                System.out.println("ERROR: Failed to delete " + path);
                return "ERROR: Failed to delete";
            }
        } else {
            file.delete();
        }

        return "File deleted successfully!";
    }

    public List<Commit> getActiveBranchCommitsHistory() {
        List<Commit> commits = new ArrayList<>();
        Branch activeBranch = currentRepo.getHead();
        Commit commit = activeBranch.getCommit();
        if (commit != null) {
            while (commit.getPreviousCommit1Sha1() != null) {
                try {
                    commits.add(commit);
                    String previousCommitSha1 = commit.getPreviousCommit1Sha1();
                    commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + previousCommitSha1);
                } catch (Exception e) {
                    System.out.println("Failed to load commit");
                }
            }
            commits.add(commit);
        }

        return commits;
    }

    public Folder getHeadTree() {
        return currentRepo.getRepoHeadTree();
    }

    public void loadXmlRepo(XmlLoader xmlLoader, boolean isContentFilesToDelete) {
        if(isContentFilesToDelete) {
            File file = new File(xmlLoader.getRepo().getLocation() + MAGIT_PATH);
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                System.out.println("Failed to delete " + xmlLoader.getRepo().getLocation() + MAGIT_PATH);
                return;
            }
            deleteWC(xmlLoader.getRepo().getLocation());
        }
        XmlRepoBuilder repoBuilder = new XmlRepoBuilder(xmlLoader);
        currentRepo = repoBuilder.getRepository();
        String repoLocation = currentRepo.getRepoLocation();
        createRepoInComputerLibrary(repoLocation, currentRepo.getRepoName());
        Map<String, Commit> commits = repoBuilder.getCommits();
        for (Commit c : commits.values()) {
            c.saveObject(repoLocation + OBJECTS_PATH);
        }
        Map<String, Folder> rootFolders = repoBuilder.getCommitsTree();
        Map<String, MagitBlob> magitBlobMap = repoBuilder.getMagitBlobsBySha1();
        for (Folder f : rootFolders.values()) {
            saveTree(f, magitBlobMap);
        }
        Map<String, Branch> branches = currentRepo.getBranches();
        Branch head = currentRepo.getHead();
        Utils.writeToTextFile(repoLocation + HEAD_PATH, head.getName());
        if (xmlLoader.getRepo().getMagitRemoteReference() != null) {
            String remoteName = xmlLoader.getRepo().getMagitRemoteReference().getName();
            try {
                Files.createDirectories(Paths.get(repoLocation + BRANCHES_PATH + File.separator + remoteName));
            } catch (IOException e) {
                //e.printStackTrace();
                return;
            }
            writeToTextFile(repoLocation + REMOTE_REPO_NAME_PATH, remoteName);
            writeToTextFile(repoLocation + REMOTE_PATH, xmlLoader.getRepo().getMagitRemoteReference().getLocation());
        }
        for (Branch b : branches.values()) {
            Utils.writeToTextFile(repoLocation + BRANCHES_PATH + File.separator + b.getName(), b.getCommitSha1());
        }
        Folder root = currentRepo.getRepoHeadTree();
        root.loadToWC(root.getFullPath());
        loadRepositories(new File(Engine.getUserReposLocation(username)));
    }

    private void saveTree(Folder f, Map<String, MagitBlob> magitBlobMap) {
        f.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
        for (magit.engine.Item item : f.getItems()) {
            if (item.getType() == ItemType.FOLDER) {
                saveTree((Folder)item, magitBlobMap);
            } else { //item.getType() == ItemType.BLOB
                ((Blob)item).saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH, magitBlobMap.get(item.getSha1()).getContent());
            }
        }
    }

    public Map<String, Commit> getCommits() {
        Map<String, Commit> commits = new HashMap<>();

        if(currentRepo != null) {
            Map<String, Branch> branches= currentRepo.getBranches();
            for (Branch b : branches.values()) {
                getPrevCommits(commits, b.getCommitSha1());
            }
        }

        return commits;
    }

    private void getPrevCommits(Map<String, Commit> commits, String commitSha1) {
        Commit currCommit = null;
        try {
            currCommit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
            commits.put(commitSha1, currCommit);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (currCommit != null && currCommit.getPreviousCommit1Sha1() != null) {
            getPrevCommits(commits, currCommit.getPreviousCommit1Sha1());
        }
    }

    public List<Commit> getPrevCommits(String commitSha1) {
        List<Commit> commits = new ArrayList<>();
        Commit currCommit = null;
        String currSha1 = commitSha1;

        do {
            try {
                currCommit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + currSha1);
                if (currCommit != null) {
                    commits.add(currCommit);
                    currSha1 = currCommit.getPreviousCommit1Sha1();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }while (currCommit != null && currCommit.getPreviousCommit1Sha1() != null);

        return commits;
    }

    public Folder getCommitTree(Commit selectedCommit) {
        return loadTreeByCommit(selectedCommit);
    }

    public Map<ChangeType, List<Item>> getDelta(Folder treeRoot, String commit1Sha1) {
        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commit1Sha1);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: commit is not Loaded");
            return null;
        }

        Folder treeRoot2 = loadTreeByCommit(commit);
        WCStatus wcStatus = new WCStatus(treeRoot, treeRoot2);
        Map<ChangeType, List<Item>> changes = getChangeTypeListMap(wcStatus);

        return changes;
    }

    public Map<ChangeType, List<Item>> getDelta(String oldCommitSha1, String newCommitSha1) {
        Commit oldCommit;
        Commit newCommit;
        try {
            oldCommit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + oldCommitSha1);
            newCommit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + newCommitSha1);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: commit is not Loaded");
            return null;
        }
        Folder oldRoot = loadTreeByCommit(oldCommit);
        Folder newRoot = loadTreeByCommit(newCommit);
        WCStatus wcStatus = new WCStatus(newRoot, oldRoot);
        Map<ChangeType, List<Item>> changes = getChangeTypeListMap(wcStatus);

        return changes;
    }

    private void fastForward(Commit theirsCommit) {
    Branch head = currentRepo.getHead();
    head.setCommit(theirsCommit);
    head.setCommitSha1(theirsCommit.calculateSha1());
    writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + head.getName(), theirsCommit.calculateSha1());
    checkout(head.getName());
}

    public MergeData mergePartA(String baseBranch, String targetBranch) {
        //mergePartA
        Commit oursCommit = currentRepo.getBranches().get(baseBranch).getCommit();
        Commit theirsCommit = currentRepo.getBranches().get(targetBranch).getCommit();
        Commit common;
        try {
            common = findCommonAncestor(oursCommit, theirsCommit);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: with parse date or load commit in findCommonAncestor function");
            return null;
        }

        //fast Forward
        if (common.calculateSha1().equals(oursCommit.calculateSha1())){
            fastForward(theirsCommit);
            return new MergeData("Fast Forward: head is 'theirs'", null);
        }
        if (common.calculateSha1().equals(theirsCommit.calculateSha1())) {
            return new MergeData("Fast Forward: no change", null);
        }

        Folder oursTree = loadTreeByCommit(oursCommit);;
        Folder theirsTree = loadTreeByCommit(theirsCommit);
        Folder commonTree = loadTreeByCommit(common);

        WCStatus oursAndCommon = new WCStatus(oursTree, commonTree);
        WCStatus theirsAndCommon = new WCStatus(theirsTree, commonTree);

        Merge merge = new Merge(oursTree, theirsTree, commonTree, oursAndCommon, theirsAndCommon, oursCommit, theirsCommit, currentRepo.getRepoLocation());
        currentRepo.setMerge(merge);

        return merge.getMergeData();
    }

    private Commit findCommonAncestor(Commit commit1, Commit commit2) throws Exception {
        Commit curr1 = commit1;
        Commit curr2 = commit2;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        String sha1;
        while(!curr1.calculateSha1().equals(curr2.calculateSha1())) {

            int compare = dateFormat.parse(curr1.getDate()).compareTo(dateFormat.parse(curr2.getDate()));
            if (compare < 0) {
                sha1 = curr2.getPreviousCommit1Sha1();
                curr2 = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + sha1);
            }
            if (compare > 0) {
                sha1 = curr1.getPreviousCommit1Sha1();
                curr1 = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + sha1);
            }
        }

        return curr1;
    }

    public void mergePartB(String commitMessage) {
        createCommit(commitMessage);
        currentRepo.setMerge(null);
    }

    public boolean resolve(String path, String content) {
        if (content == null) {
            deleteFile(path);
        } else {
            Utils.writeToTextFile(path, content);
        }
        Map<String, Change> changes = currentRepo.getMerge().getMergeData().getChanges();
        changes.remove(path);
        return changes.isEmpty();
    }

    public String saveFileToWC(String path, String content) {
        return Utils.writeToTextFile(path, content);
    }

    public String clone(String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation) {
        try {
            Files.createDirectory(Paths.get(localRepoLocation + MAGIT_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + OBJECTS_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + BRANCHES_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName));
            writeToTextFile(localRepoLocation + REPO_NAME_PATH, localRepName);
            writeToTextFile(localRepoLocation + REMOTE_REPO_NAME_PATH, remoteRepoName);
            writeToTextFile(localRepoLocation + REMOTE_PATH, remoteRepoLocation);
            fetchRemoteObjects(remoteRepoLocation, localRepoLocation);
            fetchRemoteBranches(remoteRepoLocation, localRepoLocation, remoteRepoName);
            FileUtils.moveFileToDirectory(new File(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName + File.separator + "HEAD"),
                    new File(localRepoLocation + BRANCHES_PATH), false);
            String headBranchName = readFromTextFile(localRepoLocation + HEAD_PATH);
            FileUtils.copyFileToDirectory(new File(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName + File.separator + headBranchName),
                    new File(localRepoLocation + BRANCHES_PATH), false);
            setCurrentRepo(localRepoLocation);
            checkout(headBranchName);

        } catch (IOException e) {
            System.out.println("ERROR: Failed to create clone");
            return "ERROR: Failed to create clone";
        }

        loadRepositories(new File(Engine.getUserReposLocation(username)));
        return "Clone Create successfully!";
    }

    private void fetchRemoteBranches(String remoteRepoLocation, String localRepoLocation, String remoteRepoName) throws IOException {
        String remoteBranches = remoteRepoLocation + BRANCHES_PATH;
        String localBranches = localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName;

        fetchDir(remoteBranches, localBranches);
    }

    private void fetchDir(String remoteBranches, String localBranches) throws IOException {
        File remote = new File(remoteBranches);
        File local = new File(localBranches);

        Set<String> diffSet = new HashSet<>(Arrays.asList(remote.list()));
        if (local.list().length > 0) {
            diffSet.removeAll(Arrays.asList(local.list()));
        }
        for (String obj : diffSet) {
            FileUtils.copyFile(new File(remoteBranches + File.separator + obj),
                    new File(localBranches + File.separator + obj));
        }
    }

    private void fetchRemoteObjects(String remoteRepoLocation, String localRepoLocation) throws IOException {
        String remoteObjects = remoteRepoLocation + OBJECTS_PATH;
        String localObjects = localRepoLocation + OBJECTS_PATH;

        fetchDir(remoteObjects, localObjects);
    }

    public String fetch() {
        if (currentRepo == null) {
            return "ERROR: There is no active repository in the system";
        }
        if (!Files.exists(Paths.get(currentRepo.getRepoLocation() + REMOTE_PATH))) {
            return "ERROR: this repository does not have a remote repository";
        }
        String remoteRepoLocation = readFromTextFile(currentRepo.getRepoLocation() + REMOTE_PATH);
        String localRepoLocation = currentRepo.getRepoLocation();
        String remoteRepoName = readFromTextFile(remoteRepoLocation + REPO_NAME_PATH);

        try {
            fetchRemoteBranches(remoteRepoLocation, localRepoLocation, remoteRepoName);
            fetchRemoteObjects(remoteRepoLocation, localRepoLocation);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: can not fetch files";
        }

        return "Fetch done successfully!";
    }

    public String reset(String commitSha1) {
        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: the sha1 you enter is illegal";
        }

        Branch head = currentRepo.getHead();
        head.setCommit(commit);
        head.setCommitSha1(commitSha1);
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + head.getName(), commitSha1);
        checkout(head.getName());

        return "Reset done Successfully!";
    }

    public String pull() {
        if (currentRepo == null) {
            return "ERROR: there is no active repository in the system!";
        }
        if (currentRepo.getRemoteRepoLocation() == null) {
            return "ERROR: the active repository has no remote repository!";
        }
        if (!currentRepo.getBranches().containsKey(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName())) {
           return "ERROR: the head branch is not a remote tracking branch";
        }

        Branch head = currentRepo.getHead();
        Repository remoteRepo = loadRemoteRepo(currentRepo.getRemoteRepoLocation());
        Branch remoteRepoBranch = remoteRepo.getBranches().get(head.getName());

        try {
            pullAllCommitsBetween(head.getCommit(), remoteRepoBranch.getCommit(), remoteRepo.getRepoLocation());
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: problem during pull";
        }
        reset(remoteRepoBranch.getCommitSha1());

        //update remote branch
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + currentRepo.getRemoteRepoName() + File.separator + remoteRepoBranch.getName(), remoteRepoBranch.getCommitSha1());
        String rbName = currentRepo.getRemoteRepoName() + File.separator + head.getName();
        currentRepo.getBranches().put(rbName, new Branch(rbName, remoteRepoBranch.getCommit(), remoteRepoBranch.getCommitSha1()));

        return "Pull done successfully!";
    }

    private void pullAllCommitsBetween(Commit ours, Commit update, String remoteRepoPath) throws Exception {
        String oursSha1 = ours.calculateSha1();
        if (oursSha1.equals(update.calculateSha1())) {
            return;
        }
        Commit curr1 = update;
        Commit curr2 = Commit.loadCommitObject(remoteRepoPath + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());
        do {
            curr1.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
            Folder prevRoot = loadTreeByCommit(curr2, remoteRepoPath);
            Folder updateRoot = loadTreeByCommit(curr1, remoteRepoPath);

            WCStatus delta = new WCStatus(updateRoot, prevRoot);
            delta.saveChanges(currentRepo.getRepoLocation() + OBJECTS_PATH);

            curr1 = curr2;
            curr2 = Commit.loadCommitObject(remoteRepoPath + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());
        } while (!oursSha1.equals(curr1.calculateSha1()));
    }

    private Repository loadRemoteRepo(String fullPath) {
        String headBranchName = readFromTextFile(fullPath + HEAD_PATH);
        File remote = new File(fullPath + REMOTE_REPO_NAME_PATH);
        Map<String, Branch> branches = RepoManagerHelper.loadBranches(fullPath, null);
        Branch headBranch = branches.get(headBranchName);
        return new Repository(null, fullPath, headBranch, branches);
    }

    private void pushAllCommitsBetween(Commit base, Commit update) throws Exception {
        String baseSha1 = base == null ? null : base.calculateSha1();
        if ((update.calculateSha1()).equals(baseSha1)) {
            return;
        }
        String repoLocation = currentRepo.getRepoLocation();
        String remoteRepoLocation = currentRepo.getRemoteRepoLocation();
        Commit currCommit = update;
        String currCommitSha1 = currCommit.calculateSha1();
        String previousCommitSha1 = currCommit.getPreviousCommit1Sha1();
        Commit prevCommit = getPrevCommit(repoLocation, previousCommitSha1);

        while (currCommit != null && !currCommitSha1.equals(baseSha1)) {
            currCommit.saveObject(remoteRepoLocation + OBJECTS_PATH);
            Folder prevRoot = null;
            if (prevCommit != null) {
                prevRoot = loadTreeByCommit(prevCommit, repoLocation);
            }
            Folder updateRoot = loadTreeByCommit(currCommit, repoLocation);

            WCStatus delta = new WCStatus(updateRoot, prevRoot);
            delta.saveChangesForPush(repoLocation + OBJECTS_PATH ,remoteRepoLocation + OBJECTS_PATH);

            currCommit = prevCommit;
            currCommitSha1 = previousCommitSha1;
            if (currCommit != null) {
                previousCommitSha1 = currCommit.getPreviousCommit1Sha1();
                prevCommit = getPrevCommit(repoLocation, previousCommitSha1);
            }
        }
    }

    private Commit getPrevCommit(String repoLocation, String sha1) {
        Commit prev = null;
        if (sha1 != null) {
            prev = Commit.loadCommitObject(repoLocation + OBJECTS_PATH + File.separator + sha1);
        }
        return prev;
    }

    public String push() {
        if (currentRepo == null) {
            return "ERROR: there is no active repository in the system!";
        }
        if (currentRepo.getRemoteRepoLocation() == null) {
            return "ERROR: the active repository has no remote repository!";
        }
//        if (!currentRepo.getBranches().containsKey(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName())) {
//            return "ERROR: the head branch is not a remote tracking branch";
//        }

        Repository remoteRepo = loadRemoteRepo(currentRepo.getRemoteRepoLocation());
        Branch headRB = currentRepo.getBranches().get(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName());
        Branch head = currentRepo.getHead();
        Branch remoteRepoBranch = remoteRepo.getBranches().get(head.getName());

        if(headRB != null && remoteRepoBranch != null && !headRB.getCommitSha1().equals(remoteRepoBranch.getCommitSha1())) {
            return "ERROR: the remote repository is not clean";
        }

        try {
            pushAllCommitsBetween(headRB == null ? null : headRB.getCommit(), head.getCommit());
        } catch (Exception e) {
            //e.printStackTrace();
            return "ERROR: problem during push";
        }

        writeToTextFile(currentRepo.getRemoteRepoLocation() + BRANCHES_PATH + File.separator + head.getName(), head.getCommitSha1());
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + currentRepo.getRemoteRepoName() + File.separator + head.getName(), head.getCommitSha1());
        String rbName = currentRepo.getRemoteRepoName() + File.separator + head.getName();
        currentRepo.getBranches().put(rbName, new Branch(rbName, head.getCommit(), head.getCommitSha1()));

        return "Push done successfully!";
    }

    public Branch getRBPointingCommit(String commitSha1) {
        Map<String, Branch> branches = currentRepo.getBranches();
        for (Branch b : branches.values()) {
            if (b.getCommitSha1().equals(commitSha1) && b.getName().indexOf('\\') > 0) {
                return b;
            }
        }

        return null;
    }

    public void setCurrentRepo(Repository repository) {
        currentRepo = repository;
    }

    public Repository getCurrentRepo() {
        return currentRepo;
    }

    public List<String> getFilesListByCommitSha1(String username, String commitSha1) {
       Commit commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
       if (commit != null) {
           Folder tree = loadTreeByCommit(commit);
           List<String> files = getFilesListByTree(tree, Engine.getUserReposLocation(username).length());
           return files;
       } else {
           return null;
       }
    }

    public List<String> getWCFilesList(String username) {
        Folder root = (Folder)buildWCTree(currentRepo.getRepoLocation());
        List<String> wcFiles = getFilesListByTree(root, Engine.getUserReposLocation(username).length());

        return wcFiles;
    }

    private List<String> getFilesListByTree(Folder root, int beginIndex) {
        List<String> files = new ArrayList<>();

        files.add(root.getFullPath().substring(beginIndex));
        for(Item item: root.getItems()) {
            if(item.getType() == ItemType.BLOB) {
                files.add(item.getFullPath().substring(beginIndex));
            } else { //item.getType() == ItemType.FOLDER
                files.addAll(getFilesListByTree((Folder)item, beginIndex));
            }
        }

        return files;
    }

    public String getFileSha1ByCommitAndFileName(String fileFullPath, String commitSha1) {
        Commit commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
        Folder root = loadTreeByCommit(commit);
        String fileSha1 = findFileSha1(fileFullPath, root);

        return fileSha1;
    }

    private String findFileSha1(String fileFullPath, Folder root) {
        for (Item item : root.getItems()) {
            if(item.getFullPath().equals(fileFullPath)) {
                if(item.getType() == ItemType.BLOB) {
                    return item.getSha1();
                } else {
                    return null;
                }
            } else {
                if(item.getType() == ItemType.FOLDER) {
                    return findFileSha1(fileFullPath, (Folder)item);
                }
            }
        }

        return null;
    }
}

