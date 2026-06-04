package org.mfrf.templatetransformer;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.extensions.IHolderExtension;

import java.util.List;

public class Util {
    private interface TemplateListState {}
    private record ValidList(List<ResourceKey<Item>> list) implements TemplateListState {}
    private record None() implements TemplateListState {}

    private static TemplateListState templateList = new None();

    public static boolean isValidUpgradePattern(ServerLevel level, ItemStack itemStack) {
        ensureTemplatesLoaded(level);
        return switch (templateList) {
            case ValidList validList -> validList.list().stream().anyMatch(itemStack::is);
            case None _ -> false;
            default -> throw new IllegalStateException("unreachable!");
        };
    }

    public static List<Identifier> getRegisteredSmithingTemplateIds() {
        return switch (templateList) {
            case ValidList validList -> validList.list().stream()
                    .map(ResourceKey::identifier)
                    .toList();
            case None _ -> List.of();
            default -> throw new IllegalStateException("unreachable!");
        };
    }

    private static void ensureTemplatesLoaded(ServerLevel level) {
        if (templateList instanceof None) {
            templateList = new ValidList(level.recipeAccess()
                    .recipeMap()
                    .byType(RecipeType.SMITHING)
                    .stream()
                    .flatMap(recipeHolder -> recipeHolder.value().templateIngredient().stream())
                    .flatMap(ingredient -> ingredient.getValues().stream().map(IHolderExtension::getKey))
                    .distinct()
                    .toList());
        }
    }
}
