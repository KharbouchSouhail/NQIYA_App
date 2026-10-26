package ma.nqiya.shop;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByActiveTrueOrderByCostCoinsAsc();
}
