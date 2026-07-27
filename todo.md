# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY — ~92%

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
- [x] fix: energyInfo.getCount() → getIntCount() (upstream)
- [x] fix: Terminal fields → transient (upstream)

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

### Осталось довести до ≤200 (текущие размеры)
- [ ] Network.java (294) — извлечь часть в NetworkMessages или NetworkRegistry
- [ ] Robot.java (269) — извлечь RobotMovement или RobotSerialization
- [ ] ComputerBlockEntity.java (259) — извлечь ComputerPersistence или ComputerCapabilities
- [ ] Terminal.java (244) — извлечь TerminalState или TerminalInput
- [ ] CH1.java (251) — escape-последовательности, *возможно не трогать*
- [ ] CH6.java (240) — escape-последовательности, *возможно не трогать*
- [ ] MonitorBlockEntity.java (240) — извлечь MonitorVideoCodec или MonitorPersistence
- [ ] TerminalBuffer.java (238) — извлечь TerminalBufferRenderer или TerminalBufferState
- [ ] BusCableInteractionHandler.java (236) — извлечь BusCablePlacementHandler

### Deferred
- jcodec/ — **попытаться заменить на Maven dependency org.jcodec:jcodec**
- api/ — **не трогать**

---

## 6. Terminal DynamicTexture rendering

**Проблема**: Терминал рисует каждый символ каждый кадр → мелкий шрифт, месиво при отходе, прыгающий текст.

**Решение**: Рендерить терминал один раз в DynamicTexture (640×384) и показывать её на блоке.

- [ ] Создать `TerminalTextureRenderer` — рендерит TerminalBuffer в NativeImage/DynamicTexture
- [ ] Обновить `MonitorRenderer` и `ComputerRenderer` — вместо посимвольного рисования использовать текстуру
- [ ] Добавить nearest filtering (не linear) чтобы текст не прыгал
- [ ] LOD: близко — полная текстура, далеко — уменьшенная, а не иконка
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

- [ ] **PMD: multithreading.xml** — гонки, volatile, UnsafeInit. Актуально для BlobChannelManager, DefaultSessionLayer, InetUtils.
- [ ] **PMD: codestyle.xml + design.xml** — после рефакторинга
- [ ] **SpotBugs** — обновить плагин для Gradle 9+
- [ ] **Error Prone** — Google-анализатор
- [ ] **AvoidDuplicateLiterals** — кастомные пороги
- [ ] **AvoidInstantiatingObjectsInLoops** — точечно @SuppressWarnings

---

## 10. Тесты

- [ ] `Ipv4SpaceTest` — расширить: тесты для computeIpSpace, allowed/denied hosts, edge cases
- [ ] `IntegerSpaceTest` — расширить: тесты для allocate, free, overlap
- [ ] Новые: `InetUtilsTest` — parseIpv4Address, ipv4AddressToString, checksum
- [ ] Новые: `TcpHeaderTest` — read/write ByteBuffer, connection initiation, acceptance
- [ ] Новые: `ArpProtocolTest` — writeResponse, readRequest
- [ ] Новые: `TerminalBufferTest` — basic write, scroll, cursor movement, colors
- [ ] Новые: `SessionManagerTest` — session lifecycle, expiration, close
- [ ] Новые: `NBTUtilsTest` — serialization round-trips
- [ ] Новые: `BusCableShapeBuilderTest` — shape combinations
- [ ] Новые: `ChunkUtilsTest` — chunk location, block location
- [ ] Новые: `NetworkMessageTest` — message serialization round-trips

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
