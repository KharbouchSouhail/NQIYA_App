package ma.nqiya.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private int xp;

    private int coins;

    @Column(name = "ticket_code", nullable = false, unique = true)
    private String ticketCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected User() {
    }

    public User(String email, String displayName, String ticketCode) {
        this.email = email;
        this.displayName = displayName;
        this.ticketCode = ticketCode;
    }

    public Rank rank() {
        return Rank.forXp(xp);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void addXp(int amount) {
        this.xp += amount;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void spendCoins(int amount) {
        if (amount > coins) {
            throw new IllegalStateException("Not enough coins");
        }
        this.coins -= amount;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getXp() {
        return xp;
    }

    public int getCoins() {
        return coins;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
