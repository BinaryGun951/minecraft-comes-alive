package mca.entity.interaction.gifts;

import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import mca.entity.VillagerEntityMCA;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;

public class GiftType {
    static final List<GiftType> REGISTRY = new ArrayList<>();

    public static GiftType fromJson(Identifier id, JsonObject json) {
        List<GiftPredicate> conditions = new ArrayList<>();
        JsonHelper.getArray(json, "conditions").forEach(element -> {
            conditions.add(GiftPredicate.fromJson(JsonHelper.asObject(element, "condition")));
        });

        HashMap<Item, Integer> items = new HashMap<>();
        HashMap<Tag<Item>, Integer> tags = new HashMap<>();
        JsonHelper.getObject(json, "items").entrySet().forEach(element -> {
            String string = element.getKey();
            Integer satisfaction = element.getValue().getAsInt();
            if (string.charAt(0) == '#') {
                Identifier identifier = new Identifier(string.substring(1));
                Tag<Item> tag = ServerTagManagerHolder.getTagManager().getItems().getTag(identifier);
                if (tag != null) {
                    tags.put(tag, satisfaction);
                } else {
                    throw new JsonSyntaxException("Unknown item tag '" + identifier + "'");
                }
            } else {
                Identifier identifier = new Identifier(string);
                Item item = Registry.ITEM.getOrEmpty(identifier).orElseThrow(() -> new JsonSyntaxException("Unknown item '" + identifier + "'"));
                items.put(item, satisfaction);
            }
        });

        int priority = JsonHelper.getInt(json, "priority", 0);

        JsonObject thresholds = JsonHelper.getObject(json, "thresholds", new JsonObject());
        int fail = JsonHelper.getInt(thresholds, "fail", 0);
        int good = JsonHelper.getInt(thresholds, "good", 10);
        int better = JsonHelper.getInt(thresholds, "better", 20);

        JsonObject responsesJson = JsonHelper.getObject(json, "responses", new JsonObject());
        Map<Response, String> responses = Stream.of(Response.values()).collect(Collectors.toMap(
                Function.identity(),
                response -> JsonHelper.getString(responsesJson, response.name().toLowerCase(), response.getDefaultDialogue())
        ));

        return new GiftType(id, priority, conditions, items, tags, fail, good, better, responses);
    }

    public static Stream<GiftType> allMatching(ItemStack stack) {
        return REGISTRY.stream().filter(type -> type.matches(stack));
    }

    /**
     * returns the giftType with the highest priority
     * if at least one gift fails, it chooses only from the failed gifts
     */
    public static Optional<GiftType> bestMatching(VillagerEntityMCA recipient, ItemStack stack) {
        int max = GiftType.allMatching(stack).mapToInt(a -> a.priority).max().orElse(0);
        Optional<GiftType> worst = GiftType.allMatching(stack)
                .filter(a -> a.priority == max)
                .filter(a -> a.getResponse(a.getSatisfactionFor(recipient, stack)) == Response.FAIL)
                .max(Comparator.comparingDouble(a -> a.getSatisfactionFor(recipient, stack)));

        if (worst.isPresent()) {
            return worst;
        } else {
            return GiftType.allMatching(stack)
                    .filter(a -> a.priority == max)
                    .max(Comparator.comparingDouble(a -> a.getSatisfactionFor(recipient, stack)));
        }
    }

    private final Identifier id;
    private final int priority;

    private final List<GiftPredicate> conditions;

    private final Map<Item, Integer> items;
    private final Map<Tag<Item>, Integer> tags;

    private final int fail;
    private final int good;
    private final int better;

    private final Map<Response, String> responses;

    public GiftType(Identifier id, int priority, List<GiftPredicate> conditions, Map<Item, Integer> items, Map<Tag<Item>, Integer> tags, int fail, int good, int better, Map<Response, String> responses) {
        this.id = id;
        this.priority = priority;
        this.conditions = conditions;
        this.items = items;
        this.tags = tags;
        this.fail = fail;
        this.good = good;
        this.better = better;
        this.responses = responses;
    }

    public Identifier getId() {
        return id;
    }

    /**
     * Checks whether the given item counts for this type of gift.
     */
    public boolean matches(ItemStack stack) {
        return items.keySet().stream().anyMatch(i -> i == stack.getItem()) || tags.keySet().stream().anyMatch(i -> i.contains(stack.getItem()));
    }

    /**
     * Gets the amount of satisfaction giving this gift to a villager would produce.
     */
    public int getSatisfactionFor(VillagerEntityMCA recipient, ItemStack stack) {
        Optional<Integer> value = items.entrySet().stream().filter(i -> i.getKey() == stack.getItem()).findFirst().map(Map.Entry::getValue);
        int base = value.orElseGet(() -> tags.entrySet().stream().filter(i -> i.getKey().contains(stack.getItem())).findFirst().map(Map.Entry::getValue).orElse(0));
        return base + conditions.stream().mapToInt(condition -> condition.getSatisfactionFor(recipient, stack)).sum();
    }

    /**
     * Returns the proper response a villager should produce when given this type of gift.
     */
    public Response getResponse(int satisfaction) {
        return satisfaction <= fail ? Response.FAIL
                : satisfaction <= good ? Response.GOOD
                : satisfaction <= better ? Response.BETTER
                : Response.BEST;
    }

    /**
     * Returns a line of dialogue to be spoken when a villager responds to this gift.
     */
    public String getDialogueFor(Response response) {
        return responses.get(response);
    }
}