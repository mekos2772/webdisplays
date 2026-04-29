package net.montoyo.wd.net.server_bound;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.data.BlockSide;

import java.io.IOException;

public class C2SMessageScreenUrl extends Packet {
    static { Packet.registerType(C2SMessageScreenUrl.class, "screen_url"); }
    public static final StreamCodec<FriendlyByteBuf, C2SMessageScreenUrl> STREAM_CODEC = Packet.streamCodec(C2SMessageScreenUrl::new);

    private final BlockPos pos;
    private final BlockSide side;
    private final String url;

    public C2SMessageScreenUrl(BlockPos pos, BlockSide side, String url) {
        this.pos = pos;
        this.side = side;
        this.url = url;
    }

    public C2SMessageScreenUrl(FriendlyByteBuf buf) {
        super(buf);
        this.pos = buf.readBlockPos();
        this.side = (BlockSide) BufferUtils.readEnum(buf, i -> BlockSide.values()[i], (byte) 1);
        this.url = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        BufferUtils.writeEnum(buf, side, (byte) 1);
        buf.writeUtf(url);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (!checkServer(ctx))
            return;

        ctx.enqueueWork(() -> {
            Level level = (Level) WebDisplays.PROXY.getWorld(ctx);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ScreenBlockEntity tes))
                return;

            ServerPlayer player = (ServerPlayer) ctx.player();
            var screen = tes.getScreen(side);
            if (screen == null || (screen.rightsFor(player) & ScreenRights.INTERACT) == 0)
                return;

            try {
                tes.setScreenURL(side, url);
            } catch (IOException ignored) {
            }
        });
    }
}
