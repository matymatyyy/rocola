#!/usr/bin/env bash
# Prueba de Rocola en el emulador: biblioteca, reproductor, segundo plano con la pantalla
# apagada y Doze, notificación multimedia y botones de auriculares. Deja capturas en $OUT.
# Uso: ci/emulator-test.sh <apk> <carpeta_con_mp3> <carpeta_salida>
set -uo pipefail
APK="$1"
FIXTURES="$2"
OUT="$3"
PKG=com.maty.rocola
mkdir -p "$OUT"
: > "$OUT/resultado.txt"
FAILURES=0

pass() { echo "✅ $*"; echo "OK    $*" >> "$OUT/resultado.txt"; }
fail() { echo "❌ $*"; echo "FALLA $*" >> "$OUT/resultado.txt"; FAILURES=$((FAILURES + 1)); }
note() { echo "ℹ️  $*"; echo "INFO  $*" >> "$OUT/resultado.txt"; }
shot() {
  sleep "${2:-1}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "📸 $1"
}

session() { adb shell dumpsys media_session | grep -A 30 "package=$PKG"; }
pb_state() { session | grep -m1 "state=PlaybackState" | sed 's/^ *//'; }
is_playing() { pb_state | grep -qE "state=PlaybackState \{state=(PLAYING\(3\)|3),"; }
is_paused() { pb_state | grep -qE "state=PlaybackState \{state=(PAUSED\(2\)|2),"; }
current_title() { session | grep -m1 "metadata:" | sed 's/.*description=//'; }

ui_find() {
  # Imprime "x y" del primer nodo cuyo texto o descripción coincide con alguno de los argumentos.
  # Se borra el volcado anterior para no usar coordenadas viejas si el nuevo falla.
  rm -f "$OUT/ui.xml"
  adb shell rm -f /sdcard/rocola-ui.xml
  adb shell uiautomator dump /sdcard/rocola-ui.xml > /dev/null 2>&1
  adb pull /sdcard/rocola-ui.xml "$OUT/ui.xml" > /dev/null 2>&1 || return 0
  python3 - "$OUT/ui.xml" "$@" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, targets = sys.argv[1], sys.argv[2:]
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(0)
for target in targets:
    for node in root.iter('node'):
        if node.get('text') == target or node.get('content-desc') == target:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
PY
}

dismiss_anr() {
  # El emulador de la CI a veces muestra "X isn't responding" de apps del sistema; lo cerramos con "Wait".
  local wait_btn
  wait_btn=$(ui_find "Wait" "Esperar")
  if [ -n "$wait_btn" ]; then
    # shellcheck disable=SC2086
    adb shell input tap $wait_btn
    note "Cerré un aviso de app del sistema que no respondía"
    sleep 1
  fi
}

tap() {
  local coords=""
  for _ in 1 2 3 4; do
    coords=$(ui_find "$@")
    if [ -n "$coords" ]; then
      # shellcheck disable=SC2086
      adb shell input tap $coords
      sleep 1.5
      return 0
    fi
    dismiss_anr
    sleep 1
  done
  note "No encontré en pantalla: $*"
  return 1
}

echo "== Preparando el emulador"
adb wait-for-device
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell getprop ro.build.version.release | xargs -I{} echo "Android {}"

if adb install -r -g "$APK"; then pass "APK instalado"; else fail "No se pudo instalar el APK"; exit 1; fi

echo "== Copiando música de prueba"
adb shell mkdir -p /sdcard/Music/Rocola
for f in "$FIXTURES"/*.mp3; do adb push "$f" /sdcard/Music/Rocola/ > /dev/null; done
for f in "$FIXTURES"/*.mp3; do
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Music/Rocola/$(basename "$f")" > /dev/null
done
EXPECTED=$(ls "$FIXTURES"/*.mp3 | wc -l)
COUNT=0
# Esperamos a que el escáner termine de leer las etiquetas (duración > 0), no solo a que existan las filas.
for _ in $(seq 1 90); do
  COUNT=$(adb shell "content query --uri content://media/external/audio/media --projection title:duration --where 'duration>0 AND is_music=1'" 2>/dev/null | grep -c "Row:")
  [ "$COUNT" -ge "$EXPECTED" ] && break
  sleep 2
done
adb shell "content query --uri content://media/external/audio/media --projection title:artist:album:duration:is_music" > "$OUT/mediastore.txt" 2>&1
if [ "$COUNT" -ge "$EXPECTED" ]; then pass "MediaStore indexó $COUNT archivos con sus etiquetas"; else fail "MediaStore indexó $COUNT de $EXPECTED archivos"; fi
sleep 3

echo "== Interfaz"
adb shell am start -W -n "$PKG/.MainActivity" > /dev/null
sleep 3
dismiss_anr
shot 01-biblioteca 1

adb shell am start -W -a com.maty.rocola.action.SHUFFLE_ALL -n "$PKG/.MainActivity" > /dev/null
sleep 5
if is_playing; then pass "Arranca con \"Mezclar todo\": $(current_title)"; else fail "No arrancó la reproducción ($(pb_state))"; fi
dismiss_anr
shot 02-reproductor 1

back() { adb shell input keyevent KEYCODE_BACK; sleep 1; }
on_screen() { [ -n "$(ui_find "$@")" ]; }
ensure_front() {
  if ! adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | grep -q "$PKG"; then
    note "La app no estaba al frente; la vuelvo a abrir"
    adb shell am start -W -n "$PKG/.MainActivity" > /dev/null
    sleep 2
  fi
}

if on_screen "Minimizar"; then
  if tap "A continuación"; then shot 03-cola 1; back; fi
  back # minimiza el reproductor
fi
ensure_front
shot 04-biblioteca-mini 1

if tap "Álbumes"; then
  shot 05-albumes 1
  if tap "Luces de Avenida"; then shot 06-album 1; back; fi
fi
ensure_front
tap "Artistas" && shot 07-artistas 1
tap "Carpetas" && shot 08-carpetas 1
tap "Listas" && shot 09-listas 1
tap "Canciones" > /dev/null
if tap "Buscar"; then
  adb shell input text "faro"
  shot 10-busqueda 2
  back
  on_screen "Ajustes" || back
fi
ensure_front
if tap "Ajustes"; then
  shot 11-ajustes 1
  if tap "Ecualizador"; then shot 12-ecualizador 1; back; fi
  back
fi
adb shell am start -W -n "$PKG/.MainActivity" > /dev/null

echo "== Segundo plano con la pantalla apagada"
is_playing || adb shell input keyevent KEYCODE_MEDIA_PLAY
adb shell input keyevent KEYCODE_HOME
sleep 2
PID_BEFORE=$(adb shell pidof "$PKG" | tr -d '\r')
# PIN para que al prender la pantalla aparezca la pantalla de bloqueo con los controles.
adb shell locksettings set-pin 1234 > /dev/null 2>&1 && note "Bloqueo con PIN activado para la prueba"
adb shell dumpsys battery unplug
adb shell input keyevent KEYCODE_SLEEP
sleep 3
WAKE=$(adb shell dumpsys power | grep -m1 "mWakefulness=" | tr -d '\r' | sed 's/^ *//')
note "Estado de pantalla: $WAKE"
adb shell dumpsys deviceidle enable > /dev/null
IDLE=$(adb shell dumpsys deviceidle force-idle | tr -d '\r')
note "Doze: $IDLE"
TITLES="$(current_title)"
for i in $(seq 1 10); do
  sleep 10
  t=$(current_title)
  echo "  ${i}0 s: $(pb_state | cut -c1-60) | $t"
  TITLES="$TITLES"$'\n'"$t"
done
adb shell dumpsys power | grep -iE "ExoPlayer|PARTIAL_WAKE_LOCK" | head -5 > "$OUT/wakelocks.txt"
DISTINCT=$(printf '%s\n' "$TITLES" | sort -u | grep -c .)
if is_playing; then pass "Sigue sonando después de 100 s con pantalla apagada y Doze"; else fail "Se cortó con la pantalla apagada ($(pb_state))"; fi
if [ "$DISTINCT" -ge 3 ]; then pass "Pasó de tema solo con la pantalla apagada ($DISTINCT temas en 100 s)"; else fail "No avanzó de tema con la pantalla apagada ($DISTINCT temas)"; fi
PID_AFTER=$(adb shell pidof "$PKG" | tr -d '\r')
if [ -n "$PID_AFTER" ] && [ "$PID_AFTER" = "$PID_BEFORE" ]; then pass "El proceso siguió vivo (pid $PID_AFTER)"; else fail "El proceso se reinició o murió ($PID_BEFORE -> $PID_AFTER)"; fi
if adb shell dumpsys activity services "$PKG" | grep -q "isForeground=true"; then pass "Servicio de reproducción en primer plano"; else fail "El servicio no está en primer plano"; fi
adb shell dumpsys notification --noredact | grep -A 80 "pkg=$PKG" > "$OUT/notificacion.txt"
if grep -qE "MediaStyle|android.mediaSession" "$OUT/notificacion.txt"; then pass "Notificación multimedia publicada"; else fail "No se encontró la notificación multimedia"; fi

echo "== Botones multimedia (como auriculares Bluetooth), con la pantalla apagada"
adb shell input keyevent KEYCODE_MEDIA_PAUSE
sleep 2
if is_paused; then pass "Pausa con el botón de pausa"; else fail "No pausó ($(pb_state))"; fi
adb shell input keyevent KEYCODE_MEDIA_PLAY
sleep 2
if is_playing; then pass "Reanuda con el botón de play"; else fail "No reanudó ($(pb_state))"; fi
T1=$(current_title)
adb shell input keyevent KEYCODE_MEDIA_NEXT
sleep 2
T2=$(current_title)
if [ "$T1" != "$T2" ]; then pass "Salta al siguiente tema: $T2"; else fail "No saltó de tema"; fi

echo "== Pantalla de bloqueo y notificación"
adb shell dumpsys deviceidle unforce > /dev/null
adb shell dumpsys battery reset
adb shell input keyevent KEYCODE_WAKEUP
shot 13-pantalla-bloqueo 3
adb shell locksettings clear --old 1234 > /dev/null 2>&1
adb shell wm dismiss-keyguard
sleep 2
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell cmd statusbar expand-notifications
sleep 2
adb shell cmd statusbar expand-notifications
shot 14-notificacion 2
# Botones del reproductor del panel de notificaciones, en las posiciones medidas en la
# captura (Pixel 6 del emulador; el volcado de interfaz no responde con la barra animada).
# Primero pausa, así ningún cambio de tema natural se confunde con el botón Siguiente.
tap_xy() { adb shell input tap "$1" "$2"; sleep 2; }
tap_xy 932 736
if is_paused; then pass "El botón Pausa de la notificación funciona"; else fail "El botón Pausa de la notificación no pausó ($(pb_state | cut -c1-60))"; fi
T1=$(current_title)
tap_xy 848 902
T2=$(current_title)
if [ -n "$T2" ] && [ "$T1" != "$T2" ]; then pass "El botón Siguiente de la notificación funciona: $T2"; else fail "El botón Siguiente de la notificación no cambió el tema"; fi
tap_xy 974 902
if pb_state | grep -q "Quitar de favoritas"; then pass "El corazón de la notificación marca la canción como favorita"; else fail "El corazón de la notificación no la marcó como favorita"; fi
shot 15-notificacion-favorita 1
tap_xy 932 736
if is_playing; then pass "El botón Reproducir de la notificación funciona"; else fail "El botón Reproducir de la notificación no reanudó"; fi
adb shell cmd statusbar collapse

echo "== Volver desde la notificación"
adb shell am start -W -a com.maty.rocola.action.OPEN_PLAYER -n "$PKG/.MainActivity" > /dev/null
shot 16-reproductor-otra-vez 2

echo "== Elegir varias canciones y borrarlas"
media_count() { adb shell content query --uri content://media/external/audio/media --projection _id 2>/dev/null | grep -c "Row:"; }
back # minimiza el reproductor que abrió la notificación
ensure_front
tap "Canciones" > /dev/null
BEFORE=$(media_count)
XY=$(ui_find "Arena fría")
if [ -n "$XY" ]; then
  # shellcheck disable=SC2086
  adb shell input swipe $XY $XY 900 # toque largo
  sleep 1.5
  tap "Bajamar" > /dev/null
  shot 17-seleccion 1
  if on_screen "2 seleccionadas"; then pass "Selección múltiple con toque largo (2 seleccionadas)"; else fail "No quedaron 2 canciones seleccionadas"; fi
  if tap "Borrar"; then
    sleep 2
    shot 18-confirmar-borrado 1
    # Botón ALLOW del cartel del sistema; si el volcado no lo encuentra, posición medida en la captura.
    if ! tap "ALLOW" "Allow" "PERMITIR" "Permitir" > /dev/null; then adb shell input tap 794 1408; fi
    sleep 5
    AFTER=$(media_count)
    if [ "$AFTER" -eq $((BEFORE - 2)) ]; then pass "Borró las 2 canciones elegidas ($BEFORE -> $AFTER en la biblioteca de Android)"; else fail "No se borraron las canciones ($BEFORE -> $AFTER)"; fi
    if adb shell ls /sdcard/Music/Rocola | grep -qiE "Arena_fr|Bajamar"; then fail "Los archivos siguen en el teléfono"; else pass "Los archivos se borraron del almacenamiento"; fi
    shot 19-despues-de-borrar 2
    if is_playing; then pass "La música sigue después de borrar"; else fail "Se cortó la música al borrar ($(pb_state | cut -c1-60))"; fi
  else
    fail "No apareció la barra de acciones de la selección"
  fi
else
  fail "No encontré la canción para seleccionar"
fi

echo "== Registros"
adb shell dumpsys media_session > "$OUT/media_session.txt"
adb shell dumpsys activity services "$PKG" > "$OUT/servicios.txt"
adb logcat -d > "$OUT/logcat.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
if grep -q "$PKG" "$OUT/crash.txt"; then fail "Hubo un cierre inesperado (ver crash.txt)"; else pass "Sin cierres inesperados"; fi

echo
cat "$OUT/resultado.txt"
echo "Fallas: $FAILURES"
exit "$FAILURES"
