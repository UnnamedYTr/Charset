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

package pl.asie.charset.pipes.pipe;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import pl.asie.charset.lib.network.PacketPart;
import pl.asie.charset.lib.network.PacketTile;
import pl.asie.charset.pipes.PipeUtils;

public class PacketPipeSyncRequest extends PacketTile {
	private EntityPlayer requester;

	public PacketPipeSyncRequest() {
		super();
	}

	public PacketPipeSyncRequest(TileEntity part) {
		super(part);
	}

	@Override
	public void readData(INetHandler handler, ByteBuf buf) {
		super.readData(handler, buf);
		requester = handler instanceof NetHandlerPlayServer ? ((NetHandlerPlayServer) handler).playerEntity : null;
	}

	@Override
	public void apply() {
		if (requester == null || tile == null || !(tile instanceof TilePipe)) {
			return;
		}

		PipeUtils.getPipe(tile).onSyncRequest(requester);
	}

	@Override
	public boolean isAsynchronous() {
		return false;
	}
}
