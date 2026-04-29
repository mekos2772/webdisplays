# WebDisplays — NeoForge 1.21.1 移植版

在 Minecraft 世界中放置可用的网页浏览器的模组。放置屏幕、连接键盘和外设，在游戏内浏览互联网。

从 [CinemaMod/webdisplays](https://github.com/CinemaMod/webdisplays) 移植到 NeoForge 1.21.1。

## 依赖

- **NeoForge** 21.1+
- **MCEF** (Minecraft Chromium Embedded Framework) 2.1.6+

## 功能

- 可放置的屏幕方块，支持真实网页浏览（基于 Chromium）
- MinePad — 手持网页平板
- 键盘、遥控器、红石控制器等外设
- 激光笔与屏幕交互
- 支持多格拼接的大屏幕，可配置分辨率
- 自定义进度（砸 Pad、升级屏幕、链接外设、键盘彩蛋）
- 内置 Miniserv 文件托管服务器

## 构建

```bash
./gradlew build --no-daemon
```

输出：`build/libs/webdisplays-2.0.0-1.21.1.jar`

## 致谢

- 原作者：**BARBOTIN Nicolas** (CinemaMod)
- NeoForge 1.21.1 移植：[mekos2772](https://github.com/mekos2772)
- 浏览器内核：[MCEF](https://github.com/CinemaMod/mcef)

## 许可证

同[原项目](https://github.com/CinemaMod/webdisplays)。
