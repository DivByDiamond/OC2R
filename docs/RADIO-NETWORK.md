# ORA radio network / Радиосеть ORA

Development documentation. This describes the current working branch, not a guarantee that a published OC2R build includes the feature.

- [Русский](#русский)
- [English](#english)
- [Implementation plan](roadmap/tablet-radio.md)

## Русский

### Для чего нужна сеть

Радиосеть соединяет обычные кабельные сети через вышки. Пример:

```text
ПК A → сетевой коннектор и синий кабель → вышка A
                                           ↕ радио
                                       вышка B
                                           ↕ радио
ПК B ← сетевой коннектор и синий кабель ← вышка C
```

Вышки передают Ethernet-пакеты. Поэтому программы на ПК сохраняют обычную работу с TCP/UDP. Настройка IPv4 в гостевой ОС остаётся отдельной задачей: ORA не назначает ей IP автоматически и не заменяет MAC-адрес сетевой карты.

Сеть передачи данных и GPS используют одну физическую вышку, но настраиваются отдельно. GPS-команда `joinNetwork()` и знание GPS UUID не дают доступа к передаче пакетов. Переключатель `relative_coordinates` не отключает радиомост.

### Покрытие и маршруты

По умолчанию соседние вышки соединяются на расстоянии до **45 блоков по горизонтали**. Проверка: `dx² + dz² <= 45²`. Покрытие круглое по X/Z. Высота Y может отличаться; стены не мешают. В другом измерении связи нет.

Примеры:

- Смещение X=45, Z=0: связь есть.
- Смещение X=32, Z=32: связи нет, расстояние примерно 45,25 блока.
- Одинаковые X/Z, одна вышка внизу, другая наверху: связь есть.
- Расстояние больше 45: нужна цепочка промежуточных вышек этой же сети.

Для передачи данных главная GPS-вышка не нужна. Она задаёт начало координат, а радиопакеты идут по доступному пути к получателю. Текущий мост распространяет кадр по доступным узлам своей сети; это ещё не коммутатор с изучением MAC-адресов.

Чанк промежуточной вышки можно выгрузить: её последнее подтверждённое положение сохраняется как ретранслятор. Чанк отправителя и получателя должен быть загружен. Выгруженный ПК не выполняет программы. Разрушение вышки удаляет путь; после загрузки чанка сохранённые сведения сверяются с реальными блоками. Сеть не загружает чанки специально.

### Что такое ORA

**ORA = OC2R Radio Address**, по-русски «радиоадрес OC2R».

```text
0001:0002
^^^^ ^^^^
сеть узел
```

Каждая группа содержит четыре символа base32:

```text
0123456789ABCDEFGHJKMNPQRSTVWXYZ
```

Буквы I, L, O и U не используются. Регистр при вводе не важен, отображение всегда в верхнем регистре. Пробелы и неверные символы отклоняются. `0000` в любой группе зарезервирован.

ORA является коротким адресом нашей радиосети. Это собственный формат, а не новая версия IPv4/IPv6. Внутри сервер также хранит UUID сети, UUID узла, измерение и поколение регистрации. Эти данные позволяют проверять подлинную принадлежность устройства. Адрес не вычисляется из координат и не раскрывает мировую позицию вышки.

В первой реализации ORA принадлежит **узлу вышки**. Несколько ПК за одной вышкой сохраняют свои обычные IP/MAC. Отдельные ORA для переносных устройств появятся вместе с подключением роботов и планшетов. Сейчас `getRadioAddress()` показывает адрес узла, а пользовательский метод отправки сообщения прямо на ORA ещё не подключён: существующий мост передаёт обычные Ethernet-кадры.

### Как выдаётся адрес

1. При создании радиосети сервер случайно выбирает свободный ненулевой префикс, например `7K3F`.
2. При одобренном подключении узлу случайно выбирается свободный номер этой сети, например `02Q9`. Вместе получается `7K3F:02Q9`.
3. Генератор выбирает из свободных значений. Уже занятые и ранее зарезервированные номера исключаются, поэтому случайное совпадение не перезаписывает чужой адрес.
4. Выданные адреса и история резервирования сохраняются в мире. Перезапуск сервера не меняет существующий адрес. Если обычным способом сломать вышку и поставить новую из предмета, это новый узел: старый адрес автоматически не переносится.
5. Отозванный адрес не передаётся следующему устройству. Повторная регистрация получает новый случайный свободный номер и новое поколение; прежний ключ больше не подходит.

Адреса не идут по порядку. Цифры и буквы могут встречаться в любой группе. Уже сохранённые адреса старой версии, включая `0001:0001`, остаются действительными и не перегенерируются.

Реестр в текущей реализации отдельный для каждого измерения. Поэтому одинаковый текст ORA может встретиться в разных измерениях; это разные изолированные пространства адресов. По одному тексту адреса нельзя перейти в другую сеть или измерение.

Четыре символа позволяют представить 1 048 575 ненулевых значений в группе. Практические защитные пределы текущего реестра меньше: 256 сетей, 4096 записей устройств, 1024 узла топологии и 65 536 записей истории случайных адресов на измерение. При исчерпании выдача завершается ошибкой, чужой адрес не перезаписывается.

### Подключение и доступ

Первая настройка выполняется через локально подключённую интерфейсную шину вышки. Такой физический доступ считается доверенным для создания новой сети. Синий сетевой кабель передаёт Ethernet, а интерфейсная шина нужна для RPC-настройки. После настройки передача данных не требует постоянно подключённой интерфейсной шины. Публичный UUID или ORA не являются паролем.

Основные методы устройства `gps_tower`:

| Метод | Назначение |
|---|---|
| `createRadioNetwork()` | Создать сеть и получить `network`, `adminSecret` |
| `getRadioEndpointId()` | Получить UUID узла для приглашения |
| `issueRadioInvite(adminSecret, endpointId)` | Выпустить одноразовое приглашение для конкретного узла; результат `invite`, `secret` |
| `joinRadioNetwork(invite, secret)` | Подключить этот узел по выданному ему приглашению |
| `getRadioAddress()` | Показать ORA подключённого узла |
| `getRadioStatus()` | Показать регистрацию, UUID узла и режим защиты |
| `revokeRadioEndpoint(adminSecret, endpointId)` | Отозвать регистрацию узла |

Сначала получите UUID второй вышки, затем выпустите для него приглашение на первой. Передайте приглашение на второй ПК и вызовите `joinRadioNetwork`. Все промежуточные вышки тоже должны быть подключены к этой радиосети.

```lua
local tower = assert(require("devices"):find("gps_tower"))
-- На первом ПК, один раз. Сохраните adminSecret для управления сетью.
local network = tower:createRadioNetwork()
local invitation = tower:issueRadioInvite(network.adminSecret, "UUID-ВТОРОЙ-ВЫШКИ")
-- invitation.invite и invitation.secret нужны на втором ПК.
```

```lua
local tower = assert(require("devices"):find("gps_tower"))
-- На втором ПК. Подставьте значения приглашения.
tower:joinRadioNetwork("UUID-ПРИГЛАШЕНИЯ", "СЕКРЕТ-ПРИГЛАШЕНИЯ")
print(tower:getRadioAddress())
```

`find()` не выбирает ближайшую вышку. Если к шине подключено несколько, выберите конкретное устройство через `devices:get(deviceId)`.

Секрет администратора возвращается при создании сети. Обычный просмотр статуса его не раскрывает. Реестр сохраняет хеши секретов, а собственный ключ узла хранится в серверных данных вышки. Скопированные данные блока не должны позволять второму месту занять ту же зарегистрированную позицию узла. Одноразовые приглашения пока не имеют срока истечения.

### Задержка, TTL и защита

Это разные механизмы:

- **Задержка:** кадр ставится в очередь сервера. По умолчанию каждый радиопереход занимает один тик. Два перехода дают минимум два тика, номинально 100 мс при 20 TPS. Это время одного направления, не ping.
- **Ping:** время туда и обратно, измеряемое монотонными часами (`System.nanoTime`) по фактическому ответу; в RPC вышки это `pingRadio(address, requestId)`, результат приходит в `pollRadioEvents()` как `roundTripNanos`. Число радиозвеньев (`route.hops`) и оценка времени в одну сторону (`route.nominalTicks` и `route.nominalMillis` при номинальных 50 мс на тик) — отдельные значения: это оценка расписания, а не измерение, при просадке TPS реальная задержка больше.
- **TTL:** сколько переходов разрешено пакету. Радиопереходы уменьшают остаток TTL, проводные узлы также учитывают свой расход. Это внутренний TTL сетевой шины OC2R, отдельный от поля TTL в заголовке IP. Число переходов не является временем жизни в миллисекундах.
- **Срок жизни:** слишком старый пакет удаляется из очереди. Пакеты в очереди не сохраняются при перезапуске.
- **Авторизация:** сервер проверяет регистрацию узлов и разрыв/отзыв доступа до доставки. Идентификаторы пакетов и ограниченные очереди сдерживают повторы и переполнение.
- **TLS:** шифрование соединения между приложениями. В текущем радиомосте TLS не реализован. Статус честно сообщает `trusted-local-plaintext-no-tls`.

При потере маршрута, выгрузке получателя или отзыве регистрации ожидающий пакет не доставляется. Смена условий не даёт гарантии доставки; TCP и приложение должны корректно обрабатывать потерю соединения. Базовая задержка пока не моделирует полосу пропускания, помехи или задержку от размера пакета.

Настройки находятся в общем конфиге, секция `[gameplay]`:

```toml
gps_tower_range = 45
robot_dock_range = 10
radioHopDelayTicks = 1
    radioMaxDeliveryTicks = 2
    radioLinkBytesPerTick = 1024
radioMaxHops = 64
radioMaxFrameBytes = 2048
radioQueueLimit = 4096
radioNetworkQueueLimit = 1024
radioDeviceQueueLimit = 256
radioPacketLifetimeTicks = 200
```

После изменения параметров радиопакетов перезапустите сервер. При низком TPS тик длится дольше 50 мс. Настройки дальности, GPS-начала координат и сетевого IP внутри гостевой ОС решают разные задачи.

### Док-станция робота как точка доступа

Док-станция робота — это существующий блок зарядного устройства; отдельного блока нет. Она сама является короткой точкой доступа с круглым радиусом **10 блоков** по X/Z (высота не учитывается), параметр `robot_dock_range` в секции `[gameplay]` (`Config.robotDockRange` в коде; по умолчанию 10, допустимо 1–64). Расстояние считается от центра блока дока до центра робота, граница включительна: ровно 10 блоков ещё подключает.

- Робот с регистрацией в сети подключается к вышке напрямую, если она в радиусе вышки (`gps_tower_range`, по умолчанию 45 блоков). Иначе он подключается к ближайшей док-станции в радиусе 10 блоков, у которой есть вышка, а док-станция выходит в сеть через ближайшую вышку этой же сети в радиусе вышки от дока.
- Док-станция не ретранслирует между вышками, радиусы не складываются: робот обязан быть в радиусе дока, а док — в радиусе вышки.
- Док без вышки поблизости изолирован. Док не входит ни в какую сеть и нигде не регистрируется: сервер ищет зарядные устройства только в уже загруженных чанках вокруг робота и не загружает чанки. Сломанный, заменённый или выгруженный док сразу перестаёт находиться; в другом измерении связи нет.
- Через док робот управляется так же, как на вышке: телеметрия, команды, ping, список устройств, проверка прав. Маршрут получает ещё одно радиозвено; верхний предел задержки доставки и полоса канала не меняются.
- Выход из радиуса даёт потерю связи, возвращение подключает робота заново. Команда, отправленная во время потери связи, завершается как `DISCONNECTED` или `TIMED_OUT` и не повторяется после возвращения.

Результат команды робота определяется только реальным результатом его действия. Гостевая программа вызывает `bindRadioCommand(requestId, actionId)` для действия робота (`move`/`turn`), поставленного в очередь после получения команды; привязка к более раннему, ещё не поставленному или уже занятому другой командой действию отклоняется. `COMPLETED` или `FAILED` сервер выдаёт сам, когда действие реально завершилось; действие, которое робот отбросил без результата (например, при остановке компьютера), даёт `FAILED` с результатом `CANCELLED`. Через `updateRadioCommand` гостевая программа может сообщить только `RUNNING`.

### Ethernet-подключение дока

Док-станция принимает синий сетевой кабель через обычный сетевой коннектор, как и вышка. Кабель сам по себе в сеть не включает: у дока есть собственный клиентский идентификатор, и администратор сети должен пригласить его, как любое устройство. Устройство `charger` получило методы `getRadioEndpointId()`, `joinRadioNetwork(invite, secret)`, `getRadioAddress()` и `getRadioStatus()`; приглашение выдаёт вышка: `issueRadioInvite(adminSecret, endpointId)`.

Кадры из коннектора дока уходят в радиосеть через ближайшую вышку своей сети в радиусе вышки, кадры радиосети записываются во все коннекторы дока. Док по-прежнему не ретранслирует между вышками. Неприглашённый или отозванный док ничего не передаёт, выгруженный или сломанный док перестаёт быть мостом (чанки ради дока не загружаются).

### Учётные данные, отзыв и ротация

Секрет устройства — 256 случайных бит из `SecureRandom` (base64url без заполнения); реестр хранит только SHA-256 хэш. Отзыв действует на одно устройство и не затрагивает остальные; повторное приглашение выдаёт новое поколение, новый случайный адрес и новый секрет, а секрет прежнего поколения никогда не проходит проверку (`authenticate(device, secret, generation)`). Ротация (`rotateCredential`) заменяет секрет одного устройства и увеличивает его поколение; адрес и права сохраняются, остальные клиенты не сбрасываются. Строковые формы записей с секретами (`Enrollment`, `Invitation`, `CreatedNetwork`, `PendingTicket`, `Member`) не содержат секретов и хэшей, поэтому они не попадают в журналы.

Для клиента, чья личность хранится в предмете (будущий планшет), правила заданы на уровне реестра (`RadioClientLeases`): у личности не больше одного активного держателя; каждая активация меняет секрет, и держатель обязан сохранить новый в предмете, поэтому копия предмета со старым секретом никогда не станет второй аутентифицированной личностью; выбрасывание, смерть, выход из игры, смена измерения и передача другому игроку только приостанавливают личность (адрес, права и секрет сохраняются); активация возможна только в измерении регистрации. Состояние активности не сохраняется: перезапуск сервера приостанавливает всех.

### Дубликаты и повторы

Кадр Ethernet, вернувшийся в сеть по петле, узнаётся по содержимому и меньшему TTL в окне 5 тиков и отбрасывается (`RadioFrameFilter`, до 4096 записей); намеренный повтор с тем же TTL проходит. Повторный запрос с тем же request ID от того же устройства возвращает прежнюю команду и не выполняется второй раз, в том числе после переподключения: новая сессия получает известное состояние, а команда, оборванная закрытием сессии, остаётся `DISCONNECTED` и не запускается заново. Принимающая сторона хранит принятые request ID 1200 тиков и игнорирует повторную доставку; повторное сообщение (`MESSAGE`) с тем же request ID доставляется один раз. Вся память о дубликатах ограничена по размеру и времени, при переполнении первой забывается самая старая запись.

### Возраст телеметрии

`getRadioTelemetryStatus(subscription)` возвращает по каждому полю с отсчётом `ageTicks`, `ageMillis` (номинально, 50 мс на тик), `sampleTick`, `sequence` и состояние `ONLINE`, `STALE` или `OFFLINE`. `STALE` наступает, когда возраст достигает порога подписки; `OFFLINE`, когда источник или связь пропали. Поле без отсчёта не показывается как живое.

### Что ещё предстоит

Планшеты и их переключение между точками, прикладные права на сервисы, TLS и живые виджеты остаются следующими этапами. В Onyx уже есть отдельный прототип графических виджетов; его демонстрационные данные пока не приходят из этой радиосети.

## English

### Purpose and transport

The radio bridge joins wired Ethernet networks through enrolled towers:

```text
PC A → blue cable/connectors → tower A ↔ tower B ↔ tower C → connectors/blue cable → PC B
```

Frames retain their Ethernet bytes, so guest TCP/UDP applications keep their existing network stack. Configure ordinary guest IPv4 separately. ORA does not assign an IP to a guest NIC or replace its MAC address.

Data-radio enrollment is separate from GPS membership. Knowing a GPS UUID or calling GPS `joinNetwork()` does not authorize radio frames. Radio does not depend on `relative_coordinates` or require the coordinate-reference tower along a data route.

### Coverage and unloaded relays

The default radius is 45 blocks horizontally, using `dx² + dz² <= 45²`. Coverage is circular in X/Z, ignores Y and walls, and never crosses dimensions. X=45/Z=0 is inside; X=32/Z=32 is outside. Larger distances require an enrolled relay chain.

Saved unloaded towers remain relay nodes without loading their chunks. Sending and receiving endpoints must be loaded. An unloaded computer executes no guest program. Breaking a tower removes its route; loading a chunk reconciles saved topology with actual blocks. The current bridge floods a frame to reachable enrolled endpoints, not a MAC-learning switch.

### ORA and allocation

**ORA means OC2R Radio Address.** The display format is `0001:0002`: a four-character network prefix and a four-character endpoint number.

The canonical alphabet is `0123456789ABCDEFGHJKMNPQRSTVWXYZ`. Input is case-insensitive; output is uppercase. Whitespace and invalid characters are rejected. `0000` is reserved in either group.

The server randomly selects a free nonzero prefix per dimension and a free nonzero endpoint number per network, for example `7K3F:02Q9`. Occupied and historically reserved numbers are excluded before allocation, so a collision cannot overwrite an existing address. Values do not follow a sequence; either group may contain digits and letters. Issued addresses and reservations survive world saves and restarts. Revoked numbers are not reassigned; re-enrollment gets a new random available number and credential generation. Existing sequential addresses from older saves remain valid and are not regenerated.

Full network/device UUIDs and the dimension remain the authoritative namespace. Equal text in different dimensions denotes different isolated addresses. An address is not a credential and has no automatic Internet or foreign-network route. It is not derived from coordinates and does not expose the tower world position. A normally broken/replaced tower creates a new endpoint identity rather than automatically inheriting its old address.

In this first implementation, addresses belong to tower bridge endpoints. PCs behind a tower retain ordinary IP/MAC identities. Separate robot/tablet addresses depend on future wireless client integration. ORA currently identifies the enrolled bridge endpoint; a user-facing send-to-ORA service API is not connected yet. Frame forwarding still uses ordinary Ethernet.

Each group theoretically supports 1,048,575 nonzero values; current practical registry bounds are 256 networks, 4096 device records, 1024 topology nodes and 65,536 random-address history records per dimension. Exhaustion produces an error rather than overwriting another allocation.

### Enrollment and RPC

Initial network creation requires trusted local physical access to the tower's device bus. The blue cable carries Ethernet; the device bus is the current RPC management path. Ongoing frame forwarding does not require leaving that management bus attached. The creator receives the administrator secret once. Subsequent invitations and revocation require that secret. Invites are endpoint-specific, dimension-scoped, single-use and expire after `radioInviteLifetimeTicks`.

The `gps_tower` device exposes:

| Method | Result or purpose |
|---|---|
| `createRadioNetwork()` | Returns `network` and `adminSecret` |
| `getRadioEndpointId()` | Endpoint UUID for an invitation |
| `issueRadioInvite(adminSecret, endpointId)` | Returns `invite` and `secret` |
| `joinRadioNetwork(invite, secret)` | Enroll this invited endpoint |
| `getRadioAddress()` | Display the endpoint ORA |
| `getRadioStatus()` | Enrollment, endpoint UUID, security mode |
| `revokeRadioEndpoint(adminSecret, endpointId)` | Revoke one endpoint |
| `issueRadioInviteForApproval(adminSecret, endpointId, permissions, targetEndpointId, service)` | Invite that can only open a pending request (see below) |
| `requestRadioEnrollment(invite, secret)` | Present an approval-required invite; returns the request id, grants nothing |
| `completeRadioEnrollment()` | After approval, registers this tower in the topology; `false` while not approved |
| `listPendingEnrollments(adminSecret)` | Pending requests: `request`, `endpointId`, requested `scopes`, `expiresAtTick` |
| `approveEnrollment(adminSecret, requestId, scopes)` | Admit the requester with exactly the listed scopes |
| `denyEnrollment(adminSecret, requestId)` | Remove a pending request |

#### Approval-gated enrollment

Invites are either direct (`issueRadioInvite*`, `joinRadioNetwork` admits at once) or approval-gated, chosen per invite by calling `issueRadioInviteForApproval`. A gated invite cannot be used with `joinRadioNetwork`; the joining tower calls `requestRadioEnrollment`, which consumes the invite and creates a PENDING request holding no membership and no permission. An administrator PC then lists and decides:

```lua
local pending = tower:listPendingEnrollments(adminSecret)   -- request, endpointId, scopes, expiresAtTick
tower:approveEnrollment(adminSecret, pending[1].request, {"MESSAGE_SEND"})
-- or: tower:denyEnrollment(adminSecret, pending[1].request)
-- on the joining PC, after approval: tower:completeRadioEnrollment()
```

Rules: the three admin callbacks need the network admin secret and the calling tower must itself be an enrolled member holding `NETWORK_ADMIN`, both rechecked on every call. Approval scopes are bare permission names or full descriptions from the list and must have been requested; the device gets exactly those (an empty list admits it with no permissions). The device credential is a claim secret generated at request time (stored only as a hash) and becomes valid only on approval. Limits: at most 16 pending requests per network, one per device, a fixed lifetime of 6000 ticks (five minutes), and one request per invite (replay of a consumed invite fails). Deny, expiry, double approval and approval of an unknown or foreign request fail with `SecurityException`. Pending requests are saved with the radio registry in the world and survive restarts. The ORA address is assigned at approval, so it is not shown in the pending list.

Obtain the second tower's endpoint UUID, issue its invite on the first tower, then pass the invite ID and secret to the second tower. Enroll intermediate relays too. Example on the first PC, once. Retain `adminSecret` for future administration:

```lua
local tower = assert(require("devices"):find("gps_tower"))
local network = tower:createRadioNetwork()
local invitation = tower:issueRadioInvite(network.adminSecret, "SECOND-TOWER-ENDPOINT-UUID")
-- Transfer invitation.invite and invitation.secret to the second PC.
```

On the second PC:

```lua
local tower = assert(require("devices"):find("gps_tower"))
tower:joinRadioNetwork("INVITATION-UUID", "INVITATION-SECRET")
print(tower:getRadioAddress())
```

Select an explicit device with `devices:get(deviceId)` when multiple towers are attached; `find()` does not choose by proximity.

The registry stores credential hashes; the endpoint's own bearer secret remains in server-side tower data. Public status contains no secrets. Copied block data cannot overwrite an existing endpoint position binding.

### Delay, TTL and security

- Delivery uses the server tick queue: one tick per radio hop, but the propagation delay is capped at `radioMaxDeliveryTicks` (2 ticks, 100 ms at 20 TPS) however many towers are chained. Each sender is limited to `radioLinkBytesPerTick` (1024 B/tick, about 20 KB/s). Lower TPS increases real delay.
- Ping is the round-trip time measured with the monotonic clock (`System.nanoTime`) from the actual reply. On the tower RPC call `pingRadio(address, requestId)`; the result arrives in `pollRadioEvents()` as `roundTripNanos`. The hop count (`route.hops`) and the one-way estimate (`route.nominalTicks` and `route.nominalMillis`, 50 ms per tick at the nominal 20 TPS) are separate values: they are a schedule estimate, not a measurement, and the real delay is longer when TPS drops.
- Hop TTL limits traversed nodes and is reduced by radio and wired forwarding. It is the OC2R network-bus TTL, separate from the IP header TTL and the packet lifetime deadline.
- Pending packets expire and do not persist through restarts. Route loss, revocation or an offline recipient prevents queued delivery.
- Bounded queues, packet IDs and enrollment generations support admission control and duplicate/revocation checks. The first bridge has no bandwidth serialization or interference simulation.
- TLS is not implemented. Status reports `trusted-local-plaintext-no-tls`. Application-level permissions and encrypted streams remain future work.

The `[gameplay]` settings are shown in the TOML example above. Restart after changing scheduler limits. The IPv4 configuration inside the guest remains separate from radio range and GPS coordinate settings.

### Robot dock station as access point

The robot dock station is the existing charger block; there is no separate block. It is its own short-range access point with a circular 10-block X/Z radius (Y ignored), configured by `robot_dock_range` in `[gameplay]` (`Config.robotDockRange` in code; default 10, allowed 1-64). The distance is measured from the dock's block centre to the robot's centre and the boundary is inclusive: exactly 10 blocks still attaches.

- An enrolled robot attaches directly to a tower within tower range (`gps_tower_range`, 45 blocks by default). Otherwise it attaches to the nearest dock within 10 blocks that has a tower uplink, and the dock reaches the network through the nearest tower of the same network within tower range of the dock.
- A dock does not relay between towers and radii are never added: the robot must be inside the dock radius and the dock inside a tower radius. A dock with no tower in range is isolated. A dock belongs to no network and is never registered anywhere: the server looks for chargers only in the already loaded chunks around the robot and never loads a chunk, so a broken, replaced or unloaded dock stops counting at once. Nothing crosses dimensions.
- Through a dock a robot is controlled exactly like one on a tower: telemetry, commands, ping, device list and permission rechecks. The route gains one radio leg; the delivery-delay cap and link bandwidth are unchanged.
- Leaving the radius is signal loss and returning reconnects. A command sent during signal loss ends as `DISCONNECTED` or `TIMED_OUT` and is never replayed after the reconnect.

A robot command result is authoritative. The guest program calls `bindRadioCommand(requestId, actionId)` for a robot action (`move`/`turn`) queued after the command arrived; binding an earlier action, an action not queued yet, or one already bound to another command is refused. The server reports `COMPLETED` or `FAILED` only when that action really finishes; an action the robot drops without a result (for example when its computer stops) ends the command as `FAILED` with the result `CANCELLED`. Through `updateRadioCommand` the guest can only report `RUNNING`.

### Dock station Ethernet

The dock station takes the blue network cable through an ordinary network connector, like a tower. A cable alone does not join a network: the dock has its own client identity and the network administrator has to invite it like any device. The `charger` device gained `getRadioEndpointId()`, `joinRadioNetwork(invite, secret)`, `getRadioAddress()` and `getRadioStatus()`; the invitation is issued on a tower with `issueRadioInvite(adminSecret, endpointId)`.

Frames from a connector on the dock enter the radio through the nearest tower of the dock's own network within tower range, and frames of the radio network are written to every connector on the dock. The dock still does not relay between towers. An uninvited or revoked dock passes nothing, and an unloaded or broken dock stops bridging (no chunk is loaded for a dock).

### Credentials, revocation and rotation

A device secret is 256 random bits from `SecureRandom` (unpadded base64url); the registry stores only its SHA-256 hash. Revocation acts on one device and leaves the others untouched; re-inviting issues a new generation, a new random address and a new secret, and a secret of an earlier generation never authenticates again (`authenticate(device, secret, generation)`). Rotation (`rotateCredential`) replaces one device's secret and advances its generation; address and permissions stay and no other client is reset. The string forms of records that carry secrets (`Enrollment`, `Invitation`, `CreatedNetwork`, `PendingTicket`, `Member`) contain neither secrets nor hashes, so they cannot reach a log.

For a client whose identity travels inside an item (the future tablet) the registry defines the rules (`RadioClientLeases`): an identity has at most one active holder; every activation rotates the secret and the holder must store the new one in the item, so a copy of the item holding the old secret never becomes a second authenticated identity; dropping, death, logout, dimension change and transfer to another player only suspend the identity (address, permissions and secret are kept); activation works only in the dimension of enrollment. Activity is not saved, so a server restart suspends every client.

### Duplicates and retries

An Ethernet frame that comes back into the network through a loop is recognised by its content and a lower TTL within a 5 tick window and dropped (`RadioFrameFilter`, at most 4096 entries); a deliberate resend with the same TTL passes. A repeated request ID from the same device returns the earlier command and never runs it a second time, also after a reconnect: the new session is told the known state, and a command cut off by closing its session stays `DISCONNECTED` and is not started again. The receiving side remembers received request IDs for 1200 ticks and ignores a repeated delivery; a repeated `MESSAGE` with the same request ID is delivered once. All duplicate memory is bounded in size and lifetime and forgets its oldest entry first when full.

### Telemetry age

`getRadioTelemetryStatus(subscription)` returns for every sampled field `ageTicks`, `ageMillis` (nominal, 50 ms per tick), `sampleTick`, `sequence` and a state of `ONLINE`, `STALE` or `OFFLINE`. `STALE` starts when the age reaches the subscription's limit, `OFFLINE` when the publisher or the link is gone. A field without a sample is not shown as live.

### Next steps

Tablets and their roaming, service-scoped application permissions, TLS and live dashboards remain pending. The Onyx widget prototype currently uses an explicitly separate demonstration backend.

## References

- [Implementation and acceptance plan](roadmap/tablet-radio.md)
- [GPS coordinate behavior](GPS.md)
- [Ethernet and Internet internals](NETWORKING.md)
- [TLS 1.3 specification](https://www.rfc-editor.org/rfc/rfc8446.html)
