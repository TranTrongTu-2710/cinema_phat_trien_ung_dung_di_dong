package com.example.demo.user;

import com.example.demo.user.SecurityConfig.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

/**
 * Router Java mirror 1-1 responses với users.js:
 * - Body & status giữ nguyên (201 {user, token}, 200 {}, 400 {error: "..."} ...)
 */
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository repo;
    private final JwtService jwt;

    private static String lower(String s){ return s==null?null:s.trim().toLowerCase(Locale.ROOT); }
    private static boolean isSuper(User u){ return u!=null && "superadmin".equalsIgnoreCase(u.getRole()); }

    /* ========================== Helpers ========================== */

    private ResponseEntity<?> bad(String msg){ return ResponseEntity.badRequest().body(Map.of("error", msg)); }
    private ResponseEntity<?> badLogin(){ return ResponseEntity.badRequest().body(Map.of("error", Map.of("message","You have entered an invalid username or password"))); }

    private void applyAllowed(User u, Map<String, Object> body, boolean includeRole){
        Set<String> allowed = includeRole
                ? Set.of("name","phone","username","email","password","role")
                : Set.of("name","phone","username","email","password");
        for (String k : body.keySet()){
            if (!allowed.contains(k)) throw new IllegalArgumentException("Invalid updates!");
        }
        if (body.containsKey("name")) u.setName(Objects.toString(body.get("name"), u.getName()));
        if (body.containsKey("phone")) u.setPhone(Objects.toString(body.get("phone"), u.getPhone()));
        if (body.containsKey("username")) u.setUsername(lower(Objects.toString(body.get("username"), u.getUsername())));
        if (body.containsKey("email")) u.setEmail(lower(Objects.toString(body.get("email"), u.getEmail())));
        if (body.containsKey("password")) u.setPasswordHash(BCrypt.hashpw(Objects.toString(body.get("password")), BCrypt.gensalt()));
        if (includeRole && body.containsKey("role")) u.setRole(Objects.toString(body.get("role"), u.getRole()));
        u.normalize(); u.validatePhone();
    }

    /* ========================== Routes ========================== */

    // POST /users (Create a user)
    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String,Object> body) {
        try {
            // users.js: nếu có role trong body => 400 'you cannot set role property.'
            if (body.containsKey("role")) {
                return ResponseEntity.badRequest().body(new RuntimeException("you cannot set role property."));
            }
            String name = Objects.toString(body.get("name"), null);
            String username = lower(Objects.toString(body.get("username"), null));
            String email = lower(Objects.toString(body.get("email"), null));
            String password = Objects.toString(body.get("password"), null);
            String phone = Objects.toString(body.get("phone"), null);

            if (!StringUtils.hasText(name) || !StringUtils.hasText(username) || !StringUtils.hasText(email)) {
                return ResponseEntity.badRequest().body(new RuntimeException("Validation failed"));
            }

            User u = User.builder()
                    .name(name)
                    .username(username)
                    .email(email)
                    .phone(phone)
                    .role("guest")
                    .rank("Member")
                    .point(0)
                    .build();
            u.normalize(); u.validatePhone();
            if (StringUtils.hasText(password)) {
                if (password.length() < 7 || password.toLowerCase(Locale.ROOT).contains("password")) {
                    return ResponseEntity.badRequest().body(new RuntimeException("Password should not contain word: password"));
                }
                u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            }

            u = repo.save(u);
            String token = jwt.sign(u.getId());
            u.getTokens().add(token);
            u = repo.save(u);

            Map<String,Object> res = new HashMap<>();
            res.put("user", u);
            res.put("token", token);
            return ResponseEntity.status(201).body(res);
        } catch (DuplicateKeyException dke) {
            return ResponseEntity.badRequest().body(dke); // giữ kiểu trả về "send(e)" như Node
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e); // giữ "send(e)"
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // POST /users/photo/:id
    @PostMapping("/users/photo/{id}")
    public ResponseEntity<?> uploadPhoto(@PathVariable String id,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpServletRequest req) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new RuntimeException("Please upload a file"));
            }
            Optional<User> ou = repo.findById(id);
            if (ou.isEmpty()) return ResponseEntity.status(404).build();

            Path root = Paths.get("uploads/users").toAbsolutePath().normalize();
            Files.createDirectories(root);
            String clean = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+","_");
            Path dest = root.resolve(clean);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            User u = ou.get();
            String base = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort();
            String rel = "uploads/users/" + clean;
            u.setImageurl(base + "/" + rel);
            repo.save(u);

            Map<String,Object> res = new HashMap<>();
            res.put("user", u);
            res.put("file", Map.of("path", rel));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            // Node: res.sendStatus(400).send(e) (gần tương đương 400)
            return ResponseEntity.badRequest().body(e);
        }
    }

    // POST /users/login
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody Map<String,Object> body) {
        try {
            String username = lower(Objects.toString(body.get("username"), null));
            String password = Objects.toString(body.get("password"), null);
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) return badLogin();

            Optional<User> ou = repo.findByUsername(username);
            if (ou.isEmpty()) return badLogin();
            User u = ou.get();
            if (u.getPasswordHash() == null || !BCrypt.checkpw(password, u.getPasswordHash())) return badLogin();

            String token = jwt.sign(u.getId());
            u.getTokens().add(token);
            repo.save(u);

            return ResponseEntity.ok(Map.of("user", u, "token", token));
        } catch (Exception e) {
            return badLogin();
        }
    }

    // POST /users/login/facebook
    @PostMapping("/users/login/facebook")
    public ResponseEntity<?> loginFacebook(@RequestBody Map<String,String> body) {
        String email = lower(body.get("email"));
        String userID = body.get("userID");
        String name = body.get("name");
        String genUsername = ((name == null ? "user" : name.replaceAll("\\s+","")) + userID).toLowerCase(Locale.ROOT);

        try {
            Optional<User> found = repo.findByFacebook(userID);
            if (found.isEmpty()) {
                User u = User.builder()
                        .name(name)
                        .username(genUsername)
                        .email(email)
                        .facebook(userID)
                        .role("guest").rank("Member").point(0)
                        .build();
                u.normalize();
                User saved = repo.save(u);
                String token = jwt.sign(saved.getId());
                saved.getTokens().add(token);
                repo.save(saved);
                return ResponseEntity.status(201).body(Map.of("user", saved, "token", token));
            } else {
                User u = found.get();
                String token = jwt.sign(u.getId());
                u.getTokens().add(token);
                repo.save(u);
                return ResponseEntity.ok(Map.of("user", u, "token", token));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // POST /users/login/google
    @PostMapping("/users/login/google")
    public ResponseEntity<?> loginGoogle(@RequestBody Map<String,String> body) {
        String email = lower(body.get("email"));
        String googleId = body.get("googleId");
        String name = body.get("name");
        String genUsername = ((name == null ? "user" : name.replaceAll("\\s+","")) + googleId).toLowerCase(Locale.ROOT);

        try {
            Optional<User> found = repo.findByGoogle(googleId);
            if (found.isEmpty()) {
                User u = User.builder()
                        .name(name)
                        .username(genUsername)
                        .email(email)
                        .google(googleId)
                        .role("guest").rank("Member").point(0)
                        .build();
                u.normalize();
                User saved = repo.save(u);
                String token = jwt.sign(saved.getId());
                saved.getTokens().add(token);
                repo.save(saved);
                return ResponseEntity.status(201).body(Map.of("user", saved, "token", token));
            } else {
                User u = found.get();
                String token = jwt.sign(u.getId());
                u.getTokens().add(token);
                repo.save(u);
                return ResponseEntity.ok(Map.of("user", u, "token", token));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // POST /users/logout
    @PostMapping("/users/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            User me = (User) request.getAttribute("currentUser");
            String token = (String) request.getAttribute("currentToken");
            if (me == null) return ResponseEntity.status(401).body(Map.of("error","Please authenticate."));
            me.getTokens().remove(token);
            repo.save(me);
            return ResponseEntity.ok(Map.of()); // {}
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // POST /users/logoutAll
    @PostMapping("/users/logoutAll")
    public ResponseEntity<?> logoutAll(HttpServletRequest req) {
        try {
            User me = (User) req.getAttribute("currentUser");
            if (me == null) return ResponseEntity.status(401).body(Map.of("error","Please authenticate."));
            me.getTokens().clear();
            repo.save(me);
            // users.js: res.send() => 200 body rỗng
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // GET /users (get all users) – only superadmin
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest req) {
        User me = (User) req.getAttribute("currentUser");
        if (!isSuper(me))
            return bad("Only the god can see all the users!");
        try {
            return ResponseEntity.ok(repo.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // GET /users/me
    @GetMapping("/users/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        try {
            User me = (User) req.getAttribute("currentUser");
            return ResponseEntity.ok(me);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // GET /users/:id – only superadmin
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, HttpServletRequest req) {
        User me = (User) req.getAttribute("currentUser");
        if (!isSuper(me))
            return bad("Only the god can see the user!");
        try {
            return repo.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
        } catch (Exception e) {
            // users.js: res.sendStatus(400)
            return ResponseEntity.badRequest().build();
        }
    }

    // PATCH /users/me
    @PatchMapping("/users/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        try {
            User me = (User) req.getAttribute("currentUser");
            if (me == null) return ResponseEntity.status(401).body(Map.of("error","Please authenticate."));
            try {
                applyAllowed(me, body, false);
            } catch (IllegalArgumentException inv) {
                return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));
            }
            repo.save(me);
            return ResponseEntity.ok(me);
        } catch (DuplicateKeyException dke) {
            return ResponseEntity.badRequest().body(dke);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // PATCH /users/:id – only superadmin
    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateById(@PathVariable String id, @RequestBody Map<String,Object> body, HttpServletRequest req) {
        User me = (User) req.getAttribute("currentUser");
        if (!isSuper(me))
            return bad("Only the god can update the user!");
        try {
            Optional<User> ou = repo.findById(id);
            if (ou.isEmpty()) return ResponseEntity.status(404).build();
            User u = ou.get();
            try {
                applyAllowed(u, body, true);
            } catch (IllegalArgumentException inv) {
                return ResponseEntity.badRequest().body(Map.of("error","Invalid updates!"));
            }
            repo.save(u);
            return ResponseEntity.ok(u);
        } catch (DuplicateKeyException dke) {
            return ResponseEntity.badRequest().body(dke);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    // DELETE /users/:id – only superadmin
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteById(@PathVariable String id, HttpServletRequest req) {
        User me = (User) req.getAttribute("currentUser");
        if (!isSuper(me))
            return bad("Only the god can delete the user!");
        try {
            if (!repo.existsById(id)) return ResponseEntity.status(404).build();
            repo.deleteById(id);
            return ResponseEntity.ok(Map.of("message","User Deleted"));
        } catch (Exception e) {
            // users.js: res.sendStatus(400)
            return ResponseEntity.badRequest().build();
        }
    }

    // DELETE /users/me
    @DeleteMapping("/users/me")
    public ResponseEntity<?> deleteMe(HttpServletRequest req) {
        User me = (User) req.getAttribute("currentUser");
        if (!isSuper(me))
            return bad("You cannot delete yourself!");
        try {
            repo.deleteById(me.getId());
            return ResponseEntity.ok(me);
        } catch (Exception e) {
            // users.js: res.sendStatus(400)
            return ResponseEntity.badRequest().build();
        }
    }
}
