package ma.nqiya.shop;

import java.util.List;

import ma.nqiya.security.CurrentUser;
import ma.nqiya.shop.ShopService.PurchaseResult;
import ma.nqiya.shop.ShopService.VoucherDto;
import ma.nqiya.shop.ShopService.WalletItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShopController {

    private final ShopService shopService;
    private final CurrentUser currentUser;

    public ShopController(ShopService shopService, CurrentUser currentUser) {
        this.shopService = shopService;
        this.currentUser = currentUser;
    }

    @GetMapping("/shop/vouchers")
    public List<VoucherDto> catalogue() {
        return shopService.catalogue();
    }

    @PostMapping("/shop/vouchers/{id}/buy")
    public PurchaseResult buy(@PathVariable Long id) {
        return shopService.buy(currentUser.id(), id);
    }

    @GetMapping("/wallet")
    public List<WalletItem> wallet() {
        return shopService.wallet(currentUser.id());
    }

    @PostMapping("/wallet/{id}/redeem")
    public WalletItem redeem(@PathVariable Long id) {
        return shopService.redeem(currentUser.id(), id);
    }
}
