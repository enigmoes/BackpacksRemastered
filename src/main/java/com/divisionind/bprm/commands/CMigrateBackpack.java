/*
 * BackpacksRemastered - remastered version of the popular Backpacks plugin
 * Copyright (C) 2019, Andrew Howard, <divisionind.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.divisionind.bprm.commands;

import com.divisionind.bprm.ACommand;
import com.divisionind.bprm.Backpacks;
import com.divisionind.bprm.BackpackObject;
import com.divisionind.bprm.BackpackRecipes;
import com.divisionind.bprm.nms.NMSItemStack;
import com.divisionind.bprm.nms.reflect.NBTType;
import com.divisionind.bprm.nms.reflect.NMS;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Migrates backpacks from the old NMS NBT format (pre-1.20.5) to the new PersistentDataContainer format.
 *
 * In Minecraft 1.20.5+, raw NMS NBT data written by old versions of this plugin is stored
 * inside the minecraft:custom_data DataComponent. This command reads that legacy data and
 * writes it to the PDC so the new plugin code can recognize the backpack.
 */
public class CMigrateBackpack extends ACommand {

    // Cached to avoid repeated reflection on every migrate call
    private static Object cachedCustomDataType = null;
    private static Class<?> cachedComponentTypeClass = null;

    @Override
    public String alias() {
        return "migrate";
    }

    @Override
    public String desc() {
        return "migrates your equipped backpack from the old (pre-1.20.5) format to the new format";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public String permission() {
        return "backpacks.migrate";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        Player p = (Player) sender;
        ItemStack chestplate = p.getInventory().getChestplate();

        if (chestplate == null) {
            respond(p, "&cNo tienes ningún backpack equipado como pechera.");
            return;
        }

        try {
            // Check if the item already has new-format PDC data
            NMSItemStack nmsCheck = new NMSItemStack(chestplate);
            if (nmsCheck.hasNBT("backpack_type")) {
                // Chestplate already migrated — still attempt to migrate feather keys
                migrateFeatherKeys(p);
                respond(p, "&eEste backpack ya está en el nuevo formato, no necesita migración.");
                return;
            }

            // Try to read old NBT from the minecraft:custom_data DataComponent
            Object oldTag = getCustomDataTag(chestplate);
            if (oldTag == null || !tagContains(oldTag, "backpack_type")) {
                respond(p, "&cLa pechera equipada no es un backpack del formato antiguo.");
                return;
            }

            int backpackType = tagGetInt(oldTag, "backpack_type");

            // Remap old type IDs to new IDs.
            // Old: SMALL=0, LARGE=1, LINKED=2. New: SMALL=0, MEDIUM=1, LARGE=2, EXTRALARGE=3, LINKED=4, ENDER=5
            switch (backpackType) {
                case 1: backpackType = 2; break; // old LARGE -> new LARGE
                case 2: backpackType = 4; break; // old LINKED -> new LINKED
                case 5: backpackType = 5; break; // old ENDER -> new ENDER (same)
                // old COMBINED(3), CRAFT(4), FURNACE(6) have no equivalent; leave type as-is and let getByType return null
                default: break;
            }

            BackpackObject bpo = BackpackObject.getByType(backpackType);
            if (bpo == null) {
                respond(p, "&cTipo de backpack desconocido (" + backpackType + "). No se puede migrar.");
                return;
            }

            // Write backpack_type to the new PDC format
            NMSItemStack nmsItem = new NMSItemStack(chestplate);
            nmsItem.setNBT(NBTType.INT, "backpack_type", backpackType);

            // Migrate backpack_data (serialized inventory) if present
            if (tagContains(oldTag, "backpack_data")) {
                byte[] data = tagGetByteArray(oldTag, "backpack_data");
                if (data != null && data.length > 0) {
                    nmsItem.setNBT(NBTType.BYTE_ARRAY, "backpack_data", data);
                    respond(p, "&aBackpack &e" + bpo.name() + "&a migrado correctamente con su contenido.");
                } else {
                    respond(p, "&aBackpack &e" + bpo.name() + "&a migrado. &eContenido no recuperado (inventario vacío).");
                }
            } else {
                respond(p, "&aBackpack &e" + bpo.name() + "&a migrado correctamente.");
            }

            // Update the chestplate slot with the migrated item
            p.getInventory().setChestplate(nmsItem.getModifiedItem());
            migrateFeatherKeys(p);

        } catch (Exception e) {
            respond(p, "&cError al migrar el backpack. Revisa el log del servidor para más detalles.");
            e.printStackTrace();
        }
    }

    private void migrateFeatherKeys(Player p) throws Exception {
        int migratedKeys = 0;
        boolean hasNewKey = false;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];
            if (slot == null || slot.getType() != org.bukkit.Material.FEATHER) continue;
            NMSItemStack nmsFeather = new NMSItemStack(slot);
            if (nmsFeather.hasNBT("backpack_key")) { hasNewKey = true; continue; }
            try {
                Object featherTag = getCustomDataTag(slot);
                if (featherTag != null && tagContains(featherTag, "backpack_key")) {
                    nmsFeather.setNBT(NBTType.BOOLEAN, "backpack_key", true);
                    p.getInventory().setItem(i, nmsFeather.getModifiedItem());
                    migratedKeys++;
                }
            } catch (Exception ignored) {}
        }
        if (migratedKeys > 0) {
            respond(p, "&aTambién se migró tu Backpack Key al nuevo formato.");
        } else if (!hasNewKey) {
            p.getInventory().addItem(BackpackRecipes.backpackKey.clone());
            respond(p, "&eTambién se te ha dado una Backpack Key nueva (no se detectó ninguna).");
        }
    }

    /**
     * Reads the CompoundTag from the item's minecraft:custom_data DataComponent using reflection.
     * Throws a descriptive exception at each failure point so the caller can surface it as debug info.
     *
     * @return CompoundTag as Object, or null if the item has no custom_data component.
     */
    private Object getCustomDataTag(ItemStack item) throws Exception {
        // Step 1: CraftItemStack.asNMSCopy(item) -> NMS ItemStack
        Class<?> craftClass;
        try {
            craftClass = Class.forName(NMS.CRAFT + "inventory.CraftItemStack");
        } catch (ClassNotFoundException e) {
            throw new Exception("[STEP1] No se encontró CraftItemStack en: " + NMS.CRAFT + "inventory.CraftItemStack", e);
        }
        Method asNMSCopy = craftClass.getMethod("asNMSCopy", ItemStack.class);
        Object nmsItem = asNMSCopy.invoke(null, item);

        // Step 2: Find the CUSTOM_DATA DataComponentType
        Object customDataType = findCustomDataComponentType();
        if (customDataType == null) {
            throw new Exception("[STEP2] No se encontró DataComponentType CUSTOM_DATA. " +
                "Clases buscadas: net.minecraft.core.component.DataComponents, " +
                "net.minecraft.world.item.component.DataComponents. " +
                "Clases NMS disponibles con 'DataComponent' en el nombre: " + listDataComponentClasses());
        }

        // Step 3: Call nmsItem.get(DataComponentType) via brute-force method scan.
        // isAssignableFrom fails in Spigot 1.21.x because the runtime type of CUSTOM_DATA
        // is an obfuscated inner class (DataComponentType$a$a) that doesn't match the declared
        // parameter type. Instead, try invoking every 1-param non-void method and catch
        // IllegalArgumentException to skip those that don't accept our argument.
        Object customData = null;
        boolean step3Done = false;
        String step3FailLog = "";
        for (Method m : nmsItem.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (m.getReturnType() == void.class || m.getReturnType().isPrimitive()) continue;
            try {
                customData = m.invoke(nmsItem, customDataType);
                step3Done = true;
                break;
            } catch (IllegalArgumentException ignored) {
                // Parameter type doesn't match, try next
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Method accepted the argument but threw internally; record and skip
                step3FailLog = m.getName() + " threw: " + e.getCause();
            }
        }
        if (!step3Done) {
            throw new Exception("[STEP3] Ningún método del NMS ItemStack aceptó DataComponentType. " +
                "Clase: " + nmsItem.getClass().getName() +
                (step3FailLog.isEmpty() ? "" : " | último error: " + step3FailLog));
        }
        if (customData == null) return null; // ítem sin custom_data, es normal

        // Step 4: Extract the CompoundTag from the CustomData object.
        // In Spigot 1.21.x (v1_21_R7) the method is b()->NBTTagCompound (obfuscated).
        // Identify the right method by checking the return type name for "Compound" or "NBTTag",
        // which is more reliable than probing the result's own methods (also obfuscated).
        Object compoundTag = null;
        // First pass: try well-known names
        for (String methodName : new String[]{"copyTag", "getUnsafe", "b", "a"}) {
            try {
                Method m = customData.getClass().getMethod(methodName);
                String retName = m.getReturnType().getSimpleName();
                if (retName.contains("Compound") || retName.contains("NBTTag") || retName.contains("Tag")) {
                    Object result = m.invoke(customData);
                    if (result != null) { compoundTag = result; break; }
                }
            } catch (NoSuchMethodException | IllegalArgumentException ignored) {}
        }
        // Second pass: scan all 0-param methods by return type name
        if (compoundTag == null) {
            for (Method m : customData.getClass().getMethods()) {
                if (m.getParameterCount() != 0 || m.getReturnType() == void.class || m.getReturnType().isPrimitive()) continue;
                String retName = m.getReturnType().getSimpleName();
                if (retName.contains("Compound") || retName.contains("NBTTag") || retName.contains("Tag")) {
                    try {
                        Object result = m.invoke(customData);
                        if (result != null) { compoundTag = result; break; }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (compoundTag == null) {
            StringBuilder methods = new StringBuilder();
            for (Method m : customData.getClass().getMethods()) {
                if (m.getParameterCount() == 0) methods.append(m.getName()).append("()->").append(m.getReturnType().getSimpleName()).append(" ");
            }
            throw new Exception("[STEP4] No se pudo extraer CompoundTag de " + customData.getClass().getSimpleName() +
                ". Métodos 0-param: " + methods);
        }
        return compoundTag;
    }

    private static String listDataComponentClasses() {
        StringBuilder sb = new StringBuilder();
        for (Package p : Package.getPackages()) {
            String name = p.getName();
            if (name.contains("minecraft") && name.contains("component")) {
                sb.append(name).append("; ");
            }
        }
        return sb.length() > 0 ? sb.toString() : "(ninguno encontrado)";
    }

    /**
     * Finds the DataComponentType for minecraft:custom_data by scanning all static fields
     * of DataComponents and identifying the one whose generic type contains "CustomData".
     */
    private static Object findCustomDataComponentType() {
        if (cachedCustomDataType != null) return cachedCustomDataType;

        for (String className : new String[]{
                "net.minecraft.core.component.DataComponents",
                "net.minecraft.world.item.component.DataComponents",
        }) {
            try {
                Class<?> dc = Class.forName(className);
                // First try direct field lookup (works on Mojang-mapped unobfuscated builds)
                for (String fieldName : new String[]{"CUSTOM_DATA", "custom_data"}) {
                    try {
                        Field f = dc.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val != null) {
                            cachedCustomDataType = val;
                            return val;
                        }
                    } catch (NoSuchFieldException ignored) {}
                }
                // Fallback: scan all static fields and identify by generic type
                for (Field f : dc.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    String typeName = f.getGenericType().getTypeName();
                    if (typeName.contains("CustomData") && typeName.contains("DataComponentType")) {
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val != null) {
                            cachedCustomDataType = val;
                            return val;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Gets the Class to use as parameter type when calling nmsItem.get(DataComponentType).
     * Finds the DataComponentType interface/class from the component type object itself.
     */
    private static Class<?> getComponentTypeClass(Object componentType) {
        if (cachedComponentTypeClass != null) return cachedComponentTypeClass;

        // Look for a parent class or interface named DataComponentType
        for (Class<?> iface : componentType.getClass().getInterfaces()) {
            if (iface.getSimpleName().contains("DataComponentType")) {
                cachedComponentTypeClass = iface;
                return iface;
            }
        }
        Class<?> superClass = componentType.getClass();
        while (superClass != null && superClass != Object.class) {
            if (superClass.getSimpleName().contains("DataComponentType")) {
                cachedComponentTypeClass = superClass;
                return superClass;
            }
            superClass = superClass.getSuperclass();
        }
        // Fallback: try by known class name
        try {
            Class<?> c = Class.forName("net.minecraft.core.component.DataComponentType");
            cachedComponentTypeClass = c;
            return c;
        } catch (Exception ignored) {}

        // Last resort: use the object's own class
        cachedComponentTypeClass = componentType.getClass();
        return cachedComponentTypeClass;
    }

    private boolean tagContains(Object tag, String key) throws Exception {
        // Try standard names first, then scan by signature (String->boolean) for obfuscated builds
        for (String name : new String[]{"contains", "hasKey", "e"}) {
            try {
                Method m = tag.getClass().getMethod(name, String.class);
                if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                    return (boolean) m.invoke(tag, key);
                }
            } catch (NoSuchMethodException ignored) {}
        }
        // Fallback: scan all methods with (String) -> boolean
        for (Method m : tag.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
            try { return (boolean) m.invoke(tag, key); } catch (Exception ignored) {}
        }
        throw new Exception("[tagContains] No se encontró método contains/hasKey en " + tag.getClass().getSimpleName() +
            ". Claves del tag desconocidas.");
    }

    private int tagGetInt(Object tag, String key) throws Exception {
        // 1.20.5+ uses getInt(String, int default); older used getInt(String)
        for (String name : new String[]{"getInt", "i", "d", "e"}) {
            // Try two-param version first (newer API)
            try {
                Method m = tag.getClass().getMethod(name, String.class, int.class);
                if (m.getReturnType() == int.class || m.getReturnType() == Integer.class) {
                    return (int) m.invoke(tag, key, 0);
                }
            } catch (NoSuchMethodException ignored) {}
            // Try one-param version (older API)
            try {
                Method m = tag.getClass().getMethod(name, String.class);
                if (m.getReturnType() == int.class || m.getReturnType() == Integer.class) {
                    return (int) m.invoke(tag, key);
                }
            } catch (NoSuchMethodException ignored) {}
        }
        // Scan by signature: (String, int) -> int
        for (Method m : tag.getClass().getMethods()) {
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 2 && params[0] == String.class && params[1] == int.class) {
                try { return (int) m.invoke(tag, key, 0); } catch (Exception ignored) {}
            }
            if (params.length == 1 && params[0] == String.class) {
                try { return (int) m.invoke(tag, key); } catch (Exception ignored) {}
            }
        }
        throw new Exception("[tagGetInt] No se encontró método getInt en " + tag.getClass().getSimpleName());
    }

    private byte[] tagGetByteArray(Object tag, String key) {
        // Scan all methods (public + non-public) in the class hierarchy that return byte[]
        Class<?> cls = tag.getClass();
        while (cls != null && cls != Object.class) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getReturnType() != byte[].class) continue;
                m.setAccessible(true);
                Class<?>[] params = m.getParameterTypes();
                try {
                    if (params.length == 1 && params[0] == String.class) {
                        return (byte[]) m.invoke(tag, key);
                    } else if (params.length == 2 && params[0] == String.class) {
                        if (params[1] == byte[].class)   return (byte[]) m.invoke(tag, key, new byte[0]);
                        if (params[1] == int.class)      return (byte[]) m.invoke(tag, key, 0);
                    }
                } catch (Exception ignored) {}
            }
            cls = cls.getSuperclass();
        }
        // Could not read byte array data — caller will migrate type only (inventory lost)
        Backpacks.getInstance().getLogger().warning(
            "[CMigrateBackpack] No se pudo leer backpack_data de " + tag.getClass().getSimpleName() +
            ". El contenido del backpack no será migrado.");
        return null;
    }
}
