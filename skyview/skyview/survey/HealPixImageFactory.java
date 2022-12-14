package skyview.survey;

import skyview.executive.Settings;

/** A little class that creates FITS images using a factory method. */
public class HealPixImageFactory implements ImageFactory {
    
    private boolean first      = true;
    private String  prefix     = null;
    private boolean havePrefix = false;
    
    public HealPixImage factory(String file) {
	
	if (first) {
	    first = false;
	    if (Settings.has("FilePrefix")) {
		prefix = Settings.get("FilePrefix");
		havePrefix = true;
	    }
	}
	if (havePrefix) {
	    file = prefix + file;
	}
        String test = Util.replacePrefix(file, Settings.getArray("LocalURL"));
        if (new java.io.File(test).exists()) {
            file = test;
        }
        	
	try {
	    return new HealPixImage(file);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new Error("Irrecoverable error for HealPix file: "+file);
	}
    }
}
