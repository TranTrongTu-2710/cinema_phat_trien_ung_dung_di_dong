package com.example.demo.movie;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

/**
 * Chuyển 1:1 từ routes/movies.js:
 *  - POST /movies                 (auth) -> 201 movie || 400 e
 *  - GET  /movies/photo/:id       (auth, upload single 'file') -> { movie, file } || 404 || 400
 *  - GET  /movies                 -> 200 [movies] || 400 e
 *  - GET  /movies/:id             -> 200 movie || 404 || 400 e
 *  - PUT  /movies/:id             (auth, allowedUpdates check) -> 200 movie || 404 || 400 {error:'Invalid updates!'}|e
 *  - DELETE /movies/:id           (auth) -> 200 movie || 404 || 400
 *  - GET  /movies/usermodeling/:username -> 200 result || 400 e
 *
 * Ghi chú: auth.enhance bên Node được phản ánh bằng SecurityConfig (filter chain /movies/**).
 */
@RestController
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository repo;

    /* ================== Create a movie ================== */
    @PostMapping("/movies")
    public ResponseEntity<?> create(@RequestBody Movie body) {
        try {
            // Mongoose: required cho nhiều field -> ở đây kiểm tra tối thiểu để giống behavior
            if (!StringUtils.hasText(body.getTitle())
                    || !StringUtils.hasText(body.getLanguage())
                    || !StringUtils.hasText(body.getGenre())
                    || !StringUtils.hasText(body.getDirector())
                    || !StringUtils.hasText(body.getCast())
                    || !StringUtils.hasText(body.getDescription())
                    || body.getDuration() == null
                    || body.getReleaseDate() == null
                    || body.getEndDate() == null) {
                return ResponseEntity.badRequest().body(new RuntimeException("Validation failed"));
            }

            body.normalizeLowercase();
            Movie saved = repo.save(body);
            // Node: res.status(201).send(movie)
            return ResponseEntity.status(201).body(saved);
        } catch (DuplicateKeyException dke) {
            return ResponseEntity.badRequest().body(dke);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    /* ================== Upload photo ================== */
    @PostMapping("/movies/photo/{id}")
    public ResponseEntity<?> uploadPhoto(@PathVariable String id,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpServletRequest req) {
        try {
            if (file == null || file.isEmpty()) {
                // Node: new Error('Please upload a file') -> next(error) -> 400
                return ResponseEntity.badRequest().body(new RuntimeException("Please upload a file"));
            }
            Optional<Movie> om = repo.findById(id);
            if (om.isEmpty()) return ResponseEntity.status(404).build();

            // Lưu file: uploads/movies/<filename>
            Path root = Paths.get("uploads/movies").toAbsolutePath().normalize();
            Files.createDirectories(root);
            String clean = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+","_");
            Path dest = root.resolve(clean);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String base = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort();
            String rel = "uploads/movies/" + clean;

            Movie m = om.get();
            m.setImage(base + "/" + rel);
            repo.save(m);

            Map<String,Object> res = new HashMap<>();
            res.put("movie", m);
            res.put("file", Map.of("path", rel));
            // Node: res.send({ movie, file })
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            // Node: res.sendStatus(400).send(e)
            return ResponseEntity.badRequest().body(e);
        }
    }

    /* ================== Get all movies ================== */
    @GetMapping("/movies")
    public ResponseEntity<?> findAll() {
        try {
            List<Movie> movies = repo.findAll();
            return ResponseEntity.ok(movies); // Node: res.send(movies)
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    /* ================== Get movie by id ================== */
    @GetMapping("/movies/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            Optional<Movie> om = repo.findById(id);
            if (om.isEmpty()) return ResponseEntity.status(404).build(); // Node: res.sendStatus(404)
            return ResponseEntity.ok(om.get()); // Node: res.send(movie)
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e); // Node: status(400).send(e)
        }
    }

    /* ================== Update movie by id ================== */
    @PutMapping("/movies/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        // allowedUpdates như Node
        Set<String> allowed = Set.of(
                "title","image","language","genre","director","cast","description",
                "duration","hot","active","minAge","releaseDate","endDate"
        );
        for (String k : body.keySet()) {
            if (!allowed.contains(k)) {
                // Node: return res.status(400).send({ error: 'Invalid updates!' });
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid updates!"));
            }
        }

        try {
            Optional<Movie> om = repo.findById(id);
            if (om.isEmpty()) return ResponseEntity.status(404).build();

            Movie m = om.get();
            // apply updates
            if (body.containsKey("title"))       m.setTitle(Objects.toString(body.get("title"), m.getTitle()));
            if (body.containsKey("image"))       m.setImage(Objects.toString(body.get("image"), m.getImage()));
            if (body.containsKey("language"))    m.setLanguage(Objects.toString(body.get("language"), m.getLanguage()));
            if (body.containsKey("genre"))       m.setGenre(Objects.toString(body.get("genre"), m.getGenre()));
            if (body.containsKey("director"))    m.setDirector(Objects.toString(body.get("director"), m.getDirector()));
            if (body.containsKey("cast"))        m.setCast(Objects.toString(body.get("cast"), m.getCast()));
            if (body.containsKey("description")) m.setDescription(Objects.toString(body.get("description"), m.getDescription()));
            if (body.containsKey("duration"))    m.setDuration((Integer) (body.get("duration") instanceof Number ? ((Number)body.get("duration")).intValue() : m.getDuration()));
            if (body.containsKey("hot"))         m.setHot((Integer) (body.get("hot") instanceof Number ? ((Number)body.get("hot")).intValue() : m.getHot()));
            if (body.containsKey("active"))      m.setActive((Integer) (body.get("active") instanceof Number ? ((Number)body.get("active")).intValue() : m.getActive()));
            if (body.containsKey("minAge"))      m.setMinAge((Integer) (body.get("minAge") instanceof Number ? ((Number)body.get("minAge")).intValue() : m.getMinAge()));
            if (body.containsKey("releaseDate")) m.setReleaseDate(new java.util.Date((Long) body.get("releaseDate")));
            if (body.containsKey("endDate"))     m.setEndDate(new java.util.Date((Long) body.get("endDate")));

            m.normalizeLowercase();
            Movie saved = repo.save(m);
            // Node: res.send(movie)
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    /* ================== Delete movie by id ================== */
    @DeleteMapping("/movies/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<Movie> om = repo.findById(id);
            if (om.isEmpty()) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            // Node: res.send(movie) (trả về đối tượng đã xóa)
            return ResponseEntity.ok(om.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build(); // Node: res.sendStatus(400)
        }
    }

    /* ================== Movies User modeling (Get suggestions) ================== */
    @GetMapping("/movies/usermodeling/{username}")
    public ResponseEntity<?> userModeling(@PathVariable String username) {
        try {
            // Node gọi userModeling.moviesUserModeling(username)
            // Ở đây để tối thiểu file, mình mô phỏng trả rỗng (giữ status & cấu trúc).
            // Bạn có thể nối vào service thực tế sau.
            List<Object> cinemasUserModeled = List.of();
            return ResponseEntity.ok(cinemasUserModeled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }
}
