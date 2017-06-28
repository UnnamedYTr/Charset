package pl.asie.charset.lib.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;
import pl.asie.charset.lib.material.ItemMaterial;
import pl.asie.charset.lib.material.ItemMaterialRegistry;
import pl.asie.charset.lib.utils.ItemUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IngredientMaterialFactory implements IIngredientFactory {
    public static class Ingredient extends IngredientCharset {
        private final String[] chain;
        private final TCharSet dependencies;
        private final String[] types;
        private final String nbtTag;
        private net.minecraft.item.crafting.Ingredient dependency;

        protected Ingredient(String nbtTag, String... types) {
            super(0);
            this.types = types;
            this.nbtTag = nbtTag;
            this.chain = null;
            this.dependencies = null;
        }

        protected Ingredient(String nbtTag, String chain, boolean dummy, String... types) {
            super(0);
            this.types = types;
            this.nbtTag = nbtTag;
            this.chain = chain.split("\\.");
            this.dependencies = new TCharHashSet();
            dependencies.add(this.chain[0].charAt(0));
        }

        @Override
        public TCharSet getDependencies() {
            return dependencies;
        }

        @Override
        public void addDependency(char c, net.minecraft.item.crafting.Ingredient i) {
            if (chain != null && c == chain[0].charAt(0)) {
                dependency = i;
            }
        }

        @Override
        public boolean mustIteratePermutations() {
            return super.mustIteratePermutations() || chain != null || nbtTag != null;
        }

        private ItemMaterial getChainedMaterial(ItemMaterial base) {
            for (int i = 1; i < chain.length; i++) {
                if (chain[i].charAt(0) == '?') {
                    ItemMaterial nextBase = base.getRelated(chain[i].substring(1));
                    if (nextBase != null)
                        base = nextBase;
                } else {
                    base = base.getRelated(chain[i]);
                    if (base == null)
                        return null;
                }
            }

            return base;
        }

        @Override
        public boolean apply(IngredientMatcher matcher, ItemStack stack) {
            if (chain != null) {
                ItemStack stackIn = matcher.getStack(dependency);
                if (!stackIn.isEmpty()) {
                    ItemMaterial base = getChainedMaterial(ItemMaterialRegistry.INSTANCE.getOrCreateMaterial(stackIn));
                    return base != null && ItemMaterialRegistry.INSTANCE.matches(stack, base);
                } else {
                    return false;
                }
            } else {
                return apply(stack);
            }
        }

        @Override
        public void applyToStack(ItemStack stack, ItemStack source) {
            if (nbtTag != null) {
                ItemUtils.getTagCompound(stack, true).setString(nbtTag, ItemMaterialRegistry.INSTANCE.getOrCreateMaterial(source).getId());
            }
        }

        @Override
        public ItemStack[] getMatchingStacks() {
            if (chain != null) {
                Collection<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : dependency.getMatchingStacks()) {
                    ItemMaterial materialOut = getChainedMaterial(ItemMaterialRegistry.INSTANCE.getOrCreateMaterial(stack));
                    if (materialOut != null) {
                        stacks.add(materialOut.getStack());
                    }
                }

                return stacks.toArray(new ItemStack[stacks.size()]);
            } else {
                Collection<ItemMaterial> mats = ItemMaterialRegistry.INSTANCE.getMaterialsByTypes(types);
                ItemStack[] stacks = new ItemStack[mats.size()];
                int idx = 0;
                for (ItemMaterial material : mats) {
                    stacks[idx++] = material.getStack();
                }
                return stacks;
            }
        }

        @Override
        public boolean apply(@Nullable ItemStack stack) {
            if (stack == null)
                return false;

            return ItemMaterialRegistry.INSTANCE.matches(stack, types);
        }
    }

    @Nonnull
    @Override
    public Ingredient parse(JsonContext jsonContext, JsonObject jsonObject) {
        String tag = JsonUtils.getString(jsonObject, "nbtKey");
        String[] material;

        JsonElement oreElem = jsonObject.get("material");
        if (oreElem instanceof JsonArray) {
            JsonArray array = oreElem.getAsJsonArray();
            material = new String[array.size()];
            for (int i = 0; i < array.size(); i++)
                material[i] = array.get(i).getAsString();
        } else {
            material = new String[]{ JsonUtils.getString(jsonObject, "material") };
        }

        if (jsonObject.has("chain")) {
            return new Ingredient(tag, JsonUtils.getString(jsonObject, "chain"), false, material);
        } else {
            return new Ingredient(tag, material);
        }
    }
}
