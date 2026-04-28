package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.utilities.DistSafety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Packet implements CustomPacketPayload {

    private static final Map<Class<? extends Packet>, CustomPacketPayload.Type<?>> TYPE_MAP = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected static <T extends Packet> CustomPacketPayload.Type<T> registerType(Class<T> clazz, String name) {
        CustomPacketPayload.Type<T> type = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("webdisplays", name));
        TYPE_MAP.put(clazz, type);
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> CustomPacketPayload.Type<T> typeOf(Class<T> clazz) {
        CustomPacketPayload.Type<?> type = TYPE_MAP.get(clazz);
        if (type == null) {
            try {
                Class.forName(clazz.getName(), true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to initialize packet class: " + clazz.getName(), e);
            }

            type = TYPE_MAP.get(clazz);
        }
        if (type == null)
            throw new IllegalStateException("Packet type not registered: " + clazz.getName());
        return (CustomPacketPayload.Type<T>) type;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        CustomPacketPayload.Type<?> t = TYPE_MAP.get(getClass());
        if (t == null) throw new IllegalStateException("Packet type not registered: " + getClass().getName());
        return t;
    }

    public Packet() {
    }

    public Packet(FriendlyByteBuf buf) {
    }

    public abstract void write(FriendlyByteBuf buf);

    public abstract void handle(IPayloadContext ctx);

    public boolean checkClient(IPayloadContext ctx) {
        return ctx.flow().isClientbound();
    }

    public boolean checkServer(IPayloadContext ctx) {
        return !ctx.flow().isClientbound();
    }

    public void respond(IPayloadContext ctx, Packet packet) {
        ctx.enqueueWork(() -> ctx.reply(packet));
    }

    private static final ArrayList<Runnable> runLater = new ArrayList<>();

    public void respondLater(IPayloadContext ctx, Packet packet) {
        ctx.enqueueWork(() -> runLater.add(() -> {
            if (checkClient(ctx))
                WDNetworkRegistry.sendToServer(packet);
            else
                ctx.reply(packet);
        }));
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (!runLater.isEmpty()) {
            if (DistSafety.isConnected()) {
                for (Runnable runnable : runLater) runnable.run();
                runLater.clear();
            }
        }
    }

    static {
        NeoForge.EVENT_BUS.addListener(Packet::onTick);
    }

    public static <T extends Packet> StreamCodec<FriendlyByteBuf, T> streamCodec(Function<FriendlyByteBuf, T> decoder) {
        return StreamCodec.of(
            (buf, pkt) -> pkt.write(buf),
            decoder::apply
        );
    }
}
