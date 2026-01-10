package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents user profile information including credits and team.
 *
 * <p>Example:</p>
 * <pre>{@code
 * User user = client.getUser();
 * System.out.println("Email: " + user.getEmail());
 * System.out.println("Credits: " + user.getCredits());
 * if (user.getTeam() != null) {
 *     System.out.println("Team: " + user.getTeam().getName());
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("id")
    private String id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("credits")
    private int credits;

    @JsonProperty("team")
    private Team team;

    /**
     * Default constructor for Jackson deserialization.
     */
    public User() {
    }

    /**
     * Returns the user ID.
     *
     * @return the unique user identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the user ID.
     *
     * @param id the user ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the user's display name.
     *
     * @return the display name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's display name.
     *
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user's available credits.
     *
     * @return the number of credits remaining
     */
    public int getCredits() {
        return credits;
    }

    /**
     * Sets the user's credits.
     *
     * @param credits the number of credits
     */
    public void setCredits(int credits) {
        this.credits = credits;
    }

    /**
     * Returns the user's team information.
     *
     * @return the team, or null if the user is not part of a team
     */
    public Team getTeam() {
        return team;
    }

    /**
     * Sets the user's team.
     *
     * @param team the team information
     */
    public void setTeam(Team team) {
        this.team = team;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", credits=" + credits +
                ", team=" + team +
                '}';
    }
}
