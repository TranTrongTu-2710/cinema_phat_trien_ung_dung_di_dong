package com.example.demo.movie;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Locale;

/**
 * Mapping sát schema Mongoose (movie.js)
 * - Các trường text đều được trim + lowercase như Mongoose.
 * - KHÔNG thêm createdAt/updatedAt/__v để JSON khớp với Node router hiện tại.
 */
@Document(collection = "movies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Movie {

    @Id
    @JsonProperty("_id")
    private String id;

    @NotBlank
    private String title;

    private String image;

    @NotBlank
    private String language;

    @NotBlank
    private String genre;

    @NotBlank
    private String director;

    @NotBlank
    private String cast;

    @NotBlank
    private String description;

    @NotNull
    private Integer duration;

    @Builder.Default
    private Integer active = 1; // enum [0,1]

    @Builder.Default
    private Integer hot = 1; // enum [0,1]

    @Builder.Default
    private Integer minAge = 5;

    @NotNull
    private java.util.Date releaseDate;

    @NotNull
    private java.util.Date endDate;

    /* ===== helpers mô phỏng trim + lowercase của Mongoose ===== */
    public void normalizeLowercase() {
        if (title != null)       title = title.trim().toLowerCase(Locale.ROOT);
        if (language != null)    language = language.trim().toLowerCase(Locale.ROOT);
        if (genre != null)       genre = genre.trim().toLowerCase(Locale.ROOT);
        if (director != null)    director = director.trim().toLowerCase(Locale.ROOT);
        if (cast != null)        cast = cast.trim().toLowerCase(Locale.ROOT);
        if (description != null) description = description.trim().toLowerCase(Locale.ROOT);
        if (image != null)       image = image.trim();
    }
}
