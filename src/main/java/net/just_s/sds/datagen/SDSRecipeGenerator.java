package net.just_s.sds.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class SDSRecipeGenerator extends FabricRecipeProvider {
    public SDSRecipeGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public @NonNull String getName() {
        return "";
    }


    @Override
    protected @NonNull RecipeProvider createRecipeProvider(HolderLookup.@NonNull Provider registries, @NonNull RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                shapeless(RecipeCategory.TOOLS, Items.DEBUG_STICK)
                        .requires(Items.STICK).unlockedBy(getHasName(Items.STICK), has(Items.STICK))
                        .requires(Items.CHORUS_FRUIT).unlockedBy(getHasName(Items.CHORUS_FRUIT), has(Items.CHORUS_FRUIT))
                        .save(output, ResourceKey.create(
                                Registries.RECIPE,
                                Identifier.fromNamespaceAndPath("sds", getSimpleRecipeName(Items.DEBUG_STICK))
                        ));
            }
        };
    }
}
