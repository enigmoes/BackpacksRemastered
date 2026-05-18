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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A wrapper for the player class that resolves online players.
 * Offline player inventory access is not supported (requires NMS, incompatible with 1.21+).
 */
public class AlwaysPlayer {

    private final UUID playerId;

    public AlwaysPlayer(UUID playerId) {
        this.playerId = playerId;
    }

    public Player resolvePlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public void safeSave() {
        // no-op: only online players are supported, Bukkit handles saving automatically
    }

    public String getName() {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) return player.getName();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        String name = offlinePlayer.getName();
        return name != null ? name : "unknown";
    }
}
