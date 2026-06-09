package org.mfrf.templatetransformer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingRecipe;
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
            case None _ -> getBuiltInSmithingTemplateIds();
            default -> throw new IllegalStateException("unreachable!");
        };
    }

    public static void reloadTemplates(ReloadableServerResources serverResources) {
        reloadTemplates(serverResources.getRecipeManager());
    }

    public static void reloadTemplates(RecipeManager recipeManager) {
        templateList = new ValidList(recipeManager.getRecipes().stream()
                .map(recipeHolder -> recipeHolder.value())
                .filter(SmithingRecipe.class::isInstance)
                .map(SmithingRecipe.class::cast)
                .flatMap(recipe -> recipe.templateIngredient().stream())
                .flatMap(ingredient -> ingredient.getValues().stream().map(IHolderExtension::getKey))
                .distinct()
                .toList());
    }

    private static List<Identifier> getBuiltInSmithingTemplateIds() {
        return BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof SmithingTemplateItem)
                .map(entry -> entry.getKey().identifier())
                .sorted()
                .toList();
    }

    private static void ensureTemplatesLoaded(ServerLevel level) {
        if (templateList instanceof None) {
            reloadTemplates(level.getServer().getRecipeManager());
        }
    }
}
