package net.just_s.sds;

import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class Config {
    public static String MESSAGE_nomodify = "Этот блок нельзя изменять.";
    public static String MESSAGE_select = "Параметр «%s» выбран (%s)";
    public static String MESSAGE_change = "Параметр «%s» изменён (%s)";

    public static boolean whitelist = false;
    private static List<String> properties = new ArrayList<>();
    private static List<String> blocks = new ArrayList<>();
    private static List<String> tags = new ArrayList<>();

    private static HashMap<String, List<String>> blockToProperties = new HashMap<>();

    public static boolean isInList(String property) {
        return properties.contains(property);
    }

    public static boolean isInList(Block block) {
        Stream<TagKey<Block>> tagStream = block.getRegistryEntry().streamTags();
        List<TagKey<Block>> tagArray = tagStream.toList();
        for (TagKey<Block> tag : tagArray) {
            if (tags.contains(tag.toString())) return true;
        }

        return blocks.contains(block.toString());
    }

    public static boolean isPropertyAllowed(String propertyName, @Nullable Block block) {
        if (isInList(propertyName)) {return whitelist;}
        if (block == null) {return !whitelist;}
        List<String> propertiesOfBlock = blockToProperties.get(block.toString());
        boolean bl = propertiesOfBlock == null || propertiesOfBlock.contains(propertyName);
        return whitelist == bl;
    }

    public static boolean isPropertyBanned(String propertyName, @Nullable Block block) {
        return !isPropertyAllowed(propertyName, block);
    }
}
