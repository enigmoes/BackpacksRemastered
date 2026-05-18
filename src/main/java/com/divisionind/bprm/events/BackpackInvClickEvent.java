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

import com.divisionind.bprm.BackpackRecipes;
import com.divisionind.bprm.FakeBackpackViewer;
import com.divisionind.bprm.PotentialBackpackItem;
import com.divisionind.bprm.nms.reflect.NMS;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class BackpackInvClickEvent implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        FakeBackpackViewer viewer = NMS.getBackpackViewer(e.getInventory());
        if (viewer == null)
            return;

        ItemStack clicked = e.getCurrentItem();

        if (e.getClick().equals(ClickType.NUMBER_KEY)) {
            clicked = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
        }

        if (clicked != null && BackpackRecipes.backpackKey.equals(clicked)) {
            e.setCancelled(true);
            return;
        }

        try {
            if (clicked != null) {
                PotentialBackpackItem clickedBackpack = new PotentialBackpackItem(clicked);
                if (clickedBackpack.isBackpack() && !e.getWhoClicked().hasPermission("backpacks.nest"))
                    e.setCancelled(true);
            }
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException
                | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
}
