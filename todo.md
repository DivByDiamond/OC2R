# TODO

## 0. Рефакторинг: структура и SOLID/KISS/DRY ✅ — DONE

**Правило**: ≤200 строк на файл, ≤4 файлов на папку. Выполнено полностью (~50 вынесений:
Terminal → buffer/writer/scrolling/shifter/IO/output/render/colors, Network, ComputerBlockEntity,
Robot, MonitorBlockEntity, BusCableBlockEntity и т.д.; раскладка всех больших папок по подпапкам).
Детали — в git-истории до 2026-08.

### Остатки
- `common/vm/terminal/escapes/csi/` (31) — не трогать, каждый CSI handler свой файл по дизайну
- `common/blockentity/network/` (25) → ✅ разбито: cable/, connector/, switches/, hub/, vxlan/

### Deferred
- jcodec/ — **попытаться заменить на Maven dependency org.jcodec:jcodec**
- api/ — **не трогать**

---

## 6. Terminal DynamicTexture rendering ✅ — DONE

- [x] `TerminalTextureRenderer` + `TerminalTextureBuilder` + NEAREST filtering + LOD (одна текстура на всех дистанциях)
- [ ] Увеличить область на блоке с 12×7 до 14×10 или 16×9 px

---

## 7. Projector rendering improvements

- [ ] Gamma correction после YUV→RGB конвертации
- [ ] Попробовать YUV444 вместо YUV420 (менее размытые цвета)
- [ ] Увеличить depth map с 256×256 до 512×512

---

## 8. Screen↔Container auto-регистрация ✅ — DONE

- [x] Создан `ScreenRegistry` (DSL поверх `RegisterMenuScreensEvent`), `Containers.registerScreens` переведён на него

---

## 9. Lint и статический анализ ✅ — настроен

- [x] PMD (multithreading/codestyle/design, обнулён), Checkstyle 0, SpotBugs 6.5.10/4.10.3
  (html+xml отчёты), Error Prone 5.1.0 + error_prone_core 2.50.0 (выборочно,
  `-PenableErrorProne`, warnings-only; Guava-шейдинг против mixin-процессора),
  AvoidDuplicateLiterals, AvoidInstantiatingObjectsInLoops, Qodana 2026.2.0 (`./gradlew qodana`)
- [ ] Остаточные вёрдлы в отчётах (`build/reports/*`): SpotBugs ~453 main (EI_EXPOSE_REP2 99,
  MS_CANNOT_BE_FINAL 71, ...), Error Prone ~100 — разбирать точечно при рефакторинге

---

## 10. Тесты — ✅ расширены

- [x] inet/: Ipv4Space(+Extended), IntegerSpace(+Extended), TcpHeader, InetUtils,
  Rfc1071Checksum, ArpProtocol, DefaultNetworkLayer(12), MacAddressUtils(7),
  IcmpHandler(4), SessionManager(6); FrameChunkerTest, FrameCodecTest
- [x] terminal/: TerminalBufferTest (62), SGRTest (17), SGRColorParserTest (8)
- [ ] Пробелы терминала — см. §36 «Тесты»

---

## 11. ~~jcodec → Maven dependency~~ — отменена

Делаем задачу 18 (полное удаление jcodec, видеопайплайн на raw RGB) — Maven-зависимость не нужна.

---

## 12. Мультимонитор: фрагментная модель (как в OpenComputers) — код готов

**Цель**: заменить OBJ-модель монитора на фрагментные JSON-модели + кастомную BakedModel.
Выполнено: 48 фрагментных текстур, config monitorMaxWidth/Height, ModelProperty,
`MonitorBakedModel`, `MonitorMerge`/`MonitorBreak`/`MonitorRepartition`
(сборка полного прямоугольника в любом порядке, ломание одного блока → переразметка).

- [ ] Build + проверка в игре мультимонитора разных размеров (сборка 2×2/3×3 в любом порядке, ломание одного блока → переразметка)

---

## 13. Переписывание проводов (энергия FE+EU + починка коннекции) ✅ — DONE

Кабель = энергопроводник: FE через `IEnergyStorage`, EU (IC2) через `Ic2EuBridge`/`EuEnergyAdapter`
(4 FE = 1 EU). Config `cableEnergyCapacity`/`cableEnergyTransferPerTick`.
`EnergyTransferManager` — сетевое распределение раз в тик (чинит «пинг-понг»).
Творческий блок = `InfiniteEnergyStorage`. Автоконнект + единая `recomputeConnections()`.

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

Лицевая сторона = направление блока при установке. ПКМ по не-лицевой → GUI инвентаря
компонентов (`ComputerBlockInteraction.useWithoutItem`); ПКМ по лицевой → терминал;
запуск shift+ПКМ или `PowerButton`; POST-бипы при ошибках запуска
(`ComputerPost`, 5 сигналов: firmware/энергия/CPU/память/unknown, хук `ComputerVirtualMachine.handleBootErrorChanged`).

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

Мягкий рестарт через `stop(); start();`: `AbstractVirtualMachine` —
`AtomicBoolean devicesChangedWhileRunning` + `deviceChangeRestartDelay` + `markDevicesChanged()`;
`tick()` откладывает рестарт на 2 тика (пакетирование серийных подключений).
`VMLifecycle.handleDevicesAdded/Removed` — только add/remove + mark; из `handleBeforeDeviceScan`
убран переход `RUNNING → LOADING_DEVICES`. RPC-устройства хот-плажатся живым адаптером без рестарта.

---

## 18. Убрать jcodec → прямой RGB-буфер монитора/проектора

Кадр = готовый RGB565-буфер as-is, клиент рендерит через DynamicTexture (NEAREST).
Протокол: `(pos, codec, width, height, frameSize, chunkIndex, chunkCount, data)`,
чанки по 256 KB (`FrameChunker`). Сервер: throttle `Config.monitorFps`, гейт на вотчеров.
Клиент: Reassembler → R5G6B5→ABGR → `DynamicTexture.upload()`, без decoder pool.
jcodec/балансировщики/worker pools удалены. Build зелёный (compileJava + test).

- [ ] Проверка в игре (несколько мониторов + проектор одновременно)

---

## 26. Видеокодек: RAW/H.264 переключаемый ✅ — DONE

Конфиг `videoCodec` (`raw` по умолчанию / `h264`) в `GameplaySpec`. Вендоренный jcodec
восстановлен (`li.cil.oc2.jcodec.*`, «Vendored from JCodec 0.2.5», lint-exclude вернуты,
референс в `ref/jcodec/`). `FrameCodec` (`common/vm/video/`): stateful энкодер/декодер,
YUV420↔RGB565, deflate/inflate; ручной StreamCodec (8 компонентов); Reassembler по явному
frameSize; фолбэк на RAW при BufferOverflow декодера. Тесты: FrameCodecTest + FrameChunkerTest.

- [ ] **Проверка в игре**: монитор + проектор на `videoCodec=raw` (дефолт) и `videoCodec=h264`; замер трафика (h264 должен быть в разы меньше)

---

## 19. Terminal: diff вместо сырого UART на клиенте — код готов

**Проблема**: сервер гонит на клиент *сырой байтовый поток* UART (эскейп-последовательности), и клиента парсит VT100 заново. Term-инстанс общий на сервер+клиент; при перезагрузке VM не очищается; каждый тик шлются байты.

**Реализовано (2026-08-23):**
- Сервер — единственный владелец состояния: VT-парсинг остался только на сервере (`TerminalOutput`).
- Dirty-синк: `Terminal.networkDirtyRows` (BitSet абсолютных buffer-row + флаг full-refresh) заполняется из `markDirty`/`markAllDirty`; `setChar`/шифтеры/`TerminalBuffer` роутед через `terminal.markDirty` (были прямые вызовы renderers).
- `TerminalDiff` (`common/vm/terminal/TerminalDiff.java`): снапшот = reset-флаг + width + alt-buffer + изменённые строки (ячейка = codepoint + 2×ColorData(R,G,B,mode) + style, палитрные режимы не сплющены) + курсор + окно scrollback + cursorMode/DECTCEM/bell + битмаска 14 input/render-режимов (DECSCNM, APPLICATION_SYNC, DECCKM, mouse*, bracketed paste, focus, application ESC). Ручной StreamCodec.
- Сообщения: `ComputerTerminalDiffMessage` / `RobotTerminalDiffMessage`, старые `*TerminalOutputMessage` удалены вместе с регистрацией; клиентский `io.putOutput(UART)` больше не существует.
- Сброс при рестарте VM: `TerminalUtils.resetTerminal` = `RIS.execute` + `captureFull` (заодно убран мёртвый литеральный `'J'` и статический буфер — фикс m8 из §36).
- Клиент (`TerminalDiff.apply`): пишет строки в локальный буфер, синкает alt-buffer/cursor/modes, `markAllDirty()`; рендер перестраивает только dirty-строки как раньше. Ввод не тронут (клавиатура → putInput → poll → C2S message).

**Совместимость**: серверный VT100-парсинг не менялся ни на символ — поведение терминала (vttest) сохранено by construction; дифф только переносит уже распарсенное состояние.

### Осталось проверить
- [ ] Проверка в игре: GUI компа + робота, два клиента рядом с одним ПК (tracking-chunk рассылка), скроллбек мышью, alt-screen приложения (vim/less), мышь в mc/midnight, bell.
- [ ] Прогнать vttest внутри гостя (серверный парсер не трогали, но убедиться после рефакторинга dirty-роутинга).

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

Готово: RPC `beep`/`playTone` (клиентский синтез синуса, `ToneAudioStream`),
PCM-стриминг `write(byte[])` (ring buffer + `StreamingPcmSoundInstance`, stop по тишине),
блок Speaker (RPC, автоконнект к кабелю), POST-бипы запуска, конфиг кулдауна.

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

**Готово**: `GPUItem`/`GPUDevice`/`GPUItemDeviceProvider` (4 тира, слот, крафты, модели, lang),
конфиг `gpuEnergyPerTickTier1..4`. Провайдер framebuffer уже размерно-независим
(`fb.getWidth()/getHeight()`, stride = width × 2).

| Тир | Разрешение | Текстовый режим | Описание |
|---|---|---|---|
| GPU T1 | 320×200 | 80×25 | Базовый, крафт из железа/редстоуна |
| GPU T2 | 640×400 | 160×50 | Средний, золото/лазурит |
| GPU T3 | 1024×768 | 256×96 | Продвинутый, алмазы |
| GPU T4 | 1920×1080 | 320×135 | Эндгейм, незерит/эмеральды |

- [x] **Интеграция с монитором**: `MonitorDevice` спрашивает у bus-контроллера есть ли GPU → если нет, framebuffer не монтируется (чёрный экран). Если есть — `SimpleFramebufferDevice(width, height)` из GPU (`MonitorGpuLink` + afterDeviceScan-листенер в `ComputerBlockEntity`; blob пересоздаётся при смене разрешения)
- [x] **Убрать хардкод 640×480**: сервер (`MonitorVideoController`) и клиент (`RenderInfo` пересоздаёт DynamicTexture по размеру кадра, `MonitorTextRenderer`/`MonitorDisplayWidget` берут разрешение последнего кадра) — размеро-независимо
- [x] **Без GPU → UART-терминал**: монитор не показывает framebuffer, но текстовый терминал (UART) работает
- [ ] Build + проверка в игре (монитор с GPU T1/T2/T3/T4, без GPU — чёрный)

---

## 24. CPU: конфиг частот, новые тиры, губернаторы — код готов

Готово: новые тиры CPU (50/100/200/400/1000 MHz), конфиг `cpuFrequencyTier1/2/3/4`
в `GameplaySpec` (TODO из `Config.java:12` убран), `Config.vmTimeQuotaMs` вместо
захардкоженных 25 ms, cap `cycleLimit` (≤ 2 тиков вперёд), крафты обновлены.

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
- [x] **Login incorrect при входе root** — исправлено в OnyxKernel/init/src/login/mod.rs (2026-08-23):
  терминал OC2R шлёт Enter как `'\r'` (TerminalInput.java:15), а login в raw-режиме (TIOCSRAW)
  обрезал только `'\n'` → пароль уходил на хэш с хвостовым CR. Теперь стрипаются оба
  (username + password); su.rs/passwd.rs уже умели. Дополнительно (2026-08-23): root сеется
  с ПУСТЫМ паролем (`seed.rs`), login принимает голый Enter как пустой пароль (`pn==0`
  больше не отклоняется) — вход как в Minux. Пересобрать образ: `cargo ibuild` +
  OnyxOS `scripts/mk-onyxfs-disk.sh`; на существующем диске с неизвестным паролем —
  перезалить свежий образ (first-boot пересеет root).
- [ ] **`boot_smode.rs`** (cfg-гейт, как `boot_32.rs`): вход из OpenSBI в S-mode — принять `a0`/`a1`, пропустить PMP/medeleg/mideleg (OpenSBI уже сделал), `stvec`/`sepc`/`sstatus.SPIE`, `sret` → `kmain`. ~50 строк asm
- [ ] **Сеть**: убрать хардкод `[10,0,2,15]`; DHCP или адрес из FDT/конфига
- [ ] Проверить: UART NS16550A (совместим с sedna), virtio_net, virtio-blk, libfdt — что FDT от sedna парсится `early_init`

### ОнyxKernel — вывод на монитор (сделано 2026-08-23)
- [x] **Поддержка мониторов OC2R**: ядро раньше рисовало в приватную RAM (fallback) — хосту не видно.
  Добавлено: `libfdt/fdt/framebuffer.rs` ищет `/chosen/simple-framebuffer` (compatible/reg/width/height/stride,
  MMIO ниже 0x80000000); `fb::init_device()` принимает MMIO-геометрию; `put_pixel` умеет r5g6b5 (16bpp LE);
  `draw`/`writer`/devfs переведены с констант FB_* на динамические размеры; `srv/main/display.rs`
  пробует FDT-fb первым, RAM-fallback остался для QEMU. Пересобран `onyx-kernel.bin`
  (`--features smode` + objcopy), заменён в ресурсах мода.

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

- [x] **getInput() щёлкает вид вниз без пометки dirty** — закрыто аудитом №2 (2026-08-23):
  реализовано в `[TerminalIO.java:43-49]` — `lastRowToDisplay` + `markDirty` всех строк.

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

## 36. Аудит VT100-терминала №2 — 6 суб-агентов (2026-08-23, ветка 1.21.1)

Повторный аудит `common/vm/terminal/**` (74 файла): архитектура, логика VT100/xterm, потокобезопасность, стиль/сборка, тесты, межмодульные контракты. Все находки верифицированы по исходникам; блокер Б1 подтверждён трассировкой вручную. Формат: `[файл:строка]`.

### Блокеры

- [x] **Б1 — AIOOBE при SD/RI на заполненном scrollback**
  `[buffer/TerminalBufferScrolling.java:77-81]` + `[buffer/TerminalLineShifter.java:101]`
  Ветка `shiftDown` при полных маргинах использует `lastRowToDisplay` (не Max!):
  `firstLine = L−24, lastLine = L−1`; при `L == HEIGHT*SCROLL_BACK_COUNT == 480`:
  `srcIndex = 456w, charCount = 24w, dstIndex = 457w` → arraycopy пишет до `481w−1`
  при длине массива `480w`. Гарантированный ArrayIndexOutOfBoundsException под lock
  в `TerminalOutput.putOutput` → терминал навсегда перестаёт обрабатывать вывод.
  **Репро** (только вывод гостя): `\n` × ~470 (scrollback до 480 через
  `incrementLastLineToDisplay`, cap `TerminalBufferScrolling.java:20-23`), затем `ESC[2T`
  или `ESC M` при курсоре в верхней строке (RI).
  Попутно: та же ветка семантически неверна при прокрученном назад view — сдвигает
  не окно рендера (`lastRowToDisplay` вместо `lastRowToDisplayMax`, как во всех остальных путях).
  Фикс: клампить как остальные пути (`lastRowToDisplayMax − HEIGHT .. lastRowToDisplayMax − max(count,1)`)
  + тест: полный scrollback → `CSI 2 T`.

- [x] **Б2 — Freeze/DoS терминала через SU/SD с большим счётчиком**
  `[escapes/csi/CH8.java:36-43]`, `[escapes/csi/CH9.java:33-40]`
  `for (int i = 0; i < args[0]; i++) shiftUpOne();` — `EscapeUtilities.parseArgument`
  сатурирует на `Integer.MAX_VALUE` → `\033[2147483647S` из гостя = 2³¹−1 итераций
  по 4 arraycopy каждая, всё под lock внутри `putOutput` → поток VM-вывода заморожен
  на минуты, lock удержан. Остальные хендлеры аргументы клампят — проблема локализована.
  Фикс: `int n = Math.min(args[0], Terminal.HEIGHT);` перед циклом.
  Тест: `\033[999999999S` завершается мгновенно.

### Major

- [ ] **M1 — `ESC ) Ps` (designate G1) пишет в G0; G1 недостижим**
  `[TerminalIO.java:121-122,156-167]` — оба состояния `'('` и `')'` попадают в один
  handler, всегда модифицирующий `drawingModeG0`; `drawingModeG1` пишется только
  в RIS/DECSC/DECRC. Репро: `\033)0` + SO → ASCII вместо псевдографики.
  Заодно: `A`/`1`/`2` молча игнорируются (`TerminalOutput.java:159,164-166`);
  `useG0`/`drawingModeG1` — состояние без эффекта (рендер читает сырой кодпоинт,
  см. §35 DEC Special Graphics).

- [ ] **M2 — обрезанный true-color SGR превращается в стили**
  `[escapes/csi/SGR.java:41-49]` — malformed-ветка пропускает только селектор+mode-byte:
  `\033[38;2;1m` → остаток `1` применяется как bold;
  `\033[48;2;10;20m` → смена foreground кодом 10. Ожидание (xterm): неполная цветовая
  группа игнорируется целиком. Для обрезанного `38;5` recovery корректен (проверено).
  Фикс: пропускать селектор + все аргументы до следующего селектора.

- [ ] **M3 — overflow `1 << dirtyLine` при выводе в свёрнутый scrollback**
  `[buffer/TerminalBufferWriter.java:82-87]`, `[TerminalBuffer.java:236]` —
  `dirtyLine = offset + y` достигает 479 → int-сдвиг mod 32 → нужная видимая строка
  не помечается (рендер сканирует биты 0..23, `[TerminalRenderer.java:158-159]`),
  случайные биты мусорят. Эффект: при скролле вверх свежий вывод не перерисовывается
  до следующего full-redraw. Фикс: AtomicLong + `1L <<`, либо clamp + `markAllDirty()`
  при offset > 0. Безопасны: alt-buffer (y ≤ 23) и режим «внизу» (offset = 0).

- [x] **M4 — рендер читает буфер без лока, Netty пишет под `io.lock`** — закрыто задачей 19
  (2026-08-23): клиент больше не парсит UART, дифф применяется на main-thread;
  `[TerminalCharRenderer.java:26-27,47]`, `[TerminalBackgroundRenderer.java:26,60-62]`
  vs бывший писатель `[ComputerTerminalOutputMessage.java:45 → putOutput]` (удалён).
  `setChar` = 4 отдельных store (char/color/bg/style, `[TerminalBufferWriter.java:42-79]`),
  прокрутка = 4 несмежных arraycopy (`[TerminalLineShifter.java:101-109]`) — рендер между
  шагами видит смесь. Эффект: «цветной шлейф» за текстом, атрибуты с опозданием,
  полусдвинутые строки; само сходится после burst. Отдельный тяжёлый случай:
  DECCOLM `setWidth` перевыделяет массивы (`[Terminal.java:173-176]`) на Netty-потоке —
  возможен AIOOBE прямо в кадре рендера (устаревшая ссылка + новый width).
  Фикс-варианты: снапшот под лок / версия-счётник + retry кадра / enqueueWork для putOutput.

- [ ] **M5 — `fonts/FontHandling`/`UnicodeFontRenderer` без `@OnlyIn(CLIENT)`**
  `[FontHandling.java:10]` — статическая инициализация → `new FontAtlas(1024,1024,...)`
  → `Minecraft.getInstance().getTextureManager().register(...)` (`[FontAtlas.java:35]`).
  Сейчас безопасно случайно: единственные потребители — `@OnlyIn(CLIENT)`-рендереры.
  Любой случайный импорт из common-кода уронит dedicated server при загрузке класса.
  Фикс: аннотировать оба класса (или вынести загрузку из static-init).

- [ ] **M6 — `SCROLL_BACK_COUNT` — public mutable поле в UPPER_CASE**
  `[Terminal.java:48]` `public int SCROLL_BACK_COUNT = 20;` — читается в индексной
  арифметике (`setWidth:172`, `TerminalBufferScrolling:23,56,68`, `CH8:32`); любое внешнее
  изменение после аллокации буферов → рассогласование размеров → AIOOBE.
  Фикс: `public static final` (или private+getter, если нужна конфигурация — тогда через пересоздание буферов).

### Minor

- [ ] **m1 — DECRC/restoreSavedCursor не клампят координаты после смены ширины**
  `[escapes/DECRC.java:11-12]`, `[escapes/csi/CH3.java:73-79]` — ESC7 в 132 колонках
  на колонке 100 → `?3l` (setWidth делает home, но не сбрасывает savedX) → ESC8 даёт
  `x=100 > 79` → ложный перенос на следующем символе. Фикс: clamp при restore.

- [ ] **m2 — CPR сообщает колонку width+1 (нет pending-wrap флага)**
  `[escapes/csi/DSR.java:22-31]` — `x+1` без clamp; состояние `x == width` легально
  (`putChar:31`). Системный артефакт модели немедленного переноса: ECH/DCH/ICH при
  `x==width` no-op, HTS молча теряет tab stop последней колонки. Полноценное решение —
  pending-wrap флаг вместо `x == width`.

- [ ] **m3 — `CSI 3 J` (erase scrollback, xterm E3) молча игнорируется** `[escapes/csi/ED.java:26-39]`

- [ ] **m4 — `CSI n` без параметра не отвечает** `[escapes/csi/DSR.java:17]` —
  ECMA-48: отсутствующий Ps = 5; гостевое приложение может зависнуть в ожидании `\033[0n`.

- [ ] **m5 — режим 1048 сохраняет только x/y, restore идёт полным DECRC**
  `[escapes/csi/CH2.java:110-113]` — восстановление перезапишет стиль/цвета/charset
  init-дефолтами. Смежное: SCOSC (`CH6 's'`) в alt-буфере пишет в main-слоты savedX/savedY,
  а DECRC в alt-буфере читает altSaved* — сохранение «в никуда».

- [ ] **m6 — клавиатура ставит байты в очередь при выключенном capture**
  `[TerminalKeyboardHandler.java:29-41]` — проверка только для ESC; нажатия утекают в VM
  вне фокуса терминала (`MachineTerminalWidget.tick` отправляет безусловно).

- [ ] **m7 — палитра xterm-256 вне канона**
  `[color/TerminalColors.java:46,49-52]` — компонента `df` вне набора {00,5f,87,af,d7,ff}
  (`0xdfaf*`, `0xff**df`) — похоже на опечатку `d7→df`. Сверить с эталоном xterm-256.

- [x] **m8 — `TerminalUtils.resetTerminal`: статический мутабельный ByteBuffer + голый 'J'**
  `[util/tick/TerminalUtils.java]` — переписан задачей 19 (2026-08-23): RIS + full snapshot,
  без статического буфера и литерального `'J'`.

- [ ] **m9 — DCL без volatile в `Terminal.client()`**
  `[Terminal.java:131-132,266-281]` — формально data race по JMM; спасает final-поле
  `TerminalClient.terminal`. Станет багом при добавлении любого нефинального поля.
  Фикс: `private transient volatile TerminalClient clientInstance;`

- [ ] **m10 — `lastRowToDisplay/Max` — plain int-пара с тремя писателями без синхронизации**
  Netty (IND/NEL) / main (mouseScrolled, getInput) → редкие «прыжки» окна просмотра истории.
  Плюс `hasPendingBell` — plain boolean (Netty пишет, main читает) → потеря звонка.

- [ ] **m11 — dead code**: `Utf8Decoder.hasActiveSequence()` (0 ссылок),
  `TerminalIO.putOutput(byte)` (все вызовы через ByteBuffer),
  `Terminal.getTerminalWidth()` (только тесты — закрепить API или убрать),
  `incrementLastLineToDisplay(true)` (ветка никем не вызывается);
  `TerminalRenderer.findLineIndex` / `TerminalCharRenderer.isPrintableCharacter` → private;
  `ImplementedPrivateModes.modeStatus` public static mutable → private.

- [ ] **m12 — дубли магических чисел**: цикл `i <= 23` ×3 (`TerminalBufferScrolling:36,46`,
  `TerminalIO:46`) → константа `FULL_DIRTY_MASK = (1 << HEIGHT) - 1`;
  blink `1000/500` ×4 (`TerminalRenderer:42,74`, `TerminalCharRenderer:36`,
  `TerminalBackgroundRenderer:35`) → именованные константы фазы;
  `% 8` вместо `TerminalColors.TAB_WIDTH` (`CSIManager.java:94`);
  номера mouse-режимов числами при существующих `PrivateMode.*` (`PrivateModeState:105-113`).

### Nit

- [ ] HT внутри CSI игнорирует tabs[] (`CSIManager.java:93-95` — фиксированные `% 8`)
- [ ] `ESC # 8` (DECALN) не сбрасывает маргины и не делает home (`TerminalIO.java:170-186`)
- [ ] DL предочистка `clearLine(y+i)` избыточна (перезаписывается сдвигом) `[DL.java:29-32]`
- [ ] `putResponse(String)` — N полных lock/unlock на байт; ответ не атомарен относительно readInput
- [ ] reentrant-запахи: вложенный lock в `putInput(String)/putInput(char)` (`TerminalIO:63-71,95-104`)
- [ ] разнобой `//` vs `/* */` (DSR/SGR/SGRColorParser/CH1/CSIManager/DA vs остальное); 7 строк >120 (LineLength подавлен)
- [ ] устаревший чекбокс выше (§31 «getInput без dirty») — уже реализовано в `TerminalIO.getInput():43-49`, закрыть
- [ ] XTVERSION-версия захардкожена `oc2rvt(1.0.0)` (`CH7.java:21`)
- [ ] RIS не сбрасывает transient `hasPendingBell` (остальное сверено — RIS полон)

### Потокобезопасность (сводка)

Write-path НЕ однопоточный: сервер — VM Runner (output) + Netty (input); клиент — Netty
(мутирует экран) vs Render/main (читают/скроллят без лока). Input-очередь защищена
корректно (единый `io.lock` на все offer/dequeue). `renderers` — synchronizedSet + AtomicInteger,
корректно. Основные риски — M3/M4/m9/m10 выше.

### Архитектура

- [ ] Циклы пакетов (5): terminal↔buffer↔escapes (полный треугольник — нельзя вынести ни один пакет), terminal↔modes, terminal↔render
- [ ] `Terminal` god-объект: ~60 публичных mutable-полей, вся логика модуля мутирует напрямую (вне модуля мутаций нет — потребители ходят через io/bufferManager)
- [ ] Dirty-логика размазана по 6+ местам data-слоя (Terminal.markDirty, TerminalBuffer.markDirty, TerminalBufferWriter.setChar, TerminalLineShifter ×2, TerminalIO.getInput, TerminalBufferScrolling ×2) — единая точка расчёта screen-row ↔ buffer-row
- [ ] 22 поля saved*/altSaved* копируются вручную в DECSC/DECRC/RIS → объект-снимок CursorSnapshot
- [ ] `Terminal` содержит @OnlyIn(CLIENT)-методы + ленивый TerminalClient — state знает про клиент

### Тесты (пробелы)

Текущее покрытие плотное: 87 тестов / ~367 assertions (SGRTest 17, SGRColorParserTest 8,
TerminalBufferTest 62 — интеграционные через реальный Terminal + полный escape-путь).
Не покрыто:

- [ ] `Utf8Decoder` — 0% (единственный файл без тестов): мультибайт, обрыв sequence между чанками, invalid continuation, 4-байтовые
- [ ] CSIManager на мусорном входе: >10 аргументов, CAN/SUB abort, control chars внутри CSI
- [ ] CUU/CUD/CUF/CUB — ноль тестов (самые частые последовательности ncurses!)
- [ ] Ответные DSR/DA (формат ответа в input-очереди) + табуляции HTS/TBC + интеракция tabs с DECCOLM
- [ ] OSC/DCS/APC менеджеры (терминация ST/BEL) + семантика выхода ?1047l
- [ ] Регрессии на Б1/Б2 (см. выше) — закрываются одним параметризованным тестом

### Опровергнутые гипотезы (проверено — корректно, не чинить)

Stale-args между CSI (reset на `[` и CAN/SUB); null-чтение ColorData до SGR (RIS в конструкторе);
OOB в clearChars/deleteChars/insertChars (clamp доказан); Math.clamp min>max (guard setWidth);
DECSTBM/CUP/HVP/DECOM; SGR 38/48 consumption на валидных входах; shiftLines при count>1 из IL/DL
(кламп к региону); dirty-mask формулы записи/чтения взаимно обратны; input-очередь (единый лок);
displayOnly соблюдается всеми производителями ответов; IRM ?4h реализован; RIS полон по сериализуемым полям.

---

## 37. Комплексный аудит — 6 суб-агентов (2026-08-25, ветка master, HEAD 2a6b185)

Аудит всего мода (не только терминала): структура/архитектура, логика, потокобезопасность,
стиль/сборка, тесты, контракты/безопасность. Все блокеры верифицированы чтением исходников.
Issue #17 (mount `/mnt/builtin`) можно закрывать — фикс в образе 0.0.72-oc2r1 задокументирован
в docs/BUILDROOT.md.

### Блокеры

- [ ] **Б1 — MessageUtils.withNearbyServerBlockEntity: нет проверки дистанции** `[network/util/MessageUtils.java:31-46]`
  Проверяется только существование чанка; ни `distanceToSqr`, ни прав на блок (сравни:
  `withNearbyServerEntity` честно делает `entity.closerThan(player, 8)`).
  Клиент шлёт `OpenComputerTerminalMessage`/`ComputerPowerMessage`/`KeyboardInputMessage`
  с любым BlockPos загруженного чанка → открытие терминала/выключение/ввод клавиатуры
  ЧУЖОГО компьютера на дистанции. Фикс: `pos.closerToCenterThan(player.position(), LIMIT)`.
- [ ] **Б2 — Инъекция файлов между игроками через импорт**
  `[network/message/file/ImportedFileMessage.java:35-37]` — handler не сверяет отправителя с
  `request.PendingPlayers`, без лимита размера, без санитизации имени; id последовательные
  (угадываемые). Плюс гонка: `[ImportFileRequestManager.java:22-24]` — `nextImportId++`
  ВНЕ lock (registerRequest = server thread, setImportedFile = Netty) → коллизии id,
  потеря импорта. Фикс: авторизация по PendingPlayers + AtomicInteger/lock + лимит + sanitize.
- [ ] **Б3 — TcpHeader.read: бесконечный цикл / отмотка позиции → DoS сетевого потока**
  `[inet/tcp/TcpHeader.java:33-36,70-73]` — нижней границы dataOffset нет (`> limit` только
  сверху); неизвестная опция с length 0/1 → `position += -2/-1` → вечный цикл чтения тех же
  байт (данные из РЕАЛЬНОГО интернета). Фикс: `if (size < 2) return false;` +
  `dataOffset >= position + MIN_HEADER_SIZE_NO_PORTS`.
- [ ] **Б4 — FrameChunker.Reassembler: нет chunkIndex >= 0 и проверки data.length**
  `[network/util/frame/FrameChunker.java:88-105]` — `chunkIndex=-1` → BitSet.get(-1);
  переполненный чанк → AIOOBE в arraycopy; `frameSize=Integer.MAX_VALUE` → OOM клиента.
  Данные приходят по сети (MonitorFramebufferMessage). Фикс: `chunkIndex >= 0`;
  `data.length == min(MAX_CHUNK_SIZE, frameSize - from)`; верхний предел frameSize.
- [ ] **Б5 — IntegerSpace.put оставляет перекрывающиеся диапазоны** `[util/misc/IntegerSpace.java:21-23]`
  Строгое `value < end` + эксклюзивный subMap: вложенный диапазон до `end` не удаляется и
  не мерджится. Проверено исполнением: put(5,15); put(0,10) → [0-10, 5-15], count()=22
  вместо 16; contains(12)=false при покрытом элементе. Через Ipv4Space ломает allow/deny
  интернет-карты. Фикс: удалять `key >= begin && value <= end`; мерджить с floorEntry(end).
- [ ] **Б6 — DECRC/restoreSavedCursor после смены ширины → AIOOBE** (апгрейд m1 из §36 до краша)
  `[escapes/DECRC.java:11-12]`, `[escapes/csi/CH3.java:76-81]` — ESC7 в 132 колонках на x=131 →
  `?3l` (setWidth(80)) → ESC8 → x=131 → index 1971 ≥ 1920 → AIOOBE под lock → терминал умирает.
  Фикс: setCursorPos/clamp по текущему width.

### Major

- [ ] RPCDeviceBusAdapter: handoff `synchronizedInvocation` VM thread ↔ server thread без
  volatile/атомарности (`bus/adapter/RPCDeviceBusAdapter.java:49,117-121`) + TOCTOU
  pause/resume↔step (:101-133) → rebuild реестра параллельно с диспетчеризацией RPC.
- [ ] InternetConnectionImpl.saveAdapterState: `.get()` на server thread при автосейве
  (`inet/internet/connection/InternetConnectionImpl.java:38`) — фриз тика + дедлок-риск.
- [ ] TerminalDiff.apply: равенство rows.length == rowData.length не проверяется нигде
  (`vm/terminal/TerminalDiff.java:205-207,313`) → AIOOBE/дисконнект клиента; clamp ширины.
- [ ] CUD/CUF int overflow при аргументе MAX_VALUE (`csi/CUD.java:17`, `CUF.java:17`) —
  курсор прыгает вверх; клампить как CH8/CH9.
- [ ] Дубликат RegistryUtils: `common/util/RegistryUtils.java` ≡ `common/util/item/RegistryUtils.java`,
  обе живые, раздельная статика → оставить одну.
- [ ] System.out в проде (~14 мест): ConfigManager.java:21, VxlanBlockEntity.java:102,
  SwitchLog.java:28-51, TerminalMouseHandler.java:82,146, ByteBufferFlashStorageDevice.java:111,
  PciRootPortDevice.java:54-78 → SLF4J.
- [ ] Сборка: ContainedDeps ссылается на несуществующий commons-collections4 (build.gradle.kts:331);
  дрейф sedna-buildroot 0.0.70 vs 0.0.72-oc2r1 (gradle.properties:20 vs settings.gradle.kts:30);
  architectury/markdownmanual дважды на classpath (fileTree libs + maven).

### Minor / потокобезопасность (кратко)

AsyncExecutorHelper.shutdownNow прерывает чужие ForkJoinPool-потоки (:88-104); GlobalInterruptController
неатомарный RMW маски прерываний; Terminal DCL без volatile (= m9 §36); TaskImpl.closed/SocketManager
refcount без атомарности; MultipartMessage: нет лимита параллельных потоков на соединение;
ExportedFileMessage: имя из гостя попадает в путь клиентского диалога; NativeLoader пишет native lib
в предсказуемый путь (user.dir) вместо temp-dir; скачивание natives без SHA-256; все линтеры advisory-only
(ignoreFailures=true); CSIManager:97 табуляция игнорирует tab stops; OSC/DCS/APC не прерываются CAN/SUB;
dead code: ColorUtils, RunnableUtils.doNothing, SessionOperator; api→common инверсия (6 файлов api/inet/**);
common→client перекрёстные импорты (27 файлов); God-класс Terminal.java (358 строк/32 метода).

### Тесты

18 файлов / ~150 методов / ~660 assertions, пустых нет. НЕ покрыто (high): tcp/state/* (машина состояний!),
serialization/*, InternetConnectionImpl/StreamSessionImpl/TunnelManager, robot/*, Utf8Decoder,
csi-handlers кроме SGR. GameTest'ов нет.

### Опровергнутые гипотезы аудита (не чинить)

RPC Gson → произвольный вызов невозможен (MethodInvoker только зарегистрированные группы);
BlobStorage path traversal исключён (UUID-пути); Utf8Decoder/Rfc1071Checksum/TerminalLineShifter/
BlockOperationCooldown — корректно; секретов/ProcessBuilder/eval нет; CH1..CH11/NullLayer/ICMPReply — живые.
RobotActionProcessor из §28, похоже, уже залочен (проверить и закрыть пункт §28).


---

## 38. Перф-аудит сети/мониторов/шины — 4 суб-агента (2026-08-25)

Симптом от игрока: «Network performance is really bad still». Найдена комбинация из 8 узких мест.
Суммарный потолок интернет-карты сейчас ≈ 13–30 КБ/с с коллапсом при потерях; после фиксов 1–3
достижимы сотни КБ/с — МБ/с.

### Интернет-карта (главный ограничитель throughput)

- [x] **П1 — один кадр на тик в каждую сторону** (DONE 2280692): PendingFrame →
  ArrayBlockingQueue(64) в обе стороны, drain-циклы в process() и processInternetAdapter.
- [x] **П2 — PendingFrame хранит ОДИН кадр** (DONE 2280692): класс удалён, тихая потеря
  устранена; тест InternetConnectionImplTest (drain без потерь, стоп при полной очереди).
- [x] **П3 (частично) — буфер ≥32К** (DONE 61fd98d): streamBufferSize дефолт 2000→32768
  (`InternetCardSpec.java` + `Config.java`). Осталось: скользящее окно + cumulative ACK
  в `EstablishedState` (один сегмент в полёте, точный ACK) — отдельный пункт ниже.
- [ ] **П3.1 — скользящее окно TCP**: `EstablishedState.java:20-24` один сегмент в полёте,
  точный ACK required (:104). Заменить nextSegmentMark на окно с несколькими сегментами
  и cumulative ACK; нужны тесты на tcp/state/* (сейчас не покрыты).
- [x] **П4 (частично) — read/write до EAGAIN** (DONE 91a4f07): цикл channel.read до EAGAIN/
  EOF/полного буфера в readSession; write докручивает sendBuffer до конца в sendStream.
  processQueue early-exit оставлен НАМЕРЕННО: Receiver несёт ровно одну сессию/буфер на
  вызов receiveSession, кадр = один сегмент — drain нескольких сессий терял бы данные.
  Дальнейший выигрыш только через мультибуферный receiver (отдельный пункт).
- [ ] **П5 — OP_WRITE копится, toWrite никогда не потребляется**: `SocketManager.java:47-49` —
  неограниченный рост ArrayDeque (утечка) + бесполезный selector-work. Фикс: убрать OP_WRITE
  из interest или потреблять очередь.
- [ ] **П6 — ping-pong серверный↔Internet-поток**: минимум 2 тика на кадр; 2 аллокации списков
  каждый тик даже при пустом списке соединений (`InternetManagerImpl.java:141-148,158`).
- [ ] П7 — per-packet/per-frame аллокации (низкий приоритет): `InternetConnectionImpl.java:53`
  new byte[] на кадр; `SendHandler.java:53-95` новые Discriminator'ы; `DefaultTransportLayer.java:60,84`
  TreeMap-lookup per-packet; `processSessionExpirationQueue` на каждое сообщение.

### Монитор/видео (CPU server thread + bandwidth)

- [ ] **В1 — deflate(BEST_COMPRESSION=9) поверх уже сжатого H264** `FrameCodec.java:56,87-99` —
  выигрыш ~0%, десятки мс CPU server-thread на кадр/монитор. ГЛАВНЫЙ CPU-killer. Фикс: убрать deflate.
- [ ] **В2 — весь энкод-путь на server thread**: RGB→YUV + software H264 в MonitorTickHandler.tick
  (`MonitorBlockEntity.java:23`, `MonitorTickHandler.java:25`, `ProjectorBlockEntity.serverTick:105`);
  throttle 1000/fps при fps=20 = 50 мс = каждый тик — не работает. Фикс: async-энкодер last-frame-wins.
- [ ] **В3 — 4MB direct buffer + энкодер на КАЖДЫЙ BlockEntity** `FrameCodec.java:20,26`,
  `MonitorVideoController.java:29`, `ProjectorFrameSender.java:31` — 20 мониторов = 80+ MB direct
  на сторону. Фикс: общий/ленивый энкодер.
- [ ] **В4 — RAW-режим = 600KB/кадр × 20fps ≈ 12 МБ/с/watcher** (`FrameChunker.MAX_CHUNK_SIZE=256KB`,
  дефолт videoCodec="raw" `GameplaySpec.java:62`). Фикс: H264 дефолтом.
- [ ] **В5 — RAW-fallback внутри H264-потока ломает декодер до IDR (5 сек артефактов)**:
  `FrameCodec.java:52-53` BufferOverflow → RAW-байты как «H264» → DataFormatException →
  референсная цепочка битая до KEY_INTERVAL=100 кадров. Фикс: сброс decoder / форс-IDR после fallback.
- [ ] **В6 — dirty-lines игнорируются**: `SimpleFramebufferDevice.copyFrame:62-83` копирует весь
  буфер и чистит все dirty — полный энкод даже при изменении одной строки. Фикс: кодировать dirty-регион.
- [ ] В7 — QP 12 vs 24: `FrameCodec.java:24` CQPRateControl(12) vs фабричный QP 24 (`H264Encoder.java:39`)
  — завышенный битрейт ×2–4. Унифицировать QP ≥ 24.
- [ ] В8 — клиентский decode на main thread: `MonitorFramebufferMessage.handleMessage:66` →
  inflate+H264+YUV→RGB+full texture upload 1.2MB (`RenderInfo.java:49-71`) — хитчи рендера.
  Фикс: decode вне render thread.
- [ ] В9 — аллокации per-кадр на клиенте: new Picture 460KB (:71), new byte[600KB] (:126),
  новый Deflater/Inflater + ByteArrayOutputStream (:87-119). Пулинг буферов.
- [ ] В10 — ProjectorFrameSender — дословный дубль MonitorVideoController: фиксы нужно вносить дважды.
  Выделить общую абстракцию.

### Синхронизация мира / трафик

- [ ] Подтверждены НЕисправленные дубли синка из §29: фасад ×3 (`FacadeManager.java:47-111`),
  коннекторы ×2 (`NetworkConnectorBlockEntity.getUpdateTag:81-86` + message :220-227),
  имена интерфейсов ×2 (`InterfaceNameManager.setInterfaceName:41-47`), флоппи/флеш ×2
  (`DiskDriveBlockEntity.getUpdateTag:133-136`), VS2-fallback шлёт ВСЕМ игрокам во всех
  измерениях (`NetworkMessages.java:39-63`), chunk==null теряет сообщение (`ComputerTerminalManager.java:102-104`),
  handleUpdateTag без requestModelDataUpdate (`BusCableBlockEntity.java:117-128`).
- [ ] **Т1 — TerminalDiff CELL_BYTES=37** (`TerminalDiff.java:30`, javadoc «17 bytes» врёт):
  эхо одного символа ≈ вся строка 80×37 ≈ 2990 Б, до 20/с → ~60 КБ/с на активный терминал.
  Фикс: упаковка ячейки до 4–6 Б (varint codepoint, палитра/RGB15, style битфлагами) или span-дельты → <10 КБ/с.
- MultipartMessage: ключ починен; MAX_PAYLOAD_SIZE=8КБ при лимите NeoForge 1МБ — импорт 512КБ = 64 пакета;
  оверхед заголовков <1% (ок). enqueueWork в SoundCardBeep/Pcm — избыточен, но не баг.

### VXLAN / шина / энергия

- [ ] **Ш1 — scan() шины каждый тик каждого компьютера**: `VirtualMachineTicker.java:23` →
  BFS до 128 элементов с новыми HashSet/HashMap (`BusElementManager.java:97-133`); scanDevices строит
  новые коллекции + два set-diff даже без изменений (`CommonDeviceBusController.java:57-58`).
  Реализовать push-based из §20 (dirty-флаг топологии) + early-exit по size.
- [ ] **Ш2 — энергосеть: BFS flood-fill + O(n²) redistribute каждый тик**:
  `EnergyTransferManager.collectNetwork:86-103` (~3000 getBlockState/тик для 500 кабелей),
  второй проход collectNetworkCables (:105-114), `redistributeDeficits:275-296` O(n²),
  getExternalEnergy capability-lookup на каждую сторону каждого кабеля каждый тик (:298-304).
  Фикс: кэш топологии с инвалидацией по neighbor-changed, redistribute раз в 20 тиков.
- [ ] **Ш3 — блокирующий UDP send в тик-треде**: `VxlanBlockEntity.serverTick:92-99` →
  `TunnelManager.java:144 socket.send(packet)` — до 32 блокирующих send/тик/хаб
  (hubEthernetFramesPerTick). Фикс: очередь отправки + sender-тред / DatagramChannel non-blocking.
- [ ] **Ш4 — System.out.printf КАЖДЫЙ тик** пока у хаба нет соседа: `VxlanBlockEntity.java:102`
  (= дубль замечания о System.out из §37, тут перф-эффект: synchronized println 20/с убивает TPS).
- [ ] Ш5 — лишние локи VXLAN: комментарий про «not thread-safe» устарел — очередь уже
  ArrayBlockingQueue(32) (`VxlanBlockEntity.java:36`); ReentrantLock менеджера сериализует все туннели
  (`TunnelManager.java:111-119,18`). Убрать оба лока.
- [ ] Ш6 — потеря пакетов при очереди 32: `TunnelManager.java:115 offer()` молча роняет при burst >32.
  Ёмкость конфигурируемой + drop-статистика.
- [ ] Ш7 — перф-мелочи: eager-конкатенация в LOGGER.debug per-packet (`TunnelManager.java:102`),
  новые DatagramPacket/header-buffer на каждый send (:131,140); TODO shutdown bg-треда (:83) — утечка порта.
- [ ] Ш8 — SessionManager per-packet: Instant.now() + TreeMap remove/put на каждый пакет
  (`SessionManager.java:56-63`); линейный проход ретрансмиттеров (:97-114). Monotonic clock + бакеты.
- ARP/ICMP/checksum/утилиты адресов — НЕ горячие (ARP-кэш на 1 запись приемлем; ICMP single-slot
  только error-path; checksum можно ускорить getLong-проходом — низкий приоритет).

### Приоритет внедрения

1. П1+П2+П3 (интернет ×10–50 суммарно) → 2. В1+В2 (CPU сервера) → 3. Ш1+Ш2 (TPS ферм) →
4. Т1+дубли синка (трафик) → 5. Ш3–Ш7, В4–В9 → 6. П4–П7, В10, Ш8.


---

## 39. Security-hardening inet + конфиги линтеров + библиотеки (2026-08-25)

### Security: строгая валидация для майнкрафт-мода (дополнение к Б1–Б4)

- [ ] **С1 — VXLAN: входящие UDP без аутентификации → инъекция кадров в мир**
  `[vxlan/TunnelManager.java:85-121]` — сокет не подключён к remoteHost, источник не проверяется;
  порт 4789 стандартный VXLAN. Любой, кто может послать UDP на порт сервера (bind 0.0.0.0 = интернет),
  инжектирует произвольные Ethernet-кадры в виртуальную сеть любого компьютера.
  Фикс: socket.connect() + проверка packet.getAddress() + pre-shared key/HMAC; VNI не единственный id.
- [ ] **С2 — VXLAN: vti=1000 захардкожен у всех блоков + грузится из NBT без проверок**
  `[VxlanBlockEntity.java:32,109-111]` — все хабы сервера регистрируют один VNI, tunnels.put()
  перезатирает чужой туннель (нарушена изоляция игроков); подделка NBT → попадание в чужую сеть.
  Фикс: случайный vti при создании (из UUID предмета), валидация диапазона при load.
- [ ] **С3 — Спуфинг srcIpAddress гостем**: `[DefaultNetworkLayer.java:117-137]`,
  `SendHandler.java:69-105` — проверяется только dst; гость назначает себе любой IP
  (`DefaultLinkLocalLayer.java:162 myIpV4Address = arpData.targetIpAddress()`).
  Через реальный интернет хоста — спуфинг-атаки/подстава IP сервера.
  Фикс: сверять src с выученным по ARP адресом карты, несовпадение — молча дропать.
- [ ] **С4 — deniedHosts без 169.254.0.0/16** (`InternetCardSpec.java:62-68`) — на публичном
  VPS гость читает cloud-metadata (IAM-токены) через 169.254.169.254. Добавить также 0.0.0.0/8,
  255.255.255.255/32, TEST-NET диапазоны. Hostname-записи резолвятся один раз при старте
  (`Ipv4Space.java:131-146`) — DNS rebinding конфигурации; документировать, рекомендовать CIDR.
- [ ] **С5 — PCM-флуд**: `SoundCardItemDevice.write:157-177` без rate-limit шлёт SoundCardPcmMessage
  всем трекерам чанка; клиентский PcmSoundBuffer — неограниченная очередь chunks. Гость циклит
  sound.write(huge) → флуд сети + heap клиентов. Фикс: bytes/tick бюджет + cap ~256КБ с вытеснением.
- [ ] **С6 — нет rate limit на MonitorRequestFramebufferMessage (:36-42)** (каждый запрос =
  сериализация+рассылка фреймбуфера) и KeyboardInputMessage (:42-48). Фикс: per-player cooldown.
- [ ] С7 — ICMP echo блокирует единственный internet-поток: `EchoHandler.java:56-67` sync sendICMP
  с таймаутом; ping «чёрных» адресов замирает весь интернет для всех карт. Лимит параллельных echo.
- [ ] С8 — NBT интернет-карты: MAC/IP восстанавливаются как есть (`DefaultLinkLocalLayer.java:60-81`)
  → impersonation другой карты в мировом LAN. Привязать MAC к UUID предмета детерминированно.
- [ ] С9 — TunnelManager надёжность: bind-fail → managerInstance==null, но поток стартует (NPE в фоне);
  while(true) без shutdown; DEFAULT_VXLAN_HOST="::1" (`Config.java:72`) — похоже на баг вместо 0.0.0.0.
- Resource-limits сводка: лимитированы VM-память/сессии/размер дисков/экспорт ≤1МБ;
  ОТСУТСТВУЮТ: bandwidth per card/tick, число карт на игрока (InternetManagerImpl.connect:76),
  rate-limit ICMP/PCM/framebuffer.

### Конфиги линтеров: план ужесточения (полный аудит)

Ключевой факт: **checkstyle `severity=warning` (checkstyle.xml:8)** — сборка не упадёт ДАЖЕ при
isIgnoreFailures=false; падают только error-нарушения. Чинить это первым.
CI (ci-work.yml:40 ./gradlew build) гоняет линтеры, но ignoreFailures=true ×3 + severity=warning =
двойная страховка от красной сборки; шаг Upload reports с `if: failure()` — при зелёном билде отчёты
никуда не грузятся. Error Prone в CI не включается. **Qodana: qodana.yaml + gradle-задача есть,
но ни один workflow его не вызывает — мёртвая настройка.**

Checkstyle «включено, но задавлено»: MissingSwitchDefault (:324 объявлен, подавлен фильтром :47-49),
FallThrough, IllegalCatch, EmptyCatchBlock, ReturnCount, CyclomaticComplexity, NestedIfDepth,
IllegalThrows, NeedBraces, весь naming/formatting блок, MethodLength/FileLength, MissingJavadocMethod,
все проверки в тестах. У большинства SuppressionSingleFilter НЕТ атрибута files → глушат глобально.
PMD исключил: SystemPrintln/AvoidPrintStackTrace (ruleset.xml:14-15 — при 14 живых System.out),
GuardLogStatement (:10), ImplicitSwitchFallThrough (:32), CloseResource, UnusedPrivateMethod,
GodClass/NcssCount/MutableStaticState и др. MagicNumber в PMD АКТИВЕН (не трогать).
SpotBugs exclude-filter чистый (только jcodec+generated) — наши классы багов НЕ маскируются:
IL_INFINITE_LOOP поймал бы TCP-парсер, RANGE_ARRAY_* — FrameChunker/IRM; единственная слепая зона —
vendored jcodec (допустимо). Qodana excludes согласованы с остальными конфигами.

- [ ] **Ступень A (сейчас, ~15 строк фиксов кода):**
  1. ByteBufferFlashStorageDevice.java:110-112 — LOGGER.warn("...{}", identity, e) вместо
     println(e.getMessage()) (сейчас теряется stack trace);
  2. заменить 13× System.out → SLF4J LOGGER (список в §37);
  3. PMD: удалить exclude AvoidPrintStackTrace (0 нарушений сразу) и SystemPrintln;
     опционально GuardLogStatement + ImplicitSwitchFallThrough (предварительно прогнать ./gradlew pmdMain);
  4. Checkstyle: снять глобальные подавления MissingSwitchDefault (:47-49) и FallThrough (:140-142)
     с точечными // CHECKSTYLE.OFF при единичных нарушениях;
  5. стражи рецидива в Checker: RegexpSinglelineJava id=SystemOut (`^\s*System\.(out|err)\.`)
     и id=PrintStackTrace (`.printStackTrace\(\s*\)`).
- [ ] **Ступень B (failBuild для новых violations):**
  - build.gradle.kts:400,411,425 — все три isIgnoreFailures=false;
  - checkstyle.xml:8 severity=error (обязательно, иначе п.1 бессмысленен);
  - SpotBugs: baselineFile.set(config/spotbugs/baseline.xml) — штатный ratchet;
  - PMD 7 встроенного baseline НЕТ; для checkstyle+pmd — задача lintRatchet: считать нарушения из
    XML-отчётов, падать при росте сверх config/lint-baseline.properties (готовый дифф у автора аудита);
  - порядок: сначала ступень A, потом ratchet — иначе зашить в baseline мусор форматирования.
- [ ] **Ступень C (Error Prone always-on):**
  - дефолт enableErrorProne=true (build.gradle.kts:447-451), allErrorsAsWarnings=false (:466);
  - критичный набор -Xep:*:ERROR: ArrayToString, UnusedVariable, Finally (наш класс проглоченных
    исключений), DeadException, LoopConditionChecker (класс бага бесконечного цикла TCP-парсера),
    EqualsIncompatibleType, BoxedPrimitiveEquality, CompareToZero, FormatString;
  - шумные OFF: UnusedMethod (mixin/callback-магия), StrictUsedInaccurately, StringSplitter;
  - Guava-shadow workaround (:470-478) оставить обязателен;
  - внедрение: неделю с allErrorsAsWarnings=true в CI (-PenableErrorProne), собрать фактические
    срабатывания, точечно пофиксить/выключить, потом дефолт true.
- [ ] CI: шаг upload-artifact переключить на `if: always()` (отчёты линтеров всегда), опционально
  включить Qodana или удалить мёртвую задачу qodana из build.gradle.kts:488-492.

Итоговая приоритизация: (1) сейчас — AvoidPrintStackTrace + фикс ByteBufferFlashStorageDevice;
(2) неделя — 13× System.out→LOGGER, MissingSwitchDefault/FallThrough, SpotBugs failBuild+baseline;
(3) месяц — severity=error + ratchet, Error Prone default-on, CI if:always().

### Библиотеки: что переписывать

- ceres/sedna/sedna-buildroot/markdownmanual(+architectury) — ОСТАВИТЬ (используются глубоко:
  sedna 54 файла импортов, ceres 20, markdownmanual = вся внутригровая документация client/manual/).
- **jcodec (86 файлов, ~16K строк): рекомендация — свой дельта-кодек (вариант B)**.
  Контент монитора — mostly-static text UI: тайлы 32×16 + dirty-трекинг + RLE/zlib изменённых тайлов.
  Типичный кадр терминала — единицы КБ против десятков КБ H264+deflate, CPU на порядки ниже.
  VideoCodec уже расширяем (RAW(0), H264(1) → DELTA(2)), точка интеграции одна — FrameCodec.
  Объём 300–500 строк, 2–4 дня с тестами. После этого jcodec удалить целиком (−16K строк).
  Промежуточный вариант A (точечные патчи vendored jcodec: переиспользование EncodingContext/Picture/
  MBDeblocker, убрать deflate, QP tuning) — 1–2 дня, выигрыш всего 2–4×.
- Нативная oc2rnet: исходников в репо нет (скачиваются бинарники, 112КБ×8 платформ); нужна ровно
  для одного метода sendICMP (TCP/UDP уже чистая Java NIO!), fallback isReachable существует.
  Оставить; опционально убрать за ~1 день если цель — репо без бинарников.
- Подтверждён баг версии: gradle.properties sedna_buildroot_version=0.0.70 vs download-libs.sh
  качает 0.0.72-oc2r1 (нужен для CONFIG_9P_FS / issue #17) — поднять property (5 минут).


---

## 40. Удаление jcodec: полный список используемого (переписать целиком)

Точка входа единственная: `common/vm/video/FrameCodec.java` — импортирует из jcodec ровно 7 классов
(проверено grep по всему src, других потребителей нет):

```java
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
```

НО: эти 7 классов транзитивно тянут ~86 файлов (~16K строк) — энкодер (MotionEstimator,
CABAC/CAVLC, MBWriter*, DeblockingFilter...), декодер (SliceReader, BlockInterpolator,
MBlockDecoder*...), инфраструктуру (BitReader/BitWriter, VLC, IntObjectMap, Picture/Size).
**Полная замена = переписать ФУНКЦИОНАЛЬНЫЙ контракт этих 7 классов**, не их протокол:

- [ ] **К1 — свой дельта-кодек DELTA вместо H264** (см. §39 «Библиотеки», вариант B):
  тайлы 32×16, dirty-трекинг, RLE/zlib изменённых тайлов; энкодер+декодер+сетка 300–500 строк.
  Контракт: byte[] encode(int[] rgb565, w, h) / Optional<int[]> decode(byte[], w, h).
- [ ] **К2 — заменить Picture/ColorSpace/RgbToYuv420j/Yuv420jToRgb**: при DELTA-кодеке YUV-конверсия
  НЕ НУЖНА вообще (кодируем RGB565 напрямую) → 4 класса просто исчезают. Для RAW-режима тоже.
- [ ] **К3 — VideoCodec.DELTA(2)** в enum + выбор в FrameCodec; H264 оставить как legacy-опцию?
  РЕШИТЬ: если jcodec удаляем — H264-режим выпиливается из enum и конфига (GameplaySpec.videoCodec),
  миграция старых конфигов: videoCodec=h264 → delta с WARN в лог.
- [ ] **К4 — удалить src/main/java/li/cil/oc2/jcodec/** (−86 файлов), убрать exclude'ы из
  build.gradle.kts (:405, :417), checkstyle/pmd/spotbugs/qodana конфигов и docs/jcodec-analysis.md
  (заменить на заметку о DELTA-кодеке).
- [ ] **К5 — тесты**: FrameCodecTest расширить на DELTA: статичный кадр (почти пустой поток),
  полный шум (worst case ≤ RAW), roundtrip RGB565 бит-в-бит, BufferOverflow→RAW-fallback
  (заодно закрывает В5 — RAW внутри DELTA-потока недопустим так же, как в H264).
- [ ] **К6 — порядок работ**: (1) DELTA-кодек как новый класс рядом со FrameCodec + тесты;
  (2) переключить дефолт videoCodec на DELTA; (3) неделя обкатки; (4) удалить jcodec + H264.

Порядок относительно §38: К1–К3 можно делать ВЗАМЕН В1/В4/В5/В7 (deflate, RAW-bandwidth,
RAW-in-H264 fallback, QP — всё это проблемы H264-пути и уходят вместе с ним). Это меняет приоритеты:
DELTA-кодек решает сразу 4 перф-находки одним ходом.
