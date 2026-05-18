# BackpacksRemastered (fork)

> **Fork** de [divisionind/BackpacksRemastered](https://github.com/divisionind/BackpacksRemastered), mantenido por [enigmoes](https://github.com/enigmoes/BackpacksRemastered).

![](https://img.shields.io/badge/license-GPLv3-green.svg?style=flat-square)
![](https://img.shields.io/badge/Spigot-1.21.11-orange.svg?style=flat-square)
![](https://img.shields.io/badge/Java-21-blue.svg?style=flat-square)

![](https://raw.githubusercontent.com/divisionind/BackpacksRemastered/master/screenshots/logo.png)

> BackpacksRemastered añade mochilas a Minecraft sin ninguna modificación del cliente. Las mochilas persisten su inventario en el propio ItemStack usando **PersistentDataContainer** (compatible con Spigot 1.21.11+).

---

## Cambios respecto al fork original

Este fork moderniza el plugin con los siguientes cambios principales:

### Capa NMS eliminada
La capa NMS (acceso directo a clases internas del servidor) ha sido reemplazada completamente por la API `PersistentDataContainer` de Bukkit. El plugin ya no requiere una versión exacta de NMS y es compatible con Spigot 1.20.5+.

### Nuevos tipos de mochila

| Tipo | Slots | Receta | Color |
|---|---|---|---|
| Small | 9 | 8× Leather + Chest | Marrón |
| Medium | 27 | 8× Copper Ingot + Chest | Gris |
| Large | 45 | 8× Iron Ingot + Chest | Azul |
| Extra Large | 54 | 8× Diamond + Chest | Oscuro |
| Linked | — | 4× Diamond + 2× Leather + Eye of Ender + 2× Iron | Azul |
| Ender | — | 4× Eye of Ender + 4× Obsidian + Leather | Verde |

Todas las recetas usan el patrón `8× material + cofre en el centro`.

### Tipos eliminados
- **Furnace Backpack** — funcionamiento incompatible con PDC
- **Craft Backpack** — banco de trabajo portátil
- **Combined Backpack** — mochila que contenía otras mochilas

### Comando de migración
`/backpacks migrate` — migra backpacks del formato NBT antiguo (pre-1.20.5) al nuevo formato PDC.

---

## Tabla de contenidos
**[Compilar](#compilar)**<br>
**[Adaptadores e integración](#adaptadores-e-integración)**<br>

## Compilar

### Requisitos
- Java 21
- Gradle (incluido via wrapper `gradlew`)

### Pasos
```shell
git clone https://github.com/enigmoes/BackpacksRemastered.git
cd BackpacksRemastered
./gradlew pack
```
El JAR se generará en `build/libs/BackpacksRemastered-*.jar`.

#### En Windows (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat pack
```

## Adaptadores e integración
Backpacks soporta integración con plugins de terceros mediante "Adaptadores". Estos adaptadores pueden registrar comandos personalizados u otra funcionalidad a través de "Abilities".

#### An example adaptor (see examples/ for more)
Below is an example of a plugin adaptor for a plugin named "ExamplePlugin". The name value of the `@PluginAdaptorMeta`
annotation must be the name of your plugin as registered by your plugin.yml.
```java
@PluginAdaptorMeta(name = "ExamplePlugin")
public class AdaptorExamplePlugin extends PluginAdaptor {

    private ExamplePlugin parent;

    @Override
    public void onEnable(Plugin parent) throws Exception {
        this.parent = (ExamplePlugin) parent;

        Backpacks.getInstance().registerCommands(new CExampleCommand());
        getLogger().info("Registered ExamplePlugin adaptor!");
    }

    @AbilityFunction
    public boolean hasAccessToContainer(Player player, Location location) {
        // ... logic
        return true; // or false
    }

    private static class CExampleCommand extends ACommand {
        @Override
        public String alias() {
            return "example";
        }

        @Override
        public String desc() {
            return "an example command registered by an example adaptor";
        }

        @Override
        public String usage() {
            return null;
        }

        @Override
        public String permission() {
            return "backpacks.example";
        }

        @Override
        public void execute(CommandSender sender, String label, String[] args) {
            // need a player instance? Player player = validatePlayer(sender);
            // if the sender is not a player, the command will return and respond accordingly
            respond(sender, "&eHello world!");
        }
    }
}
```

#### Cargar el adaptador
```java
public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Backpacks.getAdaptorManager().registerAdaptors(AdaptorExamplePlugin.class);
        getLogger().info("ExamplePlugin has been enabled!");
    }
} 
```

Methods tagged with the `@AbilityFunction` annotation inside of a `PluginAdaptor` are automatically registered
as abilities with the system. If no value is specified (e.g. `@AbilityFunction("someName")`) then the method's 
name is used as the ability name. Abilities overwrite each other, so, if multiple adaptors register an ability
with the same name, the last adaptor loaded will have the ability that persists. The following is a list of
current abilities used internally by Backpacks:

#### Abilities actuales
- `boolean hasAccessToContainer(Player, Location)`: devuelve `true` si el jugador tiene acceso al bloque en la ubicación indicada.
