package com.example.demo.news;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository repo;
    private final MongoTemplate mongo;

    /* ========== 1) CREATE — POST /news (auth.enhance) ========== */
    @PostMapping("/news")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body) {
        try {
            String title = Objects.toString(body.get("title"), "").trim();
            String description = Objects.toString(body.get("description"), "").trim();
            Object dateObj = body.get("date");
            Integer active = body.get("active") instanceof Number n ? n.intValue() : 1;

            if (!StringUtils.hasText(title) || !StringUtils.hasText(description) || dateObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message","Validation failed"));
            }

            String userId = null;
            if (body.containsKey("userId") && body.get("userId") != null && !Objects.toString(body.get("userId")).isBlank()) {
                String raw = Objects.toString(body.get("userId"));
                if (!ObjectId.isValid(raw)) return ResponseEntity.badRequest().body(Map.of("message","Invalid userId"));
                userId = raw;
            }

            Date date = (dateObj instanceof String s) ? new Date(s)
                    : (dateObj instanceof Number n) ? new Date(n.longValue())
                    : (dateObj instanceof Date d) ? d : null;

            if (date == null) return ResponseEntity.badRequest().body(Map.of("message","Invalid date"));

            News doc = News.builder()
                    .title(title)
                    .description(description)
                    .date(date)
                    .active(active)
                    .userId(userId) // null = global news
                    .build();
            News saved = repo.save(doc);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* ========== 2) GET /news/public — chỉ news không có userId; mặc định active=1 ========== */
    @GetMapping("/news/public")
    public ResponseEntity<?> publicNews(@RequestParam(value="includeInactive", defaultValue = "0") String includeInactive) {
        try {
            Query q = new Query();
            q.addCriteria(Criteria.where("userId").exists(false)); // KHÔNG có userId (global)
            if (!"1".equals(String.valueOf(includeInactive))) {
                q.addCriteria(Criteria.where("active").is(1));
            }
            q.with(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt")));
            List<News> items = mongo.find(q, News.class);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* ========== 3) GET /news/visible/:userId? — (no-userId) OR (userId = id) ========== */
    @GetMapping({"/news/visible", "/news/visible/{userId}"})
    public ResponseEntity<?> visibleNews(@PathVariable(required = false) String userId,
                                         @RequestParam(value="includeInactive", defaultValue = "0") String includeInactive) {
        try {
            String uid = null;
            if (userId != null) {
                if (!ObjectId.isValid(userId)) return ResponseEntity.badRequest().body(Map.of("message","Invalid userId"));
                uid = userId;
            }

            Query q = new Query();
            if (uid != null) {
                q.addCriteria(new Criteria().orOperator(
                        Criteria.where("userId").exists(false),
                        Criteria.where("userId").is(uid)
                ));
            } else {
                q.addCriteria(Criteria.where("userId").exists(false));
            }
            if (!"1".equals(String.valueOf(includeInactive))) {
                q.addCriteria(Criteria.where("active").is(1));
            }
            q.with(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt")));
            List<News> items = mongo.find(q, News.class);
            return ResponseEntity.ok(Map.of("items", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* ========== 4) GET /news/all — lấy TẤT CẢ (auth.enhance) ========== */
    @GetMapping("/news/all")
    public ResponseEntity<?> allNews() {
        try {
            Query q = new Query().with(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt")));
            return ResponseEntity.ok(mongo.find(q, News.class));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* ========== 5) PATCH /news/:id — allowed updates, unset userId nếu null/"" ========== */
    @PatchMapping("/news/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        try {
            if (!ObjectId.isValid(id)) return ResponseEntity.badRequest().body(Map.of("message","Invalid id"));

            Set<String> allowed = Set.of("title","description","date","active","userId");
            for (String k : body.keySet()) if (!allowed.contains(k))
                return ResponseEntity.badRequest().body(Map.of("message","Invalid updates!"));

            Update u = new Update();
            // userId: cho phép xoá (global) khi null/""
            if (body.containsKey("userId")) {
                Object v = body.get("userId");
                if (v == null || Objects.toString(v).isBlank()) {
                    u.unset("userId");
                } else {
                    String raw = Objects.toString(v);
                    if (!ObjectId.isValid(raw)) return ResponseEntity.badRequest().body(Map.of("message","Invalid userId"));
                    u.set("userId", raw);
                }
            }
            if (body.containsKey("title"))       u.set("title", Objects.toString(body.get("title"), ""));
            if (body.containsKey("description")) u.set("description", Objects.toString(body.get("description"), ""));
            if (body.containsKey("active"))      u.set("active", ((Number)body.get("active")).intValue());
            if (body.containsKey("date")) {
                Object dv = body.get("date");
                Date d = (dv instanceof String s) ? new Date(s)
                        : (dv instanceof Number n) ? new Date(n.longValue())
                        : (dv instanceof Date di) ? di : null;
                if (d == null) return ResponseEntity.badRequest().body(Map.of("message","Invalid date"));
                u.set("date", d);
            }

            Query q = new Query(Criteria.where("_id").is(new ObjectId(id)));
            var res = mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), News.class);
            if (res == null) return ResponseEntity.status(404).build();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
