package net.just_s.sds;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.*;

public class Config {
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("SDS.json").toFile();

    public static String MESSAGE_nomodify;
    public static String MESSAGE_select;
    public static String MESSAGE_change;

    public static boolean whitelist;
    private static HashMap<String, Boolean> properties = new HashMap<>();

    private static HashMap<String, List<String>> tags_allowed = new HashMap<>();
    private static HashMap<String, List<String>> blocks_allowed = new HashMap<>();
    private static HashMap<String, List<String>> tags_forbidden = new HashMap<>();
    private static HashMap<String, List<String>> blocks_forbidden = new HashMap<>();

    public static void load() {
        JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader(configFile)) {

            JSONObject jfile = (JSONObject) parser.parse(reader);

            whitelist = (boolean) jfile.get("whitelist");

            JSONObject messages = (JSONObject) jfile.get("messages");
            MESSAGE_nomodify = (String) messages.get("nomodify");
            MESSAGE_select = (String) messages.get("select");
            MESSAGE_change = (String) messages.get("change");

            JSONObject allowed = (JSONObject) jfile.get("allowed");
            JSONArray properties_allowed = (JSONArray) allowed.get("properties");
            JSONArray tags_allowed_js = (JSONArray) allowed.get("tags");
            JSONArray blocks_allowed_js = (JSONArray) allowed.get("blocks");

            JSONObject forbidden = (JSONObject) jfile.get("forbidden");
            JSONArray properties_forbidden = (JSONArray) forbidden.get("properties");
            JSONArray tags_forbidden_js = (JSONArray) forbidden.get("tags");
            JSONArray blocks_forbidden_js = (JSONArray) forbidden.get("blocks");

            for (String property : (Iterable<String>) properties_allowed) {
                properties.put(property, true);
            }
            for (String property : (Iterable<String>) properties_forbidden) {
                properties.put(property, false);
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
        MESSAGE_nomodify = "This block is not modifiable.";
        MESSAGE_select = "Property «%s» was selected (%s).";
        MESSAGE_change = "Property «%s» was modified (%s).";
        properties = new HashMap<>();
        tags_allowed = new HashMap<>();
        tags_forbidden = new HashMap<>();
        blocks_allowed = new HashMap<>();
        blocks_forbidden = new HashMap<>();
        save();
    }

    public static void save() {
        JSONObject jfile = new JSONObject();
        jfile.put("whitelist", whitelist);

        JSONObject messages = new JSONObject();
        messages.put("nomodify", MESSAGE_nomodify);
        messages.put("select", MESSAGE_select);
        messages.put("change", MESSAGE_change);
        jfile.put("messages", messages);

        JSONObject allowed = new JSONObject();
        JSONArray properties_allowed = new JSONArray();
        for (Map.Entry<String, Boolean> entry : properties.entrySet()) {
            if (entry.getValue()) properties_allowed.add(entry.getKey());
        }
        allowed.put("properties", properties_allowed);

        JSONArray tags_allowed_js = new JSONArray();
        for (Map.Entry<String, List<String>> entry : tags_allowed.entrySet()) {
            JSONObject tag = new JSONObject();
            tag.put("id", entry.getKey());

            List<String> list = entry.getValue();
            if (!list.isEmpty()) {
                JSONArray props = new JSONArray();
                for (String property : list) {
                    props.add(property);
                }
                tag.put("properties", props);
            }
            tags_allowed_js.add(tag);
        }
        allowed.put("tags", tags_allowed_js);

        JSONArray blocks_allowed_js = new JSONArray();
        for (Map.Entry<String, List<String>> entry : blocks_allowed.entrySet()) {
            JSONObject block = new JSONObject();
            block.put("id", entry.getKey());

            List<String> list = entry.getValue();
            if (!list.isEmpty()) {
                JSONArray props = new JSONArray();
                for (String property : list) {
                    props.add(property);
                }
                block.put("properties", props);
            }
            blocks_allowed_js.add(block);
        }
        allowed.put("blocks", blocks_allowed_js);

        jfile.put("allowed", allowed);

        JSONObject forbidden = new JSONObject();
        JSONArray properties_forbidden = new JSONArray();
        for (Map.Entry<String, Boolean> entry : properties.entrySet()) {
            if (!entry.getValue()) properties_forbidden.add(entry.getKey());
        }
        forbidden.put("properties", properties_forbidden);

        JSONArray tags_forbidden_js = new JSONArray();
        for (Map.Entry<String, List<String>> entry : tags_forbidden.entrySet()) {
            JSONObject tag = new JSONObject();
            tag.put("id", entry.getKey());

            List<String> list = entry.getValue();
            if (!list.isEmpty()) {
                JSONArray props = new JSONArray();
                for (String property : list) {
                    props.add(property);
                }
                tag.put("properties", props);
            }
            tags_forbidden_js.add(tag);
        }
        forbidden.put("tags", tags_forbidden_js);

        JSONArray blocks_forbidden_js = new JSONArray();
        for (Map.Entry<String, List<String>> entry : blocks_forbidden.entrySet()) {
            JSONObject block = new JSONObject();
            block.put("id", entry.getKey());

            List<String> list = entry.getValue();
            if (!list.isEmpty()) {
                JSONArray props = new JSONArray();
                for (String property : list) {
                    props.add(property);
                }
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

    private static void populate(HashMap<String, List<String>> map, JSONArray source) {
        for (JSONObject entry : (Iterable<JSONObject>) source) {
            String id = (String) entry.get("id");

            // if user did not specify origin of block/item, we assume it is from vanilla Minecraft.
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }

            List<String> list = new ArrayList<>();
            if (entry.containsKey("properties")) {
                JSONArray props = (JSONArray) entry.get("properties");
                for (String property : (Iterable<String>) props) {
                    list.add(property);
                }
            }
            map.put(id, list);
        }
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
            if (properties.getOrDefault(property.getName(), false))
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
                List<String> forbidden_props = blocks_forbidden.get(blockName);
                if (forbidden_props.contains(propertyName)) return false;
            }
            if (blocks_allowed.containsKey(blockName)) {
                List<String> allowed_props = blocks_allowed.get(blockName);
                if (allowed_props.contains(propertyName)) return true;
                if (allowed_props.isEmpty()) {
                    if (properties.containsKey(propertyName)) return properties.get(propertyName);
                    return true;
                }
            }
            // 2) Either block is not stated in config or
            // allowed by itself, but does not speak about property. Check its tags
            Stream<TagKey<Block>> tagStream = block.getRegistryEntry().streamTags();
            List<TagKey<Block>> tagArray = tagStream.toList();
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.id().toString();
                if (tags_forbidden.containsKey(tagName)) {
                    List<String> forbidden_props = tags_forbidden.get(tagName);
                    if (forbidden_props.contains(propertyName)) return false;
                }
            }
            for (TagKey<Block> tag : tagArray) {
                String tagName = tag.id().toString();
                if (tags_allowed.containsKey(tagName)) {
                    List<String> allowed_props = tags_allowed.get(tagName);
                    if (allowed_props.contains(propertyName)) return true;
                    if (allowed_props.isEmpty()) {
                        if (properties.containsKey(propertyName)) return properties.get(propertyName);
                        return true;
                    }
                }
            }
        }
        // 3) If tags do not speak about property, check global list
        if (properties.containsKey(propertyName)) return properties.get(propertyName);
        // 4) if property was not clarified in config, check whitelist mode
        return !whitelist;
    }
}
