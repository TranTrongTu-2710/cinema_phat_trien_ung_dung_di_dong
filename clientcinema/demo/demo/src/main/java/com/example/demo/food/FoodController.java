package com.example.demo.food;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class FoodController {

    private final FoodRepository repo;

    // CREATE  (auth.enhance)
    @PostMapping("/foods")
    public ResponseEntity<?> create(@RequestBody Food body) {
        try {
            if (!StringUtils.hasText(body.getName()) ||
                    body.getPrice() == null ||
                    !StringUtils.hasText(body.getDescription()) ||
                    body.getPathItem() == null) {
                // res.status(400).send({ error: e.message }) trong Node
                return ResponseEntity.badRequest().body(Map.of("error", "Validation failed"));
            }
            Food saved = repo.save(body);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // READ ALL
    @GetMapping("/foods")
    public ResponseEntity<?> findAll() {
        try {
            // Node có sort({createdAt:-1}) nhưng schema không bật timestamps — giữ trả thẳng danh sách
            return ResponseEntity.ok(repo.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // READ ONE
    @GetMapping("/foods/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        try {
            return repo.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UPDATE (auth.enhance) — chỉ cho phép allowed fields
    @PatchMapping("/foods/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        Set<String> allowed = Set.of("name","price","description","pathItem","image","category");
        for (String k: body.keySet()) if (!allowed.contains(k))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));

        try {
            Optional<Food> of = repo.findById(id);
            if (of.isEmpty()) return ResponseEntity.status(404).build();

            Food f = of.get();
            if (body.containsKey("name"))        f.setName(Objects.toString(body.get("name"), f.getName()));
            if (body.containsKey("price"))       f.setPrice(((Number)body.get("price")).intValue());
            if (body.containsKey("description")) f.setDescription(Objects.toString(body.get("description"), f.getDescription()));
            if (body.containsKey("pathItem"))    f.setPathItem((List<Object>) body.get("pathItem"));
            if (body.containsKey("image"))       f.setImage(Objects.toString(body.get("image"), f.getImage()));

            return ResponseEntity.ok(repo.save(f));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE (auth.enhance)
    @DeleteMapping("/foods/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<Food> of = repo.findById(id);
            if (of.isEmpty()) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            return ResponseEntity.ok(of.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
