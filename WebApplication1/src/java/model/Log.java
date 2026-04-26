package model;

/**
 * Represents a single entry in the system audit log.
 *
 * <p>Each {@code Log} record mirrors a row in the {@code log} database table.
 * Entries are created by {@link concurrency.SystemLog} and persisted by
 * {@link dao.LogDAO} every time a significant action occurs in the system
 * (e.g., reservation creation, confirmation, deletion, or TTL expiry).
 *
 * <p>This is a plain JavaBean with no business logic.
 */
public class Log {

    /**
     * Auto-incremented primary key that uniquely identifies this log entry.
     */
    private int id;

    /**
     * Email address (or {@code "SYSTEM"}) of the actor who triggered the
     * logged event.
     */
    private String user;

    /**
     * Short upper-case action code that categorizes the event
     * (e.g., {@code "RESERVE_OK"}, {@code "DELETE_OK"}, {@code "TTL_EXPIRED"}).
     */
    private String action;

    /**
     * Human-readable description providing additional context for the event.
     */
    private String description;

    /**
     * Timestamp (as a formatted string, {@code yyyy-MM-dd HH:mm:ss}) indicating
     * when the event was recorded. Populated by the database {@code NOW()}
     * function at insertion time.
     */
    private String createdAt;

    /**
     * Returns the primary key of this log entry.
     *
     * @return the log entry ID
     */
    public int getId() { return id; }

    /**
     * Sets the primary key of this log entry.
     *
     * @param id the log entry ID
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the user (or system actor) associated with this log entry.
     *
     * @return the user email or {@code "SYSTEM"}
     */
    public String getUser() { return user; }

    /**
     * Sets the user (or system actor) for this log entry.
     *
     * @param user the user email or {@code "SYSTEM"}
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Returns the action code for this log entry.
     *
     * @return the action code (upper-case)
     */
    public String getAction() { return action; }

    /**
     * Sets the action code for this log entry.
     *
     * @param action the action code
     */
    public void setAction(String action) { this.action = action; }

    /**
     * Returns the descriptive text of this log entry.
     *
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the descriptive text of this log entry.
     *
     * @param description the description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the creation timestamp of this log entry as a formatted string.
     *
     * @return the creation timestamp in {@code yyyy-MM-dd HH:mm:ss} format
     */
    public String getCreatedAt() { return createdAt; }

    /**
     * Sets the creation timestamp of this log entry.
     *
     * @param createdAt the creation timestamp string
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}