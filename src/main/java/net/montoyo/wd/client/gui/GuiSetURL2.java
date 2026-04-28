/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.TextField;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;
import net.montoyo.wd.net.server_bound.C2SMessageScreenCtrl;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.math.Vector3i;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class GuiSetURL2 extends WDScreen {
	
	//Screen data
	private ScreenBlockEntity tileEntity;
	private BlockSide screenSide;
	private Vector3i remoteLocation;
	
	//Pad data
	private ItemStack stack;
	private final boolean isPad;
	
	//Common
	private final String screenURL;
	
	@FillControl
	private TextField tfURL;
	
	@FillControl
	private Button btnShutDown;
	
	@FillControl
	private Button btnCancel;
	
	@FillControl
	private Button btnOk;
	
	public GuiSetURL2(ScreenBlockEntity tes, BlockSide side, String url, Vector3i rl) {
		super(Component.nullToEmpty(null));
		tileEntity = tes;
		screenSide = side;
		remoteLocation = rl;
		isPad = false;
		screenURL = url;
	}
	
	public GuiSetURL2(ItemStack is, String url) {
		super(Component.nullToEmpty(null));
		isPad = true;
		stack = is;
		screenURL = url;
	}
	
	@Override
	public void init() {
		super.init();
		loadFrom(ResourceLocation.fromNamespaceAndPath("webdisplays", "gui/seturl.json"));
		tfURL.setText(screenURL);
	}
	
	@Override
	protected void addLoadCustomVariables(Map<String, Double> vars) {
		vars.put("isPad", isPad ? 1.0 : 0.0);
	}
	
	protected UUID getUUID() {
		if (stack == null || !(stack.getItem() instanceof ItemMinePad2))
			throw new RuntimeException("Get UUID is being called for a non-minepad UI");
		if (!stack.has(DataComponents.CUSTOM_DATA) || !stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("PadID"))
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID("PadID", UUID.randomUUID()));
		
		return stack.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("PadID");
	}
	
	@GuiSubscribe
	public void onButtonClicked(Button.ClickEvent ev) {
		if (ev.getSource() == btnCancel)
			minecraft.setScreen(null);
		else if (ev.getSource() == btnOk)
			validate(tfURL.getText());
		else if (ev.getSource() == btnShutDown) {
			if (isPad) {
				WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(
						getUUID(),
						""
				));
				if (stack.has(DataComponents.CUSTOM_DATA)) {
					var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
					tag.remove("PadID");
					if (tag.isEmpty())
						stack.remove(DataComponents.CUSTOM_DATA);
					else
						stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				}
			}
			
			minecraft.setScreen(null);
		}
	}
	
	@GuiSubscribe
	public void onEnterPressed(TextField.EnterPressedEvent ev) {
		validate(ev.getText());
	}
	
	private void validate(String url) {
		if (!url.isEmpty()) {
			try {
				ScreenBlockEntity.url(url);
			} catch (IOException e) {
				Log.warningEx("Invalid URL entered in GuiSetURL2: %s", e, url);
				if (minecraft != null && minecraft.player != null) {
					minecraft.player.displayClientMessage(Component.literal("Invalid URL"), true);
				}
				return;
			}

			url = Util.addProtocol(url);
//			url = ((ClientProxy) WebDisplays.PROXY).getMCEF().punycode(url);
			final String finalUrl = url;

			if (isPad) {
				UUID uuid = getUUID();
				WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(uuid, finalUrl));
				CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("PadURL", finalUrl));

				ClientProxy.PadData pd = ((ClientProxy) WebDisplays.PROXY).getPadByID(uuid);

				if (pd != null && pd.view != null) {
					pd.view.loadURL(WebDisplays.applyBlacklist(finalUrl));
				}
			} else
				WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.setURL(tileEntity, screenSide, finalUrl, remoteLocation));
		}

		minecraft.setScreen(null);
	}
	
	@Override
	public boolean isForBlock(BlockPos bp, BlockSide side) {
		return (remoteLocation != null && remoteLocation.equalsBlockPos(bp)) || (bp.equals(tileEntity.getBlockPos()) && side == screenSide);
	}
	
}
