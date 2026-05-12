package org.mfrf.templatetransformer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Templatetransformer.MODID)
public class Config {

        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.ConfigValue<List<? extends String>> SMITHING_TEMPLATE_BLACKLIST = BUILDER
                        .comment("Smithing template item id blacklist. Matching registered smithing template ids will be excluded.")
                        .defineListAllowEmpty("smithingTemplateBlacklist", List.of(), Config::validateResourceLocation);

        static final ModConfigSpec SPEC = BUILDER.build();

        public static Set<Identifier> smithingTemplateBlacklist = Set.of();

        private static boolean validateResourceLocation(final Object obj) {
                return obj instanceof String id && Identifier.tryParse(id) != null;
        }

        @SubscribeEvent
        static void onLoad(final ModConfigEvent event) {
                smithingTemplateBlacklist = SMITHING_TEMPLATE_BLACKLIST.get().stream()
                                .map(Identifier::parse)
                                .collect(Collectors.toSet());
        }

        public static boolean isBlacklistedSmithingTemplate(Item item) {
                if (!(item instanceof SmithingTemplateItem)) {
                        return false;
                }

                Identifier key = BuiltInRegistries.ITEM.getKey(item);
                return key != null && smithingTemplateBlacklist.contains(key);
        }
}
