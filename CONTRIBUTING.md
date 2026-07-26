# Contributing to OC2R

## Code Style

- Java 21, NeoForge, Gradle
- ≤200 строк на файл, ≤4 файлов на папку (SRP)
- Никаких `// comments` в коде — только JavaDoc на public API
- Никаких SPDX заголовков
- Build before commit: `./gradlew build`

## PR Process

1. Форкните репозиторий
2. Создайте ветку от `1.21.1`
3. Вносите изменения с коммитами по смыслу
4. Убедитесь что `./gradlew build` проходит
5. Откройте Pull Request на `1.21.1`

## Структура

```
src/main/java/li/cil/oc2/
├── api/          # Публичное API (не менять без обсуждения!)
├── client/       # Только клиентский код (рендер, GUI)
├── common/       # Серверный код (BE, VM, network, bus)
├── data/         # Data generators
└── jcodec/       # Внешняя H.264 библиотека (не трогать)
```

## Issues

Используйте шаблоны в `.github/ISSUE_TEMPLATE/`.
