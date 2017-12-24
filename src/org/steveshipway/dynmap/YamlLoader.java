package org.steveshipway.dynmap;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
//import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
//import org.yaml.snakeyaml.nodes.MappingNode;
//import org.yaml.snakeyaml.nodes.Tag;

public class YamlLoader {


	public class MyYamlConstructor extends Constructor {
		private HashMap<String,Class<?>> classMap = new HashMap<String,Class<?>>();

//		public MyYamlConstructor(Class<? extends Object> theRoot) {
		public MyYamlConstructor() {
           super( );
           classMap.put( "python/object:utils.Vec", Map.class );
		}

       /*
        * This is a modified version of the Constructor. Rather than using a class loader to
        * get external classes, they are already predefined above. This approach works similar to
        * the typeTags structure in the original constructor, except that class information is
        * pre-populated during initialization rather than runtime.
        *
        * @see org.yaml.snakeyaml.constructor.Constructor#getClassForNode(org.yaml.snakeyaml.nodes.Node)
        */
        protected Class<?> getClassForNode(Node node) {
            String name = node.getTag().getClassName();
            Class<?> cl = classMap.get( name );
            if ( cl == null )
                throw new YAMLException( "Class not found: " + name );
            else
                return cl;
        }
	}

	
	@SuppressWarnings({"rawtypes","unchecked"})
	public HashMap<String, Object> getYamlFromFile(File f,Logger  log) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        InputStream fs = null;
        try {
            fs = new FileInputStream(f);
        } catch (FileNotFoundException e) {
        	log.warning("-- YAML cache file not found!");
            return null;
        } catch( Exception e ) {
        	log.warning("-- YAML cache file could not be opened!  Check permissions.");
            return null;
        }
        log.info("-- Reading in MCDungeon YAML cache...");

        // process file XXXX
        Yaml yaml = new Yaml(new MyYamlConstructor());
        //Yaml yaml = new Yaml(new CustomClassLoaderConstructor(myclass.class.getClassLoader()));
        
        
        ArrayList dungeons = (ArrayList) yaml.load(fs);
        
		for (int i = 0; i < dungeons.size(); i++) {
			Map d = (Map<String, Object>)dungeons.get(i);
			String dname = (String) d.get("full_name");
            data.put(dname,d);
		}
       
       	log.info("-- YAML cache file read in successfully!");
        return data;
	}
	
	public ArrayList<Map<String, Integer>> getWaypoints(String wpoints, Logger log) {
		ArrayList<Map<String, Integer>> wp = new ArrayList<Map<String, Integer>>();
		Pattern rx;
		Matcher m;

		
		// parse the string in wpoints of form "[(1,2,3),(4,5,6)]" into wp[1]{x=1,y=2,z=3}
		String pattern = "\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)";
		// Create a Pattern object
		rx = Pattern.compile(pattern);
		
		m = rx.matcher(wpoints);
		Integer base = 0;
		while (m.find(base)) {
			HashMap<String,Integer> point = new HashMap<String,Integer>();
			//log.info( "Found pattern at offset "+base+" value ["+m.group(1)+"],["+m.group(2)+"]");
			point.put("x", Integer.parseUnsignedInt(m.group(1)));
			point.put("y", Integer.parseUnsignedInt(m.group(2)));
			point.put("z", Integer.parseUnsignedInt(m.group(3)));
			wp.add(point);
			base = m.end(3);
		}
		
		return wp;
	}
	
	
}
