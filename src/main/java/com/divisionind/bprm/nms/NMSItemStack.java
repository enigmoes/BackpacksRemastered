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

import com.divisionind.bprm.nms.reflect.NBTType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;

public class NMSItemStack extends NBTMap {

    private final ItemStack item;
    private final ItemMeta meta;

    public NMSItemStack(ItemStack item)
            throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        this(item, item.getItemMeta());
    }

    private NMSItemStack(ItemStack item, ItemMeta meta)
            throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        super(meta != null ? meta.getPersistentDataContainer() : null);
        this.item = item;
        this.meta = meta;
    }

    public ItemStack getModifiedItem() throws InvocationTargetException, IllegalAccessException {
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getItem() {
        return item;
    }

    public static ItemStack setNBTOnce(ItemStack item, NBTType type, String key, Object value)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        NMSItemStack nmsItem = new NMSItemStack(item);
        nmsItem.setNBT(type, key, value);
        return nmsItem.getModifiedItem();
    }
}
