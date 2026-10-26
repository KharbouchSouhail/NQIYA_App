package ma.nqiya.shop;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ma.nqiya.common.exception.ApiException;

/** A voucher in a user's wallet, redeemed once at the partner's counter. */
@Entity
@Table(name = "purchase")
public class Purchase {

    public enum Status { ACTIVE, USED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "cost_coins", nullable = false)
    private int costCoins;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt = Instant.now();

    @Column(name = "used_at")
    private Instant usedAt;

    protected Purchase() {
    }

    public Purchase(Long userId, Voucher voucher, String code) {
        this.userId = userId;
        this.voucher = voucher;
        this.code = code;
        this.costCoins = voucher.getCostCoins();
    }

    public void redeem() {
        if (status == Status.USED) {
            throw ApiException.conflict("VOUCHER_USED", "This voucher was already used");
        }
        status = Status.USED;
        usedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public String getCode() {
        return code;
    }

    public Status getStatus() {
        return status;
    }

    public int getCostCoins() {
        return costCoins;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
