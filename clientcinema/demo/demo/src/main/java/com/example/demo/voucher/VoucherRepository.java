package com.example.demo.voucher;

import com.example.demo.voucher.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends MongoRepository<Voucher, String> {
    Optional<Voucher> findByCode(String code);

    List<Voucher> findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            Date now1, Date now2
    );
}
