package com.github.arboriginal.LavaSponge;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.collect.Sets;

public class Plugin extends JavaPlugin implements Listener {
    private final Set<Material> transparents = Sets.newHashSet(Material.AIR, Material.CAVE_AIR);
    private final BlockFace[]   adjacents    = { BlockFace.DOWN, BlockFace.UP,
            BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST };

    private boolean      ezLava, ezWater, notInL, notInW, wetOnly;
    private List<String> dryOuts;
    private Tombola      flowing, source, sponge;

    // JavaPlugin methods ----------------------------------------------------------------------------------------------

    @Override
    public void onEnable() {
        super.onEnable();
        reloadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ls-reload")) {
            reloadConfig();
            sender.sendMessage("§7[§6LavaSponge§7] §aConfiguration reloaded.");
            return true;
        }

        return super.onCommand(sender, command, label, args);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        FileConfiguration conf = getConfig(); // @formatter:off
        conf.getConfigurationSection("result").getKeys(false).forEach(k -> { conf.addDefault("result." + k, null); });
        conf.options().copyDefaults(true); // @formatter:on
        saveConfig();

        dryOuts = conf.getStringList("dryOutWorlds");
        ezLava  = conf.getBoolean("easierPlacement.lava");
        ezWater = conf.getBoolean("easierPlacement.water");
        flowing = weightsParse(conf, "result.flowing");
        notInL  = conf.getBoolean("notSoEasierPlacement.lava");
        notInW  = conf.getBoolean("notSoEasierPlacement.water");
        source  = weightsParse(conf, "result.source");
        sponge  = weightsParse(conf, "result.sponge");
        wetOnly = conf.getBoolean("wetOnly");
    }

    // Listener methods ------------------------------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("ls.use")) return;

        Block   block = e.getBlock();
        boolean isWet = (block.getType() == Material.WET_SPONGE);
        if (!isWet && (wetOnly || block.getType() != Material.SPONGE)) return;

        boolean lava = false; // @formatter:off
        for (BlockFace f : adjacents) if (block.getRelative(f).getType() == Material.LAVA) { lava = true; break; }
        // @formatter:on
        if (!lava) {
            if (isWet && dryOuts.contains(block.getWorld().getName())) block.setType(Material.SPONGE);
            return;
        }
        Location oL = block.getLocation();
        World    oW = oL.getWorld();

        for (int x = oL.getBlockX() - 3; x <= oL.getBlockX() + 3; x++)
            for (int y = oL.getBlockY() - 3; y <= oL.getBlockY() + 3; y++)
                for (int z = oL.getBlockZ() - 3; z <= oL.getBlockZ() + 3; z++) {
                    Block b = oW.getBlockAt(x, y, z);
                    if (b.getType() != Material.LAVA || b.getLocation().distanceSquared(oL) > 9) continue;
                    b.setType((((Levelled) b.getBlockData()).getLevel() == 0) ? source.pick() : flowing.pick());
                }
        block.setType(sponge.pick());
    }

    @EventHandler(ignoreCancelled = false)
    private void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null || e.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player bob = e.getPlayer();
        if (!bob.hasPermission("ls.use") || (!ezLava && !ezWater)) return;

        Block block = bob.getLocation().getBlock();
        if ((notInL && block.getType() == Material.LAVA) || (notInW && block.getType() == Material.WATER)) return;

        EquipmentSlot slot = e.getHand();
        ItemStack     item;

        switch (slot) { // @formatter:off
            case     HAND: item = bob.getInventory().getItemInMainHand(); break;
            case OFF_HAND: item = bob.getInventory().getItemInOffHand();  break;
            default: return; } // @formatter:on

        boolean isWet = (item.getType() == Material.WET_SPONGE); // @formatter:off
        if (   !isWet && item.getType() != Material.SPONGE) return; // @formatter:on

        for (Block b : e.getPlayer().getLineOfSight(transparents, 10)) {
            switch (b.getType()) { // @formatter:off
                case LAVA:  if (!ezLava  || (!isWet && wetOnly)) continue; break;
                case WATER: if (!ezWater ||   isWet)             continue; break;
                default: continue; } // @formatter:on

            BlockState bs = b.getState();
            b.setType(item.getType());

            BlockPlaceEvent event = new BlockPlaceEvent(b, bs, b.getRelative(BlockFace.DOWN), item, bob, false, slot);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) b.setBlockData(bs.getBlockData());
            else if (bob.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);

            return;
        }
    }

    // Private methods -------------------------------------------------------------------------------------------------

    private Material readMaterial(String input) {
        Material type = Material.getMaterial(input);
        if (type == null || !type.isBlock()) Bukkit.getLogger().warning("Invalid block material: §b" + input);
        return type;
    }

    private Tombola weightsParse(FileConfiguration config, String key) {
        Tombola tombola = weightsParse(config.getConfigurationSection(key));

        if (tombola == null) {
            Bukkit.getLogger().warning("Invalid configuration §b" + key + "§r, fallback to defaults.");
            tombola = weightsParse(config.getDefaults().getConfigurationSection(key));
        }

        return tombola;
    }

    private Tombola weightsParse(ConfigurationSection section) {
        Set<String> keys = section.getKeys(false);

        if (keys.size() == 1) {
            Material dictator = readMaterial(keys.toArray(new String[] {})[0]);
            return (dictator == null) ? null : new Single(dictator);
        }

        WRandom weights = new WRandom();
        for (String k : keys) {
            Material pedro = readMaterial(k);
            if (pedro == null) return null;
            weights.vote(pedro, section.getInt(k));
        }

        return weights;
    }

    // Private classes ---------------------------------------------------------------------------------- @formatter:off

    private interface Tombola { Material pick(); }

    private class Single implements Tombola {
        private final Material type; Single(Material type) { this.type = type; } public Material pick() { return type; }
    }

    private class WRandom implements Tombola {
        private TreeMap<Integer, Material> papers = new TreeMap<Integer, Material>();
        private Random random = new Random(); int voters = 1;
        public void vote(Material item, int count) { voters += count; papers.put(voters, item); }
        public Material pick() { return papers.ceilingEntry(random.nextInt(voters)).getValue(); }      // @formatter:on
    }
}
