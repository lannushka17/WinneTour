# Пакування (kostyl.dev — Частина III)

## Структура

```
packaging/
├── app-icon.png          ← javapackager (у pom: packaging/app-icon без розширення)
├── windows/winerytours.ico
├── macos/winerytours.icns
├── linux/winerytours.png
└── create-icons.py       ← генерація placeholder-іконок
```

## Локальна збірка (javapackager)

```bash
python3 packaging/create-icons.py
# Windows: ImageMagick → .ico; macOS: iconutil → .icns

mvn clean package -DskipTests
# → target/WineryTours-fat.jar
# → dist/WineryTours-1.0.0.msi | .dmg | winerytours_1.0.0.deb
```

## GitHub Actions

Після `git push origin v1.0.0` workflow **Release — Build Installers** збирає MSI, DMG, DEB, RPM і публікує GitHub Release.

Ручний запуск: **Actions** → **Release — Build Installers** → **Run workflow**.

Замініть placeholder-іконки власними файлами для фінального релізу.
