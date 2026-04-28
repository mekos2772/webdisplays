# WebDisplays 1.21.1 修复计划

更新时间: 2026-04-28

这份文档给 DS 直接执行用。目标不是继续零散救火，而是一次性处理当前已经确认的 4 个问题，并补一轮构建与启动验证。

## 总体顺序

1. 先修启动崩溃
2. 再修 GUI 行为回归
3. 再把 advancement trigger 接回去
4. 最后做完整构建和启动复查

## 问题 1: `GuiSetURL2` 被错误注册到事件总线

文件:

- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/client/gui/GuiSetURL2.java`

现象:

- 最新启动崩溃来自 `GuiSetURL2`
- NeoForge 在模组构造阶段自动注册了这个类
- 但这个类没有任何 `@SubscribeEvent` 方法, 只有 GUI 内部使用的 `@GuiSubscribe`

根因:

- 类上存在 `@EventBusSubscriber(Dist.CLIENT)`

处理:

1. 删除类注解 `@EventBusSubscriber(Dist.CLIENT)`
2. 删除不再需要的导入:
   - `net.neoforged.api.distmarker.Dist`
   - `net.neoforged.fml.common.Mod`
   - `net.neoforged.fml.common.EventBusSubscriber`
3. 不要改动 `@GuiSubscribe` 相关逻辑

预期结果:

- 模组不再因为 `GuiSetURL2 has no @SubscribeEvent methods` 在构造阶段崩溃

## 问题 2: 非键盘界面的按键被错误委托给新建的 `GuiServer`

文件:

- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/client/gui/WDScreen.java`

现象:

- 当前 `keyPressed(...)` 在不是 `GuiKeyboard` 的情况下, 会 `new GuiServer(...).keyPressed(...)`
- 这会把普通界面错误地走到终端界面的按键逻辑
- 还会在每次按键时额外创建一个无意义的 `GuiServer`

当前错误代码意图:

- `GuiKeyboard` 想自己吞掉按键
- 其他界面本来应该走标准 `Screen` 的行为

处理:

将 `keyPressed(int keyCode, int scanCode, int modifiers)` 改成下面这种语义:

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    boolean down = false;

    for (Control ctrl : controls)
        down = down || ctrl.keyDown(keyCode, scanCode, modifiers);

    if (this instanceof GuiKeyboard)
        return down;

    return down || super.keyPressed(keyCode, scanCode, modifiers);
}
```

额外说明:

- 同时删除这个文件里不再需要的导入:
  - `net.montoyo.wd.utilities.math.Vector3i`
  - `net.montoyo.wd.utilities.serialization.NameUUIDPair`
- 不要保留任何 `new GuiServer(...)` 的兜底逻辑

预期结果:

- 普通 GUI 恢复标准按键行为
- `GuiKeyboard` 保持自己的特殊处理

## 问题 3: URL 校验失败会直接把客户端抛崩

文件:

- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/client/gui/GuiSetURL2.java`

现象:

- `validate(String url)` 中如果 `ScreenBlockEntity.url(url)` 失败, 当前会 `throw new RuntimeException(e)`
- 这意味着用户输错 URL 可能直接把客户端打崩

处理目标:

- 非法输入时留在当前界面
- 不关闭 GUI
- 不发送网络包
- 给用户一个最小可见的反馈

建议实现:

1. 在 `try/catch` 中捕获 `IOException`
2. 捕获后直接:
   - 记录日志, 方便排查
   - 给玩家一个短提示
   - `return`
3. 只有校验通过后才继续 `Util.addProtocol(url)` 和后续发送逻辑

推荐写法方向:

```java
try {
    ScreenBlockEntity.url(url);
} catch (IOException e) {
    Log.warningEx("Invalid URL entered in GuiSetURL2: %s", e, url);
    if (minecraft != null && minecraft.player != null) {
        minecraft.player.displayClientMessage(Component.literal("Invalid URL"), true);
    }
    return;
}
```

说明:

- 如果项目里已有更合适的本地化文案或 GUI 错误提示控件, 优先复用
- 如果没有, 先用最小可工作的 `displayClientMessage(...)`
- 重点是不要再抛 `RuntimeException`

预期结果:

- 用户输错 URL 时界面仍然打开
- 不会发生客户端崩溃

## 问题 4: 自定义 advancement trigger 当前完全断开

文件:

- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/WebDisplays.java`
- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/core/Criterion.java`

关联资源:

- `C:/Users/14408/Desktop/1.21.1/src/main/resources/data/webdisplays/advancements/pad_break.json`
- `C:/Users/14408/Desktop/1.21.1/src/main/resources/data/webdisplays/advancements/upgrade.json`
- `C:/Users/14408/Desktop/1.21.1/src/main/resources/data/webdisplays/advancements/link_peripheral.json`
- `C:/Users/14408/Desktop/1.21.1/src/main/resources/data/webdisplays/advancements/keyboard_cat.json`

触发位置:

- `ScreenBlock.java` 触发 `criterionUpgradeScreen`
- `KeyboardBlockEntity.java` 触发 `criterionKeyboardCat`
- `ItemLinker.java` 触发 `criterionLinkPeripheral`
- `ItemMinePad2.java` 触发 `criterionPadBreak`

现象:

- `registerTrigger(Criterion ... criteria)` 现在是空实现
- 资源包里的 advancement JSON 仍然引用这些 trigger
- 结果是 trigger 名字存在于数据里, 但运行时没有注册到 `CriteriaTriggers`
- `hardRecipes` 路径又依赖 `webdisplays:pad_break`, 所以这是功能回归

建议方案:

### 方案 A: 正式接回自定义 trigger

这是推荐方案。

实现思路:

1. 新建一个真正的 trigger 类型, 基于 1.21.1 的 `SimpleCriterionTrigger`
2. 为它提供:
   - `codec()`
   - `trigger(ServerPlayer player)`
   - 对应的 `SimpleInstance`
3. 在 `WebDisplays` 构造时用 `CriteriaTriggers.register(...)` 注册 4 个 trigger
4. 把现有的 `Criterion` 旧实现迁掉, 或者保留名字但内部改成新 trigger 包装

建议落地方式:

- 保持现有外部调用点尽量不变
- 让下面这些调用继续成立:

```java
WebDisplays.INSTANCE.criterionPadBreak.trigger(serverPlayer);
```

也就是说, 最省波动的做法是:

1. 重写 `net.montoyo.wd.core.Criterion`
2. 让它继承 `SimpleCriterionTrigger<Criterion.Instance>`
3. 提供 `trigger(ServerPlayer player)` 方法
4. `WebDisplays.registerTrigger(...)` 中逐个 `CriteriaTriggers.register(...)`

注意:

- 现在已有调用是 `trigger(((ServerPlayer) player).getAdvancements())`
- 这一层建议统一改成传 `ServerPlayer`
- 因为 `SimpleCriterionTrigger` 的标准触发入口就是 `trigger(ServerPlayer, Predicate<T>)`

需要同步改动的调用点:

- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/block/ScreenBlock.java`
- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/entity/KeyboardBlockEntity.java`
- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/item/ItemLinker.java`
- `C:/Users/14408/Desktop/1.21.1/src/main/java/net/montoyo/wd/item/ItemMinePad2.java`

### 方案 B: 先止血, 再正式迁移

如果 DS 当下只想先让运行链路恢复, 可以临时这样做:

1. 保留 `registerTrigger(...)` 暂不完成
2. 把 `hardRecipes` 的 advancement 门槛去掉或改成更稳的条件

这只能算过渡。

原因:

- advancement JSON 依然会引用不存在的 trigger
- 运行期仍可能继续报相关数据加载问题
- 功能上也不是完整迁移

结论:

- 优先做方案 A

## 构建与验证

代码改完后, 至少做下面两轮验证。

### 第 1 轮: 编译与打包

执行:

```powershell
./gradlew build --no-daemon
```

通过标准:

- `BUILD SUCCESSFUL`
- 产物更新到:
  - `C:/Users/14408/Desktop/1.21.1/build/libs/webdisplays-2.0.0-1.21.1.jar`

### 第 2 轮: 启动验证

启动环境要求:

- `webdisplays-2.0.0-1.21.1.jar`
- `mcef-2.1.6-1.21.1.jar`

重点检查:

1. 模组构造阶段不再因为事件注册崩溃
2. advancement 数据加载阶段不再因为自定义 trigger 缺失报错
3. 进入游戏后打开普通 GUI, 键盘行为正常
4. 打开设置 URL 界面, 输入非法 URL 时不会崩溃
5. 与下列行为相关的 advancement 能正常触发:
   - minePad 破坏
   - 升级屏幕
   - 链接外设
   - 键盘彩蛋

## 推荐提交顺序

给 DS 的实际执行顺序建议如下:

1. 修 `GuiSetURL2` 误注册
2. 修 `WDScreen.keyPressed(...)`
3. 修 `GuiSetURL2.validate(...)`
4. 正式迁移 `Criterion`
5. 改 4 个 trigger 调用点为 `ServerPlayer`
6. 跑 `./gradlew build --no-daemon`
7. 装上 `MCEF` 后做一次真实启动验证

## 备注

- 这轮不要再扩大范围做无关重构
- 优先保持现有类名和调用关系稳定
- 先把启动和核心玩法回归收干净, 再考虑继续清 warning 或做风格整理
