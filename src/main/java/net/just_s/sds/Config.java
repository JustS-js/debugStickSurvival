package net.just_s.sds;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.*;

public class Config {
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("SDS.json").toFile();

    public static boolean whitelist;
    private static HashMap<String, List<String>> properties_allowed = new HashMap<>();
    private static HashMap<String, List<String>> properties_forbidden = new HashMap<>();

    private static HashMap<String, HashMap<String, List<String>>> tags_allowed = new HashMap<>();
    private static HashMap<String, HashMap<String, List<String>>> blocks_allowed = new HashMap<>();
    private static HashMap<String, HashMap<String, List<String>>> tags_forbidden = new HashMap<>();
    private static HashMap<String, HashMap<String, List<String>>> blocks_forbidden = new HashMap<>();

    public static void load() {
        JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader(configFile)) {

            JSONObject jfile = (JSONObject) parser.parse(reader);

            whitelist = (boolean) jfile.get("whitelist");

            JSONObject allowed = (JSONObject) jfile.get("allowed");
            JSONArray tags_allowed_js = (JSONArray) allowed.get("tags");
            JSONArray blocks_allowed_js = (JSONArray) allowed.get("blocks");

            Object ObjectProperties_allowed = allowed.get("properties");
            JSONObject properties_allowed_js = (ObjectProperties_allowed instanceof JSONArray) ? parseOldArrayToObject((JSONArray) allowed.get("properties")) : (JSONObject) allowed.get("properties");

            JSONObject forbidden = (JSONObject) jfile.get("forbidden");
            JSONArray tags_forbidden_js = (JSONArray) forbidden.get("tags");
            JSONArray blocks_forbidden_js = (JSONArray) forbidden.get("blocks");

            Object ObjectProperties_forbidden = forbidden.get("properties");
            JSONObject properties_forbidden_js = (ObjectProperties_forbidden instanceof JSONArray) ? parseOldArrayToObject((JSONArray) forbidden.get("properties")) : (JSONObject) forbidden.get("properties");

            for (Object property : properties_allowed_js.keySet()) {
                JSONArray JSONPropertyValues = (JSONArray) properties_allowed_js.get(property);
                List<String> propertyValues = new ArrayList<>();
                for (Object value : JSONPropertyValues) {
                    propertyValues.add(value.toString());
                }
                properties_allowed.put((String) property, propertyValues);
            }
            for (Object property : properties_forbidden_js.keySet()) {
                JSONArray JSONPropertyValues = (JSONArray) properties_forbidden_js.get(property);
                List<String> propertyValues = new ArrayList<>();
                for (Object value : JSONPropertyValues) {
                    propertyValues.add(value.toString());
                }
                properties_forbidden.put((String) property, propertyValues);
            }

            populate(tags_allowed, tags_allowed_js);
            populate(tags_forbidden, tags_forbidden_js);
            populate(blocks_allowed, blocks_allowed_js);
            populate(blocks_forbidden, blocks_forbidden_js);

        } catch (IOException | ParseException | ClassCastException e) {
            SDSMod.LOGGER.warn("Error while loading config: " + e);
            factorySettings();
        }
    }

    private static void factorySettings() {
        whitelist = false;
        properties_allowed = new HashMap<>();
        properties_forbidden = new HashMap<>();
        tags_allowed = new HashMap<>();
        tags_forbidden = new HashMap<>();
        blocks_allowed = new HashMap<>();
        blocks_forbidden = new HashMap<>();
        save();
    }

    public static void save() {
        JSONObject jfile = new JSONObject();
        jfile.put("whitelist", whitelist);

        JSONObject allowed = new JSONObject();
        JSONObject properties_allowed_js = new JSONObject(properties_allowed);
        allowed.put("properties", properties_allowed_js);

        JSONArray tags_allowed_js = new JSONArray();
        for (Map.Entry<String, HashMap<String, List<String>>> entry : tags_allowed.entrySet()) {
            JSONObject tag = new JSONObject();
            tag.put("id", entry.getKey());

            HashMap<String, List<String>> map = entry.getValue();
            if (!map.isEmpty()) {
                JSONObject props = new JSONObject(map);
                tag.put("properties", props);
            }
            tags_allowed_js.add(tag);
        }
        allowed.put("tags", tags_allowed_js);

        JSONArray blocks_allowed_js = new JSONArray();
        for (Map.Entry<String, HashMap<String, List<String>>> entry : blocks_allowed.entrySet()) {
            JSONObject block = new JSONObject();
            block.put("id", entry.getKey());

            HashMap<String, List<String>> map = entry.getValue();
            if (!map.isEmpty()) {
                JSONObject props = new JSONObject(map);
                block.put("properties", props);
            }
            blocks_allowed_js.add(block);
        }
        allowed.put("blocks", blocks_allowed_js);

        jfile.put("allowed", allowed);

        JSONObject forbidden = new JSONObject();
        JSONObject properties_forbidden_js = new JSONObject(properties_forbidden);
        forbidden.put("properties", properties_forbidden_js);

        JSONArray tags_forbidden_js = new JSONArray();
        for (Map.Entry<String, HashMap<String, List<String>>> entry : tags_forbidden.entrySet()) {
            JSONObject tag = new JSONObject();
            tag.put("id", entry.getKey());

            HashMap<String, List<String>> map = entry.getValue();
            if (!map.isEmpty()) {
                JSONObject props = new JSONObject(map);
                tag.put("properties", props);
            }
            tags_forbidden_js.add(tag);
        }
        forbidden.put("tags", tags_forbidden_js);

        JSONArray blocks_forbidden_js = new JSONArray();
        for (Map.Entry<String, HashMap<String, List<String>>> entry : blocks_forbidden.entrySet()) {
            JSONObject block = new JSONObject();
            block.put("id", entry.getKey());

            HashMap<String, List<String>> map = entry.getValue();
            if (!map.isEmpty()) {
                JSONObject props = new JSONObject(map);
                block.put("properties", props);
            }
            blocks_forbidden_js.add(block);
        }
        forbidden.put("blocks", blocks_forbidden_js);

        jfile.put("forbidden", forbidden);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(jfile.toJSONString());
        String prettyJsonString = gson.toJson(je);

        try {
            FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8);
            writer.write(prettyJsonString);
            writer.close();
            SDSMod.LOGGER.error("Saved new config file.");
        } catch (IOException e) {SDSMod.LOGGER.error("Error while saving:" + e.getMessage());}
    }

    private static void populate(HashMap<String, HashMap<String, List<String>>> map, JSONArray source) {
        for (JSONObject entry : (Iterable<JSONObject>) source) {
            String id = (String) entry.get("id");

            // if user did not specify origin of block/item, we assume it is from vanilla Minecraft.
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }

            HashMap<String, List<String>> propertyEntries = new HashMap<>();
            if (entry.containsKey("properties")) {
                Object ObjectProperties = entry.get("properties");
                JSONObject JSONProperties = (ObjectProperties instanceof JSONArray) ? parseOldArrayToObject((JSONArray) entry.get("properties")) : (JSONObject) entry.get("properties");
                for (Object propertyName : JSONProperties.keySet()) {
                    JSONArray JSONPropertyValues = (JSONArray) JSONProperties.get(propertyName);
                    List<String> propertyValues = new ArrayList<>();
                    for (Object value : JSONPropertyValues) {
                        propertyValues.add(value.toString());
                    }
                    propertyEntries.put((String) propertyName, propertyValues);
                }
            }
            map.put(id, propertyEntries);
        }
    }

    private static JSONObject parseOldArrayToObject(JSONArray oldArray) {
        JSONObject jsonObject = new JSONObject();

        for (String propertyName : (Iterable<? extends String>) oldArray) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.add("all");
            jsonObject.put(propertyName, jsonArray);
        }

        return jsonObject;
    }

    public static boolean isBlockAllowed(Block block) {
        // 1) check if block has been mentioned in BLOCKS part
        String blockName = Registries.BLOCK.getId(block).toString();

        if (blocks_allowed.containsKey(blockName)) return true;
        if (blocks_forbidden.containsKey(blockName)) {
            return !blocks_forbidden.get(blockName).isEmpty();
        }
        // 2) if block is not stated in config, check tags
        Stream<TagKey<Block>> tagStream = block.getRegistryEntry().streamTags();
        List<TagKey<Block>> tagArray = tagStream.toList();
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.id().toString();
            if (tags_allowed.containsKey(tagName)) return true;
        }
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.id().toString();
            if (tags_forbidden.containsKey(tagName)) {
                return !tags_forbidden.get(tagName).isEmpty();
            }
        }
        
        // 3) if block has properties in allowed properties config
        for (Property<?> property : block.getStateManager().getProperties()) {
            if (properties_allowed.containsKey(property.getName()))
                return true;
        }
        
        // 4) if block, its block-states and tags were not clarified in config, check whitelist mode
        return !whitelist;
    }

    public static boolean isPropertyAllowed(String propertyName, @Nullable Block block) {
        if (block != null) {
            String blockName = Registries.BLOCK.getId(block).toString();
            // 1) Check for exactly this block
            if (blocks_forbidden.containsKey(blockName)) {
                HashMap<String, List<String>> forbidden_props = blocks_forbidden.get(blockName);
                if (forbidden_props.containsKey("all") ||
                    forbidden_props.getOrDefault(propertyName, new ArrayList<>()).contains("all")) return false;
            }
            if (blocks_allowed.containsKey(blockName)) {
                HashMap<String, List<String>> allowed_props = blocks_allowed.get(blockName);
                if (allowed_props.containsKey("all") || allowed_props.containsKey(propertyName)) return true;
                if (allowed_props.isEmpty()) {
                    return !properties_forbidden.getOrDefault(propertyName, new ArrayList<>()).contains("all");
                }
            }
            // 2) Either block is not stated in config or
            // allowed by itself, but does not speak about this property. Check its tags
            Stream<TagKey<Block>> tagStream = block.getRegistryEntry().streamTags();
            List<TagKey<Block>> tagArray = tagStream.toList();
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.id().toString();
                if (tags_forbidden.containsKey(tagName)) {
                    HashMap<String, List<String>> forbidden_props = tags_forbidden.get(tagName);
                    if (forbidden_props.containsKey("all") ||
                        forbidden_props.getOrDefault(propertyName, new ArrayList<>()).contains("all")) return false;
                }
            }
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.id().toString();
                if (tags_allowed.containsKey(tagName)) {
                    HashMap<String, List<String>> allowed_props = tags_allowed.get(tagName);
                    if (allowed_props.containsKey("all") || allowed_props.containsKey(propertyName)) return true;
                    if (allowed_props.isEmpty()) {
                        return !properties_forbidden.getOrDefault(propertyName, new ArrayList<>()).contains("all");
                    }
                }
            }
        }
        // 3) If tags do not speak about property, check global list
        if (properties_allowed.containsKey("all") || properties_allowed.containsKey(propertyName)) return true;
        if (properties_forbidden.containsKey(propertyName)) return !properties_forbidden.get(propertyName).contains("all");
        // 4) if property was not clarified in config, check whitelist mode
        return !whitelist;
    }

    public static boolean isPropertyValueAllowed(Block block, String propertyName, String value) {
        String blockName = Registries.BLOCK.getId(block).toString();
        // 1) Check for exactly this block
        if (blocks_forbidden.containsKey(blockName)) {
            HashMap<String, List<String>> forbidden_props = blocks_forbidden.get(blockName);
            if (forbidden_props.containsKey("all")) return false;
            List<String> values = forbidden_props.getOrDefault(propertyName, new ArrayList<>());
            if (values.contains("all") || values.contains(value)) return false;
        }
        if (blocks_allowed.containsKey(blockName)) {
            HashMap<String, List<String>> allowed_props = blocks_allowed.get(blockName);
            if (allowed_props.containsKey("all")) return true;
            List<String> values = allowed_props.getOrDefault(propertyName, new ArrayList<>());
            if (values.contains("all") || values.contains(value)) return true;
            if (allowed_props.isEmpty()) {
                return !properties_forbidden.getOrDefault(propertyName, new ArrayList<>()).contains("all");
            }
        }
        // 2) Either block is not stated in config or
        // allowed by itself, but does not speak about this property. Check its tags
        Stream<TagKey<Block>> tagStream = block.getRegistryEntry().streamTags();
        List<TagKey<Block>> tagArray = tagStream.toList();
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.id().toString();
            if (tags_forbidden.containsKey(tagName)) {
                HashMap<String, List<String>> forbidden_props = tags_forbidden.get(tagName);
                if (forbidden_props.containsKey("all")) return false;
                List<String> values = forbidden_props.getOrDefault(propertyName, new ArrayList<>());
                if (values.contains("all") || values.contains(value)) return false;
            }
        }
        for (TagKey<Block> tag : tagArray) {
            String tagName = tag.id().toString();
            if (tags_allowed.containsKey(tagName)) {
                HashMap<String, List<String>> allowed_props = tags_allowed.get(tagName);
                if (allowed_props.containsKey("all")) return true;
                List<String> values = allowed_props.getOrDefault(propertyName, new ArrayList<>());
                if (values.contains("all") || values.contains(value)) return true;
                if (allowed_props.isEmpty()) {
                    return !properties_forbidden.getOrDefault(propertyName, new ArrayList<>()).contains("all");
                }
            }
        }
        // 3) If tags do not speak about property, check global list
        if (properties_allowed.containsKey("all")) return true;
        if (properties_allowed.containsKey(propertyName)) {
            List<String> values = properties_allowed.get(propertyName);
            if (values.contains(value)) return true;
        }
        if (properties_forbidden.containsKey("all")) return false;
        if (properties_forbidden.containsKey(propertyName)) {
            List<String> values = properties_forbidden.get(propertyName);
            if (values.contains(value)) return false;
        }
        // 4) if property was not clarified in config, check whitelist mode
        return !whitelist;
    }
}
