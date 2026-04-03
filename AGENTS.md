# AGENTS.md — Guía rápida para futuras sesiones en `gnirehtet`

## 1) Qué es este proyecto
`gnirehtet` implementa **reverse tethering** por `adb`: el teléfono Android usa la conexión a Internet del host (PC/Mac/Linux) sin root. El tráfico del dispositivo se intercepta con `VpnService` y se reenvía a un relay en el host.

> Estado del proyecto: mantenimiento mínimo (se corrigen bloqueos mayores/builds), por lo que conviene priorizar cambios pequeños, compatibles y de bajo riesgo.

---

## 2) Estructura del repositorio (alto nivel)

- `app/` → Cliente Android (APK `gnirehtet.apk`).
- `relay-java/` → Relay + CLI en Java (genera `gnirehtet.jar` y scripts).
- `relay-rust/` → Relay + CLI en Rust (binario nativo recomendado).
- `config/` → Checkstyle y configuración común de estilo/build.
- `release` → Script para empaquetar releases (Java + Rust + APK).
- `README.md` → Uso para usuarios.
- `DEVELOP.md` → Arquitectura y flujo de desarrollo.

`settings.gradle` incluye 3 módulos: `:app`, `:relay-java`, `:relay-rust`.

---

## 3) Flujo funcional (cómo se conecta todo)

1. En host se ejecuta `gnirehtet` (Java o Rust).
2. Se configura `adb reverse localabstract:gnirehtet tcp:31416`.
3. En Android, `GnirehtetActivity` recibe intents START/STOP.
4. `GnirehtetService` levanta un `VpnService`, lee/escribe paquetes IPv4.
5. El cliente encapsula paquetes hacia el relay por TCP.
6. El relay abre sockets TCP/UDP reales y hace traducción de paquetes.

Puntos clave:
- Solo IPv4 (sin IPv6).
- Puerto por defecto: `31416`.
- DNS por defecto en cliente Android: `8.8.8.8` si no se pasa configuración.

---

## 4) Archivos clave para orientarse rápido

### Android (`app/`)
- `GnirehtetActivity.java`: puerta de entrada por intents (`START`/`STOP`) y autorización VPN.
- `GnirehtetService.java`: ciclo de vida VPN, rutas/DNS/MTU, `Forwarder`.
- `Forwarder.java`: puente entre interfaz VPN y túnel relay.
- `PersistentRelayTunnel.java`, `RelayTunnel.java`: conexión/reconexión al relay.
- `VpnConfiguration.java`: parseo y transporte de DNS/rutas.

### Relay Java (`relay-java/`)
- `Main.java`: CLI principal (run/autorun/start/stop/relay/etc).
- `CommandLineArguments.java`: parseo de argumentos.
- `relay/Relay.java`: loop principal de IO asíncrona.
- `relay/Client.java`, `relay/Router.java`: manejo de cliente y enrutado.
- `relay/TCPConnection.java`, `relay/UDPConnection.java`: backend sockets.
- `relay/Packetizer.java`, `relay/IPv4Packet*.java`, headers: manipulación paquetes.

### Relay Rust (`relay-rust/`)
- `src/main.rs`: CLI equivalente al de Java.
- `src/cli_args.rs`: parseo de argumentos.
- `src/relay/relay.rs`: loop/eventos con `mio`.
- `src/relay/client.rs`, `router.rs`, `tcp_connection.rs`, `udp_connection.rs`.
- `src/relay/packetizer.rs`, `ipv4_packet*.rs`, `*_header.rs`.

---

## 5) Comandos de desarrollo más útiles

## Build/check desde raíz (Gradle)
- `./gradlew build` → build completo (app + relays, debug/release según tasks).
- `./gradlew checkAll` → checks Java + Rust (incluye estilo/tests según módulo).
- `./gradlew debugJava releaseJava debugRust releaseRust`

## Android
- `./gradlew :app:assembleDebug`
- `./gradlew :app:test`

## Relay Java
- `./gradlew :relay-java:test`
- `./gradlew :relay-java:assembleRelease`

## Relay Rust
- `cd relay-rust && cargo test`
- `cd relay-rust && cargo fmt -- --check`
- `cd relay-rust && cargo build --release`

## Release
- `./release` (requiere toolchains y firma configurada).

---

## 6) Convenciones/prácticas para contribuir

- Mantener **paridad funcional** entre implementación Java y Rust cuando el cambio afecte CLI/protocolo.
- Evitar rediseños grandes: preferir fixes localizados y compatibles.
- Si tocas parseo de paquetes, añadir/ajustar tests de headers/packetizer.
- Si tocas CLI, validar ayuda/argumentos en Java y Rust.
- Verificar que no se rompe el flujo `run`, `autorun`, `start`, `stop`, `tunnel`, `relay`.
- Respetar Checkstyle en Java (`config/checkstyle/checkstyle.xml`) y `cargo fmt` en Rust.

---

## 7) Debug y troubleshooting (rápido)

- Si no conecta cliente↔relay:
  - validar `adb devices` y depuración USB.
  - reejecutar `gnirehtet tunnel [serial]`.
  - revisar si el relay escucha en puerto esperado (`31416` por defecto).
- Si VPN Android no arranca:
  - revisar autorización VPN del sistema.
  - confirmar min API/restricciones de foreground service.
- Si hay desconexiones al reconectar cable USB:
  - usar comando `tunnel` o `run` nuevamente.

---

## 8) Prioridad de lectura para nuevos agentes

1. `README.md` (uso y expectativas).
2. `DEVELOP.md` (arquitectura completa).
3. Entrada por módulo:
   - Android: `GnirehtetActivity` → `GnirehtetService` → `Forwarder`.
   - Java: `Main` → `Relay` → `Client/Router`.
   - Rust: `main.rs` → `relay.rs` → `client/router`.
4. Tests relacionados con el área modificada.

---

## 9) Checklist antes de cerrar una tarea

- [ ] Código compila en el/los módulos tocados.
- [ ] Tests relevantes pasan.
- [ ] Estilo/lint pasa (`checkstyle`/`cargo fmt --check`).
- [ ] Documentación/README actualizado si cambió comportamiento de usuario o CLI.
- [ ] Si se tocó protocolo o argumentos, validar equivalencia Java/Rust.
