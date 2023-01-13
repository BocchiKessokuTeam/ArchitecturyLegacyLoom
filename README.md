# ArchitecturyLoom-Legacy

---

A fork of [MsRandom's Loom fork](https://github.com/MsRandom/architectury-loom/tree/0.12.0-legacyfg3) that is a fork of [Architectury Loom](https://github.com/architectury/architectury-loom) that supports the Legacy Forge modding toolchain.

A [Gradle](https://gradle.org/) plugin to setup a deobfuscated development environment for Minecraft mods. Primarily used in the Fabric toolchain.

* Has built in support for tiny mappings ~~(Used by [Yarn](https://github.com/FabricMC/yarn))~~
* Utilises the Fernflower and CFR decompilers to generate source code with comments.
* Designed to support ~~modern~~ **Legacy** versions of Minecraft (Tested in version 1.12.2 ~~and below~~)
* ~~Built in support for IntelliJ IDEA, Eclipse and Visual Studio Code to generate run configurations for Minecraft.~~
  - Currently, only IntelliJ IDEA and Visual Studio Code work with Forge Loom.
* Loom targets the latest version of Gradle 7 or newer 
* Supports Java 17 upwards

## Usage

View the [documentation](https://architectury.github.io/architectury-documentations/docs/forge_loom/) for usages.
