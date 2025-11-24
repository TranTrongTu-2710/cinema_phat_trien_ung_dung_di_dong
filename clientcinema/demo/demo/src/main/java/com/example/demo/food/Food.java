package com.example.demo.food;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/** Mirror schema Mongoose: name, price, description, pathItem[], image */
@Document(collection = "food")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Food {

    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank
    private String name;

    @NotNull
    private Integer price;

    @NotBlank
    private String description;

    /** Mixed[] trong Mongoose -> dùng List<Object> ở Java */
    @NotNull
    private List<Object> pathItem;

    private String image;
}
