# Estado del Proyecto — BackpacksRemastered (fork)

> Documento de seguimiento para retomar el proyecto desde cualquier dispositivo o sesión.

---

## Resumen del proyecto

Fork de [BackpacksRemastered](https://github.com/divisionind/BackpacksRemastered) adaptado para **Spigot 1.21.11**.  
El objetivo principal es eliminar la capa NMS legacy y rediseñar los tipos de mochila.

---

## Entorno de desarrollo

| Dato | Valor |
|---|---|
| Servidor | Spigot 1.21.11 (v1_21_R7, no obfuscado) |
| Java | JDK 21 (`C:\Program Files\Java\jdk-21.0.10`) |
| Gradle | 8.5 |
| Comando de build | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat pack 2>&1` |
| JAR de salida | `build\libs\BackpacksRemastered-2022.1.8-beta.0.jar` |
| Plugin en servidor | `~/minecraft/spigot121/plugins/` (acceso via SCP/SFTP) |

---

## Cambios ya realizados (completados)

### 1. Capa NMS → PersistentDataContainer ✅
- `NMSItemStack.java` reescrito: ya no usa reflexión NMS directa, usa `ItemMeta.getPersistentDataContainer()`.
- `NMS.initialize()` devuelve lista vacía (sin errores NMS al arrancar).
- `NBTMap.java` con null-checks para evitar NPE en ítems sin PDC.

### 2. Rediseño de tipos de mochila ✅

**Eliminados:**
- `FURNACE` (no funciona bien con PDC)
- `CRAFT` (crafteo portátil)
- `COMBINED` (mochila que contiene mochilas)

**Mantenidos:**
- `LINKED` — se enlaza a un contenedor del mundo
- `ENDER` — abre el cofre ender del jugador

**Nuevos tiers añadidos:**

| Tipo | Slots | Receta (8x material + cofre centro) | Color (cuero) |
|---|---|---|---|
| SMALL | 9 | 8x LEATHER + CHEST | Marrón `#654321` |
| MEDIUM | 27 | 8x COPPER_INGOT + CHEST | Gris `#8C8C8C` |
| LARGE | 45 | 8x IRON_INGOT + CHEST | Azul `#0078C8` |
| EXTRALARGE | 54 | 8x DIAMOND + CHEST | Oscuro `#282828` |

**IDs internos (importante para migración):**

| Tipo | ID nuevo | ID antiguo |
|---|---|---|
| SMALL | 0 | 0 |
| MEDIUM | 1 | *(nuevo)* |
| LARGE | 2 | 1 |
| EXTRALARGE | 3 | *(nuevo)* |
| LINKED | 4 | 2 |
| ENDER | 5 | 5 |

### 3. Archivos modificados ✅

| Archivo | Cambio |
|---|---|
| `BackpackObject.java` | Nuevos 6 tipos con IDs, colores, handlers y **atributos de armadura** |
| `BPSmall.java` | 9 slots (era 27) |
| `BPLarge.java` | 45 slots (era 54) |
| `BPMedium.java` | **NUEVO** — 27 slots, extiende BPStorage |
| `BPExtraLarge.java` | **NUEVO** — 54 slots, extiende BPStorage |
| `BPStorage.java` | `openBackpack()` redimensiona el inventario si el tamaño guardado difiere del handler (fix versiones antiguas) |
| `BackpackCraftEvent.java` | Eliminada toda la lógica COMBINED; solo queda check de permiso |
| `BackpackInvClickEvent.java` | Eliminada la delegación de clicks a BPCombined |
| `CMigrateBackpack.java` | Remapeo de IDs + fix reflexión + migración de pluma-llave + método `migrateFeatherKeys()` |
| `CInfo.java` | URL actualizada al nuevo repositorio (`enigmoes/BackpacksRemastered`) |
| `config.yml` | Recetas completamente rediseñadas; versión subida a **9** |
| `plugin.yml` | Permisos actualizados (añadidos medium/extralarge, eliminados combined/craft/furnace/split/vfurnace) |
| `Backpacks.java` | Eliminados: `CVFurnace`, `CSplit`, `BackpackFurnaceTickEvent`, `BackpackTrackEvents`, `MAX_COMBINED_BACKPACKS` |
| `README.md` | Actualizado con nuevo repo, nueva tabla de tipos, instrucciones para Windows |

### 4. Archivos eliminados ✅
- `BPCombined.java`
- `BPCraft.java`
- `BPFurnace.java`
- `CVFurnace.java`
- `CSplit.java`
- `BackpackFurnaceTickEvent.java`
- `BackpackTrackEvents.java`
- `Dockerfile` — entorno de compilación del autor original (Ubuntu+Java8/11), no necesario con Java 21 local

### 5. Correcciones de bugs ✅
- NPE en `BackpackInvClickEvent` al hacer clic con ítem null o sin pechera equipada
- NPE en `BackpackOpenCloseEvent`, `BackpackCraftEvent`, `BackpackNetheriteUpgrade`
- `CMigrateBackpack`: `NoSuchFieldException: CUSTOM_DATA` → reflexión multi-estrategia
- `CMigrateBackpack`: `NoSuchMethodException: ItemStack.get(DataComponentType)` → escaneo de métodos por invocación directa (brute-force) en vez de `isAssignableFrom`, porque el tipo runtime de `CUSTOM_DATA` es `DataComponentType$a$a` (inner class obfuscada)
- `CMigrateBackpack`: `copyTag()` no existe en v1_21_R7 → se detecta el CompoundTag buscando el método cuyo **tipo de retorno** es `NBTTagCompound` (método `b()` en v1_21_R7)
- `CMigrateBackpack`: `tagContains` y `tagGetInt` reescritos para escanear por firma de método (String→boolean, String/String+int→int) en lugar de nombre exacto
- `CMigrateBackpack`: `tagGetByteArray` usa `getDeclaredMethods` + `setAccessible` recorriendo la jerarquía de clases; si no encuentra el método devuelve `null` y el backpack se migra sin contenido (inventario vacío) en vez de lanzar excepción
- `CMigrateBackpack`: pluma-llave antigua (con `backpack_key` en `custom_data` NBT) migrada automáticamente al PDC; si no existe pluma válida, se da una nueva
- `CMigrateBackpack`: si la pechera ya está migrada, ejecutar igualmente la migración de plumas (`migrateFeatherKeys()`)
- `BPStorage.openBackpack()`: si el tamaño del inventario guardado difiere del definido en el handler (cambio de versión), se redimensiona copiando los ítems existentes
- `ArrayIndexOutOfBoundsException` al cargar recetas → versión de config subida a 9 para forzar regeneración del config antiguo en el servidor

### 6. Atributos de armadura por tipo ✅

Cada backpack tiene atributos de armadura propios que **reemplazan** los 3 puntos por defecto de la pechera de cuero. Valores definidos en `BackpackObject.java`:

| Tipo | Referencia (material receta) | Armadura | Dureza |
|---|---|---|---|
| SMALL | Cuero = 3 | **2** | 0 |
| MEDIUM | Cota malla ~5 (cobre) | **4** | 0 |
| LARGE | Hierro = 6 | **5** | 0 |
| EXTRALARGE | Diamante = 8 / d.2 | **7** | **1** |
| LINKED | Mix hierro-diamante | **5** | 0 |
| ENDER | Cuero base | **3** | 0 |

Implementado con `AttributeModifier(UUID, String, double, ADD_NUMBER, EquipmentSlot.CHEST)` (API Bukkit 1.17, compatible con 1.21.11). UUID determinista por tipo via `UUID.nameUUIDFromBytes`.

---

## Pendiente / En progreso

### 🟡 Pendiente de prueba en servidor

- [ ] `/backpacks migrate` con pluma antigua migra también la pluma-llave al nuevo formato
- [ ] Recetas de MEDIUM y EXTRALARGE funcionan en mesa de crafteo
- [ ] Los 4 tiers abren correctamente (9/27/45/54 slots)
- [ ] Los ítems guardados en SMALL y LARGE (antiguo formato, si existen) se cargan bien
- [ ] LINKED: vincular a cofre y recuperar inventario funciona
- [ ] ENDER: abre cofre ender correcto
- [ ] Netherite upgrade sobre mochila funciona
- [ ] Los atributos de armadura se muestran correctamente en la descripción del ítem
- [ ] Comprobar que config.yml se regenera al subir el nuevo JAR (versión 9)

---

## Estructura del código relevante

```
src/main/java/com/divisionind/bprm/
├── BackpackObject.java          ← enum de tipos: SMALL,MEDIUM,LARGE,EXTRALARGE,LINKED,ENDER
├── BackpackRecipes.java         ← carga recetas desde config.yml
├── Backpacks.java               ← plugin principal, registro de comandos y eventos
├── PotentialBackpackItem.java   ← wrapper para leer/escribir PDC en ItemStacks
├── backpacks/
│   ├── BPStorage.java           ← base abstracta para mochilas de inventario
│   ├── BPSmall.java             ← 9 slots
│   ├── BPMedium.java            ← 27 slots (nuevo)
│   ├── BPLarge.java             ← 45 slots
│   ├── BPExtraLarge.java        ← 54 slots (nuevo)
│   ├── BPLinked.java            ← mochila vinculada a contenedor
│   └── BPEnder.java             ← mochila ender
├── commands/
│   ├── CMigrateBackpack.java    ← migra formato antiguo NBT → PDC
│   ├── CHelp.java
│   ├── CInfo.java
│   ├── CItemGive.java
│   ├── CItemInfo.java
│   └── ...
├── events/
│   ├── BackpackCraftEvent.java
│   ├── BackpackInvClickEvent.java
│   ├── BackpackOpenCloseEvent.java
│   ├── BackpackDamageEvent.java
│   ├── BackpackLinkEvent.java
│   └── BackpackNetheriteUpgrade.java
└── nms/
    ├── NMSItemStack.java        ← usa PersistentDataContainer (no NMS directo)
    └── reflect/
        ├── NMS.java
        ├── NBTType.java
        └── NBTMap.java
```

---

## Notas técnicas importantes

- **`PotentialBackpackItem`**: usa las keys `"backpack_type"` (INT) y `"backpack_data"` (BYTE_ARRAY) en el PDC del ítem.
- **`BPStorage.openBackpack()`**: deserializa el inventario con `BukkitObjectInputStream` desde los bytes del PDC.
- **`BackpackObject.hasCraftPermission()`**: comprueba `"backpacks.craft." + name().toLowerCase()`.
- **El color de cuero** se aplica en `BackpackObject.getItem()` que llama a `BackpackHandler.createItem()`.
- **Versión de config**: ahora es **9**. Si el servidor tiene config versión 8 (o menor), se mueve a `.bak` y se regenera automáticamente.
- **`VirtualFurnace.java`**: sigue existiendo como clase huérfana (sin referencias activas). Se puede eliminar en el futuro sin riesgo.

### Quirks de reflexión en Spigot v1_21_R7 (obfuscado)

| Problema | Solución aplicada |
|---|---|
| `DataComponentType` runtime es `DataComponentType$a$a` → `isAssignableFrom` falla | Brute-force: invocar todos los métodos de 1 parámetro del NMS ItemStack y capturar `IllegalArgumentException` |
| `CustomData.copyTag()` no existe | Buscar método 0-param cuyo tipo de retorno contiene `"NBTTagCompound"` o `"Compound"` → es `b()` en v1_21_R7 |
| `NBTTagCompound.contains()` / `getInt()` / `getByteArray()` obfuscados | Escanear por firma: tipo de retorno + tipos de parámetros, usando `getDeclaredMethods` + `setAccessible` en jerarquía completa |
| `getInt` en 1.20.5+ requiere segundo parámetro `int default` | Probar firma `(String, int)` antes que `(String)` |
