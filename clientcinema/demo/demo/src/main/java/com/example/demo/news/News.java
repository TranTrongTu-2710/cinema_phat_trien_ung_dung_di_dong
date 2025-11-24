package com.example.demo.news;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/** Mirror schema Mongoose (title, description, date, active [0|1], userId?) */
@Document(collection = "news")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class News {

    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Date date;

    @Builder.Default
    private Integer active = 1; // enum [0,1]

    // userId có thể không tồn tại => để null nếu là global news
    private String userId;

    // để sort phụ giống Node (date, createdAt)
    @CreatedDate
    private Date createdAt;
}
