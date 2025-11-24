package com.example.demo.userVoucherDetials;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "user_voucher_details")
@CompoundIndex(name = "uq_user_voucher", def = "{'userId':1,'voucherId':1}", unique = true)
public class UserVoucherDetail {
    @Id
    private String id;        // e.g., auto or provided

    private String userId;    // foreign key to your existing users
    private String voucherId; // ref to vouchers.id
    private Date usedAt;
    public enum Status { USED, NOT_USED_YET }
    private Status status;    // USED | NOT_USED_YET
}
