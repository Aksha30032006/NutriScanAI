package com.nutriscan;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final UserRepo users;
    private final ScanRepo scans;
    private final WaterRepo water;
    private final ReviewRepo reviews;
    private final Jwt jwt;
    private final FoodService foods;
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    public ApiController(UserRepo users, ScanRepo scans, WaterRepo water, ReviewRepo reviews, Jwt jwt, FoodService foods) {
        this.users = users; this.scans = scans; this.water = water; this.reviews = reviews; this.jwt = jwt; this.foods = foods;
    }

    private static String uid() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
    private static String today() { return LocalDate.now(ZoneOffset.UTC).toString(); }
    private static User me(HttpServletRequest r) { return (User) r.getAttribute("user"); }
    private static String str(Map<String, Object> b, String k) { Object v = b.get(k); return v == null ? "" : String.valueOf(v); }
    private static ApiException bad(int code, String msg) { return new ApiException(code, msg); }

    /* ---------------- auth ---------------- */
    @PostMapping("/auth/signup")
    public Map<String, Object> signup(@RequestBody Map<String, Object> b) {
        String name = str(b, "name").trim(), email = str(b, "email").trim().toLowerCase(), pw = str(b, "password");
        if (name.isEmpty() || email.isEmpty() || pw.isEmpty()) throw bad(400, "Name, email and password are required");
        if (pw.length() < 6) throw bad(400, "Password must be at least 6 characters");
        if (users.findByEmail(email).isPresent()) throw bad(409, "This email already has an account");
        User u = new User();
        u.id = uid(); u.name = name; u.email = email; u.password = enc.encode(pw);
        users.save(u);
        return Map.of("token", jwt.sign(u.id), "user", u);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> b) {
        User u = users.findByEmail(str(b, "email").trim().toLowerCase()).orElse(null);
        if (u == null || !enc.matches(str(b, "password"), u.password)) throw bad(401, "Email or password is incorrect");
        return Map.of("token", jwt.sign(u.id), "user", u);
    }

    /* ---------------- profile ---------------- */
    @GetMapping("/me")
    public User getMe(HttpServletRequest r) { return me(r); }

    @PutMapping("/me")
    public User updateMe(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        User u = me(r);
        try {
            if (!str(b, "name").isBlank()) u.name = str(b, "name").trim();
            if (!str(b, "goal").isBlank()) u.goal = str(b, "goal");
            if (!str(b, "calTarget").isBlank()) u.calTarget = (int) Double.parseDouble(str(b, "calTarget"));
            if (!str(b, "proteinTarget").isBlank()) u.proteinTarget = (int) Double.parseDouble(str(b, "proteinTarget"));
            if (!str(b, "waterTarget").isBlank()) u.waterTarget = Double.parseDouble(str(b, "waterTarget"));
            if (!str(b, "height").isBlank()) u.height = Double.parseDouble(str(b, "height"));
            if (!str(b, "weight").isBlank()) u.weight = Double.parseDouble(str(b, "weight"));
            if (!str(b, "age").isBlank()) u.age = (int) Double.parseDouble(str(b, "age"));
        } catch (NumberFormatException e) { throw bad(400, "Please enter valid numbers"); }
        return users.save(u);
    }

    @PutMapping("/me/password")
    public Map<String, Object> changePassword(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        User u = me(r);
        if (!enc.matches(str(b, "current"), u.password)) throw bad(400, "Current password is incorrect");
        if (str(b, "next").length() < 6) throw bad(400, "New password must be at least 6 characters");
        u.password = enc.encode(str(b, "next"));
        users.save(u);
        return Map.of("ok", true);
    }

    @DeleteMapping("/me")
    @Transactional
    public Map<String, Object> deleteMe(HttpServletRequest r) {
        User u = me(r);
        scans.deleteByUserId(u.id);
        water.deleteByUserId(u.id);
        users.deleteById(u.id);
        return Map.of("ok", true);
    }

    /* ---------------- scans ---------------- */
    @PostMapping("/scans")
    public Scan scan(HttpServletRequest r, @RequestParam("image") MultipartFile image,
                     @RequestParam(value = "name", required = false) String name) throws Exception {
        User u = me(r);
        if (image.isEmpty()) throw bad(400, "Please upload a food image");
        String orig = Optional.ofNullable(image.getOriginalFilename()).orElse("");
        String hint = (name != null && !name.isBlank()) ? name : orig.replaceAll("\\.[^.]+$", "");
        String mime = Optional.ofNullable(image.getContentType()).orElse("image/jpeg");
        LocalDate nowD = LocalDate.now(ZoneOffset.UTC);
        List<Scan> todayScans = scans.findByUserIdOrderByCreatedAtDesc(u.id).stream().filter(x -> dayOf(x).equals(nowD)).toList();
        int tc = todayScans.stream().mapToInt(x -> x.calories).sum(), tp = todayScans.stream().mapToInt(x -> x.protein).sum();
        FoodService.Analysis ai = foods.analyze(image.getBytes(), mime, name, u, tc, tp);
        Food f = ai != null ? ai.food() : foods.match(hint);
        if (f == null) f = FoodService.FOODS.get(new Random().nextInt(FoodService.FOODS.size()));

        String id = uid();
        String ext = mime.contains("/") ? mime.split("/")[1].replace("jpeg", "jpg") : "jpg";
        Path dir = Paths.get("uploads").toAbsolutePath();
        Files.createDirectories(dir);
        Files.write(dir.resolve(id + "." + ext), image.getBytes());

        Scan s = new Scan();
        s.id = id; s.userId = u.id; s.name = f.name(); s.emoji = f.emoji();
        s.calories = (int) Math.round(f.cal()); s.protein = (int) Math.round(f.p());
        s.carbs = (int) Math.round(f.c()); s.fat = (int) Math.round(f.f());
        s.image = "/uploads/" + id + "." + ext; s.createdAt = Instant.now();
        s.ai = ai != null;
        s.tips = (ai != null && !ai.tips().isEmpty()) ? ai.tips() : foods.recommend(u, s);
        scans.save(s);
        Thread.sleep(1800); // lets the "analysing" animation play
        return s;
    }

    @GetMapping("/scans")
    public List<Scan> listScans(HttpServletRequest r, @RequestParam(value = "limit", required = false) Integer limit) {
        List<Scan> all = scans.findByUserIdOrderByCreatedAtDesc(me(r).id);
        return limit == null ? all : all.subList(0, Math.min(limit, all.size()));
    }

    @GetMapping("/scans/{id}")
    public Scan getScan(HttpServletRequest r, @PathVariable String id) {
        return scans.findByIdAndUserId(id, me(r).id).orElseThrow(() -> bad(404, "Scan not found"));
    }

    @DeleteMapping("/scans/{id}")
    @Transactional
    public Map<String, Object> deleteScan(HttpServletRequest r, @PathVariable String id) {
        scans.deleteByIdAndUserId(id, me(r).id);
        return Map.of("ok", true);
    }

    /* ---------------- stats ---------------- */
    private static LocalDate dayOf(Scan s) { return LocalDate.ofInstant(s.createdAt, ZoneOffset.UTC); }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpServletRequest r) {
        User u = me(r);
        List<Scan> mine = scans.findByUserIdOrderByCreatedAtDesc(u.id);
        List<Map<String, Object>> days = new ArrayList<>();
        int wc = 0, wp = 0, wn = 0;
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            List<Scan> ds = mine.stream().filter(s -> dayOf(s).equals(d)).toList();
            int cal = ds.stream().mapToInt(s -> s.calories).sum(), pro = ds.stream().mapToInt(s -> s.protein).sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("label", d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            m.put("calories", cal); m.put("protein", pro); m.put("scans", ds.size());
            days.add(m);
            wc += cal; wp += pro; wn += ds.size();
        }
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        List<Scan> t = mine.stream().filter(s -> dayOf(s).equals(now)).toList();
        int streak = 0;
        for (int i = days.size() - 1; i >= 0 && (int) days.get(i).get("scans") > 0; i--) streak++;
        int ml = water.findByUserIdAndDate(u.id, today()).stream().mapToInt(w -> w.ml).sum();

        Map<String, Object> todayM = new LinkedHashMap<>();
        todayM.put("calories", t.stream().mapToInt(s -> s.calories).sum());
        todayM.put("protein", t.stream().mapToInt(s -> s.protein).sum());
        todayM.put("carbs", t.stream().mapToInt(s -> s.carbs).sum());
        todayM.put("fat", t.stream().mapToInt(s -> s.fat).sum());
        todayM.put("scans", t.size());
        todayM.put("water", ml);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("today", todayM);
        out.put("week", Map.of("calories", wc, "protein", wp, "scans", wn));
        out.put("streak", streak);
        return out;
    }

    /* ---------------- water ---------------- */
    private Map<String, Object> waterState(User u) {
        int ml = water.findByUserIdAndDate(u.id, today()).stream().mapToInt(w -> w.ml).sum();
        return Map.of("ml", ml, "target", (int) Math.round(u.waterTarget * 1000));
    }

    @GetMapping("/water")
    public Map<String, Object> getWater(HttpServletRequest r) { return waterState(me(r)); }

    @PostMapping("/water")
    @Transactional
    public Map<String, Object> addWater(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        User u = me(r);
        if (Boolean.TRUE.equals(b.get("reset"))) water.deleteByUserIdAndDate(u.id, today());
        else {
            WaterLog w = new WaterLog();
            w.id = uid(); w.userId = u.id; w.date = today();
            w.ml = (int) Math.max(0, Math.min(2000, Double.parseDouble("0" + str(b, "ml"))));
            water.save(w);
        }
        return waterState(u);
    }

    /* ---------------- meal plan ---------------- */
    @GetMapping("/mealplan")
    public Map<String, Object> mealPlan(HttpServletRequest r) { return foods.mealPlan(me(r)); }

    /* ---------------- reviews ---------------- */
    @GetMapping("/reviews")
    public List<Review> listReviews() { return reviews.findTop30ByOrderByCreatedAtDesc(); }

    @PostMapping("/reviews")
    public Review addReview(HttpServletRequest r, @RequestBody Map<String, Object> b) {
        String text = str(b, "text").trim();
        if (text.length() < 5) throw bad(400, "Please write a few words about your experience");
        Review v = new Review();
        v.id = uid(); v.name = me(r).name;
        int rating; try { rating = (int) Double.parseDouble(str(b, "rating")); } catch (Exception e) { rating = 5; }
        v.rating = Math.max(1, Math.min(5, rating));
        v.text = text.length() > 400 ? text.substring(0, 400) : text;
        return reviews.save(v);
    }
}
