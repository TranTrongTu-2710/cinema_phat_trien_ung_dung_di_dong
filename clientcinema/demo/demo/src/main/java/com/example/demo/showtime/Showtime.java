package com.example.demo.showtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/** Mapping theo cách router sử dụng (startAt, startDate, endDate, movieId, cinemaId) */
@Document(collection = "showtimes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Showtime {

    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank
    private String startAt;

    @NotNull
    private Date startDate;

    @NotNull
    private String name;

    @NotNull
    private Date endDate;

    @NotBlank
    private String movieId;

    @NotBlank
    private String cinemaId;

    // để sort như Node có dùng createdAt
    @CreatedDate
    private Date createdAt;
}
