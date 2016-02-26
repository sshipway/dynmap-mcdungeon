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
	private List<Object> waypoints;
	
	// Static function to read the MCDungeon cache and return a list of Dungeon objects.
	//@SuppressWarnings("unchecked")
	@SuppressWarnings({ "unchecked" })
	public static List<Dungeon> getDungeons(World w, String filename, Logger log) {
		List<Dungeon> dlst = new ArrayList<Dungeon>();
		
		File f = new File(filename);
		if( ! f.exists() ) { return null; }
		
		log.info("Loading in MCDungeon cache file from " + filename);
		
        PickleLoader loader = new PickleLoader();
        HashMap<String, Object> loaded = loader.getDataFileStream(filename,log);
        
        if( loaded == null ) {
        	log.warning("Unable to read MCDungeon cache file.  Maybe the version is too old?");
        	return null;
        }
        
        // Just for decrypting the file contents
        //Gson gson = new Gson();
        //Type type = new TypeToken<Object>() {}.getType();
        //String json = gson.toJson(loaded, type);
        //log.info(json);

        for( Object chest: loaded.values() ) { // These should all be Maps of Chest objects
        	String dname=null;
        	int dx=0,dz=0,dlevels=0;
        	int eh = 0;
        	int x,y,z;
    		Map<String,Object> dinfo = new HashMap<String,Object>();
    		List<Object> wp = null;

    		if( ! chest.getClass().getSimpleName().endsWith("HashMap") ) {
    			log.warning("Not a HashMap object in the cache: Probably the unpickling failed");
    			continue;
    		}
    		
    		if( ! ((HashMap<String,String>)chest).containsKey("CustomName") ) {
        		log.warning("This dungeon cache entry does not look like a chest!");
        		continue;        		
        	}
        	if( ! ((Map<String,String>)chest).get("CustomName").startsWith("MCDungeon") ) {
        		log.warning("This dungeon cache entry does not look like an MCDungeon chest!");
        		continue;
        	}
        	// log.info("Processing a dungeon chest...");
        	List<HashMap<String,Object>> items = (((HashMap<String,List<HashMap<String,Object>>>)chest).get("Items"));
        	if( items == null ) { 
        		log.warning("This chest has no items!");
        		continue; 
        	}
        	for( HashMap<String,Object> book: items ) {
				if( ! book.containsKey("tag") ) {
            		log.warning("This item has no tag!");
            		continue;         			
        		}
        		if( ! ((Map<String,Object>)(book.get("tag"))).containsKey("title") ) {
            		log.warning("This item is not a book!");
            		continue;         			
        		}
        		if( ! ((String)(((Map<String,Object>)(book.get("tag"))).get("title"))).startsWith("MCDungeon") ) {
            		log.warning("This book is not an MCDungeon book!");
            		continue;         			
        		}
//            	log.info("Processing a book...");
        		List<String> pages = (List<String>)((Map<String,Object>)(book.get("tag"))).get("pages");
        		for( String page: pages ) {
//                	log.info("Processing a page...");
                	try {
	        			Map<String,Object> pagedata = (Map<String,Object>)loader.getDataString(page,log);
	        			if( pagedata != null ) {
	        				dinfo.putAll(pagedata);
	        			} else {
	        				log.warning("Page was unable to be unpickled: "+page);
	        			}
                	} catch( Exception e ) {
                		log.warning("Unable to load page for some reason! "+e.getClass().getSimpleName());
                	}
        		}
        	}
        	
        	//log.info("dinfo keys present:");
        	//for( String k: dinfo.keySet()) {
        	//	log.info("  " + k + " = " + dinfo.get(k).toString());
        	//}

        	try {
				if( dinfo.containsKey("full_name") ) {
					dname = (String)dinfo.get("full_name");
				}
				if( dinfo.containsKey("levels") ) { // for dungeons
					dlevels = (Integer)dinfo.get("levels");
				}
				if( dinfo.containsKey("steps") ) { // for thunts
					dlevels = (Integer)dinfo.get("steps");
				}
			    if( dinfo.containsKey("xsize") ) {
					dx = (Integer)dinfo.get("xsize");
				}
				if( dinfo.containsKey("zsize") ) {
					dz = (Integer)dinfo.get("zsize");
				}
				if( dinfo.containsKey("entrace_height") ) {
					eh = (Integer)dinfo.get("entrace_height");
				}
        	} catch( Exception e ) {
        		log.warning("Problem parsing cache content (A): "+e.getMessage());
        		continue;
        	}

			try {
				x = ((HashMap<String,Integer>)chest).get("x");
				y = ((HashMap<String,Integer>)chest).get("y");
				z = ((HashMap<String,Integer>)chest).get("z");
				
				//log.info("Dungeon "+dname+" at "+x+","+y+","+z);
				
				if( dinfo.containsKey("portal_exit") ) {
					x += ((HashMap<String,Integer>)(dinfo.get("portal_exit"))).get("x");
					y -= ((HashMap<String,Integer>)(dinfo.get("portal_exit"))).get("y");
					z += ((HashMap<String,Integer>)(dinfo.get("portal_exit"))).get("z");
				} else if( dinfo.containsKey("entrance_pos") ) {
					x += (((HashMap<String,Integer>)(dinfo.get("entrace_pos"))).get("x") << 4) + 8;				
					y += eh;
					z += (((HashMap<String,Integer>)(dinfo.get("entrace_pos"))).get("z") << 4) + 8;				
				} else {
//					log.warning("Unable to identify portal exit or entrance position for dungeon '"+dname+"'");
					// treasure hunt
					x += 8; z += 8; // middle of chunk
				}
        	} catch( Exception e ) {
        		log.warning("Problem parsing cache content (B)");
        		continue;
        	}

			try {
				if( dinfo.containsKey("landmarks") ) {
					wp = (List<Object>)dinfo.get("landmarks");
				}
				
			} catch( Exception e ) {
        		log.warning("Problem parsing waypoints content: "+e.getClass().getSimpleName());
        		e.printStackTrace();
        		log.info((String) dinfo.get("landmarks"));
			}
			
        	if( (dlevels > 0) && (dname != null) ) {
        		dlst.add(new Dungeon(dname,w, x,y,z ,dx,dz,dlevels,wp));
        	} else {
        		log.warning("Invalid dungeon cache ignored!");
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
	Dungeon(String n, World w, int x, int y, int z, int sx, int sz, int lvl,List<Object> wp) {
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
	public List<Object> getWaypoints() {
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
		d = this.name + "\n" + sizex + " x " + sizez + "\n" + levels + " lvls";
		return d;
	}
	public String getTHDescription() {
		String d;
		d = this.name + "\n" + this.levels + " steps";
		return d;
	}
	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}
}
