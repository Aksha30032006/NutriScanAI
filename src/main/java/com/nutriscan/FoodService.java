package com.nutriscan;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.util.*;

@Service
public class FoodService {
    public static final List<Food> FOODS = List.of(
        new Food("chicken rice", "Chicken Rice", "🍗", 420, 24, 52, 14),
        new Food("biryani", "Chicken Biryani", "🍛", 520, 26, 60, 18),
        new Food("apple", "Apple", "🍎", 95, 0.5, 25, 0.3),
        new Food("banana", "Banana", "🍌", 105, 1.3, 27, 0.4),
        new Food("rice", "Steamed Rice", "🍚", 240, 4.5, 53, 0.5),
        new Food("chicken", "Grilled Chicken", "🍗", 280, 43, 0, 11),
        new Food("salad", "Green Salad", "🥗", 120, 4, 12, 7),
        new Food("idli", "Idli (3 pcs)", "🍙", 180, 6, 36, 1),
        new Food("dosa", "Plain Dosa", "🥞", 170, 4, 28, 4),
        new Food("egg", "Boiled Eggs (2)", "🥚", 155, 13, 1, 11),
        new Food("pizza", "Pizza Slice", "🍕", 285, 12, 36, 10),
        new Food("burger", "Burger", "🍔", 540, 28, 40, 30),
        new Food("noodles", "Veg Noodles", "🍜", 380, 10, 62, 10),
        new Food("dal", "Dal & Roti", "🥘", 360, 15, 55, 8),
        new Food("paneer", "Paneer Curry", "🧀", 330, 16, 12, 24),
        new Food("oats", "Oats Bowl", "🥣", 220, 8, 38, 4),
        new Food("fish", "Fish Fry", "🐟", 300, 27, 10, 17),
        new Food("sandwich", "Veg Sandwich", "🥪", 250, 9, 36, 8));

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String apiKey;

    /** Matches typed name / file name against the built-in food list (longest key wins). */
    public Food match(String text) {
        String t = text == null ? "" : text.toLowerCase();
        return FOODS.stream()
                .filter(f -> t.contains(f.key().split(" ")[0]))
                .max(Comparator.comparingInt(f -> f.key().length()))
                .orElse(null);
    }

    public record Analysis(Food food, List<String> tips) { }

    /** One Claude call: identifies the food from the photo AND writes personalised tips. Returns null if no key or on any error. */
    public Analysis analyze(byte[] data, String mime, String typedName, User u, int todayCal, int todayProtein) {
        String key = apiKey;
        if (key == null || key.isBlank() || key.startsWith("PASTE")) return null;
        try {
            String model = Optional.ofNullable(System.getenv("ANTHROPIC_MODEL")).orElse("claude-sonnet-4-6");
            String prompt = "You are a friendly nutrition assistant. Identify the food in this photo"
                    + (typedName != null && !typedName.isBlank() ? " (the user says it is: " + typedName + ")" : "")
                    + " and estimate nutrition for the serving shown. "
                    + "The user's goal is '" + u.goal + "'. Daily targets: " + u.calTarget + " kcal and " + u.proteinTarget
                    + " g protein. Eaten so far today (before this meal): " + todayCal + " kcal, " + todayProtein + " g protein. "
                    + "Reply ONLY with JSON: {\"name\":\"\",\"calories\":0,\"protein\":0,\"carbs\":0,\"fat\":0,\"tips\":[\"\",\"\"]}. "
                    + "Give 2 or 3 short, practical tips specific to this meal, the goal and the day so far. Indian home food is fine to suggest.";
            Map<String, Object> image = Map.of("type", "image", "source", Map.of(
                    "type", "base64", "media_type", mime, "data", Base64.getEncoder().encodeToString(data)));
            Map<String, Object> text = Map.of("type", "text", "text", prompt);
            Map<String, Object> body = Map.of("model", model, "max_tokens", 600,
                    "messages", List.of(Map.of("role", "user", "content", List.of(image, text))));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("content-type", "application/json").header("x-api-key", key)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            String resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = mapper.readTree(resp);
            if (!root.has("content")) { System.err.println("[AI] API error: " + resp); return null; }
            String raw = root.get("content").get(0).get("text").asText().replaceAll("```json|```", "").trim();
            JsonNode o = mapper.readTree(raw);
            List<String> tips = new ArrayList<>();
            if (o.has("tips")) o.get("tips").forEach(t -> tips.add(t.asText()));
            Food f = new Food("ai", o.get("name").asText(), "🍽️", o.get("calories").asDouble(),
                    o.get("protein").asDouble(), o.get("carbs").asDouble(), o.get("fat").asDouble());
            System.out.println("[AI] detected: " + f.name());
            return new Analysis(f, tips);
        } catch (Exception e) {
            System.err.println("[AI] failed, using fallback: " + e.getMessage());
            return null;
        }
    }

    public List<String> recommend(User u, Scan s) {
        List<String> tips = new ArrayList<>();
        switch (u.goal) {
            case "Weight loss" -> {
                tips.add(s.calories > 550
                        ? "This is a heavy meal. Keep your next meal light, like a salad or soup."
                        : "Good portion for weight loss. Pair it with vegetables to stay full longer.");
                if (s.fat > 20) tips.add("Fat is high here. Try grilled or steamed versions next time.");
            }
            case "Gain weight" -> {
                tips.add(s.calories < 400
                        ? "Light meal. Add rice, nuts, ghee or a banana shake to hit your calorie surplus."
                        : "Good calories for weight gain. Keep 3 meals and 2 snacks through the day.");
                if (s.protein < 15) tips.add("Add eggs, curd, paneer or dal so the weight you gain is not only fat.");
            }
            case "Muscle gain" -> tips.add(s.protein < 20
                    ? "Add eggs, paneer, dal or chicken to lift the protein in this meal."
                    : "Great protein for muscle gain. Have it within 2 hours of your workout.");
            default -> tips.add("A balanced choice. Keep your portions consistent through the day.");
        }
        if (s.carbs > 60) tips.add("Carbs are on the high side. Swap half the rice or bread for vegetables.");
        return tips;
    }

    private static final Map<String, List<Object[]>> PLANS = Map.of(
        "Weight loss", List.of(new Object[]{"Breakfast", 0.25, "Oats bowl with fruit and nuts"}, new Object[]{"Lunch", 0.35, "Grilled chicken, small rice portion, big salad"}, new Object[]{"Snack", 0.10, "Apple with a handful of almonds"}, new Object[]{"Dinner", 0.30, "Dal, 2 rotis and sautéed vegetables"}),
        "Gain weight", List.of(new Object[]{"Breakfast", 0.25, "Oats with milk, banana, nuts and 2 eggs"}, new Object[]{"Lunch", 0.30, "Large rice portion with dal, chicken or paneer and curd"}, new Object[]{"Snack", 0.20, "Banana milkshake, peanut butter toast or dry fruit mix"}, new Object[]{"Dinner", 0.25, "Chapati or dosa with a rich curry and a glass of milk"}),
        "Muscle gain", List.of(new Object[]{"Breakfast", 0.25, "Eggs (3), toast and a banana"}, new Object[]{"Lunch", 0.35, "Chicken rice with curd"}, new Object[]{"Snack", 0.15, "Peanut butter sandwich or a protein shake"}, new Object[]{"Dinner", 0.25, "Paneer or fish curry with rotis"}),
        "Maintain", List.of(new Object[]{"Breakfast", 0.25, "Idli or dosa with sambar"}, new Object[]{"Lunch", 0.35, "Rice, dal, vegetables and curd"}, new Object[]{"Snack", 0.10, "Fruit bowl"}, new Object[]{"Dinner", 0.30, "Chapati with a light curry"}));

    public Map<String, Object> mealPlan(User u) {
        List<Object[]> plan = PLANS.getOrDefault(u.goal, PLANS.get("Maintain"));
        List<Map<String, Object>> meals = new ArrayList<>();
        for (Object[] m : plan) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("meal", m[0]);
            row.put("text", m[2]);
            row.put("kcal", Math.round(u.calTarget * (double) m[1]));
            meals.add(row);
        }
        return Map.of("goal", u.goal, "meals", meals);
    }
}
