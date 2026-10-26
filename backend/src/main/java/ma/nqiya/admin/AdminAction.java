package ma.nqiya.admin;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Audit log entry for every moderation action. */
@Entity
@Table(name = "admin_action")
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AdminAction() {
    }

    public AdminAction(Long adminId, String adminEmail, String action, String targetType, Long targetId,
                       String details) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
