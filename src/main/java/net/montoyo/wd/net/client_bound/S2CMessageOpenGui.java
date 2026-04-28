/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.GuiData;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.Log;

public class S2CMessageOpenGui extends Packet {
    static { Packet.registerType(S2CMessageOpenGui.class, "open_gui"); }
    public static final StreamCodec<FriendlyByteBuf, S2CMessageOpenGui> STREAM_CODEC = Packet.streamCodec(S2CMessageOpenGui::new);
	private GuiData data;
	
	public S2CMessageOpenGui(GuiData data) {
		this.data = data;
	}
	
	public S2CMessageOpenGui(FriendlyByteBuf buf) {
		super(buf);
		
		String name = buf.readUtf();
		data = GuiData.read(name, buf);
		Class<? extends GuiData> cls = GuiData.classOf(name);
		
		if (cls == null) {
			Log.error("Could not create GuiData of type %s because it doesn't exist!", name);
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(data.getName());
		data.serialize(buf);
	}
	
	public void handle(IPayloadContext context) {
		if (checkClient(context)) {
			context.enqueueWork(() -> WebDisplays.PROXY.displayGui(data));
		}
	}
}
