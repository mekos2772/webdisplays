/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.net.server_bound.C2SMessageMiniservConnect;

public class S2CMessageServerInfo extends Packet {
    static { Packet.registerType(S2CMessageServerInfo.class, "server_info"); }
    public static final StreamCodec<FriendlyByteBuf, S2CMessageServerInfo> STREAM_CODEC = Packet.streamCodec(S2CMessageServerInfo::new);
	
	private int miniservPort;
	
	public S2CMessageServerInfo(int msPort) {
		miniservPort = msPort;
	}
	
	public S2CMessageServerInfo(FriendlyByteBuf buf) {
		super(buf);
		miniservPort = buf.readShort();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeShort(miniservPort);
	}
	
	@Override
	public void handle(IPayloadContext ctx) {
		if (checkClient(ctx)) {
			try {
				WebDisplays.PROXY.setMiniservClientPort(miniservPort);
				C2SMessageMiniservConnect message = Client.getInstance().beginConnection();
				respond(ctx, message);
			} catch (Throwable err) {
				err.printStackTrace();
				throw new RuntimeException(err);
			}
		}
	}
}
