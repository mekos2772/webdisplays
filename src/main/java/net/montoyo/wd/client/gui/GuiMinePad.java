/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.cinemamod.mcef.MCEFBrowser;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.Scripts;
import net.montoyo.wd.utilities.data.BlockSide;
import org.cef.browser.CefBrowser;
import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import static net.neoforged.api.distmarker.Dist.CLIENT;

@OnlyIn(CLIENT)
public class GuiMinePad extends WDScreen {
	private static final int NAV_X_OFFSET = 24;
	private static final int NAV_Y_OFFSET = 24;
	private static final int NAV_BUTTON_W = 18;
	private static final int NAV_BUTTON_H = 14;
	private static final int NAV_GAP = 3;

	private ClientProxy.PadData pad;
	private double vx;
	private double vy;
	private double vw;
	private double vh;
	private boolean navExpanded = false;
	private boolean suppressNextRelease = false;

	public GuiMinePad() {
		super(Component.nullToEmpty(null));
	}

	public GuiMinePad(ClientProxy.PadData pad) {
		this();
		this.pad = pad;
	}

	int trueWidth, trueHeight;

	@Override
	public void init() {
		vw = ((double) width) - 32.0f;
		vh = vw / WebDisplays.PAD_RATIO;
		vx = 16.0f;
		vy = (((double) height) - vh) / 2.0f;

		trueWidth = width;
		trueHeight = height;

		this.width = (int) vw;
		this.height = (int) vh;

		super.init();

		((MCEFBrowser) pad.view).setCursor(CefCursorType.fromId(pad.activeCursor));
		((MCEFBrowser) pad.view).setCursorChangeListener((id) -> {
			pad.activeCursor = id;
			((MCEFBrowser) pad.view).setCursor(CefCursorType.fromId(id));
		});
	}

	private static void addRect(BufferBuilder bb, double x, double y, double w, double h) {
		bb.addVertex((float) x, (float) y, 0.0f).setColor(255, 255, 255, 255);
		bb.addVertex((float) (x + w), (float) y, 0.0f).setColor(255, 255, 255, 255);
		bb.addVertex((float) (x + w), (float) (y + h), 0.0f).setColor(255, 255, 255, 255);
		bb.addVertex((float) x, (float) (y + h), 0.0f).setColor(255, 255, 255, 255);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float ptt) {
		width = trueWidth;
		height = trueHeight;
		renderBackground(graphics, mouseX, mouseY, ptt);
		width = (int) vw;
		height = (int) vh;

		RenderSystem.disableCull();
		RenderSystem.setShaderColor(0.73f, 0.73f, 0.73f, 1.0f);

		Tesselator t = Tesselator.getInstance();
		BufferBuilder bb = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		addRect(bb, vx, vy - 16, vw, 16);
		addRect(bb, vx, vy + vh, vw, 16);
		addRect(bb, vx - 16, vy, 16, vh);
		addRect(bb, vx + vw, vy, 16, vh);
		var meshData = bb.buildOrThrow();
		BufferUploader.drawWithShader(meshData);

		if (pad.view != null) {
//            pad.view.draw(poseStack, vx, vy + vh, vx + vw, vy);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.disableDepthTest();
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, ((MCEFBrowser) pad.view).getRenderer().getTextureID());
			t = Tesselator.getInstance();
			BufferBuilder buffer = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			double x1 = vx;
			double y1 = vy;
			double x2 = vx + vw;
			double y2 = vy + vh;
			buffer.addVertex(graphics.pose().last().pose(), (float) x1, (float) y1, 0.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
			buffer.addVertex(graphics.pose().last().pose(), (float) x2, (float) y1, 0.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);
			buffer.addVertex(graphics.pose().last().pose(), (float) x2, (float) y2, 0.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
			buffer.addVertex(graphics.pose().last().pose(), (float) x1, (float) y2, 0.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
			meshData = buffer.buildOrThrow();
			BufferUploader.drawWithShader(meshData);
			RenderSystem.enableDepthTest();
		}

		RenderSystem.enableCull();

		graphics.drawString(
				minecraft.font, Language.getInstance().getOrDefault(
						"webdisplays.gui.minepad.close"
				), (int) vx + 4, (int) vy - minecraft.font.lineHeight - 3, 16777215, true
		);
		drawNavigationOverlay(graphics, mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return this.keyChanged(keyCode, scanCode, modifiers, true) || super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return this.keyChanged(keyCode, scanCode, modifiers, false) || super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (pad.view != null) {
			((MCEFBrowser) pad.view).sendKeyTyped(codePoint, modifiers);
			return true;
		} else {
			return super.charTyped(codePoint, modifiers);
		}
	}

	/* copied from MCEF */
	public boolean keyChanged(int keyCode, int scanCode, int modifiers, boolean pressed) {
		assert minecraft != null;
		if ((modifiers & GLFW.GLFW_MOD_SHIFT) == GLFW.GLFW_MOD_SHIFT && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (pressed && handleNavigationShortcut(keyCode, modifiers))
			return true;

		InputConstants.Key iuKey = InputConstants.getKey(keyCode, scanCode);
		String keystr = iuKey.getDisplayName().getString();
//        System.out.println("KEY STR " + keystr);
		if (keystr.length() == 0)
			return false;

		char key = keystr.charAt(keystr.length() - 1);

		if (keystr.equals("Enter")) {
			keyCode = 10;
			key = '\n';
		}

		if (pad.view != null) {
			if (pressed)
				((MCEFBrowser) pad.view).sendKeyPress(keyCode, scanCode, modifiers);
			else
				((MCEFBrowser) pad.view).sendKeyRelease(keyCode, scanCode, modifiers);

			if (pressed && key == '\n')
				if (modifiers != 0) ((MCEFBrowser) pad.view).sendKeyTyped('\r', modifiers);
			return true;
		}

		return false;
	}

	private boolean handleNavigationShortcut(int keyCode, int modifiers) {
		if (pad == null || pad.view == null)
			return false;

		boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
		boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
		boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

		if (keyCode == GLFW.GLFW_KEY_F5 || (ctrl && keyCode == GLFW.GLFW_KEY_R)) {
			if (ctrl && shift)
				pad.view.reloadIgnoreCache();
			else
				pad.view.reload();
			return true;
		}

		if (alt && keyCode == GLFW.GLFW_KEY_LEFT) {
			if (pad.view.canGoBack())
				pad.view.goBack();
			return true;
		}

		if (alt && keyCode == GLFW.GLFW_KEY_RIGHT) {
			if (pad.view.canGoForward())
				pad.view.goForward();
			return true;
		}

		return false;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		mouse(-1, false, (int) mouseX, (int) mouseY, 0);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleNavigationClick(mouseX, mouseY, button))
			return true;

		mouse(button, true, (int) mouseX, (int) mouseY, 0);
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (suppressNextRelease) {
			suppressNextRelease = false;
			return true;
		}

		mouse(button, false, (int) mouseX, (int) mouseY, 0);
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double mx = (mouseX - vx) / vw;
		double my = (mouseY - vy) / vh;
		int sx = (int) (mx * WebDisplays.INSTANCE.padResX);
		int sy = (int) (my * WebDisplays.INSTANCE.padResY);
		// TODO: this doesn't work, and I don't understand why?
		((MCEFBrowser) pad.view).sendMouseWheel(sx, sy, scrollY, (hasControlDown() && !hasAltDown() && !hasShiftDown()) ? GLFW.GLFW_MOD_CONTROL : 0);

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	public void capturedMouse(double scaledX, double scaledY, int sx, int sy) {
		double centerX = (int) (0.5 * (double) this.minecraft.getWindow().getGuiScaledWidth());
		double centerY = (int) (0.5 * (double) this.minecraft.getWindow().getGuiScaledHeight());

		if (sx == (int) centerX && sy == (int) centerY) return;

		double mx = (centerX - vx) / vw;
		double my = (centerY - vy) / vh;
		double scaledCentX = (mx * WebDisplays.INSTANCE.padResX);
		double scaledCentY = (my * WebDisplays.INSTANCE.padResY);

		double deltX = scaledX - scaledCentX;
		double deltY = scaledY - scaledCentY;

		String scr = Scripts.MOUSE_EVENT;
		pad.view.executeJavaScript(
				scr
						.replace("%xCoord%", "" + (int) centerX)
						.replace("%yCoord%", "" + (int) centerY)
						.replace("%xDelta%", "" + (deltX))
						.replace("%yDelta%", "" + (deltY)),
				"WebDisplays", 0
		);

		// lock mouse
		try {
			double xpos = (this.minecraft.getWindow().getScreenWidth() / 2);
			double ypos = (this.minecraft.getWindow().getScreenHeight() / 2);
			GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), xpos, ypos);
		} catch (Throwable ignored) {
		}
	}

	public void mouse(int btn, boolean pressed, int sx, int sy, double scrollAmount) {
		double mx = (sx - vx) / vw;
		double my = (sy - vy) / vh;

		if (pad.view != null && mx >= 0 && mx <= 1) {
			//Scale again according to the webview
			int scaledX = (int) (mx * WebDisplays.INSTANCE.padResX);
			int scaledY = (int) (my * WebDisplays.INSTANCE.padResY);

			if (btn == -1) {
				if (locked)
					capturedMouse(mx * WebDisplays.INSTANCE.padResX, my * WebDisplays.INSTANCE.padResY, sx, sy);
				else ((MCEFBrowser) pad.view).sendMouseMove(scaledX, scaledY);
			} else if (pressed)
				((MCEFBrowser) pad.view).sendMousePress(scaledX, scaledY, btn);
			else ((MCEFBrowser) pad.view).sendMouseRelease(scaledX, scaledY, btn);
			pad.view.setFocus(true);
		}
	}

	public static Optional<Character> getChar(int keyCode, int scanCode) {
		String keystr = GLFW.glfwGetKeyName(keyCode, scanCode);
		if (keystr == null) {
			keystr = "\0";
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			keystr = "\n";
		}
		if (keystr.length() == 0) {
			return Optional.empty();
		}

		return Optional.of(keystr.charAt(keystr.length() - 1));
	}

	@Override
	public void tick() {
		if (pad.view == null)
			minecraft.setScreen(null); //In case the user dies with the pad in the hand
		pollElement();
	}

	@Override
	public boolean isForBlock(BlockPos bp, BlockSide side) {
		return false;
	}

	@Override
	public void removed() {
		super.removed();
		InputConstants.updateRawMouseInput(
				minecraft.getWindow().getWindow(),
				Minecraft.getInstance().options.rawMouseInput().get()
		);
		if (pad.view instanceof MCEFBrowser browser) {
			browser.setCursor(CefCursorType.POINTER);
			browser.setCursorChangeListener((cursor) -> {
				pad.activeCursor = cursor;
			});
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		removed();
		this.minecraft.popGuiLayer();
	}

	boolean locked = false;
	double lockCenterX = -1;
	double lockCenterY = -1;

	protected void updateCrd(JsonObject obj) {
		if (obj.getAsJsonPrimitive("exists").getAsBoolean()) {
			locked = true;
			RenderSystem.recordRenderCall(() -> {
				InputConstants.updateRawMouseInput(
						minecraft.getWindow().getWindow(),
						obj.getAsJsonPrimitive("unadjust").getAsBoolean()
				);
				GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), 208897, GLFW.GLFW_CURSOR_DISABLED);
			});
			lockCenterX = obj.getAsJsonPrimitive("x").getAsDouble() + obj.getAsJsonPrimitive("w").getAsDouble() / 2;
			lockCenterY = obj.getAsJsonPrimitive("y").getAsDouble() + obj.getAsJsonPrimitive("h").getAsDouble() / 2;
		} else {
			if (locked) {
				locked = false;
				RenderSystem.recordRenderCall(()->{
					InputConstants.updateRawMouseInput(
							minecraft.getWindow().getWindow(),
							Minecraft.getInstance().options.rawMouseInput().get()
					);
					GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), 208897, GLFW.GLFW_CURSOR_NORMAL);
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CefCursorType.fromId(pad.activeCursor).glfwId);
				});
			}
		}
	}

	protected void pollElement() {
		if (pad.view instanceof WDBrowser browser) {
			JsonObject object = browser.pointerLockElement().getObj();
			if (object != null) updateCrd(object);
		}
	}

	private CefBrowser navBrowser() {
		return (pad == null) ? null : pad.view;
	}

	private int navBaseX() {
		return (int) vx + NAV_X_OFFSET;
	}

	private int navBaseY() {
		return (int) vy + NAV_Y_OFFSET;
	}

	private int navButtonX(int idx) {
		return navBaseX() + (NAV_BUTTON_W + NAV_GAP) * (idx + 1);
	}

	private void drawNavigationOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		CefBrowser browser = navBrowser();
		if (browser == null)
			return;

		int x = navBaseX();
		int y = navBaseY();
		drawNavButton(graphics, x, y, NAV_BUTTON_W, NAV_BUTTON_H, navExpanded ? "X" : "=", true, mouseX, mouseY);

		if (!navExpanded)
			return;

		drawNavButton(graphics, navButtonX(0), y, NAV_BUTTON_W, NAV_BUTTON_H, "<", browser.canGoBack(), mouseX, mouseY);
		drawNavButton(graphics, navButtonX(1), y, NAV_BUTTON_W, NAV_BUTTON_H, ">", browser.canGoForward(), mouseX, mouseY);
		drawNavButton(graphics, navButtonX(2), y, NAV_BUTTON_W, NAV_BUTTON_H, "R", true, mouseX, mouseY);
	}

	private void drawNavButton(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean enabled, int mouseX, int mouseY) {
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
		if (button != 0)
			return false;

		CefBrowser browser = navBrowser();
		if (browser == null)
			return false;

		int mx = (int) mouseX;
		int my = (int) mouseY;
		int x = navBaseX();
		int y = navBaseY();

		if (inside(mx, my, x, y, NAV_BUTTON_W, NAV_BUTTON_H)) {
			navExpanded = !navExpanded;
			suppressNextRelease = true;
			return true;
		}

		if (!navExpanded)
			return false;

		if (inside(mx, my, navButtonX(0), y, NAV_BUTTON_W, NAV_BUTTON_H)) {
			if (browser.canGoBack())
				browser.goBack();
			suppressNextRelease = true;
			return true;
		}

		if (inside(mx, my, navButtonX(1), y, NAV_BUTTON_W, NAV_BUTTON_H)) {
			if (browser.canGoForward())
				browser.goForward();
			suppressNextRelease = true;
			return true;
		}

		if (inside(mx, my, navButtonX(2), y, NAV_BUTTON_W, NAV_BUTTON_H)) {
			browser.reload();
			suppressNextRelease = true;
			return true;
		}

		return false;
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}
}
