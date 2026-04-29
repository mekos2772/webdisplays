# WebDisplays 1.21.1 — Port Status & Remaining Issue

## Overview

Port of [CinemaMod/webdisplays](https://github.com/CinemaMod/webdisplays) (Forge 1.20) to NeoForge 1.21.1.  
Repository: https://github.com/mekos2772/webdisplays  
Branch: `main`

## Completed Fixes (18 issues resolved)

| # | Problem | File | Fix |
|---|---------|------|-----|
| 1 | Startup crash: GuiSetURL2 registered on event bus with no @SubscribeEvent | `GuiSetURL2.java` | Removed `@EventBusSubscriber(Dist.CLIENT)` + unused imports |
| 2 | WDScreen.keyPressed regression: non-keyboard screens delegate to new GuiServer | `WDScreen.java` | Changed fallthrough to `super.keyPressed(...)` |
| 3 | URL validation crashes client with RuntimeException | `GuiSetURL2.java` | Catch IOException, show chat message, return early |
| 4 | Advancement triggers completely disconnected | `Criterion.java` | Rewrote as `SimpleCriterionTrigger<Instance>` with CODEC |
| 5 | registerTrigger() was empty stub | `WebDisplays.java` | Wired DeferredRegister<CriterionTrigger> |
| 6 | 4 trigger call sites use old PlayerAdvancements API | 4 files | Changed to `trigger(ServerPlayer)` |
| 7 | MCEF crash: createBrowser called before CEF initialized | `ScreenBlockEntity.java` | Added `MCEF.isInitialized()` guard in handleUpdateTag |
| 8 | Same MCEF crash path | `ScreenData.java` | Added `MCEF.isInitialized()` guard in createBrowser |
| 9 | ScreenRenderer null browser → NPE | `ScreenRenderer.java` | Added null check after createBrowser |
| 10 | kb_left/kb_right blockstates use legacy forge_marker:1 format | 2 JSON files | Converted to vanilla 1.21.1 blockstate format |
| 11 | kb_right FACING property mismatch → crash on place | `KeyboardBlockRight.java` | Shared `KeyboardBlockLeft.FACING` property instance |
| 12 | OverlayMixin: Gui.screenWidth/screenHeight removed in 1.21.1 | `OverlayMixin.java` | Use `mc.getWindow().getGuiScaledWidth/Height()` |
| 13 | OverlayMixin: renderCrosshair signature missing DeltaTracker param | `OverlayMixin.java` | Added `DeltaTracker` parameter |
| 14 | Mixin config not loading → laser pointer never worked | `neoforge.mods.toml` | Added `[[mixins]] config = "webdisplays.mixins.json"` |
| 15 | Old Forge 1.20 mods.toml conflicts with NeoForge version | `mods.toml` | Deleted old file |
| 16 | ScreenModelLoader: use explicit block atlas Material | `ScreenModelLoader.java` | `new Material(TextureAtlas.LOCATION_BLOCKS, ...)` |
| 17 | MinePadRenderer GL texture leak (arm skin + browser quad) | `MinePadRenderer.java` | Save/restore with `setShaderTexture(0,0)` reset |
| 18 | ScreenRenderer GL texture leak (browser quad) | `ScreenRenderer.java` | Rebind block atlas via `_setShaderTexture(0, atlasId)` |

## UNRESOLVED: Screen Block Texture Bug

### Symptom

Placed screen blocks show the full item/GUI texture atlas on their surface ("把包含所有物品图标的底层纹理图集结贴在了方块表面"). The browser/web rendering works correctly — the bezel/frame model shows wrong textures.

### Diagnostic Results (Confirmed by log)

1. **Sprites in correct atlas**: All 16 screen textures (`screen0` through `screen15`) are baked into `minecraft:textures/atlas/blocks.png` ✓
2. **UV coordinates correct**: Each sprite has small sub-range UV bounds, e.g., screen0 = `(0.40625, 0.78125)-(0.421875, 0.8125)` — proper atlas regions, NOT full atlas ✓
3. **No model errors**: No warnings/errors about screen model loading, blockstate parsing, or geometry baking ✓
4. **Original 1.20 code comparison**: The original CinemaMod ScreenRenderer has the same pattern (bind browser texture, draw, disable depth test, no texture restore). It works in Forge 1.20.

### Attempted Fixes That Did NOT Work

- `RenderSystem.setShaderTexture(0, 0)` — resetting state tracker (same pattern used in original `Control.java:173`)
- `RenderSystem._setShaderTexture(0, prevTex)` — GL11 save/restore
- `RenderSystem.setShaderTexture(0, atlasTexture.getId())` — explicit atlas rebind
- `RenderSystem._setShaderTexture(0, atlasId)` — bypass state tracker with raw atlas GL ID
- Removing all texture management entirely (matching original 1.20 code exactly)
- Explicit `Material(TextureAtlas.LOCATION_BLOCKS, ...)` creation in ScreenModelLoader

### Possible Root Causes (Untested)

1. **NeoForge 1.21.1 rendering pipeline change**: The block entity render pass might be ordered differently within the terrain pass, causing the ScreenRenderer's GL state changes to affect block models in the same or subsequent chunks.

2. **BufferUploader.drawWithShader vs tesselator.end()**: The 1.20 original uses `tesselator.end()` (deprecated Forge API). The 1.21.1 version uses `BufferUploader.drawWithShader()`. These might handle GL state differently.

3. **RenderType mismatch**: The ScreenBaker's `getQuads` doesn't filter by `RenderType`. In 1.21.1, quads might be rendered in an unexpected pass that uses a different atlas.

4. **ModelData/ModelProperty not serialized correctly**: The `IntegerModelProperty` (inner class of ScreenBaker) might not properly propagate model data in 1.21.1, causing all faces to fall back to texs[15] which — if somehow from the wrong atlas — could look like the item atlas.

### Key Files

```
src/main/java/net/montoyo/wd/client/renderers/ScreenRenderer.java    # BER — browser surface
src/main/java/net/montoyo/wd/client/renderers/ScreenBaker.java       # BakedModel — bezel frame
src/main/java/net/montoyo/wd/client/renderers/ScreenModelLoader.java # Custom geometry loader
src/main/resources/assets/webdisplays/models/block/screen.json       # Block model JSON
src/main/resources/assets/webdisplays/blockstates/screen.json        # Blockstate
```

### Build

```bash
./gradlew build --no-daemon
# Output: build/libs/webdisplays-2.0.0-1.21.1.jar
```

### Dependencies

- NeoForge 21.1+
- MCEF 2.1.6+ (mcef-neoforge jar in mods folder)
