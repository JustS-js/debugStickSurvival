package net.just_s.sds.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.just_s.sds.SDSMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("SDS.json").toFile();
    private static SDSConfig sdsConfig;

    protected static SDSConfig getData() {
        if (sdsConfig == null) {
            sdsConfig = new SDSConfig();
        }
        return sdsConfig;
    }

    protected static JsonElement serialize() {
        return SDSConfig.CODEC
                .encode(getData(), JsonOps.INSTANCE, JsonOps.INSTANCE.empty())
                .getOrThrow();
    }

    protected static void deserialize(JsonElement element) {
        sdsConfig = SDSConfig.CODEC.decode(JsonOps.INSTANCE, element).getOrThrow().getFirst();
    }

    public static void save() {
        JsonElement json = serialize();
        try (FileWriter w = new FileWriter(configFile)) {
            final JsonWriter jw = GSON.newJsonWriter(w);
            jw.setIndent("\t");
            GSON.toJson(json, jw);
            SDSMod.LOGGER.info("Saved new config file.");
        } catch (IOException e) {
            SDSMod.LOGGER.error("Error while saving:" + e.getMessage());
        }
    }

    public static void loadOrCreate() {
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (!(parent.exists() || parent.mkdirs())) {
                SDSMod.LOGGER.warn("Can't create config: {}", configFile.getAbsolutePath());
                return;
            }
            save();
        } else {
            load();
        }
    }

    public static void load() {
        try (FileReader f = new FileReader(configFile)) {
            deserialize(JsonParser.parseReader(f));
        } catch (Exception e) {
            SDSMod.LOGGER.warn("Exception occurred while reading config. ", e);
            save();
        }
    }

    public static boolean isBlockAllowed(Block block) {
        // 1) check if block has been mentioned in BLOCKS part
        String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();

        if (sdsConfig.allowed().containsBlock(blockName)) return true;
        if (sdsConfig.forbidden().containsBlock(blockName)) {
            return !sdsConfig.forbidden().getBlockEntry(blockName).isEmpty();
        }
        // 2) if block is not stated in config, check tags
        Stream<TagKey<Block>> tagStream = block.defaultBlockState().tags();
        List<TagKey<Block>> tagArray = tagStream.toList();
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.location().toString();
            if (sdsConfig.allowed().containsTag(tagName)) return true;
        }
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.location().toString();
            if (sdsConfig.forbidden().containsTag(tagName)) {
                return !sdsConfig.forbidden().getTagEntry(tagName).isEmpty();
            }
        }

        // 3) if block has properties in allowed properties config
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (sdsConfig.allowed().properties().containsKey(property.getName()))
                return true;
        }

        // 4) if block, its block-states and tags were not clarified in config, check whitelist mode
        return !sdsConfig.whitelist();
    }

    public static boolean isPropertyAllowed(String propertyName, @Nullable Block block) {
        if (block != null) {
            String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();
            // 1) Check for exactly this block
            if (sdsConfig.forbidden().containsBlock(blockName)) {
                Map<String, List<String>> forbidden_props = sdsConfig.forbidden().getBlockEntry(blockName).properties();
                if (forbidden_props.containsKey("all") ||
                        forbidden_props.getOrDefault(propertyName, new ArrayList<>()).contains("all")) return false;
            }
            if (sdsConfig.allowed().containsBlock(blockName)) {
                Map<String, List<String>> allowed_props = sdsConfig.allowed().getBlockEntry(blockName).properties();
                if (allowed_props.containsKey("all") || allowed_props.containsKey(propertyName)) return true;
                if (allowed_props.isEmpty()) {
                    return !sdsConfig.forbidden().properties().getOrDefault(propertyName, new ArrayList<>()).contains("all");
                }
            }
            // 2) Either block is not stated in config or
            // allowed by itself, but does not speak about this property. Check its tags
            Stream<TagKey<Block>> tagStream = block.defaultBlockState().tags();
            List<TagKey<Block>> tagArray = tagStream.toList();
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.location().toString();
                if (sdsConfig.forbidden().containsTag(tagName)) {
                    Map<String, List<String>> forbidden_props = sdsConfig.forbidden().getTagEntry(tagName).properties();
                    if (forbidden_props.containsKey("all") ||
                            forbidden_props.getOrDefault(propertyName, new ArrayList<>()).contains("all")) return false;
                }
            }
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.location().toString();
                if (sdsConfig.allowed().containsTag(tagName)) {
                    Map<String, List<String>> allowed_props = sdsConfig.allowed().getTagEntry(tagName).properties();
                    if (allowed_props.containsKey("all") || allowed_props.containsKey(propertyName)) return true;
                    if (allowed_props.isEmpty()) {
                        return !sdsConfig.forbidden().properties().getOrDefault(propertyName, new ArrayList<>()).contains("all");
                    }
                }
            }
        }
        // 3) If tags do not speak about property, check global list
        if (sdsConfig.allowed().properties().containsKey("all") || sdsConfig.allowed().properties().containsKey(propertyName))
            return true;
        if (sdsConfig.forbidden().properties().containsKey(propertyName))
            return !sdsConfig.forbidden().properties().get(propertyName).contains("all");
        // 4) if property was not clarified in config, check whitelist mode
        return !sdsConfig.whitelist();
    }

    public static boolean isPropertyValueAllowed(Block block, String propertyName, String value) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();
        // 1) Check for exactly this block
        if (sdsConfig.forbidden().containsBlock(blockName)) {
            Map<String, List<String>> forbidden_props = sdsConfig.forbidden().getBlockEntry(blockName).properties();
            if (forbidden_props.containsKey("all")) return false;
            List<String> values = forbidden_props.getOrDefault(propertyName, List.of());
            if (values.contains("all") || values.contains(value)) return false;
        }
        if (sdsConfig.allowed().containsBlock(blockName)) {
            Map<String, List<String>> allowed_props = sdsConfig.allowed().getBlockEntry(blockName).properties();
            if (allowed_props.containsKey("all")) return true;
            List<String> values = allowed_props.getOrDefault(propertyName, List.of());
            if (values.contains("all") || values.contains(value)) return true;
            if (allowed_props.isEmpty()) {
                return !sdsConfig.forbidden().properties().getOrDefault(propertyName, List.of()).contains("all");
            }
        }
        // 2) Either block is not stated in config or
        // allowed by itself, but does not speak about this property. Check its tags
        Stream<TagKey<Block>> tagStream = block.defaultBlockState().tags();
        List<TagKey<Block>> tagArray = tagStream.toList();
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.location().toString();
            if (sdsConfig.forbidden().containsTag(tagName)) {
                Map<String, List<String>> forbidden_props = sdsConfig.forbidden().getTagEntry(tagName).properties();
                if (forbidden_props.containsKey("all")) return false;
                List<String> values = forbidden_props.getOrDefault(propertyName, List.of());
                if (values.contains("all") || values.contains(value)) return false;
            }
        }
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.location().toString();
            if (sdsConfig.allowed().containsTag(tagName)) {
                Map<String, List<String>> allowed_props = sdsConfig.allowed().getTagEntry(tagName).properties();
                if (allowed_props.containsKey("all")) return true;
                List<String> values = allowed_props.getOrDefault(propertyName, List.of());
                if (values.contains("all") || values.contains(value)) return true;
                if (allowed_props.isEmpty()) {
                    return !sdsConfig.forbidden().properties().getOrDefault(propertyName, List.of()).contains("all");
                }
            }
        }
        // 3) If tags do not speak about property, check global list
        if (sdsConfig.allowed().properties().containsKey("all")) return true;
        if (sdsConfig.allowed().properties().containsKey(propertyName)) {
            List<String> values = sdsConfig.allowed().properties().get(propertyName);
            if (values.contains(value)) return true;
        }
        if (sdsConfig.forbidden().properties().containsKey("all")) return false;
        if (sdsConfig.forbidden().properties().containsKey(propertyName)) {
            List<String> values = sdsConfig.forbidden().properties().get(propertyName);
            if (values.contains(value)) return false;
        }
        // 4) if property was not clarified in config, check whitelist mode
        return !sdsConfig.whitelist();
    }
}
