# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY

**Правило**: ≤200 строк на файл, ≤4 файлов на папку.  
**Цель**: Каждый файл — одна ответственность (SRP), читается без скролла.

### Terminal (сейчас: 1 папка, 1 файл — 1263 строки)

```
vm/terminal/
├── Terminal.java           # парсер VT100 (~400 строк)
├── TerminalBuffer.java     # буферы main + alt, scrollback (~200)
├── TerminalRenderer.java   # VBO рендеринг (= бывший Renderer, ~400)
└── TerminalColors.java     # палитры, ColorData, ColorMode (~200)
```

### Robot (сейчас: 1 папка, 4 файла — Robot.java 964 строки)

```
entity/
├── Robot.java              # Entity + lifecycle (~300)
├── robot/
│   ├── RobotInventory.java     # инвентарь (~200)
│   ├── RobotMovementController.java  # движение, анимация (~200)
│   └── RobotContainer.java     # container menu (~200)
```

### ComputerBlockEntity (сейчас: 693 строки)

```
blockentity/
├── ComputerBlockEntity.java    # BE (~300)
├── computer/
│   ├── ComputerVMRunner.java   # вынос из inner класса (~200)
│   └── ComputerTerminalHandler.java  # terminal I/O (~150)
```

### build.gradle (сейчас: 382 строки)

```
build.gradle          # сборка + deps (~200)
publish.gradle        # curseforge + modrinth + gpr (~150)
```

### blockentity/ (сейчас: 15+ файлов в 1 папке)

```            
blockentity/
├── ModBlockEntity.java
├── computer/
│   └── ComputerBlockEntity.java
├── monitor/
│   └── MonitorBlockEntity.java
├── keyboard/
│   └── KeyboardBlockEntity.java
├── disk/
│   └── DiskDriveBlockEntity.java
├── network/
│   ├── BusCableBlockEntity.java
│   ├── NetworkConnectorBlockEntity.java
│   └── NetworkHubBlockEntity.java
├── energy/
│   ├── ChargerBlockEntity.java
│   └── CreativeEnergyBlockEntity.java
├── misc/
│   ├── FlashMemoryFlasherBlockEntity.java
│   └── InternetGateWayBlockEntity.java
├── projector/
│   └── ProjectorBlockEntity.java
└── robot/
    └── RobotProxyBlockEntity.java
```

### common/bus/ (сейчас: 10+ файлов)

```
bus/
├── adapter/
│   ├── RPCDeviceBusAdapter.java
│   └── VMDeviceBusAdapter.java
├── device/
│   ├── rpc/
│   └── vm/
├── element/
│   ├── AbstractBlockDeviceBusElement.java
│   ├── AbstractItemDeviceBusElement.java
│   └── AbstractGroupingDeviceBusElement.java
└── controller/
    └── DeviceBusController.java
```

### client/gui/ (сейчас: 10+ файлов)

```
gui/
├── screen/
│   ├── ComputerTerminalScreen.java
│   ├── MonitorDisplayScreen.java
│   ├── KeyboardScreen.java
│   └── RobotTerminalScreen.java
├── widget/
│   ├── MachineTerminalWidget.java
│   ├── MonitorDisplayWidget.java
│   └── ...
├── container/
│   └── ...
└── Sprites.java, Textures.java
```

### Deferred
- jcodec/ (17k строк, H.264) — **не трогать**, внешняя либа
- `Terminal.java` — без `OldTerminal.java` (дубль на удаление)

### Порядок работ
- [ ] Выделить TerminalBuffer, TerminalColors, TerminalRenderer из Terminal.java
- [ ] Вынести ComputerVMRunner из ComputerBlockEntity
- [ ] Разбить Robot.java на Robot + RobotInventory + RobotMovementController
- [ ] Разложить blockentity/ по подпапкам
- [ ] Вынести publish логику из build.gradle в publish.gradle
- [ ] Разложить bus/ по подпапкам
- [ ] Разложить gui/ по подпапкам
- [ ] Удалить OldTerminal.java если не используется

---

## 1. C API для Redstone Interface (#89)

**Проблема**: Lua на VM медленный, нет `sleep()`, неудобно для real-time контроллеров (ноут блок плеер и т.д.). Хочется писать на C/Rust под RISC-V.

**Текущее состояние**: Сделана C-библиотека `librpc` в `src/main/scripts/lib/rpc/`. Реализует RPC-протокол поверх `/dev/hvc0`. Позволяет из C-программы на RISC-V VM находить устройства, вызывать их методы (setRedstoneOutput, getRedstoneInput и т.д.).

### Файлы
- `src/main/scripts/lib/rpc/rpc.h` — заголовочный файл
- `src/main/scripts/lib/rpc/rpc.c` — реализация

### Использование
```c
#include "rpc/rpc.h"

int main() {
    rpc_bus_t bus;
    rpc_bus_open(&bus, "/dev/hvc0");

    rpc_device_t redstone;
    if (rpc_bus_find(&bus, "redstone", &redstone)) {
        rpc_device_invoke(&redstone, "setRedstoneOutput",
            "\"up\"", 15);
        int level = rpc_device_invoke_int(&redstone,
            "getRedstoneInput", "\"front\"");
    }

    rpc_bus_close(&bus);
    return 0;
}
```

### Сборка под RISC-V
```bash
riscv64-linux-gnu-gcc -static -o program.elf program.c rpc.c
```
Либо скопировать исходники на VM и скомпилировать через TCC (когда появится).

### Что дальше
- [ ] Добавить TCC (Tiny C Compiler) в buildroot-образ, чтобы компилировать C прямо на VM
- [ ] Добавить примеры: redstone_blink.c, note_block_player.c
- [ ] Сделать C++ RAII-обёртку

---

## 2. Resizable Screen (#12)

**Проблема**: Экран (Terminal/Monitor) фиксированного размера — 80×24 символов (640×384px). Хочется расширять добавлением блоков как в OC1.

**Сложность**: ОЧЕНЬ высокая. Размер Terminal жёстко зашит как `static final` константы во всех буферах: `buffer[WIDTH*HEIGHT*SCROLL_BACK_COUNT]`, `colors[...]`, `styles[...]`, `altBuffer[...]` и т.д. — ~30+ мест в одном только `Terminal.java`. Плюс рендеринг (VBO по строкам), UART-драйвер, сетевые пакеты, GUI-виджеты, блок-модели.

### Необходимые изменения

#### Java (backend)
- [ ] `Terminal.java`: WIDTH/HEIGHT из констант → поля экземпляра, динамические буферы
- [ ] `MonitorDevice.java`: WIDTH/HEIGHT из констант → поля
- [ ] `SimpleFramebufferDevice.java`: размер framebuffer динамический
- [ ] `ComputerBlockEntity.java`: передавать размер терминала
- [ ] `MonitorBlockEntity.java`: хранить размер в NBT
- [ ] `AbstractTerminalVMRunner.java`: размер терминала из BE

#### Сеть (packets)
- [ ] Синхронизировать размер между сервером и клиентом

#### Рендеринг (клиент)
- [ ] `ComputerRenderer.java`: размер области рендера
- [ ] `MonitorRenderer.java`: размер области рендера
- [ ] `MachineTerminalWidget.java`: размер GUI

#### Блоки (регистрация)
- [ ] Новые блоки (MonitorSmall, MonitorMedium, MonitorLarge)
- [ ] Или property `size` на существующем блоке + разные VoxelShape
- [ ] Крафты для расширения

### Альтернатива — SimpleFramebuffer через Projector
Проектор (`ProjectorBlockEntity`) уже умеет проецировать 640×480 framebuffer на поверхности. Можно сделать монитор, который рендерит ту же картинку что и проектор, но в GUI/на блоке — по сути "монитор-проектор". Без изменения Terminal.

### Приоритет
Пока отложено. Сначала — рефакторинг и TCC.

---

## 3. TCC (Tiny C Compiler) в образ

**Проблема**: Для C API нужно где-то компилировать. Кросс-компиляция на хосте неудобна.
**Решение**: Добавить TCC (или GCC static) в buildroot-образ, чтобы можно было писать `tcc -o prog prog.c` прямо на VM.

- [ ] Обновить `minux` (sedna-buildroot) чтобы включить TCC
- [ ] Либо сделать overlay с бинарником TCC
- [ ] Собрать новый buildroot-образ

---

## 4. Internet Card (#54)

**Описание** (из fnuecke/oc2#54): Полноценная сетевая карта с TCP/IP стеком на Java. Есть PR #63 с работающим SSH/UDP/TCP. Автор думал оформить отдельным модом.

- [ ] Решить: портировать как часть OC2R или сделать аддон
- [ ] Если портировать — оценить объём кода из PR #63
