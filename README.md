# Inventario Almacén (Android, Kotlin + Jetpack Compose + Room)

App de inventario que funciona sin internet; los datos se guardan en el teléfono.

## Funciones
- Alta, edición y baja de productos (nombre, código, unidad, ubicación, stock mínimo).
- Entradas y salidas de material con nota; no permite dejar el stock en negativo.
- Historial de movimientos (últimos 300).
- Alertas de stock mínimo: tarjeta en rojo, contador y filtro "Stock bajo".
- Búsqueda por nombre, código o ubicación.

## Cómo abrirla y correrla
1. Instala Android Studio (versión Koala o más reciente).
2. File > Open y elige esta carpeta `InventarioAlmacen`.
3. Espera a que termine "Gradle Sync" (la primera vez descarga dependencias y necesita internet).
   Si Android Studio pide crear/actualizar el Gradle wrapper, acepta.
4. Conecta un teléfono con depuración USB o crea un emulador y presiona Run (▶).

## Generar el APK para instalarlo
Build > Build Bundle(s) / APK(s) > Build APK(s). El archivo queda en
`app/build/outputs/apk/debug/app-debug.apk`.

## Estructura
- `data/Modelos.kt`: tablas Producto y Movimiento.
- `data/AppDatabase.kt`: base de datos Room y operaciones (transacciones de stock).
- `ui/InventarioViewModel.kt`: lógica y estado.
- `MainActivity.kt`: pantallas (productos, historial, diálogos).

## Ideas para la versión 2
Escáner de código de barras (ML Kit), exportar a Excel/CSV, fotos de producto,
varios almacenes, respaldo en la nube.
