package com.example.datve.movie;

import android.os.Parcel;
import android.os.Parcelable; // Quan trọng: Đảm bảo có import này
import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

// === SỬA LỖI Ở ĐÂY: Thêm "implements Parcelable" ===
public class Movie implements Parcelable {
    private String id;
    private String title;
    private String image;
    private String genre;
    private int duration;
    private String releaseDate;
    private String description;
    private String director;
    private String cast;
    private int active;
    private int hot;
    private int minAge;

    public Movie(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("_id");
        this.title = jsonObject.getString("title");
        this.image = jsonObject.getString("image");
        this.genre = jsonObject.optString("genre", "N/A");
        this.duration = jsonObject.getInt("duration");
        this.releaseDate = jsonObject.optString("releaseDate");
        this.description = jsonObject.optString("description", "Không có mô tả.");
        this.director = jsonObject.optString("director", "Không có thông tin.");
        this.cast = jsonObject.optString("cast", "Không có thông tin.");
        this.active = jsonObject.getInt("active");
        this.hot = jsonObject.optInt("hot", 0);
        this.minAge = jsonObject.optInt("minAge", 0);
    }

    // --- CÁC PHƯƠNG THỨC BẮT BUỘC CỦA PARCELABLE ---

    protected Movie(Parcel in) {
        id = in.readString();
        title = in.readString();
        image = in.readString();
        genre = in.readString();
        duration = in.readInt();
        releaseDate = in.readString();
        description = in.readString();
        director = in.readString();
        cast = in.readString();
        active = in.readInt();
        hot = in.readInt();
        minAge = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(image);
        dest.writeString(genre);
        dest.writeInt(duration);
        dest.writeString(releaseDate);
        dest.writeString(description);
        dest.writeString(director);
        dest.writeString(cast);
        dest.writeInt(active);
        dest.writeInt(hot);
        dest.writeInt(minAge);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    // --- GETTERS (Giữ nguyên không đổi) ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getGenre() { return genre; }
    public int getDuration() { return duration; }
    public String getReleaseDate() { return releaseDate; }
    public String getDescription() { return description; }
    public String getDirector() { return director; }
    public String getCast() { return cast; }
    public int getActive() { return active; }
    public int getHot() { return hot; }
    public int getMinAge() { return minAge; }
}
