/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import org.jetbrains.annotations.NotNull;

import static com.mojang.math.Axis.*;

public class ScreenRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
	public ScreenRenderer() {
	}

	public static class ScreenRendererProvider implements BlockEntityRendererProvider<ScreenBlockEntity> {
		@Override
		public @NotNull BlockEntityRenderer<ScreenBlockEntity> create(@NotNull Context arg) {
			return new ScreenRenderer();
		}
	}

	private final Vector3f mid = new Vector3f();
	private final Vector3i tmpi = new Vector3i();
	private final Vector3f tmpf = new Vector3f();

	@Override
	public void render(ScreenBlockEntity te, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!te.isLoaded())
			return;

		RenderSystem.disableBlend();

		for (int i = 0; i < te.screenCount(); i++) {
			ScreenData scr = te.getScreen(i);
			if (scr.browser == null) {
				double dist = WebDisplays.PROXY.distanceTo(te, Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition());
				if (dist <= WebDisplays.INSTANCE.loadDistance2 * 16)
					scr.createBrowser(te, true);
				if (scr.browser == null) continue;
			}

			tmpi.set(scr.side.right);
			tmpi.mul(scr.size.x);
			tmpi.addMul(scr.side.up, scr.size.y);
			tmpf.set(tmpi);
			mid.set(0.5, 0.5, 0.5);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.left);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.down);
			mid.addMul(tmpf, 0.5f);

			poseStack.pushPose();
			poseStack.translate(mid.x, mid.y, mid.z);

			switch (scr.side) {
				case BOTTOM:
					poseStack.mulPose(XP.rotation(90.f + 49.8f));
					break;

				case TOP:
					poseStack.mulPose(XN.rotation(90.f + 49.8f));
					break;

				case NORTH:
					poseStack.mulPose(YN.rotationDegrees(180.f));
					break;

				case SOUTH:
					break;

				case WEST:
					poseStack.mulPose(YN.rotationDegrees(90.f));
					break;

				case EAST:
					poseStack.mulPose(YP.rotationDegrees(90.f));
					break;
			}

			if (scr.doTurnOnAnim) {
				long lt = System.currentTimeMillis() - scr.turnOnTime;
				float ft = ((float) lt) / 100.0f;

				if (ft >= 1.0f) {
					ft = 1.0f;
					scr.doTurnOnAnim = false;
				}

				poseStack.scale(ft, ft, 1.0f);
			}

			if (!scr.rotation.isNull)
				poseStack.mulPose(ZP.rotationDegrees(scr.rotation.angle));

			float sw = ((float) scr.size.x) * 0.5f - 2.f / 16.f;
			float sh = ((float) scr.size.y) * 0.5f - 2.f / 16.f;

			if (scr.rotation.isVertical) {
				float tmp = sw;
				sw = sh;
				sh = tmp;
			}

			Tesselator tesselator = Tesselator.getInstance();
			RenderSystem.enableDepthTest();
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, ((MCEFBrowser) scr.browser).getRenderer().getTextureID());
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			builder.addVertex(poseStack.last().pose(), -sw, -sh, 0.505f).setUv(0.f, 1.f).setColor(255, 255, 255, 255);
			builder.addVertex(poseStack.last().pose(), sw, -sh, 0.505f).setUv(1.f, 1.f).setColor(255, 255, 255, 255);
			builder.addVertex(poseStack.last().pose(), sw, sh, 0.505f).setUv(1.f, 0.f).setColor(255, 255, 255, 255);
			builder.addVertex(poseStack.last().pose(), -sw, sh, 0.505f).setUv(0.f, 0.f).setColor(255, 255, 255, 255);
			BufferUploader.drawWithShader(builder.buildOrThrow());
			RenderSystem.setShaderTexture(0, 0);
			RenderSystem.disableDepthTest();

			poseStack.popPose();
		}
	}
}
