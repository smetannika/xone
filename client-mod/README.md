# Client (Fabric 1.16.5)

Клиентский Fabric-мод для Minecraft 1.16.5 (Java 8+). Меню — **Right Shift**.

## Модули

| Вкладка | Модуль | Что делает |
|---|---|---|
| Combat | TriggerBot (есть `Only crits` и ползунок `Crit chance %`), AimAssist, AutoSwap | настройки в ClickGUI |
| Render | ESP | боксы и нейм-теги (имя, здоровье, дистанция), настройки в ClickGUI |
| Render | **Freecam** | свободная камера, настройка `Speed`; управление WASD / Space / Shift |
| Server | **Server Helper** | бинды: Трапка (FT), **Дезориентация**, **Снежок**, **Зелье киллера** |
| HUD | FPS, Target HUD, Keybinds, **Potions**, **Use Tracker**, **Watermark** | перетаскиваются мышью, пока открыт ClickGUI |

- Бинд модуля: средняя кнопка мыши по модулю в ClickGUI.
- Server Helper ищет предмет по названию (хотбар, затем инвентарь), берёт его, использует и возвращает слот.
- Конфиг: `config/clientmod.properties`.
- Рендер интерфейса идёт через OpenGL (`gui/Render2D.java`).

## Сборка

Нужен JDK 17 (байткод собирается под Java 8).

```
./gradlew build
```

Готовый jar: `build/libs/client-1_16_5-1.1.0.jar`. Для запуска нужен Fabric Loader и Fabric API для 1.16.5.

**Use Tracker** — показывает, что используют игроки, с которыми ты в пвп (еда, зелья, перлы, снежки), а у брошенных сплэш-зелий — процент попадания по тебе и по нему. Пвп определяется по ударам рядом (до 7 блоков); есть опция `Track all nearby`.
