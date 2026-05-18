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

package com.divisionind.bprm.nms;

import com.divisionind.bprm.Backpacks;
import com.divisionind.bprm.nms.reflect.NBTType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;

public class NBTMap {

    protected PersistentDataContainer pdc;

    public NBTMap(PersistentDataContainer pdc) {
        this.pdc = pdc;
    }

    protected NamespacedKey key(String name) {
        return new NamespacedKey(Backpacks.getInstance(), name);
    }

    public void setNBT(NBTType type, String keyStr, Object value) {
        if (pdc == null) throw new IllegalStateException("Cannot set NBT: item has no ItemMeta (pdc is null)");
        NamespacedKey k = key(keyStr);
        switch (type) {
            case INT:        pdc.set(k, PersistentDataType.INTEGER,       (Integer) value); break;
            case BYTE_ARRAY: pdc.set(k, PersistentDataType.BYTE_ARRAY,   (byte[])  value); break;
            case STRING:     pdc.set(k, PersistentDataType.STRING,        (String)  value); break;
            case BOOLEAN:    pdc.set(k, PersistentDataType.BYTE,          ((Boolean) value) ? (byte) 1 : (byte) 0); break;
            case LONG:       pdc.set(k, PersistentDataType.LONG,          (Long)    value); break;
            case DOUBLE:     pdc.set(k, PersistentDataType.DOUBLE,        (Double)  value); break;
            case FLOAT:      pdc.set(k, PersistentDataType.FLOAT,         (Float)   value); break;
            case SHORT:      pdc.set(k, PersistentDataType.SHORT,         (Short)   value); break;
            case BYTE:       pdc.set(k, PersistentDataType.BYTE,          (Byte)    value); break;
            case INT_ARRAY:  pdc.set(k, PersistentDataType.INTEGER_ARRAY, (int[])   value); break;
            case COMPOUND:   pdc.set(k, PersistentDataType.TAG_CONTAINER, (PersistentDataContainer) value); break;
            default: throw new IllegalArgumentException("Unsupported NBTType: " + type);
        }
    }

    public Object getNBT(NBTType type, String keyStr) {
        if (pdc == null) return null;
        NamespacedKey k = key(keyStr);
        switch (type) {
            case INT:        return pdc.get(k, PersistentDataType.INTEGER);
            case BYTE_ARRAY: return pdc.get(k, PersistentDataType.BYTE_ARRAY);
            case STRING:     return pdc.get(k, PersistentDataType.STRING);
            case BOOLEAN: {
                Byte b = pdc.get(k, PersistentDataType.BYTE);
                return b != null && b != 0;
            }
            case LONG:       return pdc.get(k, PersistentDataType.LONG);
            case DOUBLE:     return pdc.get(k, PersistentDataType.DOUBLE);
            case FLOAT:      return pdc.get(k, PersistentDataType.FLOAT);
            case SHORT:      return pdc.get(k, PersistentDataType.SHORT);
            case BYTE:       return pdc.get(k, PersistentDataType.BYTE);
            case INT_ARRAY:  return pdc.get(k, PersistentDataType.INTEGER_ARRAY);
            case COMPOUND:   return pdc.get(k, PersistentDataType.TAG_CONTAINER);
            default: throw new IllegalArgumentException("Unsupported NBTType: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getNBT(Class<T> clazz, String key) {
        return (T) getNBT(NBTType.getByClass(clazz), key);
    }

    public void removeNBT(String keyStr) {
        pdc.remove(key(keyStr));
    }

    public void setAsMap(String keyStr, NBTMap value) {
        pdc.set(key(keyStr), PersistentDataType.TAG_CONTAINER, value.pdc);
    }

    public NBTMap getAsMap(String keyStr) {
        PersistentDataContainer child = pdc.get(key(keyStr), PersistentDataType.TAG_CONTAINER);
        return child != null ? new NBTMap(child) : null;
    }

    public boolean hasNBT(String keyStr) {
        if (pdc == null) return false;
        return pdc.getKeys().contains(key(keyStr));
    }

    public Set<String> getKeys() {
        Set<String> result = new HashSet<>();
        if (pdc == null) return result;
        String ns = Backpacks.getInstance().getName().toLowerCase();
        for (NamespacedKey k : pdc.getKeys()) {
            if (k.getNamespace().equals(ns)) {
                result.add(k.getKey());
            }
        }
        return result;
    }

    public byte getKeyInternalTypeId(String keyStr) {
        NamespacedKey k = key(keyStr);
        if (pdc.has(k, PersistentDataType.INTEGER))       return NBTType.INT.getInternalId();
        if (pdc.has(k, PersistentDataType.BYTE_ARRAY))    return NBTType.BYTE_ARRAY.getInternalId();
        if (pdc.has(k, PersistentDataType.STRING))        return NBTType.STRING.getInternalId();
        if (pdc.has(k, PersistentDataType.LONG))          return NBTType.LONG.getInternalId();
        if (pdc.has(k, PersistentDataType.DOUBLE))        return NBTType.DOUBLE.getInternalId();
        if (pdc.has(k, PersistentDataType.FLOAT))         return NBTType.FLOAT.getInternalId();
        if (pdc.has(k, PersistentDataType.SHORT))         return NBTType.SHORT.getInternalId();
        if (pdc.has(k, PersistentDataType.BYTE))          return NBTType.BYTE.getInternalId();
        if (pdc.has(k, PersistentDataType.INTEGER_ARRAY)) return NBTType.INT_ARRAY.getInternalId();
        if (pdc.has(k, PersistentDataType.TAG_CONTAINER)) return NBTType.COMPOUND.getInternalId();
        return (byte) 0;
    }

    public Object getTagCompound() {
        return pdc;
    }
}
