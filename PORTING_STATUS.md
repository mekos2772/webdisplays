# WebDisplays → NeoForge 1.21.1 适配状态

## 总览

| 维度 | 数据 |
|------|------|
| 源版本 | 1.19.2 Forge |
| 目标版本 | 1.21.1 NeoForge |
| Java 文件总数 | 189 |
| 已编译通过 | ~170 (90%) |
| 仍有错误 | ~20 |
| MCEF 依赖 | 2.1.6-1.21.1 ✅ 已支持 |

## 已完成的适配

### 构建系统
- NeoGradle `net.neoforged.moddev` 2.0.140
- NeoForge 21.1.115 + Parchment mappings
- Java 21, Gradle 8.14.2

### 网络层（完全重写）
- `Packet.java` → `CustomPacketPayload` 接口
- `WDNetworkRegistry.java` → `PayloadRegistrar` + `RegisterPayloadHandlersEvent`
- `NetworkEvent.Context` → `IPayloadContext`
- `PacketDistributor.TargetPoint` → `sendToPlayersTrackingChunk` / `sendToPlayer`
- 全部 14 个网络包已适配

### 注册系统
- `ForgeRegistries` → `BuiltInRegistries`
- `RegistryObject` → `Supplier` / `DeferredHolder`
- Block/Item/BlockEntity/CreativeTab 注册全部更新

### Block/Item 层
- `BlockBehaviour.Properties.copy()` → `ofFullCopy()`
- `use()` → `useItemOn()` 返回 `ItemInteractionResult`
- 所有 Block 添加 `codec()` 方法
- `BaseEntityBlock` → `MapCodec` 支持

### BlockEntity 层
- `saveAdditional(CompoundTag)` → `saveAdditional(CompoundTag, HolderLookup.Provider)`
- `load(CompoundTag)` → `loadAdditional(CompoundTag, HolderLookup.Provider)`
- `getUpdateTag()` / `handleUpdateTag()` 签名更新
- `serializeNBT()` / `deserializeNBT()` 移除
- `invalidateCaps()` 移除

### 配置系统
- `ForgeConfigSpec` → `ModConfigSpec`
- `ModLoadingContext.get().registerConfig()` API 更新

### 能力/Capability 系统
- `ICapabilitySerializable` → `AttachmentType`
- `LazyOptional` → `Optional`
- `WDDCapability` 完全重写

### 事件系统
- `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`
- `FMLJavaModLoadingContext` → `IEventBus` 构造注入
- `@Mod.EventBusSubscriber` → `@EventBusSubscriber`
- `net.minecraftforge.eventbus` → `net.neoforged.bus`

### 杂项
- `new ResourceLocation("ns", "path")` → `ResourceLocation.fromNamespaceAndPath("ns", "path")`
- `ItemStack.getTag()` / `setTag()` → `DataComponents.CUSTOM_DATA`
- `ClipContext` 构造函数 → `CollisionContext.empty()` 参数
- `LevelTickEvent.side` → `ev.getLevel().isClientSide()`
- `RenderSystem.getModelViewStack()` → `poseStack` / `RenderSystem.getModelViewMatrix()`

## 剩余问题（全部在客户端 GUI/渲染层）

### 文件列表

| 文件 | 问题 |
|------|------|
| `WDScreen.java` | `renderBackground` 签名、`mouseScrolled` 签名 |
| `Control.java` | `beginFramebuffer`/`endFramebuffer` 待重写 |
| `ControlGroup.java` | `vBuffer.begin/vertex/end` → `BufferBuilder.addVertex` |
| `GuiKeyboard.java` | `GameRenderer.getFov` 变 private、渲染 API |
| `GuiMinePad.java` | `getTag` → DataComponents、`mouseScrolled` |
| `GuiScreenConfig.java` | 渲染 API |
| `GuiServer.java` | `vBuffer` 模式 |
| `GuiSetURL2.java` | `getTag`/`setTag` → DataComponents |
| `RenderRecipe.java` | `RecipeHolder` 类型变更 |
| `TextField.java` | `mouseScrolled` 签名 |
| `Icon.java` | `ResourceLocation.fromNamespaceAndPath` |
| `ScreenModelLoader.java` | `bake()` 方法签名 |
| `ScreenRenderer.java` | 渲染 API |
| `ClientProxy.java` | `setCancelled`/`setCanceled`、部分渲染 API |
| `ItemLaserPointer.java` | `getTag`、Vec3 API |
| `ItemMinePad2.java` | `getTag` → DataComponents |
| `ItemOwnershipThief.java` | `getTag` → DataComponents |
| `AnnoCFG.java` | `ModLoadingContext` API |
| `JSServerRequest.java` | `FriendlyByteBuf` 序列化 |
| `Packet.java` | `replyHandler` |

### 核心 API 变更模式

```java
// renderBackground
// 旧: renderBackground(poseStack)
// 新: renderBackground(guiGraphics, mouseX, mouseY, partialTick)

// mouseScrolled
// 旧: mouseScrolled(double mouseX, double mouseY, double delta)
// 新: mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)

// BufferBuilder
// 旧: vBuffer.begin(mode, format); vBuffer.vertex(...); tessellator.end();
// 新: BufferBuilder b = Tesselator.getInstance().begin(mode, format);
//     b.addVertex(mat, x, y, z); BufferUploader.drawWithShader(b.buildOrThrow());

// ItemStack NBT
// 旧: stack.getTag().getUUID("key")
// 新: stack.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("key")

// GameRenderer.getFov
// 已变为 private，需使用其他方式获取 FOV
```

## 构建命令

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot"
cd /c/Users/14408/Desktop/1.21.1
./gradlew compileJava --no-daemon
```

## 注意事项

1. MCEF 依赖从 `https://mcef-download.cinemamod.com/repositories/releases` 获取
2. NeoForge maven 需要 `https://maven.neoforged.net/releases` 可访问
3. Gradle wrapper 使用腾讯云镜像加速
4. 部分 TODO 标记的功能（能力系统附件、framebuffer 操作）需要后续完善
