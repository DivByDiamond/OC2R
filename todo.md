# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY ✅ — ~70%

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
- [x] DefaultTransportLayer.java (520→170)
- [x] NetworkSwitchBlockEntity.java (511→226) — извлечены HostEntry, LuaHostEntry, PortSettings, SwitchLog
- [x] ProjectorBlockEntity.java (479→222)
- [x] BusCableBlockEntity.java (477→211)
- [x] AbstractBlockStorageDevice.java (414→204)
- [x] ComputerRenderer.java (367→207)
- [x] StreamSessionImpl.java (353→114)
- [x] CommonDeviceBusController.java (332→140)
- [x] BlobStorage.java (306→112)
- [x] MachineTerminalWidget.java (313→198)
- [x] NetworkInterfaceCardScreen.java (302→180)
- [x] DefaultSessionLayer.java (277→193 — вместе с фиксом fallthrough)
- [x] Большая часть групп 4-12 (файлы либо уже ≤200, либо переименованы/перемещены)

### Осталось разложить по подпапкам (≤4 файлов на папку)

Много папок нарушают правило. Приоритет — самые большие:

| Папка | Файлов | Предлагаемая структура |
|---|---|---|
| `common/inet/` | 53 | `inet/layer/`, `inet/session/`, `inet/protocol/` |
| `common/network/message/` | 43 | `message/computer/`, `message/robot/`, `message/monitor/` |
| `common/util/` | 38 | Разбить по смыслу (world, item, nbt, text) |
| `common/vm/terminal/escapes/csi/` | 31 | Вряд ли трогать — каждый CSI handler свой файл по дизайну |
| `common/block/` | 27 | `block/computer/`, `block/cable/`, `block/monitor/` |
| `client/gui/screen/` | 25 | `screen/computer/`, `screen/robot/`, `screen/monitor/` |
| `common/blockentity/network/` | 25 | Уже разбито, можно докрутить |

Полный список нарушителей — 49 папок, задача на отдельную сессию.

### Осталось довести до ≤200 (текущие размеры)
- [ ] Network.java (294)
- [ ] Robot.java (269)
- [ ] ComputerBlockEntity.java (259)
- [ ] Terminal.java (244)
- [ ] CH1.java (251) — escapse-последовательности, *возможно не трогать*
- [ ] CH6.java (240) — escapse-последовательности, *возможно не трогать*
- [ ] MonitorBlockEntity.java (240)
- [ ] TerminalBuffer.java (238)
- [ ] BusCableInteractionHandler.java (236)
- [ ] NetworkSwitchBlockEntity.java (226)
- [ ] ProjectorBlockEntity.java (222)
- [ ] NetworkConnectorBlockEntity.java (222)
- [ ] RobotMovementController.java (ориг 256, проверить текущий)
- [ ] DiskDriveBlockEntity.java (ориг 255, проверить)
- [ ] FlashMemoryFlasherBlockEntity.java (ориг 252, проверить)
- [ ] InternetManagerImpl.java (ориг 246, проверить)

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

### Что дальше
- [ ] Добавить TCC (Tiny C Compiler) в buildroot-образ, чтобы компилировать C прямо на VM
- [ ] Добавить примеры: redstone_blink.c, note_block_player.c
- [ ] Сделать C++ RAII-обёртку

---

## 2. Resizable Screen (#12)

**Проблема**: Экран (Terminal/Monitor) фиксированного размера — 80×24 символов (640×384px). Хочется расширять добавлением блоков как в OC1.

**Сложность**: ОЧЕНЬ высокая. Размер Terminal жёстко зашит как `static final` константы во всех буферах. Плюс рендеринг (VBO по строкам), UART-драйвер, сетевые пакеты, GUI-виджеты, блок-модели.

### Необходимые изменения
- [ ] `Terminal.java`: WIDTH/HEIGHT из констант → поля экземпляра, динамические буферы
- [ ] `MonitorDevice.java`: WIDTH/HEIGHT из констант → поля
- [ ] `SimpleFramebufferDevice.java`: размер framebuffer динамический
- [ ] `ComputerBlockEntity.java`: передавать размер терминала
- [ ] `MonitorBlockEntity.java`: хранить размер в NBT
- [ ] `AbstractTerminalVMRunner.java`: размер терминала из BE
- [ ] Синхронизировать размер между сервером и клиентом
- [ ] `ComputerRenderer.java`: размер области рендера
- [ ] `MonitorRenderer.java`: размер области рендера
- [ ] `MachineTerminalWidget.java`: размер GUI
- [ ] Новые блоки (MonitorSmall, MonitorMedium, MonitorLarge) или property `size`

### Приоритет
Пока отложено. Сначала — рефакторинг и TCC.

---

## 3. TCC (Tiny C Compiler) в образ

- [ ] Обновить `minux` (sedna-buildroot) чтобы включить TCC
- [ ] Либо сделать overlay с бинарником TCC
- [ ] Собрать новый buildroot-образ



## 5. Lint и статический анализ

**Статус**: Checkstyle и PMD уже включены в сборку. Сборка чистая.

### Что можно улучшить
- [ ] **PMD: добавить `category/java/multithreading.xml`** — ловит race conditions, небезопасную инициализацию, некорректный `volatile`. Актуально для:
  - Асинхронного I/O в `BlobChannelManager`
  - Сетевого стека (`DefaultSessionLayer`, `InetUtils`)
  - `CompletableFuture` в `DiskDriveDevice`, `FlashMemoryFlasherDevice`
- [ ] **PMD: `codestyle.xml` и `design.xml`** — добавить **после** завершения рефакторинга, иначе много шума
- [ ] **SpotBugs** — требует Gradle <9 или новую версию плагина
- [ ] **Error Prone** — настройка компилятора, Google-стиль
- [ ] **AvoidDuplicateLiterals** — включить с кастомными порогами вместо полного исключения
- [ ] **AvoidInstantiatingObjectsInLoops** — включить, супреснить точечно `@SuppressWarnings`

### Команды
```bash
./gradlew checkstyleMain
./gradlew pmdMain
```

### Конфиги
- `checkstyle.xml` — Google Style, 200 строк/файл, JavaDoc на public API
- `config/pmd/ruleset.xml` — bestpractices + errorprone + performance (+ multithreading когда включим)
- `qodana.yaml` — Qodana (IDEA движок)
