# FicticionalTV · app Android (teléfono + Android TV)

**Una sola app** para las dos cosas. Al abrirse detecta el dispositivo:

| Dispositivo | Carga | Control |
|---|---|---|
| Android TV / Google TV / Fire TV | `https://ficticionaltv.vercel.app/tv/` (tu interfaz de TV) | Control remoto (flechas, OK, Atrás) |
| Teléfono / tablet | `https://ficticionaltv.vercel.app/` (tu web con su interfaz móvil) | Táctil; Atrás navega hacia atrás |

Como es un contenedor que carga tu sitio, **todo lo que subas a tu web se ve en la app sin
reinstalar**. El video de YouTube funciona porque la app carga desde tu dominio https (no `file://`).

Qué incluye: pantalla completa de video, cookies de terceros para los reproductores, "Continuar viendo"
guardado, pantalla "Sin conexión" con Reintentar, icono + banner de Android TV, y el puente
`AndroidTV.exit()` que tu `tv.js` ya esperaba.

## Obtener el APK (sin instalar Android Studio)
1. Crea un repositorio nuevo en GitHub (ej. `ftv-android`) y sube **el contenido de esta carpeta**.
2. Pestaña **Actions** → "Build APK" → espera ~3 min (también se ejecuta solo en cada push).
3. Entra a la ejecución terminada → **Artifacts** → descarga `FicticionalTV-apk` → dentro está `app-debug.apk`.

## Obtener el APK con Android Studio
Abre esta carpeta → espera el Gradle Sync → **Build > Build APK(s)**.

## Instalar
**Teléfono:** pasa el APK al celular, ábrelo y permite "instalar apps desconocidas".

**Android TV:** (la TV y el PC en la misma red)
1. En la TV: Ajustes > Acerca de > pulsa 7 veces "Compilación" (activa opciones de desarrollador) →
   Depuración por red/ADB activada. Anota la IP.
2. En el PC: `adb connect IP_DE_LA_TV:5555` y luego `adb install app-debug.apk`.
   (Alternativa sin PC: instala la app "Downloader" en la TV y descarga el APK desde un enlace, p. ej.
   un Release de GitHub.)

La app aparece en el launcher de la TV con su banner.

## Cambiar el dominio
Edita las constantes al inicio de `app/src/main/java/com/ficticionaltv/app/MainActivity.java`.

## Notas
- El APK `debug` sirve para uso personal. Para Google Play necesitas un APK/AAB firmado con tu keystore
  y cumplir las políticas de contenido de Play (revisan el contenido que muestra la app).
- Android TV exige Android 7+ (minSdk 24) y un WebView actualizado (se actualiza por Play Store).
- Tu web necesita que `/tv/` cargue bien en Vercel (ver nota de la barra final `/tv` vs `/tv/`).
