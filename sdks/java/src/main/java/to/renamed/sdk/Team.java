package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents team information for a user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    /**
     * Default constructor for Jackson deserialization.
     */
    public Team() {
    }

    /**
     * Creates a new Team with the specified properties.
     *
     * @param id the team ID
     * @param name the team name
     */
    public Team(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the team ID.
     *
     * @return the unique team identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the team ID.
     *
     * @param id the team ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the team name.
     *
     * @return the team display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the team name.
     *
     * @param name the team name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
