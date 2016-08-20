package org.steveshipway.dynmap;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.dynmap.markers.PolyLineMarker;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.utils.WorldManager;

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
        	deficon = "star";
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
    	MarkerIcon ico,wico;
        Plugin mvplugin;
        List<World> worlds = null;
    	
        markerapi = api.getMarkerAPI();
        if(markerapi == null) {
            crit("Error loading dynmap marker API!");
            return;
        }

        /* get multiverse */
        mvplugin = getServer().getPluginManager().getPlugin("Multiverse-Core"); 
        if (mvplugin instanceof MultiverseCore) {
            /* the multiverse is (MultiverseCore)mvplugin  */
        	log.info("Multiverse detected: identifying all the worlds using getMVWorlds()");
            MVWorldManager wm = ((MultiverseCore)mvplugin).getMVWorldManager();
            worlds = new ArrayList<World>();
            for( MultiverseWorld mvworld : wm.getMVWorlds() ) {
                worlds.add(mvworld.getCBWorld());
            }
        } else {
        	log.info("Multiverse NOT FOUND - using standard Bukkit API");
            worlds = Bukkit.getWorlds() ;
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

        if( cfg.getBoolean("dungeons.enabled", true ) ) {
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
		        for( World world: worlds ) {
		        	log.info("Processing world: " + world.getName());
			        List<Dungeon> dungeons = Dungeon.getDungeons(world, world.getWorldFolder().getPath() + File.separator + "mcdungeon_cache" + File.separator + "dungeon_scan_cache",log);
			        if( dungeons == null ) {
			        	log.warning("No dungeons found in world "+world.getName());
			        } else {
				        for( Dungeon dungeon: dungeons ) {
					        Location blk = dungeon.getLocation();
				            Marker dmkr = set.createMarker(dungeon.getId(), dungeon.getName(), true, world.getName(), 
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
        }


        if( cfg.getBoolean("thunts.enabled", true ) ) {
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
            ico = getIcon("thunts");
        } else { ico = null; }
        
        if( cfg.getBoolean("waypoints.enabled", true ) ) {
        	/* Waypoints */
	        wset = markerapi.getMarkerSet("waypoints.markerset");
	        if(wset == null)
	            wset = markerapi.createMarkerSet("waypoints.markerset", cfg.getString("waypoints.name", "Waypoints"), null, false);
	        else
	            wset.setMarkerSetLabel(cfg.getString("waypoints.name", "Waypoints"));
	        if(wset == null) {
	            crit("Error creating treasure hunt waypoints marker set");
	            return;
	        }
	        minzoom = cfg.getInt("waypoints.minzoom", 0);
	        if(minzoom > 0)
	            wset.setMinZoom(minzoom);
	        wset.setLayerPriority(cfg.getInt("waypoints.layerprio", 11));
	        wset.setHideByDefault(cfg.getBoolean("waypoints.hidebydefault", true));
    	    wico = getIcon("waypoints");
        } else { wico = null; }

        if( (ico!=null) || (wico!=null) ) {

	        for( World world: worlds ) {
	        	log.info("Processing world: " + world.getName());
		        List<Dungeon> thunts = Dungeon.getDungeons(world, world.getWorldFolder().getPath() + File.separator + "mcdungeon_cache" + File.separator + "thunt_scan_cache",log);
		        if( thunts == null ) {
		        	log.warning("No Treasure Hunts found in world "+world.getName());
		        } else {
			        for( Dungeon thunt: thunts ) {
			        	if( ico!=null) {
			        		/* add treasure hunt */
				            Marker dmkr = tset.createMarker(thunt.getId(), thunt.getName(), true, world.getName(), 
							        	thunt.getWaypoints().get(0).get("x"),
							        	thunt.getWaypoints().get(0).get("y"),
							        	thunt.getWaypoints().get(0).get("z"),
				                        ico, false);
				            if(dmkr != null) {
				            	String desc = thunt.getTHDescription();
				            	dmkr.setDescription(desc); /* Set popup */
				            	dmkr.setLabel(thunt.getName());
				            }
			        	}
			        	if(wico!=null) {
			        		/* add waypoints and line */
			        		int i = 0;
				            double xarr[] = new double[thunt.getLevels()];
				            double yarr[] = new double[thunt.getLevels()];
				            double zarr[] = new double[thunt.getLevels()];

			        		for( Map<String,Integer> m: thunt.getWaypoints() ) {
					            xarr[i] = m.get("x");
					            yarr[i] = m.get("y");
					            zarr[i] = m.get("z");
			        			if( i > 0 ) {
						            Marker dmkr = wset.createMarker(thunt.getId()+"_"+i, thunt.getName()+"<BR>\nStep "+i, true, world.getName(), 
					                    m.get("x"),m.get("y"),m.get("z"), wico, false);
						            if(dmkr != null) {
						            //	String desc = thunt.getTHDescription();
						            //	dmkr.setDescription(desc); /* Set popup */
						            	dmkr.setLabel("Step "+i);
						            }
			        			}
					            i += 1;
			        		}
				            PolyLineMarker pmkr = wset.createPolyLineMarker(thunt.getId()+"_line", "", true, world.getName(), 
			            		xarr,yarr,zarr,
			            		false);
				            if( pmkr == null ) {
				            	log.warning("Error creating polyline marker: "+thunt.getLevels()+" steps");
				            }
			        	}
			        }
		        }
	        }
        }
        
        /* Finished! */
        info("Dynmap-MCDungeon version " + this.getDescription().getVersion() + " is activated");
    }
        
}
