package ma.nqiya.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A one-time 6-digit login code. Only its SHA-256 hash is stored. */
@Entity
@Table(name = "login_code")
public class LoginCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private int attempts;

    private boolean consumed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LoginCode() {
    }

    public LoginCode(String email, String codeHash, Instant expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now, int maxAttempts) {
        return !consumed && now.isBefore(expiresAt) && attempts < maxAttempts;
    }

    public void registerAttempt() {
        attempts++;
    }

    public void consume() {
        consumed = true;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }
}
