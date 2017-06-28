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

import com.google.common.base.Predicate;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import pl.asie.charset.api.locks.ILockingEntity;
import pl.asie.charset.api.locks.Lockable;
import pl.asie.charset.lib.capability.Capabilities;

public class EntityLock extends EntityHanging implements IEntityAdditionalSpawnData, ILockingEntity {
    static final DataParameter<Integer> COLOR_0 = EntityDataManager.createKey(EntityLock.class, DataSerializers.VARINT);
    static final DataParameter<Integer> COLOR_1 = EntityDataManager.createKey(EntityLock.class, DataSerializers.VARINT);

    private static final DataParameter<EnumFacing> HANGING_ROTATION = EntityDataManager.createKey(EntityLock.class, DataSerializers.FACING);
    private static final Predicate<Entity> IS_HANGING_ENTITY = new Predicate<Entity>() {
        public boolean apply(Entity entity) {
            return entity instanceof EntityHanging;
        }
    };

    private String lockKey = null;
    protected int[] colors = new int[] { -1, -1 };
    private TileEntity tileCached;
    private boolean locked = true;

    public EntityLock(World worldIn) {
        super(worldIn);
    }

    public EntityLock(World worldIn, ItemStack stack, BlockPos pos, EnumFacing facing) {
        super(worldIn, pos);
        this.setColors(stack.getTagCompound());
        this.setLockKey(((ItemLock) stack.getItem()).getRawKey(stack));
        this.updateFacingWithBoundingBox(facing);
    }

    private void setColors(NBTTagCompound compound) {
        if (compound != null) {
            colors[0] = compound.hasKey("color0") ? compound.getInteger("color0") : -1;
            colors[1] = compound.hasKey("color1") ? compound.getInteger("color1") : -1;
        } else {
            colors[0] = -1;
            colors[1] = -1;
        }
    }

    private void setLockKey(String s) {
        this.lockKey = s;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setLockKey(compound.hasKey("key") ? compound.getString("key") : null);
        setColors(compound);
    }

    @Override
    public String getName() {
        if (this.hasCustomName()) {
            return this.getCustomNameTag();
        } else {
            return I18n.translateToLocal("item.charset.lock.name");
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (lockKey != null) {
            compound.setString("key", lockKey);
        }
        if (colors[0] != -1) {
            compound.setInteger("color0", colors[0]);
        }
        if (colors[1] != -1) {
            compound.setInteger("color1", colors[1]);
        }
    }

    public Lockable getAttachedLock() {
        TileEntity tile = getAttachedTile();
        if (tile != null && tile.hasCapability(Capabilities.LOCKABLE, null)) {
            Lockable lock = tile.getCapability(Capabilities.LOCKABLE, null);
            if (!lock.hasLock()) {
                if (LockEventHandler.getLock(tile) == null) {
                    lock.addLock(this);
                }
            }

            return lock.getLock() == this ? lock : null;
        } else {
            return null;
        }
    }

    public TileEntity getAttachedTile() {
        if (tileCached == null || tileCached.isInvalid()) {
            BlockPos pos = this.hangingPosition.offset(this.facingDirection.getOpposite());
            tileCached = world.getTileEntity(pos);
        }

        return tileCached;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.setEntityInvulnerable(true);
    }

    @Override
    public boolean hitByEntity(Entity entityIn) {
        if (entityIn instanceof EntityPlayer && entityIn.isSneaking()) {
            if (!this.world.isRemote) {
                ItemStack stack = ((EntityPlayer) entityIn).getHeldItemMainhand();
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemKey) || !(((ItemKey) stack.getItem()).canUnlock(lockKey, stack))) {
                    stack = ((EntityPlayer) entityIn).getHeldItemOffhand();
                    if (stack.isEmpty() || !(stack.getItem() instanceof ItemKey) || !(((ItemKey) stack.getItem()).canUnlock(lockKey, stack))) {
                        return super.hitByEntity(entityIn);
                    }
                }

                if (!this.isDead) {
                    this.setDead();
                    this.onBroken(entityIn);
                }

                return true;
            } else {
                return super.hitByEntity(entityIn);
            }
        }

        return super.hitByEntity(entityIn);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // TODO: Remove in 1.13
        if (!world.isRemote && getAttachedTile() instanceof ILockableContainer) {
            ILockableContainer container = (ILockableContainer) tileCached;
            if (container.isLocked() && container.getLockCode().getLock().startsWith("charset")) {
                container.setLockCode(LockCode.EMPTY_CODE);
            }
        }

        if (!world.isRemote && lockKey != null && getAttachedLock() == null) {
            drop();
        }
    }

    @Override
    public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && lockKey != null && getAttachedLock() != null) {
            boolean canUnlock = LockEventHandler.unlockOrRaiseError(player, getAttachedTile(), getAttachedLock());

            if (canUnlock) {
                Lockable lock = getAttachedLock();
                if (lock != null) {
                    locked = false;

                    BlockPos pos = this.hangingPosition.offset(this.facingDirection.getOpposite());
                    IBlockState state = world.getBlockState(pos);

                    state.getBlock().onBlockActivated(world, pos, state, player, hand, this.facingDirection,
                            0.5F + this.facingDirection.getFrontOffsetX() * 0.5F,
                            0.5F + this.facingDirection.getFrontOffsetY() * 0.5F,
                            0.5F + this.facingDirection.getFrontOffsetZ() * 0.5F
                    );

                    locked = true;
                }

                return EnumActionResult.SUCCESS;
            } else {
                return EnumActionResult.FAIL;
            }
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public float getCollisionBorderSize()
    {
        return 0.0F;
    }

    @Override
    public boolean onValidSurface() {
        if (getAttachedLock() == null) {
            return false;
        }

        if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            return false;
        } else {
            return this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox(), IS_HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public int getWidthPixels() {
        return 8;
    }

    @Override
    public int getHeightPixels() {
        return 8;
    }

    private ItemStack createItemStack(Item item) {
        ItemStack lock = new ItemStack(item);
        lock.setTagCompound(new NBTTagCompound());
        if (lockKey != null) {
            lock.getTagCompound().setString("key", lockKey);
        }
        if (colors[0] != -1) {
            lock.getTagCompound().setInteger("color0", colors[0]);
        }
        if (colors[1] != -1) {
            lock.getTagCompound().setInteger("color1", colors[1]);
        }
        return lock;
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return createItemStack(CharsetStorageLocks.keyItem);
    }

    public void drop() {
        Lockable lock = getAttachedLock();
        if (lock != null) {
            lock.removeLock(this);
        }
        this.entityDropItem(createItemStack(CharsetStorageLocks.lockItem), 0.0F);
        this.setDead();
    }

    @Override
    public void onBroken(Entity brokenEntity) {
        drop();
    }

    @Override
    public void playPlaceSound() {
        // TODO
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(facingDirection.ordinal());
        buffer.writeInt(colors[0]);
        buffer.writeInt(colors[1]);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.updateFacingWithBoundingBox(EnumFacing.getFront(buffer.readUnsignedByte()));
        colors[0] = buffer.readInt();
        colors[1] = buffer.readInt();
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public boolean isLockValid(TileEntity tile) {
        if (!isEntityAlive()) {
            return false;
        }

        if (tile == null) return true;
        if (getAttachedTile().getPos().equals(tile.getPos())) return true;

        return false;
    }

    @Override
    public int getLockEntityId() {
        return getEntityId();
    }

    @Override
    public String getLockKey() {
        return lockKey;
    }
}
