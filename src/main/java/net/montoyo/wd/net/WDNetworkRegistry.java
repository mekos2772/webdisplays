package net.montoyo.wd.net;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.montoyo.wd.net.client_bound.*;
import net.montoyo.wd.net.server_bound.*;

public class WDNetworkRegistry {
    public static final String networkingVersion = "2";
    public static WDNetworkRegistry INSTANCE = new WDNetworkRegistry();

    public static void init(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(networkingVersion);

        // login handshake
        registrar.playToClient(Packet.typeOf(S2CMessageServerInfo.class), S2CMessageServerInfo.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(Packet.typeOf(C2SMessageMiniservConnect.class), C2SMessageMiniservConnect.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(Packet.typeOf(S2CMessageMiniservKey.class), S2CMessageMiniservKey.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // guis
        registrar.playToClient(Packet.typeOf(S2CMessageCloseGui.class), S2CMessageCloseGui.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(Packet.typeOf(S2CMessageOpenGui.class), S2CMessageOpenGui.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // screen creation
        registrar.playToClient(Packet.typeOf(S2CMessageAddScreen.class), S2CMessageAddScreen.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // screen modifications
        registrar.playToServer(Packet.typeOf(C2SMessageScreenCtrl.class), C2SMessageScreenCtrl.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToServer(Packet.typeOf(C2SMessageScreenUrl.class), C2SMessageScreenUrl.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(Packet.typeOf(S2CMessageScreenUpdate.class), S2CMessageScreenUpdate.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // redstone control
        registrar.playToServer(Packet.typeOf(C2SMessageRedstoneCtrl.class), C2SMessageRedstoneCtrl.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // autocomplete
        registrar.playToServer(Packet.typeOf(C2SMessageACQuery.class), C2SMessageACQuery.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(Packet.typeOf(S2CMessageACResult.class), S2CMessageACResult.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // jsquery
        registrar.playToClient(Packet.typeOf(S2CMessageJSResponse.class), S2CMessageJSResponse.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));

        // minepad
        registrar.playToServer(Packet.typeOf(C2SMessageMinepadUrl.class), C2SMessageMinepadUrl.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
    }

    public static void sendToServer(Packet packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Packet packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToNear(Level level, BlockPos pos, Packet packet) {
        if (level instanceof ServerLevel sl)
            PacketDistributor.sendToPlayersTrackingChunk(sl, sl.getChunkAt(pos).getPos(), packet);
    }

    public static void sendToNearExcept(Player exclude, Level level, BlockPos pos, Packet packet) {
        if (level instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                if (sp != exclude && sp.blockPosition().distSqr(pos) < 4096)
                    PacketDistributor.sendToPlayer(sp, packet);
            }
        }
    }
}
