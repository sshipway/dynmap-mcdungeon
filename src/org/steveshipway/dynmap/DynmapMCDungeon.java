package org.steveshipway.dynmap;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
//import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

public class DynmapMCDungeon extends JavaPlugin {
    private static Logger log;
    Plugin dynmap;
    DynmapAPI api;
    MarkerAPI markerapi;
    FileConfiguration cfg;
    MarkerSet set, tset, wset;
    boolean reload = false;
    
    // for logging
    public static void info(String msg) {
        log.log(Level.INFO, msg);
    }
    public static void crit(String msg) {
        log.log(Level.SEVERE, msg);
    }
    
    // Initial loading
    @Override
    public void onLoad() {
        log = this.getLogger();
    }
	
	// Fired when plugin is first enabled
    @Override
    public void onEnable() {
        info("initializing");
        PluginManager pm = getServer().getPluginManager();
        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");
        if(dynmap == null) {
            crit("Cannot find dynmap!");
            return;
        }
        api = (DynmapAPI)dynmap; /* Get API */
        info("detected Dynmap API version " + api.getDynmapVersion() );
        
        getServer().getPluginManager().registerEvents(new OurServerListener(), this);
        /* If both enabled, activate */
        if(dynmap.isEnabled()) {
            activate();
        } else {
        	info("waiting for Dynmap to activate");
        }

    }
    // Fired when plugin is disabled: clean up markers
    @Override
    public void onDisable() {
        if(set != null) {
            set.deleteMarkerSet();
            set = null;
        }
        if(tset != null) {
            tset.deleteMarkerSet();
            tset = null;
        }
        if(wset != null) {
            wset.deleteMarkerSet();
            wset = null;
        }
    }
    
    // listen for dynmap startup, if it happens after
    private class OurServerListener implements Listener {
        @EventHandler(priority=EventPriority.MONITOR)
        public void onPluginEnable(PluginEnableEvent event) {
            Plugin p = event.getPlugin();
            String name = p.getDescription().getName();
            if(name.equals("dynmap")) {
                if(dynmap.isEnabled()) {
                    activate();
                }
            }
        }
    }
    
    // Determine what our Icon will be
    private MarkerIcon getIcon(String itype) {
    	String deficon;
        String marker;
        MarkerIcon icon = null;

    	if( itype == "thunts" ) {
        	deficon = "chest";
    	} else if( itype == "waypoints" ) {
        	deficon = "walk";
    	} else {
        	deficon = "tower";
    	}
        marker = cfg.getString(itype+".icon", deficon);
        icon = markerapi.getMarkerIcon(marker);
        if(icon == null) {
            crit("Invalid icon: " + marker);
        }
        return icon;
    }
    
    // Start the plugin, populate the markers
    private void activate() {
    	int minzoom;
    	MarkerIcon ico;
    	
        markerapi = api.getMarkerAPI();
        if(markerapi == null) {
            crit("Error loading dynmap marker API!");
            return;
        }
        
        /* Load configuration */
        if(reload) {
            this.reloadConfig();
            if(set != null) {
                set.deleteMarkerSet();
                set = null;
            }
            if(tset != null) {
                tset.deleteMarkerSet();
                tset = null;
            }
            if(wset != null) {
                wset.deleteMarkerSet();
                wset = null;
            }
        }
        else {
            reload = true;
        }
        cfg = getConfig();
        cfg.options().copyDefaults(true);   /* Load defaults, if needed */
        this.saveConfig();  /* Save updates, if needed */

        /* Now, add marker set for dungeons (make it transient) */
        set = markerapi.getMarkerSet("dungeons.markerset");
        if(set == null)
            set = markerapi.createMarkerSet("dungeons.markerset", cfg.getString("dungeons.name", "Dungeons"), null, false);
        else
            set.setMarkerSetLabel(cfg.getString("dungeons.name", "Dungeons"));
        if(set == null) {
            crit("Error creating dungeons marker set");
            return;
        }
        minzoom = cfg.getInt("dungeons.minzoom", 0);
        if(minzoom > 0)
            set.setMinZoom(minzoom);
        set.setLayerPriority(cfg.getInt("dungeons.layerprio", 10));
        set.setHideByDefault(cfg.getBoolean("dungeons.hidebydefault", false));
 
        /* Now retrieve the dungeons list and create markers */
        ico = getIcon("dungeons");
        if(ico != null) {
	        for( World world: Bukkit.getWorlds() ) {
	        	log.info("Processing world: " + world.getName());
		        List<Dungeon> dungeons = Dungeon.getDungeons(world, world.getWorldFolder().getPath() + File.separator + "mcdungeon_cache" + File.separator + "dungeon_scan_cache",log);
		        if( dungeons == null ) {
		        	log.warning("No dungeons found in world "+world.getName());
		        } else {
			        for( Dungeon dungeon: dungeons ) {
				        String markid = "_mcd_" + dungeon.getName();
				        Location blk = dungeon.getLocation();
			            Marker dmkr = set.createMarker(markid, dungeon.getId(), world.getName(), 
			                        blk.getX(), blk.getY(), blk.getZ(), ico, false);
			            if(dmkr != null) {
			            	String desc = dungeon.getDescription();
			            	dmkr.setDescription(desc); /* Set popup */
			            	dmkr.setLabel(dungeon.getName());
			            }
			            
			        }
		        }
	        }
        }

        /* Treasure Hunts */
        tset = markerapi.getMarkerSet("thunts.markerset");
        if(tset == null)
            tset = markerapi.createMarkerSet("thunts.markerset", cfg.getString("thunts.name", "Treasure Hunts"), null, false);
        else
            tset.setMarkerSetLabel(cfg.getString("thunts.name", "Treasure Hunts"));
        if(tset == null) {
            crit("Error creating treasure hunts marker set");
            return;
        }
        minzoom = cfg.getInt("thunts.minzoom", 0);
        if(minzoom > 0)
            tset.setMinZoom(minzoom);
        tset.setLayerPriority(cfg.getInt("thunts.layerprio", 10));
        tset.setHideByDefault(cfg.getBoolean("thunts.hidebydefault", false));
        
        /* Waypoints */
        
        
        /* Finished! */
        info("Dynmap-MCDungeon version " + this.getDescription().getVersion() + " is activated");
    }
        
}
