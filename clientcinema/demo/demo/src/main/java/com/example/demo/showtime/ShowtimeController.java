package com.example.demo.showtime;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Mirror 1:1 routers/showtime (Node):
 *  POST   /showtimes            -> 201 showtime | 400 e
 *  GET    /showtimes            -> [showtime] | 400
 *  GET    /showtimes/:id        -> 200 | 404 | 400
 *  PATCH  /showtimes/:id        -> 200 | 404 | 400 {error:'Invalid updates!'}
 *  DELETE /showtimes/:id        -> 200 | 404 | 400
 *  GET    /showtimes/by_movie/:movieId? -> {items:[...]} | 400 {"message": "..."}
 */
@RestController
@RequiredArgsConstructor
public class ShowtimeController {
    private final ShowtimeRepository repo;

    @PostMapping("/showtimes")
    public ResponseEntity<?> create(@RequestBody Showtime body) {
        try {
            if (!StringUtils.hasText(body.getStartAt()) ||
                    body.getStartDate() == null ||
                    body.getEndDate() == null ||
                    !StringUtils.hasText(body.getMovieId()) ||
                    !StringUtils.hasText(body.getCinemaId())) {
                return ResponseEntity.badRequest().body(new RuntimeException("Validation failed"));
            }
            Showtime saved = repo.save(body);
            return ResponseEntity.status(201).body(saved);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.badRequest().body(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @GetMapping("/showtimes")
    public ResponseEntity<?> findAll() {
        try { return ResponseEntity.ok(repo.findAll()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }

    @GetMapping("/showtimes/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            return repo.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @PatchMapping("/showtimes/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        Set<String> allowed = Set.of("startAt","startDate","endDate","movieId","cinemaId");
        for (String k : body.keySet()) if (!allowed.contains(k))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));

        try {
            Optional<Showtime> os = repo.findById(id);
            if (os.isEmpty()) return ResponseEntity.status(404).build();
            Showtime s = os.get();
            if (body.containsKey("startAt"))   s.setStartAt(Objects.toString(body.get("startAt"), s.getStartAt()));
            if (body.containsKey("startDate")) s.setStartDate(new Date((Long) body.get("startDate")));
            if (body.containsKey("endDate"))   s.setEndDate(new Date((Long) body.get("endDate")));
            if (body.containsKey("movieId"))   s.setMovieId(Objects.toString(body.get("movieId"), s.getMovieId()));
            if (body.containsKey("cinemaId"))  s.setCinemaId(Objects.toString(body.get("cinemaId"), s.getCinemaId()));
            return ResponseEntity.ok(repo.save(s));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @DeleteMapping("/showtimes/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<Showtime> os = repo.findById(id);
            if (os.isEmpty()) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            return ResponseEntity.ok(os.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /showtimes/by_movie/:movieId?
    @GetMapping({"/showtimes/by_movie", "/showtimes/by_movie/{movieId}"})
    public ResponseEntity<?> byMovie(@PathVariable(required = false) String movieId,
                                     @RequestParam(value = "includeInactive", required = false, defaultValue = "0") String includeInactive) {
        try {
            String rawId = movieId != null ? movieId : null;

            // Validate như Node: nếu truyền nhưng sai định dạng -> 400 {message:'Invalid userId'}
            String idOrNull = null;
            if (rawId != null) {
                if (ObjectId.isValid(rawId)) idOrNull = rawId;
                else return ResponseEntity.badRequest().body(Map.of("message","Invalid userId"));
            }

            // Lọc: (movieId không tồn tại) OR (movieId == idOrNull) — đúng theo router
            // (Ở đây dùng lọc trong bộ nhớ để giữ ít file; có thể đổi sang MongoTemplate nếu dataset lớn)
            List<Showtime> all = repo.findAll();
            List<Showtime> items = new ArrayList<>();
            for (Showtime s : all) {
                boolean noMovie = s.getMovieId() == null || s.getMovieId().isBlank();
                if (idOrNull == null) {
                    if (noMovie) items.add(s);
                } else {
                    if (noMovie || idOrNull.equals(s.getMovieId())) items.add(s);
                }
            }

            // sort như Node: { date: -1, createdAt: -1 } (field "date" có thể không tồn tại -> bỏ qua)
            items.sort((a,b) -> {
                Date ca = b.getCreatedAt(), cb = a.getCreatedAt(); // đảo để DESC
                if (ca != null && cb != null) return ca.compareTo(cb);
                return 0;
            });

            return ResponseEntity.ok(Map.of("items", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
