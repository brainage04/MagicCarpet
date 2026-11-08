#!/usr/bin/env bash
# Starts a plain vanilla 26.3 server (offline, flat, creative) on a free local port and runs the Fabric
# client GameTests against it (MagicCarpetVanillaServerClientGameTest). Run it through nomad-heavy from the
# repository root: bash tools/vanilla-server-check.sh
set -u
export XDG_RUNTIME_DIR=$(mktemp -d); chmod 700 "$XDG_RUNTIME_DIR"
export SDL_VIDEO_DRIVER=x11; unset WAYLAND_DISPLAY DISPLAY
SERVER_PID=""
cleanup() { [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null; wait "$SERVER_PID" 2>/dev/null; rm -rf "$XDG_RUNTIME_DIR"; }
trap cleanup EXIT

./gradlew --no-daemon :fabric:compileGametestJava || exit 1

JAR="$HOME/.gradle/caches/fabric-loom/26.3/minecraft-server.jar"
[ -f "$JAR" ] || { echo "no vanilla server jar at $JAR"; exit 1; }
S="$PWD/build/vanilla-server"; rm -rf "$S"; mkdir -p "$S"
PORT=$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1])')
echo eula=true > "$S/eula.txt"
cat > "$S/server.properties" <<EOF
online-mode=false
white-list=false
enforce-whitelist=false
enforce-secure-profile=false
server-ip=127.0.0.1
server-port=$PORT
gamemode=creative
force-gamemode=true
level-type=minecraft\:flat
spawn-protection=0
view-distance=4
simulation-distance=4
EOF
(cd "$S" && exec java -Xmx1G -jar "$JAR" nogui > console.log 2>&1 < /dev/null) &
SERVER_PID=$!
for i in $(seq 1 240); do grep -q 'Done (' "$S/console.log" 2>/dev/null && break; sleep 1; done
grep -q 'Done (' "$S/console.log" || { echo "vanilla server did not start"; tail -20 "$S/console.log"; exit 1; }
echo "vanilla server up on 127.0.0.1:$PORT (pid $SERVER_PID)"

MAGIC_CARPET_VANILLA_SERVER="127.0.0.1:$PORT" ./gradlew --no-daemon :fabric:runProductionClientGameTest
RC=$?
echo "=== vanilla server console (decode lines) ==="
grep -n -i -E 'decode|lost connection|joined the game|left the game' "$S/console.log"
cp "$S/console.log" build/vanilla-server-console.log

exit $RC
