# Rocola

Reproductor de música para Android hecho con Kotlin, Jetpack Compose y Media3. Reproduce la música guardada en el teléfono y sigue sonando con la pantalla apagada, con la notificación multimedia de siempre para pausar o saltar de tema.

## Qué hace

- **Biblioteca**: canciones, álbumes, artistas, carpetas y listas, con búsqueda que ignora tildes.
- **Segundo plano**: servicio en primer plano con wake lock, así la música sigue con la pantalla apagada y en modo Doze.
- **Notificación y pantalla de bloqueo**: anterior, pausa, siguiente y un corazón para marcar favoritas.
- **Auriculares**: los botones de los auriculares (también Bluetooth) controlan la música, la pausa al desconectarlos, y retoma la última cola aunque la app esté cerrada.
- **Reproductor**: tapa con colores que salen del disco, dial para adelantar o atrasar, deslizar la tapa para cambiar de tema, aleatorio, repetir, velocidad.
- **Cola**: ver lo que sigue, reordenar arrastrando y quitar deslizando. "Reproducir a continuación" y "Agregar a la cola" desde cualquier canción.
- **Temporizador para dormir**, con el volumen bajando de a poco, o al terminar la canción actual.
- **Ecualizador** con estilos del sistema y refuerzo de graves.
- **Favoritas y listas** propias, guardadas en el teléfono.
- **Selección múltiple**: mantené apretada una canción y tocá otras para reproducirlas, mandarlas a la cola o a una lista, o borrarlas del teléfono (Android pide confirmación).
- **Atajo "Mezclar todo"**: mantené apretado el ícono de la app.

## Compilar

Con Android Studio, abrí la carpeta y tocá Build > Build APK(s). Por línea de comandos:

```bash
./gradlew assembleRelease
# APK en app/build/outputs/apk/release/app-release.apk
```

Cada push a `main` compila el APK en GitHub Actions y lo prueba en un emulador con Android 14. Ahí reproduce, apaga la pantalla, fuerza el modo Doze, espera 100 segundos y verifica que siga sonando, que avance de tema, que la notificación y la pantalla de bloqueo muestren los controles, y que funcionen los botones de la notificación (pausa, siguiente, favorita) y los de auriculares. El APK, las capturas y el resultado de la prueba quedan en la versión **Compilación de prueba** del repo (Releases), que se actualiza con cada cambio.

## Firma

El repo incluye una clave de desarrollo (`keystore/rocola.jks`) para que cada APK se pueda instalar encima del anterior. Si algún día lo publicás en Play Store, generá una clave propia y no la subas al repo.

## Si la música se corta en tu teléfono

Algunas marcas (Xiaomi, Samsung, Huawei) cierran apps en segundo plano aunque estén reproduciendo. En Ajustes de Rocola tocá **Quitar restricción de batería** y elegí Permitir.
