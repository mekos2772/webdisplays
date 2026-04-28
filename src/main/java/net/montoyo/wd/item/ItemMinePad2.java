/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.CraftComponent;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class ItemMinePad2 extends Item implements WDItem {
    public ItemMinePad2(Properties properties) {
        super(properties
                        .stacksTo(1)
//				.tab(WebDisplays.CREATIVE_TAB)
        );
    }

    private static String getURL(ItemStack is) {
        if (!is.has(DataComponents.CUSTOM_DATA) || !is.get(DataComponents.CUSTOM_DATA).copyTag().contains("PadURL"))
            return CommonConfig.Browser.homepage;
        else
            return is.get(DataComponents.CUSTOM_DATA).copyTag().getString("PadURL");
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(Level world, Player ply, @Nonnull InteractionHand hand) {
        ItemStack is = ply.getItemInHand(hand);
        boolean ok;

        if (ply.isShiftKeyDown()) {
            if (world.isClientSide)
                WebDisplays.PROXY.displaySetPadURLGui(is, getURL(is));

            ok = true;
        } else if (is.has(DataComponents.CUSTOM_DATA) && is.get(DataComponents.CUSTOM_DATA).copyTag().contains("PadID")) {
            if (world.isClientSide)
                WebDisplays.PROXY.openMinePadGui(is.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("PadID"));

            ok = true;
        } else {
            UUID uuid = UUID.randomUUID();
            String url = getURL(is);
            WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(uuid, url));
            CustomData.update(DataComponents.CUSTOM_DATA, is, tag -> tag.putUUID("PadID", uuid));

            ok = true;
        }

        return new InteractionResultHolder<>(ok ? InteractionResult.SUCCESS : InteractionResult.PASS, is);
    }


    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity ent) {
        if (ent.onGround() && !ent.level().isClientSide) {
            CompoundTag tag = ent.getItem().has(DataComponents.CUSTOM_DATA) ? ent.getItem().get(DataComponents.CUSTOM_DATA).copyTag() : null;

            if (tag != null && tag.contains("ThrowHeight")) {
                //Delete it, it touched the ground
                double height = tag.getDouble("ThrowHeight");
                UUID thrower = null;

                if (tag.contains("ThrowerMSB") && tag.contains("ThrowerLSB"))
                    thrower = new UUID(tag.getLong("ThrowerMSB"), tag.getLong("ThrowerLSB"));

                if (tag.contains("PadID") || tag.contains("PadURL")) {
                    tag.remove("ThrowerMSB");
                    tag.remove("ThrowerLSB");
                    tag.remove("ThrowHeight");
                    ent.getItem().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                } else //We can delete the whole tag
                    ent.getItem().remove(DataComponents.CUSTOM_DATA);

                if (thrower != null && height - ent.getBlockY() >= 20.0) {
                    ent.level().playSound(null, ent.getBlockX(), ent.getBlockY(), ent.getBlockZ(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 4.0f, 1.0f);
                    ent.level().addFreshEntity(new ItemEntity(ent.level(), ent.getBlockX(), ent.getBlockY(), ent.getBlockZ(), CraftComponent.EXTCARD.makeItemStack()));
                    ent.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);

                    Player ply = ent.level().getPlayerByUUID(thrower);
                    if (ply != null && ply instanceof ServerPlayer)
                        WebDisplays.INSTANCE.criterionPadBreak.trigger((ServerPlayer) ply);
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
