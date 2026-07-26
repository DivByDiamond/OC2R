<div align="center">

# OC2R

Minecraft mod adding virtual computers with RISC-V emulation. Fork of [North-Western-Development/OC2R], itself a fork of [OpenComputers 2] by Sangar.

![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square&logo=openjdk&logoColor=white)
![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square&logo=gnu&logoColor=white)
![Version](https://img.shields.io/badge/version-0.1.0-darkgreen?style=flat-square)

[English](#english) | [Русский](#russian)

</div>

---

<a name="english"></a>

## English

### Overview

OC2R is a Minecraft mod that adds fully functional virtual computers to the game. It runs a 64-bit RISC-V virtual machine (powered by [Sedna]) capable of booting Linux, with a high-level Lua API for interacting with in-game devices.

This repository is a **fork** of [North-Western-Development/OC2R], which was itself a **fork** of Sangar's original [OpenComputers 2]. The goal is to continue maintenance, fix bugs, and support newer Minecraft versions.

### Features

| Feature | Description |
|---------|-------------|
| RISC-V VM | Full 64-bit RISC-V emulation, boots Linux |
| Lua API | High-level device interaction without kernel drivers |
| Custom hardware | Blocks, items, cables, screens, keyboards, etc. |
| Native networking | Cross-platform native networking library |
| Persistence | Computer state saved between reloads |
| Device API | Simple Java API for adding virtual devices |

### Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/computer` | — | Interact with computers |

### Dependencies

- Required: NeoForge 21.1.243+, Java 21+
- Optional: JEI (recipe viewer)

### Installation

1. Download the jar from [Releases](../../releases)
2. Place it in `mods/`
3. Restart the server/client

---

<a name="russian"></a>

## Русский

### Обзор

OC2R — мод для Minecraft, добавляющий полноценные виртуальные компьютеры. Он запускает 64-битную RISC-V виртуальную машину (на базе [Sedna]), способную загружать Linux, с высокоуровневым Lua API для взаимодействия с игровыми устройствами.

Этот репозиторий является **форком** [North-Western-Development/OC2R], который сам был **форком** оригинального [OpenComputers 2] от Sangar. Цель — продолжать поддержку, исправлять баги и поддерживать актуальные версии Minecraft.

### Возможности

| Возможность | Описание |
|-------------|----------|
| RISC-V VM | Полная эмуляция 64-битного RISC-V, загрузка Linux |
| Lua API | Взаимодействие с устройствами без драйверов ядра |
| Кастомное железо | Блоки, предметы, провода, экраны, клавиатуры и т.д. |
| Нативная сеть | Кроссплатформенная нативная библиотека |
| Персистентность | Состояние компьютера сохраняется между перезагрузками |
| Device API | Простой Java API для добавления устройств |

### Команды

| Команда | Право | Описание |
|---------|-------|----------|
| `/computer` | — | Взаимодействие с компьютерами |

### Зависимости

- Обязательные: NeoForge 21.1.243+, Java 21+
- Опциональные: JEI (просмотр рецептов)

### Установка

1. Скачайте jar из [Releases](../../releases)
2. Поместите в `mods/`
3. Перезапустите сервер/клиент

---

### Building

```bash
./gradlew build
```

### License

GNU General Public License v3.0

[North-Western-Development/OC2R]: https://github.com/North-Western-Development/OC2R
[OpenComputers 2]: https://github.com/fnuecke/oc2
[Sedna]: https://github.com/fnuecke/sedna
