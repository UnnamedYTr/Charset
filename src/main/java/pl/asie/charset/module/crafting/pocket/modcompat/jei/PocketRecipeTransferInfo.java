/*
 * Copyright (c) 2015, 2016, 2017, 2018, 2019 Adrian Siekierka
 *
 * This file is part of Charset.
 *
 * Charset is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Charset is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Charset.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.charset.module.crafting.pocket.modcompat.jei;

import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.inventory.Slot;
import pl.asie.charset.module.crafting.pocket.ContainerPocketTable;

import java.util.List;

public class PocketRecipeTransferInfo implements IRecipeTransferInfo<ContainerPocketTable> {
	@Override
	public Class<ContainerPocketTable> getContainerClass() {
		return ContainerPocketTable.class;
	}

	@Override
	public String getRecipeCategoryUid() {
		return VanillaRecipeCategoryUid.CRAFTING;
	}

	@Override
	public boolean canHandle(ContainerPocketTable container) {
		return true;
	}

	@Override
	public List<Slot> getRecipeSlots(ContainerPocketTable container) {
		return container.getCraftingSlots();
	}

	@Override
	public List<Slot> getInventorySlots(ContainerPocketTable container) {
		return container.getNonCraftingSlots();
	}
}
