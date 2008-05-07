package net.sf.okapi.plugins;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.okapi.Library.Base.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PluginsAccess {

	private static final String PLUGIN_FILENAME  = "plugin.xml";

	public static final String TYPE_UTILITY      = "utility";
	public static final String TYPE_FILTER       = "filter";
	public static final String TYPE_PARAMEDITOR  = "editor";
	
	private HashMap<String, PluginItem>     items;
	
	public PluginsAccess () {
		items = new HashMap<String, PluginItem>();
	}
	
	public boolean containsID (String id) {
		return items.containsKey(id);
	}
	
	/**
	 * Add all the plug-ins in a given folder.
	 * @param folder Folder where to look for plug-ins.
	 */
	public void addAllPackages (String folder)
		throws Exception 
	{
		// Look in all sub-folder (one level) and see if there
		// is a manifest to read.
		File[] dirs = (new File(folder)).listFiles();
		for ( File dir : dirs ) {
			if ( !dir.isDirectory() ) continue;
			String tmp = dir.getAbsolutePath() + File.separator + PLUGIN_FILENAME;
			File file = new File(tmp);
			if ( !file.exists() ) continue;
			// Add the package
			addPackage(file.getAbsolutePath());
		}
	}
	
	//TODO: Maybe this needs to be an helper static method somewhere
	private Element getFirstElement (Element parent,
		String name)
	{
		NodeList nl = parent.getElementsByTagName(name);
		if (( nl == null ) || ( nl.getLength() == 0 )) return null;
		else return (Element)nl.item(0);
	}

	public Iterator<String> getIterator () {
		return items.keySet().iterator();
	}
	
	public void addPackage (String path)
		throws Exception
	{
		try {
			DocumentBuilderFactory Fact = DocumentBuilderFactory.newInstance();
			Fact.setValidating(false);
			Document doc = Fact.newDocumentBuilder().parse(new File(path));
			String prefLang = Utils.getCurrentLanguage(); 
			Element rootElem = doc.getDocumentElement();

			NodeList nl = rootElem.getElementsByTagName("plugin");
			for ( int i=0; i<nl.getLength(); i++ ) {
				Element elem = (Element)nl.item(i);
				PluginItem item = new PluginItem();

				item.id = elem.getAttribute("id");
				if ( item.id.length() == 0 )
					throw new Exception("Attribute 'id' invalid or missing");
				item.pluginClass = elem.getAttribute("pluginClass");
				if ( item.pluginClass.length() == 0 )
					throw new Exception("Attribute 'pluginClass' invalid or missing");
				item.editorClass = elem.getAttribute("editorClass");

				int nDone = 0;
				NodeList infoList = elem.getElementsByTagName("info");
				for ( int j=0; j<infoList.getLength(); j++ ) {
					Element elemInfo = (Element)infoList.item(j);
					String lang = elemInfo.getAttribute("xml:lang");
					int n = 0;
					if ( Utils.areSameLanguages(lang, prefLang, true) ) n = 3;
					else if ( Utils.areSameLanguages(lang, prefLang, false) ) n = 2;
					else if ( lang.length() == 0 ) n = 1;
					if ( n > nDone ) {
						Element elem2 = getFirstElement(elemInfo, "name");
						item.name = elem2.getTextContent();
						elem2 = getFirstElement(elemInfo, "description");
						item.description = elem2.getTextContent();
						elem2 = getFirstElement(elemInfo, "provider");
						item.provider = elem2.getTextContent();
						nDone = n;
					}
					if ( nDone == 3 ) break; // Best match found
				}

				// Add the new item to the list (overrides any existing one)
				items.put(item.id, item);
			}
		}
		catch ( Exception E ) {
			throw E;
		}
	}

	public PluginItem getItem (String id) {
		return items.get(id);
	}
}
