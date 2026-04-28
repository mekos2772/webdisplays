package net.montoyo.wd.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.montoyo.wd.client.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class OverlayMixin {

    @Inject(at = @At("HEAD"), method = "renderCrosshair", cancellable = true)
    public void preDrawCrosshair(GuiGraphics pGuiGraphics, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientProxy.renderCrosshair(mc.options, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0, pGuiGraphics, ci);
    }
}
