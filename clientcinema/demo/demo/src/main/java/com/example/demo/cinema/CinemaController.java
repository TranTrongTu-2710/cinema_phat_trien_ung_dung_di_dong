package com.example.demo.cinema;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.*;
import java.util.*;

/**
 * Mirror 1:1 routers/cinema (Node):
 *  POST   /cinemas                -> 201 cinema | 400 e
 *  POST   /cinemas/photo/:id      -> { cinema, file } | 404 | 400
 *  GET    /cinemas                -> [cinema] | 400
 *  GET    /cinemas/:id            -> 200 | 404 | 400
 *  PATCH  /cinemas/:id            -> 200 | 404 | 400 {error:'Invalid updates!'}
 *  DELETE /cinemas/:id            -> 200 | 404 | 400
 *  GET    /cinemas/usermodeling/:username -> 200 result | 400
 */
@RestController
@RequiredArgsConstructor
public class CinemaController {
    private final CinemaRepository repo;

    @PostMapping("/cinemas")
    public ResponseEntity<?> create(@RequestBody Cinema body) {
        try {
            if (!StringUtils.hasText(body.getName()) ||
                    body.getTicketPrice() == null ||
                    !StringUtils.hasText(body.getCity()) ||
                    body.getSeats() == null ||
                    body.getSeatsAvailable() == null) {
                return ResponseEntity.badRequest().body(new RuntimeException("Validation failed"));
            }
            body.normalizeLowercase();
            Cinema saved = repo.save(body);
            return ResponseEntity.status(201).body(saved);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.badRequest().body(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @PostMapping("/cinemas/photo/{id}")
    public ResponseEntity<?> uploadPhoto(@PathVariable String id,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpServletRequest req) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new RuntimeException("Please upload a file"));
            }
            Optional<Cinema> oc = repo.findById(id);
            if (oc.isEmpty()) return ResponseEntity.status(404).build();

            Path root = Paths.get("uploads/cinemas").toAbsolutePath().normalize();
            Files.createDirectories(root);
            String clean = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+","_");
            Path dest = root.resolve(clean);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String base = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort();
            String rel = "uploads/cinemas/" + clean;

            Cinema c = oc.get();
            c.setImage(base + "/" + rel);
            repo.save(c);

            Map<String,Object> res = new HashMap<>();
            res.put("cinema", c);
            res.put("file", Map.of("path", rel));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @GetMapping("/cinemas")
    public ResponseEntity<?> findAll() {
        try { return ResponseEntity.ok(repo.findAll()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e); }
    }

    @GetMapping("/cinemas/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            return repo.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @PatchMapping("/cinemas/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        Set<String> allowed = Set.of("name","ticketPrice","city","seats","seatsAvailable");
        for (String k : body.keySet()) if (!allowed.contains(k))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));

        try {
            Optional<Cinema> oc = repo.findById(id);
            if (oc.isEmpty()) return ResponseEntity.status(404).build();
            Cinema c = oc.get();
            if (body.containsKey("name")) c.setName(Objects.toString(body.get("name"), c.getName()));
            if (body.containsKey("ticketPrice")) c.setTicketPrice(((Number)body.get("ticketPrice")).intValue());
            if (body.containsKey("city")) c.setCity(Objects.toString(body.get("city"), c.getCity()));
            if (body.containsKey("seats")) c.setSeats((List<Object>) body.get("seats"));
            if (body.containsKey("seatsAvailable")) c.setSeatsAvailable(((Number)body.get("seatsAvailable")).intValue());
            c.normalizeLowercase();
            return ResponseEntity.ok(repo.save(c));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @DeleteMapping("/cinemas/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<Cinema> oc = repo.findById(id);
            if (oc.isEmpty()) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            return ResponseEntity.ok(oc.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/cinemas/usermodeling/{username}")
    public ResponseEntity<?> userModeling(@PathVariable String username) {
        try {
            // Giữ nguyên response shape; nếu chưa có thuật toán userModeling, trả danh sách rỗng
            List<Object> cinemasUserModeled = List.of();
            return ResponseEntity.ok(cinemasUserModeled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }
}
