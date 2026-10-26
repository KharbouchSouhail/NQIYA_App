package ma.nqiya.shop;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.reward.RewardService;
import ma.nqiya.user.User;
import ma.nqiya.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository vouchers;
    private final PurchaseRepository purchases;
    private final UserRepository users;
    private final RewardService rewardService;

    public ShopService(VoucherRepository vouchers, PurchaseRepository purchases, UserRepository users,
                       RewardService rewardService) {
        this.vouchers = vouchers;
        this.purchases = purchases;
        this.users = users;
        this.rewardService = rewardService;
    }

    public record VoucherDto(Long id, String partnerName, String title, String description, String category,
                             int costCoins, String city) {

        static VoucherDto of(Voucher v) {
            return new VoucherDto(v.getId(), v.getPartnerName(), v.getTitle(), v.getDescription(), v.getCategory(),
                    v.getCostCoins(), v.getCity());
        }
    }

    public record WalletItem(Long id, VoucherDto voucher, String code, Purchase.Status status, int costCoins,
                             Instant purchasedAt, Instant usedAt) {

        static WalletItem of(Purchase p) {
            return new WalletItem(p.getId(), VoucherDto.of(p.getVoucher()), p.getCode(), p.getStatus(),
                    p.getCostCoins(), p.getPurchasedAt(), p.getUsedAt());
        }
    }

    public record PurchaseResult(WalletItem item, int coinsLeft) {
    }

    @Transactional(readOnly = true)
    public List<VoucherDto> catalogue() {
        return vouchers.findByActiveTrueOrderByCostCoinsAsc().stream().map(VoucherDto::of).toList();
    }

    @Transactional
    public PurchaseResult buy(Long userId, Long voucherId) {
        Voucher voucher = vouchers.findById(voucherId).filter(Voucher::isActive)
                .orElseThrow(() -> ApiException.notFound("Voucher"));
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getCoins() < voucher.getCostCoins()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "NOT_ENOUGH_COINS",
                    "You need %d more coins".formatted(voucher.getCostCoins() - user.getCoins()));
        }
        rewardService.debitPurchase(user, voucher.getCostCoins(), voucher.getPartnerName() + " - " + voucher.getTitle());
        Purchase purchase = purchases.save(new Purchase(userId, voucher, newCode()));
        return new PurchaseResult(WalletItem.of(purchase), user.getCoins());
    }

    @Transactional(readOnly = true)
    public List<WalletItem> wallet(Long userId) {
        return purchases.findByUserIdOrderByPurchasedAtDesc(userId).stream().map(WalletItem::of).toList();
    }

    /** Called when the user completes the hold-to-use gesture in front of the cashier. */
    @Transactional
    public WalletItem redeem(Long userId, Long purchaseId) {
        Purchase purchase = purchases.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> ApiException.notFound("Voucher"));
        purchase.redeem();
        return WalletItem.of(purchase);
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("NQ-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (purchases.existsByCode(code));
        return code;
    }
}
