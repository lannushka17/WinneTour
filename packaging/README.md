# Пакування (kostyl.dev — Частина III)

## Структура

```
packaging/
├── app-icon.png          ← для javapackager (без розширення в pom: packaging/app-icon)
├── windows/winerytours.ico
├── macos/winerytours.icns
├── linux/winerytours.png
└── create-icons.py       ← генерація placeholder-іконок
```

## Локальна збірка (javapackager)

```bash
python3 packaging/create-icons.py
# Windows: додатково .ico через ImageMagick
# macOS: iconutil для .icns

mvn clean package -DskipTests
# → target/WineryTours-fat.jar
# → dist/WineryTours-1.0.0.msi | .dmg | _1.0.0.deb
```

## GitHub Actions

Після `git push origin v1.0.0` workflow `Release — Build Installers` збирає MSI, DMG, DEB, RPM.

Замініть placeholder-іконки власними файлами для фінального релізу.
