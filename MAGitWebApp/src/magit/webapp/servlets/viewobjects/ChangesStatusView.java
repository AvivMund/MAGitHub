package magit.webapp.servlets.viewobjects;

import java.util.List;

public class ChangesStatusView {
    List<String> deleted;
    List<String> updated;
    List<String> created;

    public ChangesStatusView(List<String> deleted, List<String> updated, List<String> created) {
        this.deleted = deleted;
        this.updated = updated;
        this.created = created;
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public List<String> getUpdated() {
        return updated;
    }

    public List<String> getCreated() {
        return created;
    }
}
