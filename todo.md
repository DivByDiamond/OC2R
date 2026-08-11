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
- `common/blockentity/network/` (25) → ✅ разбито: cable/ (4), cable/facade/ (4), connector/ (4), connector/interfaces/ (4), switches/ (4), switches/host/ (2), switches/port/ (3), hub/ (1), vxlan/ (2)

**Примечание**: Package-private классы, перемещённые в подпапки, стали public для cross-package доступа.

### Файлы ≤200 строк — ✅ ALL DONE (except jcodec/, api/, CSI CH1/CH6/CH2)

### Следующая волна (план) — TODO

**Файлы >200 строк:**
- [x] `entity/robot/RobotActionProcessor` (297→194) → `RobotActionProcessorSerialization` (serialize/deserialize) + lock-helper `withLock`
- [x] `blockentity/monitor/MonitorBlockEntity` (239→199) → `MonitorTickHandler` (serverTick + updateMonitorState)
- [x] `vm/AbstractVirtualMachine` (218→181) → `VirtualMachineTicker` (tick + restart-логика)

**Папки >4 файлов:**
- [x] `common/item` (25) → storage/, storage/flash/, network/, network/cable/, computer/, block/, tool/, root ≤4
- [x] `common/container` (23) → base/, computer/, robot/, monitor/, slot/, handler/, network/, data/, root ≤4
- [x] `common/entity/robot` (25) → action/, action/processor/, movement/, rotation/, state/, misc/, root ≤4
- [x] `common/bus/device/provider/item` (22) → storage/, storage/disk/, network/, energy/, module/, module/card/, root ≤4
- [x] `common/bus/device/rpc/item` (20) → module/, util/, card/, file/, file/request/, root ≤4
- [x] `client/renderer` (15) → stage/ (ColorCompositingStage, DepthBufferStage, DepthOnlyRenderTarget, RenderInfo), stage/shader/ (ModRenderType, ModShaders), projector/ (ProjectorCameraEntity, ProjectorDepthRenderer, ProjectorDepthRenderInfo), cable/ (CableRenderUtils, NetworkCableConnection, NetworkCablePoint, NetworkCableRenderer), root (BusInterfaceNameRenderer, MonitorGUIRenderer, package-info)
- [x] `client/renderer/blockentity` (13) → computer/, monitor/, projector/, charger/, network/, root (OverlayRenderer, RendererQuadHelper, package-info)
- [x] `data` (15) → recipe/, recipe/peripheral/, model/, tag/, loot/, root (DataGenerators, package-info)
- [x] `common/vm` (15) → runner/, handler/, memory/, misc/, root ≤4 (VirtualMachine, VMRunner, VMErrorCalculator, VMRunState, package-info)
- [x] `common/bus/device/vm/item` (14) → storage/, storage/misc/, network/, misc/, root ≤4
- [x] `common/network/message/robot` (11) → terminal/, inventory/, state/, root ≤4
- [x] `common/blockentity/misc` (11) → gateway/, redstone/, redstone/state/, flash/, misc/ (PciCardCageBlockEntity)
- [x] `common/serialization/gson` (10) → rpc/, rpc/invocation/, root ≤4
- [x] `common/bus/adapter` (10) → rpc/, rpc/method/, root (RPCDeviceBusAdapter, DeviceRegistry, EmptyMethodGroup, RPCDeviceWithIdentifier)
- [x] `common/vm/context/global` (9) → memory/, interrupt/, root (GlobalVMContext, GlobalEventBus, package-info)
- [x] `common/bus/element` (9) → group/, group/query/, root (Abstract*BusElement)
- [x] `common/bus/device/vm/block` (9) → disk/, flash/, misc/, root (KeyboardDevice, MonitorDevice, package-info)
- [x] `common/bus/device/util` (9) → info/, optional/, root (Devices, IdentityProxy, package-info)
- [x] `common/bus/device/rpc` (9) → filter/, adapter/, root ≤4
- [x] `common/bus/device/data` (9) → firmware/, block/, root ≤4
- [x] `common/blockentity/projector` (9) → video/, misc/, root (ProjectorBlockEntity, ProjectorCapabilities, ProjectorState)
- [x] `common/blockentity/monitor` (10) → video/, misc/, root (MonitorBlockEntity, MonitorStateManager, MonitorTickHandler)
- [x] `common/network/message/computer` (10) → terminal/, misc/ (FirmwareFlasherMessage), root ≤4
- [x] `client/gui/widget` (9) → terminal/, misc/, root (Sprite, Texture, package-info)
- [x] `common/network/message/monitor` (8) → input/, framebuffer/, root (MonitorStateMessage, package-info)

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
- [x] **@SuppressWarnings("deprecation") удалены** — заменены на новые API:
  - CustomItemColors → `RegisterColorHandlersEvent.Item`
  - BusCableBakedModel → `getParticleIcon(ModelData)` (остался узкий suppression на абстрактном deprecated `getParticleIcon()`)
  - ProjectorBlockEntity/PciCardCageBlockEntity → override `setBlockState` убран (renderBounds лениво, пустой override удалён)
  - ChargerBlock → `Mirror.mirror(Direction)` вместо `state.rotate(Rotation)`
  - InternetCardSpec → `defineList(..., String::new, ...)`
  - RobotSlot → `stack.canFitInsideContainerItems()`, RobotItem → `canFitInsideContainerItems(ItemStack)`
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

## 12. Мультимонитор: фрагментная модель (как в OpenComputers)

**Цель**: заменить OBJ-модель монитора на фрагментные JSON-модели + кастомную BakedModel по позиции блока в мультиблоке. Референс: `ref/` (CaitlynMainer/OpenComputers), файл `ScreenModel.scala`, текстуры `textures/block/screen/`.

- [ ] Скопировать 48 фрагментных текстур (f*/b*) из ref в `assets/oc2r/textures/block/monitor/`, оставить overlay
- [ ] Config: `monitorMaxWidth`/`monitorMaxHeight` (по умолчанию 5), читать в `MonitorMultiblock` вместо захардкоженных MAX
- [ ] ModelData: `MonitorBlockEntity` отдаёт width/height/offset/facing через ModelProperty
- [ ] `MonitorBakedModel` (аналог `ScreenModel`): выбор фрагмента рамки по позиции блока
- [ ] Заменить JSON blockstate/models: фрагментная модель вместо monotonic OBJ
- [ ] Удалить `monitor.obj`/`monitor.mtl`, синхронизировать item-модель
- [ ] Build + проверка в игре мультимонитора разных размеров

---

## 13. Переписывание проводов (энергия FE+EU + починка коннекции)

**Решение**: кабель становится энергопроводником с универсальной энергией. **FE/RF** — через `IEnergyStorage` (родной), **EU (IC2)** — через EnergyNet adapter. Передача с буфером и лимитом на тик из конфига. Коннекция чинится.

- [ ] Config: `cableEnergyCapacity` + `cableEnergyPerTick` (буфер и лимит передачи)
- [ ] Создать `CableEnergyStorage` (FixedEnergyStorage-подобный буфер) + зарегистрировать `Capabilities.EnergyStorage.BLOCK` в `BusCableCapabilities`
- [ ] `EnergyTransferManager`: тик кабеля, BFS по сети, распределение источник→буфер→потребителя с лимитом на тик
- [ ] **EU (IC2) через adapter**: мост на EnergyNet (опциональная зависимость, без жёсткой привязки)
- [ ] Автоконнект: кабель соединяется с соседями с `IEnergyStorage` (энергия) и `DeviceBusElement` (bus), без ручного INTERFACE для энергии
- [ ] Реконфигурация: единая `recomputeConnections()` вместо размазанного `updateShape`/`canHaveCableTo`
- [ ] Build + тесты + проверка в игре (gameTestServer)

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

---

## 14. Переписывание корпуса ПК (Block)

**Механика корпуса ПК (Block):**

**Лицевая сторона** — сторона, куда блок смотрит при установке (направление блока, как у печки/рабочего стола).

- [ ] **ПКМ по любой стороне, кроме лицевой** — открываем GUI инвентаря компонентов (слоты для GPU, CPU, RAM и т.д.)
- [ ] **ПКМ строго по лицевой стороне** — включаем ПК
- [ ] **Обработка ошибок**: если ПК не может включиться (нет нужных компонентов, нет энергии и т.д.), вместо включения воспроизводится 1 звук из набора POST-кодов ошибок

---

## 15. Клавиатура/мышь и ввод (переделка)

**Механика клавиатуры/мыши и ввода:**

- [ ] **Подключение к ПК/монитору**: Shift+ПКМ по блоку клавиатуры/мыши (блок подсвечивается), затем ПКМ по монитору или ПК → «успешно слиньковано». Связь держится, пока не сломать монитор или клавиатуру/мышь
- [ ] **Захват управления**: при ПКМ по блоку клавиатуры/мыши происходит захват управления (захват курсора)
- [ ] **Логика ввода**: как только управление захвачено, игрок наводит прицел (рейкаст) на нужный монитор, и фокус ввода переходит на него. Все нажатия клавиш и клики мыши перенаправляются прямо в ПК (механика как в Tom's Peripherals)
- [ ] **ESC** — единственная кнопка, которую клавиатура/мышь не захватывает: выходит из режима ввода

---

## 16. UART-планшет (Item)

**Механика UART-планшета (Item):**

- [ ] **ПКМ планшетом по блоку ПК**: открывает GUI компонентов, но в правом верхнем углу мини-игра подключения: 3 пина (RX, TX, GND) и 3 вытягивающихся провода. Пользователь должен соединить провода с пинами (аналог задачи с проводами в Among Us). Как только провода соединены правильно, GUI закрывается — связь установлена
- [ ] **ПКМ планшетом по воздуху**: открывает GUI терминала планшета, который дублирует то, что в данный момент выводит UART подключенного ПК

---

## 17. Автоматическая перезагрузка VM при изменении состава устройств ✅ — DONE

**Проблема**: подключение/отключение блочных устройств (монитор, проектор, клавиатура) к *работающему* компу игнорировалось гостем — устройства появлялись только после ручного рестарта.

**Решение (вариант A) — мягкий рестарт через существующий путь `stop(); start();`:**

- [x] `AbstractVirtualMachine`: добавлены `AtomicBoolean devicesChangedWhileRunning` + `deviceChangeRestartDelay` (тесно связанный с `tick()`); метод `markDevicesChanged()`
- [x] `AbstractVirtualMachine.tick()`: если флаг выставлен и `runState == RUNNING` и `state.board.isRunning()` — отложенный `stop(); start();` с задержкой 2 тика (пакетирование серийных подключений в один рестарт)
- [x] `VMLifecycle.handleDevicesAdded/Removed`: только `addDevices/removeDevices` + `vm.markDevicesChanged()` (deferred mount)
- [x] `VMLifecycle.handleBeforeDeviceScan`: убран переход `RUNNING → LOADING_DEVICES` (источник гонок горячего монтирования)
- [x] RPC-устройства продолжают хот-плажиться живым адаптером и до рестарта не доводят
- [x] Импорт `VMRunState` в `VMLifecycle` оставлен (используется в `load()`/`stopRunnerAndReset()`)
- [x] Проверка: `./gradlew compileJava` BUILD SUCCESSFUL, checkstyle/PMD без новых нарушений

---

## 18. Убрать jcodec → прямой RGB-буфер монитора/проектора

**Проблема**: каждый кадр монитора проходит `RGB→YUV420 → H.264 (jcodec) → Deflate → сеть → Inflate → H.264 decode → YUV→RGB` на клиенте. Два thread pool (encoder + decoder), byte budget с дропом кадров (SkipCount, круговой список) — всё ради сжатия, которое для LAN избыточно.

**Целевое решение**: кадр = уже готовый RGB-буфер, передаётся как есть, клиент рендерит через DynamicTexture (NEAREST). Никакого jcodec, дефлейта, YUV.

- [ ] **Протокол**: `MonitorFramebufferMessage` → `(pos, int width, int height, ByteBuffer rgb)` — raw RGB24. Размер кадра 640×400×3 = 768 KB
- [ ] **Трафик**: 768 KB × 20 fps ≈ 15 MB/s — ок для локальной игры/LAN; для интернета — опционально понизить fps (10) или размер
- [ ] **Сервер**: убрать `MonitorVideoController`/`ProjectorVideoController` encode-часть; отправка только если `device.hasChanges()` (diff), throttle 20 fps
- [ ] **Клиент**: `MonitorFrameMessage.handleMessage()` → прямо в `NativeImage`/`DynamicTexture.upload()` (NEAREST), без decoder thread pool
- [ ] **Удалить**: `jcodec/`, H264Encoder/H264Decoder, YUV420 conversion, `MonitorLoadBalancer`, `ProjectorLoadBalancer` (byte budget, skipCount, circular list), encoder/decoder worker pools
- [ ] **Сохранить**: кеш последнего кадра на клиенте (пока активных кадров нет — показывать старый); упростить серверную отдачу, убрать byte budget/дроп кадров
- [ ] Build + проверка в игре (несколько мониторов + проектор одновременно)

---

## 19. Terminal: diff вместо сырого UART на клиенте

**Проблема**: сервер гонит на клиент *сырой байтовый поток* UART (эскейп-последовательности), и клиента парсит VT100 заново. Term-инстанс общий на сервер+клиент; при перезагрузке VM не очищается; каждый тик шлются байты.

**Целевое решение — разделение сервер/клиент + отправка diff:**

- [ ] **Сервер (единственный владелец состояния)**: `Terminal`/`TerminalBuffer` + парсинг VT-escape (как сейчас в `TerminalOutput`), но на сервере
- [ ] **Событие `TerminalChanged`**: при изменении строк помечается список dirty-строк
- [ ] **`TerminalDiffMessage`** (`pos, int[] dirtyLines, byte[][] lineData, int cursorLine, int cursorCol`) — шлётся только при изменении
- [ ] **Клиент**: хранит локальную `TerminalBuffer`-копию, применяет diff, рендерит dirty-строки (без повторного VT-парсинга)
- [ ] **Сброс терминала**: при перезагрузке VM сервер шлёт «clear» + полный снапшот
- [ ] **Рендеринг**: `TerminalRenderer` переиспользует VBO, rebuild только dirty-строк (отдельная под-задача)
- [ ] Убрать отправку сырого UART-потока клиенту (`ComputerTerminalOutputMessage`) и клиентский `TerminalOutput`

---

## 20. Push-based шина вместо polling BFS-сканирования

### Что сейчас (polling)

Топология шины перестраивается **полным BFS от корня при каждом сканировании**:

```
ComputerBlockEntity.serverTick() → virtualMachine.tick()
  → busController.scan()                        ← КАЖДЫЙ TICK
    → BusElementManager.scan()  полный BFS по getNeighbors()
    → state = READY / INCOMPLETE / TOO_COMPLEX / MULTIPLE_CONTROLLERS
    → full rebuild списка устройств → scanDevices() → diff
```

Проблемы:
- Полная перестройка графа на каждый тик (O(N) × каждый тик), даже без изменений.
- Повторы через таймауты: `INCOMPLETE` retry 10 s, `TOO_COMPLEX` при >128 элементов, retry 5 s.
- VM получает события `devicesAdded/Removed` только когда scan завершился корректно; при сбоях — бесконечные повторы.
- Лишнее событие `beforeDeviceScan` (мы уже убрали из него hot-mount, но сам скан остался).
- 128-элементный лимит — искусственный, упирается в алгоритм, а не в реальные ограничения Minecraft.

### Целевое решение — push-based (событийная) топология

Суть: **никакого периодического сканирования.** Граф перечитывается только по факту изменения мира. Каждый узел сам сообщает сети о себе:

```
DeviceBusElement (каждый блок/элемент)
   onRegister()    → «я подключён», добавить себя в сеть (RootController)
   onUnregister()  → «я отключён», убрать себя из графа
   onNeighborChanged(dir)  → «мой сосед изменился» (block change / chunk load)
```

**Как это будет жить в коде:**

1. **`BusTopology`** — граф (`Map<Node, Set<Node>>` рёбра) вместо `BusElementManager.scan()`:
   - инкрементальное добавление/удаление ребра — не полная перестройка
   - `onConnected`/`onDisconnected` события поднимаются точечно (не через полный diff)
2. **Источники событий**:
   - `onBlockPlacedBy` / `onBlockRemoved` / `onNeighborChanged` существующих блоков
   - `ChunkLoadEvent` / `ChunkUnloadedEvent` (устройство появляется/пропадает — BFS не нужен)
   - слоты (`AbstractItemDeviceBusElement.handleSlotContentsChanged`) уже триггерятся напрямую — сохранить
3. **Убрать `scan()` из `tick()`** — вместо него: `updateElements()` только по dirty-флагу («bus dirty» выставляется при любом событии топологии)
4. **`BusState` почти не нужен**: вместо `READY/INCOMPLETE/TOO_COMPLEX` — «OK» либо «соседний чанк не загружен» (ждать событие загрузки, а не retry-loop)
5. **Конфликт нескольких контроллеров** на одной шине — детектировать на топологии (union-find или явная разметка рёбер), без «MULTIPLE_CONTROLLERS retry 5 s»
6. **`beforeDeviceScan` полностью исчезает**: устройство появляется одним событием → `devicesAdded` → мягкий рестарт VM (вариант A, задача 17)

**Риски и ограничения:**
- Точность событий Minecraft: соседний чанк может быть не загружен — сосед временно отсутствует, появится по `ChunkLoadEvent`
- Выгрузка чанков → `onUnregister` при `BlockEntity.remove` / chunk unload, иначе «призрачные» узлы
- Unit-тест топологии: добавить/удалить узел → граф корректно растёт/сжимается

**Этапы:**
- [ ] Исследовать: все места, где `scan()` вызывается (`tick`, `refreshDevices`, chunk events)
- [ ] Создать `DeviceTopology` (инкрементальный граф) + события подключения
- [ ] Драйвер-события от блоков (place/remove/neighbor/chunk)
- [ ] Убрать `scan()` из `tick()`; свой dirty-флаг.
- [ ] Мигрировать контроллеры и клиентские части (проектор, кабельные панели)
- [ ] Unit-тест: `DeviceTopologyTest` — merge/split subtree, chunk load/unload

---

## 21. Звук: тональный генератор, PCM-стриминг, Speaker-блок

**Проблема**: звуковая карта (`SoundCardItemDevice`) играет только ванильные `SoundEvent` по имени, с кулдауном 2 с — нельзя играть мелодии. Нет синтеза, нет PCM-стриминга, нет отдельного блока-динамика.

- [ ] **Тональный генератор** (как OC1 speaker): RPC-метод `beep(frequency, duration)` — клиентский синтез синуса/квадрата без `.ogg`-файлов. Один `SoundEvent`-заглушка + кастомный `SoundInstance` с генерацией PCM в буфер (`AudioStream`/`Source`). Параметры: частота 20–20000 Гц, длительность мс
- [ ] **PCM-стриминг** (как Data Card звук в OC1): буфер сэмплов из VM → ванильный sound system через `Source`. Сэмплы гонятся через RPC/serial, клиент проигрывает как `AudioStream`. Сложнее, но даёт реальное аудио из VM
- [ ] **Убрать кулдаун 2 с** (или сделать настраиваемым в конфиге `soundCardCooldownMs`) — сейчас нельзя играть мелодии
- [ ] **Предмет «Speaker»/динамик** как отдельный блок: сейчас карта играет звук из позиции компьютера. Speaker-блок = отдельный BlockEntity, ставится рядом, подключается к bus, звук исходит из позиции блока-динамика
- [ ] Build + проверка в игре (мелодия из Lua/Python через beep)

---

## 22. Диски: cleanup, тиры, 3D-модели, мёртвый код

**Проблемы**: orphaned blob-файлы, мёртвый `HardDriveWithExternalDataItem`, размеры HDD через единый множитель, 2D-иконка дискеты, flash 12 MB захардкожен.

- [x] **Orphaned blobs cleanup**: при `/clear` предмета blob-файл НЕ удаляется (только `MemoryDevice.dispose` вызывает `deleteAsync`). Нужен cleanup-механизм — например, реестр активных blob-handle'ов + периодическая проверка orphaned при старте сервера → `BlobStorage.ACTIVE_HANDLES` (регистрируются при `validateHandle`), `cleanupOrphaned()` по `ServerStartedEvent` через `ServerScheduler` с задержкой 5 с (устройства успевают смонтироваться)
- [ ] **`HardDriveWithExternalDataItem`** — класс определён, провайдер есть, но предмет **не зарегистрирован** в `Items.java`. Мёртвый код — либо добить (зарегистрировать + модель + рецепт), либо удалить класс + провайдер + `HardDriveDeviceWithInitialData` → **зарегистрирован как `HARD_DRIVE_ONYXOS` (OnyxOS liquid-диск, рабочее дерево)**
- [x] **Размеры HDD по тирам отдельно**: замена `diskSizeFactor` на `diskSizeTier1/2/3/4` (8/16/32/128 MB) в `VMSpec`/`Config`, `Items.java` читает тиры
- [x] **Новые тиры HDD**: **8 / 16 / 32 / 128 MB** (было 2/4/8/16)
- [ ] **3D-модель дискеты**: сейчас в слоте дисковода рисуется 2D-иконка (`FIXED` display context). Добавить нормальную 3D-модель floppy для рендера в `DiskDriveRenderer`
- [x] **Flash memory — тиры**: `flashMemorySizeTier1/2/3` (4/8/16 MB) в конфиге; новые предметы `flash_memory_small`/`flash_memory_medium`, существующий `flash_memory` = 16 MB; `ByteBufferFlashStorageDevice` уже работал на `size` из предмета
- [ ] Build + проверка в игре (hot-swap, сохранение данных, cleanup)

---

## 23. GPU — видеокарта как предмет (Tier 1/2/3/4)

**Проблема**: GPU отсутствует полностью. Монитор сам предоставляет `SimpleFramebufferDevice` 640×480 RGB565 — разрешение захардкожено, тиров нет. Без ощущения «собираешь компьютер из компонентов».

**Решение**: добавить предмет GPU, который **управляет разрешением** framebuffer. `SimpleFramebufferDevice` уже поддерживает произвольные `WIDTH/HEIGHT` — параметризовать из GPU-предмета при mount. **GPU — обязательное условие для монитора** (без GPU → чёрный экран, только UART-терминал).

- [ ] **Тиры GPU и разрешения:**

| Тир | Разрешение | Текстовый режим | Описание |
|---|---|---|---|
| GPU T1 | 320×200 | 80×25 | Базовый, крафт из железа/редстоуна |
| GPU T2 | 640×400 | 160×50 | Средний, золото/лазурит |
| GPU T3 | 1024×768 | 256×96 | Продвинутый, алмазы |
| GPU T4 | 1920×1080 | 320×135 | Эндгейм, незерит/эмеральды |

- [x] **Предмет GPU**: `GPUItem` (width/height/tier) + регистрация в `Items.java` (4 тира), тег `devices/gpu`, тег `device_needs_reboot`, слот GPU в компьютере (`GPU_SLOTS = 1`, `DeviceType GPU`), крафты, модели, lang
- [x] **GPU-устройство**: `GPUDevice` (VMDevice) — хранит `width`/`height`/`tier` из предмета, mount без MMIO
- [x] **Провайдер**: `GPUItemDeviceProvider` — создаёт `GPUDevice` из предмета, энергопотребление по тиру (`gpuEnergyPerTickTier1..4` = 2/3/5/8)
- [ ] **Интеграция с монитором**: `MonitorDevice` должен использовать разрешение из GPU и не монтировать framebuffer без GPU — отложено, видеопайплайн жёстко захардкожен на 640×480 (пересекается с задачей 18 «убрать jcodec»; `SimpleFramebufferDevice`/провайдер уже размерно-независимы)
- [ ] **Интеграция с монитором**: `MonitorDevice` спрашивает у bus-контроллера есть ли GPU → если нет, framebuffer не монтируется (чёрный экран). Если есть — `SimpleFramebufferDevice(width, height)` из GPU
- [ ] **Device-tree**: `SimpleFramebufferDeviceProvider` — `width`/`height`/`stride` из GPU, а не захардкоженные 640×480
- [ ] **Без GPU → UART-терминал**: монитор не показывает framebuffer, но текстовый терминал (UART) работает
- [ ] Конфиг: `gpuEnergyPerTickTier1..4` в `EnergySpec`
- [ ] Build + проверка в игре (монитор с GPU T1/T2/T3/T4, без GPU — чёрный)

---

## 24. CPU: конфиг частот, новые тиры, губернаторы

**Проблема**: частоты захардкожены в `Items.java` (25/50/100/200/1000 MHz), конфиг не реализован (`Config.java:12` — TODO). `timeQuota` 25 ms захардкожен. На T_INF cycleLimit копится бесконечно.

- [ ] **Новые тиры CPU** (минимальный не 25, а 50 MHz):

| Тир | Частота | Описание |
|---|---|---|
| CPU T1 | 50 MHz | Базовый (было 25) |
| CPU T2 | 100 MHz | Средний (было 50) |
| CPU T3 | 200 MHz | Продвинутый (было 100) |
| CPU T4 | 400 MHz | Эндгейм (было 200) |
| CPU T_INF | 1000 MHz | Creative (без изменений) |

- [ ] **Конфиг частот**: реализовать `cpuFrequencyTier1/2/3/4` в `GameplaySpec` (убрать TODO из `Config.java:12`). `Items.java` читает из конфига, а не захардкоженные константы
- [ ] **`timeQuota` в конфиг**: вынести `TIMESLICE_IN_MS = 25` из `VMRunner.java` в `Config.vmTimeQuotaMs`. На слабых серверах — снизить, на мощных — поднять
- [ ] **Cap на накопление `cycleLimit`**: сейчас `cycleLimit` растёт без ограничений → на T_INF копится «долг» циклов. Добавить cap: `cycleLimit = min(cycleLimit + getCyclesPerTick(), 2 × getCyclesPerTick())` — не больше 2 тиков вперёд
- [ ] Обновить крафты `recipe/cpu_tier_1..4.json` под новые частоты
- [ ] Build + проверка в игре (разные CPU, баланс энергии/производительности)

---

## 25. Flash Builder: GUI + манифест репозитория + свой образ

**Вердикт**: вариант A (цельный образ) + allowlist GitHub + `flashMemorySize = 15 MB` в конфиге.

**Проблема**: сейчас записать свою прошивку можно только через гостевую ОС и `flash.sh` (нет GUI, нет загрузки с URL). Захардкожен 12 MB в 5 местах.

### Мод (oc2r)

- [ ] **`Config.flashMemorySize = 15`** (в MB) в `StorageSpec`/`Config` — читается везде вместо захардкоженных 12 MB: `Items.java` (flash_memory), `ByteBufferFlashStorageDevice` (`claimMemory`/`allocate`), `FlashMemoryFlasherDevice`, `MinuxFirmware`, `src/main/scripts/bin/flash.sh`
- [ ] **GUI флешера** (`flash_memory_flasher`): контейнер + экран (сейчас GUI нет вовсе — только физическая вставка/выброс). Поле «URL репозитория» + кнопка «Записать»
- [ ] **`FirmwareManifest`** (парсер манифеста): читает `oc2r-firmware.json` → `{name, version, layout, image}`
- [ ] **`FirmwareDownloader`**: скачивает манифест и `.img` через `java.net.http.HttpClient` (асинхронно, на worker-пуле), **allowlist**: только `github.com`, `raw.githubusercontent.com`, `objects.githubusercontent.com` (в конфиге список хостов)
- [ ] **Сборка flash-образа**: по `layout` = `minux` — OpenSBI (`fw_jump.bin` из jar) на offset 0 + образ на offset 2 MB, хвост — нули до `flashMemorySize`; `layout` = `raw` — образ как есть. Пишется в blob (`BlobStorage`), handle → NBT предмета
- [ ] **Обработка ошибок**: сеть недоступна / манифест невалидный / размер > flashMemorySize → сообщение в GUI, флешка не портится
- [ ] **Пример `oc2r-firmware.json`** — положить в `docs/` как шаблон для комьюнити
- [ ] Build + проверка в игре (GUI флешера, запись OnyxOS-образа, загрузка компа с него)

### Репозитории OnyxOS (пример для людей)

- [ ] `.github/workflows/release.yml` — на push тега: build boot (riscv-gcc) + kernel (cargo) + shell; склейка `onyx-flash.img` (OpenSBI → 0, kernel → 2 MB, нули до 15 MB); attach к GitHub Release
- [ ] `oc2r-firmware.json` в корне:
```json
{
  "name": "OnyxOS",
  "version": "0.3.0",
  "layout": "minux",
  "image": "https://github.com/loki5512344/OnyxOS/releases/latest/download/onyx-flash.img"
}
```
- [ ] README-раздел «Как сделать свою прошивку» (манифест + workflow как пример)

---

## 26. OnyxOS в OC2R: S-mode boot + OnyxFS диск + сеть

**Цель**: запустить OnyxKernel (github.com/loki5512344/OnyxKernel) внутри VM OC2R как альтернативу Minux.

**Проверено (факты):**
- `linker.ld`: `KERNEL_BASE = 0x80200000` — совпадает с адресом загрузки ядра в `MinuxFirmware` (`startAddress + 0x200000`). Схема `layout: minux` подходит без изменений.
- **Блокер**: `boot.S` рассчитан на вход в M-mode (`csrr mhartid`, `pmpaddr0/pmpcfg0`, `medeleg/mideleg`, `mstatus`, `mret`) — это путь OnyxBoot. В sedna ядро входит в S-mode через OpenSBI с `a0`=hartid, `a1`=DTB → первый же `csrr mhartid` = illegal instruction.
- После boot: `kmain` монтирует **OnyxFS** и грузит `/bin/init`; встроенные rootfs OC2R (cramfs/squashfs) он не читает. Сеть захардкожена `[10,0,2,15]` (QEMU user-net).

### ОнyxKernel (репо)
- [ ] **`boot_smode.rs`** (cfg-гейт, как `boot_32.rs`): вход из OpenSBI в S-mode — принять `a0`/`a1`, пропустить PMP/medeleg/mideleg (OpenSBI уже сделал), `stvec`/`sepc`/`sstatus.SPIE`, `sret` → `kmain`. ~50 строк asm
- [ ] **Сеть**: убрать хардкод `[10,0,2,15]`; DHCP или адрес из FDT/конфига
- [ ] Проверить: UART NS16550A (совместим с sedna), virtio_net, virtio-blk, libfdt — что FDT от sedna парсится `early_init`

### Мод (oc2r) — доставка OnyxOS-образа
- [ ] **OnyxFS-диск**: образ rootfs (собранный `mkimage` из `tools/` OnyxKernel, содержит `/bin/init`, `/bin/osh`, userland) подаётся как виртуальный HDD/floppy через существующий blob-механизм, а НЕ как встроенный rootfs
- [ ] `layout: minux` в `FirmwareManifest`/`FirmwareDownloader` (задача 25) уже раскладывает kernel на 0x80200000 — проверить на реальном Onyx-образе
- [ ] Build + проверка в игре: компьютер с флешкой-OnyxOS грузится до `login:` с OnyxKernel

### Проверить дополнительно (открытые вопросы)
- [ ] Память: сколько RAM нужно OnyxOS (256 MB в QEMU) vs `maxAllocatedMemory` OC2R (512 MB default) — влезет ли
- [ ] InterruptController sedna: OnyxKernel использует PLIC + CLINT — есть ли в sedna, как прокидывается в FDT
- [ ] VirtIO-нумерация `/dev/vda|vdb|vdc` (sedna: vda=bootfs, vdb=rootfs, vdc=первый HDD) — где окажется OnyxFS-диск, не конфликтует ли
- [ ] **GPU/framebuffer**: OnyxKernel рисует PSF-шрифты в framebuffer; до задачи 23 (GPU) монитор даёт 640×480 — проверить формат `r5g6b5`

---

## 27. Аудит подсистем мода (сам мод, не ядро)

**Цель**: систематически посмотреть оставшиеся подсистемы OC2R — что работает, что кривое, что чинить. Заодно собрать находки для задач 12–24.

- [ ] **Сеть**: `NetworkSwitch`, `NetworkConnector`, `InternetCard`, `VXLAN Hub`, TCP/IP-стек (`common/inet/`) — как реально ходят пакеты, есть ли косяки. Стабильность мультиплеера
- [x] **Робот**: `RobotEntity`, `RobotMovementController`, инвентарь — насколько «живой» vs задачи V2 (большая фича) → см. задачу 28
- [ ] **PCI Card Cage**: `PciCardCageDevice` (16MB window) — как расширяет слоты, работает ли
- [ ] **Redstone Interface**: `RedstoneInterfaceBlockEntity`, `BundledRedstoneCallbacks` — фронты, слабый сигнал
- [ ] **Энергия в блоке**: `FixedEnergyStorage`, зарядник, `consumeEnergy` — перекликается с задачей 13
- [ ] **GUI/контейнеры**: `ComputerInventoryContainer`, `AbstractMachineInventoryScreen` — как синкаются слоты, баги
- [x] **Синхронизация мира**: `BusCableFacadeMessage`, network sync, ChunkData — мультиплеер → см. задачу 29
- [ ] **`inet/` TCP/IP-стек**: `StreamSessionImpl`, `SessionManager`, retransmission — качество реализации

---

## 28. Фиксы робота

**Итог аудита**: робот — полностью реализованная рабочая фича (порт OC2), но есть гонки потоков, утечки и баги с предметами.

- [ ] **Гонка потоков в `RobotActionProcessor`**: `addAction()` вызывается с VM-потока (`@Callback(synchronize=false)` в `RobotDevice`), а `tick()` на серверном потоке читает `ArrayDeque` без блокировки (`queue.poll()`, поле `action` не volatile). Потенциальная порча очереди. → обернуть очередь в lock (как уже сделано для `results`) или `ConcurrentLinkedDeque`
- [ ] **`RobotBlockCollider.collideWithWorld()` ломает блоки каждый тик** без проверки «двигается ли робот» — застрявший/пересекающий блок робот непрерывно «пережёвывает» террейн, в т.ч. при отскоке. → ломать только если есть активное движение в направлении
- [ ] **Утечка `RobotEventHandler`**: `register()` на первом тике сервера, `unregister()` только при unload чанка/мира. При `discard()`/поднятии робота предметом листенеры не отписываются. → отписать в `Robot.remove()`
- [ ] **`BlockOperationsModuleDevice.place()` не списывает предмет**: `itemStack.copy()` передаётся в `BlockPlaceContext`; если блок ставится без `consumesAction()` — предмет из инвентаря не извлекается. → списывать по факту `place`-результата
- [ ] **`exportToItemStack` теряет состояние VM/терминала**: при поднятии робота предметом сохраняются только предметы+энергия, рабочая программа/память теряются (асимметрия с `save()` сущности). → решить: либо документировать, либо сериализовать VM-состояние в предмет
- [ ] Робот — `Entity`, не `LivingEntity`: нет HP/урона/гравитации (`setNoGravity(true)` — выбил блок под ним, висит в воздухе). → либо осознанный дизайн, либо гравитация+HP (задача V2)
- [ ] Движение разрешено только при запущенной VM (`addAction` проверяет `isRunning()`) — «ручное» управление без ОС невозможно
- [ ] Item-рендер статичен: `RobotWithoutLevelRenderer` не вызывает анимацию в руке

---

## 29. Фиксы синхронизации мира

**Итог аудита**: 3 реальных бага (один ломает мультиплеер) + многочисленное дублирование синхронизации.

### Баги (критично)
- [x] **`MultipartMessage` — баг ключа кэша** (`MultipartMessage.java:133,144,149`): при сборке на сервере используется статическое `lastAssignedMultipartMessageId` вместо поля записи `multipartMessageId` → на выделенном сервере **все импорты файлов всех клиентов пишутся в один буфер (ключ 0)** и перемешиваются. В одиночной игре работает случайно. → исправлено в `d59ab0a` (field `multipartMessageId` + key `(connection, multipartMessageId)`); S→C добавлен `sendToClient` + `ExportedFileMessage` зарегистрирован multipart
- [x] **`ServerCanceledImportFileMessage` — cast на неправильной стороне** (`:33`): зарегистрирован `playToClient`, но хендлер делает `(ServerPlayer) context.player()` на клиенте → ClassCastException при каждом импорте. → исправлено в `d59ab0a` (обработка через `Minecraft.getInstance()` на клиенте)
- [x] **`MonitorStateManager` save/load перепутаны** (`:54-67`): `savePersistent` пишет `isPowered` под ключом `projecting`, а `loadPersistent` читает `hasEnergy` из `has_energy` (никогда не пишется) → `hasEnergy` сбрасывается после перезагрузки мира; `isMounted` не сохраняется вовсе → `hasEnergy`/`isPowered` консистентно пишутся и читаются; `isMounted` — runtime-состояние, персистить не нужно
- [x] **`InternetGateWayBlockEntity.notifyPlayers`**: только `sendBlockUpdated(2)` без BE-данных → `inbound/outboundCount` (анимация) не обновляются на клиенте живьём → добавлена рассылка `ClientboundBlockEntityDataPacket.create(this)` игрокам, трекающим чанк
- [x] **`ExportedFileMessage`** шлётся одним payload размером до `1MB-1` — на грани лимита NeoForge (1МБ) → большой экспорт может тихо падать; нужен multipart и для S→C → `MultipartMessage.sendToClient` + регистрация `ExportedFileMessage`

### Дублирование (байты на проводе)
- [ ] Фасад синкается трижды: update-tag + `sendBlockUpdated(UPDATE_ALL)` + `BusCableFacadeMessage`
- [ ] Соединения коннекторов — дважды: update-tag + `NetworkConnectorConnectionsMessage`
- [ ] Имена интерфейсов — дважды: update-tag + `BusInterfaceNameMessage`
- [ ] Флоппи/флеш — дважды: update-tag + `DiskDriveFloppyMessage`/`FirmwareFlasherMessage`
- [ ] Состояние монитора/проектора — дважды: `setBlock(LIT)` + `*StateMessage`
- [ ] **`sendToClientsTrackingChunk` VS2-fallback** рассылает всем игрокам во всех измерениях (комментарий про «wrong dimension» не соответствует коду) → фильтровать по измерению
- [ ] `ComputerTerminalManager.chunk` может быть null до первого `serverTick` → сообщение теряется (компенсируется update-tag'ом)
- [ ] `BusCableBlockEntity.handleUpdateTag` не зовёт `requestModelDataUpdate()` → модель не обновится для игрока, начавшего tracking
