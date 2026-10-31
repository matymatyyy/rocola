#!/usr/bin/env bash
# Genera canciones de prueba (acordes sintetizados) con etiquetas ID3 y tapas,
# para probar Rocola en el emulador de la CI. Uso: ci/make_fixtures.sh <carpeta_salida>
set -euo pipefail
OUT="${1:-fixtures}"
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$OUT"

# album|artista|año|tapa|título|frecuencias del acorde|segundos
TRACKS=(
  "Luces de Avenida|Los Faroles|2024|luces|Corrientes a las tres|220 277 330|33"
  "Luces de Avenida|Los Faroles|2024|luces|Neón y garúa|196 247 294|31"
  "Luces de Avenida|Los Faroles|2024|luces|Último colectivo|174 220 262|35"
  "Marea Baja|Coral Sur|2023|marea|Arena fría|262 330 392|32"
  "Marea Baja|Coral Sur|2023|marea|Faro|294 370 440|31"
  "Marea Baja|Coral Sur|2023|marea|Bajamar|247 311 370|34"
  "Fiebre Lenta|Nadia Ferro|2025|fiebre|Calor de enero|330 415 494|31"
  "Fiebre Lenta|Nadia Ferro|2025|fiebre|Ventilador|349 440 523|33"
  "Fiebre Lenta|Nadia Ferro|2025|fiebre|Siesta eléctrica|311 392 466|32"
  "Kilómetro Cero|Ruta 40|2022|kilometro|Kilómetro cero|165 208 247|31"
  "Kilómetro Cero|Ruta 40|2022|kilometro|Viento Zonda|185 233 277|34"
  "Kilómetro Cero|Ruta 40|2022|kilometro|Mate en la banquina|147 185 220|32"
  "Maquetas|Anónimo|2021||Demo sin tapa|233 294 349|31"
  "Notas|Grabadora|2026||Nota de voz|440 440 440|12"
)

declare -A NUM
for row in "${TRACKS[@]}"; do
  IFS='|' read -r album artist year cover title freqs secs <<<"$row"
  NUM[$album]=$(( ${NUM[$album]:-0} + 1 ))
  n=${NUM[$album]}
  read -r f1 f2 f3 <<<"$freqs"
  # Acorde con un leve trémolo para que suene a "algo" y no a un tono plano.
  expr="(0.22*sin(2*PI*${f1}*t)+0.16*sin(2*PI*${f2}*t)+0.12*sin(2*PI*${f3}*t))*(0.75+0.25*sin(2*PI*2*t))"
  slug=$(echo "${artist}-${n}-${title}" | iconv -f utf-8 -t ascii//TRANSLIT | tr -cs 'A-Za-z0-9' '_' )
  file="$OUT/${slug}.mp3"
  tags=(-metadata "title=${title}" -metadata "artist=${artist}" -metadata "album_artist=${artist}"
        -metadata "album=${album}" -metadata "track=${n}" -metadata "date=${year}" -metadata "genre=Rock")
  if [[ -n "$cover" ]]; then
    ffmpeg -loglevel error -y -f lavfi -i "aevalsrc=${expr}:s=44100:d=${secs}" -i "$HERE/covers/${cover}.jpg" \
      -map 0:a -map 1:v -c:a libmp3lame -b:a 64k -ac 1 -c:v copy -id3v2_version 3 \
      -metadata:s:v title="Album cover" -metadata:s:v comment="Cover (front)" -disposition:v attached_pic \
      "${tags[@]}" "$file"
  else
    ffmpeg -loglevel error -y -f lavfi -i "aevalsrc=${expr}:s=44100:d=${secs}" \
      -c:a libmp3lame -b:a 64k -ac 1 -id3v2_version 3 "${tags[@]}" "$file"
  fi
done
ls -la "$OUT"
