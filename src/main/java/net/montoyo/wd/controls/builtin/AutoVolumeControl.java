package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class AutoVolumeControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("webdisplays", "auto_volume");
	
	boolean autoVol;
	
	public AutoVolumeControl(boolean autoVol) {
		super(id);
		this.autoVol = autoVol;
	}
	
	public AutoVolumeControl(FriendlyByteBuf buf) {
		super(id);
		autoVol = buf.readBoolean();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(autoVol);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		ServerPlayer player = (ServerPlayer) ctx.player();
		// I feel like there's probably a better permission category
		checkPerms(ScreenRights.MANAGE_UPGRADES, permissionChecker, player);
		tes.setAutoVolume(side, autoVol);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		tes.setAutoVolume(side, autoVol);
	}
}
