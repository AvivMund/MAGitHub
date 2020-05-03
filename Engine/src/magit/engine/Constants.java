package magit.engine;

import java.io.File;

public class Constants {
    public static final String SYSTEM_USERS_PATH = "c:\\magit-ex3";
    public static final String MAGIT_PATH = File.separator + ".magit";
    public static final String REMOTE_REPO_NAME_PATH = MAGIT_PATH + File.separator + "remote name";
    public static final String REPO_NAME_PATH = MAGIT_PATH + File.separator + "repository name";
    public static final String REMOTE_PATH = MAGIT_PATH + File.separator + "remote location";
    public static  final String OBJECTS_PATH = MAGIT_PATH + File.separator + "objects";
    public static  final String BRANCHES_PATH = MAGIT_PATH + File.separator + "branches";
    public static  final String HEAD_PATH =  BRANCHES_PATH + File.separator + "HEAD";
    public static  final String MASTER_PATH = BRANCHES_PATH + File.separator + "master";

}
