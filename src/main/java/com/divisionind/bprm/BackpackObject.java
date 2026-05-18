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

package com.divisionind.bprm;

import com.divisionind.bprm.backpacks.*;
import com.divisionind.bprm.nms.NMSItemStack;
import com.divisionind.bprm.nms.reflect.NBTType;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public enum BackpackObject {

    //                                                                    armor  toughness
    SMALL     (Color.fromRGB(101, 67, 33),  0, new BPSmall(),      2.0,  0.0), // leather ref=3
    MEDIUM    (Color.fromRGB(140, 140, 140),1, new BPMedium(),     4.0,  0.0), // copper  ref~5
    LARGE     (Color.fromRGB(0, 120, 200),  2, new BPLarge(),      5.0,  0.0), // iron    ref=6
    EXTRALARGE(Color.fromRGB(40, 40, 40),   3, new BPExtraLarge(), 7.0,  1.0), // diamond ref=8/t2
    LINKED    (Color.BLUE,                  4, new BPLinked(),     5.0,  0.0), // iron-diamond mix
    ENDER     (Color.GREEN,                 5, new BPEnder(),      3.0,  0.0); // leather base

    private ItemStack item;
    private List<String> lore;

    private final Color color;
    private final int type;
    private final String permission;
    private final BackpackHandler handler;
    private final double armor;
    private final double toughness;

    BackpackObject(Color color, int type, BackpackHandler handler, double armor, double toughness) {
        this.color = color;
        this.type = type;
        this.permission = "backpacks.craft." + name().toLowerCase();
        this.handler = handler;
        this.armor = armor;
        this.toughness = toughness;
    }

    void init(String name, List<String> lore) {
        this.lore = lore;
        this.item = getBackpack(color, type, name, lore, armor, toughness);
    }

    public ItemStack getItem() {
        return item;
    }

    public int getTypeId() {
        return type;
    }

    public boolean hasCraftPermission(HumanEntity entity) {
        return entity.hasPermission(permission);
    }

    public BackpackHandler getHandler() {
        return handler;
    }

    public List<String> getLore() {
        return lore;
    }

    public static BackpackObject getByType(int type) {
        for (BackpackObject bp : values()) {
            if (bp.type == type) return bp;
        }
        return null;
    }

    public static BackpackObject getByName(String name) {
        for (BackpackObject item : values()) {
            if (item.name().equalsIgnoreCase(name)) return item;
        }
        return null;
    }

    private static ItemStack getBackpack(Color color, int type, String name, List<String> lore, double armor, double toughness) {
        ItemStack backpack = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) backpack.getItemMeta();
        meta.setColor(color);
        meta.setDisplayName(Backpacks.translate(name));
        meta.setLore(lore);
        // Set armor value (replaces leather chestplate default of 3)
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
            UUID.nameUUIDFromBytes(("backpack_armor_" + type).getBytes(StandardCharsets.UTF_8)),
            "backpack_armor", armor, Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        if (toughness > 0.0) {
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.nameUUIDFromBytes(("backpack_toughness_" + type).getBytes(StandardCharsets.UTF_8)),
                "backpack_toughness", toughness, Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        }
        backpack.setItemMeta(meta);

        // apply backpack_type nbt data
        try {
            return NMSItemStack.setNBTOnce(backpack, NBTType.INT, PotentialBackpackItem.FIELD_NAME_TYPE, type);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException
                | NoSuchMethodException e) {
            return null;
        }
    }
}
