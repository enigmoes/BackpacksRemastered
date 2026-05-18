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

package com.divisionind.bprm.events;

import com.divisionind.bprm.ACommand;
import com.divisionind.bprm.BackpackObject;
import com.divisionind.bprm.Backpacks;
import com.divisionind.bprm.PotentialBackpackItem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class BackpackCraftEvent implements Listener {

    @EventHandler
    public void onCraftEvent(CraftItemEvent e) {
        if (e.isCancelled()) return;

        try {
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            HumanEntity ent = e.getWhoClicked();
            PotentialBackpackItem backpackItem = new PotentialBackpackItem(item);

            if (!backpackItem.isBackpack()) return;
            int backpack_type = backpackItem.getType();
            BackpackObject backpack = BackpackObject.getByType(backpack_type);
            if (backpack == null || !backpack.hasCraftPermission(ent)) {
                ent.sendMessage(Backpacks.translate(
                        String.format("&cYou do not have permission to craft the %s backpack.",
                                backpack == null ? "null" : backpack.name().toLowerCase())));
                e.setCancelled(true);
                return;
            }

            ACommand.respondf(ent, "&eYou just crafted a %s backpack.", backpack.name().toLowerCase());
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException
                | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
}
