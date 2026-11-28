package com.nanonaitor.arsenal.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.AddPackFindersEvent;

/**
 * Player-defined weapon materials. Each entry adds every weapon in that
 * material, named after it, crafted like the iron one with its ingredient and
 * with its metal tinted its colour (handles, logs and wood keep theirs). Up to
 * eight. Items exist from startup, so the file is read before they register and
 * changes need a restart; it lives in its own file, config/nanonaitors_arsenal-materials.toml.
 */
public final class CustomMaterials {
    public static final String FILE = "nanonaitors_arsenal-materials.toml";
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTRIES;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("materials");
        ENTRIES = BUILDER.comment(
                "Your own weapon materials, one per line: id, Display Name, #RRGGBB, ingredient, harvest level, durability, attack damage, enchantability",
                "Example: \"amethyst, Amethyst, #9A5CC6, minecraft:amethyst_shard, 2, 300, 2.0, 18\"",
                "id: lowercase letters, digits, _ (used in item IDs: keep it once worlds use it). Display Name replaces \"Iron\" in item names.",
                "ingredient: an item (modid:item) or an item tag (#forge:ingots/copper); it crafts and repairs the weapons.",
                "harvest level 0-4 also picks which built-in tier the special rules follow (0 wood, 1 stone, 2 iron, 3 diamond, 4 netherite).",
                "Up to 8 materials. Restart Minecraft after changes; the server and every client need the same list.")
            .defineListAllowEmpty("entries", List.of(), value -> value instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** Reads the file directly, before Forge loads configs, so the items can register. */
    public static void loadEarly() {
        Path path = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(FILE);
        List<? extends String> entries = List.of();
        if (Files.isRegularFile(path)) {
            try (var config = com.electronwill.nightconfig.core.file.CommentedFileConfig.of(path)) {
                config.load();
                List<String> read = config.getOrElse("materials.entries", List.of());
                entries = read;
            } catch (RuntimeException error) {
                com.mojang.logging.LogUtils.getLogger().warn("Could not read {}: {}", FILE, error.getMessage());
            }
        }
        for (String problem : WeaponTier.configureCustom(entries))
            com.mojang.logging.LogUtils.getLogger().warn("Arsenal custom materials: {}", problem);
        for (WeaponTier tier : WeaponTier.activeValues()) if (tier.custom())
            com.mojang.logging.LogUtils.getLogger().info("Arsenal custom material '{}' ({}) added", tier.id, tier.displayName());
    }

    public static boolean any() {
        return WeaponTier.activeValues().stream().anyMatch(WeaponTier::custom);
    }

    /** Adds the generated recipes as a built-in data pack that is always on. */
    public static void addPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !any()) return;
        String id = ArsenalMod.MOD_ID + "_custom_materials";
        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(id,
            Component.literal("Arsenal custom materials"), true, name -> new RecipePack(name),
            PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN)));
    }

    /** Every custom weapon's recipe: the iron recipe with the custom ingredient and result. */
    public static Map<ResourceLocation, String> recipes() {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        for (WeaponTier tier : WeaponTier.activeValues()) {
            if (!tier.custom()) continue;
            var spec = WeaponTier.CustomSpec.parse(String.join(",", tier.id, tier.displayName(),
                String.format("#%06X", tier.color()), tier.ingredient(), "0", "1", "0", "0"));
            for (WeaponKind kind : WeaponKind.values()) {
                if (kind == WeaponKind.LINKED_CLAWS) continue;
                InputStream in = ArsenalMod.class.getResourceAsStream("/data/" + ArsenalMod.MOD_ID + "/recipes/" + kind.id + "_iron.json");
                if (in == null) continue;
                try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    swap(json, spec.ingredientJson(), kind.id + "_" + tier.id);
                    out.put(new ResourceLocation(ArsenalMod.MOD_ID, "recipes/custom/" + kind.id + "_" + tier.id + ".json"),
                        json.toString());
                } catch (java.io.IOException | RuntimeException error) {
                    com.mojang.logging.LogUtils.getLogger().warn("Arsenal custom recipe {} {}: {}", kind.id, tier.id, error.getMessage());
                }
            }
        }
        return out;
    }

    /** Iron ingots and blocks become the custom ingredient; iron results become the custom weapon. */
    private static void swap(JsonElement json, JsonObject ingredient, String result) {
        if (json instanceof JsonArray array) { for (JsonElement e : array) swap(e, ingredient, result); return; }
        if (!(json instanceof JsonObject object)) return;
        if (object.get("key") instanceof JsonObject key) {
            for (var entry : key.entrySet()) {
                if (entry.getValue() instanceof JsonObject value && value.has("item")) {
                    String item = value.get("item").getAsString();
                    if (item.equals("minecraft:iron_ingot") || item.equals("minecraft:iron_block")) entry.setValue(ingredient.deepCopy());
                }
            }
        }
        if (object.get("result") instanceof JsonObject res && res.has("item")
            && res.get("item").getAsString().endsWith("_iron"))
            res.addProperty("item", ArsenalMod.MOD_ID + ":" + result);
        for (var entry : object.entrySet()) if (!entry.getKey().equals("key")) swap(entry.getValue(), ingredient, result);
    }

    /** An in-memory data pack holding the generated recipes. */
    private static final class RecipePack implements PackResources {
        private final String id;
        private final Map<ResourceLocation, String> files;

        RecipePack(String id) {
            this.id = id;
            this.files = recipes();
        }

        private static IoSupplier<InputStream> text(String value) {
            return () -> new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override public IoSupplier<InputStream> getRootResource(String... path) { return null; }

        @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            String value = type == PackType.SERVER_DATA ? files.get(location) : null;
            return value == null ? null : text(value);
        }

        @Override public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != PackType.SERVER_DATA) return;
            files.forEach((location, value) -> {
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith(path + "/"))
                    output.accept(location, text(value));
            });
        }

        @Override public Set<String> getNamespaces(PackType type) {
            return type == PackType.SERVER_DATA ? Set.of(ArsenalMod.MOD_ID) : Set.of();
        }

        @Override public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
            if (!"pack".equals(serializer.getMetadataSectionName())) return null;
            JsonObject pack = new JsonObject();
            pack.addProperty("pack_format", 15);
            pack.addProperty("description", "Arsenal custom material recipes");
            return serializer.fromJson(pack);
        }

        @Override public String packId() { return id; }

        @Override public boolean isBuiltin() { return true; }

        @Override public void close() {}
    }

    private CustomMaterials() {}
}
