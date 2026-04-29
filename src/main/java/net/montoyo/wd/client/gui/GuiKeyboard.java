/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.camera.KeyboardCamera;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.Control;
import net.montoyo.wd.client.gui.controls.Label;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.controls.builtin.ClickControl;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageScreenCtrl;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.serialization.TypeData;
import net.montoyo.wd.utilities.serialization.Util;
import org.cef.browser.CefBrowser;
import org.cef.misc.CefCursorType;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class GuiKeyboard extends WDScreen {

    private static final String WARNING_FNAME = "wd_keyboard_warning.txt";
    private static final int NAV_X = 8;
    private static final int NAV_Y = 8;
    private static final int NAV_BUTTON_W = 18;
    private static final int NAV_BUTTON_H = 14;
    private static final int NAV_GAP = 3;

    private ScreenBlockEntity tes;
    private BlockSide side;
    private ScreenData data;
    private final ArrayList<TypeData> evStack = new ArrayList<>();
    private BlockPos kbPos;
    private boolean showWarning = true;
    private boolean navExpanded = false;
    private boolean suppressNextRelease = false;

    @FillControl
    private Label lblInfo;

    @FillControl
    private Button btnOk;

    public GuiKeyboard() {
        super(Component.nullToEmpty(null));
    }

    public GuiKeyboard(ScreenBlockEntity tes, BlockSide side, BlockPos kbPos) {
        this();
        this.tes = tes;
        this.side = side;
        this.kbPos = kbPos;
    }

    @Override
    protected void addLoadCustomVariables(Map<String, Double> vars) {
        vars.put("showWarning", showWarning ? 1.0 : 0.0);
    }

    private static final boolean vivecraftPresent;

    static {
        boolean vivePres = false;
        if (ModList.get().isLoaded("vivecraft")) vivePres = true;
        vivecraftPresent = vivePres;
    }

    @Override
    public void init() {
        super.init();

        if (minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished())
            showWarning = false; //NO NEED
        else
            showWarning = !hasUserReadWarning();

        loadFrom(ResourceLocation.fromNamespaceAndPath("webdisplays", "gui/kb_right.json"));

        if (showWarning) {
            int maxLabelW = 0;
            int totalH = 0;

            for (Control ctrl : controls) {
                if (ctrl != lblInfo && ctrl instanceof Label) {
                    if (ctrl.getWidth() > maxLabelW)
                        maxLabelW = ctrl.getWidth();

                    totalH += ctrl.getHeight();
                    ctrl.setPos((width - ctrl.getWidth()) / 2, 0);
                }
            }

            btnOk.setWidth(maxLabelW);
            btnOk.setPos((width - maxLabelW) / 2, 0);
            totalH += btnOk.getHeight();

            int y = (height - totalH) / 2;
            for (Control ctrl : controls) {
                if (ctrl != lblInfo) {
                    ctrl.setPos(ctrl.getX(), y);
                    y += ctrl.getHeight();
                }
            }
        } else {
            if (!minecraft.isWindowActive()) {
                minecraft.setWindowActive(true);
                minecraft.mouseHandler.grabMouse();
            }
        }

        defaultBackground = showWarning;
        syncTicks = 5;

        if (vivecraftPresent)

        KeyboardCamera.focus(tes, side);

        data = tes.getScreen(side);
        CefBrowser browser = data.browser;
        ((MCEFBrowser) browser).setCursor(CefCursorType.fromId(data.mouseType));
        ((MCEFBrowser) browser).setCursorChangeListener((id) -> {
            data.mouseType = id;
            ((MCEFBrowser) browser).setCursor(CefCursorType.fromId(id));
        });
    }

    @Override
    public void removed() {
        super.removed();
        if (vivecraftPresent)
        KeyboardCamera.focus(null, null);
        CefBrowser browser = data.browser;
        if (browser instanceof MCEFBrowser mcef) {
            mcef.setCursor(CefCursorType.POINTER);
            mcef.setCursorChangeListener((cursor) -> data.mouseType = cursor);
        }
    }

    @Override
    public void onClose() {
        removed();
        super.onClose();
        this.minecraft.popGuiLayer();
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!showWarning)
            drawNavigationOverlay(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (quitOnEscape && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (handleNavigationShortcut(keyCode, modifiers))
            return true;

        addKey(new TypeData(TypeData.Action.PRESS, keyCode, modifiers, scanCode));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        addKey(new TypeData(TypeData.Action.TYPE, codePoint, modifiers, 0));
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        addKey(new TypeData(TypeData.Action.RELEASE, keyCode, modifiers, scanCode));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleNavigationShortcut(int keyCode, int modifiers) {
        if (data == null || data.browser == null)
            return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        if (keyCode == GLFW.GLFW_KEY_F5 || (ctrl && keyCode == GLFW.GLFW_KEY_R)) {
            if (ctrl && shift)
                data.browser.reloadIgnoreCache();
            else
                data.browser.reload();
            return true;
        }

        if (alt && keyCode == GLFW.GLFW_KEY_LEFT) {
            if (data.browser.canGoBack())
                data.browser.goBack();
            return true;
        }

        if (alt && keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (data.browser.canGoForward())
                data.browser.goForward();
            return true;
        }

        return false;
    }

    void addKey(TypeData data) {
        tes.type(side, "[" + WebDisplays.GSON.toJson(data) + "]", kbPos);

        evStack.add(data);
        if (!evStack.isEmpty() && !syncRequested())
            requestSync();
    }

    @Override
    protected void sync() {
        if(!evStack.isEmpty()) {
            WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.type(tes, side, WebDisplays.GSON.toJson(evStack), kbPos));
            evStack.clear();
        }
    }

    @GuiSubscribe
    public void onClick(Button.ClickEvent ev) {
        if(showWarning && ev.getSource() == btnOk) {
            writeUserAcknowledge();

            for(Control ctrl: controls) {
                if(ctrl instanceof Label) {
                    Label lbl = (Label) ctrl;
                    lbl.setVisible(!lbl.isVisible());
                }
            }

            btnOk.setDisabled(true);
            btnOk.setVisible(false);
            showWarning = false;
            defaultBackground = false;
            minecraft.setWindowActive(true);
            minecraft.mouseHandler.grabMouse();
        }
    }

    private boolean hasUserReadWarning() {
        try {
            File f = new File(FMLPaths.GAMEDIR.get().toString(), WARNING_FNAME);

            if(f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String str = br.readLine();
                Util.silentClose(br);

                return str != null && str.trim().equalsIgnoreCase("read");
            }
        } catch(Throwable t) {
            Log.warningEx("Can't know if user has already read the warning", t);
        }

        return false;
    }

    private void writeUserAcknowledge() {
        try {
            File f = new File(FMLPaths.GAMEDIR.get().toString(), WARNING_FNAME);

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("read\n");
            Util.silentClose(bw);
        } catch(Throwable t) {
            Log.warningEx("Can't write that the user read the warning", t);
        }
    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return bp.equals(kbPos) || (bp.equals(tes.getBlockPos()) && side == this.side);
    }

    protected void mouse(double mouseX, double mouseY, Consumer<Vector2i> func) {
        float pct = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        double fov = Minecraft.getInstance().options.fov().get();

        mouseX /= width;
        mouseY /= height;

        mouseX -= 0.5;
        mouseY -= 0.5;
        mouseY = -mouseY;

        Matrix4f proj = Minecraft.getInstance().gameRenderer.getProjectionMatrix(fov);

        Entity e = Minecraft.getInstance().getEntityRenderDispatcher().camera.getEntity();

        PoseStack camera = new PoseStack();
        float[] angle = KeyboardCamera.getAngle(e, pct);
        camera.mulPose(Axis.XP.rotationDegrees(angle[0]));
        camera.mulPose(Axis.YP.rotationDegrees(angle[1] + 180.0F));

        Vector4f coord = new Vector4f(2f * (float) mouseX, 2 * (float) mouseY, 0, 1f);
        coord.add(proj.invert().transform(coord));
        coord = camera.last().pose().invert().transform(coord);

        Vec3 vec3 = e.getEyePosition(pct);
        Vec3 vec31 = new Vec3(coord.x, coord.y, coord.z).normalize();

        BlockHitResult result = tes.trace(side, vec3, vec31);
        if (result.getType() != HitResult.Type.MISS) {
            tes.interact(result, func);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserMove(tes, side, hit));
        });

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleNavigationClick(mouseX, mouseY, button))
            return true;

        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            tes.handleMouseEvent(side, ClickControl.ControlType.DOWN, hit, button);
            WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserDown(tes, side, hit, button));
        });

        KeyboardCamera.setMouse(button, true);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (suppressNextRelease) {
            suppressNextRelease = false;
            return true;
        }

        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            tes.handleMouseEvent(side, ClickControl.ControlType.UP, hit, button);
            WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserUp(tes, side, button));
        });

        KeyboardCamera.setMouse(button, false);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private CefBrowser navBrowser() {
        return data == null ? null : data.browser;
    }

    private int navButtonX(int idx) {
        return NAV_X + (NAV_BUTTON_W + NAV_GAP) * (idx + 1);
    }

    private void drawNavigationOverlay(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        CefBrowser browser = navBrowser();
        if (browser == null)
            return;

        drawNavButton(graphics, NAV_X, NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H, navExpanded ? "X" : "=", true, mouseX, mouseY);
        if (!navExpanded)
            return;

        drawNavButton(graphics, navButtonX(0), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H, "<", browser.canGoBack(), mouseX, mouseY);
        drawNavButton(graphics, navButtonX(1), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H, ">", browser.canGoForward(), mouseX, mouseY);
        drawNavButton(graphics, navButtonX(2), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H, "R", true, mouseX, mouseY);
    }

    private void drawNavButton(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, int w, int h, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        int color;
        if (!enabled)
            color = 0x66303030;
        else if (hover)
            color = 0xCC3F3F3F;
        else
            color = 0xAA202020;

        graphics.fill(x, y, x + w, y + h, color);
        graphics.drawCenteredString(minecraft.font, label, x + (w / 2), y + 3, enabled ? 0xFFFFFFFF : 0x88FFFFFF);
    }

    private boolean handleNavigationClick(double mouseX, double mouseY, int button) {
        if (showWarning || button != 0)
            return false;

        CefBrowser browser = navBrowser();
        if (browser == null)
            return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (inside(mx, my, NAV_X, NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H)) {
            navExpanded = !navExpanded;
            suppressNextRelease = true;
            return true;
        }

        if (!navExpanded)
            return false;

        if (inside(mx, my, navButtonX(0), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H)) {
            if (browser.canGoBack())
                browser.goBack();
            suppressNextRelease = true;
            return true;
        }

        if (inside(mx, my, navButtonX(1), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H)) {
            if (browser.canGoForward())
                browser.goForward();
            suppressNextRelease = true;
            return true;
        }

        if (inside(mx, my, navButtonX(2), NAV_Y, NAV_BUTTON_W, NAV_BUTTON_H)) {
            browser.reload();
            suppressNextRelease = true;
            return true;
        }

        return false;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void tick() {
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() / Minecraft.getInstance().getWindow().getWidth();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() / Minecraft.getInstance().getWindow().getHeight();

        mouse(mouseX * width, mouseY * height, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserMove(tes, side, hit));
        });

        super.tick();
    }
}
