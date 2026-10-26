package ma.nqiya.photo;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A compressed JPEG/WebP stored in Postgres, with the AI verdict computed server-side when it was uploaded.
 */
@Entity
@Table(name = "photo")
public class Photo {

    public enum Kind { BEFORE, AFTER }

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    @Column(name = "ai_accepted", nullable = false)
    private boolean aiAccepted;

    @Column(name = "ai_title")
    private String aiTitle;

    @Column(name = "ai_description")
    private String aiDescription;

    @Column(name = "dirt_level")
    private String dirtLevel;

    @Column(name = "waste_type")
    private String wasteType;

    private boolean used;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Photo() {
    }

    public Photo(Long ownerId, Kind kind, String contentType, byte[] data) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.kind = kind;
        this.contentType = contentType;
        this.data = data;
    }

    public void recordVerdict(boolean accepted, String title, String description, String dirtLevel, String wasteType) {
        this.aiAccepted = accepted;
        this.aiTitle = title;
        this.aiDescription = description;
        this.dirtLevel = dirtLevel;
        this.wasteType = wasteType;
    }

    public void markUsed() {
        this.used = true;
    }

    public UUID getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isAiAccepted() {
        return aiAccepted;
    }

    public String getAiTitle() {
        return aiTitle;
    }

    public String getAiDescription() {
        return aiDescription;
    }

    public String getDirtLevel() {
        return dirtLevel;
    }

    public String getWasteType() {
        return wasteType;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
