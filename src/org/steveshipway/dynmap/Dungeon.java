package org.steveshipway.dynmap;

//import java.util.ArrayList;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;

public class Dungeon {
	private String name = "unknown";
	private Location loc = null;
	private int sizex;
	private int sizez;
	private int levels;
	private ArrayList<Map<String,Integer>> waypoints;
	
	// Static function to read the MCDungeon cache and return a list of Dungeon objects.
	//@SuppressWarnings("unchecked")
	@SuppressWarnings({ "unchecked" })
	public static List<Dungeon> getDungeons(World w, String pathname, Logger log) {
		List<Dungeon> dlst = new ArrayList<Dungeon>();
		
		File f = new File(pathname );
		if( ! f.exists() ) {
			log.warning("- MCDungeon Dungeon cache file missing!");
			return null; 
		}
		
		log.info("- Loading in MCDungeon cache file from " + pathname);
		
        YamlLoader loader = new YamlLoader();
        HashMap<String, Object> loaded = loader.getYamlFromFile(f,log);
        
        if( loaded == null ) {
        	log.warning("-- Unable to read MCDungeon cache file.  Maybe the version is too old?  Need v7 cache or later with YAML format (v1.10+)");
        	return null;
        }
        

        //Type type = new TypeToken<Object>() {}.getType();
        //String json = gson.toJson(loaded, type);
        //log.info(json);

        for( Object d: loaded.values() ) { // These should all be HashMaps
        	String dname=null;
        	int dx=0,dz=0,dlevels=0;
        	int eh = 0;
        	int x,y,z;

    		ArrayList<Map<String,Integer>> wp = null;

    		if( ! d.getClass().getSimpleName().endsWith("HashMap") ) {
    			log.warning("-- Not a HashMap object in the cache: Probably the YAML failed");
    			continue;
    		}
    		if( ! ((HashMap<String,String>)d).containsKey("full_name") ) {
        		log.warning("-- This dungeon cache entry does not look correct!");
        		continue;        		
        	}
        	// log.info("Processing a dungeon entry...");

        	try {
				if( ((HashMap<String,Object>)d).containsKey("full_name") ) {
					dname = ((HashMap<String,String>)d).get("full_name");
				}
				if( ((HashMap<String,Object>)d).containsKey("levels") ) { // for dungeons
					dlevels = ((HashMap<String,Integer>)d).get("levels");
				}
				if( ((HashMap<String,Object>)d).containsKey("steps") ) { // for thunts
					dlevels = ((HashMap<String,Integer>)d).get("steps");
				}
			    if( ((HashMap<String,Object>)d).containsKey("xsize") ) {
					dx = ((HashMap<String,Integer>)d).get("xsize");
				}
				if( ((HashMap<String,Object>)d).containsKey("zsize") ) {
					dz = ((HashMap<String,Integer>)d).get("zsize");
				}
				if( ((HashMap<String,Object>)d).containsKey("entrace_height") ) {
					eh = ((HashMap<String,Integer>)d).get("entrace_height");
				}
        	} catch( Exception e ) {
        		log.warning("-- Problem parsing cache content (A): "+e.getMessage());
        		continue;
        	}

			try {

				x = ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("position"))).get("x");
				y = ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("position"))).get("y");
				z = ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("position"))).get("z");
				
				//log.info("Dungeon "+dname+" at "+x+","+y+","+z);
				
				if( ((HashMap<String,Object>)d).containsKey("portal_exit") ) {
					x += ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("portal_exit"))).get("x");
					y -= ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("portal_exit"))).get("y");
					z += ((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("portal_exit"))).get("z");
				} else if( ((HashMap<String,Object>)d).containsKey("entrance_pos") ) {
					x += (((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("entrace_pos"))).get("x") << 4) + 8;				
					y += eh;
					z += (((HashMap<String,Integer>)(((HashMap<String,Object>)d).get("entrace_pos"))).get("z") << 4) + 8;				
				} else {
//					log.warning("Unable to identify portal exit or entrance position for dungeon '"+dname+"'");
					// treasure hunt
					x += 8; z += 8; // middle of chunk
				}
        	} catch( Exception e ) {
        		log.warning("-- Problem parsing cache content (B): " + e.getMessage());
        		continue;
        	}

			try {
				if( ((HashMap<String,Object>)d).containsKey("landmarks") ) {
					wp = loader.getWaypoints( (String) ((HashMap<String,Object>)d).get("landmarks") ,log);
				}
			} catch( Exception e ) {
        		log.warning("-- Problem parsing waypoints content: "+e.getClass().getSimpleName());
        		e.printStackTrace();
        		log.info(  ((HashMap<String,Object>)d).get("landmarks").toString());
			}
			
        	if( (dlevels > 0) && (dname != null) ) {
        		dlst.add(new Dungeon(dname,w, x,y,z ,dx,dz,dlevels,wp));
        	} else {
        		log.warning("-- Invalid dungeon cache ignored!");
        	}
        }
        
		return dlst;
	}
	
	Dungeon(String n, Location l, int x, int z, int lvl) {
		name = n;
		loc = l;
		sizex = x;
		sizez = z;
		setLevels(lvl);
		waypoints = null;
	}
	Dungeon(String n, World w, int x, int y, int z, int sx, int sz, int lvl) {
		name = n;
		loc = new Location(w, x, y, z);
		sizex = sx;
		sizez = sz;
		setLevels(lvl);
		waypoints = null;
	}
	Dungeon(String n, World w, int x, int y, int z, int sx, int sz, int lvl,ArrayList<Map<String,Integer>> wp) {
		name = n;
		loc = new Location(w, x, y, z);
		sizex = sx;
		sizez = sz;
		setLevels(lvl);
		waypoints = wp;
	}	
	public Location getLocation() {
		return loc;
	}
	public ArrayList<Map<String,Integer>> getWaypoints() {
		return waypoints;
	}
	public String getId() {
		String did = "_mcd_" + this.name + this.loc.getBlockX() + "_" + this.loc.getBlockZ();
		return did.replaceAll("[^0-9a-zA-Z]", "_");
	}
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		String d;
		d = this.name + ",<BR>\n" + sizex + " x " + sizez + ",<BR>\n" + levels + " lvls";
		return d;
	}
	public String getTHDescription() {
		String d;
		d = this.name + ",<BR>\n" + this.levels + " steps";
		return d;
	}
	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}
}
