package magit.webapp.pr;

import java.util.HashMap;
import java.util.Map;

public class PRManager {
    Map<String, Map<Integer, PullRequest>> usersPr = new HashMap<>();

    public void addPrByUsername(String username, PullRequest pullRequest) {
        if(!usersPr.containsKey(username)) {
            usersPr.put(username, new HashMap<>());
        }
        int id = usersPr.get(username).size();
        pullRequest.setId(id);
        usersPr.get(username).put(id, pullRequest);
    }

    public Map<Integer, PullRequest> getPrsByUsername(String username) {
        return usersPr.get(username);
    }

    public PullRequest rejectPR(String rejectReason, String username, int id) {
         PullRequest pr = usersPr.get(username).get(id);
         pr.setStatus(PRStatus.REJECTED);
         pr.setRejectMessage(rejectReason);

         return pr;
    }

    public PullRequest acceptPR(String username, int id) {
        PullRequest pr = usersPr.get(username).get(id);
        pr.setStatus(PRStatus.ACCEPTED);

        return pr;
    }
}
