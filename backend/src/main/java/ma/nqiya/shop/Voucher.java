package ma.nqiya.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An offer from a partner shop, bought with coins. */
@Entity
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "cost_coins", nullable = false)
    private int costCoins;

    @Column(nullable = false)
    private String city;

    private boolean active = true;

    protected Voucher() {
    }

    public Long getId() {
        return id;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getCostCoins() {
        return costCoins;
    }

    public String getCity() {
        return city;
    }

    public boolean isActive() {
        return active;
    }
}
