package com.example.demo.userVoucherDetials;

import com.example.demo.userVoucherDetials.UserVoucherDetail;
import com.example.demo.userVoucherDetials.UserVoucherDetail.Status;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserVoucherDetailRepository extends MongoRepository<UserVoucherDetail, String> {
    Optional<UserVoucherDetail> findByUserIdAndVoucherId(String userId, String voucherId);

    List<UserVoucherDetail> findByVoucherIdAndStatus(String voucherId, Status status);

    List<UserVoucherDetail> findByUserId(String userId);

    List<UserVoucherDetail> findByUserIdAndStatus(String userId, Status status);

    List<UserVoucherDetail> findByUserIdAndVoucherIdInAndStatus(String userId, Collection<String> voucherIds, Status status);

    // Batch helper for Endpoint A
    List<UserVoucherDetail> findByVoucherIdAndUserIdInAndStatus(String voucherId, Collection<String> userIds, Status status);
}
