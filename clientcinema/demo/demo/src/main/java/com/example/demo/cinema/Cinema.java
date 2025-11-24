package com.example.demo.cinema;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Locale;

/** Mapping sát cinemaSchema (Node) */
@Document(collection = "cinemas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cinema {

    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank
    private String name;

    @NotNull
    private Integer ticketPrice;

    @NotBlank
    private String city;          // lowercase + trim

    @NotNull
    private List<Object> seats;   // Schema.Types.Mixed[]

    @NotNull
    private Integer seatsAvailable;

    private String image;

    public void normalizeLowercase() {
        if (city != null) city = city.trim().toLowerCase(Locale.ROOT);
        if (name != null) name = name.trim();
        if (image != null) image = image.trim();
    }
}
