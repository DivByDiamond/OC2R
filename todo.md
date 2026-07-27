# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY ✅ — ~98%

**Правило**: ≤200 строк на файл, ≤4 файлов на папку.

### Выполнено
- [x] Terminal.java (1263→420) → Terminal, TerminalBuffer, TerminalRenderer, TerminalColors
- [x] ComputerVMRunner из ComputerBlockEntity
- [x] Robot.java (964→181) → RobotInventory, RobotMovementController, AbstractRobotEntity
- [x] blockentity/ по подпапкам
- [x] publish.gradle из build.gradle
- [x] bus/ → adapter/, controller/, element/
- [x] gui/ → screen/, widget/
- [x] OldTerminal.java удалён
- [x] DefaultTransportLayer.java (520→170)
- [x] NetworkSwitchBlockEntity (511→164) → SwitchPacketForwarder, HostEntry, etc.
- [x] ProjectorBlockEntity (479→194) → ProjectorCapabilities
- [x] BusCableBlockEntity (477→188) → BusCableCapabilities
- [x] AbstractBlockStorageDevice (414→169) → BlobStorageCloseHelper
- [x] ComputerRenderer (367→152) → ComputerRendererDebug
- [x] StreamSessionImpl (353→114)
- [x] CommonDeviceBusController (332→140)
- [x] BlobStorage (306→112)
- [x] MachineTerminalWidget (313→198)
- [x] NetworkInterfaceCardScreen (302→180)
- [x] DefaultSessionLayer (277→193)
- [x] fix: energyInfo.getCount() → getIntCount() (upstream)
- [x] fix: Terminal fields → transient (upstream)
- [x] **Network.java (294→125) → NetworkMessages.java (102)** — sendTo* utility methods extracted
- [x] **ComputerBlockEntity (259→169) → ComputerTerminalManager (105)** — terminal/IO layer extracted
- [x] **Terminal.java (244→156)** — I/O delegation removed, callers use .io and .bufferManager
- [x] **TerminalIO.java (224→102) → TerminalOutput.java (144)** — VT100 state machine extracted
- [x] **MonitorBlockEntity (240→170) → MonitorVideoController (64), MonitorCapabilities (41)** — video/caps extracted
- [x] **TerminalBuffer (238→151) → TerminalBufferWriter (97)** — char writing extracted
- [x] **BusCableInteractionHandler (236→179) → BusCableItemHelper (77)** — item/drop logic extracted
- [x] **MonitorTextRenderer (218→104) → MonitorRendererCache (41), RendererQuadHelper (91)** — cache+quad extracted
- [x] **TerminalTextureRenderer → TerminalTextureRenderer (106) + TerminalTextureBuilder (150)** — DynamicTexture rendering
- [x] **ComputerBlock (212→186) → ComputerBlockInteraction (84)** — interaction logic
- [x] **NetworkConnectorBlockEntity (222→158) → NetworkConnectorLifecycle (91)** — lifecycle/caps
- [x] **RPCDeviceBusAdapter (219→169) → RPCMessageProcessor (71)** — message dispatch
- [x] **AbstractVirtualMachine (204→187) → VMErrorCalculator (28)** — error computation
- [x] **TerminalBufferScrolling (208→110) → TerminalLineShifter (135)** — line shift logic
- [x] **ModBlockStateProvider (225→164) → CableItemTransforms (76)** — model transforms
- [x] **BusCableBakedModel (209→176) → BusCableModelTypes (49)** — model properties
- [x] **EnergySpec (207→179) → EnergySpecValues (37)** — loadValues
- [x] **RedstoneInterfaceBlockEntity (206→176) → BundledRedstoneCallbacks (53)** — bundled callbacks
- [x] **AbstractVMItemStackHandlers (202→180) → VMItemStackHandlerSerialization (80)** — serialization

### Раскладка по подпапкам (≤4 файлов на папку) — ✅ DONE

- [x] `common/inet/` (53) → layer/, tcp/, tcp/state/, session/, session/manager/, session/echo/, session/datagram/, session/stream/, protocol/, internet/, internet/connection/, util/, util/checksum/
- [x] `common/network/message/` (43) → computer/, robot/, monitor/, projector/, disk/, network/, file/, misc/
- [x] `common/network/` root (8) → Network.java в корне, util/, loadbalancer/, info/
- [x] `common/util/` (38) → scheduler/, nbt/, item/, world/, text/, block/, sound/, async/, event/, tick/, misc/
- [x] `common/block/` (27) → computer/, cable/, monitor/, projector/, network/, disk/, keyboard/, energy/, misc/, common/, types/
- [x] `client/gui/screen/` (25) → computer/, robot/, monitor/, network/, keyboard/, common/, widget/, file/, misc/
- `common/vm/terminal/escapes/csi/` (31) — не трогать, каждый CSI handler свой файл по дизайну
- `common/blockentity/network/` (25) — уже разбито, можно докрутить позже

**Примечание**: Package-private классы, перемещённые в подпапки, стали public для cross-package доступа.

### Файлы ≤200 строк — ✅ ALL DONE (except jcodec/, api/, CSI CH1/CH6/CH2)

### Deferred
- jcodec/ — **попытаться заменить на Maven dependency org.jcodec:jcodec**
- api/ — **не трогать**

---

## 6. Terminal DynamicTexture rendering ✅ — DONE

- [x] `TerminalTextureRenderer` + `TerminalTextureBuilder` — рендерит TerminalBuffer в NativeImage/DynamicTexture (640×384)
- [x] `MonitorTextRenderer` — использует DynamicTexture при всех дистанциях (не иконку)
- [x] NEAREST filtering (не linear) — текст не прыгает
- [x] LOD: одна текстура, масштабируется перспективой на всех расстояниях
- [ ] Увеличить область на блоке с 12×7 до 14×10 или 16×9 px

---

## 7. Projector rendering improvements

- [ ] Gamma correction после YUV→RGB конвертации
- [ ] Попробовать YUV444 вместо YUV420 (менее размытые цвета)
- [ ] Увеличить depth map с 256×256 до 512×512

---

## 8. Screen↔Container auto-регистрация

**Опционально**: вместо ручного `event.register(CONTAINER.get(), Screen::new)` для каждого —
DSL через ScreenRegistry:
```java
ScreenRegistry.register(event, COMPUTER, ComputerContainerScreen::new);
ScreenRegistry.register(event, COMPUTER_TERMINAL, ComputerTerminalScreen::new);
```

---

## 9. Lint и статический анализ

- [x] **PMD: multithreading.xml** — добавлено с исключениями для NeoForge thread model
- [ ] **PMD: codestyle.xml + design.xml** — после рефакторинга
- [ ] **SpotBugs** — обновить плагин для Gradle 9+
- [ ] **Error Prone** — Google-анализатор
- [ ] **AvoidDuplicateLiterals** — кастомные пороги
- [ ] **AvoidInstantiatingObjectsInLoops** — точечно @SuppressWarnings

---

## 10. Тесты — ✅ расширены

- [x] `Ipv4SpaceTest` — расширен + `Ipv4SpaceExtendedTest` (8 новых тестов: empty, single, subnet, range, denylist, overlap, DNS, CIDR30)
- [x] `IntegerSpaceTest` — расширен + `IntegerSpaceExtendedTest` (8 новых тестов: empty, duplicate, merge, gap, range, overlap, fill)
- [x] `TcpHeaderTest` (5 тестов) — read/write round trip, connection init, accept, reject, min size
- [x] `InetUtilsTest` (6 тестов) — parse IPv4 valid/invalid, loopback, toString, ICMP body
- [x] `Rfc1071ChecksumTest` (3 тестов) — zero, known data, empty range
- [x] `ArpProtocolTest` (3 тестов) — ARP protocol, MacAddress, prefix
- [x] `TerminalBufferTest` (5 тестов) — initial state, clearLine, shiftUp, shiftDown, clearAll

---

## 11. jcodec → Maven dependency

**Опционально**: заменить встроенный `common/jcodec/` на Maven dependency `org.jcodec:jcodec`.

- [ ] Проверить что org.jcodec:jcodec API совместим с нашим usage
- [ ] Если совместим — удалить встроенный jcodec/ и добавить dependency в build.gradle.kts
- [ ] Если не совместим — оставить встроенный (не трогать)

---

## 1. C API для Redstone Interface (#89)

**Проблема**: Lua на VM медленный, нет `sleep()`, неудобно для real-time контроллеров. Хочется писать на C/Rust под RISC-V.

**Текущее состояние**: Сделана C-библиотека `librpc` в `src/main/scripts/lib/rpc/`.

- [ ] Добавить TCC в buildroot-образ
- [ ] Добавить примеры: redstone_blink.c, note_block_player.c
- [ ] Сделать C++ RAII-обёртку

---

## 2. Resizable Screen (#12) — Deferred

Сначала рефакторинг и TCC.

---

## 3. TCC (Tiny C Compiler) в образ

- [ ] Обновить minux чтобы включить TCC
- [ ] Собрать новый buildroot-образ
