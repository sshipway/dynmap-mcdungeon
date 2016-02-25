package org.steveshipway.dynmap;

import java.io.File;
import java.io.FileNotFoundException;
//import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
//import org.python.core.PyTuple;
import org.python.core.PyNone;
import org.python.modules.cPickle;

//import com.google.common.reflect.TypeToken;
//import com.google.gson.Gson;
//import java.lang.reflect.Type;

public class PickleLoader {

	public HashMap<String, Object> getDataFileStream(String filename,Logger  log) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        File f = new File(filename);
        InputStream fs = null;
        try {
            fs = new FileInputStream(f);
        } catch (FileNotFoundException e) {
        	log.warning("Pickle file not found!");
            return null;
        } catch( Exception e ) {
        	log.warning("Pickle file could not be opened!  Check permissions.");
            return null;
        }
        log.info("Reading in MCDungeon cache pickle...");
                
        PyFile picklefile = new PyFile(fs);

        PyDictionary phash = null;
        try {
        	phash = (PyDictionary) cPickle.load(picklefile);     	
        } catch ( PyException e3 ) {
        	log.severe("Cannot unpickle the MCDungeon cache! (Python error)");
        	if( PyException.isExceptionClass(e3.type) ) {
        		log.info("Is exception class "+ PyException.exceptionClassName(e3.type));
        	}
        	try {
        		e3.printStackTrace(); // throws a null pointer exception, usually
        	} catch ( Exception e2 ) {
        		log.info("Error printing stack trace");
        	}
           	return null;    
        } catch ( Exception e ) {
        	log.severe("Cannot unpickle the MCDungeon cache! Err: " + e.getClass().getSimpleName());
            e.printStackTrace();
        	return null;
        }
        ConcurrentMap<PyObject, PyObject> aMap = phash.getMap();
       	log.info("Pickle file processing into Java objects");
        for (Map.Entry<PyObject, PyObject> entry : aMap.entrySet()) {
            String keyval = entry.getKey().toString();
            PyObject tileentity = (PyDictionary) entry.getValue();
            try {
				data.put(keyval, pythonToJava(tileentity));
			} catch (Exception e) {
				log.severe("Unable to convert Python object ["+keyval+"] to Java! ");
				log.info(e.getMessage());

//				Gson gson = new Gson();
//		        Type type = new TypeToken<Object>() {}.getType();
//		        String json = gson.toJson(tileentity, type);
//		        log.info(json);
//	            e.printStackTrace();

				data.put(keyval, tileentity);
			}
        }
       	log.info("Pickle file read in successfully!");
        return data;
    }
   
    public HashMap<String, Object> getDataString(String str, Logger log) {
        HashMap<String, Object> data = new HashMap<String, Object>();
    	PyString pstr = new PyString(str);
    	PyDictionary pickle;
    	try {
        	pickle = (PyDictionary) cPickle.loads(pstr);
        } catch( PyException e ) {        	
        	Pattern p = Pattern.compile("S'([a-z_]+)'",0);
        	Pattern px = Pattern.compile("S'x'\\nI(-?\\d+)",0);
        	Pattern py = Pattern.compile("S'y'\\nI(-?\\d+)",0);
        	Pattern pz = Pattern.compile("S'z'\\nI(-?\\d+)",0);
        	Matcher m = p.matcher(str);
        	if( m.find() ) {
        		try {
	        		Matcher vm;
	        		HashMap<String,Integer> vec = new HashMap<String,Integer>();
	        		vm = px.matcher(str);
	        		if( vm.find() ) { vec.put("x", Integer.parseInt(vm.group(1))); } else { vec.put("x", 999); }
	        		vm = py.matcher(str);
	        		if( vm.find() ) { vec.put("y", Integer.parseInt(vm.group(1))); } else { vec.put("y", 999); }
	        		vm = pz.matcher(str);
	        		if( vm.find() ) { vec.put("z", Integer.parseInt(vm.group(1))); } else { vec.put("z", 999); }
	        		data.put(m.group(1), vec);
        		} catch( Exception ee ) {
        			log.severe("Problems trying to eat bad pickles");
        			ee.printStackTrace();
        		}
        		return data;
        	} else {
            	log.warning("Unable to parse picklestring!");
            	return null;        	
        	}
        } catch( Exception e ) {
        	log.warning("Unable to parse picklestring! " + e.getClass().getSimpleName());
        	return null;
        }
    	
    	ConcurrentMap<PyObject, PyObject> aMap = pickle.getMap();
       	//log.info("Pickle file processing into Java objects");
        for (Map.Entry<PyObject, PyObject> entry : aMap.entrySet()) {
            String keyval = entry.getKey().toString();
            PyObject dataval = entry.getValue();
            try {
				data.put(keyval, pythonToJava(dataval));
			} catch (Exception e) {
				log.warning("Unable to convert Python to Java in picklestring: "+e.getClass().getSimpleName());
	            //e.printStackTrace();
				data.put(keyval, dataval);
			}
        }

        return data;
    }
    
    @SuppressWarnings("unchecked")
	public static Object pythonToJava(PyObject pyObject) throws Exception {
        try {
            Object javaObj = null;
            if (pyObject instanceof PyList) {
                List<Object> list = new ArrayList<Object>();
                for (PyObject bagTuple : ((PyList) pyObject).asIterable()) {
                    list.add(pythonToJava(bagTuple));
                }
                javaObj = list;
            } else if (pyObject instanceof PyDictionary) {
                Map<?, Object> map = Py.tojava(pyObject, Map.class);
                Map<Object, Object> newMap = new HashMap<Object, Object>();
                for (Map.Entry<?, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof PyObject) {
                        newMap.put(entry.getKey(), pythonToJava((PyObject) entry.getValue()));
                    } else {
                        // Jython sometimes uses directly the java class: for example for integers
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
                javaObj = newMap;
            } else if (pyObject instanceof PyLong) {
                javaObj = pyObject.__tojava__(Long.class);
            } else if (pyObject instanceof PyInteger) {
                javaObj = pyObject.__tojava__(Integer.class);
            } else if (pyObject instanceof PyFloat) {
                // J(P)ython is loosely typed, supports only float type, 
                // hence we convert everything to double to save precision
                javaObj = pyObject.__tojava__(Double.class);
            } else if (pyObject instanceof PyString) {
                javaObj = pyObject.__tojava__(String.class);
            } else if (pyObject instanceof PyNone) {
                return null;
            } else {
                throw new Exception("Non supported  datatype found, cast failed: "+(pyObject==null?null:pyObject.getClass().getName()));
            }
            if(javaObj.equals(Py.NoConversion)) {
                throw new Exception("Cannot cast into any java type: "+(pyObject==null?null:pyObject.getClass().getName()));
            }
            return javaObj;
        } catch (Exception e) {
            throw new Exception("Cannot convert jython type ("+(pyObject==null?null:pyObject.getClass().getName())+") to Java datatype: "+ e, e);
        }
    }
    
}