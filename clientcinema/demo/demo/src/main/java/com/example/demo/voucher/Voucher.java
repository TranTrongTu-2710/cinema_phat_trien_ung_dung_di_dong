package com.example.demo.voucher;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "vouchers")
@CompoundIndex(name = "idx_active_time", def = "{'active':1,'startAt':1,'endAt':1}")
public class Voucher {
    @Id
    private String id;            // e.g., v_2025BLACKFRI

    private String code;          // recommend unique index at DB init
    private String name;
    private String description;

    public enum VoucherType { PERCENT, FIXED }
    private VoucherType type;     // PERCENT | FIXED
    private Integer value;        // % or fixed amount (VND)
    private Integer maxDiscount;  // optional for PERCENT
    private Integer minOrderTotal;

    /** Stored as BSON Date. JSON uses ISO-8601 (UTC) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date startAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date endAt;

    private boolean active;       // true ~ "1"

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date updatedAt;
}
