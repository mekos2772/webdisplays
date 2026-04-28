# WebDisplays for NeoForge 1.21.1

WebDisplays is a Minecraft mod that adds working web browsers into the game as in-world screen blocks. Place screens, link keyboards and other peripherals, browse the internet inside Minecraft.

Ported from [CinemaMod/webdisplays](https://github.com/CinemaMod/webdisplays) to NeoForge 1.21.1.

## Dependencies

- **NeoForge** 21.1+
- **MCEF** (Minecraft Chromium Embedded Framework) 2.1.6+

## Features

- Placeable screen blocks with real web browsing (via Chromium)
- MinePad — handheld web tablet
- Keyboard, Remote Control, and Redstone Control peripherals
- Laser pointer interaction with screens
- Multi-block screens with configurable resolution
- Custom advancements (pad break, screen upgrade, link peripheral, keyboard cat)
- Miniserv file hosting server built-in

## Build

```bash
./gradlew build --no-daemon
```

Output: `build/libs/webdisplays-2.0.0-1.21.1.jar`

## Credits

- Original mod by **BARBOTIN Nicolas** (CinemaMod)
- Ported to NeoForge 1.21.1 by [mekos2772](https://github.com/mekos2772)
- Uses [MCEF](https://github.com/CinemaMod/mcef) for Chromium integration

## License

Same as the [original project](https://github.com/CinemaMod/webdisplays).
