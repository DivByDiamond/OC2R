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

### План распила (build + commit после каждого)

Очерёдность: от самых больших файлов к маленьким, группами по 4 параллельных агента.

#### Группа 1: 4 самых больших файла ✅
- [x] ComputerBlockEntity.java (685→499) → извлечены 3 inner класса в подпакеты (bus/, vm/, handler/)
- [x] ProjectorDepthRenderer.java (614→480) → извлечены DepthOnlyRenderTarget, ProjectorCameraEntity, ProjectorDepthRenderInfo
- [x] BusCableBlock.java (584→454) → извлечены ConnectionType, BusCableShapeBuilder
- [x] RPCDeviceBusAdapter.java (560→491) → извлечены все 5 внутренних типов

#### Группа 2: 4 файла 472-520 строк
- [ ] DefaultTransportLayer.java (520) → извлечь ICMPReply, SessionReceiver
- [ ] NetworkSwitchBlockEntity.java (511) → извлечь HostEntry, LuaHostEntry, PortSettings, SwitchLog
- [ ] ProjectorBlockEntity.java (479) → извлечь FrameConsumer, VideoEncoder/Decoder логику
- [ ] BusCableBlockEntity.java (477) → извлечь FacadeType, BusCableBusElement, NeighborListener

#### Группа 3: 4 файла 428-473 строк
- [ ] MonitorBlockEntity.java (473) → извлечь FrameConsumer, VideoEncoder/Decoder
- [ ] NetworkConnectorBlockEntity.java (472) → извлечь ConnectionResult, NullNetworkInterface, NetworkConnectorNetworkInterface
- [ ] FileChooserScreen.java (433) → извлечь FileChooserCallback, FileList, FileEntry
- [ ] AbstractVirtualMachine.java (428) → извлечь SerializedState, lifecycle-методы

#### Группа 4: 4 файла 367-420 строк
- [ ] AbstractBlockStorageDevice.java (414) → split
- [ ] ComputerRenderer.java (367) → split
- [ ] StreamSessionImpl.java (353) → split
- [ ] TerminalRenderer.java (351) → split

#### Группа 5: 4 файла 306-341 строк
- [ ] NetworkCableRenderer.java (341) → split
- [ ] CommonDeviceBusController.java (332) → split
- [ ] InventoryOperationsModuleDevice.java (331) → split
- [ ] RedstoneInterfaceBlockEntity.java (330) → split

#### Группа 6: 4 файла 286-324 строк
- [ ] FileImportExportCardItemDevice.java (324) → split
- [ ] ProjectorLoadBalancer.java (316) → split
- [ ] MonitorLoadBalancer.java (316) → split
- [ ] MachineTerminalWidget.java (313) → split

#### Группа 7: 4 файла 274-306 строк
- [ ] BlobStorage.java (306) → split
- [ ] BlockOperationsModuleDevice.java (305) → split
- [ ] NetworkInterfaceCardScreen.java (302) → split
- [ ] TerminalBuffer.java (294) → split

#### Группа 8: 4 файла 252-286 строк
- [ ] InetUtils.java (291) → split
- [ ] AsyncUtils.java (286) → split
- [ ] DefaultSessionLayer.java (277) → split
- [ ] ComputerBlock.java (274) → split

#### Группа 9: 4 файла 246-272 строк
- [ ] AbstractMachineTerminalScreen.java (272) → split
- [ ] AbstractGroupingDeviceBusElement.java (268) → split
- [ ] ServerScheduler.java (261) → split
- [ ] MonitorRenderer.java (256) → split

#### Группа 10: 4 файла 240-256 строк
- [ ] RobotMovementController.java (256) → split
- [ ] DiskDriveBlockEntity.java (255) → split
- [ ] FlashMemoryFlasherBlockEntity.java (252) → split
- [ ] InternetManagerImpl.java (246) → split

#### Группа 11: 4 файла 205-240 строк
- [ ] ProjectorRenderer.java (240) → split
- [ ] InternetGateWayBlockEntity.java (220) → split
- [ ] MessageUtils.java (217) → split
- [ ] AbstractMonitorDisplayScreen.java (216) → split

#### Группа 12: остальные (200-210 строк, ~8 файлов)
- [ ] CH2.java (210) → split
- [ ] TooltipUtils.java (208) → split
- [ ] DefaultLinkLocalLayer.java (208) → split
- [ ] NBTArraySerializers.java (206) → split
- [ ] Main.java (205) → split
- [ ] VxlanBlockEntity.java (203) → split
- [ ] Terminal.java (420) → уже <200 не нужно? Нет, Terminal всё ещё >200
- [ ] Остальные ~2 файла

### Deferred
- jcodec/ — **не трогать**
- api/ — **не трогать**

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

---

## Lint инструменты (включить после рефакторинга)

**Конфиги уже настроены**, но отключены чтобы не мешать рефакторингу.
После рефакторинга — включить и пройтись по всем замечаниям.

### Стек
| Инструмент | Что делает | Включение |
|---|---|---|
| **Checkstyle** | Стиль кода | `tasks.withType(Checkstyle).configureEach { enabled = true }` |
| **PMD** | Мёртвый код, дубли | `tasks.withType(Pmd).configureEach { enabled = true }` |
| **SpotBugs** | NPE, утечки (Gradle 9 несовместим) | `plugins { id 'com.github.spotbugs' version '6.1.7' }` |
| **Error Prone** | Баги при компиляции (Google) | Настройка компилятора |

### Конфиги
- `checkstyle.xml` — Google Style, 200 строк/файл, JavaDoc на public API
- `config/pmd/ruleset.xml` — bestpractices + errorprone + performance
- `qodana.yaml` — Qodana (IDEA движок)

### Команды
```bash
# Lint
./gradlew checkstyleMain
./gradlew pmdMain

# Qodana (Docker)
docker run --rm -e QODANA_TOKEN -v .:/data/project -v ./qodana-report:/data/results jetbrains/qodana-jvm:latest

# SpotBugs (требует Gradle <9 или новую версию плагина)
./gradlew spotbugsMain
```
