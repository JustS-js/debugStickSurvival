package net.just_s.sds.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record SDSConfig (
        Rules allowed,
        Rules forbidden,
        boolean whitelist
) {
    public static final Codec<SDSConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Rules.CODEC.fieldOf("allowed").forGetter(SDSConfig::allowed),
                    Rules.CODEC.fieldOf("forbidden").forGetter(SDSConfig::forbidden),
                    Codec.BOOL.fieldOf("whitelist").orElse(true).forGetter(SDSConfig::whitelist)
            ).apply(instance, SDSConfig::new)
    );

    public SDSConfig() {
        this(
                new Rules(
                        List.of(
                                new Entry("iron_bars", Map.of()),
                                new Entry("bamboo", Map.of(
                                                "leaves", List.of("none", "large"),
                                                "age", List.of("all")
                                        )
                                )
                        ),
                        Map.of(),
                        List.of(
                                new Entry("stairs", Map.of()),
                                new Entry("walls", Map.of()),
                                new Entry("c:glass_panes", Map.of()),
                                new Entry("fences", Map.of()),
                                new Entry("slabs", Map.of(
                                                "type", List.of("top", "bottom")
                                        )
                                )
                        )
                ),
                new Rules(
                        List.of(),
                        Map.of("waterlogged", List.of("all")),
                        List.of()
                ),
                true
        );
    }

    public record Rules(
            List<Entry> blocks,
            Map<String, List<String>> properties,
            List<Entry> tags
    ) {
        static Codec<Rules> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.list(Entry.CODEC).fieldOf("blocks").forGetter(SDSConfig.Rules::blocks),
                        Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).fieldOf("properties").forGetter(SDSConfig.Rules::properties),
                        Codec.list(Entry.CODEC).fieldOf("tags").forGetter(SDSConfig.Rules::tags)
                ).apply(instance, SDSConfig.Rules::new)
        );

        public Entry getBlockEntry(String key) {
            for (Entry entry : blocks) {
                String id = entry.id();
                // if user did not specify origin of block/item, we assume it is from vanilla Minecraft.
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }
                if (id.equals(key)) {
                    return entry;
                }
            }
            return null;
        }

        public boolean containsBlock(String key) {
            return getBlockEntry(key) != null;
        }

        public Entry getTagEntry(String key) {
            for (Entry entry : tags) {
                String id = entry.id();
                // if user did not specify origin of block/item, we assume it is from vanilla Minecraft.
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }
                if (id.equals(key)) {
                    return entry;
                }
            }
            return null;
        }

        public boolean containsTag(String key) {
            return getTagEntry(key) != null;
        }
    }

    public record Entry(
            String id,
            Map<String, List<String>> properties
    ) {
        static Codec<Entry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("id").forGetter(Entry::id),
                        Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).fieldOf("properties").forGetter(Entry::properties)
                ).apply(instance, Entry::new)
        );

        public boolean isEmpty() {
            return properties == null || properties.isEmpty();
        }
    }
}
