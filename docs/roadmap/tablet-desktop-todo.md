# Планшет Onyx — новый рабочий стол и запуск Minecraft

Статус: **выполнено** (2026-11-09, вечер Europe/Warsaw).

## Что сделано

### 1. Корпус и управление Minecraft

- `TabletTerminalScreen` переведён на собственный корпус 296×224 с экраном
  256×160 в точке (20,38); vanilla-спрайты кнопок не используются.
- Новый `TabletControl` рисует вкладки Screen/Console, питание, захват ввода
  и инвентарь; активная вкладка и фокус выделены, tooltip переведены.
- Сохранены Ctrl→hard off, Shift→reboot, обычное выключение, отпускание
  захвата и переключение вкладок без остановки ВМ.

### 2. Настоящий рабочий стол Onyx

Репозиторий `OnyxApps`, коммит `6a3ad99` (ветка слита в `main` fast-forward):

- `apps/otablet/desktop/`: `desktop.h`, `state.c`, `paint.c`, `device.c`
  (четыре файла, каждый < 200 строк).
- Часы с реальным uptime, System с `/proc/meminfo`, Notes с вводом,
  сохранением в `/users/tablet-notes.txt` и повторной загрузкой.
- Нижний dock: Clock, Notes, System, Snake, Shell; окна закрываются,
  открываются снова и получают фокус.
- Snake запускает настоящий `/bin/osnake` дочерним процессом, забирает
  framebuffer/TTY, дожидается выхода и возвращает рабочий стол.
- Shell возвращает в консоль; Q/правый клик также выходят.
- `otablet` собирается с `--ring1`: ядро разрешает создание файлов только
  в ring 1, иначе сохранение заметок возвращает `EPERM`.

### 3. Новый stock-образ

- Пакетный root пересобран helper'ом `assemble-onyx-stock.py`:
  - root SHA-256: `ffd874d2eeded1eb81d43df90820e1e48b92e5386b5578b0d0e9df3f63731e21`;
  - kernel SHA-256 не менялся: `34b38649d85c373b3194d96f8e8fcd34303b8c9d5b579cf5c07d58528476a95d`;
  - payload 16 117 760 байт, дополнен до 16 МиБ;
  - прежние `/bin/dnstest`, `/bin/oed`, `/bin/ohttp` сохранены байт-в-байт.
- Образ обновлён в `src/main/resources/onyxos/` и в image-ветке
  (`ece14316`); `docs/ONYXOS.md` описывает новый рабочий стол и SHA.

## Проверки

- `make -C tests/native/tablet test` — PASS.
- `make otablet ONYXCC=.../onyxcc` — PASS, флаг ONX ring1 = 0x2.
- Реальный Sedna 5.4, 16 МиБ, 12.5 МГц, экран 256×160, absolute pointer:
  `ONYX_DESKTOP_SMOKE_PASS RAM=16 MHz=12.5 windows=close,reopen notes=hello
  child=osnake,return` (лог `/tmp/onyx-desktop-smoke-final2.log`).
- Fabric 26.1.2 client GameTest с `-Pfabric.clientTests=true
  -Pfabric.tabletTests=true` — `BUILD SUCCESSFUL`; проверены реальные клики,
  закрытие/повторное открытие окон, заметки, osnake, инвентарь и возврат
  захвата. Скриншоты:
  `fabric/versions/26.1.2/build/run/clientGameTest/screenshots/`.
- Checkstyle 1.21.1/26.1.2/26.2, PMD и SpotBugs 1.21.1 — PASS.
- Client fixture компилируется для 26.1.2 и 26.2 с tablet-флагами.

## Слияние веток

- OC2R: `feature/tablet-graphics` и `feature/tablet-image` слиты в `work`.
- OnyxApps: `feature/tablet-widgets` слита в `main` (fast-forward `6a3ad99`).
- Push/публикация не выполнялись.

## Запуск для показа

Подготовлен `/tmp/oc2r-tablet-preview/`: отдельный QA-мир, свежий flash/CPU
и новый UUID планшета. `launch.sh` запускает обычный Fabric 26.1.2 клиент
без авто-выхода, открывает планшет, входит root, запускает `otablet --device`
и включает Screen/input capture.

## Что не входит

Экран на предмете в руке, слот HDD и периодическое сохранение против
серверного краша не реализованы. Zed-задача отменена.
