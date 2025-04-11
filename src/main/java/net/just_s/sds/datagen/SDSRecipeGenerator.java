package net.just_s.sds.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class SDSRecipeGenerator extends FabricRecipeProvider {
    public SDSRecipeGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {
                createShapeless(RecipeCategory.TOOLS, Items.DEBUG_STICK)
                        .input(Items.STICK).criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
                        .input(Items.CHORUS_FRUIT).criterion(hasItem(Items.CHORUS_FRUIT), conditionsFromItem(Items.CHORUS_FRUIT))
                        .offerTo(recipeExporter, RegistryKey.of(
                                RegistryKeys.RECIPE,
                                Identifier.of("sds", getRecipeName(Items.DEBUG_STICK))
                        ));
            }
        };
    }

    @Override
    public String getName() {
        return "";
    }


}
