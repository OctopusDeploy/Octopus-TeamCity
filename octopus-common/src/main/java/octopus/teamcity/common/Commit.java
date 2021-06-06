package octopus.teamcity.common;

public class Commit {
    public String id;
    public String comment;

    public Commit (final String id, final String comment) {
        this.id = id;
        this.comment = comment;
    }
}
