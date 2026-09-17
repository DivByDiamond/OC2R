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

## 18. Убрать jcodec → прямой RGB-буфер монитора/проектора — ИСТОРИЧЕСКАЯ, см. §40

Актуальность (2026-09-15): этот подход (raw-only, jcodec целиком удалён) был реализован,
но **§26 вернул jcodec обратно** (переключаемый RAW/H.264), а затем **§40 предложил третий,
текущий путь** — свой DELTA-кодек (тайлы + dirty-трекинг), К1–К3 которого уже в коде
(`DeltaFrameCodec.java` существует). Открытый чекбокс ниже устарел вместе с самим подходом —
актуальная работа и её открытые пункты живут в §40 (К4–К6).

~~Кадр = готовый RGB565-буфер as-is, клиент рендерит через DynamicTexture (NEAREST).~~
~~jcodec/балансировщики/worker pools удалены.~~ (обращено §26, затем заменено §40)

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

## 20. Push-based шина вместо polling BFS-сканирования — СМ. §43

Актуальность (2026-09-15): проблема описана верно и остаётся, но целевое решение теперь
спроектировано заново и шире в **§43** (единая кабельная система: device-bus + энергия +
network-mesh одним графом) — включая уточнение, важное по итогам обсуждения дизайна §43:
персистентный инкрементальный граф с merge/split (как предлагалось ниже) — сам по себе
источник багов того же класса (рассинхрон сохранённого состояния сети). §43 вместо этого
использует **ленивый in-memory BFS, инвалидируемый по событиям, без персистентного стейта
сети в сейве**. Секция ниже оставлена как постановка проблемы; этапы внизу не актуализировать
отдельно — прогресс трекать в §43.

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
- [x] **`HardDriveWithExternalDataItem`** — пункт устарел (проверено 2026-09-15): предмет **зарегистрирован** как `HARD_DRIVE_ONYXOS` (`Items.java:97`), провайдер `hard_drive_custom` подключён в `ProviderRegistry`, есть рецепт (`StorageRecipes.java:178`), модель (`ModItemModelProvider.java:52`), цвет (`CustomItemColors.java:61`), запись в creative tab. Не мёртвый код — был закрыт раньше, чекбокс не обновили
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

**Статус на 2026-09-15: все три пункта ниже закрыты, OnyxOS реально грузится в OC2R.** Секция оставлена
как история диагностики — сама диагностика была верной, просто с тех пор всё решено (см. чекбоксы ниже).

**Проверено (факты, актуализировано 2026-09-15):**
- `linker.ld`: `KERNEL_BASE = 0x80200000` — совпадает с адресом загрузки ядра в `MinuxFirmware` (`startAddress + 0x200000`). Схема `layout: minux` подходит без изменений.
- ~~Блокер: `boot.S` рассчитан на вход в M-mode~~ — решено `boot_smode.rs` (см. чекбокс ниже): отдельный
  S-mode энтрипоинт, не трогает ни одной M-mode-only CSR, просто паркует вторичные харты, зануляет BSS,
  прыгает в `kmain`.
- ~~`kmain` монтирует OnyxFS, встроенные rootfs OC2R не читает~~ — решено: OC2R сам шлёт правильный формат
  (`src/main/resources/onyxos/onyxfs.img`, OnyxFS, не cramfs/squashfs), подаётся как виртуальный HDD через
  `HARD_DRIVE_ONYXOS`/`OnyxOSBlockDeviceData` (см. §22 выше).
- ~~Сеть захардкожена `[10,0,2,15]`~~ — решено: `srv/main/mod.rs` теперь пробует DHCP первым, явно
  пропускает его при отсутствии virtio-net устройства (комментарий в коде: "OC2R/sedna has none"), и
  только тогда падает на статику `10.0.2.15/255.255.255.0` — которая к тому же совпадает с
  point-to-point моделью сетевой карты OC2R (см. §27 «Находки аудита inet/»: гость сам назначает
  себе IP, DHCP не предусмотрен по дизайну карты). Не блокер, а случайно (или намеренно) совместимо.

### ОнyxKernel (репо)
- [x] **Login incorrect при входе root** — исправлено в OnyxKernel/init/src/login/mod.rs (2026-08-23):
  терминал OC2R шлёт Enter как `'\r'` (TerminalInput.java:15), а login в raw-режиме (TIOCSRAW)
  обрезал только `'\n'` → пароль уходил на хэш с хвостовым CR. Теперь стрипаются оба
  (username + password); su.rs/passwd.rs уже умели. Дополнительно (2026-08-23): root сеется
  с ПУСТЫМ паролем (`seed.rs`), login принимает голый Enter как пустой пароль (`pn==0`
  больше не отклоняется) — вход как в Minux. Пересобрать образ: `cargo ibuild` +
  OnyxOS `scripts/mk-onyxfs-disk.sh`; на существующем диске с неизвестным паролем —
  перезалить свежий образ (first-boot пересеет root).
- [x] **`boot_smode.rs`** — реализовано (см. «вывод на монитор» ниже, `--features smode`, 2026-08-23):
  вход из OpenSBI в S-mode принят и работает, ядро успешно грузится и рисует в framebuffer.
- [x] **Сеть**: убрать хардкод `[10,0,2,15]`; DHCP или адрес из FDT/конфига — решено: DHCP пробуется
  первым (пропускается явно при отсутствии virtio-net), статика — только fallback, совпадающий с
  point-to-point моделью карты OC2R
- [ ] Проверить: UART NS16550A (совместим с sedna), virtio_net, virtio-blk, libfdt — что FDT от sedna парсится `early_init`

### ОнyxKernel — вывод на монитор (сделано 2026-08-23)
- [x] **Поддержка мониторов OC2R**: ядро раньше рисовало в приватную RAM (fallback) — хосту не видно.
  Добавлено: `libfdt/fdt/framebuffer.rs` ищет `/chosen/simple-framebuffer` (compatible/reg/width/height/stride,
  MMIO ниже 0x80000000); `fb::init_device()` принимает MMIO-геометрию; `put_pixel` умеет r5g6b5 (16bpp LE);
  `draw`/`writer`/devfs переведены с констант FB_* на динамические размеры; `srv/main/display.rs`
  пробует FDT-fb первым, RAM-fallback остался для QEMU. Пересобран `onyx-kernel.bin`
  (`--features smode` + objcopy), заменён в ресурсах мода.

### Мод (oc2r) — доставка OnyxOS-образа
- [x] **OnyxFS-диск**: образ rootfs подаётся как виртуальный HDD через `HARD_DRIVE_ONYXOS`/
  `OnyxOSBlockDeviceData` (`src/main/resources/onyxos/onyxfs.img`, оверрайд через
  `config/oc2r/onyxfs.img`) — не встроенный rootfs, ровно как и планировалось
- [ ] `layout: minux` в `FirmwareManifest`/`FirmwareDownloader` (задача 25) уже раскладывает kernel на 0x80200000 — проверить на реальном Onyx-образе
- [x] Build + проверка в игре: компьютер с OnyxOS-диском грузится до `login:` с OnyxKernel — работает

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

## 31. Аудит VT100-терминала (2026-08-18, ветка work, HEAD 75c8cc4) — ✅ DONE, все 25 находок закрыты

Первый полный аудит `common/vm/terminal/**`. Все блокеры (Б1 clearLine сбрасывал цвет, Б2 SU
no-op на свежем терминале, Б3 dirty-маска мапила не те строки), major/minor/nit-находки и
архитектурные решения закрыты и покрыты тестами. Что осталось как источник истины на будущее:
`ModeTable` (единый источник для 74 private + 4 ANSI режимов, заменил ~60-case таблицы в
CH1/CH2/CH3/CH6), `CSISequenceHandler.defaultParameters(CSIState)` (per-handler дефолты args
вместо единого правила в CSIManager — DECSTBM vs XTRESTORE делят один финал `r`), единственная
точка dirty-распределения — `Terminal.markDirty(mask)`/`markAllDirty()` (buffer-слой/CSI-хендлеры
больше не трогают `renderers` напрямую).

## 32. Аудит блоков на логические баги + PMD (2026-08-18, ветка work)

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

- [x] **CH10/CH11 → новые buffer-хелперы + убрать `System.out.println`** (маленький) ✅ — уже использует `deleteChars`/`insertChars` (проверено `CH10.java:26`, `CH11.java:24/29`), `TerminalMouseHandler.java:86` уже `LOGGER.warn`.

- [ ] **DEC Special Graphics рендер** (средний, ~100 строк + тесты)
  - `drawingMode`/`SPECIAL_GRAPHICS` парсится (`ESC ( 0`, `TerminalOutput.java:161-163`), но **не используется в рендере**: `TerminalCharRenderer.isPrintableCharacter` берёт сырой кодпоинт без трансляции DEC-графики (`0x6A`→`─`, `0x71`→`─`, `0x71`→`┘` и т.д.).
  - Результат: +vttest suite 2 (charsets), рамки в ncurses-приложениях (vim/top/mc).
  - Проверить после: `ESC ( 0` + box-drawing в vttest suite 2.

- [x] **DECSLRM + DECSTR** (средний) — реализовано 2026-09-17
  - `DECSLRM` (left/right margins, `CSI Pl;Pr s`, `CH6.handleDECSLRM`) — `Terminal.scrollColFirst/scrollColLast`,
    работает только при включённом `DECLRMM` (mode 69, теперь помечен `isImplemented=true` в `ModeTable`).
    Затронуто: cursor homing/DECOM (`Terminal.setRelativeCursorPos` — новый оверлоад с `xRelative`,
    `VPA` явно передаёт `false`), autowrap (`TerminalBufferWriter.rightWrapBoundary`/wrap-to-left-margin),
    ICH/DCH (`TerminalBuffer.rightEditBoundary`), IL/DL (`copyRowRange` — колоночно-ограниченный сдвиг
    вместо full-row `shiftLines`, когда курсор внутри маргинов и они не на всю ширину). Сброс маргинов:
    `setWidth`(DECCOLM)/`resizeHeight`/`DECSTR`/RIS — сброс на всю ширину; `resizeWidth`(DECSCPP) —
    неразрушающий clamp/track (см. `Terminal.adjustColumnMarginsForWidthChange`).
    **Осознанно не сделано**: SU/SD (`CSI Ps S/T`) и линейный перенос по IND/NEL/RI остаются
    full-width — не ограничены DECSLRM (архитектурно завязаны на scrollback-кольцо
    `TerminalLineShifter`, колоночно-ограниченный сдвиг сломал бы это без отдельного плана);
    SL/SR (`CSI Ps SP @`/`CSI Ps SP A`) тоже не урезаны маргинами по левому краю (только по правому,
    через `rightEditBoundary`, случайно — не по спецификации). Тесты: `TerminalBufferTest`
    (`decslrm*` — 11 тестов: parsing gate, homing, invalid Pl/Pr, RIS reset, autowrap внутри/вне
    маргинов, ICH/DCH/IL/DL bounding, DECSCPP resize).
  - `DECSTR` (soft reset, `CSI ! p`) — **сделано** (`escapes/index/DECSTR.java`, тесты `DecstrTest`): сброс таблиц режимов без полного RIS. Курсор и тэбы сохраняются; скролл-маргины (вкл. DECSLRM) сбрасываются в полный экран (DEC VT510-RM Table 5-9 + xterm-410 `VTReset(full=false)` — прежняя заметка «маргины сохраняются» была неверной).

## 36. Аудит VT100-терминала №2 — 6 суб-агентов (2026-08-23, ветка 1.21.1)

Повторный аудит `common/vm/terminal/**` (74 файла): архитектура, логика VT100/xterm, потокобезопасность, стиль/сборка, тесты, межмодульные контракты. Все находки верифицированы по исходникам; блокер Б1 подтверждён трассировкой вручную. Формат: `[файл:строка]`.

### Блокеры

Б1 (AIOOBE SD/RI на полном scrollback), Б2 (freeze SU/SD MAX) — закрыты; клиенты см. тесты `CSI 2 T`, `999999999S`.

### Major

M1 G1 designate, M2 truncated true-color, M3 dirtyLine overflow, M4 render race, M5 @OnlyIn, M6 SCROLL_BACK_COUNT final — закрыты (PR #24, #28, #30, #34, #36).

### Minor

- [x] **m1 — DECRC/restoreSavedCursor не клампят координаты после смены ширины** — закрыто
  PR #24 (unify cursor save/restore into `SavedCursor`, 2026-08-25): restore идёт через
  `setCursorPos` (clamp), репро-тест `decrcClampsSavedCursorAfterWidthShrink` добавлен.

- [ ] **m2 — CPR сообщает колонку width+1 (нет pending-wrap флага)**
  `[escapes/csi/DSR.java:22-31]` — `x+1` без clamp; состояние `x == width` легально
  (`putChar:31`). Системный артефакт модели немедленного переноса: ECH/DCH/ICH при
  `x==width` no-op, HTS молча теряет tab stop последней колонки. Полноценное решение —
  pending-wrap флаг вместо `x == width`.

- [x] **m3 — `CSI 3 J` (erase scrollback, xterm E3) молча игнорируется** `[escapes/csi/ED.java:26-39]` ✅ — `ED:39` → `bufferManager.clearScrollback()` (копия видимого окна в начало, хвост blank, `lastRowToDisplay(Max)=height`, `markAllBufferRowsDirty`).

- [x] **m4 — `CSI n` без параметра не отвечает** — закрыто PR #28 (2026-08-25):
  `DSR.defaultParameters()` теперь возвращает `{5}`, bare `CSI n` резолвится в
  `Ps=5` → `\033[0n` по ECMA-48.

- [ ] **m5 — режим 1048 сохраняет только x/y, restore идёт полным DECRC**
  `[escapes/csi/CH2.java:110-113]` — восстановление перезапишет стиль/цвета/charset
  init-дефолтами. Смежное: SCOSC (`CH6 's'`) в alt-буфере пишет в main-слоты savedX/savedY,
  а DECRC в alt-буфере читает altSaved* — сохранение «в никуда».

- [ ] **m6 — клавиатура ставит байты в очередь при выключенном capture**
  `[TerminalKeyboardHandler.java:29-41]` — проверка только для ESC; нажатия утекают в VM
  вне фокуса терминала (`MachineTerminalWidget.tick` отправляет безусловно).

- [x] **m7 — палитра xterm-256 вне канона** — закрыто PR #30 (2026-08-25): `0xdf` был
  опечаткой вместо `0xd7` для 4-го уровня куба; тест на полную каноническую палитру
  xterm-256 добавлен (942e1f9).

- [x] **m8 — `TerminalUtils.resetTerminal`: статический мутабельный ByteBuffer + голый 'J'**
  `[util/tick/TerminalUtils.java]` — переписан задачей 19 (2026-08-23): RIS + full snapshot,
  без статического буфера и литерального `'J'`.

- [x] **m9 — DCL без volatile в `Terminal.client()`** ✅
  `[Terminal.java:131-132,266-281]` — формально data race по JMM; спасает final-поле
  `TerminalClient.terminal`. Станет багом при добавлении любого нефинального поля.
  Фикс: `private transient volatile TerminalClient clientInstance;` — исправлено.

- [ ] **m10 — `lastRowToDisplay/Max` — plain int-пара с тремя писателями без синхронизации**
  Netty (IND/NEL) / main (mouseScrolled, getInput) → редкие «прыжки» окна просмотра истории.
  Плюс `hasPendingBell` — plain boolean (Netty пишет, main читает) → потеря звонка.

- [x] **m11 — dead code** — в основном закрыто PR #36 (2026-08-25, refactor/terminal-dead-code-cleanup):
  удалены `Utf8Decoder.hasActiveSequence()`, `TerminalIO.putOutput(byte)`,
  `TerminalBuffer.shiftUp/shiftDown(int)`, `SessionOperator`/`ColorUtils`/`RunnableUtils`,
  неиспользуемые поля `Glyph`; `TerminalRenderer.findLineIndex` / `TerminalCharRenderer.
  isPrintableCharacter`/`renderForegroundChar` / `TerminalBufferWriter.setChar` → private;
  `ImplementedPrivateModes.modeStatus` → `private static final`, `instance` → `public static final`.
  Остаток (вне области PR #36, независимо перепроверено ревью — не трогать без причины):
  - [ ] `Terminal.getTerminalWidth()` — используется только тестами, закрепить как публичный
    тестовый API или убрать
  - [ ] `incrementLastLineToDisplay(true)` — ветка никем не вызывается

- [ ] **m12 — дубли магических чисел**: цикл `i <= 23` ×3 (`TerminalBufferScrolling:36,46`,
  `TerminalIO:46`) → константа `FULL_DIRTY_MASK = (1 << HEIGHT) - 1`;
  blink `1000/500` ×4 (`TerminalRenderer:42,74`, `TerminalCharRenderer:36`,
  `TerminalBackgroundRenderer:35`) → именованные константы фазы;
  `% 8` вместо `TerminalColors.TAB_WIDTH` (`CSIManager.java:94`);
  номера mouse-режимов числами при существующих `PrivateMode.*` (`PrivateModeState:105-113`).

### Nit

- [x] HT внутри CSI игнорирует tabs[] (`CSIManager.java:93-95` — фиксированные `% 8`) ✅ — `CSIManager:105` теперь как `TerminalOutput.handleTab`: `while` по `tabs[]/altTabs[]`.
- [x] `ESC # 8` (DECALN) не сбрасывает маргины и не делает home (`TerminalIO.java:170-186`) ✅ — `TerminalOutput.handleHash:344` теперь `scrollFirst=0, scrollLast=height-1, setCursorPos(0,0)` перед fill.
- [x] DL предочистка `clearLine(y+i)` избыточна (перезаписывается сдвигом) `[DL.java:29-32]` ✅ — уже убрана, комментарий `DL:26` «No pre-clear...».
- [ ] `putResponse(String)` — N полных lock/unlock на байт; ответ не атомарен относительно readInput
- [ ] reentrant-запахи: вложенный lock в `putInput(String)/putInput(char)` (`TerminalIO:63-71,95-104`)
- [ ] разнобой `//` vs `/* */` (DSR/SGR/SGRColorParser/CH1/CSIManager/DA vs остальное); 7 строк >120 (LineLength подавлен)
- [ ] устаревший чекбокс выше (§31 «getInput без dirty») — уже реализовано в `TerminalIO.getInput():43-49`, закрыть
- [ ] XTVERSION-версия захардкожена `oc2rvt(1.0.0)` (`CH7.java:21`)
- [x] RIS не сбрасывает transient `hasPendingBell` (остальное сверено — RIS полон) ✅ — `RIS.java:32` `hasPendingBell=false`.

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
- [x] OSC/DCS/APC менеджеры (терминация ST/BEL) — закрыто PR #35 (2026-08-25,
  `StringSequenceTest.java`, 17 тестов: ST/BEL termination, CAN/SUB abort, ESC+non-`\`
  abort-and-redispatch, двойной ESC, nested string start)
- [ ] Семантика выхода `?1047l` — ещё не покрыта
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

Б1 дистанция MessageUtils, Б2 инъекция файлов, Б3 TcpHeader loop, Б4 FrameChunker, Б5 IntegerSpace — DONE (f5ccb2d,6df1e09,abd739e,bd9711d,082bc4c). Б6 DECRC — см. §36 m1.

### Major

- [ ] RPCDeviceBusAdapter: handoff `synchronizedInvocation` VM thread ↔ server thread без
  volatile/атомарности (`bus/adapter/RPCDeviceBusAdapter.java:49,117-121`) + TOCTOU
  pause/resume↔step (:101-133) → rebuild реестра параллельно с диспетчеризацией RPC.
- [ ] InternetConnectionImpl.saveAdapterState: `.get()` на server thread при автосейве
  (`inet/internet/connection/InternetConnectionImpl.java:38`) — фриз тика + дедлок-риск.
- [x] TerminalDiff.apply: равенство rows.length == rowData.length не проверяется нигде
  (`vm/terminal/TerminalDiff.java:205-207,313`) → AIOOBE/дисконнект клиента; clamp ширины. ✅ — `readSnapshot` уже клампит отрицательный `rowCount` и `boundedCount`; `apply:418` теперь `min(rows.length, rowData.length)` overlap, ширина клампится `resizeWidth`.
- [x] CUD/CUF int overflow при аргументе MAX_VALUE (`csi/CUD.java:17`, `CUF.java:17`) ✅ —
  клампит `moveCursorBy` через `Math.clamp(dx, -width, width)`; тесты `cudMovesCursorDownAndClampsSaturatedCount` и т.д.
- [x] Дубликат RegistryUtils: `common/util/RegistryUtils.java` ≡ `common/util/item/RegistryUtils.java`,
  обе живые, раздельная статика → оставить одну. ✅ Удален `common/util/item/RegistryUtils.java`, `Main.java:26` переключен на `common.util.RegistryUtils`.
- [x] System.out в проде (~14 мест): ConfigManager.java:21, VxlanBlockEntity.java:102,
  SwitchLog.java:28-51, TerminalMouseHandler.java:82,146, ByteBufferFlashStorageDevice.java:111,
  PciRootPortDevice.java:54-78 → SLF4J. ✅ Закрыто ступенью A (00a7aa7): 13× System.out → Log4j, проверки `grep -r System.out src/main` пусто.
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
- [x] **П3.1 — скользящее окно TCP** (закрыт 80a66b1, подтверждено верификацией 2026-08-26):
  окно с несколькими сегментами в полёте + cumulative ACK; тесты EstablishedStateTest.
- [x] **П4 (частично) — read/write до EAGAIN** (DONE 91a4f07): цикл channel.read до EAGAIN/
  EOF/полного буфера в readSession; write докручивает sendBuffer до конца в sendStream.
  processQueue early-exit оставлен НАМЕРЕННО: Receiver несёт ровно одну сессию/буфер на
  вызов receiveSession, кадр = один сегмент — drain нескольких сессий терял бы данные.
  Дальнейший выигрыш только через мультибуферный receiver (отдельный пункт).
- [x] **П5 — OP_WRITE копится, toWrite никогда не потребляется** (закрыт 797dbc1, см. VXLAN-секцию).
- [x] **П6 — ping-pong серверный↔Internet-поток** (DONE 2026-08-26, см. VXLAN-секцию).
- [x] П7 — per-packet/per-frame аллокации (см. VXLAN-секцию: пул кадров + Ш8; дискриминаторы
  SendHandler осознанно оставлены).

### Монитор/видео (CPU server thread + bandwidth)

- [x] **В1 — deflate(BEST_COMPRESSION=9) поверх уже сжатого H264** (DONE da56987):
  deflate/inflate удалены, H264-payload теперь сырой Annex-B; guard на start code
  сохраняет контракт «мусор → empty» (тест h264PayloadIsRawAnnexBNotZlib).
  Попутно ушла часть В9 (per-кадровые Deflater/Inflater/BAOS).
- [x] **В2 — весь энкод-путь на server thread** (DONE 2026-08-26): новый
  `common/vm/video/AsyncVideoEncoder` — общий для монитора и проектора, один shared daemon-worker,
  inbox ёмкостью 1 с вытеснением (last-frame-wins), outbox(8), пул буферов с точным матчингом длины.
  Серверный тик теперь только copyFrame + offer; готовые кадры уходят через flush() каждый тик
  вне throttle/dirty-гейтов (`MonitorTickHandler.tick`, `ProjectorBlockEntity.serverTick`) — иначе
  последний кадр анимации застревал в outbox при статичной картинке. Ownership буферов эксклюзивен:
  RAW-passthrough/fallback возвращают входной массив как есть → recycle строго после слайсинга
  (FrameChunker.slice копирует). FrameCodec остался потоконебезопасным по дизайну — thread confinement
  через worker. Тесты AsyncVideoEncoderTest (4). В10-дубль структуры контроллера не тронут —
  абстракция вынесена только для энкодера.
- [x] **В3 — 4MB direct buffer + энкодер на КАЖДЫЙ BlockEntity** (DONE ed9e195):
  ленивая аллокация encoder/decoder/buffer/picture при первом реальном использовании;
  idle BE и RAW-конфиг не выделяют ничего. Шаринг одного энкодера между активными BE
  отклонён сознательно: чередование BE каждый тик вынудило бы IDR на каждом кадре
  (смерть inter-frame сжатия).
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

- [x] **Дубли синка из §29 — закрыты (2026-08-26), три группы**:
  - **Фасад ×3 → ×1 + handleUpdateTag-фикс (атомарно)**: `BusCableBlockEntity.handleUpdateTag`
    дополнен `requestModelDataUpdate()` (модель строится из фасада; раньше маскировалось дублем);
    после этого из `FacadeManager.setFacade/removeFacade` удалены явный
    `sendBlockUpdated(UPDATE_ALL)` (vanilla уже шлёт BE-data packet из setHasFacade) и
    `BusCableFacadeMessage` целиком (класс, регистрация).
  - **Коннекторы ×2 → ×1**: `NetworkConnectorConnectionsMessage` удалён;
    `onConnectedPositionsChanged` → `sendBlockUpdated(UPDATE_CLIENTS)` (update-tag несёт
    позиции); в `handleUpdateTag` коннектора добавлена `NetworkCableRenderer.invalidateConnections()`
    (кэш рендера, прецедент common→client вызова уже был). Убран @OnlyIn shim.
  - **Имена интерфейсов ×2 → tag-only S→C**: рассылка заменена на `sendBlockUpdated`
    (getUpdateTag несёт ВСЕ имена); `BusInterfaceNameMessage` остался только как C→S input
    от GUI (playBidirectional → playToServer, клиентская ветка вычищена).
  - **Флоппи/флеш ×2 → ×1**: `DiskDriveFloppyMessage`/`FirmwareFlasherMessage` удалены,
    `onContentsChanged` → `sendBlockUpdated` (содержимое слота в update-tag'е BE).
  - **chunk==null**: `ComputerTerminalManager.sendToClientsTrackingComputer` теперь лениво
    резолвит чанк до первого serverTick'а — run/boot-error сообщения больше не теряются.
  - **Осталось осознанно**: (a) монитор/проектор — hasEnergy живёт ТОЛЬКО в *StateMessage
    (isMounted дублируется LIT+tag, но чистка потребует трогать рендер/контейнер),
    низкий приоритет; (b) VS2-fallback рассылает всем во всех измерениях — НАИВНЫЙ фильтр
    по dimension НЕЛЬЗЯ (игроки физически не в ship-world → 0 получателей); нужен маппинг
    ship-chunk → parent-level через VS2 API или радиусная эвристика — отдельное решение.
- [x] **- [x] **Т1 — TerminalDiff упаковка ячеек** (уже реализовано в коде, помечено при
  верификации): формат переписан — varint-кодпойнт + attr-байт с опциональными полями
  цветов/style + RLE-раны одинаковых ячеек; одиночное эхо ≈ несколько байт на строку
  вместо фиксированных 37Б на ячейку (javadoc TerminalDiff, TerminalDiffTest).
- MultipartMessage: ключ починен; MAX_PAYLOAD_SIZE=8КБ при лимите NeoForge 1МБ — импорт 512КБ = 64 пакета;
  оверхед заголовков <1% (ок). enqueueWork в SoundCardBeep/Pcm — избыточен, но не баг.

### VXLAN / шина / энергия

- [x] **Ш1 — scan() шины каждый тик** (DONE 53b50d2, частично устарел): при верификации
  выяснено — event-driven скан с dirty-флагом `scheduleBusScan` + O(1) ранний выход уже
  существуют (BFS НЕ каждый тик). Доделано: переиспользование BFS/diff-коллекций в
  `BusElementManager`, один проход вместо двух diff-HashSet. Полный push-based граф §20
  не требуется (событийная модель покрывает).
- [x] **Ш2 — энергосеть: BFS flood-fill + O(n²) redistribute каждый тик** (DONE d553911):
  `EnergyNetworkCache` (per-level/per-origin кэш, инвалидация на config change/load/unload
  кабеля + валидация isRemoved), redistribute раз в 20 тиков; pull/push ежетиковые.
  Capability-lookup'ы оставлены свежими (кэшировать чужие capability небезопасно).
- [x] **Ш3 — блокирующий UDP send** (закрыт 5c11f5d, подтверждено верификацией 2026-08-26):
  `DatagramChannel.configureBlocking(false)` (TunnelManager.java:123), send non-blocking,
  приём на daemon-thread с селектором.
- [x] **Ш4 — System.out.printf каждый тик** (закрыт ранее, доведён до конца 2026-08-26):
  System.out давно заменён на LOGGER; остаточный warn-спам «unregistered upstream» каждый тик
  (VxlanBlockEntity.java:142) переведён на report-once до восстановления регистрации
  (warnedUnregisteredUpstream + сброс в loadServer).
- [x] **Ш5 — лишние локи VXLAN** (закрыт 5c11f5d): ReentrantLock удалён, ConcurrentHashMap +
  thread-safe offer (комментарий в TunnelManager.java:193).
- [x] **Ш6 — молчаливая потеря пакетов** (закрыт 5c11f5d): drop-статистика
  `TunnelInterface.droppedFrames` (:194-196,:281) + WARN владельца очереди раз в тик
  (VxlanBlockEntity.java:133-140) + конфиг vxlanPacketQueueCapacity.
- [x] **Ш7 — per-send аллокации в TunnelManager.sendToOuternet** (DONE 2026-08-26):
  InetSocketAddress кешируется в конструкторе (`cachedRemoteAddress`), заголовочный буфер
  стал grow-only полем `sendBuffer` (reuse безопасен: send() копирует в ядро синхронно);
  ByteBuffer.wrap(buffer, 0, total) без выделения массива на кадр.
- [x] **Ш8 — SessionManager per-packet** (смягчено и закрыто 2026-08-26): coarse shared clock
  (кэш Instant обновляется не чаще раза в 200 мкc через nanoTime — Instant.now() больше не
  на каждый пакет); expiration/retransmission сканы переведены с keySet+`get(time)` на
  итерацию Map.Entry с iterator.remove() (минус O(log n) на кандидата). updateSession
  remove+put оставлен: lastUpdateTime это Instant из api/ (не трогаем), TreeMap на long
  потребовал бы правки api. Коллизионный цикл enqueue оставлен — редкий случай.
- [x] **П5 — OP_WRITE/toWrite** (закрыт 797dbc1, подтверждено верификацией 2026-08-26):
  OP_WRITE не регистрируется (SocketManager.java:105-113, javadoc :100-104), очередь удалена.
- [x] **П6 — ping-pong серверный↔Internet-поток** (DONE 2026-08-26):
  `InternetManagerImpl.onTick` при пустых `connections && tasks` выходит без submit'а на
  executor (раньше — два стрима с аллокациями + безусловный round-trip каждый тик даже без карт).
  «Минимум 2 тика на кадр» — by design, не тронут.
- [x] **П7 — per-packet аллокации** (частично закрыто 2026-08-26, остальное осознанно):
  InternetConnectionImpl.java:101 → пул кадров `framePool` (obtain/recycle) + контракт
  «адаптеру кадр передаётся заимствованным»: gateway единственный удерживал ссылку
  (inboundQueue.addLast) → добавлена защитная копия frame.clone() у него; VirtIONetworkDevice
  копирует байты в гостевую память внутри writeEthernetFrame (проверено javap'ом sedna 2.0.13).
  Discriminator'ы SendHandler оставлены как есть: они выступают map-key (мутация кэша
  сломала бы хеширование), а объект ~24Б на TLAB — цена ниже риска. DefaultTransportLayer/
  SessionManager churn covered by Ш8.
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
- [x] **С5 — PCM-флуд** (DONE 2026-08-26): `SoundCardItemDevice.write` — token bucket
  `soundCardPcmBytesPerSecond` (конфиг GameplaySpec, default 128 КиБ/с, 4КиБ–16МБ/с), refill
  непрерывный по nanoTime, превышение → IllegalArgumentException гостю. Короткие burst'ы после
  маунта работают (бакет стартует полным).
- [x] **С6 — rate limit на C2S-сообщения** (DONE 2026-08-26): новый
  `common/network/util/PlayerRateLimits` (per-player throttle + event-window, WeakHashMap,
  потокобезопасно) + тест PlayerRateLimitsTest(4, поймал off-by-one первого ивента окна);
  MonitorRequestFramebufferMessage / ProjectorRequestFramebufferMessage — throttle 250 мс/игрок
  (легитимный клиент шлёт раз в секунду, спам держал watchers живыми → принудительный энкод в пустоту);
  KeyboardInputMessage — 64 события/с/игрок (каждое событие = прерывание гостю → жгла CPU ВМ).
- [x] **С7 — ICMP echo блокировал internet-поток** (DONE 2026-08-26): нативная ветка
  `EchoHandler.handleEchoSession` (sync `DefaultSessionLayer.sendICMP`, до 1 с на пинг «чёрного»
  адреса — стопорила ВСЕ интернет-карты сервера: кадры, TCP/UDP, задачи тика; saveAdapterState
  с серверного потока тоже вис на submit().get()) переведена на тот же executor
  «internet/blocking-session», что и fallback; ответ доставляется асинхронно как раньше.
  Попутно фикс бага: `size = data.remaining()` считался ПОСЛЕ `data.get(payload)` → всегда 0,
  нативный sendICMP звался с пустым payload.
- [x] **С8 — NBT интернет-карты: MAC/IP восстанавливались как есть** (DONE 2026-08-26):
  per-card UUID (`DeviceId` в adapter-state, генерится при первом использовании в
  `InternetCardDevice`, переживает демонтирование) + `MacAddressUtils.macFromUuid`
  (SplitMix64 finalizer, детерминированно); `DefaultLinkLocalLayer.loadMacAddress`
  теперь ДЕРИВИРУЕТ MAC из UUID и игнорирует player-writable поле `MACAddress`
  (legacy-путь оставлен для карт до обновления; их MAC сменится один раз). Тесты
  MacAddressUtilsTest +2. IP остался гостевым (point-to-point, назначается ОС) —
  спуфинг source закрыт С3-фильтром по ARP-claim.
- [x] **С9 — TunnelManager надёжность** (частично устарел на момент работы): bind-fail guard
  (`managerInstance != null` перед стартом потока) и shutdown (select-timeout checkpoint +
  ServerStoppingEvent) уже были сделаны в 672e7a3/5c11f5d. Доделано здесь: DEFAULT_VXLAN_HOST
  `"::1"` (IPv6 loopback — тихо нерабочий дефолт на IPv4-серверах) → раздельные дефолты
  `bindHost="0.0.0.0"`, `remoteHost="127.0.0.1"` (Config + VXLANSpec); null-check
  `TunnelManager.instance()` в VxlanBlockEntity.onUnload/loadServer (NPE после ServerStopping);
  selector → volatile (симметрично channel).
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

- [x] **Ступень A** (DONE 00a7aa7): все 5 подпунктов выполнены — 13× System.out → Log4j
  (Log4j2, не SLF4J — по конвенции кодовой базы), ByteBufferFlashStorageDevice снова
  логирует полный stack trace, PMD excludes AvoidPrintStackTrace/SystemPrintln удалены
  (0 нарушений), подавления MissingSwitchDefault/FallThrough сняты (1 нарушение в
  TerminalOutput исправлено default-веткой, FallThrough — 0), стражи RegexpSinglelineJava
  id=SystemOut/PrintStackTrace добавлены в TreeWalker.
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

- [x] **К1 — свой дельта-кодек DELTA** (DONE 2d77dd6): `DeltaFrameCodec` — тайлы 32×16,
  dirty-трекинг, per-tile лучший из RLE/zlib/raw, zlib-ключевой кадр на старте и смене
  разрешения; в пейлоаде передаются размеры (stale-стрим отвергается), декодирование
  идёт на копии кадра (битый дельта-блок не портит референс).
- [x] **К2 — YUV не нужен на DELTA-пути** (DONE 2d77dd6): DELTA кодирует RGB565 напрямую,
  jcodec-классы остались только на legacy-H264 пути (исчезнут вместе с К4).
- [x] **К3 — VideoCodec.DELTA(2)** в enum + выбор в FrameCodec (DONE 2d77dd6).
  РЕШЕНИЕ: H264 остаётся legacy-опцией до К4 (неделя обкатки DELTA дефолтом:
  GameplaySpec videoCodec raw→delta); при К4 h264 выпиливается из enum/конфига с WARN-миграцией.
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

---

## 41. Апстрим fnuecke/oc2 v0.3.0 (1.21.1/0.3.0) — сверка и портирование (2026-09-07)

Мейнтейнер fnuecke вернулся к оригинальному моду, релиз v0.3.0. Наш форк — полный
рефакторинг, независимый от их дерева (не git fork, свой git history). Часть терминальных
фиксов у них пересекается с уже закрытым §36 (M1-M5) — считаем закрытым, не портировать.

### 41.1 GameTest CI/CD (по образцу fnuecke/oc2, self-hosted раннеры общие — pocketprobe-kube)

У нас уже есть `runs { register("gameTestServer") {...} }` в build.gradle.kts (:195-198),
но никуда не подключён; своих `@GameTest`-классов нет вообще (только unit-тесты в src/test).
У апстрима — отдельный gradle-подпроект `gametest` с ~20 классами (RegistrationTests,
RecipeTests, DeviceBusTests, NetworkConnectorTests, RobotCrushTests, WrenchTests, GuestTests...),
таск `gameTest` = `dependsOn(":neoforge:runGameTestServer")` (buildSrc/BuildUtils.kt:108-121),
NeoForge сам пишет JUnit XML в `build/test-results/gameTest/*.xml`, buildSrc/GameTestReport.kt
только нормализует корневой тег. CI: build.yml гоняет `./gradlew gameTest` после `build`,
аплоадит артефакт `gametest-results-${os}`; отдельный test-report.yml (workflow_run от build)
публикует JUnit-отчёт через `dorny/test-reporter`.

- [x] Добавить таск `gameTest` в build.gradle.kts: `dependsOn("runGameTestServer")`
  (аналог `registerGameTestTask()` из их buildSrc/BuildUtils.kt:108)
- [x] Завести `src/main/java/li/cil/oc2/gametest/` и написать
  3 стартовых smoke-тестов: RegistrationTests (DeviceTypes), RecipeTests (everyModItemIsCraftable,
  everyRecipeCraftsInCraftingTable), DeviceBusTests (busTracksNeighborLifecycle,
  computerStartsWithoutBootError) + хелперы TestSupport, ComputerFixture
- [x] Дополнить `.github/workflows/ci-work.yml`: шаг `./gradlew gameTest` после build,
  аплоад артефакта `gametest-results`
- [x] Добавить `.github/workflows/test-report.yml` (workflow_run от `ci`), публикация через
  `dorny/test-reporter@v3` (reporter `java-junit`)
- [ ] Постепенно расширять набор тестов под наши модули (терминал, network connector,
  redstone interface) по мере работы над 41.2

### 41.2 Точечные фиксы из релиза (диф-план, без реализации)

- [x] **Redstone side mixed-up (issue #164)** — исправлено (`ec49a41`, 2026-09-07):
  `getRedstoneOutput`/`setRedstoneOutput` в `RedstoneInterfaceCardItemDevice` и
  `RedstoneInterfaceBlockEntity` теперь используют `HorizontalBlockUtils.toGlobal()`
  так же, как `getRedstoneInput` — выходы поворачиваются вместе с FACING блока.
- [ ] **Robot `detect(side)` API (issue #108)**: **Отсутствует.** Метода нет в
  `api/capabilities/Robot.java` (только getInventory/getSelectedSlot/setSelectedSlot) и
  в `common/entity/Robot.java`. Feature request, не баг — приоритет низкий.
- [x] **Network connector на заборах (issue #225)** — исправлено (2026-09-13):
  `NetworkConnectorBlock.canSurvive()` теперь дополнительно разрешает крепление,
  если блок с прикреплённой стороны помечен `BlockTags.FENCES` (базовая проверка
  `FaceAttachedHorizontalDirectionalBlock.canSurvive` по-прежнему работает для
  обычных solid-граней). `getStateForPlacement` не трогали — он уже вызывает
  `canSurvive` через суперкласс, так что размещение на заборе заработало само.
- [ ] **JEI computer recipe (issue #270)**: **Уже обработано.** `ExtraGuiAreasJEIPlugin`
  удаляет preconfigured computer из JEI (осознанно); обычные рецепты компьютера видны.
- [ ] **Keyboard as terminal user (issue #186)**: **Уже обработано.** `TerminalUserProvider`
  реализован в `ComputerTerminalManager` и `Robot`; `TerminalKeyboardHandler` — GUI-level,
  ввод идёт только при открытом терминале.

### 41.3 Не относится / уже закрыто

- Терминальные фиксы v0.3.0 — перекрыты §36 (M1-M5, коммит f6f9d09), у нас детальнее
  (charset G0/G1, SGR recovery, dirty-mask overflow, ST-string ESC-abort parity).
- NeoForge startup crash (#294), projector blank display (#230), sound attenuation (#288) —
  надо проверить отдельно, не связаны с терминалом, не исследовано.
- Linux fdisk/swap (#47/#268), console keymaps (#147) — гостевой Linux-образ, не Java-код мода.
- Block device data unification (#127, BREAKING) — датапак-формат образов
  (`data/oc2/block_devices/{hdd,floppy,flash}/`), требует отдельного решения по совместимости.

### 41.4 Обновления зависимостей (проверено 2026-09-07)

- [x] **ceres `0.0.6` → `0.0.7`** (2026-09-02): sanity-check на размер массивов при десериализации
  (>64MB отклоняется). Безопасный апдейт — применён: `gradle.properties`, `download-libs.sh`, `libs/`.
- [x] **sedna `3.1.0` → `4.0.1`** — обновлено (`ec49a41`, 2026-09-07): `gradle.properties`,
  `scripts/download-libs.sh`; `GlobalMemoryRangeAllocator` мигрирован на `DeviceBus`
  (`board.getDeviceBus().addDevice/removeDevice` вместо `board.addDevice/removeDevice`).
  `compileJava` зелёный. Подтверждено (2026-09-13, релиз-ноуты sedna 4.0.0): апстрим
  добавил `Z80Board` наравне с `R5Board` (Board rework специально «to bring Z80Board
  closer to the R5Board», GDB debug support для Z80) — т.е. sedna 4.0 уже умеет и
  RISC-V, и Z80 VM. Добавление Z80 как выбираемой архитектуры в oc2r — отдельная
  большая фича, не входит в этот пункт (см. задачу за скоупом §41).

## 42. Multiloader (NeoForge+Fabric) + мультиверсия (1.21.1/26.1/26.2) — план (2026-09-15)

Дизайн: **`docs/MULTILOADER.md`**. Ничего ещё не реализовано — только спек, разбитый на
этапы, чтобы `work` не расходился с рабочей веткой надолго.

- Триггер: Mojang в 2026 сменил версионирование на `YY.drop` (26.1 "Tiny Takeover" март,
  26.2 "Chaos Cubed" июнь, взамен `1.x`; `1.21.11` — последний релиз старой схемы). Оба
  лоадера (NeoForge, Fabric) уже поддерживают 26.1/26.2.
- Матрица целей: {1.21.1, 26.1, 26.2} × {NeoForge, Fabric} — 6 таргетов.
- Связанность с NeoForge сейчас: 236 из ~1050 java-файлов напрямую импортируют
  `net.neoforged.*` (registries 40, network.handling 42, capabilities 30, energy 16, FML/bus
  остальное) — просто "добавить Fabric" не получится, нужна платформенная абстракция.

**Этапы** (каждый — свой implementation-план и свои PR, когда до него дойдёт очередь):
- [ ] **Этап 1** — `core`/`neoforge` Gradle-split (без поведенческих изменений) + registries
  как образцовая подсистема (`core: RegistryBridge` → `neoforge` impl → tests/CI). Остальные
  ~230 NeoForge-файлов сознательно не трогаются. Детали, критерии готовности, git/CI-стратегия
  — в `docs/MULTILOADER.md`.
- [ ] **Этап 2** — реальный `fabric`-модуль + миграция capabilities/network/energy/client-events
  по шаблону из Этапа 1.
- [ ] **Этап 3** — мультиверсия 1.21.1/26.1/26.2 через Stonecutter, на оба лоадера.
- [ ] **Этап 4** — CI/релизный матрикс на 6 таргетов, CurseForge/Modrinth на каждый.

**Тулчейн**: раздельные официальные плагины (`net.neoforged.moddev` для NeoForge — не
трогаем, `fabric-loom` для Fabric), НЕ Architectury Loom — обоснование в доке (риск ломать
уже настроенный NeoForge-стек ради выгоды, которая понадобится только на Этапе 2).

**Инструмент `jmove`** (`/storage/project/rust/jmove`) проверен на этот кейс — не подходит
для bulk-переноса `src/main/java` → `core/src/main/java` (отказывается переносить файл за
пределы обнаруженного source root, а пакет тут и так не меняется — нужен обычный `git mv`).
Пригодится позже для точечных package-переносов на Этапах 2+ (build из коммита `0216fd6`,
рабочее дерево сейчас не компилируется — незакоммиченный WIP fix-engine).

## 43. Переработка кабельной системы с нуля (device bus + energy + network mesh) — план (2026-09-15)

Дизайн: **`docs/CABLE-SYSTEM.md`**. Ничего ещё не реализовано — только спек ядра.

- Триггер: баги (фантомные кабели, энергия перестаёт передаваться на расстоянии, лаги на
  поворотах, ломается при break+replace, конфликт энергия/данные в одном проводе) — все
  трассируются к одной причине: кэш соседства/сети без единого источника истины, рассинхрон
  client/server и после чанк-релоада.
- `BusCable`/`DeviceBusElement` (device bus) и `NetworkConnector` (point-to-point меш) —
  две независимые механики сейчас, заменяются **одним** типом кабеля.
- Ключевые решения: авто-коннект по соседству (не point-to-point), **никаких персистентных
  сетевых объектов в сейве** — ленивый in-memory BFS от запрашивающего, инвалидация по
  событиям (place/break/onLoad/wrench), нулевая топологическая работа в `tick()`; владение
  устройством вычисляется детерминированно при каждой перестройке (не хранится) и **видимо**
  игроку («занято: Computer #N»), не молча; энергия — receive-only `IEnergyStorage` на
  гранях (приём из чужих модов почти бесплатно), буфер в каждом кабеле, не в сети; hub/switch
  и `NetworkConnector` выпиливаются осознанно (дальние point-to-point связи — фиче-лосс,
  записан явно, возможное продолжение отдельным под-проектом).
- **Не решено в этой спеке** (свои спеки позже): wrench-инструмент/UX (тег `c:wrenches`-стиль,
  сама механика — позже), миграция существующих построек с `BusCable`/`NetworkConnector`,
  шаринг устройств (кроме модели владения выше).

## 44. Терминал: line drawing + strikethrough + double-size (2026-09-15)

Три таски по терминалу (приёмка: boxes сразу видны в tmux, vttest section 2).
Ориентироваться на доки: исходники xterm + VT510 programmer manual. Очередь: 44.2 → 44.1 → 44.4 → 44.3.

### 44.1 Line drawing characters (DEC Special Graphics) — средний PR — [x] сделано (2026-09-16)

Реализовано: `TerminalBufferWriter.mapDecSpecialGraphics` (маппинг 0x60-0x7E → Unicode,
применяется в `putChar` перед `setChar`) + `TerminalCharRenderer.renderBoxDrawing`
(процедурные квады для box-drawing U+2500-253C и scan-line U+23BA-23BD, остальные —
обычные глифы шрифта). Пункты плана ниже — исторический план, оставлен для справки.

Состояние в коде (проверено 2026-09-15, устарело — см. выше):
- Парсер готов: `ESC ( 0`/`ESC ) 0` выставляют `drawingModeG0/G1`
  (`TerminalOutput.handleCharsetDesignate`, строка ~326), `SO`/`SI` крутят `useG0`,
  DECSC/DECRC сохраняют чарсеты, RIS/DECSTR сбрасывают.
- **Рендера нет**: активный чарсет никогда не применяется — `putChar`
  (`TerminalBufferWriter`) пишет сырой ASCII. Поэтому tmux/vim рисуют `jxkm` буквами.
- **Шрифт не спасёт**: проверено `canDisplay` по monocraft-r.ttf — ни одного глифа в
  U+2500–U+257F (все false). Значит «замапить в Unicode и рендерить шрифтом» не работает,
  глифы рисовать процедурно.

План:
- [ ] Таблица маппинга DEC Special Graphics 0x5F–0x7E → Unicode U+2423/U+2500-блок
      (источники: Wikipedia «DEC Special Graphics», xterm `convtbl.c` — как эталон;
      решения по спорным ячейкам (`^`/`g`/`~`) принимать по xterm, как везде в этом коде).
      **Уточнение 2026-09-16 (проверено по xterm-411, /tmp/opencode/refs/extracts):**
      эталонная таблица — `fontutils.c` `dec2ucs` (строки 4973–5007): рамочные глифы это
      `j k l m n`(углы/крест), `q x`(гориз/верт линии), `t u v w`(тау-формы); НЕ `a..f`
      (`a`=▒ 2592, `b`=␉ 2409, `c`=␌, `d`=␍, `e`=␊, `f`=°, `g`=±, `h`=␤, `i`=␋,
      `o p r s`=scan-линии, `y z`=`≤ ≥`, `{`=π, `|`=≠, `}`=£, `~`=·).
      `_` (0x5F) в DEC-Special это **space** (xterm сворачивает 0x5F→0 в charsets.c),
      `^` и 0x7F не участвуют в маппинге; применение только для диапазона 0x60–0x7E.
- [ ] Применять в единственной точке — `putChar` перед `setChar`. REP идёт через `putChar`
      повторно: решить, храним в `lastPrintedChar` сырой или замапленный символ (xterm
      ререпитит под текущим чарсетом — зафиксировать выбором теста).
- [ ] Буфер хранит Unicode-кодпоинты (int[] уже позволяет) — diff-формат не меняется.
- [ ] Клиент: в `TerminalCharRenderer` для диапазона U+2500–U+257F рисовать квады рёбер
      (гориз. штрих по середине высоты, верт. по центру ячейки, углы — комбинации),
      по образцу underline-квада (строка ~151); `getGlyph` для них не звать.
- [ ] Тесты: юнит (поток `ESC ( 0 jxkm` → ┘┌└─ в буфере; `ESC ( B` возвращает ASCII;
      SI/SO; DECRC restores charset), `TerminalDiffCodecTest` на BMP-кодпоинты.
- [ ] Ручная приёмка: tmux с рамкой (`tmux` показывает boxes в status-баре/поп-апах) и
      любой нcurses-скрипт; эталон — vttest §2a (DEC Special Graphics), simcity.c/nInvaders позже.

### 44.2 Strikethrough (SGR 9/29) — маленький PR, начать с него — [x] сделано (2026-09-16)

- [x] `Terminal.STYLE_CROSSED_OUT_MASK = 1 << 7`. **Осторожно**: `style` — `byte`
      (Terminal.java:67), 0x80 станет отрицательным байтом; побитовые операции ок, но
      сериализация в diff (`buf.put(cell.style())`) обязана сохранить бит — тест ниже.
- [ ] `SGRStyleDispatch`: `case 9` — set, `case 29` — clear (коды DEC/xterm; 21 не трогать,
      у нас там не занят). Сброс `case 0` уже покрыт (style = DEFAULT_STYLE).
- [ ] Рендер: квад на середине высоты ячейки (y = CHAR_HEIGHT/2, толщина 2px), цвет —
      тот же, что foreground (как underline), в `TerminalCharRenderer`.
- [ ] Тесты: `SGRTest` (9, 29, 0, композиция с underline), `TerminalDiffCodecTest`
      round-trip стиля 0x80 (знаковое расширение — ровно тот баг, который тут легко словить).

### 44.3 Double-sized characters (ESC #3/#4/#5/#6) — большой, два PR

- [x] PR-A: парсинг (`handleHash` знает только `'8'`/DECALN) + per-line атрибут
      double-height верх/низ (`ESC # 3`/`# 4`) и сброс (`# 5` или обычная строка). ✅ 2026-09-17: `Terminal.LINE_ATTR_*` (0 SINGLE,1 DWL,2 DHL_TOP,3 DHL_BOTTOM), `lineAttrs`/`altLineAttrs` per-row (480/24), `handleHash` ставит атрибут текущей строки `y` (whole line, xterm doublechr.c), `setWidth`/`resizeHeight`/`clear`/`clearScrollback`/`TerminalLineShifter` копируют/бланкуют атрибуты, `TerminalDiff` bump `PROTOCOL_VERSION 1→2` + `lineAttrs` byte per row parallel to `rows`, `apply`/`clearBuffers` sync. Тесты `TerminalDiff` обновлены, vttest harness зелёный. Рендер пока stub (single-width).
      **Уточнение 2026-09-16 (xterm-411 `doublechr.c`):** коды: `# 3`=DHL-верх,
      `# 4`=DHL-низ, `# 5`=SWL (одинарная ширина), `# 6`=DWL (двойная ширина),
      `ESC # 7` **не существует** — пара «двойная ширина+высота» набирается как
      `#3/#4` по строке + `#6`; атрибут **per-line** (2-битное поле LineData) и
      применяется ко ВСЕЙ текущей строке от колонки 0, а не «от курсора до EOL»;
      сброс — `# 5` либо смена строки (DECRST не связан). Ссылки: doublechr.c:108-141,
      ptyx.h:1680-1695, EK-VT520-RM (DECDHL стр.188, DECSWL стр.304).
- [ ] PR-B: double-width (`# 6`): 2 ячейки на символ → ломает autowrap (pending wrap
      на последней колонке), EL/DL/IL/copy-путь и модель Cell в diff (1 codepoint = 1 колонка):
      расширять протокол (padding-ячейки или wide-флаг). Отдельный implementation-план. (Протокол v2 уже зарезервирован, PR-A использует line-attr, PR-B потребует wide-клетки.)
- [x] MVP-критерий: не ломает парсинг (сейчас `# 3` уходит в warn-ветку — это терпимо, но
      молча игнорировать до полного рендера лучше с явным TODO, чем падать). ✅ — `handleHash` теперь парсит #3/#4/#5/#6 без warn, помечает dirty.

### 44.4 vttest без интерактива — автоматизация приёмки

- vttest сам по себе интерактивный (curses-вопросы «видите квадрат?»), в CI напрямую не гоняется.
- Ключевое наблюдение: экранировки, которые vttest выдаёт, **захардкожены** — значит можно
  взять фиксированные геометрии (80x24 + 80x132), для каждой страницы vttest получить эталон
  содержимого каждой ячейки и прогнать **все страницы как тесты**. Генерация эталонов —
  типичная работа для модели: набрасывает ожидаемые сетки ячеек, человек ревьюит перед коммитом.

План (слой 1 — cell-level, детерминированный, без клиента):
- [ ] Харвест потоков: прогнать каждую страницу vttest по захвату (tee/stdin-логгер или
      прямой extraction из `screen_test.c`) → `.bytes`-фикстуры на страницу.
- [ ] Формат теста: data-driven — ресурс-файлы `<page>.bytes` + `<page>.cells` (golden-сетка
      кодов + стилей), параметризованный JUnit-тест перебирает все страницы: грузим байты в
      серверный `Terminal` на 80x24 (часть страниц — 80x132), сравниваем посимвольно.
- [ ] Ассертить не только codepoints, но и `styles`/цвета — иначе §44.2 (strikethrough) и
      DECSCNM-страницы тесту не видны.
- [ ] Спорные страницы решать явно, не молча: запрос/ответ (DSR/DA/status line — vttest ждёт
      reply от хоста, нужен стаб-вход), purely-visual (blink, double-size из §44.3) — список
      исключений с причиной или перенос в слой 2.
- [ ] Ревью эталонов: golden-сетки генерирует модель, в мёрж только после ручной сверки —
      иначе закрепим галлюцинации как спеку.

План (слой 2 — пиксели, ловит ошибки рендера, которые невидимы в буфере):
- [ ] Через GameTest-инфраструктуру (§41.1): гостевой скрипт печатает паттерн, снять
      терминальную DynamicTexture, diff с golden-изображением (аналог xterm scrapshot:
      expect + ImageMagick). Покрывает процедурные глифы из §44.1 и масштаб из §44.3.
- Приоритет: слой 1 раньше (он же приёмка §44.1/§44.2), слой 2 — когда будет GameTest-хук.

- [ ] Опорные ссылки: invisible-island.net/xterm/ctlseqs/ctlseqs.html; Wikipedia
      «DEC Special Graphics»; xterm `convtbl.c`; VT510 manual — bitsavers
      (pdf/dec/terminal/vt510); демо-приёмка: simcity.c (Anders Gräsjo), nInvaders.

### Приоритет и связка (сужено 2026-09-16: не универсальный стенд, а VT для shell/vim/tmux/OnyxOS)

**Оставить:** `vttest` + `VttestHarnessTest` goldens как формат регрессий, `xterm`/`ref/vttest/charsets.c` только для спорных VT-таблиц, текущие scrolling property-тесты.

**Добавить ровно 6 регрессий:**
- UTF-8: `split write`, `0xFF` + resync, `wide at EOL`
- Resize: `altBuffer`, `scroll region`, `DECCOLM 132→80` (cursor/visible rows/buffer)

**VT-регрессии только по факту:** DEC graphics U+2500 (tmux) ✅, SGR bold ✅, margins/DECOM ✅ (`decom-margins` fixture), wrap ✅ (`line-wrap` + `no-autowrap` fixtures), scrollback ✅ (`scrollback` fixture), `simcity`/`nInvaders`.

**Отложить:** headless xterm runner, массовый fuzz, `terminal-core` модуль, kitty/sixel/hyperlinks/clipboard, широкая Unicode/emoji матрица, копирование `libvterm`/`kitty` без нужды. `TerminalBufferTest` disabled — оставить до `§42` core split.

Критерий DONE следующего этапа: §44.1/§44.2 зеленые (`dec-special-graphics` xfail→pass) + 6 тестов, без расширения харнесса.

1. **44.2** — вечер, разминочный.
2. **44.1** — ~день работы, максимальный видимый эффект (tmux/нcurses сразу красивее).
3. **44.4** слой 1 — по ходу, слой 2 — когда будет GameTest-хук из §41.1.
4. **44.3** — после, с отдельным планом на PR-B.
5. **§42 Этап 1** (core/neoforge split, «builds both at once») — после текущего раунда фиксов в мастере.

## 45. GameTest CI: настоящая базовая линия (2026-09-16) — ✅ DONE, см. PR ci/gametest-parallel

GameTest раньше никогда не выполнялся (ни локально, ни в CI — тихий exit 0). Починено:
`@GameTestHolder`, свой `empty.nbt`, `GameTestResultReporter` (фейлит на пустой/отсутствующий
отчёт, закрывает vacuous-green), `ci-work.yml` разрезан на `lint`/`test`/`gametest`.

- [ ] Осталось: 9 тестов помечены `required = false` (список причин — в истории git/PR),
      снять флаг после починки; прогнать vttest-страницы через GameTest-слой 2 (§44.4)
      поверх живой инфраструктуры.

## 46. Отложенные хвосты Dana'иного "hard"-таска (terminal sweep chunk 1-3, PR #45/#46/#49)

Все три PR смёржены, но каждый честно задокументировал мелкие отложенные несоответствия —
фиксирую здесь, чтобы не потерялись:

- [x] (chunk 1, PR #45) `Utf8Decoder` ресинкается иначе, чем xterm-410's `decodeUtf8` при
      lead byte в середине multi-byte последовательности — сверить и привести к параметру.
      ✅ Частично исправлено: `processContinuation` теперь ресинкается на lead byte (`0xC0` маска)
      вместо stitching low 6 bits в предыдущий кодпоинт; xterm дополнительно эмитит `UCS_REPL`
      за оборванную последовательность, у нас — silent drop (без REPL) до отдельного решения.
- [x] (chunk 1, PR #45) Shift-грязные scrollback-строки вне видимого окна не уходят клиенту,
      пока view не переприклеится к низу — pre-existing, лечится текущим mark-all при вводе,
      но не устранено на уровне протокола.
      ✅ Исправлено 2026-09-17 вместе с chunk 3 (общий корень — см. ниже): `TerminalDiff.capture`
      раньше на full-refresh отбрасывал `dirty.rows()` целиком и подставлял только
      `visibleWindowRows()`, из-за чего `Terminal.markAllBufferRowsDirty()` (единственный вызов —
      `TerminalBuffer.clearScrollback`, ED `3 J`) молча терялся. Теперь `capture` шлёт объединение
      видимого окна и явно помеченных строк вне него (`TerminalDiff.fullRefreshRows`). Тест
      `eraseScrollbackFullRefreshShipsOffScreenRowsNotJustVisibleWindow`.
- [x] (chunk 2, PR #46) XTRESTORE восстанавливает флаг DECCOLM, но не сам resize — xterm
      маршрутизирует восстановление режима через DECSET update path, мы нет.
      ✅ Исправлено: `XTRESTORE.execute` теперь вызывает `resetRendition()` + `setWidth()`
      при восстановлении DECCOLM, как это делают CH2/CH3; тест
      `xtrestoreDeccolmAlsoRestoresColumnWidth`.
- [x] (chunk 3, PR #49) >32 shift-операций в одном diff-окне сбрасывают бэклог и форсят
      full refresh — scrollback выше видимого окна расходится с реальностью с этого момента
      (full refresh перерисовывает только видимое окно). Нужен протокольный фикс (geometry
      epoch либо отправка scrollback вместе с full refresh).
      ✅ Исправлено 2026-09-17: выбран вариант «scrollback вместе с full refresh». Overflow-ветка
      `Terminal.recordNetworkShift` теперь помечает грязными ВСЕ буферные строки
      (`networkDirtyRows.set(0, height * SCROLL_BACK_COUNT)`), не только флаг `fullRefresh` —
      в паре с фиксом `TerminalDiff.capture` выше это даёт самозаживление: один full-refresh
      diff перевозит весь scrollback и клиент больше не расходится навсегда. Тест
      `shiftBacklogOverflowSelfHealsFullScrollbackOnNextCapture`. Компромисс: этот diff разово
      большой (до `height * SCROLL_BACK_COUNT` строк вместо `height`), но триггер редкий
      (>32 физических сдвигов в одном окне — очень быстрый вывод на упёртой в капасити
      scrollback), и корректность важнее.
- [ ] (chunk 3, PR #49) Accepted residue: возможен tear содержимого ячейки между кадрами,
      и мутация палитры на месте (in-place) — не блокер, но известная неточность рендера.

---

## 47. Аудит 2026-09-17 — 6 суб-агентов (сенior-ревьюер, HEAD текущего)

Полный аудит: структура/архитектура, логика (границы/массивы/арифметика), потокобезопасность, стиль/сборка, тесты, контракты/безопасность. Верификация чтением исходников. Ниже — только то, что надо исправить (опровергнутые гипотезы см. в отчёте аудита, не дублируются).

### Блокеры (детерминированные краши / потеря данных / неверная security-логика)

- [ ] **Б1 — DeltaFrameCodec tilesX==0 → ArithmeticException на client net thread** `[common/vm/video/DeltaFrameCodec.java:248]`: `tileIndex % tilesX` при `width==0` (tilesX=0) → краш клиента. Фикс: guard `if(tilesX==0) return Optional.empty()` в `applyOneTile` + `long frameBytes=(long)width*height*2` c overflow-проверкой и капой `<=32MiB` в `encode/decode`.
- [ ] **Б2 — InternetManagerImpl.tasks LinkedList cross-thread без синхронизации** `[common/inet/internet/InternetManagerImpl.java:44-45,83,144]` : Server пишет `tasks.add()`, Internet читает `removeIf()` → CME/потеря задач, интернет-молчание. Фикс: `ConcurrentLinkedQueue<TaskImpl>` или `synchronized(tasks)` в обеих точках.
- [ ] **Б3 — RPCDeviceBusAdapter handoff без volatile** `[common/bus/adapter/RPCDeviceBusAdapter.java:49,80,101-129] + MethodInvoker.java:64`: `synchronizedInvocation` и `isPaused` plain → потеря синхронного RPC, зависание VM на `while(...null)`. Фикс: `volatile` (+ `isPaused` volatile).
- [ ] **Б4 — Ipv4Space/IntegerSpace signed TreeMap → обход deniedHosts/allowedHosts** `[common/util/misc/IntegerSpace.java:10,77] + [common/inet/util/InetUtils.java:92] + [common/inet/util/Ipv4Space.java:10]`: `TreeMap<Integer,Integer>` signed, `count()` int overflow, `put("0.0.0.0/1")`/`128.0.0.0/1` порядок неверен → фильтр безопасности не матчит. Фикс: `new TreeMap<>(Comparator.comparingInt(Integer::compareUnsigned))`, `count():long` через `(long)value - key +1L`, `getSubnetByPrefix` разрешить 0..32, regex `prefix` → `(?:[0-9]|[12][0-9]|3[0-2])`, `interfaceIdPattern \\d+`.
- [ ] **Б5 — FrameChunker.slice без валидации index** `[common/network/util/frame/FrameChunker.java:22]`: `index<0` или `>=chunkCount` → `NegativeArraySizeException`/AIOOBE на net thread. Фикс: `if(index<0||index>=chunkCount(frame.length)) throw IAE`.
- [ ] **Б6 — Terminal.renderers итерация без synchronized** `[common/vm/terminal/Terminal.java:172,785,787,800]`: `synchronizedSet` требует `synchronized(renderers)` при итерации → CME на VM thread или потеря dirty. Фикс: `synchronized(renderers){ forEach... }` в `markDirty/markAllDirty` и `getRenderer/releaseRenderer`.
- [ ] **Б7 — SimpleFramebufferDevice dirtyLines гонка VM vs server** `[common/vm/device/SimpleFramebufferDevice.java:13,21,37,62,106,120]`: `store/setDirty/hasChanges` без lock vs `copyFrame/close` с lock → `BitSet` word tear / AIOOBE / чёрный экран. Фикс: `store`+`setDirty`+`hasChanges` под `lock`.

### Критичные логические баги (major)

- [ ] **Л1 — IntegerSpace.count() int overflow** `[IntegerSpace.java:77]` → см. Б4 (long).
- [ ] **Л2 — InetUtils.getSubnetByPrefix reject 31/32/0 + regex не пускает 0.0.0.0/0** `[InetUtils.java:128] + [Ipv4Space.java:19]` → см. Б4.
- [ ] **Л3 — DeltaFrameCodec width*height*2 overflow** `[DeltaFrameCodec.java:78,189]` → см. Б1.
- [ ] **Л4 — Terminal hasPendingBell без volatile/lock** `[Terminal.java:hasPendingBell]` : VM пишет, server читает `bell=hasPendingBell; hasPendingBell=false` без lock → потеря bell. Фикс: `volatile boolean hasPendingBell` или под `networkDirtyLock` как `paletteRevision`.
- [ ] **Л5 — VMRunner cycleLimit/cycles/runtimeError без volatile** `[common/vm/VMRunner.java:46,48,49]` : stale квота/невидимый краш. Фикс: `volatile long cycleLimit/cycles` + `volatile Component runtimeError`.
- [ ] **Л6 — GlobalInterruptController raisedInterruptMask RMW без атомарности** `[common/vm/context/global/GlobalInterruptController.java:9]` → потеря прерывания. Фикс: `AtomicInteger` или `synchronized`.
- [ ] **Л7 — ServerScheduler synchronizedMap итерация без блока** `[common/util/scheduler/ServerScheduler.java:181]` → CME. Фикс: `synchronized(levelTickSchedulers){ for(...) }`.
- [ ] **Л8 — InternetConnectionImpl.isStopped / TaskImpl.closed без volatile** `[InternetConnectionImpl.java:79] + [TaskImpl.java:7]` → неудаляемая задача/коннект. Фикс: `volatile`.
- [ ] **Л9 — TunnelManager.managerInstance без volatile** `[common/vxlan/TunnelManager.java:61]` → публикация гонки. Фикс: `volatile`.
- [ ] **Л10 — NBTDeserializerImpl маскирует ошибку формата** `[common/serialization/nbt/NBTDeserializerImpl.java:103]` : возврат `into` при неожиданном Tag → должен `throw SerializationException`.
- [ ] **Л11 — RPC JSON десериализаторы NPE без has/isJsonNull** `[common/bus/adapter/MessageJsonDeserializer.java:16] + [MethodInvocationJsonDeserializer.java:14]` → `writeError(NPE)` вместо `JsonParseException("missing 'type'")`. Фикс: явные проверки `has("type")`.
- [ ] **Л12 — NetworkMessages fallback broadcast всем измерениям** `[common/network/NetworkMessages.java:55-62]` → утечка экрана в другие измерения. Фикс: фильтр `player.level()==hostLevel` или аналог `sendToPlayersTrackingChunk`.
- [ ] **Л13 — common → client 6 импортов** `[ComputerVirtualMachine.java:5] → LoopingSoundManager, [MonitorStateManager.java:5] → MonitorGUIRenderer, [MonitorBlockEntity.java:4], [BusCableModelData.java:6], [NetworkConnector*.java:9], [BusCableInteractionHandler.java:7]` → ломает dedicated server, инвертировать через EventBus/DistExecutor.

### Потокобезопасность — дополнения к блокерам (см. отчёт §5)

- [ ] **П1 — Terminal buffer/colors/styles без лока vs serializeRow** `[Terminal.java:58] + [TerminalDiff.java:284]` : tear кадра (accepted, но докум. как риск) — опционально вынести в `networkDirtyLock` или задокум. как known.
- [ ] **П2 — ServerScheduler SimpleScheduler listeners без синхронизации** `[SimpleScheduler.java:8]` `for(Runnable r:listeners)` без `synchronized` vs `add/remove`.
- [ ] **П3 — SocketManager usesCount без синхронизации** `[SocketManager.java:29]` → double-create leak Selector.
- [ ] **П4 — BusElementManager scanDelay off-by-one** `[BusElementManager.java:91]` 101 тик вместо 100 — minor.

### Стиль / сборка (minor)

- [ ] **Стиль-1 — checkstyle 41 правило глобально подавлено** `[config/checkstyle/checkstyle.xml:21-148]` SuppressionSingleFilter без files → скрыты 98 строк >120 (Network.java:58 len151, Terminal.java:938 len292) и Terminal 973 строки. Фикс: убрать суппрессии `LineLength/FileLength/MethodLength` или задокум. как техдолг.
- [ ] **Стиль-2 — qodana.yaml excludes расходятся с checkstyle/spotbugs** `[qodana.yaml:6] vs [checkstyle.xml:15]` — синхронизировать (generated/gametest).
- [ ] **Стиль-3 — gradle.properties динамические ccl 4.6.1.+ / cbm 3.5.0.+** `[gradle.properties:32-33]` → пин `strictly`, добавить `gradle.lockfile`/`verification-metadata.xml`, убрать дубль `fileTree(libs)` vs `maven libs` `[build.gradle.kts:226 vs 168]`.
- [ ] **Стиль-4 — System.out в gametest** `[gametest/DeviceBusTests.java:34] + [RedstoneInterfaceTests.java:44]` 5× `println` вне линта из-за `exclude "**/gametest/**"` `[build.gradle.kts:408]` → добавить `// NOPMD` или включить gametest в checkstyle с фильтром.
- [x] **Стиль-5 — бинарники natives в репо** `[src/main/resources/natives/ 8 файлов]` закоммичены (осознанно для оффлайн), `gradle/wrapper.jar` тоже — задокументировано в `CONTRIBUTING.md` (2026-09-17).
- [ ] **Стиль-6 — магические числа без констант** — checkstyle MagicNumber отсутствует, PMD тоже — ввести `FULL_DIRTY_MASK`, `BLINK_*`, `TAB_WIDTH` константы (см. §36 m12).

### Контракты / безопасность (дополнения)

- [ ] **КБ-1 — JSON десериализаторы** см. Л11.
- [ ] **КБ-2 — NetworkMessages broadcast** см. Л12.
- [ ] **КБ-3 — TunnelManager volatile** см. Л9.
- [ ] **КБ-4 — ImportFileRequestManager forged sender** — текущее `!PendingPlayers.contains(sender)` не consume request — корректно, добавить тест на `sender==null`.

### Тесты — дыры (high)

- [ ] **Т1 — VM ядро 0 тестов**: `vm/{VirtualMachine,VMRunner,runner/*,lifecycle/*,context/*}` → добавить `VMRunnerTest`, `VMLifecycleTest`.
- [ ] **Т2 — Bus 0**: `common/bus/*` (~60 файлов) только `ImportFileRequestManagerTest` + 3 gametest → `BusControllerTest`, `GroupManagerTest`, `RPCAdapterTest`.
- [ ] **Т3 — inet неполно**: нет `UDP/DHCP/DNS`, `TcpStates` (SYN_SENT...), `Ethernet/LinkLocal` → расширить `DefaultNetworkLayerTest` + новые.
- [ ] **Т4 — конкуренция**: только `FrameStateTest` + `AsyncVideoEncoderTest` → добавить `VMRunner tick vs TerminalDiff capture` multithread, `SessionManager` под потоком.

### Архитектура (средний приоритет, не блокер)

- [ ] **А1 — Terminal God 973 строки** `[Terminal.java:27]` → выделить `TerminalNetworkState`/`TerminalRenderState`, `ModeTable` уже выделен, но `buffer/colors/styles` + dirty + seqlock остались.
- [ ] **А2 — циклы пакетов** `blockentity↔bus.provider`, `vm↔bus.device.vm` → разорвать через `DeviceFactory`/`api` интерфейс.
- [ ] **А3 — теневое имя TunnelManager** `[NetworkTunnelDevice.java:42]` внутренний `TunnelManager` перекрывает `common.vxlan.TunnelManager` → переименовать в `TunnelEndpointRegistry`.

Приоритет внедрения: Б1-Б7 → Л4-Л9 → Л11-Л13 → Стиль-1/3 → Т1-Т2 → остальное. Верификация каждого: `./gradlew checkstyleMain pmdMain spotbugsMain lintRatchet test gameTest` + ручной прогон.
