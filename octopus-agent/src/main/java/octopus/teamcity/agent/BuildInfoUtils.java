package octopus.teamcity.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import octopus.teamcity.common.Commit;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.Change;

import java.util.ArrayList;
import java.util.List;

public class BuildInfoUtils {
    public static String createJsonCommitHistory(final Build build) {
        final List<Change> changes = build.fetchChanges();

        final List<Commit> commits = new ArrayList<>();
        for (Change change : changes) {

            final Commit c = new Commit();
            c.Id = change.getVersion();
            c.Comment = change.getComment();

            commits.add(c);
        }

        final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(commits);
    }
}
