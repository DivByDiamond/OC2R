# TODO: планшет Onyx — новый рабочий стол и запуск Minecraft

Обновлено 2026-11-09, около 21:34 Europe/Warsaw. Это передача состояния незавершённой работы.
Продолжать реализацию; пользователь попросил записать TODO из-за лимита, но не останавливать работу.

## Запрос и разрешения

Пользователь отверг прежний минималистичный `otablet --device`, фиолетовые полосы/счётчик и стандартные
кнопки Minecraft. Нужно: собственный красивый корпус/кнопки, понятные окна виджетов с закрытием,
нижняя панель запуска настоящих приложений; запустить обновлённый Minecraft на его рабочем столе.
Коммиты и слияния локальных веток разрешены. Не делать push/публикацию. В сообщениях коммитов и
трейлерах не упоминать автоматизацию/соавторство. Не менять пользовательские миры и настройки.

## Последнее полностью готовое состояние (до нового дизайна)

- OC2R `/storage/project/jvm/oc2r`, ветка `work`, код `aa5b3763`, документация `2f307c1e`.
- Задний RPC-разъём сенсора, графический планшет 256×160 RGB565, мышь/клавиатура и переключатель
  Screen/Console одной ВМ уже слиты. Радио/AP/enrollment/защита клонов сделаны ранее.
- Старый простой счётчик работал в настоящем Minecraft: клик, видимая цифра 1, Q→оболочка,
  переключение вкладок/инвентаря и освобождение захвата проверены.
- Старые шесть сборок и отчёт: `build/verified/aa5b3763/README.md`. Они **не содержат новый дизайн**.
- Предыдущие проверки: 2508 unit и 581 GameTest (137/139/139 NF, 166 Fabric1.21.1), без ошибок.
  Эти результаты не заменяют проверки нового рабочего стола.
- Оригиналы Onyx уже содержат Kernel `7b2d655`, Apps `1f3df66`, OS `8c8d3fa`.
- Старый packaged kernel SHA256 `34b38649d85c373b3194d96f8e8fcd34303b8c9d5b579cf5c07d58528476a95d`;
  старый root SHA256 `ceded23f51ed030f0bb21b3238ff171894249605d13f66c73e880fe2afe7b9d6`.

## Незавершённые изменения: НЕ УДАЛЯТЬ / НЕ RESET

### 1. Корпус/управление Minecraft

Рабочая копия `/tmp/oc2r-tablet-graphics`, ветка `feature/tablet-graphics`, база `2f307c1e`.
Незакоммиченные файлы:

- `src/main/java/li/cil/oc2/client/gui/screen/tablet/TabletTerminalScreen.java`
- новый `src/main/java/li/cil/oc2/client/gui/widget/tablet/TabletControl.java`
- `fabric/src/clientGametest/java/li/cil/oc2/fabric/gametest/FabricTabletTest.java`

Реализовано: корпус 296×224, экран 256×160 с отступом (20,38), собственные рисованные кнопки
без vanilla sprites, понятная выделенная активная вкладка, питание/ввод/инвентарь, винты/решётки,
янтарный индикатор. Сохранены Ctrl→hard off, Shift→reboot, обычный shutdown и переведённые tooltip.
Палитра: graphite #17232D, rim #405866, recess #080F14, ivory #E6E5D8, teal #63C7BE, amber #E5B66F.

Первый all-six compile прошёл: `/tmp/oc2r-tablet-shell-compile.log` (1m56).
После tooltip-правки новая quality+all-six compile идёт в `/tmp/oc2r-tablet-shell-quality.log`;
сеанс exec `38314` (если ещё жив). Checkstyle нашёл ошибки, нужно дочитать/исправить после окончания
компиляции; PMD прошёл, SpotBugs ещё анализировался. Не править Java во время javac.

Осталось:

- [ ] Дождаться quality, исправить Checkstyle/прочие реальные findings, повторить нужные проверки.
- [ ] Компилировать client fixture с **`-Pfabric.clientTests=true -Pfabric.tabletTests=true`**,
      иначе compileGametestJava проверяет только shared server fixtures! Проверить 26.1.2 и 26.2.
- [ ] Дождаться нового packaged Onyx; прогнать настоящий Fabric26.1.2 клиент через private Xvfb.
- [ ] Проверить новый корпус, активные вкладки, EN/RU и кнопки реальным кликом, снять скриншоты.
- [ ] Закоммитить проверенный дизайн нейтральным сообщением и слить в `work`.

### 2. Настоящий рабочий стол Onyx

Обязательно прочитать `/storage/project/systems/Onyx/AGENTS.md`.
Шесть независимых Git-репозиториев; команды Git выполнять в соответствующем компоненте.
Изолированные компоненты: `/tmp/onyx-tablet-workspace/{OnyxKernel,OnyxApps,OnyxOS,OnyxCompiller,OnyxShell,OnyxBoot}`.

Рабочая копия Apps `/tmp/onyx-tablet-workspace/OnyxApps`, база `1f3df66`.
Незакоммиченные изменения:

- новая `apps/otablet/desktop/{desktop.h,state.c,paint.c,io.c,device.c}`;
- `apps/otablet/main.c`: `--device` теперь запускает desktop, mock/PPM toolkit сохранён;
- `apps/otablet/host/font.c`;
- `tests/native/tablet/Makefile`, новый `tests/native/tablet/desktop.c`.

Реализованы full256×160 desktop, светлые окна Clock/Notes/System с X, нижний dock
Clock/Notes/System/Snake/Shell, порядок/фокус/закрытие/повторное открытие окон.
Clock показывает реальный uptime, System читает `/proc/meminfo`, Notes редактируется и сохраняется
в `/users/tablet-notes.txt`. Snake должен запускать настоящий `/bin/osnake` дочерним процессом,
отдавать ему framebuffer/TTY, ждать выхода и возвращать desktop. Shell выходит в консоль.
Новых изменений Kernel не планируется.

Actual renderer previews (не макеты):

- `/tmp/onyx-desktop-preview-4x.png`
- `/tmp/onyx-desktop-notes-preview-4x.png`

Направление дизайна просмотрено и принято: navy фон, ivory окна, teal заголовки, amber память,
читаемые названия в dock. Последняя правка форматирует память в MiB и ещё требует повторной проверки.

Найденные и уже внесённые исправления, которые надо проверить:

- `/proc/meminfo` реально выводит `MemTotal\t: ` и `MemFree\t\t: `: разбор через двоеточие,
  а не фиксированный offset; native exact-format test добавлен.
- Notes ограничены шестью видимыми строками, чтобы не вводить невидимый текст.
- Быстрый native256×160 RGB565 contiguous write, без 40 тысяч делений/160 syscalls на кадр;
  перерисовка только dirty/движение курсора, курсор восстанавливается после кадра.

Предыдущие native/target результаты:
`/tmp/onyx-desktop-native.log` и `/tmp/onyx-desktop-target.log` — PASS до последней MiB-правки.
Команды:

```sh
make -C tests/native/tablet test
make otablet ONYXCC=/tmp/onyx-tablet-workspace/OnyxCompiller/onyxcc
```

Осталось:

- [ ] Повторить native/target после последних изменений.
- [ ] Настоящий Sedna с **16 МиБ**, 256×160: close/reopen, ввод/сохранение/повторный запуск Notes,
      настоящие значения памяти, короткий клик, Snake child→Q→desktop, Shell→оболочка.
- [ ] **Child lifecycle ещё НЕ ДОКАЗАН**. Это главный риск: не подменять запуск картинкой/фиктивным успехом.
- [ ] Проверить отзывчивость при 12,5 МГц и отсутствие stale/held pointer после child.
- [ ] Коммит Apps, обновить точный lock OS, собрать locked stock image и проверить его.
- [ ] Fast-forward оригинальные Apps/OS только после проверки их status; не затрагивать чужие изменения.

### 3. Новый stock image для мода

Рабочая копия `/tmp/oc2r-tablet-image`, ветка `feature/tablet-image`, последний коммит `89d51f31`.
**Новый desktop image пока не собран.**

Существующий воспроизводимый helper `scripts/demo/assemble-onyx-stock.py` принимает fresh manifest,
previous root, kernel, rebuilt seven tools, mkimage и отдельный output directory. Сохраняет все прежние
`/bin` программы, оставляет `dnstest/oed/ohttp`, накладывает семь OC2R инструментов, проверяет байты,
ограничивает root16MiB. Не перезаписывать существующие частные COW root пользователей.
Семь собранных инструментов в `/tmp/oc2r-tablet-image/build/guest-tools`.
Прежние candidates `/tmp/oc2r-tablet-font-stock`, `/tmp/onyx-tablet-final`; это старый счётчик.

- [ ] Собрать новый lock-selected Onyx образ через aggregate OnyxOS, проверить выбранные `.vent/repos`.
- [ ] Собрать отдельный candidate helper'ом, повторить actual16MiB desktop/child/notes smoke.
- [ ] Только после этого заменить единственную пару ресурсов `src/main/resources/onyxos/` в image worktree,
      kernel оставить прежним, если Kernel не менялся; обновить docs/ONYXOS.md SHA/коммиты/контракт.
- [ ] Проверить diff и закоммитить/слить image branch.

## Клиентский acceptance контракт нового desktop

Обновлённый `FabricTabletTest.java` находится в незакоммиченном graphics worktree.
Ориентир — актуальный код, если координаты изменились; обе стороны должны согласовать контракт.

- UART `OTABLET_READY 256x160`, `OTABLET_DESKTOP_READY`.
- Clock X: (108,30), `OTABLET_ACTION=101`; Clock dock: (32,148), ACTION1.
- Notes dock: (80,148), ACTION2; ввести native H,E,L,L,O; Save (216,119), `OTABLET_NOTES_SAVED=5`.
- Notes X (236,28), ACTION102; reopen, проверить glyph H в (20,45), foreground RGB5651987.
- Snake dock (176,148): `OTABLET_APP_STARTED=osnake`, nativeQ, `OTABLET_APP_RETURN=osnake STATUS=0`.
- Shell dock (224,148): `OTABLET_EXIT`; UART `cat /users/tablet-notes.txt` должен вывести hello.
- Header Screen/Console, capture off/on, inventory проверяются реальным MouseButtonEvent/hit test,
  не прямым `showGraphics`/menu method.
- Подсчёт новых маркеров должен сравнивать полные строки: ACTION1 не должен совпадать с ACTION101.
- Избегать screenshots частичного первого кадра; ждать стабильного полного desktop.

Private client пример:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk LIBGL_ALWAYS_SOFTWARE=true ALSOFT_DRIVERS=null \
  xvfb-run -a -s '-screen 0 1280x720x24' \
  ./gradlew :fabric:26.1.2:runClientGameTest \
  -Pfabric.clientTests=true -Pfabric.tabletTests=true --max-workers=1 --console=plain
```

Сначала native/Sedna+скриншот-review, затем client. Ресурсная загрузка клиента обычно около двух минут.

## Запустить видимый Minecraft пользователю (ещё НЕ запущен)

Подготовка целиком вне Git: `/tmp/oc2r-tablet-preview/`.

- `launch.sh` — normal Fabric26.1.2 runClient, Java25, DISPLAY=:1, unset WAYLAND_DISPLAY,
  silent audio, workers1, Gradleheap1GiB, **без автоматического выхода**.
- `preview.init.gradle` — добавляет локальный QA initializer только в отдельный detached checkout,
  не включать в release JAR и не выполнять на основной рабочей копии.
- `src/qa/TabletPreview.java` — открывает новый планшет UUID с новой flash и CPU, стартует ВМ,
  входит root, запускает `otablet --device`, ждёт READY, включает Screen/input capture и оставляет
  клиент интерактивным. Последняя правка root: ждать READY, не переключаться до старта приложения.
- `run/saves/Tablet Preview` — копия прежнего QA мира, session.lock не копировался.
  Чужие миры не трогались. Новая flash обязательна: старый COW root не получает новое приложение.

После финального коммита:

```sh
git worktree add --detach /tmp/oc2r-tablet-preview/checkout FINAL_SOURCE_COMMIT
bash /tmp/oc2r-tablet-preview/launch.sh > /tmp/oc2r-tablet-preview/client.log 2>&1
```

Запускать длительным exec/session или detached process так, чтобы Minecraft остался работать.
**Первый compile QA-helper/init ещё не проверен**: при ошибке исправлять только `/tmp` helper,
не production API ради демонстрации. Подтвердить в логе READY и живое окно, сделать screenshot
видимого клиента; не закрывать его после проверки. Не запускать второй heavy client одновременно.

## Ограничения и безопасная интеграция

- RAM компьютера15GiB, swap почти полон. Только один тяжёлый Gradle/client за раз,
  `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`, `--max-workers=1`.
- Java не редактировать во время javac: ранее ErrorProne падал от этого.
- В оригинальном Kernel есть **чужая незакоммиченная работа xHCI**:
  `kernel/src/drivers/bus/usb/xhci/{device.rs,init.rs,mod.rs,regs/ring.rs,xfer/}`. Не reset/stash/commit её.
- Build Onyx иногда меняет tracked compiler/Shell outputs. Восстанавливать только свои доказанные
  generated изменения в изолированных worktree, не чужие исходники.
- Новых ABI/kernel-изменений пока нет; нужны desktop Apps + lock + packaged root + host UI.
- После завершения: review, commit/merge веток, targeted tests и all6compile/assemble;
  release сборки без `-Pfabric.clientTests`/tabletTests.
- Fabric release не содержит GameTest/QA классов; NeoForge исторически содержит зарегистрированные
  GameTests в main — это существующий стиль, не надо менять упаковку в рамках дизайна.
- Скопировать новые шесть JAR в основное build/libs и `build/verified/NEW_SOURCE_HASH`, SHA256SUMS,
  screenshots/README/verification.json. Старые результаты не называть проверкой новой версии.
- Запустить видимый клиент с финальным образом и оставить открытым; сообщить пользователю,
  что реально готово и как открыть/закрыть окна и запустить приложения.

## Что не входит в завершённый объём

Экран на предмете в руке, слот HDD и периодическое сохранение против серверного краша пока не сделаны.
Не считать их реализованными после редизайна. Zed-задачу пользователь отменил — к ней не возвращаться.
