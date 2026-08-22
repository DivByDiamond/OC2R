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
- [x] `common/vm/terminal` (16) → buffer/, buffer/utf8/, render/, render/overlay/, color/, root (Terminal, TerminalIO, TerminalOutput, TerminalClient)
- [x] `common/vm/terminal/escapes` (8) → index/ (IND, NEL, RI, RIS), root (EscapeUtilities, DECSC, DECRC, HTS); apc/dcs/osc/csi не тронуты
- [x] `common/vm/terminal/modes` (6) → impl/ (ImplementedPrivateModes, MouseMode), root (Mode, ModeState, PrivateMode, PrivateModeState)
- [x] `common/vm/context/managed` (8) → memory/, interrupt/, root (ManagedVMContext, ManagedEventBus, package-info)
- [x] `common/serialization` корень (6) → nbt/util/, root (BlobStorage, BlobChannelManager, BlobStorageEvents, package-info)
- [x] `common/serialization/nbt` (5) → reference/, root ≤4
- [x] `common/serialization/ceres` (8) → color/, json/, memory/, text/, root (Serializers, package-info)
- [x] `common/config/common` (8) → energy/, network/, root ≤4
- [x] `common/inet/layer` (8) → impl/, link/, root (SendHandler, LayerParametersImpl, NullLayer, package-info)
- [x] `common/inet/tcp/state` (6) → finish/, root (AcceptState, ConnectState, EstablishedState, package-info)
- [x] `common/inet/session/manager` (5) → ready/, root ≤4
- [x] `common/network/message/network` (6) → connector/, root ≤4
- [x] `common/network/message/file` (6) → cancel/, root ≤4
- [x] `common/bus/controller` (6) → event/, root ≤4
- [x] `common/bus/device/rpc/block` (6) → adapter/, root ≤4
- [x] `common/block/computer` (6) → interaction/, factory/, root (ComputerBlock, ComputerBlockShapes, package-info)
- [x] `common/block/cable` (6) → interaction/, item/, shape/, root ≤4
- [x] `common/util/world` (6) → chunk/, level/, blockentity/, root (BlockLocation, ChunkLocation, package-info)
- [x] `common/util/scheduler` (6) → runnable/, root ≤4
- [x] `common/vm/context` корень (5) → event/, interrupt/, memory/, root (VMContextManagerCollection, package-info)
- [x] `client/gui/screen/common` (6) → machine/, monitor/, root ≤4
- [x] `common` корень (6) → setup/ (CommonSetup, NativeLoader), config/ (ConfigManager), root (Main, Constants, package-info)

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

## 8. Screen↔Container auto-регистрация ✅ — DONE

**Опционально**: вместо ручного `event.register(CONTAINER.get(), Screen::new)` для каждого —
DSL через ScreenRegistry:
```java
ScreenRegistry.register(event, COMPUTER, ComputerContainerScreen::new);
ScreenRegistry.register(event, COMPUTER_TERMINAL, ComputerTerminalScreen::new);
```

- [x] Создан `ScreenRegistry` (DSL поверх `RegisterMenuScreensEvent`), `Containers.registerScreens` переведён на него

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
- [x] **PMD: codestyle.xml + design.xml** — добавлены rulesets с исключениями; итого **422 нарушения** (в отчёте, билд не падает): UselessParentheses 74, AvoidInstantiatingObjectsInLoops 58, CyclomaticComplexity 56, CognitiveComplexity 41, GenericsNaming 27, CollapsibleIfStatements 24, NPathComplexity 23, CallSuperInConstructor 19, AvoidDeeplyNestedIfStmts 16, UnnecessaryModifier 14, UnnecessaryCast 12, остальное <10. Исключены (конфликт со стилем/Checkstyle): ControlStatementBraces, FieldNamingConventions, UseUtilityClass
- [x] **SpotBugs** — плагин `com.github.spotbugs` **6.5.10** (Gradle 9+), движок `toolVersion` **4.10.3**; `ignoreFailures=true`, отчёты html+xml в `build/reports/spotbugs/`; **453 бага** main (52 priority-1: EI_EXPOSE_REP2 99, MS_CANNOT_BE_FINAL 71, EI_EXPOSE_REP 61, ST_WRITE_TO_STATIC 49, PA_PUBLIC_PRIMITIVE_ATTRIBUTE 47, ...) + 1 в test. Фиксы: `reports.create("html")/("xml")` (6.x не регистрирует отчёты по умолчанию), исключение `package-info.class` из анализа (SpotBugs падает с ResourceNotFoundException), exclude-фильтр на jcodec/generated
- [x] **Error Prone** — плагин `net.ltgt.errorprone` **5.1.0** + `error_prone_core` **2.50.0**; включён **выборочно**: `./gradlew compileJava -PenableErrorProne`; все диагностики — warnings (билд не падает); **100 предупреждений** (EffectivelyPrivate 21, MutablePublicArray/InconsistentCapitalization/EnumOrdinal/EmptyCatch по 7, ...). Воркэраунды: mixin-процессор шейдит Guava 21.0 → современный Guava подмешивается в начало processorpath (иначе NoSuchMethodError buildOrThrow); `-PenableErrorProne` без `=true` даёт пустую строку свойства → парсинг флага по presence
- [x] **AvoidDuplicateLiterals** — включено: `maxDuplicateLiterals=8`, `minimumLength=6`, `skipAnnotations=true`; в отчёте 2 нарушения
- [x] **AvoidInstantiatingObjectsInLoops** — правило включено (было исключено); **58 нарушений** в отчёте (33 файла, топ: ComputerBlockItemRenderer 6, AbstractContainer/RobotInventoryContainer 4) — билд не падает; точечные `@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")` — при рефакторинге
- [x] **Qodana** — плагин `org.jetbrains.qodana` **2026.2.0** (таск `qodanaScan` в Docker), алиас `./gradlew qodana`, конфиг `qodana.yaml` (jetbrains/qodana-jvm:latest, exclude jcodec/build)

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

## 11. ~~jcodec → Maven dependency~~ — отменена

Делаем задачу 18 (полное удаление jcodec, видеопайплайн на raw RGB) — Maven-зависимость не нужна.

---

## 12. Мультимонитор: фрагментная модель (как в OpenComputers)

**Цель**: заменить OBJ-модель монитора на фрагментные JSON-модели + кастомную BakedModel по позиции блока в мультиблоке. Референс: `ref/` (CaitlynMainer/OpenComputers), файл `ScreenModel.scala`, текстуры `textures/block/screen/`.

- [x] Скопировать 48 фрагментных текстур (f*/b*) из ref в `assets/oc2r/textures/block/monitor/`, оставить overlay
- [x] Config: `monitorMaxWidth`/`monitorMaxHeight` (по умолчанию 5), читать в `MonitorMultiblock` вместо захардкоженных MAX
- [x] ModelData: `MonitorBlockEntity` отдаёт width/height/offset/facing через ModelProperty
- [x] `MonitorBakedModel` (аналог `ScreenModel`): выбор фрагмента рамки по позиции блока
- [x] Заменить JSON blockstate/models: фрагментная модель вместо monotonic OBJ
- [x] Удалить `monitor.obj`/`monitor.mtl`, синхронизировать item-модель
- [x] Item-модель: `getQuads(side=null)` отдаёт все 6 граней (иначе предмет не рендерился); datagen MONITOR убран из `ModBlockStateProvider` (blockstate теперь ручной, избегаем регенерации)
- [x] **Сборка мультиблока → полный прямоугольник** (любой порядок): `MonitorMerge` (BFS по 4-соседству всех same-facing мониторов, развёртка в footprint; объединение только если bounding box полностью заполнен и ≤ конфиг-лимита; иначе новый блок остаётся 1×1). `MonitorRepartition` — общая переразметка (W/H/offset + перенос живого состояния на новый origin). Исправляет «дырявый 2×2» (раньше extend DOWN расширял H без проверки полноты строки).
- [x] **Ломание одного блока** вместо всего мультиблока: `MonitorBreak` — рушится только кликнутый блок; остальные делятся на связные компоненты и переразмечаются в один/несколько полных прямоугольников (жадное извлечение максимального полного прямоугольника); состояние следует за origin (или на ближайший к нему блок). Убран `IS_BREAKING_MULTIBLOCK` и drop W×H-1 предметов (ванильная loot-table даёт 1 предмет).
- [x] `MonitorMultiblock` сокращён до координатных хелперов (139 строк), логика вынесена в `MonitorMerge`/`MonitorBreak`/`MonitorRepartition`
- [ ] Build + проверка в игре мультимонитора разных размеров (сборка 2×2/3×3 в любом порядке, ломание одного блока → переразметка)

---

## 13. Переписывание проводов (энергия FE+EU + починка коннекции) ✅ — DONE

**Решение**: кабель становится энергопроводником с универсальной энергией. **FE/RF** — через `IEnergyStorage` (родной), **EU (IC2)** — через EnergyNet adapter. Передача с буфером и лимитом на тик из конфига. Коннекция чинится.

- [x] Config: `cableEnergyCapacity` (3000 FE буфер) + `cableEnergyTransferPerTick` (512 FE/тик на соседа)
- [x] Создать `CableEnergyStorage` (FixedEnergyStorage-подобный буфер) + зарегистрировать `Capabilities.EnergyStorage.BLOCK` в `BusCableCapabilities`
- [x] `EnergyTransferManager`: сетевое распределение раз в тик — `collectNetwork` BFS по сети, пул из чистых источников, равномерное перераспределение буферов по сети, пуш в приёмники с лимитом на тик (чинит «пинг-понг» энергии между кабелями, из-за которого энергия не шла дальше 1-го кабеля)
- [x] Творческий энергоблок = бесконечный источник: `InfiniteEnergyStorage` (extract-only) + регистрация capability
- [x] **EU (IC2) через adapter**: мост `Ic2EuBridge`/`EuEnergyAdapter` (опциональная зависимость, без жёсткой привязки, 4 FE = 1 EU)
- [x] Автоконнект: кабель соединяется с соседями с `IEnergyStorage`, `DeviceBusElement` и любыми блоками с RPC-методами (динамик, редстоун-интерфейс) — без ручного INTERFACE
- [x] Реконфигурация: единая `recomputeConnections()` вместо размазанного `updateShape`/`canHaveCableTo`
- [x] Build + проверка в игре (творческий блок → кабель → комп; скорость 512/t против потребления 258/t)

---

## 1. C API для Redstone Interface (#89)

**Проблема**: Lua на VM медленный, нет `sleep()`, неудобно для real-time контроллеров. Хочется писать на C/Rust под RISC-V.

**Текущее состояние**: Сделана C-библиотека `librpc` в `src/main/scripts/lib/rpc/`.

- [x] Добавить TCC в buildroot-образ — **уже есть**: sedna-buildroot 0.0.64 (закреплён в `gradle.properties`) содержит `/usr/bin/tcc` (297 КБ) + `/usr/lib/tcc/include/` в `rootfs.cramfs` (проверено по содержимому jar); см. `docs/BUILDROOT.md`
- [x] Добавить примеры: redstone_blink.c, note_block_player.c — `src/main/scripts/lib/rpc/redstone_blink.c`, `src/main/scripts/lib/rpc/note_block_player.c` (+ обновлён `Makefile`)
- [x] Сделать C++ RAII-обёртку — `src/main/scripts/lib/rpc/rpc_raii.hpp` (классы `rpc::Bus`/`rpc::Device`, деструкторы закрывают bus) + демо `example_raii.cpp`; для этого в `rpc.c`/`rpc.h` добавлены `rpc_device_invoke_*_raw`

---

## 2. Resizable Screen (#12) — Deferred

Сначала рефакторинг и TCC.

---

## 3. TCC (Tiny C Compiler) в образ

- [x] Обновить minux чтобы включить TCC — сделано апстримом: конфиг minux (buildroot) содержит `BR2_PACKAGE_TINYCC=y`, релиз 0.0.64 уже включает tcc; версия закреплена в `gradle.properties`/`download-libs.sh`
- [ ] Собрать новый buildroot-образ — требует toolchain/Docker для minux (вне этого репо); инструкция: `docs/BUILDROOT.md`

---

## 14. Переписывание корпуса ПК (Block) ✅ — DONE

**Механика корпуса ПК (Block):**

**Лицевая сторона** — сторона, куда блок смотрит при установке (направление блока, как у печки/рабочего стола).

- [x] **ПКМ по любой стороне, кроме лицевой** — открываем GUI инвентаря компонентов (слоты для GPU, CPU, RAM и т.д.) (`ComputerBlockInteraction.useWithoutItem`: `hit.getDirection() != FACING` → `openInventoryScreen`)
- [x] **ПКМ строго по лицевой стороне** — открываем терминал (`hit.getDirection() == FACING` → `openTerminalScreen`)
- [x] **Запуск**: shift+ПКМ или кнопка питания в GUI (уже была — `PowerButton` в терминале и инвентаре)
- [x] **POST-бипы при ошибках запуска**: нет энергии/фirmware/CPU/памяти → 5 разных бипов (`ComputerPost`, `POST_BEEP_*`): 1 бип firmware, 2 — энергия, 3 — CPU, 4 — память, long — unknown. Хук: `ComputerVirtualMachine.handleBootErrorChanged`

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

## 21. Звук: тональный генератор, PCM-стриминг, Speaker-блок ✅ — DONE

**Решение**: звуковая карта (`SoundCardItemDevice`) и блок-динамик (`speaker`) с RPC-методами `beep`/`playTone`/`write`. Клиентский синтез тона + PCM-стриминг через `AudioStream`.

- [x] **Тональный генератор**: RPC `beep(frequency, duration)` / `playTone` — клиентский синтез синуса (44.1 кГц, 16-bit mono, fade in/out против кликов). `ToneAudioStream` + `ToneSoundInstance`; `SoundEvent`-заглушка `sound_card_beep` (stream) + `sounds.json`
- [x] **PCM-стриминг**: RPC `write(byte[] pcm)` (чанки ≤4096) → `SoundCardPcmMessage` → клиентский ring buffer (`PcmSoundBuffer` + `PcmAudioStream`) → looping `StreamingPcmSoundInstance`, stop по 2 с тишины. Сообщения `SoundCardBeepMessage`/`SoundCardPcmMessage` через tracking chunk
- [x] **Кулдаун 2 с** — уже настраиваемый в конфиге (`Config.soundCardCoolDownSeconds`); beep/playTone/write не ограничены кулдауном
- [x] **Блок «Speaker»/динамик**: `SpeakerBlock` + `SpeakerBlockEntity` (NamedDevice, тип `speaker`, RPC beep/playTone/write, звук из позиции блока), регистрация, креативная вкладка, модели/lang ×4. Автоконнект к кабелю (см. задачу 13)
- [x] POST-бипы при ошибках запуска (задача 14): 5 `.ogg` (`post_beep_*`), звук `computer_running` со `stream: true`
- [x] Build + проверка в игре (мелодия из Lua/Python через beep)

### Проверка в игре — открыто (v0.1.0+383c4ec)
- [ ] **Спикер молчит**: `lua -e 'local d=require("devices"); d:find("speaker"):beep(880,500)'` — RPC без ошибок, но звука нет. Цепочка целая: callback → `SoundClientMessages.sendBeep` → `SoundCardBeepMessage` → `SoundClientManager.playTone` → `ToneSoundInstance` (stream `sound_card_beep`). POST-бипы (та же регистрация звука) играют → реестр/резолв в порядке. Что проверять: (1) лог клиента на `Unable to play unknown soundEvent`/исключения в `enqueueWork`; (2) расстояние — `Attenuation.LINEAR` с дальностью 16 блоков, отойди/подойди к динамику; (3) молчаливый сбой в streaming-пути `SoundEngine` (исключение в `CompletableFuture.thenAccept` не логируется) — при необходимости играть не через `stream=true`, а вернуть звук из `SoundBufferLibrary` (запасной `.ogg`).
- [ ] **`d:find("sound")` вернул nil** — был следствием выключенного ПК (нехватка энергии, конфиг 256/t). После фикса (transfer 1024/t, буфер компа 8000) проверить ещё раз.
- [ ] Проверить `write` (PCM-стриминг) после починки тона.

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

**⚠ Merge-blocker для `work` → `1.21.1`**: GPU-предметы сейчас бесполезны (монитор игнорирует тир/разрешение и mount'ится без GPU). Пока не доделана интеграция GPU↔монитор, ветки не сливать.

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
- [ ] **Убрать хардкод 640×480 в encode-пайплайне**: `MonitorVideoController` создаёт `Picture.create(WIDTH, HEIGHT, YUV420J)` со статическими `MonitorDevice.WIDTH/HEIGHT` — параметризовать от фактического разрешения framebuffer'а (GPU)
- [ ] **Device-tree**: `SimpleFramebufferDeviceProvider` — width/height/stride из GPU, а не захардкоженные 640×480
  - **Уточнение (2026-08-21)**: провайдер уже размерно-независим (`fb.getWidth()/getHeight()`, `stride = width × 2`). Реальный хардкод — только `MonitorDevice.WIDTH/HEIGHT` + `Picture` в `MonitorVideoController` и клиентский рендер (задача 6: зона 12×7 px на блоке, кадр 640×384 DynamicTexture).
- [ ] **Без GPU → UART-терминал**: монитор не показывает framebuffer, но текстовый терминал (UART) работает
- [x] Конфиг: `gpuEnergyPerTickTier1..4` в `EnergySpec`
- [ ] Build + проверка в игре (монитор с GPU T1/T2/T3/T4, без GPU — чёрный)

---

## 24. CPU: конфиг частот, новые тиры, губернаторы

**Проблема**: частоты захардкожены в `Items.java` (25/50/100/200/1000 MHz), конфиг не реализован (`Config.java:12` — TODO). `timeQuota` 25 ms захардкожен. На T_INF cycleLimit копится бесконечно.

- [x] **Новые тиры CPU** (минимальный не 25, а 50 MHz):

| Тир | Частота | Описание |
|---|---|---|
| CPU T1 | 50 MHz | Базовый (было 25) |
| CPU T2 | 100 MHz | Средний (было 50) |
| CPU T3 | 200 MHz | Продвинутый (было 100) |
| CPU T4 | 400 MHz | Эндгейм (было 200) |
| CPU T_INF | 1000 MHz | Creative (без изменений) |

- [x] **Конфиг частот**: реализовать `cpuFrequencyTier1/2/3/4` в `GameplaySpec` (убрать TODO из `Config.java:12`). `Items.java` читает из конфига, а не захардкоженные константы
- [x] **`timeQuota` в конфиг**: вынести `TIMESLICE_IN_MS = 25` из `VMRunner.java` в `Config.vmTimeQuotaMs`. На слабых серверах — снизить, на мощных — поднять
- [x] **Cap на накопление `cycleLimit`**: сейчас `cycleLimit` растёт без ограничений → на T_INF копится «долг» циклов. Добавить cap: `cycleLimit = min(cycleLimit + getCyclesPerTick(), 2 × getCyclesPerTick())` — не больше 2 тиков вперёд
- [x] Обновить крафты `recipe/cpu_tier_1..4.json` под новые частоты
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

### Находки аудита inet/ (2026-08-21, из issue #13)

Контекст: Nathan22211 не смог завести интернет из VM (ping 1.1.1.1, DNS через 8.8.8.8 — всё молча падает). Разбор показал реальные баги стека.

**Верификация (2026-08-21, ветка work, HEAD 6518289)**: все 5 кодовых находок подтверждены по исходникам; строки/файлы совпадают.

**Вероятная первопричина у репортера** — комбинация пунктов 3+4: ping идёт через ICMP-fallback `isReachable()` (ложно-отрицательный на выделенном сервере без CAP_NET_RAW), DNS падает из-за молчаливого дропа фрагментированных UDP-ответов (EDNS0 >512 Б). Плюс возможная misconfiguration: DHCP нет, гость должен сам назначить себе IP (карточка — point-to-point линк).

- [x] **`assert false;` в `DefaultSessionLayer.java:112`** — убран (2026-08-21). Отладочный артефакт: с `-ea` любой TCP-read крашит JVM, без `-ea` — мёртвый код
- [x] **Нет ICMP Destination Unreachable / Time Exceeded при дропе пакетов** — исправлено (2026-08-21): `DefaultNetworkLayer.queueIcmpError` ставит в очередь ICMP type 3 code 4 (frag-needed, MTU 1500) для фрагментов и type 11 code 0 для TTL=1; доставка гостю на следующем receive-поллинге (паттерн ARP-reply из `DefaultLinkLocalLayer`). deniedHosts остался silent drop (security-фильтр) с комментарием
- [x] **Фрагментированные IP-пакеты дропаются молча** (`DefaultNetworkLayer.java`) — исправлено тем же механизмом (см. выше); сборка фрагментов не делается (только ICMP frag-needed с MTU)
- [x] **ICMP-fallback `InetAddress.isReachable()`** (`EchoHandler.java`) — исправлено (2026-08-21): одноразовый WARN при первом неудачном fallback (CAP_NET_RAW false-negative), семантика ответа не менялась
- [x] **VXLAN-зависимость интернет-карты не проверяется** — исправлено (2026-08-21): `InternetManagerImpl.initialize()` логирует WARN «internet card is enabled but VXLAN is disabled»; комментарий в `InternetCardSpec.java` поправлен; ключ конфига не переименован
- [ ] **Документация для пользователей**: нет DHCP, карточка = point-to-point линк (гость назначает себе любой IP, карточка отвечает на ARP). Написать в README/доку как настраивать сеть в госте + предупреждение про deniedHosts
- [ ] Ответить в issue #13 после фиксов ping/DNS + приложить инструкцию из предыдущего пункта

### Новые баги, найденные при написании тестов (2026-08-21)

- [x] **`MacAddressUtils` — знаковое расширение байта**: `parseMacAddress` собирал prefix/address без маски `& 0xFF` → любой MAC с байтом ≥ 0x80 парсился мусором (`5E:D1:...` → prefix `0xFFD1`, `...:FF` → адрес `0xFFFFFFFF`); `byteToHex` форматировал отрицательные байты мусорными символами. Исправлено + тесты `MacAddressUtilsTest`
- [x] **`InetUtils.quickICMPBody` — `put()` вместо `get()`**: буфер перезаписывался нулями вместо копирования в результат → все ICMP-unreachable ответы уходили с обнулённой цитатой исходного пакета (RFC 792 payload). Исправлено на `data.get(result, 4, ...)`
- [x] **`IcmpHandler.reject` — source address 0.0.0.0**: `ICMPReply` создаётся с `srcIpAddress=0`, consume делает `updateIpv4(0, dst)` → порт-unreachable ответ приходит гостю с источника `0.0.0.0`. Задокументировано тестом `IcmpHandlerTest` (поведение сохранено), **нужен фикс**: передавать адрес недоступного хоста как src
- [x] **Mockito 4.3.1 → 5.17.0**: byte-buddy 1.12.7 не поддерживает Java 21 («Could not modify all classes»); до этого Mockito в тестах фактически не использовался. `mockito-inline` → `mockito-core` (inline-мокер встроен в 5.x). Тестовый classpath теперь наследует main (`testCompileClasspath`/`testRuntimeClasspath` extendsFrom), т.к. inet-слои грузят NBT/MC-классы в рантайме
- Итого: **+29 модульных тестов** (`DefaultNetworkLayerTest` 12, `MacAddressUtilsTest` 7, `IcmpHandlerTest` 4, `SessionManagerTest` 6), всего 152, зелёные; checkstyle/PMD без новых нарушений

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

## 30. Блок «Загрузчик ОС» (OS Loader / Flash Writer block)

Отдельный блок, в который кладётся флеш-память и подаётся путь/URL к образу прошивки, а на выходе получается предмет (flash/HDD с прошитой ОС) — без права пересборки мода и без доступа к датапакам/серверу.

### Зачем
- Сейчас OnyxOS-образы зашиты в jar (или читаются из `config/oc2r/`). Хочется дать игроку способ «залить свою ОС» прямо в игре: вставить флешку/диск, указать источник образа, получить предмет.
- Не требует пересборки мода для новой ОС, не требует прав на сервер (игрок работает с предметами в своём мире).

### Идеи реализации
- [ ] Блок (аналог `FlashMemoryFlasher` / `DiskDrive`): GUI со слотами «источник образа» (файл в `config/oc2r/` или путь в папке мира) и «флеш/диск».
- [ ] Интерфейс ввода: текстовое поле пути к `.bin`/`.img` (например `config/oc2r/onyx-kernel.bin`, `config/oc2r/onyxfs.img`) + кнопка «Записать».
- [ ] На выходе: `flash_memory_onyxos` (флешка с firmware=onyxos, но kernel берётся из указанного файла) или `hard_drive_onyxos` (диск с rootfs из файла).
- [ ] Механизм «внешних образов» уже заложен: `OnyxOSFirmware`/`OnyxOSBlockDeviceData` читают `config/oc2r/*` с fallback на jar (коммит `0b90b3b`). Блок должен использовать тот же источник, но с выбором конкретного файла.
- [ ] Альтернатива/расширение: скачивание по URL (сеть из `inet/`) в `config/oc2r/` и прошивка.
- [ ] Build + проверка в игре: прошил флешку из файла → вставил в комп → OnyxOS грузится с кастомным kernel/rootfs.

## 31. Аудит VT100-терминала (2026-08-18, ветка work, HEAD 75c8cc4)

Аудит модуля `src/main/java/li/cil/oc2/common/vm/terminal/**`. Формат: `[файл:строка]`.

### Блокеры

- [x] **Б1 — `clearLine()` сбрасывает текущий цвет переднего плана**
  `[buffer/TerminalBuffer.java:65]` — `terminal.currentForegroundColorMode = ColorMode.SIXTEEN_COLOR;` внутри очистки строки.
  После EL/ED/DL программа в truecolor/256-цветах начинает писать в 16-цветную палитру.
  Фикс: удалить строку 65 + тест: `CSI 38;2;r;g;b` + `CSI K` + символ → цвет не меняется.

- [x] **Б2 — SU (CSI S) — no-op, пока scrollback не заполнен**
  `[buffer/TerminalBufferScrolling.java:66-68]`, `[escapes/csi/CH8.java:18-20]`
  Условие `lastRowToDisplay == HEIGHT * SCROLL_BACK_COUNT` ложно на свежем терминале,
  а CH8 не вызывает `incrementLastLineToDisplay()` → `printf` + `CSI S` не двигает экран.
  Фикс: при `lastRowToDisplay < max` — `incrementLastLineToDisplay()`, физический сдвиг — при достижении max.

- [x] **Б3 — dirty-маска `shiftMainBuffer` мапит не те строки**
  `[buffer/TerminalLineShifter.java:126-128]`
  `i` — абсолютный индекс строки буфера, а формула — копия из `setChar` для относительной.
  Правильно: `localI = i + HEIGHT - lastRowToDisplay`. Текущая даёт смещение на `(max - HEIGHT)`.
  Эффект: после IL/DL/SU/SD с маргинами (при выросшем scrollback) видимые строки не перерисовываются.

### Major

- [x] **ED (CSI J) case 2 двигает курсор домой**
  `[TerminalBuffer.java:41]`, `[escapes/csi/ED.java:28-29]`
  `clear()` вызывает `setCursorPos(0,0)`. По VT100 ED не должен трогать курсор.
  Фикс: отдельный `clearScreen()` без перемещения курсора.

- [x] **SGR 38/48 обрывает последовательность**
  `[escapes/csi/SGR.java:47]`
  `return;` после обработки 38/48 — `CSI 38;5;196;1m` теряет bold, `...;48;5;52m` теряет фон.
  Фикс: `i = index; continue;` вместо `return`.

- [x] **Не-ASCII ввод обрезается до одного байта**
  `[client/gui/widget/terminal/TerminalKeyboardHandler.java:17,41]`, `[TerminalIO.java:99-108]`
  `putInput((byte) ch)` — кириллица/вставка из буфера → моджибек. Фикс: UTF-8-кодировать.

- [x] **RIS не сбрасывает часть состояния**
  `[escapes/index/RIS.java:10-45]`
  Не сбрасываются: `scrollFirst`/`scrollLast`, `savedX`/`savedY`, `altSavedX`/`altSavedY`, `cursorMode`, очередь `input`.

- [x] **SD/RI сдвигают весь буфер — втягивают scrollback в экран**
  `[buffer/TerminalBufferScrolling.java:84-100]`
  `shiftDown` всегда сдвигает `[0..478]`; при выросшем scrollback верхняя строка экрана заполняется
  содержимым scrollback вместо пустой. Верный диапазон — видимая область `[L-24 .. L-1]`.

- [x] **Гонка на серверной очереди input**
  `[TerminalIO.java:121-135]`, `[TerminalOutput.java:43-46]`
  `putResponse`/`enqueueInput` пишут под `TerminalOutput.lock`, а `readInput`/`putInput` — под `TerminalIO.lock`.
  Разные лока, `ByteArrayFIFOQueue` не потокобезопасен → потерянные/перемешанные байты при DSR-запросе + вводе.
  Фикс: единый лок на оба пути.

- [x] **System.out.println в продакшн-коде**
  `[TerminalOutput.java:122]`, `[CSIManager.java:164]`, `[CH1.java:15]`, `[CH4.java:15,17,19]`,
  `[CH5.java:15,17,19,38]`, `[CH7.java:27,29,31]`, `[CH8.java:14,16]`, `[CH9.java:13,15]`,
  `[CH10.java:15]`, `[ED.java:13]`, `[CH6.java:17,19]`, `[modes/impl/ImplementedPrivateModes.java:94]`
  Печать на каждый нераспознанный байт/режим → консольный спам. Заменить на логгер.

### Minor

- [x] **TBC очищает только main tabs, HTS пишет в altTabs**
  `[escapes/csi/TBC.java:13-21]`, `[escapes/HTS.java:8-10]` — согласовать.

- [ ] **getInput() щёлкает вид вниз без пометки dirty**
  `[TerminalIO.java:42-43]` — после прокрутки вверх + нажатия клавиши экран остаётся старым.

- [x] **Dirty-маска сырым `y` при прокрутке**
  `[CH10.java:104]`, `[CH11.java:91,162]`, `[ECH.java:52]`, `[TerminalBuffer.java:120]`
  Используют `1 << y` вместо `localY`-трансформации из `setChar` (TerminalBufferWriter.java:138-143).

- [x] **`ColorData()` — локальная переменная `Mode` вместо поля**
  `[color/TerminalColors.java:117-123]` — `Mode` остаётся null, рендер может упасть по NPE.

- [x] **Двойной `lock.lock()`**
  `[TerminalOutput.java:43,46]` — реентрантный, не крашит, но маскирует границы. Убрать внутренний.

- [x] **Мёртвые поля Terminal**
  `[Terminal.java:25,78-81]` — `Use1006`, `continuationByte`, `bytesRead`, `bytesToRead`, `unicode` — удалить.

- [x] **Сериализуемость/размер NBT терминала**
  `[Terminal.java:50]` — буфер/alt-массивы и `input` помечены `transient` (Ceres создаёт инстанс через
  no-arg конструктор, экран сбрасывается при загрузке). NBT ~512 КиБ → ~2 КиБ на снимок.
  `input` и так не сериализовался (поля fastutil transient).

### Nit

- [x] **Дубликат square-глифа в FontAtlas** `[fonts/FontAtlas.java:52-56]`
  Удалён второй `glyphs.add(square)` — при resize UV делился на 2 дважды.
- [x] **Дубликат fw_jump.bin в 3 местах + легаси onyx-kernel**
  `src/main/resources/generated/` удалён (gitignored мёртвый вес, легаси onyx-kernel нигде не читается);
  копия из `scripts/firmware_files/` удалена — `packageScripts` теперь кладёт fw_jump.bin в zip из `onyxos/`.
  Один источник: `src/main/resources/onyxos/fw_jump.bin`.
- [x] **Константы PrivateMode/Mode не используются** — CH2/CH3 захардкожены, заменить на константы.

### Архитектура

- [x] **Дублирование ~60-case таблиц режимов** в CH1/CH2/CH3/CH6 — вынести в один `ModeTable`.
  Создан `modes/ModeTable.java` — единственный источник истины для 74 private + 4 ANSI режимов
  (номер из `PrivateMode`/`Mode`, kind PRIVATE/ANSI, флаг implemented). CH1/CH6 делают save/restore
  через `get()`/`set()` таблицы; CH2/CH3 оставили только спец-тела (DECCOLM/DECOM/mouse/alt-buffer
  и т.п., ~15/6 case) + `default` через `mode.set(...)`; `PrivateModeState.getMode()` и
  `ImplementedPrivateModes` тоже делегируют таблице. Поведение идентично — 38 тестов зелёные.
- [x] **Dirty-механика в buffer-слое** — `TerminalBuffer`/`TerminalLineShifter` знают про `renderers`
  и `getDirtyMask` (TerminalBuffer.java:42,118-120, TerminalLineShifter.java:80-84,131-135).
  Перенести в render-слой.
  Решено (2026-08-18): buffer-слой и CSI-хендлеры больше не трогают `renderers`/`getDirtyMask` —
  единственная точка распределения dirty — хуки `Terminal.markDirty(mask)` / `Terminal.markAllDirty()`;
  сам реестр `renderers` живёт в `TerminalClient` (render-слой), маски — в `TerminalRenderer.dirty`.
- [x] **Договор молчания по `args`/`argCount`** — CSIManager клампает до 10 и подмешивает пустой нулевой слот;
  каждый хендлер сам фильтрует `args[i]==0`. Нормализовать (0 → default) в CSIManager.
  Решено (2026-08-18): единый дефолт невозможен — зависит от функции и CSI-модификаторов
  (DECSTBM vs XTRESTORE на одном `r`; DECSCUSR где 0 валиден; DECSET/DECRST где 0 = пустой слот).
  Дефолт параметризован per-handler: `CSISequenceHandler.defaultParameters(CSIState)` возвращает
  значения по слотам, CSIManager подменяет пропущенный/0 перед `execute`; хендлеры без дефолта
  (CH2/CH3/CH6/CH7/DECREQTPARM) не трогаются и читают args как есть.

### Тесты

- [x] **Снять `@Disabled` с TerminalBufferTest** — декомпозировать `@OnlyIn(Dist.CLIENT)` с `Terminal`
  (оставить только на `getRenderer()`/`clientTick()`), добавить assert'ы (сейчас тел нет).
- [x] **Покрыть критичные пути:** CSI-парсер, DECSTBM+DECOM, IL/DL/SU/SD с count>1 и маргинами,
  alt-буфер 47/1047/1049, SGR 38;2/38;5, pending-wrap (колонка 80), dirty-маску при прокрутке.

---

## 32. Аудит блоков на логические баги + PMD (2026-08-18, ветка work)

Аудит мониторов и логики блоков на баги по образцу кабеля (энергия не шла дальше 1-го кабеля из-за «пинг-понга»). Формат: `[файл:строка]`.

### Баги (найдено при аудите)

- [x] **Монитор перекодирует кадры вечно**
  `[vm/device/SimpleFramebufferDevice.java:72]` — `applyChanges()` конвертил буфер, но не чистил `dirtyLines`
  (в оригинале OC2 чистит). → `hasChanges()` всегда true → монитор шлёт кадры каждый тик бюджета лоадбалансера даже без изменений.
  Фикс: `dirtyLines.clear()` внутри lock после конвертации.

- [x] **NetworkSwitch — краш на загрузке (IndexOutOfBoundsException)**
  `[blockentity/network/switches/NetworkSwitchBlockEntity.java:47-51]` —
  `new ArrayList<>(BLOCK_FACE_COUNT)` создаёт *пустой* список (capacity 6, size 0), а `adj.set(side.get3DDataValue(), …)` на нём всегда кидает IOOBE.
  → любой чанк со свитчем крашит сервер при загрузке (свитч в креативе/командой).
  Фикс: предзаполнить список null'ами перед `set`.

- [x] **NetworkConnector — бесконечный цикл пустых кадров → краш свитча**
  `[blockentity/network/connector/NetworkConnectorBlockEntity.java:72-77]` —
  контракт `NetworkInterface.readEthernetFrame()`: «нет данных → null», но `NullNetworkInterface`/свитч/хаб/VXLAN возвращают пустой `byte[0]`.
  Цикл `while (frame != null && byteBudget > 0)` крутится ~78 раз/тик и шлёт пустые кадры.
  Пустой кадр в свитче → `SwitchPacketForwarder.forward` → `PacketProcessor.macToLong(frame, 0)` на `byte[0]` → ArrayIndexOutOfBoundsException → краш сервера.
  Фикс: `while (frame != null && frame.length > 0 && byteBudget > 0)`.

- [x] **PCI Card Cage — потребление энергии мертво**
  `[blockentity/misc/PciCardCageBlockEntity.java:37]` — `handleMountedChanged(boolean)` пустой → `isMounted` никогда не true → `serverTick()` ранний выход.
  Плюс `energyPresent` считался только на клиенте (`handleUpdateTag`), на сервере не вычислялся → `has_energy` всегда false в апдейт-теге.
  Фикс: `isMounted = value;` + вычислять `energyPresent` на сервере и слать через `setChanged()`.

- [x] **BundledRedstone — get/set на разных гранях**
  `[blockentity/misc/redstone/BundledRedstoneCallbacks.java:18,24,29,43]` —
  `getBundledOutput` читает индекс `side.getDirection()`, а `setBundledOutput`/`setBundledOutputs`/`getBundledInput` используют `side.getDirection().getOpposite()`.
  → записанный bundled-сигнал выходит на противоположной грани, read-back возвращает чужую грань.
  Фикс: убрать `.getOpposite()` (set/get/input симметричны, мировой считыватель `getBundledSignal` читает `worldDir`-индекс напрямую).

### PMD — 423 предсуществующих warning'ов (все правила, не только complexity)

- [ ] **Обнулить PMD-вёрдл в `./gradlew pmdMain`** — починить 423 warning'а (см. `build/reports/pmd/main.html`),
  сгруппировать по правилам (Avoid instantiating new objects inside loops, complexity: Cognitive/Cyclomatic/NPath,
  Useless parentheses, Unnecessary cast, final→static, параметр `frame_bytes` не по нотации и т.д.).
  Мой код добавляет: `EnergyTransferManager` (complexity 10-12/NPath 216-392), `BusCableBlockEntity.java:164`,
  `NetworkSwitchBlockEntity` (6 шт, в осн. предсуществующие). Решение по каждому правилу: фикс кода / обоснованный `// NOPMD` / конфиг PMD.

### Заметка (не чинено, требует решения по спецификации)

- [ ] **Redstone Interface: индекс сторон** — `setRedstoneOutput`/`getRedstoneOutput` используют `side.getDirection()` (мировой индекс),
  а мировой считыватель `getOutputForDirection` конвертит world→local (`HorizontalBlockUtils.toLocal`) →
  при FACING != NORTH выходы/чтения расходятся. Нужно решить: Side = world-фикс или local-относительный, и привести всё к одному.

## 33. Документация кода + разбор NOPMD-маркеров (2026-08-19, ветка work, HEAD 8168a1f)

PMD обнулён (423→0), но ценой ~81 inline `// NOPMD`-маркера и отсутствия документации на сложной логике.
Два направления: (а) полноценные Javadoc'и, (б) пересмотр NOPMD, где он прикрывает неудачный рефакторинг.

### Документация (Javadoc)

- [ ] **Классы без шапки** — добавить Javadoc на верх класса (назначение, инварианты, threading-модель) для:
  `EnergyTransferManager` (сетевое распределение энергии раз в тик, pull/redistribute/push), `BusCableBlockEntity`,
  `NetworkConnectorConnectionManager`, `SwitchHostTable`/`SwitchPortManager`, `MonitorBreak`/`MonitorMerge`/`MonitorRepartition`
  (алгоритм поиска прямоугольника: BFS-расширение, приоритет corners), `TerminalOutput`/`CSIManager`/`SGR` (конечный автомат VT100,
  фазы ESC/CSI/OSC, что делает каждый диспетчер), `ModeTable` (таблица режимов ANSI), `EstablishedState` (конечный автомат TCP-сессии),
  `SimpleFramebufferDevice` (dirty-слои и кодирование кадров), `AbstractContainer`/`AbstractMachineTerminalContainer` (слотовая логика),
  `ICaptureInputStateStorage` (контракт захвата ввода).
- [ ] **Сложные методы** — Javadoc/комментарии перед сложной логикой: `distribute`/`redistribute`/`collectNetwork` (энергия),
  `findBestRectangle`/`expandBlock` (монитор-мультиблок), `selectStyle`/`handleExtendedColor` (SGR), `playSound` (SoundCardItemDevice),
  `stackIntoExistingSlots` (контейнеры), `renderBackground` (run-length отрисовка).
- [ ] Правило: документация на том же языке, что и остальной код (в проекте вперемешку; выбрать RU/EN и вести в одном стиле).

### Разбор NOPMD

- [ ] Пересмотреть ~81 маркер, заменить где возможно на реальный код:
  - `// NOPMD getter API ... renaming is API churn` (6 шт: `getCaptureInputState`/`getPowerState`) — либо переименовать
    в `isXxx` со всеми каллерами (~20 файлов), либо вынести в интерфейс с нормальной Javadoc-спецификацией контракта.
  - `// NOPMD 10-case VT100 ... dispatch` (4 шт: `dispatch`, `handleSingleCharEscape`, `handleControlChar`, `handleModifier`) —
    порог PMD = 10, а у них ровно 10 веток; можно вынести каждую группу case'ов в отдельный метод/таблицу, чтобы уйти ниже порога.
  - `// NOPMD ... depends on loop iteration` (26 шт) — проверить, нельзя ли вынести аллокацию из цикла (пулы, mutable-буферы);
    где зависит от итерации — оставить, но с Javadoc-обоснованием вместо однострочника.
  - `// NOPMD immutable after init` (CH2) и `// NOPMD allocation depends on loop iteration` — свести к одному стилю формулировок.
- [ ] Цель: после рефакторинга повторить `./gradlew pmdMain` (0) + `checkstyleMain` (0) + `test`.

## 34. Ручное тестирование в игре — провода и экраны (2026-08-19)

После фиксов энергии кабеля, аудита блоков и рефакторинга монитора нужно проверить в игре:

- [ ] **Кабель/энергия** — цепочка из 3+ кабелей до генератора и потребителя: энергия должна доходить до последнего
  (фикс «пинг-понга»); перезапуск мира — сеть не должна потерять энергию; IC2-EU-мост (если есть).
- [ ] **Свитч (NetworkSwitch)** — загрузка чанка со свитчем не должна крашить сервер (фикс пустого `adj`-списка);
  несколько клиентов через свитч — кадры ходят, без спама пустых кадров.
- [ ] **NetworkConnector** — соединение через коннекторы и разрыв при ломании (фикс пустых кадров `frame.length > 0`).
- [ ] **PCI Card Cage** — установка карты включает потребление энергии, `has_energy` в UI обновляется на сервере.
- [ ] **Bundled Redstone** — запись/чтение bundled-сигнала на одной и той же грани (фикс `.getOpposite()`);
  проверить все 4 горизонтальные грани при FACING != NORTH (см. §32 про индекс сторон — известный открытый вопрос).
- [ ] **Монитор** — отрисовка кадра не должна «пережёвывать» CPU без изменений (фикс `dirtyLines.clear()`);
  текст терминала рендерится корректно; мультиблок монитора (объединение/разъединение/ломание) работает.
- [ ] **Спикер** — новая текстура/модель в стиле Charger отображается со всех сторон.

## 35. Terminal follow-up PRs (из ревью PR #10, 2026-08-21)

Follow-up'ы из ревью `pr/screen-features` (PR #10). Мелкие, изолированные, ревьюятся за 10 минут. Всё на ветке `work`.

- [ ] **CH10/CH11 → новые buffer-хелперы + убрать `System.out.println`** (маленький)
  - `escapes/csi/CH10.java` (DCH) и `escapes/csi/CH11.java` (ICH/SL) оставлены на inline-реализациях сдвига — в `TerminalBuffer` уже есть `deleteChars`/`insertChars`. Перевести на хелперы (убрать две параллельные копии логики).
  - `client/gui/widget/terminal/TerminalMouseHandler.java:82` — `System.out.println("ERR: Unsupported primary mode")` в продакшн-коде → логгер.
  - Автор PR #10 согласен открыть этот PR (в ответе на ревью: «I'd rather not expand the port's blast radius. Happy to open a follow-up PR»).

- [ ] **DEC Special Graphics рендер** (средний, ~100 строк + тесты)
  - `drawingMode`/`SPECIAL_GRAPHICS` парсится (`ESC ( 0`, `TerminalOutput.java:161-163`), но **не используется в рендере**: `TerminalCharRenderer.isPrintableCharacter` берёт сырой кодпоинт без трансляции DEC-графики (`0x6A`→`─`, `0x71`→`─`, `0x71`→`┘` и т.д.).
  - Результат: +vttest suite 2 (charsets), рамки в ncurses-приложениях (vim/top/mc).
  - Проверить после: `ESC ( 0` + box-drawing в vttest suite 2.

- [ ] **DECSLRM + DECSTR** (средний)
  - `DECSLRM` (left/right margins, `CSI Pl;Pr s` — в `CH6.java:24` стоит `LOGGER.warn("DECSLRM not implemented")`) — нужен tmux / вертикальные сплиты vim. Пересечение с `DECOM` и `DECLRMM`/`DECRLM`.
  - `DECSTR` (soft reset, `CSI ! p` — `CH5.java:21` warn) — сброс таблиц режимов без полного RIS (курсор/тэбы/скролл-маргины сохраняются).
