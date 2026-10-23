IkkoOptimization - Fabric 1.21.11 (Yarn mappings, Java 21)

Збірка:
1) Зайди на https://fabricmc.net/develop/template , вибери Minecraft 1.21.11, Yarn, mod id = ikkooptimization -> скачай шаблон
   (там є gradle wrapper, якого немає в цьому архіві).
2) Скопіюй у шаблон папку src/ (заміни), а також build.gradle, gradle.properties, settings.gradle.
   Якщо Gradle скаже, що версії застарілі - онови їх з сайту fabricmc.net/develop.
3) ./gradlew build  ->  build/libs/IkkoOptimization-1.0.0.jar  кидай у mods.

Використання:
- Right Shift = відкрити/закрити HUD (можна перебіндити там же, кнопка "Бінд HUD", або в Керуванні).
- Подвійний швидкий пробіл у повітрі без елітр -> одягає елітри, злітає, далі за режимом:
  Феєрверк   - бере феєрверк з хотбару і юзає ПКМ.
  Заряд вітру - дивиться вниз, бере заряд вітру з хотбару, кидає + підстрибує, злітає.
- Конфіг: config/ikkooptimization.json

Збірка через GitHub (без локальних інструментів):
1) Створи репозиторій на github.com і завантаж у нього ВМІСТ цієї папки (разом з прихованою папкою .github).
2) Відкрий вкладку Actions -> build -> останній запуск (або Run workflow).
3) Внизу сторінки запуску в розділі Artifacts скачай IkkoOptimization -> всередині .jar для папки mods
   (бери файл без "-sources" в назві).
