package com.example.demo.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.regex.Pattern;

/**
 * JSON output khớp Node:
 * role, rank, point, _id, username, name, email, phone, createdAt, updatedAt, __v
 */
@Document(collection = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonPropertyOrder({
        "role","rank","point","_id","username","name","email","phone","createdAt","updatedAt","__v"
})
public class User {

    // _id
    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank @Size(max = 255)
    private String name;

    @NotBlank @Size(max = 100) @Indexed(unique = true)
    private String username; // lowercase

    @NotBlank @Email @Size(max = 255) @Indexed(unique = true)
    private String email; // lowercase

    @JsonIgnore
    @Size(min = 7)
    private String passwordHash;

    @Builder.Default
    private String role = "guest"; // guest|admin|superadmin

    @Builder.Default
    private String rank = "Member"; // Member|VIP

    @Builder.Default
    @Min(0)
    private Integer point = 0;

    private String facebook;
    private String google;

    @Size(max = 25) @Indexed(unique = true, sparse = true)
    private String phone;

    @JsonIgnore
    private String imageurl;

    @Builder.Default
    @JsonIgnore
    private Set<String> tokens = new HashSet<>();

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

    // __v (version như Mongoose)
    @Version
    @JsonProperty("__v")
    private Long version;

    /* Helpers tương tự Mongoose */
    public void normalize() {
        if (username != null) username = username.trim().toLowerCase(Locale.ROOT);
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
        if (phone != null) phone = phone.trim();
    }
    public void validatePhone() {
        if (phone == null || phone.isBlank()) return;
        Pattern p = Pattern.compile("^[+]?\\d{8,15}$");
        if (!p.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone is invalid");
        }
    }
}
