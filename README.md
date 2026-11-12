# Filtro de llamadas

App Android (Kotlin + Jetpack Compose, Material 3) que filtra las llamadas entrantes con reglas
definidas por el usuario. Usa la API del sistema `CallScreeningService`: la app recibe el rol
de «identificación de llamadas y spam» y Android le consulta cada llamada entrante que no sea
de un contacto guardado.

## Reglas

Las reglas se evalúan de arriba abajo y gana la primera activa que coincide. Si ninguna
coincide, la llamada pasa.

| Condición | Coincide con |
|---|---|
| Países concretos | números de los países elegidos (país detectado con libphonenumber) |
| Internacionales | cualquier país distinto del país de referencia («solo nacionales») |
| Prefijo | números que empiezan por unos dígitos; con `+` se compara con el formato internacional, sin `+` con el número nacional |
| Números ocultos | llamadas sin número o con el número restringido |
| Cualquier número | todas las llamadas que llegan al filtro |

Acciones: **bloquear** (se rechaza sin sonar), **silenciar** (entra sin sonar ni vibrar) y
**permitir** (para hacer excepciones por encima de otras reglas).

El país de referencia sale de la SIM; se puede fijar a mano en Ajustes. Hay un mosaico de
ajustes rápidos para pausar el filtrado.

## Idiomas

La interfaz está en inglés (`values/`, idioma de respaldo), español, francés, italiano,
alemán y portugués. Por defecto sigue el idioma del sistema; en Android 13+ se puede elegir
otro solo para esta app desde Ajustes → Idioma (o desde los ajustes de idioma del sistema).
AGP genera `locales_config.xml` a partir de las carpetas `values-xx`
(`generateLocaleConfig` + `res/resources.properties`), así que basta con añadir una carpeta
nueva para sumar un idioma. Los nombres de países salen del propio sistema en el idioma activo.

Reglas, ajustes e historial se guardan como JSON en el almacenamiento interno de la app
(`rules.json` y `history.json`).

## Estructura

```
app/src/main/java/com/icaria/callrules/
  data/      modelo (reglas, ajustes, historial) y repositorio JSON
  engine/    análisis de números (libphonenumber), motor de reglas, país de referencia
  service/   RuleScreeningService (CallScreeningService)
  tile/      mosaico de ajustes rápidos
  ui/        pantallas Compose: reglas, editor, registro, ajustes, selector de países
app/src/test/  tests JVM del motor de reglas y del formato JSON
```

## Compilar

Necesita JDK 17 y el SDK de Android con la plataforma 36.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para que filtre, la app debe tener el rol de filtrado de llamadas. Se pide desde la propia
app (botón «Activar») o por adb:

```bash
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.icaria.callrules
```

El log de cada llamada filtrada sale en logcat con el tag `CallRules`.
