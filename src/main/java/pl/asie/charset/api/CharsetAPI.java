/*
 * Copyright (c) 2015, 2016, 2017, 2018, 2019 Adrian Siekierka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.asie.charset.api;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import pl.asie.charset.api.lib.IBlockCapabilityProvider;
import pl.asie.charset.api.lib.ISimpleInstantiatingRegistry;

import javax.annotation.Nullable;

public class CharsetAPI {
	public static CharsetAPI INSTANCE = new CharsetAPI();

	public boolean isPresent() {
		return false;
	}

	public <T> boolean mayHaveBlockCapability(Capability<T> capability, IBlockState state) {
		return false;
	}

	@Nullable
	public final <T> T getBlockCapability(Capability<T> capability, IBlockAccess world, BlockPos pos, EnumFacing facing) {
		return getBlockCapability(capability, world, pos, world.getBlockState(pos), facing);
	}

	@Nullable
	public <T> T getBlockCapability(Capability<T> capability, IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing facing) {
		return null;
	}

	// The following methods should be called after isPresent() ONLY.

	public <T> void registerBlockCapabilityProvider(Capability<T> capability, Block block, IBlockCapabilityProvider<T> provider) {
		throw new RuntimeException("Charset API not initialized - please use isPresent()!");
	}

	public <T> ISimpleInstantiatingRegistry<T> findSimpleInstantiatingRegistry(Class<T> c) {
		throw new RuntimeException("Charset API not initialized - please use isPresent()!");
	}
}
