package skyview.sia;

import net.ivoa.util.CGI;
import skyview.executive.Settings;
import skyview.executive.Imager;

import java.util.HashMap;

import java.util.HashMap;
import java.util.ArrayList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

/** This class initiates a SkyView SIA request.
 */
public class SIA {
   
    
    private static FileWriter  testLog;
   
    private static SIA lastSIA;
    private static String  dftSurvey = "0408mhz,1420mhz,2massh,2massj,2massk,408mhz,4850mhz,co,cobe,cobeaam,cobezsma,dss,dss1b,dss1r,dss2b,dss2ir,dss2r,egrethard,egretsoft,euve83,euve171,euve405,euve555,first,gb6,granat_sigma_flux,granat_sigma_sig,halpha,heao1a,hri,integralspi_gc,iras12,iras25,iras60,iras100,iris12,iris25,iris60,iris100,neat,nh,nvss,pspc1int,pspc2int,rass.25kev,rass.75kev,rass1.5kev,rass,rasshb,rasssb,rass3bb,rass3hb,rass3sb,rxte3_20k_sig,rxte3_8k_sig,rxte8_20k_sig,sdssg,sdssi,sdssr,sdssu,sdssz,sfd100m,sfddust,shassa-c,shassa-cc,shassa-h,shassa-sm,sumss,wenss,wfcf1,wfcf2";
    
    private boolean   doFits;
    private String[]  quicklook;
    private double ra, dec, sizex, sizey;
    
    
    private String proj;
    private String csys;
    private double equinox;
    
    private int  naxisx, naxisy;
    private String interp;
    
    private String survey;
    
    private boolean hasMetadata(String[] formats) {
	
	for (String format: formats) {
	    if (format.equals("metadata")) {
		return true;
	    }
	}
	return false;
    }
    
    private String getSurvey() {
	
	String want = Settings.get("survey");
	if (want == null) {
	    return Settings.get("DefaultSIASurveys");
	} else {
	    if (Settings.has(want)) {
		return Settings.get(want);
	    } else {
		return want;
	    }
	}
    }
    
    private String[] getQuicklook(String[] formats) {
        Set<String> graphics = new HashSet<String>();
	for (int i=0; i<formats.length; i += 1) {
            String format = formats[i];
            
            // default graphic format is jpeg.
	    if (format.equals("graphic")  ||
                format.equals("image/jpeg")  || format.equals("image/jpg")) {
                graphics.add("jpeg");
            } else if (format.equals("image/gif")) {
		graphics.add("gif");
	    } else if (format.equals("image/tiff")) {
		graphics.add("tiff");
	    } else if (format.equals("image/png")) {
		graphics.add("png");
	    }
            if (format.equals("all") || format.equals("graphic-all")) {
                graphics.add("jpeg");
                graphics.add("gif");
                graphics.add("tiff");
                graphics.add("png");
            }
            // The syntax format=graphic-png,jpeg,...
            // supports the first matched syntax.  This must be the
            // last format in the list so we add in the remaining formats.
            if (format.startsWith("graphic-")) {
                String first = format.substring(8);
                String[] possibilities = new String[1+ (formats.length - i - 1)];
                possibilities[0] = first;
                System.arraycopy(formats,i+1, possibilities, 1, formats.length - i - 1);
                
                // Only accept first matching possibility.
                for (String p: possibilities) {
                    p = p.trim();
                    if (p.equals("jpeg")  || p.equals("jpg")) {
                        graphics.add("jpeg");
                        break;
                    } else if (p.equals("gif")) {
                        graphics.add("gif");
                        break;
                    } else if (p.equals("tiff")) {
                        graphics.add("tiff");
                        break;
                    } else if (p.equals("png")) {
                        graphics.add("png");
                        break;
                    }
                }
                break; // We already handled the remaining format entries.
            }
	}
        return graphics.toArray(new String[]{});
    }
    
    private boolean hasFits(String[] formats) {
	for (String format: formats) {
	    if (format.equals("all") ||
		format.equals("image/fits") ||
		format.equals("image/x-fits") ||
		format.equals("application/fits")) {
		return true;
	    }
	}
	return false;
    }
    
    private void getGeometry() {
	String[] pos = Settings.getArray("pos");
	if (pos.length != 2) {
	    error("POS not specified or incorrectly formatted:"+Settings.get("pos"));
	}
	try {
	    double ra   = Double.parseDouble(pos[0].trim());
	    double dec  = Double.parseDouble(pos[1].trim());
	    String[] sz = Settings.getArray("size");
	    if (sz.length < 1 || sz.length > 2) {
		error("Invalid string in size:"+Settings.get("size"));
	    }
	    double sizex = Double.parseDouble(sz[0]);
	    if (sz.length == 2) {
		sizey = Double.parseDouble(sz[1]);
	    } else {
		sizey = sizex;
	    }
	} catch (Exception e) {
	    error("Error parsing POS or SIZE:"+Settings.get("pos")+ " :: "+Settings.get("size"));
	}
	
	proj    = getProjection();
	Settings.put("projection", proj);
	csys    = getFrame();
	equinox = getEquinox();
	Settings.put("equinox", ""+equinox);
	
        int[] axes = getNaxes();
	naxisx = axes[0];
	naxisy = axes[1];
	
	interp  = getInterpolation();
    }
    
    int[] getNaxes() {
	
	if (Settings.has("NAXIS")) {
	    String[] ax = Settings.getArray("NAXIS");
	    try {
		int nx = Integer.parseInt(ax[0].trim());
		int ny;
		if (ax.length > 1) {
		    ny = Integer.parseInt(ax[1].trim());
		} else {
		    ny = nx;
		}
		return new int[]{nx,ny};
	    } catch (Exception e) {
		// Ignore errors and use default 
	    }
	}
	return new int[]{300,300};
    }
		
    
    String getProjection() {
	if (Settings.has("proj")) {
	    String set  = Settings.get("proj");
	    if (set.length() >= 3) {
	        String xset = set.substring(0,1).toUpperCase() +
		             set.substring(1,3).toLowerCase();
		if (xset.equals("Tan") ||
		    xset.equals("Ait") ||
		    xset.equals("Sin") ||
		    xset.equals("Car") ||
		    xset.equals("Csc") ||
		    xset.equals("Zea")) {
		    return xset;
		}
	    }
	}
	return "Tan";
    }
    
    String getFrame() {
	if (Settings.has("cframe")) {
	    String frame = Settings.get("cframe").toUpperCase().trim();
	    if (frame.equals("ICRS")) {
		return "ICRS";
	    } else if (frame.equals("FK4")) {
		return "B";
	    } else if (frame.equals("FK5")) {
		return "J";
	    } else if (frame.startsWith("G")) {
		return "Galactic";
	    } else if (frame.startsWith("E")) {
		return "E";
	    }
	} else {
	    Settings.put("cframe", "FK5");
	}
	return "J";
    }
    
    String getInterpolation() {
	if (Settings.has("Interpolation")) {
	    String interp = Settings.get("Interpolation").toUpperCase();
	    if (interp.equals("LI")) {
		return "LI";
	    } else if (interp.equals("NN")) {
		return "NN";
	    } else if (interp.equals("CLIP")) {
		return "Clip";
	    } else if (interp.startsWith("LANCZOS")) {
		return "Lanczos"+interp.substring(7);
	    } else if (interp.startsWith("SPLINE")) {
		return "Spline"+interp.substring(6);
	    }
	}
	return "Clip";
    }
	    
    double getEquinox() {
	if (Settings.has("equinox")) {
	    try {
		return Double.parseDouble(Settings.get("equinox"));
	    } catch(Exception e) {
		// Just ignore
	    }
	}
	if (csys.equals("B")) {
	    return 1950;
	} else {
	    return 2000;
	}
    }
    
    private static FileWriter logger;
    
    public static void main(String[] args) {
	for (int i=0; i<args.length; i += 1) {
	    Settings.updateFromFile(args[i]);
	}
	try {
//	    logger = new FileWriter("/tmp/sialog"+new java.util.Date().getTime());
	} catch (Exception e) {}
	log("Starting");
	SIA mySIA = new SIA();
	lastSIA = mySIA;
	mySIA.run();
    }
    
    public void writeSIAHeader() {
	Pattern p = Pattern.compile("\\$(\\w+)\\W");
	
	BufferedReader bf = null;
	try {
	    bf =
	      new BufferedReader(new InputStreamReader(
		 skyview.survey.Util.getResourceOrFile(Settings.get("SIA_HEADER"))));
	} catch (Exception e) {
	    error("Error opening SIA header file:"+e);
	}
	String line;
	try {
	    while ( (line = bf.readLine()) != null) {
	        Matcher m = p.matcher(line);
	        if (m.find()) {
		    String gr = m.group(1);
		    if (Settings.has(gr)) {
		        line = m.replaceFirst("value=\""+Settings.get(gr)+"\" ");
		    } else {
		        line = m.replaceFirst("");
		    }
	        }
	        System.out.println(line);
	    } 
	    bf.close();
	    System.out.flush();
	} catch (Exception e) {
	    System.err.println("IO error reading SIA header:"+e);
	    // Soldier on...
	}
    }
    
    public void writeSIAFooter() {
	System.out.println("</TABLEDATA></DATA></TABLE>");
	System.out.println("<INFO name=\"QUERY_STATUS\" value=\"OK\" />");
	System.out.println("</RESOURCE></VOTABLE>");
	System.out.close();
	System.err.close();
	log("Exiting!!!");
	System.exit(1);
    }
    
    public void doMetadata() {
	writeSIAHeader();
	writeSIAFooter();
    }
    
    public void run() {
	
	log("Running");
	try {
	    CGI params    = new CGI();
	    log ("Got CGI");
	    System.in.close();
	    
	    String[] keys = params.keys();
	    HashMap<String,String> newArgs = new HashMap<String,String>();
	    
	    // No SIA arguments should be repeated so
	    // we dont worry about concatenating multiple values.
	    for (String key: keys) {
	        String[] values = params.values(key);
	        for (String val: values) {
		    Settings.put(key, val);
	        }
	    }

	    log ("Got args");
	    String[] formats = Settings.getArray("format");
	    if (formats.length == 0) {
		Settings.put("format", "Image/FITS,Image/JPEG");
	    }
	    formats = Settings.getArray("format");
	    
	    for (int i=0; i<formats.length; i += 1) {
		formats[i]=formats[i].trim().toLowerCase();
	    }
	    
	    if (hasMetadata(formats)) {
		doMetadata();
		return;
	    }
	    
	    doFits    = hasFits(formats);
	    quicklook = getQuicklook(formats);
	    if (!doFits && quicklook == null) {
		doFits    = true;
		quicklook = new String[]{"jpeg"};
	    }

	    getGeometry();
	    
	    survey = getSurvey();
	    
	    log ("Got all params");
	} catch (Exception e) {
	    error("Exception found:"+e);
	}
	
	ArrayList<String> args = new ArrayList<String>();
	
	// Add the parameters than specify the output image
	args.add("survey="+survey);
	args.add("position="+Settings.get("pos"));
	if (sizex != 0) {
	    args.add("size="+Settings.get("size"));
	}
	args.add("pixels="  +  naxisx+","+naxisy);
	Settings.put("pixels", naxisx+","+naxisy);
	
	if (sizex==0 && naxisx == 0) {
	    error("Invalid defaults: cannot set both size and naxis to 0");
	}
	
	args.add("projection="+proj);
	args.add("coordinates="+csys);
	args.add("equinox="+equinox);
	args.add("sampler="+interp);
	Settings.put("sampler", interp);
	
	log("Set settings");
	
	// Now add parameters to do special SIA processing.
	args.add("nofits");  // Don't create FITS output.
	args.add("noexit");
	args.add("imagefinder=skyview.sia.Checker");
	args.add("mosaicker=skyview.sia.SIAWriter");
	
	log ("Set settings 2");
	
	// Write the header.
	writeSIAHeader();
	
	log ("Wrote header");
	boolean xerror = false;
	
	try {
	    log("Start imager at:"+new java.util.Date());
	    Imager.main((String[]) args.toArray(new String[0]));
	    log("End imager at:"+new java.util.Date());
	} catch (Exception e) {
	    // Move on quietly since we've already written the
	    // header...
	    log ("Exception:"+e);
	    int count = SIAWriter.getCount();
	    System.out.println("</TABLEDATA></DATA></TABLE>");
	    System.out.println("<INFO name=\"QUERY_STATUS\" value=\"ERROR\" >");
	    System.out.println("Error during processing:"+e+".");
	    System.out.println(count+" records processed before error");
	    System.out.println("</INFO></RESOURCE></VOTABLE>");
	    e.printStackTrace(System.err);
	    xerror = true;
	}
	if (!xerror) {
	    log ("About to write footer");
	    writeSIAFooter();
	}
    }
    
    void error(String msg) {
	log("Error:"+msg);
        System.out.println("Content-type: text/xml\n");
	System.out.println("<?xml version=\"1.0\"?>");
        System.out.println("<!DOCTYPE VOTABLE SYSTEM \"http://us-vo.org/xml/VOTable.dtd\">");
	System.out.println("<VOTABLE><RESOURCE type=\"results\">");
	System.out.println("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">");
	System.err.println(msg);
	System.out.println("</INFO></RESOURCE></VOTABLE>");
	System.out.close();
	System.err.close();
	System.exit(0);
    }
    
    public static SIA getSIA() {
	return lastSIA;
    }
    
    public boolean useFits() {
	return doFits;
    }
    
    public String[] quicklook() {
	return quicklook;
    }
    
    public static void log(String msg) {
	if (true) return;
	if (logger != null) {
	    try {
		logger.write(new java.util.Date()+":"+msg+"\n");
		logger.flush();
	    } catch (Exception e) {}
	}
    }
}
