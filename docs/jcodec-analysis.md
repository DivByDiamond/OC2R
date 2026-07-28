# Анализ встроенной библиотеки jcodec в OC2R

## 1. Общая статистика

| Метрика | Значение |
|---------|----------|
| Файлов | 86 |
| Строк кода | ~16 141 |
| Пакетов | 7 |
| Внешних зависимостей | **Ноль** (только Java stdlib) |
| Модифицирована ли? | Нет (vanilla jcodec) |

---

## 2. Структура пакетов

```
li.cil.oc2.jcodec
├── codecs
│   ├── common
│   │   └── biari/               # Бинарное арифметическое кодирование (CABAC)
│   │       ├── MConst.java       # Константы для CABAC
│   │       └── MDecoder.java     # Декодер CABAC
│   └── h264
│       ├── encode/               # Энкодер H.264
│       │   ├── CQPRateControl.java      # CQP rate control
│       │   ├── EncodedMB.java           # Закодированный макроблок
│       │   ├── EncodingContext.java     # Контекст кодирования
│       │   ├── H264EncoderUtils.java    # Утилиты энкодера
│       │   ├── IntraPredEstimator.java  # Intra-предсказание
│       │   ├── MBDeblocker.java         # Деблокинг при кодировании
│       │   ├── MBEncoderHelper.java     # Helper для макроблоков
│       │   ├── MBWriterI16x16.java      # Intra 16x16 writer
│       │   ├── MBWriterINxN.java        # Intra NxN writer
│       │   ├── MBWriterP16x16.java      # Inter P-кадра writer
│       │   ├── MotionEstimator.java     # Оценка движения
│       │   └── RateControl.java         # Интерфейс rate control
│       ├── decode/               # Декодер H.264
│       │   ├── aso/                     # Arbitrary Slice Order
│       │   │   ├── FlatMBlockMapper.java
│       │   │   ├── MapManager.java
│       │   │   ├── Mapper.java
│       │   │   ├── MBToSliceGroupMap.java
│       │   │   ├── PrebuiltMBlockMapper.java
│       │   │   └── SliceGroupMapBuilder.java
│       │   ├── deblock/                 # Деблокинг-фильтр
│       │   │   └── DeblockingFilter.java
│       │   ├── BlockInterpolator.java
│       │   ├── CABACContst.java
│       │   ├── CAVLCReader.java
│       │   ├── ChromaPredictionBuilder.java
│       │   ├── CoeffTransformer.java
│       │   ├── DebockerInput.java
│       │   ├── DecoderState.java
│       │   ├── FrameReader.java
│       │   ├── Intra4x4PredictionBuilder.java
│       │   ├── Intra8x8PredictionBuilder.java
│       │   ├── Intra16x16PredictionBuilder.java
│       │   ├── MBlock.java
│       │   ├── MBlockDecoderBase.java
│       │   ├── MBlockDecoderBDirect.java
│       │   ├── MBlockDecoderInter.java
│       │   ├── MBlockDecoderInter8x8.java
│       │   ├── MBlockDecoderIntra16x16.java
│       │   ├── MBlockDecoderIntraNxN.java
│       │   ├── MBlockDecoderIPCM.java
│       │   ├── MBlockDecoderUtils.java
│       │   ├── MBlockSkipDecoder.java
│       │   ├── PredictionMerger.java
│       │   ├── RefListManager.java
│       │   ├── SliceDecoder.java
│       │   ├── SliceHeaderReader.java
│       │   └── SliceReader.java
│       ├── io/                   # Битстрим I/O
│       │   ├── model/                   # H.264 модели данных
│       │   │   ├── AspectRatio.java
│       │   │   ├── Frame.java
│       │   │   ├── HRDParameters.java
│       │   │   ├── MBType.java
│       │   │   ├── NALUnit.java
│       │   │   ├── NALUnitType.java
│       │   │   ├── PictureParameterSet.java
│       │   │   ├── PredictionWeightTable.java
│       │   │   ├── RefPicMarking.java
│       │   │   ├── RefPicMarkingIDR.java
│       │   │   ├── SeqParameterSet.java
│       │   │   ├── SliceHeader.java
│       │   │   ├── SliceType.java
│       │   │   └── VUIParameters.java
│       │   ├── write/                   # Запись битстрима
│       │   │   ├── CAVLCWriter.java
│       │   │   └── SliceHeaderWriter.java
│       │   ├── CABAC.java
│       │   └── CAVLC.java
│       ├── H264Const.java
│       ├── H264Decoder.java
│       ├── H264Encoder.java
│       ├── H264Utils.java
│       └── POCManager.java
├── common
│   ├── io/                       # Базовый I/O
│   │   ├── BitReader.java
│   │   ├── BitWriter.java
│   │   ├── VLC.java
│   │   └── VLCBuilder.java
│   ├── model/                    # Общие модели
│   │   ├── ColorSpace.java       # Цветовые пространства
│   │   ├── Picture.java          # Растровое изображение (YUV/RGB)
│   │   └── Size.java             # Размер
│   ├── tools/
│   │   └── MathUtil.java
│   ├── ArrayUtil.java
│   ├── IntArrayList.java
│   ├── IntObjectMap.java
│   ├── VideoDecoder.java         # Интерфейс видеодекодера
│   └── VideoEncoder.java         # Интерфейс видеоэнкодера
└── scale/                        # Преобразования цветовых пространств
    ├── RgbToYuv420j.java         # RGB → YUV420J
    ├── Transform.java            # Интерфейс трансформации
    └── Yuv420jToRgb.java         # YUV420J → RGB
```

---

## 3. Потребители jcodec (вне пакета jcodec)

### 3.1. Энкодеры

| Файл | Использование |
|------|---------------|
| `common/vm/device/SimpleFramebufferDevice.java` | Конвертирует R5G6B5 → YUV420J ч/з `RgbToYuv420j`, вызывает `H264Encoder` |
| `common/blockentity/monitor/MonitorVideoEncoder.java` | Сериализует `Picture` → H.264 NAL, отправляет в Shared Memory |
| `common/blockentity/projector/ProjectorVideoEncoder.java` | То же для проектора |

### 3.2. Декодеры

| Файл | Использование |
|------|---------------|
| `common/blockentity/monitor/MonitorVideoDecoder.java` | H.264 NAL → `Picture` (YUV420J) |
| `common/blockentity/projector/ProjectorVideoDecoder.java` | То же для проектора |

### 3.3. Рендеринг (отображение)

| Файл | Использование |
|------|---------------|
| `client/renderer/RenderInfo.java` | `Yuv420jToRgb.YUVJtoRGB()` → `NativeImage` для монитора |
| `client/renderer/ProjectorDepthRenderInfo.java` | `Yuv420jToRgb.YUVJtoRGB()` → `NativeImage` для проектора |

### 3.4. Владельцы данных

| Файл | Использование |
|------|---------------|
| `blockentity/monitor/MonitorVideoController.java` | Создаёт `Picture.create(W, H, YUV420J)` |
| `blockentity/projector/ProjectorBlockEntity.java` | Создаёт `Picture.create(W, H, YUV420J)` и `Picture.create(DEPTH_W, DEPTH_H, GRAY)` |
| `bus/device/vm/block/MonitorDevice.java` | `SimpleFramebufferDevice(WIDTH, HEIGHT)`, вызывает `applyChanges(picture)` |
| `bus/device/vm/block/ProjectorDevice.java` | `SimpleFramebufferDevice(WIDTH, HEIGHT)`, вызывает `applyChanges(picture)` |
| `blockentity/projector/FrameConsumer.java` | Интерфейс `processFrame(Picture)` |
| `blockentity/projector/ProjectorState.java` | Хранит `Picture` для состояния проектора |

---

## 4. Архитектура энкодера

### 4.1. Входные данные

```
VM Framebuffer (R5G6B5)
    ↓ SimpleFramebufferDevice.convertR5G6B5ToYUV420J()
    ↓
25fps screen capture (в `MonitorVideoEncoder` / `ProjectorVideoEncoder`)
    ↓
RgbToYuv420j.rgb2yuv(Picture RGB → Picture YUV420J)
    ↓
H264Encoder.encodeFrame(Picture YUV420J)
```

### 4.2. Процесс кодирования

1. **initSPS()** — инициализация Sequence Parameter Set (profile=66 baseline, chromaFormatIdc=1/YUV420)
2. **Нарезка на макроблоки** — каждый кадр делится на макроблоки 16×16
3. **Выбор режима** — Intra (I-кадр) или Inter (P-кадр) через `MotionEstimator`
4. **Внутрикадровое предсказание** — `IntraPredEstimator`, `MBWriterI16x16`, `MBWriterINxN`
5. **Межкадровое предсказание** — `MotionEstimator`, `MBWriterP16x16`
6. **DCT + квантование** — `CoeffTransformer` (4×4 integer DCT)
7. **Энтропийное кодирование** — CAVLC (Context-Adaptive Variable Length Coding)
8. **Формирование NAL-юнитов** — слайсы упаковываются в NAL, записываются через `BitWriter`

### 4.3. Rate Control

`CQPRateControl` — Constant Quantization Parameter. Принимает QP в конструкторе (для OC2R: 12). Высокое качество при низкой сложности.

### 4.4. Ключевые кадры

Настраиваемый `keyInterval` (по умолчанию 100 кадров). При достижении кодируется полный I-кадр (intra).

---

## 5. Архитектура декодера

```
Bitstream (NAL units)
    ↓
H264Decoder.decodeFrame(ByteBuffer)
    ↓
FrameReader → SliceReader → SliceDecoder
    ↓
Picture (YUV420J)
```

1. **NAL Unit Parsing** — разбиение потока на NAL-юниты (SPS, PPS, IDR, Non-IDR)
2. **Slice Header Parsing** — извлечение параметров слайса
3. **Макроблок-байпасс** — ASO (Arbitrary Slice Order)
4. **Для каждого макроблока:**
   - Декодирование заголовка MB (тип, MV, ref idx)
   - Intra/Inter предсказание
   - Обратный DCT
   - Деквантование
   - CAVLC/CABAC декодирование остаточных коэффициентов
5. **Деблокинг-фильтр** — пост-процессинг для уменьшения артефактов
6. **Выход** — декодированный `Picture` с 3 плоскостями (Y, U, V)

---

## 6. Цветовые пространства

### 6.1. Определения (`ColorSpace.java`)

```java
YUV420  = new ColorSpace("YUV420", 3, {0,1,1}, {0,1,1}, true);  // chroma subsampling 4:2:0
YUV420J = new ColorSpace("YUV420J", 3, {0,1,1}, {0,1,1}, true); // то же, но full range
YUV422  = new ColorSpace("YUV422", 3, {0,1,1}, {0,0,0}, true);  // 4:2:2
YUV444  = new ColorSpace("YUV444", 3, {0,0,0}, {0,0,0}, true);  // 4:4:4 (без субдискретизации)
```

Массивы `{subX, subY}` для каждой компоненты. Индекс: 0=Y, 1=U/Cb, 2=V/Cr. Значение: `>> sub` — сдвиг для получения размера плоскости.

### 6.2. Конвертация

| Направление | Класс |
|-------------|-------|
| RGB → YUV420J | `scale/RgbToYuv420j.java` — `rgb2yuv(Picture rgb, Picture yuv)` |
| YUV420J → RGB | `scale/Yuv420jToRgb.java` — `YUVJtoRGB(y, cb, cr, byte[3], offset)` |

Поток: **16 бит на пиксель** → кратность цветовых компонент внутри конвертации.

---

## 7. Жёсткая привязка к YUV420

### 7.1. Энкодер

| Файл:Строка | Проблема |
|-------------|----------|
| `H264Encoder.java:95` | `if (pic.getColor() != ColorSpace.YUV420J) throw` |
| `H264Encoder.java:158` | `picOut = Picture.create(mbWidth<<4, mbHeight<<4, ColorSpace.YUV420J)` |
| `H264Encoder.java:196` | `sps.chromaFormatIdc = ColorSpace.YUV420J` |
| `H264Encoder.java:414` | `getSupportedColorSpaces() → {YUV420J}` |

### 7.2. Декодер

| Файл:Строка | Проблема |
|-------------|----------|
| `H264Decoder.java:158` | `if (sps.chromaFormatIdc != ColorSpace.YUV420J) throw` |
| `H264Decoder.java:306` | `new Frame(width, height, buffer, ColorSpace.YUV420, ...)` |

### 7.3. Структуры данных

| Файл:Строка | Проблема |
|-------------|----------|
| `EncodingContext.java:36-37` | chroma row sizes: `new byte[8]` (надо 16 для 444) |
| `SliceDecoder.java:192-197` | `for (int i=0; i<8; i++)` для копии chroma (надо 16) |
| `MBWriterP16x16.java:79-84` | chroma block size = 8×8 hardcoded |
| `MBEncoderHelper.java` | `mbX << 3` для chroma offset (надо `<< 4` для 444) |
| `DeblockingFilter.java` | chroma strides = width/2, loop до height/2 |

### 7.4. CABAC

| Файл:Строка | Проблема |
|-------------|----------|
| `CAVLC.java:198-207` | Разные VLC таблицы для 420/422/444, но 444 никогда не используется |

---

## 8. Почему jcodec — это проблема

### 8.1. Сложность замены

- **86 файлов, ~16K строк** — полноценный H.264 baseline энкодер + декодер
- **Нет внешних зависимостей** — весь код самописный, можно модифицировать
- **Напрямую встроен в проект** — не Maven-зависимость, лежит в `li.cil.oc2.jcodec.*`

### 8.2. Варианты замены

| Вариант | Плюсы | Минусы |
|---------|-------|--------|
| **Maven: org.jcodec:jcodec** | Официальная библиотека, обновления, багфиксы | API может не совпадать, версия может быть древняя |
| **Встроить FFmpeg (JNI/JNR)** | Полноценный H.264, YUV444, GPU | Кросс-платформенность, размер, лицензия (LGPL) |
| **Продолжить использовать встроенный** | Работает, размер мал, нет лицензионных проблем | Нет YUV444, нет оптимизаций |
| **Переписать на Java NIO + VAAPI** | Быстро, современно | Огромная работа |

### 8.3. Сколько займёт YUV444

1. Убрать проверки типа `ColorSpace` — 5 мин
2. Сделать `EncodingContext` адаптивным к `ColorSpace` — 1 час
3. Переписать `SliceDecoder.putMacroblock()` — 2 часа
4. Переписать `MBWriter*` — 4 часа
5. Обновить `DeblockingFilter` — 2 часа
6. Тестирование — 4+ часа

**Итого: минимум 2-3 дня плотной работы.**

---

## 9. Вывод

Встроенный jcodec — это рабочий H.264 baseline codec чисто на Java, без внешних зависимостей. Для YUV444 потребуется модификация ~14 мест в энкодере/декодере с изменением логики работы с хромой (с 8×8 на 16×16 макроблоки). Замена на Maven `org.jcodec:jcodec` — самый реалистичный путь, но требует проверки API-совместимости. FFmpeg — перебор для этой задачи.
