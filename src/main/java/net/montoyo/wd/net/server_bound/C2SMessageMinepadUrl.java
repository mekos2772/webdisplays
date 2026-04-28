package net.montoyo.wd.net.server_bound;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.net.Packet;

import java.util.UUID;

public class C2SMessageMinepadUrl extends Packet {
    static { Packet.registerType(C2SMessageMinepadUrl.class, "minepad_url"); }
    public static final StreamCodec<FriendlyByteBuf, C2SMessageMinepadUrl> STREAM_CODEC = Packet.streamCodec(C2SMessageMinepadUrl::new);
	UUID id;
	String url;
	
	public C2SMessageMinepadUrl(UUID id, String url) {
		this.id = id;
		this.url = url;
	}
	
	public C2SMessageMinepadUrl(FriendlyByteBuf buf) {
		super(buf);
		this.id = buf.readUUID();
		this.url = buf.readUtf();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(id);
		buf.writeUtf(url);
	}
	
	private static CompoundTag getTag(ItemStack stack) {
		return stack.has(DataComponents.CUSTOM_DATA) ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : null;
	}

	protected void merge(ItemStack stack) {
		CompoundTag tag = getTag(stack);
		if (url.equals("")) {
			if (tag != null) {
				tag.remove("PadID");
				if (tag.isEmpty())
					stack.remove(DataComponents.CUSTOM_DATA);
				else
					stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
		} else {
			if (tag == null)
				tag = new CompoundTag();
			tag.putUUID("PadID", id);
			tag.putString("PadURL", url);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}
	
	@Override
	public void handle(IPayloadContext ctx) {
		ServerPlayer sender = (ServerPlayer) ctx.player();
		// check if the player is holding a minePad with the requested id
		// if the player is, then update that pad
		for (InteractionHand value : InteractionHand.values()) {
			ItemStack stack = sender.getItemInHand(value);
			CompoundTag tag = getTag(stack);
			if (stack.getItem() instanceof ItemMinePad2 && tag != null && tag.contains("PadID")) {
				UUID padId = tag.getUUID("PadID");
				if (padId.equals(id)) {
					merge(stack);
					return;
				}
			}
		}
		
		// if the player is not holding the requested minePad, update the first one that does not already have an ID
		for (InteractionHand value : InteractionHand.values()) {
			ItemStack stack = sender.getItemInHand(value);
			CompoundTag tag = getTag(stack);
			if (stack.getItem() instanceof ItemMinePad2 && (tag == null || !tag.contains("PadID"))) {
				merge(stack);
				return;
			}
		}
	}
}
