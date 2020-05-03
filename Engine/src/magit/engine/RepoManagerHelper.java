package magit.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static magit.engine.Utils.readFromTextFile;
import static magit.engine.Constants.*;

public class RepoManagerHelper {

    static public Repository loadRepo(String fullPath) {
        String repoName = readFromTextFile(fullPath + REPO_NAME_PATH);
        String headBranchName = readFromTextFile(fullPath + HEAD_PATH);
        File remote = new File(fullPath + REMOTE_REPO_NAME_PATH);
        String remoteRepoName = null;
        String remoteRepoLocation = null;
        if (remote.exists()) {
            remoteRepoName = readFromTextFile(fullPath + REMOTE_REPO_NAME_PATH);
            remoteRepoLocation = readFromTextFile(fullPath + REMOTE_PATH);
        }
        Map<String, Branch> branches = loadBranches(fullPath, remoteRepoName);
        Branch headBranch = branches.get(headBranchName);
        Repository newRepo = new Repository(repoName, fullPath, headBranch, branches);
        newRepo.setRemoteRepoName(remoteRepoName);
        newRepo.setRemoteRepoLocation(remoteRepoLocation);
        return newRepo;
    }

    static public Map<String, Branch> loadBranches(String fullPath, String remoteRepoName) {
        Map<String, Branch> branches = new HashMap<>();
        try (Stream<Path> paths = Files.list(Paths.get(fullPath + BRANCHES_PATH + File.separator))) {
            paths.filter(f -> !f.getFileName().startsWith("HEAD") && !f.toFile().isDirectory()).forEach(curr -> branches.put(curr.getFileName().toString(), loadBranch(curr, curr.getFileName().toString(), fullPath)));
        } catch (IOException e) {
            System.out.println("Failed to load branches");
        }
        if (null != remoteRepoName) {
            try (Stream<Path> paths = Files.list(Paths.get(fullPath + BRANCHES_PATH + File.separator + remoteRepoName + File.separator))) {
                paths.filter(f -> !f.getFileName().startsWith("HEAD") && !f.toFile().isDirectory()).forEach(curr -> branches.put(remoteRepoName + File.separator + curr.getFileName().toString(), loadBranch(curr, remoteRepoName + File.separator + curr.getFileName().toString(), fullPath)));
            } catch (IOException e) {
                System.out.println("Failed to load branches");
            }
        }
        return branches;
    }

    static private Branch loadBranch(Path branchPath, String branchName, String repoPath) {
        String commitSha1 = readFromTextFile(branchPath.toString());
        Commit commit = null;
        try {
            if (commitSha1 != null && !commitSha1.equals("null")) {
                commit = Commit.loadCommitObject(repoPath + OBJECTS_PATH + File.separator + commitSha1);
            } else {
                commitSha1 = null;
            }
        } catch (Exception e) {
            System.out.println("Failed to load branch " + branchName);
        }

        return new Branch(branchName, commit, commitSha1);
    }

}
