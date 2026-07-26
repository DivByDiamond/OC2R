# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY ✅

**Правило**: ≤200 строк на файл, ≤4 файлов на папку.

### Выполнено
- [x] Terminal.java (1263→420) → Terminal, TerminalBuffer, TerminalRenderer, TerminalColors
- [x] ComputerVMRunner из ComputerBlockEntity
- [x] Robot.java (964→710) → RobotInventory, RobotMovementController
- [x] blockentity/ по подпапкам
- [x] publish.gradle из build.gradle
- [x] bus/ → adapter/, controller/, element/
- [x] gui/ → screen/, widget/
- [x] OldTerminal.java удалён

### >200 строк — план распила (build + commit после каждого)

Кап: 30 файлов >200 строк → довести все до <200.

#### Файл 1: `entity/Robot.java` (710)
- `RobotEnergyStorage` — энергия (поля/методы по энергии)
- `RobotNetworkHandler` — сетевые сообщения
- `RobotUpgradeHandler` — проверки апгрейдов
- `Robot` (остаток)

#### Файл 2: `serialization/NBTSerialization.java` (693)
- `ItemStackSerialization`
- `BlockStateSerialization` (+ Fluid, Energy, Collection)
- `NBTSerialization` (остаток)

#### Файл 3: `blockentity/computer/ComputerBlockEntity.java` (685)
- `ComputerEnergyStorage`
- `ComputerItemStackHandlers`
- `ComputerScreens`
- `ComputerBlockEntity` (остаток)

#### Файл 4: `renderer/ProjectorDepthRenderer.java` (614)
- `ProjectorDepthMeshBuilder`
- `ProjectorDepthShader`
- `ProjectorDepthFrameBuffer`
- `ProjectorFrustumCuller`
- `ProjectorDepthRenderer` (остаток)

#### Файл 5: `block/BusCableBlock.java` (584)
- `BusCableShapeBuilder`
- `BusCableConnectionManager`
- `BusCableBlock` (остаток)

#### Файл 6: `bus/adapter/RPCDeviceBusAdapter.java` (560)
- `RPCDeviceRegistry`
- `RPCMethodDispatcher`
- `RPCExecutionContext`
- `RPCDeviceBusAdapter` (остаток)

#### Файл 7: `inet/DefaultTransportLayer.java` (520)
- `TcpConnectionManager`
- `PacketFragmentation`
- `DefaultTransportLayer` (остаток)

#### Файл 8: `blockentity/network/NetworkSwitchBlockEntity.java` (511)
- `NetworkSwitchPortManager`
- `NetworkSwitchPacketProcessor`
- `NetworkSwitchRoutingTable`
- `NetworkSwitchBlockEntity` (остаток)

#### Файлы 9-12: `ProjectorBlockEntity` (479), `BusCableBlockEntity` (477), `MonitorBlockEntity` (473), `NetworkConnectorBlockEntity` (472)
Каждый:
- `*EnergyStorage` — энергия
- `*NetworkHandler` — сетевые сообщения
- основа (~200)

#### Файл 13: `gui/screen/FileChooserScreen.java` (433)
- `FileListWidget`
- `FileActionsBar`
- `FileChooserScreen` (остаток)

#### Файл 14: `vm/AbstractVirtualMachine.java` (428)
- `VMStateMachine`
- `VMDeviceManager`
- `VMInterruptHandler`
- `AbstractVirtualMachine` (остаток)

#### Файл 15: `bus/device/vm/item/AbstractBlockStorageDevice.java` (414)
- `BlockStorageGeometry`
- `BlockStorageIO`
- `AbstractBlockStorageDevice` (остаток)

#### Файлы 16-20: `ComputerRenderer` (367), `StreamSessionImpl` (353), `TerminalRenderer` (351), `NetworkCableRenderer` (341), `CommonDeviceBusController` (332)
Каждый → 2 части

#### Файлы 21-25: `InventoryOperationsModuleDevice` (331), `RedstoneInterfaceBlockEntity` (330), `FileImportExportCardItemDevice` (324), `ProjectorLoadBalancer` (316), `MonitorLoadBalancer` (316)
Каждый → utility/logic вынос

#### Файлы 26-30: `MachineTerminalWidget` (313), `BlobStorage` (306), `BlockOperationsModuleDevice` (305), `NetworkInterfaceCardScreen` (302), `TerminalBuffer` (294)
Каждый → split 2-3

#### Остальные (200-290 строк, ~20 файлов)
По 1 split на файл

### Deferred
- jcodec/ — **не трогать**

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
