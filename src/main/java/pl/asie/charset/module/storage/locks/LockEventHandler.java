/*
 * Copyright (c) 2015-2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.asie.charset.module.storage.locks;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import pl.asie.charset.api.lib.IMultiblockStructure;
import pl.asie.charset.api.locks.Lockable;
import pl.asie.charset.api.storage.IKeyItem;
import pl.asie.charset.lib.CharsetIMC;
import pl.asie.charset.lib.capability.Capabilities;
import pl.asie.charset.lib.capability.CapabilityProviderFactory;
import pl.asie.charset.lib.utils.ThreeState;
import pl.asie.charset.module.tweaks.carry.CarryHandler;

import java.util.Iterator;

public class LockEventHandler {
    public static CapabilityProviderFactory<Lockable> PROVIDER;

    public static Lockable getLock(TileEntity tile) {
        if (tile != null) {
            if (tile.hasCapability(Capabilities.LOCKABLE, null)) {
                Lockable lock = tile.getCapability(Capabilities.LOCKABLE, null);
                if (lock.hasLock() && lock.getLock().isLockValid(tile) && lock.getLock().isLocked()) {
                    return lock;
                }
            }

            if (tile.hasCapability(Capabilities.MULTIBLOCK_STRUCTURE, null)) {
                IMultiblockStructure structure = tile.getCapability(Capabilities.MULTIBLOCK_STRUCTURE, null);
                Iterator<BlockPos> iterator = structure.iterator();
                while (iterator.hasNext()) {
                    TileEntity tile2 = tile.getWorld().getTileEntity(iterator.next());
                    if (tile2.hasCapability(Capabilities.LOCKABLE, null)) {
                        Lockable lock = tile2.getCapability(Capabilities.LOCKABLE, null);
                        if (lock.hasLock() && lock.getLock().isLockValid(tile2) && lock.getLock().isLocked()) {
                            return lock;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean unlockOrRaiseError(EntityPlayer player, TileEntity tile, Lockable lock) {
        if (player.getEntityWorld().isRemote) {
            return true;
        }

        ItemStack stack = player.getHeldItemMainhand();

        boolean canUnlock = false;
        if (!stack.isEmpty() && stack.getItem() instanceof IKeyItem) {
            IKeyItem key = (IKeyItem) stack.getItem();
            canUnlock = key.canUnlock(lock.getLock().getLockKey(), stack);
        }

        if (!canUnlock) {
            stack = player.getHeldItemOffhand();
            if (!stack.isEmpty() && stack.getItem() instanceof IKeyItem) {
                IKeyItem key = (IKeyItem) stack.getItem();
                canUnlock = key.canUnlock(lock.getLock().getLockKey(), stack);
            }
        }

        if (!canUnlock) {
            ITextComponent displayName = tile.getDisplayName();
            if (displayName == null) {
                displayName = new TextComponentTranslation(tile.getBlockType().getUnlocalizedName() + ".name");
            }
            player.sendStatusMessage(new TextComponentTranslation("container.isLocked", displayName), true);
            player.getEntityWorld().playSound(player, tile.getPos(), SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        return canUnlock;
    }

    private static final ResourceLocation LOCKABLE = new ResourceLocation("charset:lockable");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        TileEntity tile = event.getObject();
        ResourceLocation location = TileEntity.getKey(tile.getClass());
        ThreeState state = CharsetIMC.INSTANCE.allows("lock", location);
        boolean hasCap = state == ThreeState.YES;

        if (state == ThreeState.MAYBE) {
            if (tile instanceof TileEntityChest) {
                hasCap = true;
            }

            if ("minecraft".equals(location.getResourceDomain()) && tile instanceof TileEntityLockable) {
                hasCap = true;
            }
        }

        if (hasCap) {
            if (PROVIDER == null) {
                PROVIDER = new CapabilityProviderFactory<>(Capabilities.LOCKABLE, Capabilities.LOCKABLE_STORAGE);
            }
            event.addCapability(LOCKABLE, PROVIDER.create(new Lockable(tile)));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        Lockable lockable = getLock(tile);
        if (lockable != null && !unlockOrRaiseError(event.getEntityPlayer(), tile, lockable)) {
            event.setUseBlock(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        IBlockState state = event.getState();
        if (state.getBlock().hasTileEntity(state)) {
            TileEntity tile = event.getWorld().getTileEntity(event.getPos());
            Lockable lockable = getLock(tile);
            if (lockable != null && !unlockOrRaiseError(event.getPlayer(), tile, lockable)) {
                event.setCanceled(true);
            }
        }
    }
}
