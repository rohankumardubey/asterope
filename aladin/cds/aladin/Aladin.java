// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//


package cds.aladin;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import cds.aladin.bookmark.Bookmarks;
import cds.allsky.Context;
import cds.allsky.MocGen;
import cds.allsky.HipsGen;
import cds.tools.CDSFileDialog;
import cds.tools.ExtApp;
import cds.tools.Util;
import cds.tools.VOApp;
import cds.tools.VOObserver;
import cds.xml.Field;
import cds.xml.XMLParser;

/**
 * La classe Aladin est le point d'entree d'Aladin.
 * Elle cree tous les objets et contient les differents
 * flags d'etat permettant de savoir si on est en mode debug,
 * standalone...
 * <BR>
 * Elle fonctionne a la fois en Applet ou en Standalone via la fonction main()
 *
 * @author   Pierre Fernique [CDS], Thomas Boch [CDS], Ana?s Oberto[CDS], Fran?ois Bonnarel [CDS]
 *
 * @beta The beta version incorporates new features in test phase for the next official Aladin version.<BR>
 * @beta The stability of these features is not totally guaranteed.
 * @beta <P>
 * @beta <B>New features and performance improvements:</B>
 * @beta <UL>
 * @beta    <LI> HiPS improvements : cube support, index.html
 * @beta </UL>
 * @beta
 * @beta <B>Major fixed bugs:</B>
 * @beta <UL>
 * @beta    <LI> HiPS generation "." directory bug fixed
 * @beta <UL>
 * @beta </UL>
 *
 */
public class Aladin extends JApplet
                    implements ExtApp,VOApp,ClipboardOwner,
                               MouseListener,MouseMotionListener,
                               ActionListener,
                               DropTargetListener, DragSourceListener, DragGestureListener
                    {


//   static final boolean VP=true;
   
//   static final Dimension SCREENSIZE= Toolkit.getDefaultToolkit().getScreenSize();
    static Dimension SCREENSIZE= null;
    static final boolean LSCREEN= true; //SCREENSIZE.width>1000;

    /** Nom de l'application */
    static protected final String TITRE   = "Aladin";
    static protected final String FULLTITRE   = "Aladin Sky Atlas";

    /** Numero de version */
    static public final    String VERSION = "v8.101";
    static protected final String AUTHORS = "P.Fernique, T.Boch, A.Oberto, F.Bonnarel";
    static protected final String OUTREACH_VERSION = "    *** UNDERGRADUATE MODE (based on "+VERSION+") ***";
    static protected final String BETA_VERSION     = "    *** BETA VERSION (based on "+VERSION+") ***";
    static protected final String PROTO_VERSION    = "    *** PROTOTYPE VERSION (based on "+VERSION+") ***";
    static protected  String currentVersion = null;	// Version courante dispo

    /** MRdecomp active */
    static protected final boolean MRDECOMP= false;

    /** Taille moyenne des fonts */
    static protected int  SIZE   = 12;

    static final String ICON              = "icon.gif";
    static final String ALADINMAINSITE    = "aladin.u-strasbg.fr";
    static final String WELCOME           = "Bienvenue sur "+TITRE+
                                            " - "+getReleaseNumber();
    static String COPYRIGHT         = "(c) 2014 UDS/CNRS - by CDS - Distributed under GNU GPL v3";

    static protected String CACHE = ".aladin"; // Nom du r?pertoire cache
    static protected String CACHEDIR = null;   // Filename du r?pertoire cache, null si non encore
                                               // cr??, "" si impossible ? cr?er

    static protected final String FOVURL  = "http://"+Aladin.ALADINMAINSITE+"/java/FOVs.xml";
    static protected final String TREEURL = "http://"+Aladin.ALADINMAINSITE+"/java/Tree.dic";
    static protected final String LANGURL = "http://"+Aladin.ALADINMAINSITE+"/java/nph-aladin.pl?frame=getLang";

    // La couleur du fond
    static final Color BKGD   = Color.lightGray;
//    static final Color BKGD   = new Color(246,246,246);
    static final Color GREEN = new Color(27,137,0);
    static final Color DARKBLUE = new Color(102,102,153);
    static final Color MYBLUE = new Color(49,106,197);
    static final Color LBLUE = new Color(229,229,229);
    public static final Color BLUE =  new Color(214,214,255);
    static final Color MAXBLUE =  new Color(153,153,255);
    static final Color BLUEHELP = new Color(25,76,127);
    static final Color MYGRAY = new Color(180,183,187);
    static final Color STACKBLUE = new Color(140,140,255);
    static final Color STACKGRAY = new Color(150,150,150);
    static final Color BLACKBLUE = new Color(0,0,200);
    static final Color BACKGROUND   = new Color(198,218,239); 
    static final Color ORANGE   = new Color(255,137,58); 
    static final Color LIGHTORANGE   = new Color(255,211,58); 

    // couleur de fond du bouton Load... lorsqu'il est op?rationnel
//    static final Color COLOR_LOAD_READY = new Color(110,230,50);
    static final Color COLOR_LOAD_READY = new Color(50,205,110);

    // Le repertoire d'installation d'Aladin
    static String HOME;

    // Le nom de la machine d'ou provient l'applet (s'il y a lieu)
    static String APPLETSERVER=null;
    static String HOSTSERVER=null;

    // Le nom de la base de donn?es qui a lanc? l'applet ( champ &from=XXX dans l'url )
    static String FROMDB=null;

    // Les noms des fichiers GLU locaux additionnels pass?s en ligne de commande
    static String GLUFILE=null;

    // url pour passer un script ? l'applet
    static String SCRIPTFILE=null;

    // Les noms des fichiers Strings locaux additionnels pass?s en ligne de commande
    static String STRINGFILE=null;

    // Le nom de la machine distante qui utilise Aladin Java ? travers un cgi
    static String RHOST=null;

    // Le mode de d?marrage d'Aladin (full, frame, preview, le d?faut si null)
    protected String SCREEN=null;
    private boolean flagScreen=false;   // true si le mode SCREEN doit ?tre pris en compte (voir paint())

    // true si on tourne sous LINUX (pour pallier ? un bug MemoryImage.newPixel)
    static boolean ISLINUX=false;

    static boolean ISJNLP=false;
    static boolean ISJVM15=false;
    static boolean ISJVM16=false;

    // true si mode robot supporte !
    static boolean ROBOTSUPPORT=false;

    // true si Centre de rotation FOV d?port? support?
    static boolean ROTATEFOVCENTER=true;

    // true si le reseau est accessible
    static boolean NETWORK=true;

    // true si on affiche la console
    static boolean CONSOLE=true;

    // true si on affiche le banner
    static boolean BANNER=true;

    // true si on affiche le copyright sur les sorties PNG,JPG EPS et autres
    static boolean CREDIT=true;

    // true si on charge les bookmarks
    static boolean BOOKMARKS=true;

    // true si on compare le num?ro de version avec la version courante
    static boolean TESTRELEASE=true;

    // true si on ne lance pas de hub interne, quelles que soient les preferences
    static boolean NOHUB=false;

    // true si on ne lance ne charge pas les plugins
    static boolean NOPLUGIN=false;

     // G?re le mode particuliers
    static boolean LOG=true;  // false si on inhibe les logs
    public static boolean BETA   =false;
    public static boolean CDS=false;   // true si on tourne en mode CDS
    public static boolean PROTO=false;	// true si on tourne en mode PROTO (n?cessite Proto.jar)
    static public boolean OUTREACH   =false;
    static boolean setOUTREACH   =false;
    static int ALIASING=0;            // 0-d?faut syst?me, 1-actif, -1-d?sactiv?

    static boolean ENABLE_FOOTPRINT_OPACITY=true; // footprints en transparence ?
    static float DEFAULT_FOOTPRINT_OPACITY_LEVEL=0.15f+0.000111f; // niveau de transparence (entre 0.0 et 1.0)

    // Si le menu ou le sous-menu commence par l'une des chaines ci-dessous,
    // il ne s'affichera que dans le mode correspondant.
    // Rq: le mode proto active automatiquement le mode beta
    static final String BETAPREFIX = "BETA:";
    static final String PROTOPREFIX = "PROTO:";
    static final String OUTREACHPREFIX = "OUTREACH:";
    static final String NOAPPLETPREFIX = "NOAPPLET:";

    // Si une image est plus petite que cette limite, on pr?f?rera garder les pixels
    // d'origine (PlanImage.pixelsOrigin) en m?moire pour ?viter des acc?s disques
    // pour chaque valeur de pixel
    static final int LIMIT_PIXELORIGIN_INMEM = 8*1024*1024;

    // Limite image en full access
    static final long LIMIT_HUGEFILE = Math.min(Integer.MAX_VALUE,Runtime.getRuntime().maxMemory()/2L);
    
    static long MAXMEM = Runtime.getRuntime().maxMemory()/(1024*1024);
    
    // Marge limite en MO pour le chargement des cubes en RAM.
    // Il faut au-moins 500Mo de disponible pour une telle strat?gie 
//    static int MARGERAM = !PROTO ? 20000 : MAXMEM>500 ? 150 : 500;
    static int MARGERAM = MAXMEM>500 ? 150 : 500;

    // Le nom du dico GLU specifique a Aladin
    static String ALAGLU = "AlaGlu.dic";

    // Caract?res (?ventuellement plusieurs) utilis?s comme s?parateur
    // de colonne pour les tables CSV
    protected String CSVCHAR = "\t";
    
    // True si par d?faut l'outil Tag doit centrer sur l'objet le plus proche
    protected boolean CENTEREDTAG = false;

    // Le mapping des pixels par defaut (si surcharge via setconf)
    protected String CMDEFAULT = null;
//    protected String CMDEFAULT = "reverse gray autocut Log";

    // Le mapping du background par defaut (si surcharge via setconf)
    protected String BKGDEFAULT = null;

    // Un filtre d?di? doit-il ?tre appliqu? par d?faut
    protected String FILTERDEFAULT=null;

    // Le num?ro de session d'Aladin
    static private int ALADINSESSION = -1;
    protected int aladinSession=0;

    // Les fontes associees a Aladin
    static int  SSIZE,SSSIZE,LSIZE  ;
    static public Font BOLD,PLAIN,ITALIC,SBOLD,SSBOLD,SPLAIN,SSPLAIN,SITALIC,
                LPLAIN,LBOLD,LITALIC,LLITALIC,L,COURIER,BCOURIER;

    // L'instance d'aladin lui-meme, pour la methode main() et
    // l'utilisation par une autre application java (voir methode launch() );
    public static Aladin aladin;

    static boolean PLASTIC_SUPPORT = true; // activation ou non du support PLASTIC/SAMP
    static boolean USE_ACR = false; // True si on utilise les librairies ACR (AstroGrid)

    private Banner banner=null;

    // Les objets associees a l'interface
    FullScreen fullScreen=null;   // G?re le Frame du mode plein ?cran, null si non actif
    public Bookmarks bookmarks;          // G?re les favoris
    View view;                    // Gere la "View frame"
    Status status;                // Gere la ligne de "Status"
    Match sync;                   // Gere le logo pour la grille
    Grid grid;                    // Gere le logo pour la grille
    Oeil oeil;                    // Gere le logo pour l'oeil
    Northup northup;              // G?re le logo pour le Nord en haut
    ViewControl viewControl;	  // Gere le logo de controle des views
    Tips urlStatus;               // Gere la ligne de l'info sur les URLs
    MyLabel memStatus;            // Gere la ligne de l'info sur l'usage de la m?moire
    Mesure mesure;                // Gere la "Frame of measurements"
    MySplitPane splitH;           // G?re la s?paration mesure/Vue
    Search search;                // G?re le bandeau de recherche dans les mesures
    public ToolBox toolBox;       // Gere la "Tool bar"
    public Calque calque;         // Gere a la fois les plans et le zoom
    Localisation localisation;    // Gere l'affichage de la "Localisation"
    Logo logo;                    // Gere le "logo"
    PlasticWidget plasticWidget;  // Gere le widget PLASTIC
    PlasticPreferences plasticPrefs; // Gere les preferences PLASTIC
    Help help;                    // Gere le "Help" en ligne
    public ServerDialog dialog;   // Gere l'interrogation des serveurs
    TreeView treeView;            // Gere l'arbre contenant l'historique des interrogations
    FrameColorMap frameCM;              // Gere la fenetre du controle de la table des couleurs
    FrameRGB frameRGB;            // Gere la fenetre pour la creation des plans RGB
    FrameBlink frameBlink;        // Gere la fenetre pour la creation des plans Blink
    FrameArithmetic frameArithm;   // Gere la fenetre pour la creation des plans Arithmetic via une op?ration arithm?tique
    FrameMocFiltering frameMocFiltering;   // Gere la fenetre pour les op?rations de filtrage par les MOCs
    FrameMocOperation frameMocOperation;   // Gere la fenetre pour les op?rations sur les MOCs
    FrameMocGenImgs frameMocGenImgs; // Gere la fenetre pour la g?n?ration d'un MOC ? partir d'une collection d'images
    FrameMocGenImg frameMocGenImg;   // Gere la fenetre pour la g?n?ration d'un MOC ? partir d'images
    FrameMocGenCat frameMocGenCat;   // Gere la fenetre pour la g?n?ration d'un MOC ? partir de catalogues
    FrameMocGenRes frameMocGenRes;   // Gere la fenetre pour la g?n?ration d'un MOC ? partir d'un autre MOC de meilleure r?solution
    FrameBitpix frameBitpix;       // Gere la fenetre pour de conversion du bitpix d'une image
    FrameConvolution frameConvolution; // Gere la fenetre pour la creation des plans Arithmetic via une convolution
    FrameHealpixArithmetic frameHealpixArithm;   // Gere la fenetre pour la creation des plans Arithmetic pour Healpix
    FrameCDSXMatch frameCDSXMatch;// Gere la fenetre pour le x-match
    FrameColumnCalculator frameCalc; // Gere la fenetre pour ajout de colonnes
    FrameContour frameContour;    // Gere la fenetre pour les choix de niveaux de contour
    FrameInfo frameInfo;          // Gere la fenetre d'informations sur un noeud de l'arbre
    FrameInfoServer frameInfoServer; // G?re la fen?tre des infos sur un serveur
    FrameMacro frameMacro;        // Gere la fenetre des Macros
    FrameVOTool frameVOTool;      // G?re les applications VO accessibles par Aladin
    protected FrameProgenAjeter frameProgen = null;
    protected FrameProp frameProp;// Fen?tre des propri?t?s individuelles d'un objet graphique
    public FrameAllskyTool frameAllsky;  // G?re la creation locale d'un allsky
    public Console console;                  // Gere la fenetre de la console
    public Command command=null;	      // Gere les commandes asynchrones
    Synchro synchroServer;              // G?re les synchronisations des servers
    Synchro synchroPlan;              // G?re les synchronisations des Plans
    FrameNewCalib frameNewCalib=null; // Gere la fenetre de recalibration astrometrique
    public Configuration configuration;	      // Configuration utilisateur
    public KernelList kernelList;    // G?re la liste des noyaux de convolution
    static protected Chaine chaine;     // G?re les chaines de textes (support multilangage
    AppMessagingInterface appMessagingMgr;    // G?re la connexion/l'envoi de messages PLASTIC/SAMP

    // Les objets internes
    public Glu glu=null;   // Gere les interactions avec le GLU
    static Cache cache=null; // G?re le cache
    protected Plugins plugins;    // Acc?s aux plugins
    CardLayout cardView;          // Gere la permutation entre le "Help" et la "View"
    CreatObj co;		          // pour gerer la creation parallele des widgets
    public Save save=null;		          // pour gerer les sauvegardes
    ExtApp extApp = null;         // Application cooperative a Aladin
    String javaVersion;
    static boolean macPlateform = false; // Aladin est-il ex?cut? sur un Mac ?
    private String lastDir=null;  // Le dernier r?pertoire utilis?
    private final long startTime = System.currentTimeMillis();  // Date de d?marrage
    private long sizeCache=0L;    // Taille du cache disque pour les grosses images

    // plugin VOSpec
    Object vospec;

    // r?f?rence sur la derni?re fenetre FilterProperties ? avoir ?t? cr??e (pour robot)
    FilterProperties lastFilterCreated;

    // r?f?rence sur le bouton Load (pour robot)
    MyButton loadBtn;

    // Les memorisations en vue de mises a jour
    JPanel infoPanel;             // Panel de la ligne tout en bas
    JPanel bigView;               // Panel contenant a la fois le view et le help
    JPanel mesurePanel;		      // Panel contenant les mesures
    Vector vButton;               // Vecteur des boutons du menu a (des/)activer
    Container myParent=null;      // Pour pouvoir re-fenestrer
    Rectangle origPos=null;       // Dimension d'origine dans le navigateur
    static public String error;          // La derniere chaine d'erreur (DEVRAIT NE PAS ETRE STATIC)
    protected JMenuBar jBar;      // La barre de menu
    private JButton bDetach;
    private JMenuItem miDetach,miCalImg,miCalCat,miAddCol,miSimbad,miAutoDist,miVizierSED,miXmatch,miROI,/*miTip,*/
                      miVOtool,miGluSky,miGluTool,miPref,miPlasReg,miPlasUnreg,miPlasBroadcast,
                      miDel,miDelAll,miPixel,miContour,miSave,miPrint,miSaveG,miScreen,miPScreen,miMore,miNext,
                      miLock,miDelLock,miStick,miOne,miNorthUp,
                      miProp,miGrid,miReticle,miReticleL,miNoReticle,
                      miTarget,miOverlay,miRainbow,miZoomPt,miZoom,miSync,miSyncProj,miCopy1,miPaste,
                      /* miPrevPos,miNextPos, */
                      miPan,miGlass,miGlassTable,miPanel1,miPanel2c,miPanel2l,miPanel4,miPanel9,miPanel16,
                      miImg,miOpen,miCat,miPlugs,miRsamp,miRGB,miMosaic,miBlink,
                      miGrey,miFilter,miFilterB,miSelect,miSelectAll,miSelectTag,miTagSelect,miDetag,miSearch,
                      miUnSelect,miCut,miStatSurf,miTransp,miTranspon,miTag,miDist,miDraw,miTexte,miCrop,miCreateHpx,
                      miCopy,miHpxGrid,miHpxDump,
                      miTableInfo,miClone,miPlotcat,miConcat,miExport,miExportEPS,miBackup, /* miHistory, */
                      miInFold,miConv,miArithm,miMocHips,miMocGenImg,miMocGenCat,miMocOp,miMocToOrder,miMocFiltering,miMocCrop,
                      miHealpixArithm,miNorm,miBitpix,miPixExtr,miHead,miFlip,
                      miSAMPRegister,miSAMPUnregister,miSAMPStartHub,miSAMPStopHub,miLastFile,
                      miBroadcastAll,miBroadcastTables,miBroadcastImgs; // Pour pouvoir modifier ces menuItems
    JButton ExportYourWork,searchData,avant,apres;

    static boolean STANDALONE = false;	// True si on fonctionne en mode standalone
    static boolean SIGNEDAPPLET = false;// True si on fonctionne en mode applet sign?

    // Juste pour les essais NED
    static String CGIPATH = null;

    // Pour savoir si on a d?j? affich? un message d'usage restreint de l'applet
    static private boolean warningRestricted = false;

    // Gestion du niveau de trace
    static final int MAXLEVELTRACE = 4;
    static public int levelTrace=0;

    // Variables associees au mode de fonctionnement
    boolean flagLoad=false;	      // true si on est en mode de chargement
    MyFrame f=null;        // Le "Frame" en mode "Standalone"
    protected boolean msgOn=true;           // True si le message d'accueil est actif
    static boolean flagLaunch=false; // true si on a demarre aladin par launch
    static boolean NOGUI=false;  // True si le mode script est actif (sans interface)
    boolean inHelp=false;  // True si le mode "Help" est actif
    boolean inScriptHelp=false;  // True si le mode "ScriptHelp" est actif
    static int iv=0;              // Indice de la performance JAVA
    static long speed=-1;         // Indice de performance (lower is better)
    static final long slownessThreshold = 800; // Seuil a partir duquel on considere la JVM comme lente
    static boolean isSlow;        // true si lance en mode APPLET et speed>seuil
//    boolean flagInsets=false;     // True si on a deja pris en compte le Insets du peer
    boolean print=false;	  // true si on est entrain d'imprimer
    protected boolean gc=true;	// false si on a inhib? l'appel ? Aladin.gc()
    static Applet extApplet=null; // Decrit l'applet qui aurait appele launch()
    protected boolean firstLoad=true; // true si on n'a pas encore affich? le ServerDialog
    protected boolean flagDetach=true; // true si on tourne aladin dans sa propre frame


    // Les textes associes aux differentes possibilites du menu
    static final int GETHEIGHT  = 15;		// Cochonnerie de getHeight()

    // Les menus;
    String MFILE,MSAVE,OPENLOAD,LASTFILE,OPENFILE,OPENURL,LOADIMG,LOADCAT,LOADVO,LOADFOV,/*HISTORY,*/MEDIT,MVIEW,
           MIMAGE,MCATALOG,MOVERLAY,MDOC ;
    String MTOOLS,MPLUGS,MINTEROP,MHELP,MDCH1,MDCH2,MPRINT,MQUIT,MCLOSE,PROP;
    String MBGKG; // menus pour les backgrounds

    // Sous-menus
    String CMD,MBKM,XMATCH,CALIMG,PIXEL,CONTOUR,GRID,RETICLE,RETICLEL,NORETICLE,
           TARGET,OVERLAY,RAINBOW,DEL,DELALL,CALCAT,ADDCOL,ROI,VOTOOL,SIMBAD,VIZIERSED,AUTODIST,/*TIP,*/MSCROLL,
           COOTOOL,PIXELTOOL,CALCULATOR, SESAME,NEW,PREF,
           /*CEA_TOOLS,*/MACRO,TUTO,HELP,HELPSCRIPT,FAQ,MAN,FILTER,FILTERB,
           TUTORIAL,SENDBUG,PLUGINFO,NEWS,ABOUT,ZOOMP,ZOOMM,ZOOM,ZOOMPT,PAN,SYNC,PREVPOS,NEXTPOS,
           SYNCPROJ,GLASS,GLASSTABLE,RSAMP,VOINFO,FULLSCREEN,PREVIEWSCREEN,MOREVIEWS,ONEVIEW,NEXT,LOCKVIEW,
           DELLOCKVIEW,STICKVIEW,FULLINT,NORTHUP,COPIER,COLLER,
           RGB,MOSAIC,BLINK,GREY,SELECT,SELECTTAG,DETAG,TAGSELECT,SELECTALL,UNSELECT,PANEL,
           PANEL1,PANEL2C,PANEL2L,PANEL4,PANEL9,PANEL16,NTOOL,DIST,DRAW,PHOT,TAG,STATSURF,STATSURFCIRC,
           STATSURFPOLY,CUT,TRANSP,TRANSPON,CROP,COPY,CLONE,CLONE1,CLONE2,PLOTCAT,CONCAT,CONCAT1,CONCAT2,TABLEINFO,
           SAVEVIEW,EXPORTEPS,EXPORT,BACKUP,FOLD,INFOLD,ARITHM,MOC,MOCGENIMG,MOCGENIMGS,MOCGENCAT,
           MOCM,MOCTOORDER,MOCFILTERING,MOCCROP,MOCHELP,MOCLOAD,MOCHIPS,
           HEALPIXARITHM,/*ADD,SUB,MUL,DIV,*/
           CONV,NORM,BITPIX,PIXEXTR,HEAD,FLIP,TOPBOTTOM,RIGHTLEFT,SEARCH,ALADIN_IMG_SERVER,GLUTOOL,GLUINFO,
           REGISTER,UNREGISTER,BROADCAST,BROADCASTTABLE,BROADCASTIMAGE,SAMPPREFS,STARTINTERNALHUB,STOPINTERNALHUB,
           HPXCREATE,HPXGRID,HPXDUMP,FOVEDITOR,HPXGENERATE,GETOBJ;
    String JUNIT=PROTOPREFIX+"*** Aladin internal code tests ***";

     /** Retourne l'objet g?rant les chaines */
     public static Chaine getChaine() { return chaine; }

  /**
    * Retourne le host dans une URL, null si probleme
    */
   static protected String getSite(String url) {
      if( url==null || !url.startsWith("http://") ) return null;
      int pos = url.indexOf('/',7);
      return pos>0?url.substring(7,pos):url.substring(7);
   }

   private boolean flagFrame=false;

   /** Va tester s'il s'agit d'une applet sign? ou non et va lancer l'applet dans une fen?tre ? part si le parametre
    * inFrame est pr?sent */
   @Override
   public void init() {
      try {
         System.getProperty("java.home");
         STANDALONE=SIGNEDAPPLET=true;
      } catch( Exception e )  { }

      String frame=null;
      try { frame = getParameter("inFrame"); flagFrame=frame!=null; } catch( Exception e4 ) {};
      flagDetach=false;
      myInit();

      //       if( !flagFrame ) { flagDetach=false; myInit(); }
      //       else startInFrame(this);

   }

   @Override
   public void stop() {
      // Nettoyage de la pile
      try { calque.FreeAll(); } catch( Exception e ) {}

      if( dialog!=null ) dialog.hide();
      if( frameCM!=null ) frameCM.hide();
      if( f!=null ) f.hide();
      Message.hideFrame();

      saveConfig();
      removeCache();
      // unregister from PLASTIC/SAMP if needed and destroy singleton
      if( Aladin.PLASTIC_SUPPORT && messagingMgrCreated )	{
         getMessagingMgr().unregister(true);
      }
      
      if( isLogging() ) {
         long t=System.currentTimeMillis();
         while( isLogging() ) {
            Util.pause(500);
            if( System.currentTimeMillis()-t>30000 ) break; // Au-del? de 30s on quitte
         }
      }


      super.stop();
   }

   /** Demarrage d'Aladin JAVA.
     * Dans le cas d'un demarrage par applet et que le parametre
     * frame=load est positionne, init se contente
     * de re-appeler Aladin JAVA avec le parametre frame=launching
     * ce qui permet de changer la page HTML qui contient le message pour
     * patienter (avec re-transmission des autres parametres)
     *
     * @see aladin.Aladin#suiteInit()
     */
    public void myInit() {
       if( SCREENSIZE==null ) SCREENSIZE = Toolkit.getDefaultToolkit().getScreenSize();

       setMacProperties();

       // set user-agent (see RFC 2616, User-Agent section)
       try {
           System.setProperty("http.agent", "Aladin/"+Aladin.VERSION);
       }
       catch(Exception e) {e.printStackTrace();}

       // a bit of magic for supporting all HTTPS connections
       Util.httpsInit();


       // Pour gerer le chargement en deux fois
       if( isApplet() ) {
          String [] var = { "-c","-rm","-server","-source","img","-preview",
                            "-fov","-aladin.resolution","-aladin.zoom","script","inFrame","from" };

          // Mode trace (pour aider au debogage)
          String trace = getParameter("-trace");
          if( trace!=null ) levelTrace=3;

          // Mode trace++ (pour aider au debogage)
          trace = getParameter("-debug");
          if( trace!=null ) levelTrace=4;
          
          // d?sactivation du lancement automatique du hub interne
          try { if( getParameter("-nohub")!=null ) NOHUB = true; } catch(Exception e) {}

          try {
            // Provenance de l'applet (si different de aladin.u-strasbg.fr)
             HOSTSERVER = getSite(getCodeBase().toString());
             if( HOSTSERVER!=null  && !HOSTSERVER.equals(ALADINMAINSITE) ) APPLETSERVER=HOSTSERVER;

             // Recuperation du CGIPATH s'il est different du getCodeBase();
             CGIPATH=getCodeBase()+"";
             try {
                String cgi = getParameter("cgi");
                if( cgi!=null ) CGIPATH="http://"+HOSTSERVER+cgi;
             } catch( Exception e3 ) {};
         } catch( Exception e1 ) {
            e1.printStackTrace();
         }

          // Recup?ration du nom du lanceur de l'applet (Simbad, VizieR, NED...)
          try { FROMDB = getParameter("from"); } catch( Exception e ) {}

          // Recup?ration d'une ?ventuelle surcharge glu
          try { GLUFILE = getParameter("-glufile"); } catch( Exception e ) {}

          // Recup?ration d'un script pass? par url
          try { SCRIPTFILE = getParameter("-scriptfile"); } catch( Exception e ) {}

          // Recup?ration du mode de d?marrage
          try {
             SCREEN = getParameter("-screen");
             flagScreen = SCREEN!=null;
          } catch( Exception e ) {}

          // Dans le cas d'une applet non sign?e,
          // on va v?rifier que le site qui a fourni l'applet Aladin est aussi un site GLU,
          // sinon il faudra demander un red?marrage sur un site Aladin complet puisque le client
          // ne supporte pas l'applet sign?e
          String load=null;
          if( isNonCertifiedApplet() ) {
             try {
                URL testGlu=new URL(CGIPATH + "/"+Glu.NPHGLU+"?J2000");
                DataInputStream dis=new DataInputStream(testGlu.openStream());
                if( !dis.readLine().startsWith("%DataTypeName") ) throw new Exception();

             } catch( Exception e) {
                System.err.println("Unsigned applet not supported for this HTTP Aladin site\n => redirection in progress...");
                load="redirect";
             }
          }

          if( load==null ) try { load = getParameter("-load"); } catch( Exception e4 ) {};

          if( !flagFrame && load!=null ) {
             if( !flagLoad ) {
                URL utest=null;
                String param="";
                String s;

                trace(1,"init loading");

                // Recopie des parametres a transmettre
                for( int i=0; i<var.length; i++ ) {
                   s = getParameter(var[i]);
                   if( s!=null ) param=param+"&"+var[i]+"="+URLEncoder.encode(s);
                }

                // Simple red?marrage ou demande de redirection
                String mode = load.equals("redirect")?"redirect":"launching";

                try {
                   utest = new URL(CGIPATH+"/nph-aladin.pl?frame="+mode+param);
                } catch( Exception eurl ) { System.out.println("Pb :"+eurl); }
                getAppletContext().showDocument(utest);
             }
             flagLoad=true;
             return;
          }
       }
       suiteInit();
    }

    /**
     * Pour supporter le lancement d'Aladin depuis une autre applet
     * via la methode launch()
     */
    @Override
    public String getParameter(String key) {
       if( extApplet!=null ) return extApplet.getParameter(key);
       return super.getParameter(key);
    }
    @Override
    public URL getCodeBase() {
       try {
         if( extApplet!=null ) return extApplet.getCodeBase();
          return super.getCodeBase();
      } catch( Exception e ) {
         return null;
      }
    }
    @Override
    public AppletContext getAppletContext() {
       if( extApplet!=null ) return extApplet.getAppletContext();
       return super.getAppletContext();
    }

    /** M?morisation du dernier r?pertoire utilis? dans Aladin */
    protected void memoDefaultDirectory(FileDialog f) { lastDir = f.getDirectory();  }
    protected void memoDefaultDirectory(String f)     { lastDir = f; }

    /** Selection du dernier r?pertoire utilis? dans Aladin */
    protected void setDefaultDirectory(FileDialog f) {
       f.setDirectory(getDefaultDirectory());
    }

    /** Selection du dernier r?pertoire utilis? dans Aladin */
    protected void setDefaultDirectory(CDSFileDialog f) {
       f.setDirectory(getDefaultDirectory());
    }


    /** R?cup?ration du r?pertoire par d?faut de l'utilisateur */
    public String getDefaultDirectory() {
       String dir=null;

       if( lastDir!=null ) dir=lastDir;
       if( dir==null ) dir=configuration.get(Configuration.DIR);

       // On va essayer le r?pertoire courant de l'utilisateur
       if( dir==null ) {
          try { dir = System.getProperty("user.dir");
          } catch( Exception e ) { dir=null; }
       }

       // On va essayer le r?pertoire HOME de l'utilisateur
       if( dir==null ) {
          try { dir = System.getProperty("user.home");
          } catch( Exception e ) { dir=null; }
       }

       // Toujours pas de r?pertoire par d?faut => on prend celui qui contient Aladin.jar
       if( dir==null ) {
          if( Aladin.HOME==null ) setAladinHome();
          dir = Aladin.HOME.substring(0,Aladin.HOME.length()-1);
       }
       return dir;
    }

    /** Compl?te le filename si n?cessaire par le r?pertoire par d?faut
     *  R??crit ?calement les URLs du type file://localhost/<path> pour qu'elles soient comprises par Java
     *
     * @param filename le nom de fichier tel que re?u par l'application
     */
    public String getFullFileName(String filename) {
       if( filename==null || filename.length()==0 ) return filename;

       if( filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://") ) return filename;

       File f;
       try {
          if( filename.startsWith("file:/")) {
             // les URLs du type file://localhost/<path> ne sont pas bien trait?es
          	 // et Java lance une exception si on cr?e un fichier du type new File(new URI("file://localhost/path"))
             int idx = filename.indexOf("file://localhost");
             if( idx>=0 ) {
             	f = new File(new URI("file://"+filename.substring(idx+16)));
             }
             else f = new File(new URI(filename));

             return f.getAbsolutePath();
          }
          else f = new File(filename);
       }
       catch(Exception e) {
           f = new File(filename);
       }

       String path = f.getParent();
       String name = f.getName();
       if( path!=null && (path.charAt(0)!='/' && path.charAt(0)!='\\' && path.indexOf(':')<0) ) { path=""; name=filename; }
       if( path==null || path.length()==0 ) path = getDefaultDirectory();
       if( path==null || path.length()==0 ) return filename;

       String s=path+ (path.endsWith(Util.FS) ? "":Util.FS) + name;
       return s;
    }

   /** Creation des fonts */
    protected void creatFonts() {
       if( BOLD!=null ) return;
       String s = "SansSerif";

       trace(1,"Creating Fonts");

       BOLD   = new Font(s,Font.BOLD,  SIZE);
       PLAIN  = new Font(s,Font.PLAIN, SIZE);
       ITALIC = new Font(s,Font.ITALIC,SIZE);
       SSIZE  = SIZE-2;
       SSSIZE  = SSIZE-1;
       SBOLD  = new Font(s,Font.BOLD,  SSIZE);
       SSBOLD = new Font(s,Font.BOLD,  SSSIZE);
       SPLAIN = new Font(s,Font.PLAIN, SSIZE);
       SSPLAIN= new Font(s,Font.PLAIN, SSSIZE);
       SITALIC= new Font(s,Font.ITALIC,SSIZE);
       LSIZE  = SIZE+2;
       LPLAIN = new Font(s,Font.PLAIN, LSIZE);
       LBOLD  = new Font(s,Font.BOLD,  LSIZE);
       LITALIC= new Font(s,Font.ITALIC,LSIZE);
       LLITALIC= LBOLD;
       COURIER= new Font("Monospaced",Font.PLAIN,Aladin.SIZE);
       BCOURIER= new Font("Monospaced",Font.PLAIN+Font.BOLD,Aladin.SIZE);
    }

    /** Cr?ation des chaines dans la langue */
    protected void creatChaine() {
       MBGKG   = chaine.getString("MBKGD");
       MEDIT   = chaine.getString("MEDIT");
       MFILE   = chaine.getString("MFILE");
       MSAVE   = chaine.getString("MSAVE");
       MVIEW   = chaine.getString("MVIEW");
       MIMAGE  = chaine.getString("IMAGE");
       MCATALOG= chaine.getString("VZCAT");
       MOVERLAY= chaine.getString("MOVERLAY");
       OPENFILE= chaine.getString("MOPENFILE");
       OPENLOAD= chaine.getString("MOPENLOAD");
       LASTFILE=chaine.getString("MLASTFILE");
       OPENURL = chaine.getString("MOPENURL");
       LOADIMG = chaine.getString("MLOADIMG");
       LOADCAT = chaine.getString("MLOADCAT");
       LOADVO  = chaine.getString("MLOADVO");
//       HISTORY = chaine.getString("HISTORY");
       LOADFOV = chaine.getString("MLOADFOV");
       PIXEL   = chaine.getString("MPIXEL");
       CONTOUR = chaine.getString("MCONTOUR");
       GRID    = chaine.getString("VWMGRID");
       RETICLE = chaine.getString("VWMRETICLE");
       RETICLEL= chaine.getString("VWMRETICLEL");
       NORETICLE=chaine.getString("VWMNORETICLE");
       TARGET =  chaine.getString("VWMTARGET");
       OVERLAY = chaine.getString("VWMSCALE");
       RAINBOW = chaine.getString("MRAINBOW");
       DEL     = chaine.getString("MDEL");
       DELALL  = chaine.getString("MDELALL");
       PROP    = chaine.getString("MPROP");
       ZOOMP   = chaine.getString("MZOOMP");
       ZOOMM   = chaine.getString("MZOOMM");
       ZOOM    = chaine.getString("MZOOM");
       ZOOMPT  = chaine.getString("MZOOMPT");
       COPIER  = chaine.getString("MCOPYALL");
       COLLER  = chaine.getString("MPASTEALL");
       PREVPOS = chaine.getString("MPREVPOS");
       NEXTPOS = chaine.getString("MNEXTPOS");
       SYNC    = chaine.getString("MSYNC");
       SYNCPROJ= chaine.getString("MSYNCPROJ");
       LOCKVIEW   = aladin.chaine.getString("VWMNEWROI");
       DELLOCKVIEW   = aladin.chaine.getString("VWMDELROI");
       STICKVIEW = BETAPREFIX+aladin.chaine.getString("VWMSTICKON");
       PAN     = chaine.getString("MPAN");
       RSAMP   = chaine.getString("MRSAMP");
       GLASS   = chaine.getString("MGLASS");
       GLASSTABLE   = chaine.getString("MGLASSTABLE");
       RGB     = chaine.getString("MRGB");
       MOSAIC  = chaine.getString("MMOSAIC");
       BLINK   = chaine.getString("MBLINK");
       GREY    = chaine.getString("SLMGREY");
       SELECT  = chaine.getString("SLMSELECT");
       SELECTTAG=chaine.getString("SELECTTAG");
       TAGSELECT=chaine.getString("TAGSELECT");
       DETAG   = chaine.getString("DETAG");
       SEARCH  = chaine.getString("MSEARCH");
       SELECTALL=chaine.getString("MSELECTALL");
       UNSELECT= chaine.getString("MUNSELECT");
       FILTERB = chaine.getString("MFILTERB");
       FILTER  = chaine.getString("SLMFILTER");
       PANEL   = chaine.getString("MPANEL");
       PANEL1  = chaine.getString("MPANEL1");
       PANEL2C = chaine.getString("MPANEL2");
       PANEL2L  = chaine.getString("MPANEL2L");
       PANEL4  = chaine.getString("MPANEL4");
       PANEL9  = chaine.getString("MPANEL9");
       PANEL16 = chaine.getString("MPANEL16");
       DIST    = chaine.getString("MDIST");
       NTOOL   = chaine.getString("MTOOL");
       DRAW    = chaine.getString("MDRAW");
       PHOT    = chaine.getString("MPHOT");
       TAG     = chaine.getString("MTAG");
       CUT     = chaine.getString("MCUT");
       STATSURF= chaine.getString("MSTATSURF");
       STATSURFCIRC= chaine.getString("MSTATSURFCIRC");
       STATSURFPOLY= chaine.getString("MSTATSURFPOLY");
       TRANSP  = chaine.getString("MTRANSP");
       TRANSPON= chaine.getString("MTRANSPON");
       CROP    = chaine.getString("VWMCROP1");
       HPXGENERATE = chaine.getString("HPXGENERATE");
       FOVEDITOR = chaine.getString("FOVEDITOR");
       HPXCREATE=chaine.getString("HPXCREATE");
       HPXGRID  =chaine.getString("HPXGRID");
       COPY     = chaine.getString("MCOPY");
       TABLEINFO= chaine.getString("VWTABLEINFO");
       CLONE   = chaine.getString("VWCPLANE");
       CLONE1   = chaine.getString("VWCPLANEUNIQ");
       CLONE2   = chaine.getString("VWCPLANEMULTI");
       PLOTCAT  = chaine.getString("VWPLOTCAT");
       CONCAT  = chaine.getString("VWCONCAT");
       CONCAT1 = chaine.getString("VWCONCATUNIQ");
       CONCAT2 = chaine.getString("VWCONCATMULTI");
       SAVEVIEW= chaine.getString("MSAVEVIEW");
       EXPORTEPS=chaine.getString("MEXPORTEPS");
       EXPORT  = chaine.getString("MEXPORT");
       BACKUP  = chaine.getString("MBACKUP");
       FOLD    = chaine.getString("SLMCREATFOLD");
       INFOLD  = chaine.getString("SLMINSFOLD");
       ARITHM  = chaine.getString("MARITHM");
       MOC    =  chaine.getString("MMOC");
       MOCGENIMG   =chaine.getString("MMOCGENIMG");
       MOCGENIMGS  =chaine.getString("MMOCGENIMGS");
       MOCGENCAT   =chaine.getString("MMOCGENCAT");
       MOCM     =chaine.getString("MMOCOP");
       MOCTOORDER     =chaine.getString("MMOCTOORDER");
       MOCFILTERING =chaine.getString("MMOCFILTERING");
       MOCCROP =chaine.getString("MMOCCROP");
       MOCHELP =chaine.getString("MMOCHELP");
       MOCLOAD =chaine.getString("MMOCLOAD");
       MOCHIPS =chaine.getString("MMOCHIPS");
       HEALPIXARITHM = PROTOPREFIX + chaine.getString("MHEALPIXARITHM");
//       ADD     = chaine.getString("MADD");
//       SUB     = chaine.getString("MSUB");
//       MUL     = chaine.getString("MMUL");
//       DIV     = chaine.getString("MDIV");
       NORM    = chaine.getString("MNORM");
       BITPIX  = chaine.getString("MBITPIX");
       PIXEXTR = chaine.getString("MPIXEXTR");
       CONV    = chaine.getString("MCONV");
       HEAD    = chaine.getString("MHEAD");
       FLIP    = chaine.getString("PROPFLIPFLOP");
       TOPBOTTOM = chaine.getString("PROPTOPBOTTOM");
       RIGHTLEFT = chaine.getString("PROPRIGHTLEFT");
       MDOC    = chaine.getString("MDOC");

       MTOOLS = chaine.getString("MTOOLS");
       MPLUGS = chaine.getString("MPLUGS");
       MINTEROP = chaine.getString("MINTEROP");
       MHELP  = chaine.getString("MHELP");
       MDCH1  = chaine.getString("MDCH1");
       MDCH2  = chaine.getString("MDCH2");
       MPRINT = chaine.getString("MPRINT");
       MQUIT  = chaine.getString("MQUIT");
       MCLOSE = chaine.getString("MCLOSE");
       CMD    = chaine.getString("CMD");
       MBKM   = chaine.getString("MBKM");
       XMATCH = chaine.getString("SLMXMATCH");
       CALIMG = chaine.getString("CALIMG");
       CALCAT = chaine.getString("CALCAT");
       ADDCOL = chaine.getString("SLMNEWCOL");
       ROI    = chaine.getString("ROI");
       SESAME = chaine.getString("SESAME");
       COOTOOL= chaine.getString("COOTOOL");
       PIXELTOOL= chaine.getString("PIXELTOOL");
       CALCULATOR= chaine.getString("CALCULATOR");
       SIMBAD = chaine.getString("SIMBAD");
       VIZIERSED = chaine.getString("VIZIERSED");
       AUTODIST = chaine.getString("AUTODIST");
//       TIP    = chaine.getString("TIP");
//       MSCROLL= chaine.getString("MSCROLL");
       VOTOOL = chaine.getString("VOTOOL");
       PREF   = chaine.getString("PREF");
       NEW    = NOAPPLETPREFIX+chaine.getString("NEW");
//       CEA_TOOLS = chaine.getString("CEA_TOOLS");
       MACRO  = chaine.getString("MACRO");
       TUTO   = chaine.getString("TUTO");
       HELP   = chaine.getString("HELP");
       HELPSCRIPT = chaine.getString("HELPSCRIPT");
       FAQ    = chaine.getString("FAQ");
       MAN    = chaine.getString("MAN");
       TUTORIAL= chaine.getString("TUTORIAL");
       SENDBUG = chaine.getString("SENDBUG");
       PLUGINFO = chaine.getString("PLUGINFO");
       VOINFO = chaine.getString("VOTOOLINFO");
       GLUTOOL = chaine.getString("GLUTOOL");
       NEWS   = chaine.getString("NEWS");
       ABOUT  = chaine.getString("ABOUT");
       FULLSCREEN = chaine.getString("FULLSCREEN");
       PREVIEWSCREEN = chaine.getString("PREVIEWSCREEN");
       MOREVIEWS = chaine.getString("VWMOREVIEWS");
       ONEVIEW = chaine.getString("VWONEVIEW");
       NEXT = chaine.getString("VWNEXT");
       FULLINT = chaine.getString("VWFULLINT");
       NORTHUP = chaine.getString("VWNORTHUP");
       GETOBJ =    chaine.getString("GETOBJ");


       // les chaines pour SAMP
       String name = getMessagingMgr().getProtocolName();

       REGISTER = chaine.getString("PWREGISTER").replaceAll("SAMP", name);
       UNREGISTER = chaine.getString("PWUNREGISTER").replaceAll("SAMP", name);
       BROADCAST = chaine.getString("PWBROADCAST");
       BROADCASTIMAGE = chaine.getString("SLMBDCASTIMAGES");
       BROADCASTTABLE = chaine.getString("SLMBDCASTTABLES");
       SAMPPREFS = chaine.getString("PWPREFS").replaceAll("SAMP", name);
       STARTINTERNALHUB = chaine.getString("PWSTARTINTERNALHUB");
       STOPINTERNALHUB = chaine.getString("PWSTOPINTERNALHUB");
    }

    /** Cr?ation du menu principal sous la forme d'un tableau ? trois dimensions permettant
     * deux sous-niveaux de menus
     * voir createJBar();
     */
    protected String[][][] createMenu() {
       // TODO : je n'obtiens pas ce que je veux sous Mac ...
       String meta = macPlateform?"meta":"ctrl";
       String alt = macPlateform?"meta shift":"alt";

       // TODO : meta ne fonctionne pas sous windows
       if( OUTREACH ) {
          return new String[][][]{
          { {MFILE},
             {OPENLOAD+"|"+meta+" L"},{OPENFILE+"|"+meta+" O"},
             {},{LOADIMG,"-"},{LOADCAT,"-"},{BACKUP},{MPRINT+"|"+meta+" P"},
             {},{ extApplet!=null ? MCLOSE : isApplet()?MDCH1: MQUIT}
        },
        { {MEDIT},
             {"?"+PAN+"|"+alt+" Z"},
                 {ZOOM,"?"+ZOOMPT+"|F6","",ZOOMM+"|F7",ZOOMP+"|F8"},
             {},{COPIER+"|"+meta+" C"},{COLLER+"|"+meta+" V"},
             {},{SELECTALL+"|"+meta+" A"},{UNSELECT+"|"+meta+" U"},
             {},{DEL+"|DELETE"},{DELALL+"|shift DELETE"},
             {},{HEAD+"|"+alt+" H"},{PROP+"|"+alt+" ENTER"}, {}, {PREF},
        },
        { {MIMAGE},
           {PIXEL+"|"+meta+" M"},{"?"+GLASS+"|"+meta+" G"},
//           {},{TRANSP},
           {},{RGB},{GREY},{BLINK},
           {},{CALIMG},
           {},{FLIP,TOPBOTTOM,RIGHTLEFT},
        },
        { {MCATALOG},
           {XMATCH},{ADDCOL},{FILTERB,"-"},
           {},{CLONE},
        },
        { {MOVERLAY},
           {CONTOUR},
           {},{DIST+"|"+alt+" D"},{PHOT},{DRAW},{TAG},
           {},{"?"+GRID+"|"+alt+" G"},{"?"+OVERLAY+"|"+alt+" O"},
        },
        { {MTOOLS},
           {SESAME+"|"+meta+" R"},{VOTOOL,VOINFO},
        },
        { {MVIEW},
           {"?"+FULLSCREEN+"|F11"}, {PREVIEWSCREEN+"|F12"}, {NEXT+"|TAB"},
           {},{MOREVIEWS+"|F9"}, {"?"+LOCKVIEW},
        },
        { {MHELP},
             {HELP},{ABOUT},
        },
  };

       }

       
       String[][][] menu = new String[][][] {
             { {MFILE},
                  {OPENLOAD+"|"+meta+" L"},{OPENFILE+"|"+meta+" O"},{OPENURL},
                  {LASTFILE,"???"},
                  {},{MBGKG,"???"},
                  {},{LOADIMG,"-"},{LOADCAT,"-"}, {LOADVO}, {LOADFOV},
                  {},{MSAVE+"|"+meta+" S"},{SAVEVIEW,"-"},{EXPORTEPS},{EXPORT},{BACKUP},
//                  {},{HISTORY+"|"+(macPlateform?alt:meta)+" H"},
                  {},{MPRINT+"|"+meta+" P"},
                  {},{NEW+"|"+meta+" N"},
                  {},{aladinSession>0 || extApplet!=null ? MCLOSE : isApplet()?MDCH1: MQUIT}
             },
             { {MEDIT},
                  {"?"+PAN+"|"+alt+" Z"},
                      {ZOOM,ZOOMM+"|F2",ZOOMP+"|F3","","?"+ZOOMPT+"|F4"},
                  {},{COPIER+"|"+meta+" C"},{COLLER+"|"+meta+" V"},
                  {},{FOLD},{INFOLD},
                  {},{SELECTALL+"|"+meta+" A"},{SELECT},{SELECTTAG},{UNSELECT+"|"+meta+" U"},
                  /*{},{TAGSELECT},*/{DETAG},
                  {},{DEL+"|DELETE"},{DELALL+"|shift DELETE"},
                  {},{HEAD+"|"+alt+" H"},{PROP+"|"+alt+" ENTER"}, {}, {PREF},
             },
             { {MIMAGE},
                {PIXEL+"|"+meta+" M"},{"?"+GLASS+"|"+meta+" G"},{"?"+GLASSTABLE},
                {},{STATSURF, STATSURFCIRC, STATSURFPOLY},{CUT},
//                {},{TRANSP},{"?"+TRANSPON},
                {},{RGB},{GREY},{MOSAIC},{BLINK},
                {},{RSAMP},{CALIMG},
                {},{FLIP,TOPBOTTOM,RIGHTLEFT},{ARITHM},{HEALPIXARITHM},{CONV},{NORM},{BITPIX},{PIXEXTR},
                {},{COPY},{CROP},
             },
             { {MCATALOG},
                {PLOTCAT},{XMATCH},{ADDCOL},
                {},{SEARCH+"|"+meta+" F"},
                {},{FILTER},{FILTERB,"-"},
                {},
                {},{CONCAT,CONCAT1,CONCAT2},{CLONE,CLONE1,CLONE2},
                {},{TABLEINFO},
             },
             { {MOVERLAY},
                {CONTOUR},
//                {MOC,MOCGEN,MOCFILTERING,MOCCROP,MOCM},
                {},{DIST+"|"+alt+" D"},{PHOT},{DRAW},{TAG},
                {},{NTOOL+"|"+alt+" N"},
                {},{"?"+OVERLAY+"|"+alt+" O"},{"?"+RAINBOW+"|"+alt+" R"},{"?"+TARGET+"|"+alt+" T"},
                   {"?"+GRID+"|"+alt+" G"},/*{"?"+HPXGRID+"|"+(macPlateform?"meta shift":"alt")+" W"},*/
                {},{"%"+RETICLE},{"%"+RETICLEL},{"%"+NORETICLE},
             },
             { {MOC},
                {MOCHIPS},{MOCLOAD},{MOCGENCAT},{MOCGENIMG},{MOCGENIMGS},{},{MOCM},{MOCTOORDER},{},{MOCFILTERING},{MOCCROP},{MOCHELP}
             },
             { {MTOOLS},
                {SESAME+"|"+meta+" R"},{COOTOOL},{PIXELTOOL},{CALCULATOR},
                {},{"?"+SIMBAD},{"?"+VIZIERSED},{"?"+AUTODIST},/*{"?"+TIP},{"?"+MSCROLL},{CEA_TOOLS},*/
                {}, {ROI}, {MBKM},{CMD+"|F5"},{MACRO},
                {},{VOTOOL,VOINFO}, {GLUTOOL,"-"}, {MPLUGS,PLUGINFO},
                {},{HPXGENERATE},{HPXCREATE},
                   { PROTOPREFIX+"HEALPix mouse control","%No mouse NSIDE control","%Mouse NSIDE 2^0","%Mouse NSIDE 2^1","%Mouse NSIDE 2^2","%Mouse NSIDE 2^3","%Mouse NSIDE 2^4","%Mouse NSIDE 2^5","%Mouse NSIDE 2^6",
                   "%Mouse NSIDE 2^7","%Mouse NSIDE 2^8","%Mouse NSIDE 2^9","%Mouse NSIDE 2^10","%Mouse NSIDE 2^11",
                   "%Mouse NSIDE 2^12","%Mouse NSIDE 2^13","%Mouse NSIDE 2^14","%Mouse NSIDE 2^15","%Mouse NSIDE 2^16",
                   "%Mouse NSIDE 2^17","%Mouse NSIDE 2^18","%Mouse NSIDE 2^19","%Mouse NSIDE 2^20","%Mouse NSIDE 2^21",
                   "%Mouse NSIDE 2^22","%Mouse NSIDE 2^23","%Mouse NSIDE 2^24","%Mouse NSIDE 2^25","%Mouse NSIDE 2^26",
                   "%Mouse NSIDE 2^27","%Mouse NSIDE 2^28","%Mouse NSIDE 2^29",},
                {},{FOVEDITOR},

                {JUNIT},
             },
             { {MVIEW},
                {FULLSCREEN+"|F11"}, {PREVIEWSCREEN+"|F12"}, {NEXT+"|TAB"},
                {},{PANEL,"%"+PANEL1+"|shift F1","%"+PANEL2C,"%"+PANEL2L,
                   "%"+PANEL4+"|shift F2","%"+PANEL9+"|shift F3","%"+PANEL16+"|shift F4"},
                {},{MOREVIEWS+"|F9"},{ONEVIEW}, {DELLOCKVIEW}, {"?"+LOCKVIEW},
//                {},{"?"+LOCKVIEW},{DELLOCKVIEW}, 
                {},{"?"+STICKVIEW},
                {},{"?"+NORTHUP+"|"+alt+" X"},{"?"+SYNC+"|"+alt+" S"},{"?"+SYNCPROJ+"|"+alt+" Q"},
             },
             { {MHELP},
                  {HELP+"|F1"}, 
//                                {TUTO, "Show me how to load an image",
//                                 "Show me how to display catalogs on an image",
//                                 "Show me how to play with the Aladin stack",
//                                 "Show me how to use the multiview mode",
//                                 "Show me how to do a contour",
//                                 "Show me how to control the image contrast",
//                                 "Show me how to create a colored image",
//                                 "What is a filter",
//                                 "Show me how to play with the metadata lists and trees"},
                  {MDOC,FAQ,TUTORIAL,MAN},
                  {},{HELPSCRIPT+"|"+(macPlateform?alt:meta)+" F5"},
                  {},{SENDBUG}, {NEWS}, {ABOUT}
             },
       };

       // ajout menu interop
       if( PLASTIC_SUPPORT ) {
          String[][][] retMenu = new String[menu.length+1][][];
          for( int i=0; i<menu.length-1; i++ ) {
             retMenu[i] = menu[i];
          }
          if( Aladin.BETA ) {
             retMenu[retMenu.length-2] = new String[][] { {MINTEROP},
                   {REGISTER}, {UNREGISTER},
                   {}, {STARTINTERNALHUB}, {STOPINTERNALHUB},
                   {}, {BROADCAST}, {BROADCASTIMAGE, "-"}, {BROADCASTTABLE, "-"},
                   {}, {SAMPPREFS}
             };
          }
          else {
             retMenu[retMenu.length-2] = new String[][] { {MINTEROP},
                   {REGISTER}, {UNREGISTER},
                   {}, {BROADCAST}, {BROADCASTIMAGE, "-"}, {BROADCASTTABLE, "-"},
                   {}, {SAMPPREFS}
             };
          }

          retMenu[retMenu.length-1] = menu[menu.length-1];
          return retMenu;
       }
       else {
           return menu;
       }

   }

    /** Retourne true si la barre de menu et/ou les ComboBox de
     * localisation et de pixel sont d?roul? et cachent une partie
     * de la zone des vues (voir ViewSimple.mouseEntered()
     * et ViewSimple.mouseExited())
     */
    protected boolean menuActivated() {
       int n = jBar.getMenuCount();
       for( int i=0; i<n; i++ ) {
          JMenu jm = jBar.getMenu(i);
          if( jm!=null && jm.isPopupMenuVisible() ) return true;
       }
//       if( pixel.isPopupVisible() ) return true;
       if( localisation.isPopupVisible() ) return true;

       return false;
    }
    
    protected void memoLastFile(String path) {
       if( NOGUI ) return;
       configuration.setLastFile(path, true);
       updateLastFileMenu();
    }
    
    /** Met ? jour le menu des fichiers r?cemment ouverts */
    protected void updateLastFileMenu() {
       if( miLastFile==null ) return;
       if( configuration.lastFile==null ) {
          miLastFile.setEnabled(false);
          return;
       }
       miLastFile.setEnabled(configuration.lastFile.size()>0);
       miLastFile.removeAll();
       JMenuItem item;
       Iterator<String> it = configuration.lastFile.descendingIterator();
       while( it.hasNext() ) {
          String a = it.next();
          miLastFile.add(item = new JMenuItem( Util.getShortPath(a,50)));
          item.setActionCommand(a);
          item.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               String filename = e.getActionCommand();
               calque.newPlan(filename, null, null);
            }
         });
       }
       miLastFile.add(item = new JMenuItem( chaine.getString("MLASTFILECLEAR")));
       item.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             configuration.lastFile=null;
             updateLastFileMenu();
          }
       });
    }
    
    /**
     * Met ? jour le menu Interop
     */
    private void updateInteropMenu() {
        AppMessagingInterface mgr = getMessagingMgr();

        boolean isRegistered = mgr.isRegistered();
        miSAMPRegister.setEnabled(! isRegistered);
        miSAMPUnregister.setEnabled(isRegistered);

        if( Aladin.PROTO ) {
            boolean plaskitRunning = mgr.internalHubRunning();
            miSAMPStartHub.setEnabled(!plaskitRunning);
            miSAMPStopHub.setEnabled(plaskitRunning);
        }

        ArrayList<String> imgApps = mgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS);
        ArrayList<String> tabApps = mgr.getAppsSupporting(AppMessagingInterface.ABSTRACT_MSG_LOAD_VOT_FROM_URL);

        int nbCatalog=0;
        int nbImg=0;
        Plan [] plan = aladin.calque.getPlans();
        for( int i=0; i<plan.length; i++ ) {
            Plan pc = plan[i];
            if( !pc.selected ) continue;
            if( pc.isCatalog() && pc.flagOk ) nbCatalog++;
            if( pc.type==Plan.IMAGE && pc.flagOk ) nbImg++;
            if( pc.type==Plan.IMAGEHUGE && pc.flagOk ) nbImg++;
        }

        JMenuItem item;
        // ajout des applis pouvant recevoir des images
        miBroadcastImgs.removeAll();
        miBroadcastImgs.add(item = new JMenuItem(calque.select.MALLAPPS));
        item.setActionCommand(BROADCASTIMAGE);
        item.addActionListener(this);
        ((JMenu)miBroadcastImgs).addSeparator();

        for (String app : imgApps) {
            miBroadcastImgs.add(item = new JMenuItem(app));
            item.setActionCommand(BROADCASTIMAGE);
            item.addActionListener(this);
        }

        // ajout des applis pouvant recevoir des tables
        miBroadcastTables.removeAll();
        miBroadcastTables.add(item = new JMenuItem(calque.select.MALLAPPS));
        item.setActionCommand(BROADCASTTABLE);
        item.addActionListener(this);
        ((JMenu)miBroadcastTables).addSeparator();
        for (String app: tabApps) {
            miBroadcastTables.add(item = new JMenuItem(app));
            item.setActionCommand(BROADCASTTABLE);
            item.addActionListener(this);
       }

        boolean canBroadcast = isRegistered && (nbCatalog>0 || nbImg>0);;

        miBroadcastAll.setEnabled( canBroadcast && (imgApps.size()>0 || tabApps.size()>0));

        miBroadcastImgs.setEnabled(isRegistered && nbImg>0 && imgApps.size()>0);
        miBroadcastTables.setEnabled(isRegistered && nbCatalog>0 && tabApps.size()>0);
    }

    /** Creation d'un JMenuBar en fonction d'un tableau ? 3 dimensionspermettant
     * deux sous-niveaux de menus.
     * - une dimension vide au niveau 1, ou une chaine vide au niveau 2 donne lieu ? un S?parateur
     * - un menu qui commence par "?" va donner lieu ? une JCheckboxMenuItem
     * - un menu qui commence par "%" var donner lieu ? un JRadioButtonMenuItem
     * { {Niveau0-A}, {Niveau1-A}, {}, {Niveau1-B,"",Niveau2-A}, {Niveau1-C} },
     * { {Niveau0-B}, ...
     */
    protected JMenuBar createJBar(String menu[][][]) {
       jBar = new JMenuBar();
       boolean separator=false;     // pour ?viter de s?paration de suite

       for( int i=0; i<menu.length; i++ ) {
          String s=menu[i][0][0];
          if( (s = isSpecialMenu(s))==null ) continue;
          JMenu jm = new JMenu(s);
          if (s.equals(MINTEROP)) {
                jm.addMenuListener(new MenuListener() {
                    public void menuSelected(MenuEvent e) {
                        updateInteropMenu();
                    }
                    public void menuCanceled(MenuEvent e) {}
                    public void menuDeselected(MenuEvent e) {}
                });
            }
          JMenuItem ji;
          ButtonGroup mg=null;

          memoMenuItem(s,jm);

          for( int j=1; j<menu[i].length; j++ ) {
             if( menu[i][j].length==0 ) { if( !separator ) { jm.addSeparator(); separator=true; } continue; }
             s=menu[i][j][0];
             if( (s = isSpecialMenu(s))==null ) continue;

             if( menu[i][j].length>1 ) {
                JMenu jms = new JMenu(s);
                memoMenuItem(s,jms);
                for( int k=1; k<menu[i][j].length; k++ ) {
                   s=menu[i][j][k];
                   if( (s = isSpecialMenu(s))==null ) continue;
                   if( s.length()==0 ) { if( !separator ) { jms.addSeparator(); separator=true; } continue; }
                   StringBuffer key = new StringBuffer();
                   s = hasKeyStroke(key,s);
                   if( s.charAt(0)=='%' ) {
                      ji = new JRadioButtonMenuItem(s=s.substring(1));
                      if( mg==null ) { mg = new ButtonGroup(); ji.setSelected(true); }
                      mg.add(ji);
                   } else {
                      ji = s.charAt(0)=='?' ? new JCheckBoxMenuItem(s=s.substring(1)) : new JMenuItem(s);
                      if( jms.getText().equals(TUTO) ) ji.setActionCommand(TUTO);
                      mg=null;
                   }
                   if( key.length()>0 ) {
                      ji.setAccelerator(KeyStroke.getKeyStroke(key.toString()));
                   }
                   ji.addActionListener(this);
                   memoMenuItem(s,ji);
                   separator=false;
                   jms.add(ji);
                }
                jm.add(jms);
             } else {
                StringBuffer key = new StringBuffer();
                s = hasKeyStroke(key,s);
                if( s.charAt(0)=='%' ) {
                   ji = new JRadioButtonMenuItem(s=s.substring(1));
                   if( mg==null ) { mg = new ButtonGroup(); ji.setSelected(true); }
                   mg.add(ji);
                } else {
                   ji = s.charAt(0)=='?' ? new JCheckBoxMenuItem(s=s.substring(1)) : new JMenuItem(s);
                   mg=null;
                }
                if( key.length()>0 ) {
                   ji.setAccelerator(KeyStroke.getKeyStroke(key.toString()));
                }
                ji.addActionListener(this);
                memoMenuItem(s,ji);
                separator=false;
                jm.add(ji);
             }
          }
          jBar.add(jm);
       }

       jBar.add(javax.swing.Box.createGlue());
       JButton b;

       // Si applet, ajout d'un bouton tout ? droite pour proposer l'installation
       if( isApplet() ) {
          bDetach = b=new JButton(MDCH1);
          b.setBorderPainted(false);
          b.setContentAreaFilled(false);
          b.addMouseListener(new MouseAdapter() {
             @Override
            public void mouseEntered(MouseEvent e) {
                ((JButton)e.getSource()).setForeground(Color.blue);
             }
             @Override
            public void mouseExited(MouseEvent e) {
                ((JButton)e.getSource()).setForeground(Color.black);
             }
          });
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                if( ! flagDetach ) detach();
                else unDetach();
             }
          });
          jBar.add(b);

          b=new JButton(chaine.getString("MINSTALL"));
          b.setBorderPainted(false);
          b.setContentAreaFilled(false);
          b.addMouseListener(new MouseAdapter() {
             @Override
            public void mouseEntered(MouseEvent e) {
                ((JButton)e.getSource()).setForeground(Color.blue);
             }
             @Override
            public void mouseExited(MouseEvent e) {
                ((JButton)e.getSource()).setForeground(Color.black);
             }
          });
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                install();
             }
          });
          jBar.add(b);
          
          jBar.add(javax.swing.Box.createGlue());
       }

       try {
          b = new JButton(new ImageIcon(aladin.getImagette("Preview.gif")));
          b.setMargin(new Insets(0,0,0,0));
          b.setToolTipText(PREVIEWSCREEN);
          b.setBorderPainted(false);
          b.setContentAreaFilled(false);
          b.addActionListener( new ActionListener() {
             public void actionPerformed(ActionEvent e) {  fullScreen(1); }
          });
          if( !isApplet() ) jBar.add(b);

          b = new JButton(new ImageIcon(aladin.getImagette("Fullscreen.gif")));
          b.setMargin(new Insets(0,0,0,0));
          b.setToolTipText(FULLSCREEN);
          b.setBorderPainted(false);
          b.setContentAreaFilled(false);
          b.addActionListener( new ActionListener() {
             public void actionPerformed(ActionEvent e) {  fullScreen(0); }
          });
          if( !isApplet() ) jBar.add(b);
       } catch( Exception e ) { if( levelTrace>=3 ) e.printStackTrace(); }


       // Chargement des plugins ?ventuels
       if( !NOPLUGIN && !isApplet() && !ISJNLP ) {
          (new Thread("plugin search"){
             @Override
            public void run() { pluginReload();}
          }).start();
       }

       // Pour les Cieux
       gluSkyReload();

       // Pour les applications VO
       VOReload();
       
       // Pour les fichiers r?cents
       updateLastFileMenu();

       // Ajout des formats de sauvegarde support?s
       if( miSave!=null ) {
          miSave.removeAll();
          appendJMenu((JMenu)miSave,Save.getFormatMenu());
       }

//       // Ajout des kernels de convolution
//       if( miConv!=null ) {
//          new Kernel();
//          miConv.removeAll();
//          appendJMenu((JMenu)miConv,Kernel.getKernelMenu());
//       }

       // Chargement des filtres pr?d?finis
       if( miFilterB!=null ) {
          miFilterB.removeAll();
          appendJMenu((JMenu)miFilterB,FilterProperties.getBeginnerFilters());
       }

       return jBar;
    }

    /** Retourne le path ? la profondeur prof, ou null si impossible */
    private String prefixMenu(String pathMenu,int prof) {
       int pos=0;
       for( int i=0; i<prof-1; i++) pos = indexOfSlash(pathMenu,pos)+1;
       int npos = indexOfSlash(pathMenu,pos);
       if( npos==-1 ) return null;
       return unBackSlash( pathMenu.substring(0,npos) );
    }

    /** Retourne l'indice du prochain / ? partir de la position pos.
     * Ne prend pas en compte les / pr?c?d? de \ */
    private int indexOfSlash(String s, int pos ) {
       pos--;
       do { pos = s.indexOf('/',pos+1); }
       while( pos>0 && s.charAt(pos-1)=='\\' );
       return pos;
    }

    /** Retourne true si le path ? la profondeur prof n'est pas termin? */
    private boolean hasSubMenu(String pathMenu,int prof) { return prefixMenu(pathMenu,prof)!=null; }

    /** Dernier terme du path. ON doit prendre garde au / pr?fix? par \ qui ne
     * comptent pas */
    private String suffixMenu(String pathMenu) {
       int pos=pathMenu.length()+1;
       do { pos= pathMenu.lastIndexOf('/',pos-1); }
       while( pos>0 && pathMenu.charAt(pos-1)=='\\');
       if( pos<0 ) return pathMenu;
       return unBackSlash(pathMenu.substring(pos+1));
    }

    /** Supprime les \ qui pr?c?dent les / */
    private String unBackSlash(String s) {
       int n;
       StringBuffer res = new StringBuffer(n=s.length());
       for( int i=0; i<n; i++ ) {
          if( s.charAt(i)=='\\' && i<n-1 && s.charAt(i+1)=='/' ) continue;
          res.append(s.charAt(i));
       }
       return res.toString();
    }

    /** Cr?ation r?cursive des sous-menus (utilis? par appendJMenu() */
    private JMenu createJMenu1(String []sm, int index, int prof) {
       String name = prefixMenu(sm[index],prof);
       JMenu jm = new JMenu(suffixMenu(name)+"...");
       JMenuItem ji;
       ButtonGroup mg = null;

       for(int i=index; i<sm.length; i++ ) {
          if( sm[i]==null ) continue;             // d?j? trait?
          if( !sm[i].startsWith(name+"/") ) continue; // pas concern?
          if( hasSubMenu(sm[i],prof+1) ) {
             jm.add(createJMenu1(sm,i,prof+1) );
          }
          else {
             String s = suffixMenu(sm[i]);
             if( s.charAt(0)=='%' ) {
                ji = new JRadioButtonMenuItem(s=s.substring(1));
                if( mg==null ) { mg = new ButtonGroup(); ji.setSelected(true); }
                mg.add(ji);
             } else ji = new JMenuItem(s);
             ji.addActionListener(this);
             jm.add(ji);
             sm[i]=null;
          }
       }
       return jm;
    }

    /** Ajout au menu pass? en param?tre de la liste des chaines indiqu?es
     * en param?tre. Les menus peuvent ?tre r?cursifs si ils sont construits
     * avec un path. Exemple: Catalog/Browser. Les / qui ne d?crivent pas
     * la hi?rarchie doivent ?tre pr?c?d?s de \. Les menus pr?c?d?s du
     * caract?re % donnera lieu ? un JRadioMenu */
    private JMenu appendJMenu(JMenu jm,String []SM) {
       JMenuItem ji;
       ButtonGroup mg=null;

       // La copie est indispensable car on va utiliser le tableau pour "marquer"
       // les items d?j? trait?s
       String sm[] = new String[SM.length];
       System.arraycopy(SM,0,sm,0,SM.length);

       for( int k=0; k<sm.length; k++ ) {
          if( sm[k]==null ) continue;
          String s = new String(sm[k]);

          if( hasSubMenu(s,1) ) jm.add(createJMenu1(sm,k,1));
          else {
             if( s.charAt(0)=='%' ) {
                ji = new JRadioButtonMenuItem(s=s.substring(1));
                if( mg==null ) { mg = new ButtonGroup(); ji.setSelected(true); }
                mg.add(ji);
             } else  {
            	 ji = new JMenuItem( unBackSlash(s) );
            	 // ajout raccourci ctrl-I pour acc?s serveur Aladin
            	 if( s.equals(ALADIN_IMG_SERVER) ) {
            		 ji.setAccelerator(KeyStroke.getKeyStroke(
            			        KeyEvent.VK_I, macPlateform?ActionEvent.META_MASK:ActionEvent.CTRL_MASK));
            	 }
             }
             ji.addActionListener(this);
             jm.add(ji);
          }

       }
       return jm;
    }


    /** Mise en forme d'un sous-menu muni d'une extension pour d?crire un touche d'acc?l?ration
     * Celle-ci est pr?fix?e par le caract?re r?serv? '|'
     * ex : "Copy|ctrl c"
     * @param key Retourne la chaine d?crivant la touche d'acc?l?ration, "" si aucune
     * @param s le menu
     * @return le menu dont on a enl?v? l'acc?l?rateur,
     */
    private String hasKeyStroke(StringBuffer key,String s) {
       int i=s.lastIndexOf('|');
       if( i<0 ) return s;
       key.append( s.substring(i+1) );
       return s.substring(0,i);
    }

    /** Mise en forme des sous-menus BETA, PROTO ou OUTREACH si besoin, return null si
     * le sous-menu doit ?tre ommis */
    protected String isSpecialMenu(String sm){
       if( sm==null ) {
          try { throw new Exception(); } catch(Exception e) { e.printStackTrace(); return "XXX undefined XXX";}
       }
       if( sm.length()==0 ) return sm;
       char c=sm.charAt(0);
       boolean flagSwitch=c=='%' || c=='?';
       int len = flagSwitch ? 1:0;
       if( sm.startsWith(BETAPREFIX,len) ) {
          if( !BETA ) return null;
          return (flagSwitch?c+"":"")+sm.substring(len+BETAPREFIX.length());
       } else if( sm.startsWith(PROTOPREFIX,len) ) {
          if( !PROTO ) return null;
          return (flagSwitch?c+"":"")+sm.substring(len+PROTOPREFIX.length());
       } else if( sm.startsWith(OUTREACHPREFIX,len) ) {
          if( !OUTREACH ) return null;
          return (flagSwitch?c+"":"")+sm.substring(len+OUTREACHPREFIX.length());
       } else if( sm.startsWith(NOAPPLETPREFIX,len) ) {
          if( isApplet() ) return null;
          return (flagSwitch?c+"":"")+sm.substring(len+NOAPPLETPREFIX.length());
       }
       return sm;
    }

    /** Rep?rage des sous-menus particuliers pour d'?ventuelles modif. ult?rieures */
    private void memoMenuItem(String m,JMenuItem ji) {
       int i;

            if( isMenu(m,CALIMG))  miCalImg  = ji;
       else if( isMenu(m,CALCAT))  miCalCat  = ji;
       else if( isMenu(m,MDCH1))   miDetach  = ji;
       else if( isMenu(m,ADDCOL))  miAddCol  = ji;
       else if( isMenu(m,XMATCH))  miXmatch  = ji;
       else if( isMenu(m,SIMBAD))  miSimbad  = ji;
       else if( isMenu(m,AUTODIST))  miAutoDist  = ji;
       else if( isMenu(m,VIZIERSED))  miVizierSED  = ji;
//       else if( isMenu(m,TIP))     miTip     = ji;
       else if( isMenu(m,VOTOOL))  miVOtool  = ji;
       else if( isMenu(m,MBGKG))   miGluSky  = ji;
       else if( isMenu(m,GLUTOOL)) miGluTool = ji;
       else if( isMenu(m,ROI))     miROI     = ji;
       else if( isMenu(m,PREF))    miPref    = ji;
       else if( isMenu(m,DEL))     miDel     = ji;
       else if( isMenu(m,PROP))    miProp    = ji;
       else if( isMenu(m,DELALL))  miDelAll  = ji;
       else if( isMenu(m,PIXEL))   miPixel   = ji;
       else if( isMenu(m,CONTOUR)) miContour = ji;
       else if( isMenu(m,MPRINT))  miPrint   = ji;
       else if( isMenu(m,MSAVE))   miSaveG   = ji;
       else if( isMenu(m,FULLSCREEN))miScreen   = ji;
       else if( isMenu(m,PREVIEWSCREEN))miPScreen   = ji;
       else if( isMenu(m,MOREVIEWS))miMore   = ji;
       else if( isMenu(m,ONEVIEW)) miOne     = ji;
       else if( isMenu(m,NEXT))    miNext    = ji;
       else if( isMenu(m,LOCKVIEW))miLock    = ji;
       else if( isMenu(m,NORTHUP)) miNorthUp = ji;
       else if( isMenu(m,DELLOCKVIEW)) miDelLock= ji;
       else if( isMenu(m,STICKVIEW)) miStick = ji;
       else if( isMenu(m,GRID))    miGrid    = ji;
       else if( isMenu(m,HPXGRID)) miHpxGrid = ji;
       else if( isMenu(m,RETICLE)) miReticle = ji;
       else if( isMenu(m,RETICLEL))  miReticleL  = ji;
       else if( isMenu(m,NORETICLE)) miNoReticle = ji;
       else if( isMenu(m,TARGET))  miTarget  = ji;
       else if( isMenu(m,OVERLAY)) miOverlay = ji;
       else if( isMenu(m,RAINBOW)) miRainbow = ji;
       else if( isMenu(m,ZOOM))    miZoom    = ji;
       else if( isMenu(m,COPIER))   miCopy1    = ji;
       else if( isMenu(m,COLLER))   miPaste    = ji;
       else if( isMenu(m,ZOOMPT))  miZoomPt  = ji;
//       else if( isMenu(m,PREVPOS)) miPrevPos  = ji;
//       else if( isMenu(m,NEXTPOS)) miNextPos  = ji;
       else if( isMenu(m,SYNC))    miSync    = ji;
       else if( isMenu(m,SYNCPROJ))miSyncProj= ji;
       else if( isMenu(m,PAN))     miPan     = ji;
       else if( isMenu(m,RSAMP))   miRsamp   = ji;
       else if( isMenu(m,RGB))     miRGB     = ji;
       else if( isMenu(m,MOSAIC))  miMosaic  = ji;
       else if( isMenu(m,BLINK))   miBlink   = ji;
       else if( isMenu(m,GLASS))   miGlass   = ji;
       else if( isMenu(m,GLASSTABLE))   miGlassTable   = ji;
       else if( isMenu(m,PANEL1))  miPanel1  = ji;
       else if( isMenu(m,PANEL2C))  miPanel2c  = ji;
       else if( isMenu(m,PANEL2L))  miPanel2l  = ji;
       else if( isMenu(m,PANEL4))  miPanel4  = ji;
       else if( isMenu(m,PANEL9))  miPanel9  = ji;
       else if( isMenu(m,PANEL16)) miPanel16 = ji;
       else if( isMenu(m,LOADIMG)) miImg     = ji;
       else if( isMenu(m,OPENFILE))miOpen    = ji;
       else if( isMenu(m,LOADCAT)) miCat     = ji;
       else if( isMenu(m,MPLUGS) ) miPlugs   = ji;
       else if( isMenu(m,GREY) )   miGrey    = ji;
       else if( isMenu(m,FILTER) ) miFilter  = ji;
       else if( isMenu(m,FILTERB) )miFilterB = ji;
       else if( isMenu(m,SEARCH) ) miSearch  = ji;
       else if( isMenu(m,SELECT) ) miSelect  = ji;
       else if( isMenu(m,SELECTTAG) ) miSelectTag  = ji;
       else if( isMenu(m,TAGSELECT) ) miTagSelect  = ji;
       else if( isMenu(m,DETAG) )  miDetag   = ji;
       else if( isMenu(m,SELECTALL) ) miSelectAll = ji;
       else if( isMenu(m,UNSELECT) )  miUnSelect  = ji;
       else if( isMenu(m,CUT) )    miCut     = ji;
       else if( isMenu(m,STATSURF) ) miStatSurf     = ji;
       else if( isMenu(m,TRANSP) ) miTransp  = ji;
       else if( isMenu(m,TRANSPON) ) miTranspon  = ji;
       else if( isMenu(m,DIST) )   miDist    = ji;
       else if( isMenu(m,PHOT) )    miTag     = ji;
       else if( isMenu(m,DRAW) )   miDraw    = ji;
       else if( isMenu(m,TAG) )  miTexte   = ji;
       else if( isMenu(m,CROP) )   miCrop    = ji;
       else if( isMenu(m,HPXCREATE) ) miCreateHpx = ji;
       else if( isMenu(m,HPXDUMP) )   miHpxDump = ji;
       else if( isMenu(m,COPY) )   miCopy    = ji;
       else if( isMenu(m,TABLEINFO) ) miTableInfo = ji;
       else if( isMenu(m,CLONE) )  miClone   = ji;
       else if( isMenu(m,PLOTCAT) )  miPlotcat   = ji;
       else if( isMenu(m,CONCAT) )  miConcat   = ji;
       else if( isMenu(m,SAVEVIEW) )  miSave      = ji;
       else if( isMenu(m,LASTFILE) )  miLastFile      = ji;
       else if( isMenu(m,EXPORT) )    miExport    = ji;
       else if( isMenu(m,EXPORTEPS) ) miExportEPS = ji;
       else if( isMenu(m,BACKUP) )    miBackup    = ji;
//       else if( isMenu(m,HISTORY) )   miHistory   = ji;
       else if( isMenu(m,INFOLD) ) miInFold  = ji;
       else if( isMenu(m,ARITHM) ) miArithm  = ji;
       else if( isMenu(m,MOCM) )   miMocOp  = ji;
       else if( isMenu(m,MOCTOORDER) )   miMocToOrder  = ji;
       else if( isMenu(m,MOCFILTERING) )   miMocFiltering  = ji;
       else if( isMenu(m,MOCCROP) )   miMocCrop  = ji;
       else if( isMenu(m,MOCGENIMG) )   miMocGenImg  = ji;
       else if( isMenu(m,MOCHIPS) )   miMocHips  = ji;
       else if( isMenu(m,MOCGENCAT) )   miMocGenCat  = ji;
       else if( isMenu(m,HEALPIXARITHM) ) miHealpixArithm  = ji;
       else if( isMenu(m,NORM) )   miNorm    = ji;
       else if( isMenu(m,BITPIX) ) miBitpix  = ji;
       else if( isMenu(m,PIXEXTR) ) miPixExtr  = ji;
       else if( isMenu(m,CONV) )   miConv    = ji;
       else if( isMenu(m,HEAD) )   miHead    = ji;
       else if( isMenu(m,FLIP) )   miFlip    = ji;
       else if( isMenu(m,REGISTER))          miSAMPRegister = ji;
       else if( isMenu(m,UNREGISTER))        miSAMPUnregister = ji;
       else if( isMenu(m, STARTINTERNALHUB)) miSAMPStartHub = ji;
       else if( isMenu(m, STOPINTERNALHUB))  miSAMPStopHub = ji;
       else if( isMenu(m,BROADCAST))         miBroadcastAll = ji;
       else if( isMenu(m,BROADCASTTABLE))    miBroadcastTables = ji;
       else if( isMenu(m,BROADCASTIMAGE))    miBroadcastImgs = ji;
       else if( (i=m.indexOf("NSIDE"))>=0 ) {
          try { miNside.put(new Integer(m.substring(i+8)),ji); } catch( Exception e) {}
       }
    }

    int lastOrder=-2;  // -2:? calculer, -1:inutilis?, 0 et suivant:order courant
    boolean healpixCtrl=true;
    Hashtable<Integer, JMenuItem> miNside = new Hashtable<Integer, JMenuItem>();

    /** Dessin des losanges Healpix de controle */
    protected int getOrder() {
       if( lastOrder!=-2 ) return lastOrder;
       lastOrder=-1;
       Enumeration<Integer> e = aladin.miNside.keys();
       while( e.hasMoreElements() ) {
          int order = e.nextElement();
          JMenuItem mi = aladin.miNside.get(order);

          if( mi.isSelected() ) { lastOrder=order; break; }
       }
       return lastOrder;
    }

    /** Ajoute au menu principal la liste des servers d'images et des servers catalogues */
    protected void addServerMenu(ServerDialog dialog) {
       if( miImg!=null ) miImg.removeAll();
       if( miCat!=null ) miCat.removeAll();
       if( miGluTool!=null ) miGluTool.removeAll();
       String[] names = dialog.getServerNames(Server.IMAGE,true);
       if( names!=null && names.length>0 && names[0].indexOf("Aladin")>=0) ALADIN_IMG_SERVER = names[0];
       if( miImg!=null ) appendJMenu((JMenu)miImg,names);
       if( miCat!=null ) appendJMenu((JMenu)miCat,dialog.getServerNames(Server.CATALOG,true));
       if( miGluTool!=null ) appendJMenu((JMenu)miGluTool,
             dialog.getServerNames(Server.APPLI | Server.APPLIIMG,false));
    }

    /** Reg?n?re le popup menu associ? aux plugins */
    protected void pluginReload() {
       if( miPlugs==null ) return;
//       if( plugins!=null ) plugins.controleur.dispose();
       plugins = new Plugins(this);         // On le reg?n?re syst?matiquement
       JMenuItem ji = ((JMenu)miPlugs).getItem(0);
       miPlugs.removeAll();
       miPlugs.add(ji);
       String m[] = plugins.getNames();
       if( m.length>0 ) {
          ((JMenu)miPlugs).addSeparator();
          appendJMenu((JMenu)miPlugs,m);
       }
    }

    /** Reg?n?re le popup menu associ? aux Ciels */
    public void gluSkyReload() {
       if( isNonCertifiedApplet() || miGluSky==null ) return;

       String m[] = glu.getGluSkyMenu();

       if( m.length==0 ) return;
       miGluSky.removeAll();
       appendJMenu((JMenu)miGluSky,m);
//       sky(m[0].substring(1));  // POUR LE MOMENT JE SELECTIONNE LE PREMIER, IL FAUDRA PRENDRE EN COMPTE LES PREF

     }

    /** Reg?n?re le popup menu associ? aux VOtools */
    protected void VOReload() {
       if( isNonCertifiedApplet() || miVOtool==null ) return;
       JMenuItem ji = ((JMenu)miVOtool).getItem(0);
       miVOtool.removeAll();
       miVOtool.add(ji);
       String m[] = glu.getAppMenu();
       if( m.length>0 ) {
          ((JMenu)miVOtool).addSeparator();
          appendJMenu((JMenu)miVOtool,m);
       }
     }

    /** Retourne le num?ro de session d'Aladin. N'a d'int?r?t que dans le
     * cas d'instanciation multiple d'Aladin */
    public int getInstanceId() { return aladinSession; }


   /** Creation des objets et mise en place de l'interface.
     * On utilisera la plupart du temps des Panels hierarchises
     */
    protected void suiteInit() {
       aladin=this;                 // Une horreur pour que ?a marche en applet

       if( !flagLaunch ) {
          try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
             
//           UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

          } catch( Exception e ) { e.printStackTrace(); }
       }

       addMouseMotionListener(this);
       addMouseListener(this);
       setBackground((new JButton()).getBackground());   // UN PEU TORDU
       ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(0,3,0,2));

       aladinSession = (++ALADINSESSION);
//       CDSHealpix.init();
       configuration = new Configuration(this);
       if( STANDALONE ) {
          try {  configuration.load(); }
          catch( Exception e ) { System.err.println(e.getMessage()); }
       }

       if( !setOUTREACH ) OUTREACH = configuration.isOutReach();
       ENABLE_FOOTPRINT_OPACITY = configuration.isTransparent();
       DEFAULT_FOOTPRINT_OPACITY_LEVEL = configuration.getTransparencyLevel();
       if( configuration.isBeginner() && !flagScreen ) { SCREEN="preview"; flagScreen=true; }
       
       if( kernelList==null ) kernelList = new KernelList(this);
       if( chaine==null ) chaine = new Chaine(this);
       creatChaine();

       osName = System.getProperty("os.name");
       osArch = System.getProperty("os.arch");
       osVersion = System.getProperty("os.version");
       javaVersion = System.getProperty("java.version");
       javaVendor = System.getProperty("java.vendor");

       int v = numJVMVersion(javaVersion);
       ISLINUX = osName.indexOf("Linux")>=0;
       ISJNLP = FROMDB!=null && FROMDB.equals("CDS-WebStart");
       ISJVM15 = javaVersion.startsWith("1.5");
       ISJVM16 = javaVersion.startsWith("1.6");

       makeCursor(this,WAITCURSOR);

       // Pour g?rer les acc?s prot?g?.
       try {
//          Class auth = Class.forName("java.net.Authenticator");
//          Method setDefault = auth.getDeclaredMethod("setDefault",new Class[]{ auth });
//          setDefault.invoke((Object)null, new Object[] { new MyAuthenticator() } );

          // METHODE PLUS SIMPLE DES QUE COMPATIBLE JVM 1.2
          Authenticator.setDefault(new MyAuthenticator());

       } catch( Exception e) {  }

       // Affichage du banner
       if( BANNER && !NOGUI && aladinSession==0 && (!isApplet() || flagLaunch) ) {
          (new Thread("AladinBanner") {
             @Override
            public void run() { banner=new Banner(aladin); }

          }).start();
          Util.pause(50);
       }

       getContentPane().setLayout( new BorderLayout(0,0) );
       int id = getInstanceId();

       if( !flagLaunch && !NOGUI ) {
          System.out.println("\nAladin ("+VERSION+") "+(id>0?"- instance "+id+" ":"")+chaine.getString("STARTING")+ "...");
          System.out.println(chaine.getString("BANNER")+"\n  "+Aladin.COPYRIGHT);
       }

       //Recuperation d'un frame bidon pour l'applet qui n'en a pas
       if( f==null ) f = new MyFrame(this,TITRE+" "+getReleaseNumber());

       // Initialisation des objets
       // Rq:  L'ordre de creation des objets n'est pas qcq
       creatFonts();
       
       cache = new Cache(aladin);
       bookmarks = new Bookmarks(this);
       co = new CreatObj(this);

       // Mise ? jour des langues support?es
       configuration.loadRemoteLang();
       
       
       JButton b;
       ButtonGroup bg = new ButtonGroup();
       searchData = b = new JButton(new ImageIcon(getImagette("Load.gif")));
       b.setMargin(new Insets(0,0,0,0));
       b.setBorderPainted(false);
       b.setContentAreaFilled(false);
       // sera activ? dans CreatObj apres creation de ServerDialog
       b.setEnabled(false);
       Util.toolTip(searchData,chaine.getString("TIPOPEN"));
       b.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             setHelp(false);
             execute(OPENLOAD);
          }
       });
       bg.add(b);

       ExportYourWork = b = new JButton(new ImageIcon(getImagette("Export.gif")));
       b.setMargin(new Insets(0,0,0,0));
       b.setBorderPainted(false);
       b.setContentAreaFilled(false);
       Util.toolTip(b,chaine.getString("TIPEXPORT"));
       b.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             setHelp(false);
             execute(MSAVE);
          }
       });
       bg.add(b);

//       avant = b = new JButton(new ImageIcon(getImagette("Avant.gif")));
//       b.setMargin(new Insets(0,0,0,0));
//       b.setBorderPainted(false);
//       b.setContentAreaFilled(false);
//       Util.toolTip(b,chaine.getString("TIPAVANT"));
//       b.addActionListener( new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//             view.undo( (e.SHIFT_MASK & e.getModifiers())!=0 );
//          }
//       });
//       bg.add(b);
//
//       apres = b = new JButton(new ImageIcon(getImagette("Apres.gif")));
//       b.setMargin(new Insets(0,0,0,0));
//       b.setBorderPainted(false);
//       b.setContentAreaFilled(false);
//       Util.toolTip(b,chaine.getString("TIPPRES"));
//       b.addActionListener( new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//             view.redo( (e.SHIFT_MASK & e.getModifiers())!=0 );
//          }
//       });
//       bg.add(b);

       // Le bandeau sous le menu : Panel saisie comportant la localisation
       // et le target lie au plan de reference
       JToolBar saisie1 = new JToolBar();
       saisie1.setFloatable(false);
       saisie1.setBorder(BorderFactory.createEmptyBorder());
       saisie1.add(searchData);
       saisie1.add(ExportYourWork);
//       saisie1.addSeparator();
//       saisie1.add(avant);
//       saisie1.add(apres);
       JPanel saisie = new JPanel( new BorderLayout(0,0));
       saisie.add(saisie1,BorderLayout.WEST);
       saisie.add(localisation, BorderLayout.CENTER);
//       if( !OUTREACH && !BETA ) saisie.add(pixel);

       // creation widget plastic (doit se faire avant la creation du menu)
       if( PLASTIC_SUPPORT ) {
           plasticWidget = new PlasticWidget(this);
       }

       // Creation du menu
       if( !NOGUI ) {
          trace(1,"Creating the Menu");
          JMenuBar jBar = createJBar( createMenu() );
          // TODO : que faire en mode applet ??
          if( STANDALONE && macPlateform && !isApplet() ) f.setJMenuBar(jBar);
          else setJMenuBar(jBar);
       }

       trace(1,"Creating the main interface");

       // Le Panel contenant a la fois le View et le help
       cardView =  new CardLayout();
       bigView = new JPanel(cardView);
       bigView.add("Help",help);
       bigView.add("View",view);
       
       JPanel gauche1 = new JPanel( new BorderLayout(3,0));
       gauche1.add(bigView,BorderLayout.CENTER);

       // D?sactivation des ?l?ments de menus et des boutons non encore accessible
       setButtonMode();

       // Le panel gauche : contient la boite a boutons et les calques
       final JPanel gauche = new JPanel(new BorderLayout(3,0));
       gauche.add(calque,BorderLayout.CENTER);
       
       JPanel gauche2;
       gauche2 = new JPanel(new BorderLayout(2,0));
       gauche2.setBorder( BorderFactory.createEmptyBorder(0, 2, 0, 0));
       gauche2.add(toolBox,BorderLayout.WEST);
       gauche2.add(gauche,BorderLayout.CENTER);

       // Le panel haut1 : contient le menu et le bandeau d'info
       JPanel haut1 = new JPanel(new BorderLayout(1,1));
       haut1.add(saisie,BorderLayout.NORTH);
       JPanel  panelBookmarks = new JPanel( new BorderLayout(0,0));
       panelBookmarks.add( bookmarks.getToolBar(), BorderLayout.CENTER);
       haut1.add(panelBookmarks,BorderLayout.SOUTH);

       // Le panel haut : contient le logo et le haut1
       JPanel haut = new JPanel(new BorderLayout(0,0));
       haut.setBorder(BorderFactory.createEmptyBorder(4,0,0,40));
       haut.add(haut1,BorderLayout.CENTER);
       haut.add(logo,BorderLayout.EAST);

       // le panel du status
       JPanel searchPanel = new JPanel(new BorderLayout(0,0));
       searchPanel.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
       
       JPanel y = new JPanel( new FlowLayout(FlowLayout.CENTER,0,0));
       y.setBorder(BorderFactory.createEmptyBorder());
       y.add(grid);
       if( !OUTREACH ) { y.add(oeil); y.add(northup); }
       y.add(viewControl);
       if( !OUTREACH ) y.add(sync);

       makeAdd(searchPanel,y,"West");
       makeAdd(searchPanel,status,"Center");
       makeAdd(searchPanel,search,"East");

       GridBagLayout g = new GridBagLayout();
       infoPanel = new JPanel(g);
       GridBagConstraints gc = new GridBagConstraints();
       gc.gridwidth = 3;
       gc.weightx = 1;
       gc.anchor=GridBagConstraints.WEST;
       gc.fill=GridBagConstraints.HORIZONTAL;
       g.setConstraints(urlStatus, gc);
       infoPanel.add(urlStatus);

       gc.weightx = 0;
       gc.anchor=GridBagConstraints.EAST;
       g.setConstraints(memStatus, gc);
       infoPanel.add(memStatus);

       if( PLASTIC_SUPPORT ) {
          getMessagingMgr().setPlasticWidget(plasticWidget);

          if( macPlateform ) gc.insets.right = 14;
          g.setConstraints(plasticWidget, gc);
          infoPanel.add(plasticWidget);

          plasticPrefs = new PlasticPreferences(this);
       }

       // Le panel principal

       JPanel ct;
       // indispensable sous MacOS, sinon les raccourcis clavier ne fonctionneront pas !
       if( f!=null && macPlateform && !isApplet() ) ct = (JPanel)f.getContentPane();
       else ct = (JPanel)getContentPane();

       ct.setBackground(getBackground());
       ct.setLayout( new BorderLayout(3,3) );
       ct.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//       ct.setBorder(BorderFactory.createEmptyBorder(0,3,0,3));

       // test thomas (avec un s?parateur) + Pierre
//       final MySplitPane splitV = new MySplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
//             gauche1, gauche2);
//       splitV.setBorder(BorderFactory.createEmptyBorder());
//       splitV.setResizeWeight(1);
//       gauche.setMinimumSize(new Dimension(ZoomView.SIZE + ToolBox.W,200));
//       gauche1.setMinimumSize(new Dimension(300,300));
       
       JPanel splitV = new JPanel( new BorderLayout(0,0));
       splitV.add(gauche1,BorderLayout.CENTER);
       splitV.add(gauche2,BorderLayout.EAST);

       JPanel bigViewSearch = new JPanel( new BorderLayout(0,0));
       bigViewSearch.add(splitV,BorderLayout.CENTER);
       bigViewSearch.add(searchPanel,BorderLayout.SOUTH);

       splitH = new MySplitPane(JSplitPane.VERTICAL_SPLIT, true,
             bigViewSearch, mesure);
       mesure.setPreferredSize(new Dimension(100,150));
       mesure.setMinimumSize(new Dimension(100,0));
       splitH.remove(mesure);
       splitH.setBorder(BorderFactory.createEmptyBorder());
       makeAdd(ct,haut,"North");
       makeAdd(ct,splitH,"Center");
       makeAdd(ct,infoPanel,"South");

       // Dernier objet a creer et traitement des parametres
       co.creatLastObj();

       // Juste pour s'en souvenir en cas de re-fenestration
       if( SIGNEDAPPLET || (!STANDALONE && extApp==null) ) {
          origPos = getBounds();
          myParent = getParent();
       }

       // Evaluation de la vitesse, methode Thomas
       speed = getSpeed();
       isSlow = speed>slownessThreshold;

       // Log
       log("Start",(SIGNEDAPPLET?"signed applet ":STANDALONE?"standalone ":"applet ")+VERSION+
                       " perf="+speed+
                       " java="+javaVersion+"/"+javaVendor+
                       " syst="+osName+"/"+osArch+"/"+osVersion+
                       (FROMDB!=null?" from="+FROMDB:"")+
                       " lang="+configuration.getLanguage());

       if( !flagLaunch && !NOGUI ) {
          System.out.println(chaine.getString("YOURJVM")+" "+javaVersion+" / "+javaVendor);
       }

       // En mode trace, affichage du classpath
       if( STANDALONE ) Aladin.trace(1, "Classpath is : ** "+System.getProperty("java.class.path")+" **");

       // Suppression d'?ventuels vieux caches oubli?s
       if( STANDALONE && getInstanceId()==0 ) removeOldCaches();

       // Cache le banner si ce n'est d?j? fait
       if( banner!=null ) banner.setVisible(false);

       // Le mot d'accueil pour le demarrage
       if( aladinSession==0 ) {
          msgOn=true;
          help.setCenter(true);
          help.setText(isLoading()?logo.inProgress():logo.Help());
       } else {
          cardView.show(bigView,"View");
          msgOn=false;
       }

       // Lecture des commandes scripts sur la console (et/ou stdin)
       if( CONSOLE ) command.readStandardInput();

       // Message d'avertissement pour le mode applet brid?e
       if( !STANDALONE && v>=120 && !warningRestricted) {
          warningRestricted = true;
          warning(chaine.getString("RESTRICTED"));
       }
       
       manageDrop();

       if( !aladin.NOGUI ) {
          (new Thread("Start"){
             @Override
            public void run () {
                Util.pause(7000);
                localisation.infoStart();
             }
          }).start();
       }
    }
    
    protected void manageDrop() {
       // IL Y A UN GROS BUG SOUS LINUX QUI FAIT QUE LA JVM DU BROWSER SE PLANTE ET
       // PLANTE LE BROWSER LORSQUE L'ON FAIT UN DETACH() SI LA FRAME EST DRAG&DROP
       if( !( isApplet() && osName.startsWith("Linux")) ) {

          // Pour g?rer le DnD de fichiers externes
          new DropTarget (this, this);
          DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_COPY_OR_MOVE, this);
       }
    }
    
    // Surcharges de classes pour supprimer le trait s?parateur du JSplitPane
    class MySplitPane extends JSplitPane {
       public MySplitPane(int newOrientation, boolean newContinuousLayout,
             Component newLeftComponent, Component newRightComponent ) {
          super(newOrientation,newContinuousLayout,newLeftComponent,newRightComponent);
          flagMesure = newOrientation==JSplitPane.VERTICAL_SPLIT;
          setUI(new MyBasicSplitPaneUI());
       }
       
       private boolean flagMesure;
       private int mesureHeight;
       
       // Repositionne le diviseur ? la position m?moris?e
       public void restoreMesureHeight() {
          setDividerLocation(getHeight()-(mesureHeight<=0 ? 150 : mesureHeight)); }
       
       // Positionne le diviseur en fonction de la taille de la fen?tre des mesures,
       // et m?morise cette valeur pour pouvoir y revenir 
       public void setMesureHeight(int h) { mesureHeight=h; }
       
       // Retourne la taille de la fen?tre des mesures.
       public int getMesureHeight() { return mesureHeight; }
       
       // On bride ? 55 pixels minimum pour la taille de la fen?tre des mesures
       public void setDividerLocation(int n) {
          if( flagMesure ) {
             int h = getHeight();
             if( h-n<53 ) return;
             mesureHeight = h-n;
          }
          super.setDividerLocation(n);
       }
       
    }
    class MyBasicSplitPaneUI extends BasicSplitPaneUI {
       @Override
    public BasicSplitPaneDivider createDefaultDivider() {
          return new MySplitPaneDivider(this);
      }
    }
    class MySplitPaneDivider extends BasicSplitPaneDivider {
       public MySplitPaneDivider(BasicSplitPaneUI ui) { super(ui); }
       @Override
    public void paint(Graphics g) {
//          g.setColor(Color.gray);
//          if( getBasicSplitPaneUI().getOrientation()==JSplitPane.HORIZONTAL_SPLIT ) {
//             Util.drawVerticalSplitPaneTriangle(g,0,view.getHeight()-25);
//          } else {
//             Util.drawHorizontalSplitPaneTriangle(g,view.getWidth()-25,0);
//          }
       }
    }


    /** Subtilit? pour faire de la mise en page une fois que toutes les peer classes
     * aient ?t? correctement initialis?es
     */
    @Override
    public void paint(Graphics g) {
       if( !flagScreen || isApplet() ) { super.paint(g); return; }

       if( SCREEN.equals("full") ) {
          detach(false);
          fullScreen(0);
       } else if( SCREEN.startsWith("preview") ) {
          detach(false);
          fullScreen(SCREEN.equals("previewhidden") ? 2 : 1);
       } else if( SCREEN.equals("frame") ) {
          detach();
       }
       flagScreen=false;
    }

    /** Positionnement d'un message d'attente */
    protected void setBannerWait() {
       help.setText(logo.inProgress());
    }

    public void dragGestureRecognized(DragGestureEvent dragGestureEvent) { }
    public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
       dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    public void dragExit (DropTargetEvent dropTargetEvent) {}
    public void dragOver (DropTargetDragEvent dropTargetDragEvent) {}
    public void dropActionChanged (DropTargetDragEvent dropTargetDragEvent){}
    public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent){}
    public void dragEnter(DragSourceDragEvent DragSourceDragEvent){}
    public void dragExit(DragSourceEvent DragSourceEvent){}
    public void dragOver(DragSourceDragEvent DragSourceDragEvent){}
    public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent){}

    public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
       try {
          DataFlavor uriList = new DataFlavor("text/uri-list; class=java.lang.String");
          Transferable tr = dropTargetDropEvent.getTransferable();

          // On pr?f?re tout d'abord charger via une URL si possible
          // car cela ?vite de planter sur les caches de Firefox
          // et permet ?galement de supporter IE
          if( tr.isDataFlavorSupported(uriList) ) {
             dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
             String s = (String)tr.getTransferData(uriList);
             StringTokenizer st = new StringTokenizer(s,"\n\r");
             while( st.hasMoreTokens() ) {
                String f = st.nextToken();
                if( f.trim().length()==0 ) continue;
                calque.newPlan(f,null,null);
                console.printCommand("load "+f);
             }
             dropTargetDropEvent.getDropTargetContext().dropComplete(true);

          // Sinon par le nom de fichier
          } else if( tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
                dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
             java.util.List fileList = (java.util.List) tr.getTransferData(DataFlavor.javaFileListFlavor);
             Iterator iterator = fileList.iterator();
             while( iterator.hasNext() ) {
                File file = (File) iterator.next();
                calque.newPlan(file.getAbsolutePath(),file.getName(),null);
                console.printCommand("load "+file.getAbsolutePath());
             }
             dropTargetDropEvent.getDropTargetContext().dropComplete(true);

          } else dropTargetDropEvent.rejectDrop();

       } catch( Exception e ) {
          e.printStackTrace();
          dropTargetDropEvent.rejectDrop();
       }
    }


    /** Retourne true si Aladin a ?t? lanc? avec des param?tres de chargement */
    protected boolean isLoading() { return flagLoad; }

    /** Cr?ation si n?cessaire d'un r?pertoire cache destin? ? Aladin
     *  se trouvera dans ${HOME}/.aladin
     *  @return true si ok, false sinon
     */
    protected boolean createCache() {
       if( CACHEDIR!=null ) return CACHEDIR.length()!=0;

       try {
          // Existe-il d?j? un r?pertoire g?n?rique .aladin sinon je le cr?e ?
          CACHEDIR = System.getProperty("user.home")+Util.FS+CACHE;
          File f = new File(CACHEDIR);
          if( !f.isDirectory() ) if( !f.mkdir() ) { CACHEDIR=""; return false; }

          // Je vais cr?er un sous-r?pertoire cache pour la session uniquement
          CACHEDIR = CACHEDIR+Util.FS+CACHE+"."+(int)(Math.random()*65536);
          f = new File(CACHEDIR);

          if( !f.mkdir() ) { CACHEDIR=""; return false; }
          Aladin.trace(3,"Create cache directory: "+CACHEDIR);
          launchCacheUpdater();
       } catch( Exception e ) { e.printStackTrace(); return false; }
       return true;
    }

    // Thread du cacheUpdater
    private Thread updaterCache=null;

    // Pour pouvoir arr?ter proprement le thread d'update de la date du cache
    private boolean cacheUpdaterRunning=false;

    /** Demande d'arr?t du thread de mise ? jour de la date de derni?re modif du cache */
    private void stopCacheUpdater() {
       cacheUpdaterRunning=false;
       if( updaterCache!=null ) updaterCache.interrupt();
    }

    /** Mise ? jour de la date de modif du r?pertoire cache afin qu'une autre session
     * ne puisse faire un nettoyage intempestif (toutes les minutes) */
    private void launchCacheUpdater() {
       cacheUpdaterRunning=true;
       (updaterCache=new Thread("cacheUpdater"){
          @Override
        public void run() {
//System.out.println("Cache updater started for "+CACHEDIR);
             while( cacheUpdaterRunning ) {
                try {
                   Util.pause(60*1000);
                   if( CACHEDIR==null ) { cacheUpdaterRunning=false; continue; }
                   File f = new File(CACHEDIR);
                   f.setLastModified(System.currentTimeMillis());
//System.out.println("Update cache "+CACHEDIR);
                } catch( Exception e) {}
             }
             updaterCache=null;
//System.out.println("Cache updater stopped");
          }
       }).start();
    }

    // Permet de conna?tre la taille approximative du cache disque
    protected void setInCache(long size) {
       sizeCache+=size;
    }

    /** Construit le r?pertoire des VOTools et le cr?e si n?cessaire */
    protected String getVOPath() {
       String dir = System.getProperty("user.home")
       +Util.FS+aladin.CACHE
       +Util.FS+"VOTools";
       try {
          File f = new File(dir);
          if( !f.isDirectory() ) if( !f.mkdir() ) throw new Exception();
       } catch( Exception e ) {
//        aladin.warning("Your plugin directory can not be created !\n["+dir+"]");
          if( Aladin.levelTrace>=3 ) e.printStackTrace();
       }
       return dir;
    }
    
    public Glu getGlu() { return glu; }
    public Cache getCache() { return cache; }
    public Command getCommand() { return command; }
    public Configuration getConfiguration() { return configuration; }

    /** Suppression du cache de la session et de tout ce qu'il contient */
    static protected void removeCache() {

       // Suppression du cache de la session
       if( CACHEDIR==null || CACHEDIR.length()==0 ) return;
       removeThisCache(CACHEDIR);
       CACHEDIR=null;
    }

    /**  Sauvegarde de la config utilisateur si n?cessaire
     */
    protected void saveConfig() {
    	if( !STANDALONE ) return;
        try{
           configuration.save();
           configuration.saveLocalFunction();
        } catch( Exception e ) { System.err.println(e.getMessage()); }

        try {
           if( !NOGUI ) console.saveHistory();
        } catch( Exception e ) { System.err.println(e.getMessage()); }
    }

    /** Suppression d'?ventuels vieux caches oubli?s dans une session pr?c?dente (plus vieux de 24h)
     * On cherche tous les r?pertoires qui se trouvent dans le home/.aladin de l'utilisateur
     * et qui suivent la syntaxe ".aladin.nnnn ou nnn est un nombre */
    protected void removeOldCaches() {
       long date = System.currentTimeMillis()-24*3600000L;
       try {
          String alaCache = System.getProperty("user.home")+Util.FS+CACHE;
          File fcache = new File(alaCache);
          String f[] = fcache.list();
          for( int i=0; f!=null && i<f.length; i++ ) {
             File g = new File(fcache+Util.FS+f[i]);
             if( !g.isDirectory() ) continue;
             String name = g.getName();
             if( !name.startsWith(CACHE+".")) continue;
             if( g.lastModified()>date ) continue;
             boolean flagCont=false;
             for( int j=CACHE.length()+2; j<name.length(); j++ ) {
                if( !Character.isDigit(name.charAt(j) ) ) { flagCont=true; break; }
             }
             if( flagCont ) continue;
             removeThisCache(alaCache+Util.FS+name);
          }
       } catch( Exception e ) { e.printStackTrace(); }

    }


    /** Suppression du cache pass? en param?tre et de tout ce qu'il contient */
    static protected void removeThisCache(String cacheDir) {
       try {
          File cache = new File(cacheDir);
          String f[] = cache.list();
          for( int i=0; f!=null && i<f.length; i++ ) {
// System.out.println("f="+f[i]);
             (new File(cacheDir+Util.FS+f[i])).delete();
          }
          cache.delete();
       } catch( Exception e ) { e.printStackTrace(); }
 Aladin.trace(3,"Remove cache directory: "+cacheDir);
    }

   /** Memorisation de la derniere version disponible (transmis par Glu.log)
    * En cas de modification, on efface le cache, notamment le dico GLU */
   protected void setCurrentVersion(String s )  {
       currentVersion = s;
       if( !NETWORK ) return;
       
       // Banner de demande de maj de la version si n?cessaire
       testUpgrade();

       // Doit-on nettoyer le cache et recharger les bookmarks officielles
       // car le num?ro officiel de la version Aladin a chang? ?
       String lastCurrentVersion = configuration.getOfficialVersion();
       if( currentVersion!=null && currentVersion.length()!=0 &&
             (lastCurrentVersion==null || !lastCurrentVersion.equals(currentVersion)) ) {
          configuration.setOfficialVersion(currentVersion);
          trace(1,"Reset cache & bookmarks definition (new official Aladin version)...");
          cache.clear();
          if( bookmarks!=null ) bookmarks.reload();
       }
       
       // Doit-on nettoyer le cache et recharger les bookmarks officielles
       // car le num?ro de version Aladin a chang? par rapport ? la derni?re utilisation
       else if( configuration.getVersion()==null || !configuration.getVersion().equals(VERSION) ) {
          trace(1,"Reset cache & bookmarks definition (new Aladin version)...");
          cache.clear();
          if( bookmarks!=null ) bookmarks.reload();
       }

       // Doit-on nettoyer le cache car la derni?re session date de plus de 15 jours
       else if((System.currentTimeMillis()-configuration.getLastRun())>15*86400*1000L ) {
          trace(1,"Reloading GLU records & VizieR keywords (too old definitions) => clear local cache...");
          cache.clear();
       }
    }
   
   /** V?rifie s'il est n?cessaire de demander ? l'utilisateur l'installation
    * de la nouvelle version */
   private void testUpgrade() {
      if( NOGUI || isApplet() || !TESTRELEASE ) return;
      
      (new Thread("testUpgrade"){
         @Override
         public void run() { 
            try {
               Thread.currentThread().sleep(5000);
               testVersion();
            } catch( Exception e) { }
         }
      }).start();
   }

   /** Indication de l'etat de l'impression */
   synchronized void setFlagPrint(boolean print) { this.print = print;  }
   synchronized boolean isPrinting() { return print; }

   /** Indication d'un save, export ou backup en cours */
   protected boolean isSaving() { return !command.isSyncSave(); }
   
   /** Envoi d'un log en cours */
   protected boolean isLogging() { return glu.isLogging(); }

   /** Transformation de la chaine du numero de version vx.xxx en valeur
    * numerique
    * @param s la chaine v1.120 par exemple
    * @return 11 par exemple (on ne prend pas en compte le 2 derniers digits)
    *         ou 0 si s==null ou d'un mauvais format;
    */
    protected int numVersion(String s) {
       if( s==null || s.length()<6 ) return 0;
       char [] a = s.toCharArray();
       if( a[0]!='v' || a[2]!='.' ) return 0;
       int i= (a[1]-'0')*10 + (a[3]-'0');
       return i;
    }

    /** Transformation de la chaine du numero de version vx.xxx en valeur
     * numerique x.xxx (tous les digits sont pris en compte)
     * ex: v6.037  => 6.037 */
    public double realNumVersion(String s) {
       try {
          int deb=0;
          while( !Character.isDigit(s.charAt(deb)) ) deb++;
          int fin=s.length()-1;
          while( !Character.isDigit(s.charAt(fin)) ) fin--;
          s = s.substring(deb,fin+1);
          return Double.parseDouble(s);
       } catch( Exception e ) {}
       return 0;
    }

   /** Transformation de la chaine du numero de version n.n.n en valeur
    * numerique
    * @param s la chaine 1.3.1 par exemple
    * @return 131 par exemple  ou 0 si s==null ou d'un mauvais format;
    */
    protected int numJVMVersion(String s) {
       if( s==null || s.length()<5 ) return 0;
       char [] a = s.toCharArray();
       int i= (a[0]-'0')*100 + (a[2]-'0')*10 + (a[4]-'0');
       return i;
    }

   /** calcule la vitesse de la machine virtuelle
    *  @return un indicateur de performance (lower is better)
    *
    */
   protected static long getSpeed() {
       return 0;
//       long start = System.currentTimeMillis();
//       Vector vec = new Vector();
//
//       for(int i=0;i<30000;i++) {
//          vec.addElement(new Vector(30));
//          /* Vector pipo = (Vector) */ vec.elementAt(i);
//       }
//       long end = System.currentTimeMillis();
//
//       vec=null;
//       return (end-start);
//   }
       }

   /** Transformation de la chaine du numero de version vx.abc en sa valeur
    * generale x.a
    */
    static protected String getReleaseNumber() {
       return VERSION.substring(0,VERSION.indexOf('.')+2);
    }

   /** Test du numero de version */
    protected void testVersion() {
       int cv = numVersion(currentVersion);
       if( cv==0 ) return;
       int v = numVersion(VERSION);
       if( v>=cv ) return;
       String s = chaine.getString("MAJOR")
          +"!Aladin Java "+currentVersion
          +chaine.getString("MAJOR1");
       if( !confirmation(s) ) return;
       glu.showDocument("AladinJava.SA","");
    }

   /** Fin du message d'accueil */
    protected void endMsg() {
       if( !msgOn ) return;
       cardView.show(bigView,"View");
       msgOn=false;
       if( isFullScreen() ) fullScreen.repaint();
       setHelp(false);
    }
    
    
    
//    /** Visualisation (cr?ation si n?cessaire) de la fen?tre des prog?niteurs */ 
//    protected void showFrameProgen() {
//       if( frameProgen==null ) frameProgen = new FrameProgen(aladin);
//       else frameProgen.setVisible(true);
//    }

   /** Efface le contenu du Status. En fait, si l'evenement
    * arrive jusqu'ici c'est qu'il n'a pas ete traite par les autres
    * objets, donc on peut effacer
    */
    public void mouseMoved(MouseEvent e) {
       if( inHelp ) help.setDefault();
       else if( status!=null ) {
          if( dialog!=null && !command.isSync() ) status.setText(chaine.getString("SEESTACK"));
          else status.setText("");
       }
    }

    /** On ins?re l'applet dans sa propre fenetre */
    protected void detach() { detach(true); }
    protected void detach(boolean show) {
       try {
          if( flagDetach ) return;
          makeAdd(f,this,"Center");
          bDetach.setText(MDCH2);
          miDetach.setText(MDCH2);
          f.pack();
          if( show ) f.setVisible(true);
          flagDetach=true;
       } catch( Exception e ) {
          if( levelTrace>=3 ) e.printStackTrace();
       }
    }

   /** Remise en place de l'Applet dans la fenetre du navigateur */
    protected void unDetach() {
       if( !flagDetach ) return;
       f.remove(this);
       f.dispose();
       flagDetach=false;
       makeAdd(myParent,this,"Center");
       bDetach.setText(MDCH1);
       miDetach.setText(MDCH1);
       reshape(origPos.x,origPos.y,origPos.width,origPos.height);
       myParent.show();
       myParent.invalidate();
       myParent.layout();
    }

    /** Passage en plein ?cran
     * @param mode 0-plein ?cran classique,
     *             1-fen?tre preview
     *             2-fen?tre preview mais d?marre cach? (tr?s utile en mode applet
     */
    protected void fullScreen(int mode) {
       if( fullScreen==null ) {
          boolean full = mode==0;
          boolean startHidden = mode==2;
          pan(false);
          fullScreen = new FullScreen(this,view.getCurrentView(),full,startHidden);
//          f.setVisible(false);
       } else {
          fullScreen.end();
          fullScreen=null;
//          f.setVisible(true);
       }
    }

   // Juste pour eviter que la classe Save.class ne soit chargee
   // dans la version applet
   private void saveG() {
      save.show();
   }

   protected boolean save(String s) {
      String fmt[] = Save.getFormat();
      for( int i=0; i<fmt.length; i++ ) {
         if( isMenu(s,fmt[i]) ) {
            save.saveFile(1,Save.getCodedFormat(i),-1);
            return true;
         }
      }
      if( isMenu(s,EXPORTEPS) ) save.saveFile(1,Save.getCodedFormat(Util.indexInArrayOf("EPS", fmt)),-1);
      else if( isMenu(s,BACKUP) ) save.saveFile(0);
      else if( isMenu(s,EXPORT) ) save.exportPlans();
      else return false;

      return true;
   }

   protected int allsky() {
      TreeNodeAllsky gSky = glu.getGluSky(0);
      return allsky(gSky);
   }

   /** Activation d'un background */
   protected int allsky(TreeNodeAllsky gSky) { return hips(gSky,null,null,null); }
   protected int hips(TreeNodeAllsky gSky,String label,String target,String radius) {
      int n=1;
      if( !gSky.isMap() ) n=calque.newPlanBG(gSky,label,target,radius);
      else n=calque.newPlan(gSky.getUrl(), label, gSky.copyright,target,radius);
      toolBox.repaint();
      return n;
   }

   /** Mise en place du ciel s */
   protected boolean allsky(String s) {
      int i = glu.findGluSky(s,2);
      if( i<0 ) return false;
      TreeNodeAllsky ga = glu.getGluSky(i);
      console.printCommand("get hips(\""+ga.aladinLabel+"\")");
      allsky(ga);
      return true;
   }

   /** Lancement de l'appli PLASTIC s */
   protected boolean appli(String s) {
      int i = glu.findGluApp(s);
      if( i<0 ) return false;
      GluApp ga = glu.getGluApp(i);
      ga.exec();
      return true;
   }

   // Affichage des authors et contributors
   private void about() {
      Aladin.info(TITRE+" ("+VERSION+(BETA?" beta":PROTO?" proto":"")+") "+
            chaine.getString("CDS")+
            "Authors: Pierre Fernique, Thomas Boch,\n      Ana?s Oberto, Fran?ois Bonnarel\n" +
            "      (see also the Aladin FAQ for all other contributers)\n \n" +
            "* Copyright: UDS/CNRS - distributed under GNU GPL v3\n  \n" +
            "Portions of the code (progressive catalogs, PM facility) have been developped  in the framework of GAIA CU9 (2012-2022)." +
            "The outreach mode has been developed in the framework of EuroVO AIDA & ICE projects (2008-2012)." +
            "WCS in JPEG, extended SIA, IDL bridge, FoV advanced integration, Fits cubes, Xmatcher by ellipses, SAMP " +
            "integration have been developed in the framework of the EuroVO VOTech project (2005-2008). " +
            "The contours, filters, data tree, column calculator and Xmatcher have been developed " +
            "in the framework of the Astrophysical Virtual Observatory (AVO), an EC RTD project 2002-2004. " +
            "The RGB feature has been developed in the framework of the IDHA project (ACI GRID of the French Ministere de la Recherche).\n \n" +
            "* Contact:\ncds-question@unistra.fr\nhttp://aladin.u-strasbg.fr\n \n " +
            "If the Aladin sky atlas was helpful for your research work, the citation of the following " +
            "article 2000A&AS..143...33B would be appreciated.");
   }

   // Pour envoyer un rapport de bug/une question
   private void sendBugReport() {
       String s = "mailto:cds-question@unistra.fr?subject=[Aladin] Bug report/question";
       // on ajoute la date pour g?n?rer des sujets uniques, sinon on a des problemes ? trier dans question
       String date = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH).format(new Date());
       s += " ("+date+")";
       s += "&body=";
       s += "Aladin version: "+(SIGNEDAPPLET?"signed applet ":STANDALONE?"standalone ":"applet ")+VERSION;
       s += "%0A" + "JVM: "+javaVersion+" / "+javaVendor;
       s += "%0A" + "OS: "+osName+"/"+osArch+"/"+osVersion;
       s += "%0A" + "Language: "+configuration.getLanguage();
       s += "%0A%0A" + "Bug description: ";

       glu.showDocument("Http", s.replaceAll(" ", "%20"), true);
   }

   // Pour afficher les nouveautes
   private void newsReport() {
       glu.showDocument("Http", "http://aladin.u-strasbg.fr/java/NewInV8.png", true);
   }

   // Pour affiche la page d'info sur les plugins
   private void pluginsReport() {
       plugins.showFrame();
   }

   // Pour affiche la page d'info sur les VOTools
   private void VOReport() {
       FrameVOTool.display(this);
   }

   // Juste pour eviter que la classe Printer.class ne soit chargee
   // dans la version applet
   // RETIRER POUR DES PROBLEMES AVEC JSHRINK
   /*
   private void printer() {
      try {
         Class x = Class.forName("cds.aladin.Printer");
         Class a = this.getClass();
         Constructor c = x.getDeclaredConstructor(new Class [] { a });
	 c.newInstance(new Object[] { this });
      } catch( Exception e ) { System.out.println(e); }
   }
   */
   protected void printer() { new Printer(this); }

   // test des menus qui vire un ?ventuel pr?fixe "BETA:", "PROTO:" ou "OUTREACH:"
   protected boolean isMenu(String s,String t) {
      if( s==null || t==null ) return false;
      return s.equals(t)
     || (t.startsWith(NOAPPLETPREFIX) && s.equals(t.substring(NOAPPLETPREFIX.length()) ))
     || (t.startsWith(BETAPREFIX) && s.equals(t.substring(BETAPREFIX.length()) ))
     || (t.startsWith(PROTOPREFIX) && s.equals(t.substring(PROTOPREFIX.length()) ))
     || (t.startsWith(OUTREACHPREFIX) && s.equals(t.substring(OUTREACHPREFIX.length()) ))

      ;
   }

   public void actionPerformed(ActionEvent evt) {
	   JMenuItem ji = (JMenuItem)evt.getSource();
	   // lancement d'un tutorial 'Show me how to ...' ?
	   if( ji.getActionCommand().equals(TUTO) ) {
		   launchTuto(ji.getText());
		   return;
	   }

//	   if( ji== hpxCtrl ) { view.newView(); view.repaintAll(); }
//
//       if( Aladin.BETA && hpxCtrl!=null ) {
//          for( int  i=0; i<healpixCtrl.length; i++ ) {
//             if( ji==healpixCtrl[i] ) { view.repaintAll(); return; }
//          }
//       }

       Object src = evt.getSource();

       // envoi de plans ? une appli SAMP/PLASTIC
       if( src instanceof JMenuItem && ((JMenuItem)src).getActionCommand().equals(BROADCASTTABLE) ) {
           String o = ((JMenuItem)src).getText();
           // broadcast ? toutes les applis
           if( o.equals(calque.select.MALLAPPS) ) {
              broadcastSelectedTables(null);
           }
           else {
              broadcastSelectedTables(new String[]{o.toString()});
           }
        } else if( src instanceof JMenuItem
              && ((JMenuItem)src).getActionCommand().equals(BROADCASTIMAGE) ) {
            String o = ((JMenuItem)src).getText();
            // broadcast ? toutes les applis
           if( o.equals(calque.select.MALLAPPS) ) {
              broadcastSelectedImages(null);
           }
           // envoi ? une appli particuli?re
           else {
              broadcastSelectedImages(new String[]{o.toString()});
           }

        }

	   String s = ji.getActionCommand();
	   execute(s);
   }

   /** Reactions aux differents boutons du menu */
//    public boolean action(Event e, Object o) {
//      String s;
//
//      // Rendre ? C?sar ce qui est ? C?sar
//      if( !NOGUI && e.target==calque.zoom.cZoom ) {
//         calque.zoom.submit();
//         return true;
//      }
//      if( !(o instanceof String) ) return true;
//      // lancement tutoriaux
//
//      s=(String)o;
//
//      return execute(s);
//    }

    /** Reactions aux differents boutons du menu */
    protected boolean execute(String s) {

       // En mode Outreach, le save ne fait qu'une sauvegarde PNG
       if( OUTREACH && isMenu(s,MSAVE) ) s="PNG";

       if( s.indexOf("NSIDE")>=0 ) { lastOrder=-2; aladin.calque.repaintAll(); }

       // Interface d'interrogation des serveurs
       else if( isMenu(s,OPENFILE) || isMenu(s,OPENLOAD) || isMenu(s,OPENURL) || isMenu(s,LOADVO)
             || isMenu(s,LOADFOV) || isMenu(s,ALADIN_IMG_SERVER) ) {
         if( dialog==null ) {
            Aladin.warning(chaine.getString("NOTYET"));
            return true;
         }
         if( firstLoad ) {
            // initialisation focus
            dialog.server[dialog.current].setInitialFocus();
         	firstLoad = false;
         }

         if( isMenu(s,OPENLOAD) ) dialog.setVisible(true);
         else {
            Server server = /* isMenu(s,OPENFILE) || */isMenu(s,OPENURL)? dialog.localServer
               : isMenu(s,LOADVO) ? dialog.discoveryServer
               : isMenu(s,ALADIN_IMG_SERVER) ? dialog.aladinServer
               : dialog.fovServer ;

            if( isMenu(s,OPENFILE) ) ((ServerFile)dialog.localServer).browseFile();
            else dialog.show(server);
         }

      } else if( isMenu(s,HELP) ) { setHelp(!inHelp);
      } else if( isMenu(s,HELPSCRIPT) ) {
         help.setCenter(false);
         status.setText(chaine.getString("SCRIPT"));
         command.execHelpCmd("");
      } else if( isMenu(s,FULLSCREEN) ) { fullScreen(0);
      } else if( isMenu(s,PREVIEWSCREEN) ) { fullScreen(1);
      } else if( isMenu(s,MOREVIEWS) ) { view.autoViewGenerator();
      } else if( isMenu(s,ONEVIEW) ) { view.oneView();
      } else if( isMenu(s,NEXT) )  { view.next(1);
      } else if( isMenu(s,LOCKVIEW) )  { view.getCurrentView().switchLock();
      } else if( isMenu(s,NORTHUP) )  { view.getCurrentView().switchNorthUp();
      } else if( isMenu(s,DELLOCKVIEW) )  { view.freeLock(); calque.repaintAll();
      } else if( isMenu(s,STICKVIEW) ) { view.stickSelectedView(); calque.repaintAll();
      } else if( isMenu(s,MSAVE) ) { saveG();
      } else if( isMenu(s,MDCH1) ) { detach();
      } else if( isMenu(s,MDCH2) ) { unDetach();
      } else if( isMenu(s,FAQ) )   { glu.showDocument("Aladin.java.getFAQ","");
      } else if( isMenu(s,MAN) )   { glu.showDocument("Aladin.java.getManual.pdf","");
      } else if( isMenu(s,TUTORIAL) ){ glu.showDocument("Aladin.Tutorial", "");
      } else if( isMenu(s,MPRINT) ){ printer();
      } else if( isMenu(s,SENDBUG) ) { sendBugReport();
      } else if( isMenu(s,NEWS) )  { newsReport();
      } else if( isMenu(s,PLUGINFO)) {pluginsReport();
      } else if( isMenu(s,VOINFO)) { VOReport();
      } else if( isMenu(s,ABOUT) ) { about();
      } else if( isMenu(s,CMD) )   { console();
      } else if( isMenu(s,MBKM) )  { bookmarks.showFrame();
      } else if( isMenu(s,XMATCH) ){ xmatch();
      } else if( isMenu(s,ADDCOL) ){ addCol();
      } else if( isMenu(s,PIXEL) ) { pixel();
      } else if( isMenu(s,CONTOUR)){ contour();
      } else if( isMenu(s,RETICLE))  { reticle(1);
      } else if( isMenu(s,RETICLEL)) { reticle(2);
      } else if( isMenu(s,NORETICLE)){ reticle(0);
      } else if( isMenu(s,TARGET)) { target();
      } else if( isMenu(s,OVERLAY)){ overlay();
      } else if( isMenu(s,RAINBOW)){ rainbow();
      } else if( isMenu(s,GRID))   { grid();
      } else if( isMenu(s,HPXGRID)){ hpxGrid();
//      } else if( isMenu(s,HISTORY)){ history();
      } else if( isMenu(s,ZOOMP))  { calque.zoom.setZoom("+");
      } else if( isMenu(s,ZOOMM))  { calque.zoom.setZoom("-");
      } else if( isMenu(s,ZOOMPT)) { zoom();
      } else if( isMenu(s,COPIER)) { copier();
      } else if( isMenu(s,COLLER)) { coller();
//      } else if( isMenu(s,PREVPOS)) { view.undo(false);
//      } else if( isMenu(s,NEXTPOS)) { view.redo(false);
      } else if( isMenu(s,SYNC))   { switchMatch(false);
      } else if( isMenu(s,SYNCPROJ))   { switchMatch(true);
      } else if( isMenu(s,PANEL1) || isMenu(s,PANEL1) || isMenu(s,PANEL2C) || isMenu(s,PANEL2L) 
              || isMenu(s,PANEL4) || isMenu(s,PANEL9) || isMenu(s,PANEL16))   { panel(s);
      } else if( isMenu(s,PAN))    { pan();
      } else if( isMenu(s,RSAMP))  { rsamp();
      } else if( isMenu(s,RGB))    { RGB();
      } else if( isMenu(s,MOSAIC)) { blink(1);
      } else if( isMenu(s,BLINK))  { blink(0);
      } else if( isMenu(s,GREY))   { grey();
      } else if( isMenu(s,GLASS))  { glass();
      } else if( isMenu(s,GLASSTABLE))  { glassTable();
      } else if( isMenu(s,FILTER)) { filter();
      } else if( isMenu(s,FOLD))   { fold();
      } else if( isMenu(s,INFOLD)) { inFold();
      } else if( isMenu(s,SELECT)) { select();
      } else if( isMenu(s,SELECTTAG)) { selecttag();
      } else if( isMenu(s,TAGSELECT)) { tagselect();
      } else if( isMenu(s,DETAG))  { untag();
      } else if( isMenu(s,SEARCH)) { search();
      } else if( isMenu(s,SELECTALL)){ selectAll();
      } else if( isMenu(s,UNSELECT)) { unSelect();
      } else if( isMenu(s,TABLEINFO))  { tableInfo(null);
      } else if( isMenu(s,CLONE1))  { cloneObj(true);
      } else if( isMenu(s,CLONE2))  { cloneObj(false);
      } else if( isMenu(s,PLOTCAT))  { createPlotCat();
      } else if( isMenu(s,CONCAT1)){ concat(true);
      } else if( isMenu(s,CONCAT2)){ concat(false);
      } else if( isMenu(s,CROP))   { crop();
      } else if( isMenu(s,HPXCREATE))   { createHpx();
      } else if( isMenu(s,HPXDUMP))   { crop();
      } else if( isMenu(s,COPY))   { copy();
      } else if( isMenu(s,TRANSP)) { transparency();
      } else if( isMenu(s,TRANSPON)) { transpon();
      } else if( isMenu(s,CUT))    { view.deSelect(); graphic(ToolBox.DIST);
      } else if( isMenu(s,STATSURFCIRC))    { view.deSelect(); graphic(ToolBox.PHOT);
      } else if( isMenu(s,STATSURFPOLY))    { view.deSelect(); graphic(ToolBox.DRAW);
      } else if( isMenu(s,CUT))    { view.deSelect(); graphic(ToolBox.DIST);
      } else if( isMenu(s,DIST))   { graphic(ToolBox.DIST);
      } else if( isMenu(s,NTOOL))  { newPlanTool();
      } else if( isMenu(s,DRAW))   { graphic(ToolBox.DRAW);
      } else if( isMenu(s,PHOT))   { graphic(ToolBox.PHOT);
      } else if( isMenu(s,TAG))    { graphic(ToolBox.TAG);
      } else if( isMenu(s,PROP) )  { prop();
      } else if( isMenu(s,DEL) )   { delete();
      } else if( isMenu(s,DELALL) ){ reset();
      } else if( isMenu(s,CALIMG) ){ launchRecalibImg(null);
      } else if( isMenu(s,CALCAT) ){ launchRecalibCat(null);
      } else if( isMenu(s,SIMBAD) ){ simbadPointer();
      } else if( isMenu(s,VIZIERSED) ){ vizierSED();
      } else if( isMenu(s,AUTODIST) )   { autodist();
//      } else if( isMenu(s,TIP) )   { tip();
      } else if( isMenu(s,SESAME) ){ sesame();
      } else if( isMenu(s,COOTOOL) ){ cooTool();
      } else if( isMenu(s,PIXELTOOL) ){ pixelTool();
      } else if( isMenu(s,CALCULATOR) ){ calculator();
//      } else if( isMenu(s,CEA_TOOLS) ){ showCEATools();
      } else if( isMenu(s,MACRO) ) { macro();
      } else if( isMenu(s,PREF) )  { preferences();
      } else if( isMenu(s,NEW) )   { windows();
      } else if( isMenu(s,ROI) )   { roi();
      } else if( isMenu(s,MCLOSE) ){ quit(0);
      } else if( isMenu(s,ARITHM) ){ updateArithm();
      } else if( isMenu(s,MOCGENIMG) ){ updateMocGenImg();
      } else if( isMenu(s,MOCGENIMGS) ){ updateMocGenImgs();
      } else if( isMenu(s,MOCGENCAT) ){ updateMocGenCat();
      } else if( isMenu(s,MOCM) )  { updateMocOp();
      } else if( isMenu(s,MOCTOORDER) ) { updateMocToOrder();
      } else if( isMenu(s,MOCCROP) )  { crop();
      } else if( isMenu(s,MOCHELP) )  { info(chaine.getString("MOCHELP"));
      } else if( isMenu(s,MOCLOAD) )  { loadMoc();
      } else if( isMenu(s,MOCHIPS) )  { loadMocHips();
      } else if( isMenu(s,MOCFILTERING) )  { updateMocFiltering();
      } else if( isMenu(s,CONV) )  { updateConvolution();
      } else if( isMenu(s,HEALPIXARITHM) ){ updateHealpixArithm();
      } else if( isMenu(s,NORM) )  { norm();
      } else if( isMenu(s,BITPIX) )  { updateBitpix();
      } else if( isMenu(s,PIXEXTR) )  { new FramePixelExtraction(this);
      } else if( isMenu(s,HEAD) )  { header();
      } else if( isMenu(s,HPXGENERATE)){ buildAllsky();
      } else if( isMenu(s,FOVEDITOR))  { buildFoV();
      } else if( isMenu(s,TOPBOTTOM) )  { flip(0);
      } else if( isMenu(s,RIGHTLEFT) )  { flip(1);
      } else if( isMenu(s,REGISTER) ) { getMessagingMgr().register(false, true);
      } else if( isMenu(s,UNREGISTER) ) {
          if (getMessagingMgr().unregister()) {
              this.dontReconnectAutomatically = true;
          }
      } else if( isMenu(s,STARTINTERNALHUB) ) { getMessagingMgr().startInternalHub();
      } else if( isMenu(s,STOPINTERNALHUB) ) { getMessagingMgr().stopInternalHub(false);
      } else if( isMenu(s,BROADCAST) ) { broadcastSelectedPlanes(null);
      } else if( isMenu(s,SAMPPREFS) ) { plasticWidget.showPrefs();
      } else if( isMenu(s,JUNIT) ) { test();
      } else if( isMenu(s,MQUIT) )  {
         if( isPrinting() ) {
            Aladin.warning(chaine.getString("PRINTING"));
            return true;
         }
		 quit(0);

      // Peut ?tre une convolution pr?d?finie
//      } else if( conv(s) ) { return true;

      // Peut ?tre un save
      } else if( save(s) ) { return true;

      // Peut ?tre un fond de ciel
      } else if( allsky(s) ) { return true;

      // Peut ?tre une application VO plastic
      } else if( appli(s) ) { return true;

      // Mode outreach, acc?s direct sur derni?re position connue
      // ou demande de la position par d?faut
      } else if( /* OUTREACH &&*/ dialog!=null && dialog.submitServer(s) ) { return true;

      // Peut ?tre un filtre pr?d?fini
      }else if( filterB(s) ) { return true;

      // Peut ?tre un plugin ?
      } else if( plugins!=null ) {
         AladinPlugin ap = plugins.find(s);
         if( ap!=null ) {
            try { ap.start(); } catch( AladinException e1 ) {
               e1.printStackTrace();
               warning(this,chaine.getString("PLUGERROR")+"\n\n"+e1.getMessage());
            }
         }
      }
      return true;
    }

    /** Propose d'installer Aladin en standalone */
    void install() {
       if( !confirmation(chaine.getString("INSTALLSA")) ) return;
       glu.showDocument("AladinJava.SA","");

    }

    /** Affichage des propri?t?s du premier planImage s?lectionn? dans
     * le cas o? l'utilisateur peut changer sa transparence */
    void transparency() {
       PlanImage pi = calque.getFirstSelectedPlanImage();
       if( pi==null ) return;
       Properties.createProperties(pi);
    }

    /** Positionne toutes les transparences des images de la pile ? 100%, respectivement ? 0% */
    void transpon() {
       float val=0f;
       if( miTranspon.isSelected() ) val=1f;
       calque.setOpacityLevelImage(val);
       calque.repaintAll();
    }

    /** Ex?cution de l'inversion verticale ou horizontale du plan de base */
    protected void flip(int methode) {
       try { flip(calque.getFirstSelectedSimpleImage(),methode); }
       catch( Exception e) { e.printStackTrace(); }
    }

    /** Ex?cution de l'inversion verticale ou horizontale */
    protected void flip(PlanImage p,int methode) throws Exception {
       aladin.console.printCommand("flipflop "+(methode==0 ? "V" : "H"));
       aladin.view.flip(p,methode);
    }

    /** Affichage du header fits de l'image courante */
    protected void header() {
       header(calque.getFirstSelectedPlan());
    }

    /** Affichage du header fits du plan pass? en param?tre */
    protected void header(Plan plan) {
       if( plan==null ) return;
       if( plan instanceof PlanImage ) ((PlanImage)plan).headerFits.seeHeaderFits();
       else if( plan instanceof PlanFolder ) ((PlanFolder)plan).headerFits.seeHeaderFits();
       else ((PlanCatalog)plan).headerFits.seeHeaderFits();
    }

//    /** Ex?cute une convolution sur le plan de base */
//    protected boolean conv(String kernel) {
//       if( Kernel.findKernel(kernel)<0 ) return false;
//       command.execLater("conv "+kernel);
//       return true;
//    }

    
    /** Ex?cute une normalisation sur le plan de base */
    protected void norm() {
       command.execLater("norm");
    }

    /** Ex?cute une op?ration arithm?tique sur les deux plans images s?lectionn?s */
//    protected void arithmetic(String op) {
//       Vector v = calque.getSelectedPlanes();
//       PlanImage p1,p2;
//       try {
//          p1 = (PlanImage)v.elementAt(0);
//          p2 = (PlanImage)v.elementAt(1);
//          command.exec(Tok.quote(p1.getLabel())+" "+op+" "+Tok.quote(p2.getLabel()));
//       } catch( Exception e ) { e.printStackTrace(); }
//    }

    /** Cr?ation d'un nouveau folder dans la pile */
    protected void fold() {
       int n=calque.newFolder(null,0,false);
       Plan p = calque.getPlan(n);
       if( p!=null ) console.printCommand("md "+p.getLabel());
    }

    /** Insertion des plans s?lectionn?s dans un nouveau folder de la pile */
    protected void inFold() {
       calque.select.insertFolder();
    }

//    /** Affichage du metadata tree g?n?ral */
//    protected void history() {
//       treeView.toFront();
//       treeView.show();
//    }

    /** Affiche les informations sur les colonnes du PlanCatalog
     * pass? en param?tre, ou si null, tous les plans catalogues s?lectionn?s */
    protected void tableInfo(Plan p) {
       if( p!=null )  new FrameInfoTable(aladin,p);
       else {
          Vector v = calque.getSelectedPlanes();
          Enumeration e = v.elements();
          FrameInfoTable f,of=null;
          while( e.hasMoreElements() ) {
             p = (Plan)e.nextElement();
             if( !p.isCatalog() ) continue;
             if( !p.flagOk ) continue;
             f = new FrameInfoTable(aladin,p);
             if( of!=null ) f.setLocation(of.getLocation().x+60,of.getLocation().y+40);
             of=f;
          }
       }
    }

    /** Activation du CLONE des objects depuis la JBar */
    protected void cloneObj(boolean uniqTable) {
       calque.newPlanCatalogBySelectedObjet(uniqTable);
    }
    
    /** Cr?ation d'un graphe de nuage de points sur le plan Catalog s?lectionn? */
    protected void createPlotCat() {
       PlanCatalog p = calque.getFirstSelectedPlanCatalog();
       if( p==null ) return;
       if( !view.getCurrentView().isFree() && !view.isMultiView() ) view.setModeView(ViewControl.MVIEW2L);
       int nview = aladin.view.getLastNumView(p);
       view.setPlanRef(nview, p);
       view.viewSimple[nview].addPlotTable(p, 0, 1,true);
    }

    /** Activation du CONCAT des objects depuis la JBar */
    protected void concat(boolean uniqTable) {
       calque.newPlanCatalogByCatalogs(null,uniqTable);
    }

    /** Activation du COPY depuis la JBar */
    protected void copy() {
       PlanImage pi = calque.getFirstSelectedSimpleImage();
       command.execLater("copy "+Tok.quote(pi.getLabel()));
    }

    /** Activation du DUMP depuis la JBar */
    protected void crop() {
       toolBox.setMode(ToolBox.CROP, Tool.DOWN);
    }

    /** Cr?ation d'un fichier map HEALpix ? partir d'un PlanImage et affichage de cette map */
    protected void createHpx() {
       final PlanImage pi = calque.getFirstSelectedSimpleImage();
       pi.flagProcessing=true;

       calque.select.repaint();
       String name = pi.label.replace(' ','-').replace('/','-').replace('\\','-');
       int i = name.lastIndexOf('.');
       if( i>0 ) name = name.substring(0,i);
       final String filename = name+".hpx";
       info("Aladin is creating an HEALPix file map from the image plane \""+pi.label+"\"\n" +
             "=> \""+aladin.getFullFileName(filename)+"\"...\n" +
       "After this step, it will reload it automatically in a new plane.");

       (new Thread("createHpx"){
          @Override
        public void run() {
             try {
                save.saveImage(filename,pi,1);
                pi.flagProcessing=false;
                calque.repaint();
                calque.newPlan(filename,filename,"Aladin HEALPix generation");
             } catch( Exception e ) { e.printStackTrace(); }
          }
       }).start();
    }

    /* Reset des donn?es */
    protected void reset() {
       view.unStickAll();
       view.setModeView(ViewControl.MVIEW1);
       calque.FreeAll();
       mesure.setReduced(true);
       gc();
       localisation.reset();
//       pixel.reset();
       dialog.setGrab(); // Desactivation du GrabIt ?
       command.reset();
       dialog.setDefaultTarget("");
       dialog.setDefaultTaille(ServerDialog.DEFAULTTAILLE);
       calque.repaintAll();
    }

    /** Active le widget Search */
    protected void search() {
       mesure.setReduced(false);
       search.focus();
    }

    /** Pour s?lectionner tous les objets des plans s?lectionn?s */
    protected void select() {
       calque.selectAllObjectInPlans();
    }

    /** Pour s?lectionner toutes les sources marqu?es */
    protected void selecttag() {
       calque.selectAllObject(2);
    }

    /** Pour marquer toutes les sources s?lectionn?es */
    protected void tagselect() {
       aladin.mesure.tag();
    }

    /** D?tague toutes les sources marqu?es */
    protected void untag() {
       calque.untag();
    }

    /** Pour s?lectionner tous les objets */
    protected void selectAll() {
       calque.selectAllObject(0);
    }

    /** Pour d?s?lectionner tous les objets */
    protected void unSelect() {
       view.deSelect();
       calque.repaintAll();
    }

    /** POur afficher les propri?t?s des plans s?lectionn?s */
    protected void prop() {
       calque.select.propertiesOfSelectedPlanes();
    }
    
    

    /** Pour un ADDCOL */

    protected void addCol() {
       Plan p=calque.getFirstSelectedPlan();
       if( p!=null && p.isSimpleCatalog() ) addCol(p);
    }

    protected void addCol(Plan p) {
       trace(1,"Starting Add Column tool...");
       if( frameCalc==null ) frameCalc = new FrameColumnCalculator(this);
       frameCalc.update(p);
       frameCalc.setVisible(true);
       frameCalc.toFront();
    }

     /** Pour un XMATCH */
     protected void xmatch() {
        trace(1,"Xmatching in progress...");
        if( frameCDSXMatch==null ) {
           trace(1,"Creating the XMatch window");
           frameCDSXMatch = new FrameCDSXMatch(this);
        }
        frameCDSXMatch.update();
     }

    /** Cr?ation d'un nouveau filtre et affichage des propri?t?s
     * correspondantes */
    protected void filter() {
       Plan p = view.calque.newPlanFilter();
       if( p!=null ) Properties.createProperties(p);
       calque.repaintAll();
    }

    /**
     * Cr?ation d'un filtre pour d?butant via la JBar
     * PF 24/8/2007
     * @param label nom du filtre
     * @return false si le filtre est inconnu
     */
    protected boolean applyBeginnerFilter(String label) {
       int idx = Util.indexInArrayOf(label, FilterProperties.BEGINNER_FILTER);
       if( idx<0 ) return false;

       // lorsque le label est null, le nom du filtre est dans la definition
       PlanFilter pf = (PlanFilter)calque.newPlanFilter("Filter", FilterProperties.BEGINNER_FILTERDEF[idx]);
       if( pf!=null ) {
          pf.setActivated(true);
          pf.updateState();

          // affichage dans la console de la commande script ?quivalente
          aladin.console.printCommand("filter "+pf.label+" {\n"+pf.script+"\n}");
       }
       return true;
    }

    /** Cr?ation d'un filtre pr?d?fini
     * @param s Nom du filtre
     * @return false si le filtre n'existe pas
     */
    protected boolean filterB(String s) {
       if( applyBeginnerFilter(s) ) {
          calque.select.repaint();
          return true;
       }
       return false;
    }

    /** Positionnement du nombre de vues via la JBar */
    protected void panel(String s) {
       try {
          int n = Integer.parseInt(s.substring(0,s.indexOf(' ')));
          if( s.indexOf("hor")>=0 ) n++;
          view.setModeView(n);
       } catch( Exception e ) {}
    }

    /** Positionnement du mode du r?ticule via la JBar */
    protected void reticle(int mode) {
       calque.setReticle(mode);
       console.printCommand("reticle "+(!calque.hasReticle() ? "off" : calque.reticleMode==1?"on" : "large" ));
       calque.repaintAll();
    }

    /** Activation ou d?sactivation du r?ticule via la Jbar */
    protected void target() {
       calque.setOverlayFlag("target", miTarget.isSelected() );
//       console.setCommand("target "+(calque.hasTarget()?"on":"off"));
       console.printCommand("setconf overlays="+( calque.hasTarget()?"+":"-" )+"target");
       calque.repaintAll();
    }

    /** Activation ou d?sactivation des infos d'overlays colormap via la Jbar */
    protected void rainbow() {
       view.showRainbow(miRainbow.isSelected());
       view.repaintAll();
    }

    /** Activation ou d?sactivation des infos d'overlays via la Jbar */
    protected void overlay() {
       calque.setOverlay(miOverlay.isSelected());
       console.printCommand("overlay "+(calque.flagOverlay?"on":"off"));
       calque.repaintAll();
    }

    /** Activation ou d?sactivation de la grille via la Jbar */
    protected void grid() {
       calque.setGrid(miGrid.isSelected(),true);
       calque.repaintAll();
    }
    
    /** Permute l'activation/d?sactivation de la grille HEALPix */
    public void switchHpxGrid() {
       miHpxGrid.setSelected( !miHpxGrid.isSelected() );
       hpxGrid();
    }

    /** Activation ou d?sactivation de la grille HEALPix via la Jbar */
    public void hpxGrid() { hpxGrid(miHpxGrid.isSelected()); }
    public void hpxGrid(boolean flag) {
       calque.setOverlayFlag("hpxgrid", flag );
       view.newView();
       view.repaintAll();
    }

    /** Activation ou d?sactivation de la synchronisation des vues
     * @param byProjection : true si synchronisation par projection, sinon par zoom
     */
    protected void switchMatch(boolean byProjection) {
       int syncMode = sync.getMode();
       boolean syncOk = syncMode==2 && byProjection ||
                        syncMode==3 && !byProjection ? true :
                              view.switchSelectCompatibleViews();
       match( !syncOk ? 0 : byProjection ? 3 : 2);
    }

    /** Cycle sur les modes match (aucun, simple match, match + orientation)
     */
    protected void cycleMatch() {
       int syncMode = sync.getMode();
       view.switchSelectCompatibleViews();
//       int mode = syncMode==3 ? 0 : syncMode==1 ? 2 : 3;
       int mode = syncMode==3 ? 0 :  3;
       if( (mode==2 || mode==3) && !view.isSelectCompatibleViews() ) view.selectCompatibleViews();
       else if( mode==0 || mode==1 && view.isSelectCompatibleViews() ) view.unselectViewsPartial();
       match(mode);
    }

    /** Positionnement du match
     * @param mode 0 ou 1 arr?t, 2 par zoom, 3 par zoom et rotation (projection)
     */
    protected void match(int mode) {
       if( mode==2 || mode==3 ) {
          sync.megaSync=mode==3;
//          view.setZoomRaDecForSelectedViews(aladin.calque.zoom.getValue(),null);
          view.setZoomRaDecForSelectedViews(0,null);
          log("match",mode==3?"scale+angle":"scale");
       } else {
          view.repaintAll();
          sync.megaSync=false;
       }
       sync.repaint();
    }

    /** Activation ou d?sactivation du zoom point? via la Jbar */
    protected void zoom() {
       if( miZoomPt.isSelected() ) {
          toolBox.tool[ToolBox.SELECT].mode=Tool.UP;
          toolBox.tool[ToolBox.ZOOM].mode=Tool.DOWN;
       } else {
          toolBox.tool[ToolBox.SELECT].mode=Tool.DOWN;
          toolBox.tool[ToolBox.ZOOM].mode=Tool.UP;
       }
       toolBox.repaint();
    }

    /** Activation ou d?sactivation du panning via la Jbar */
    protected void pan() { pan(miPan.isSelected()); }
    protected void pan(boolean mode) {
       if( mode ) {
          toolBox.tool[ToolBox.SELECT].mode=Tool.UP;
          toolBox.tool[ToolBox.PAN].mode=Tool.DOWN;
       } else {
          toolBox.tool[ToolBox.SELECT].mode=Tool.DOWN;
          toolBox.tool[ToolBox.PAN].mode=Tool.UP;
       }
       toolBox.repaint();
       view.setDefaultCursor();
    }
    
    protected void newPlanTool() {
       Plan p = calque.createPlanTool(null);
       console.printCommand("draw newtool("+Tok.quote(p.label)+")");
    }

    /** Activation d'un des outils graphiques via la Jbar */
    protected void graphic(int n) {
       toolBox.setGraphicButton(n);
       calque.selectPlanTool();
       toolBox.repaint();
   }

    /** Activation ou d?sactivation du GREY via la Jbar */
    protected void grey() {
       aladin.console.printCommand("grey");
       view.calque.newPlanImage((PlanImageRGB)(view.getCurrentView().pref));
    }

    /** Activation ou d?sactivation du MGLASS via la Jbar */
    protected void glass() {
       if( miGlass.isSelected() ) toolBox.tool[ToolBox.WEN].mode=Tool.DOWN;
       else toolBox.tool[ToolBox.WEN].mode=Tool.UP;

       aladin.calque.zoom.zoomView.setPixelTable(miGlassTable.isSelected());

       calque.repaintAll();
    }

    /** Activation ou d?sactivation du MGLASS via la Jbar */
    protected void glassTable() {
       if( miGlassTable.isSelected()) toolBox.tool[ToolBox.WEN].mode=Tool.DOWN;
       else if( !miGlass.isSelected() )toolBox.tool[ToolBox.WEN].mode=Tool.UP;

       aladin.calque.zoom.zoomView.setPixelTable(miGlassTable.isSelected());

       calque.repaintAll();
    }

    /** Suppression soit des objets, soit des vues s?lectionn?es,
     * soit des plans suivant le dernier clic */
    protected void delete() {
       if( view.isDelSelObjet() ) view.delSelObjet();
       else if( view.isViewSelected() ) view.freeSelected();
       else calque.FreeSet(false);
    }

    /** Cr?ation de la fen?tre pour param?trer un r??chantillonnage */
    protected void rsamp() {
       new FrameResample(this);
    }

    /** Ouverture de la fen?tre des pixels avec maj du bouton pixel associ? */
    protected void pixel() {
       toolBox.tool[ToolBox.HIST].mode=Tool.DOWN;
       toolBox.repaint();
       updatePixel();
    }

    /** Mise ? jour de la fen?tre des pixels en fonction de la position du bouton associ? */
    public void updatePixel() {
       if( frameCM==null ) {
          trace(1,"Creating the colormap window");
          frameCM = new FrameColorMap(this);
       }
       boolean visible = toolBox.tool[ToolBox.HIST].mode==Tool.DOWN;
       frameCM.setVisible(visible);
       if( visible ) frameCM.majCM();
    }

    /** Ouverture de la fen?tre des RGB avec maj du bouton associ? */
    protected void RGB() {
       toolBox.tool[ToolBox.RGB].mode=Tool.DOWN;
       toolBox.repaint();
       updateRGB();
    }

    /** Mise ? jour de la fen?tre pour la construction d'une RGB */
    protected void updateRGB() {
       if( frameRGB==null ) {
          trace(1,"Creating the RGB window");
          frameRGB = new FrameRGB(aladin);
       }
       frameRGB.maj();
    }

    /** Mise ? jour de la fen?tre pour les operations arithmetiques */
    protected void updateArithm() {
       if( frameArithm==null ) {
          trace(1,"Creating the Arithmetic window");
          frameArithm = new FrameArithmetic(aladin);
       }
       frameArithm.maj();
    }
    
    /** Mise ? jour de la fen?tre pour les operations des MOCs */
    protected void updateMocFiltering() {
       if( frameMocFiltering==null ) {
          trace(1,"Creating the MocOp window");
          frameMocFiltering = new FrameMocFiltering(aladin);
       }
       frameMocFiltering.maj();
    }
    
    /** Chargemetn du MOC correspondant au plan HiPS courant  */
    protected void loadMocHips() {
       Plan p = calque.getFirstSelectedPlan();
       if( p==null || p instanceof PlanMoc || !(p instanceof PlanBG) || !((PlanBG)p).hasMoc() ) p=calque.getPlanBase();
       if( p==null || p instanceof PlanMoc || !(p instanceof PlanBG) || !((PlanBG)p).hasMoc() ) return;
       ((PlanBG)p).loadMoc();
    }
    
    private boolean loadMocFirst=true;
    
    /** Mise ? jour de la fen?tre pour les operations des MOCs */
    protected void loadMoc() {
       dialog.show("VizieR");
       if( loadMocFirst ) SwingUtilities.invokeLater(new Runnable() {
          public void run() {
             info(dialog,chaine.getString("MMOCLOADHELP"));
          }
       });   
       loadMocFirst=false;
    }

    /** Mise ? jour de la fen?tre pour les operations des MOCs */
    protected void updateMocOp() {
       if( frameMocOperation==null ) {
          trace(1,"Creating the MocOp window");
          frameMocOperation = new FrameMocOperation(aladin);
       }
       frameMocOperation.maj();
    }
    
    /** Mise ? jour de la fen?tre pour la g?n?ration d'un MOC ? partir d'un autre MOC de meilleure r?solution */
    protected void updateMocToOrder() {
       if( frameMocGenRes==null ) {
          trace(1,"Creating the MocGenRes window");
          frameMocGenRes = new FrameMocGenRes(aladin);
       }
       frameMocGenRes.maj();
    }

    /** Mise ? jour de la fen?tre pour la g?n?ration d'un MOC */
    protected void updateMocGenCat() {
       if( frameMocGenCat==null ) {
          trace(1,"Creating the MocGenCat window");
          frameMocGenCat = new FrameMocGenCat(aladin);
       }
       frameMocGenCat.maj();
    }

    /** Mise ? jour de la fen?tre pour la g?n?ration d'un MOC ? partir d'une collection d'images */
    protected void updateMocGenImgs() {
       if( frameMocGenImgs==null ) {
          trace(1,"Creating the MocGenImgs window");
          frameMocGenImgs = new FrameMocGenImgs(aladin);
       }
       frameMocGenImgs.maj();
    }

    /** Mise ? jour de la fen?tre pour la g?n?ration d'un MOC */
    protected void updateMocGenImg() {
       if( frameMocGenImg==null ) {
          trace(1,"Creating the MocGenImg window");
          frameMocGenImg = new FrameMocGenImg(aladin);
       }
       frameMocGenImg.maj();
    }

    /** Mise ? jour de la fen?tre pour les operations de convolutions */
    protected void updateConvolution() {
       if( frameConvolution==null ) {
          trace(1,"Creating the Convolution window");
          frameConvolution = new FrameConvolution(aladin);
       }
       frameConvolution.maj();
    }

    /** Mise ? jour de la fen?tre pour les operations arithmetiques sur plans Healpix*/
    protected void updateHealpixArithm() {
       if( frameHealpixArithm==null ) {
          trace(1,"Creating the Arithmetic window");
          frameHealpixArithm = new FrameHealpixArithmetic(aladin);
       }
       frameHealpixArithm.maj();
    }
    
    /** Mise ? jour de la fen?tre pour les operations arithmetiques */
    protected void updateBitpix() {
       if( frameBitpix==null ) {
          trace(1,"Creating the Bitpix window");
          frameBitpix = new FrameBitpix(aladin);
       }
       frameBitpix.maj();
    }

    /** Ouverture de la fen?tre des blinks avec maj du bouton associ? */
    protected void blink(int mode) {
       toolBox.tool[ToolBox.BLINK].mode=Tool.DOWN;
       toolBox.repaint();
       updateBlink(mode);
    }

    /** Mise ? jour de la fen?tre pour la construction
     * d'un BLINK (mode=0), ou d'une MOSAIC (mode=1) */
    protected void updateBlink(int mode) {
       if( frameBlink==null ) {
          trace(1,"Creating the Blink window");
          frameBlink = new FrameBlink(aladin);
       }
       frameBlink.maj();
       frameBlink.setMode(mode);
    }

    /** Ouverture de la fen?tre des Contours avec maj du bouton pixel associ? */
    protected void contour() {
       toolBox.tool[ToolBox.CONTOUR].mode=Tool.DOWN;
       toolBox.repaint();
       updateContour();
    }

    /** Mise ? jour de la fen?tre des Contours en fonction de la position du bouton associ? */
    protected void updateContour() {
       if( frameContour==null ) {
          trace(1,"Creating the Contour window");
          frameContour = new FrameContour(this);
       }
       frameContour.majContour();
    }

    /**
     * Affiche la fen?tre de gestion des "macros"
     * (mode script ?volu?, portant sur une liste de param?tres)
     */
    protected void macro() {
        FrameMacro fm = getFrameMacro();
        fm.setVisible(true);
        fm.toFront();
    }

    protected synchronized FrameMacro getFrameMacro() {
        // lazy initialization
        if( frameMacro==null ) {
            frameMacro = new FrameMacro(this);
            // log usage of macro
            log("Macro", "");
        }

        return frameMacro;
    }

    /** Creation d'une nouvelle instance d'Aladin */
    protected void windows() {
       if( isApplet() ) return;
       main(new String[]{});
       //       launch();
    }

    /**
     * Affiche la fenetre pour cr?er un allsky
     */
    protected void buildAllsky() {
       FrameAllskyTool.display(this);
    }

    /**
     * Affiche la fenetre pour cr?er un FoV
     */
    protected void buildFoV() {
       glu.showDocument("FovEditor","");
    }


    /** Terminaison propre d'Aladin */
    protected void quit(int code) {

       // Deselection des objets en cours dans le cas ou une application
       // type VOPlot est utilisee en parallele

       if( hasExtApp() ) try { resetCallbackVOApp(); } catch( Exception e) {}

       if( aladinSession==0 ) {
          trace(4,"Aladin.quit in progress... " );
          trace(3,"User configuration backup...");
          // Sauvegarde config utilisateur
//          console.printInfo("Aladin stopped");
          saveConfig();
          
          // Arr?t d'un ?ventuel calcul de allsky
          try {
             Context context = frameAllsky!=null && frameAllsky.context!=null ? frameAllsky.context
                   : command.hipsgen!=null && command.hipsgen.context!=null ? command.hipsgen.context : null;
            if( context!=null && context.isTaskRunning() ) {
               context.taskAbort();
               long t = System.currentTimeMillis();
               while( context.isTaskRunning() && System.currentTimeMillis()-t<3000 ) Util.pause(100);
            }
         } catch( Exception e1 ) { }

          // Suppression d'un cache ?ventuel
          trace(3,"Cache cleaning...");
          stopCacheUpdater();
          removeCache();
          
          // Nettoyage de la pile
          try { calque.FreeAll(); } catch( Exception e ) {}
       }

       // appel des m?thodes cleanup() des plugins
       if( plugins!=null ) {
          trace(3,"Plugin cleaning...");
          try { plugins.cleanup(); } catch( Exception e ) {}
       }

       if( aladinSession>0 || flagLaunch ) { // Si Aladin demarre par launch() cacher la fenetre
//          System.out.println("Aladin.action: flagLaunch true => dispose");
          trace(3,"Slave session => not true exit() ...");
          reset();		// N?cessaire pour ne pas avoir de ressurections intempestives
          command.stop();
          f.setVisible(false);        // Pour une sombre histoire de bug MAC

       } else {         // Sinon terminer l'application

          if( isPrinting() || isSaving() || isLogging() ) {
             if( isPrinting() || isSaving() ) trace(3,"Print or Save in progress => waiting...");
             f.setVisible(false);
             long t=System.currentTimeMillis();
             while( isPrinting() || isSaving() || isLogging() ) {
                Util.pause(500);
                if( System.currentTimeMillis()-t>5*60000 ) break; // Au-del? de 5 minutes on quitte
             }
          }
          trace(3,"See you !");
          System.exit(code);
       }

    }

    /** JE NE SAIS PAS QUI A RAJOUTE CELA ??? LE CFH ?? */
    public void shut() { Thread.currentThread().stop(); }

    /** Ouverture de la fen?tre des pr?f?rences utilisateur */
    protected void preferences() {
    	configuration.show();
    }


    /** retourne l'instance de FrameInfo actuellement utilis?e
     * En cr?e une si n?cessaire */
    protected synchronized FrameInfo getFrameInfo() {
    	// lazy initialization
    	if( frameInfo==null ) frameInfo = new FrameInfo(this);

    	return frameInfo;
    }

//    /** Lancement de VOPlot avec les objets selectionnes ou tous les objets
//     *  si aucun
//     */
//    protected void voplot() {
//        if( calque.isFree() ) return;
// trace(1,"Starting VOPlot...");
////       new Thread(){
////          public void run() {
//             try {
//
//                MyByteArrayStream s = writeObjectInVOTable();
//
//                extApp = launchVOPlot();
//                InputStream in = s.getInputStream();
//                extApp.loadVOTable(aladin,in);
//                addVOAppObserver(extApp);
//                glu.log("VOPlot","starting");
//             }catch( IOException ioe ) {
//                if( levelTrace>=3 ) ioe.printStackTrace();
//                warning(chaine.getString("VOPLOTERR1"), 1);
//             }
//             catch( Exception es ) {
//                if( levelTrace>=3 ) es.printStackTrace();
//                warning(chaine.getString("VOPLOTERR")+"\n"+es);
//                glu.log("VOPlot","Too+old+JVM+for+VOPlot");
//             }
////          }
////       }.start();
//    }

    protected void sesame() {
       localisation.focus(Localisation.YOUROBJ);
    }
    
    protected void calculator() {
       localisation.focus(chaine.getString("YOUREXPR"),"= ");
    }
    
    
    protected FrameCooToolbox frameCooTool=null;
    protected void cooTool() {
       if( frameCooTool==null ) frameCooTool=new FrameCooToolbox(this);
       else frameCooTool.setVisible(true);
    }

    protected FramePixelToolbox framePixelTool=null;
    protected void pixelTool() {
       if( framePixelTool==null ) framePixelTool=new FramePixelToolbox(this);
       else framePixelTool.setVisible(true);
    }

    /** Lancement ou arr?t du mode Simbad Pointer */
    protected void simbadPointer() {
       calque.setSimbad(!calque.flagSimbad);
    }

    /** Lancement ou arr?t du mode VizieR SED Pointer */
    protected void vizierSED() {
       calque.setVizierSED(!calque.flagVizierSED);
    }
    
  /** Activation ou d?sactivation de l'outil de mesure automatique des distance */
  protected void autodist() {
     calque.setAutoDist(!calque.flagAutoDist);
  }


//    /** Activation ou d?sactivation des tooltips sur les objets */
//    protected void tip() {
//       calque.flagTip=!calque.flagTip;
//    }

   /** D?marrage d'une extraction de vignettes ROI */
   protected void roi() { view.createROI(); }

   /** lance le pad */
   private void console() {
      if( view.hasSelectedObj() ) view.selObjToPad();
      console.show();
      console.toFront();
   }

   /** lance un tutorial */
   private void launchTuto(String s) {
       if( (!calque.isFree() || view.isMultiView())
          && !confirmation(chaine.getString("DEMO")+":\n- \""+s+
                        "\" -\n"+chaine.getString("DEMO1")) ) return;

       // en applet sign?, on force l'affichage de l'applet dans sa propre frame
       if( isCertifiedApplet() && !flagDetach ) detach();

       // la chaine se pr?sente sous la forme 'Tutorial.Show-me-how-to' dans Aladin.string
       String tutoStr = chaine.getString("Tutorial."+s.replaceAll(" ", "-"));
       if( tutoStr!=null ) {
           reset();
           log("Tutorial",s);
           command.readFromStream(new ByteArrayInputStream(tutoStr.getBytes()));
           if( command.infoTxt!=null )
               command.infoTxt.setText("");

           command.curTuto = s;
       }
   }

   /** Lancement d'une recalibration sur une image*/
   protected void launchRecalibImg(Plan p) {
      if( p==null ) p = calque.getFirstSelectedSimpleImage();
      Plan [] plan=null;
      if( p==null ) {
         plan = calque.getPlans();
         for( int i=0; i<plan.length; i++ ) {
            if( plan[i].isImage() && !Projection.isOk(plan[i].projd) ) { p=plan[i]; break; }
         }
      }
      if( p==null ) {
         for( int i=0; i<plan.length; i++ ) {
            if( plan[i].isImage() ) { p=plan[i]; break; }
         }
      }

      if( p==null ) {
         warning(chaine.getString("NEEDIMG"));
         return;
      }
      if( frameNewCalib==null ) {
         frameNewCalib = new FrameNewCalib(this,p,null);
      } else frameNewCalib.majFrameNewCalib(p);

   }

   /** Lancement d'une recalibration sur un catalog*/
   protected void launchRecalibCat(Plan p) {
      try {
         if( p==null ) p = calque.getFirstSelectedPlan();
      } catch( Exception e ) {}
      if( p==null || !p.isCatalog() ) {
         warning(chaine.getString("NEEDCAT"));
         return;
      }

      trace(1,"Recalibrating catalog in progress...");
      if( frameNewCalib==null ) {
         frameNewCalib = new FrameNewCalib(this,p,null);
      } else frameNewCalib.majFrameNewCalib(p);

   }

   /**
    * Broadcaste les plans s?lectionn?s aux applications PLASTIC
    * @param : 0 --> on broadcaste tout
    *          1 --> seulement les tables
    *          2 --> seulement les images
    * @param : tableau des destinataires. Si null, on envoie ? tout le monde
    */
   private void broadcastSelectedPlanes(int mask, String[] recipients) {
      boolean bdcastTab = (mask==0 || mask==1);
      boolean bdcastImg = (mask==0 || mask==2);
      getMessagingMgr().trace("Broadcasting selected planes using mask "+mask);
      if( ! Aladin.PLASTIC_SUPPORT || ! getMessagingMgr().isRegistered() ) return;
      Plan p;
      for( int i=calque.plan.length-1; i>=0; i-- ) {
         p = calque.plan[i];
         if( !p.selected ) continue;
         if( /* p.isSimpleCatalog() */ p.isCatalog() && bdcastTab ) getMessagingMgr().broadcastTable(p, recipients);
         else if( p.type==Plan.IMAGE && bdcastImg ) getMessagingMgr().broadcastImage(p, recipients);
      }
   }

   /**
    *
    * @param recipients
    * @see Aladin#broadcastSelectedPlanes(int, String[])
    */
   protected void broadcastSelectedPlanes(String[] recipients) {
      broadcastSelectedPlanes(0, recipients);
   }

   protected void broadcastSelectedTables(String[] recipients) {
   	broadcastSelectedPlanes(1, recipients);
   }

   protected void broadcastSelectedImages(String[] recipients) {
   	broadcastSelectedPlanes(2, recipients);
   }

    // Juste pour eviter que la classe com.jvt.applets.PlotVOApplet ne soit chargee
   // dans la version applet, notamment pour Explorer 5 qui veut charger une tonne
   // de classes inutiles juste pour faire plaisir au SecurityChecker
   private ExtApp launchVOPlot() throws Exception {
      ExtApp voplot=null;
      Class x = Class.forName("com.jvt.applets.PlotVOApplet");
      Method m = x.getDeclaredMethod("launch",new Class[]{});
      voplot =  (ExtApp)m.invoke((Object)null, new Object[] {});

      return voplot;
   }

   @Override
public void setLocation(int x, int y) {
      System.out.println("Aladin setLocation(x="+x+" y="+y+")");
      super.setLocation(x,y);
   }

   @Override
public void setLocation(Point p) {
      System.out.println("Aladin setLocation(p="+p.x+","+p.y+")");
      super.setLocation(p);
   }

   static public JLabel createLabel(String s) {
      JLabel l = new JLabel(s);
      l.setBorder(BorderFactory.createEmptyBorder(0,3,0,2));
      l.setFont(Aladin.BOLD);
      l.setForeground(DARKBLUE);
      return l;
   }
   
   /** retourne true s'il y a un r?seau disponible */
   static public boolean hasNetwork() { return NETWORK; }

   /** retourne true s'il s'agit d'un Aladin avec interface graphique (NOGUI) */
   static public boolean hasGUI() { return !NOGUI; }

   /** Retourne true s'il tourne en applet */
   static public boolean isApplet() { return SIGNEDAPPLET || !STANDALONE; }

   /** Retourne true s'il tourne en applet certifi? */
   static public boolean isCertifiedApplet() { return SIGNEDAPPLET; }

   /** Retourne true s'il tourne en applet non certifi? */
   static public boolean isNonCertifiedApplet() { return !STANDALONE && !SIGNEDAPPLET; }

   /** Retourne true s'il un acc?s non limit? au r?seau, au disque, ? l'imprimante... */
   static public boolean hasNoResctriction() { return STANDALONE; }

   /** Retourne true si Aladin est en mode fullscreen (ou preview) */
   final public boolean isFullScreen() { return fullScreen!=null; }
   
   /** Retourne true si Aladin est en mode PROTO */
   public boolean isProto() { return PROTO; }
   
   /** Retourne true si Aladin est en mode OUTREACH */
   public boolean isOutreach() { return OUTREACH; }

   
   /** D?s que je saurai le faire */
   protected boolean hasClipBoard() {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable tr=null;
      try {
         tr = clipboard.getContents(null);
      } catch( Exception e ) {
         clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
         try {
            tr = clipboard.getContents(null);
         } catch( Exception e1 ) { }
      }
      
      if( tr==null ) return false;
      DataFlavor [] df =tr.getTransferDataFlavors();
      for( DataFlavor df1 : df ) {
         if( df1.equals(DataFlavor.javaFileListFlavor) ) return true;
         if( df1.equals(DataFlavor.stringFlavor) ) return true;
      }
      return false;
   }
   
   protected void copier() {
      ViewSimple v = view.getCurrentView();
      v.copier();
   }
   
   protected void coller() {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      try {
         paste(clipboard.getContents(null) );
      } catch( Exception e ) {
         e.printStackTrace();
         clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
         try {
            paste( clipboard.getContents(null) );
         } catch( Exception e1 ) {
            e1.printStackTrace();
         }
      }
   }
   
   public synchronized void paste(Transferable tr) {
      try {
         
         if( tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
            java.util.List fileList = (java.util.List) tr.getTransferData(DataFlavor.javaFileListFlavor);
            Iterator iterator = fileList.iterator();
            while( iterator.hasNext() ) {
               File file = (File) iterator.next();
               calque.newPlan(file.getAbsolutePath(),file.getName(),null);
               console.printCommand("load "+file.getAbsolutePath());
            }

         } else if( tr.isDataFlavorSupported(DataFlavor.stringFlavor) ) {
            String s = (String)tr.getTransferData(DataFlavor.stringFlavor);
            ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
            calque.newPlan(in, "Data", "Clipboard");
            in.close();
         }

      } catch( Exception e ) {
         if( levelTrace>=3 ) e.printStackTrace();
         console.printError(e.getMessage());
      }
   }

   /** Copie du texte dans le clipboard de la machine
    *  (sous Unix/Linux, ? la fois dans le clipboard syst?me et dans le clipboard de s?lection)
    * @param text le texte ? mettre dans le presse-papiers
    */
   protected void copyToClipBoard(String text) {
      if( isNonCertifiedApplet() ) return;
      if( text==null ) return;
      Transferable selection = new StringSelection(text);
      copyToClipBoard(selection);
   }
   
   /** Copie du texte dans le clipboard de la machine
    *  (sous Unix/Linux, ? la fois dans le clipboard syst?me et dans le clipboard de s?lection)
    * @param L'image ? mettre dans le presse-papiers
    */
   protected void copyToClipBoard(Image img) {
      if( isNonCertifiedApplet() ) return;
      if( img==null ) return;
      TransferableImage selection = new TransferableImage( img );
      copyToClipBoard(selection);
   }

   /** Copie d'un objet dans le clipboard de la machine
    *  (sous Unix/Linux, ? la fois dans le clipboard syst?me et dans le clipboard de s?lection)
    * @param l'objet transferable
    */
   protected static void copyToClipBoard(Transferable selection) {
      // Il y a 2 clipboards :
      // - un dont le contenu est accessible par Ctrl-V (ou Pomme-V pour les MACeux)
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      if( clipboard!=null ) clipboard.setContents(selection, aladin);
      // - l'autre dont le contenu est accessible par le bouton du milieu
      // (mais on ne peut y acc?der que depuis Java 1.4 !) et il n'existe pas sous Windows
      //      if( Aladin.JAVAAFTER140 ) {
      clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
      if( clipboard!=null ) clipboard.setContents(selection, aladin);
      //      }
   }

   class TransferableImage implements Transferable { 
      private Image image; 
      public TransferableImage(Image uneImage){ 
         image = uneImage;    
      }    
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException{ 
         if(!isDataFlavorSupported(flavor)){throw new UnsupportedFlavorException(flavor);} 
         return image;    
      } 
      public DataFlavor[] getTransferDataFlavors(){ 
         return new DataFlavor[]{DataFlavor.imageFlavor}; 
      } 
      public boolean isDataFlavorSupported(DataFlavor flavor){ 
         return DataFlavor.imageFlavor.equals(flavor);    
      } 
   } 

   /** impl?mentation de l'interface ClipboardOwner */
   public void lostOwnership(Clipboard clipboard, Transferable contents) {}

   /** Sort du mode help si necessaire
    * @return true si on a quitte le mode Help
    */
   protected void helpOff() { setHelp(false); help.resetStack(); }

/** Passage en mode Help ou retour a la normale
    * @param vrai : true  -> passage en mode help
    *               false -> retour a la normal
    */
   protected void setHelp(boolean vrai) {
      if( vrai ) info(chaine.getString("HELPMOVE"));
      inHelp=vrai;
      help.setCenter(false);
      if( vrai ) {
         log("Help","interactive");
         localisation.setMode(MyBox.AFFICHAGE);
//         pixel.setMode(MyBox.AFFICHAGE);
         makeCursor(this,HANDCURSOR);
      } else {
         makeCursor(this,DEFAULTCURSOR);
      }

      // Desactivation des composantes innaccessibles
      if( !msgOn ) {
         Enumeration e = vButton.elements();
         while( e.hasMoreElements() ) {
            MyButton c = (MyButton) e.nextElement();
            c.enable(!vrai);       // je desactive si je suis en help
         }
      }
      setButtonMode();
      msgOn=false;
      cardView.show(bigView,inHelp?"Help":"View");
      if( vrai ) help.setDefault();
   }

   static final int DEFAULTCURSOR = 0;
   static final int WAITCURSOR 	= 1;
   static final int HANDCURSOR 	= 2;
   static final int CROSSHAIRCURSOR=3;
   static final int MOVECURSOR 	= 4;
   static final int RESIZECURSOR 	= 5;
   static final int TEXTCURSOR 	= 6;
   static final int TURNCURSOR    = 7;
   static final int PLANCURSOR    = 8;
   static final int STRECHCURSOR  = 9;
   static final int JOINDRECURSOR = 10;
   static final int TAGCURSOR = 11;

  /** Retourne le Frame parent */
   protected Frame getFrame(Component c) {
      while( c!=null && !(c instanceof Frame) ) c=c.getParent();
      return (Frame)c;
   }

   static final int MARGEB=25;  // Taille de la barre sous windows


   /** retourne le d?callage en absisse qu'il faut ajouter ? la position lors de
    * la cr?ation d'une nouvelle frame dans le cas de multi-?crans
    */
   static private int getMainWindowOffset() {
      try {
         Point p = aladin.f.getLocation();
         Dimension d = aladin.f.getSize();
         if( p.x+d.width<0 ) return -1024;  // 2?me ?cran ? gauche
         else if( p.x>SCREENSIZE.width )return SCREENSIZE.width; // 2?me ?cran ? droite
      } catch( Exception e) {}
      return 0; // Pas de deuxi?me ?cran ou probl?me
   }

   /** Retourne le positionnement des Frames utilis?es par Aladin
    *  en fonction de la taille de l'?cran et du nombre d'?crans afin que les fen?tres
    *  ent dans le m?me ?cran.
    *  @param f objet ? positionner
    *  @return la localisation
    */
  static protected Point computeLocation(Frame f) {
      Point p = computeLocation1(f);
      int offset = getMainWindowOffset();
      p.translate(offset, 0);
      return p;
   }
   static private Point computeLocation1(Frame f) {
      Dimension d;
      if( f instanceof FrameColorMap )       return new Point(180,aladin.getY());
      if( f instanceof FrameRGBBlink ) return new Point(500,500);
      if( f instanceof Properties )    return new Point(20,10);
      if( f instanceof ServerDialog )  return new Point(0,SCREENSIZE.height-f.getSize().height-MARGEB-100);
      if( f instanceof FrameNewCalib ) return new Point(0,100);
      if( f instanceof FrameInfo )     return new Point(100,0);
      if( f instanceof FrameServer )   return new Point(500,200);
      if( f instanceof FrameInfoServer)return new Point(20,200);
      if( f instanceof Save )          return new Point(200,200);
      if( f instanceof FrameContour )  return new Point(350,200);
      if( f instanceof FrameCDSXMatch )return new Point(100,200);
      if( f instanceof FrameColumnCalculator ) return new Point(20,250);
      if( f instanceof FrameHeaderFits ) return new Point(50,0);
      if( f instanceof Console )       return new Point(0,SCREENSIZE.height-f.getSize().height-MARGEB);
      if( f instanceof Configuration ) return new Point(20,10);
      if( f instanceof FrameInfoTable )return new Point(0,50);

      // Pour aladin lui-m?me
//      d = f.getSize();
//      int id = aladin.getInstanceId();
//      int x = 500+id*20;
//      if( x+d.width>SCREENSIZE.width ) x=SCREENSIZE.width-d.width;
//      int y = (SCREENSIZE.height-MARGEB)/2-d.height/2-100+id*55;

      d = f.getSize();
      int x = 500;
      if( x+d.width>SCREENSIZE.width ) x=SCREENSIZE.width-d.width;
      int y = (SCREENSIZE.height-MARGEB)/2-d.height/2-100;
      return new Point(x,y<0?0:y);

   }

   /** Ajoute un offset ? la position de la fen?tre dans le cas o? il s'agit d'une
    * fen?tre secondaire */
   private void offsetLocation() {
      int id = getInstanceId();
      if( id==0 ) return;
      Point p = f.getLocation();
      p.y+=id*55;
      p.x+=id*20;
      f.setLocation(p);
   }

   /** Curseurs pour la rotation des Apertures
    * et pour le d?placement d'un plan */
   static private Cursor turnCursor=null,planCursor=null,joindreCursor=null,tagCursor=null;

   static private int TURNCURSORDEF[][]={
    {0,0,0,0,0,0,0,0,0,0},
    {2,2,2,2,2,2,2,2,0,0},
	{2,1,1,1,1,1,1,2,0,0},
	{2,2,2,2,1,1,1,2,0,0},
	{0,0,2,1,1,1,1,2,0,0},
	{0,2,1,1,1,2,1,2,0,0},
	{0,2,1,1,2,2,1,2,0,0},
	{2,1,1,2,0,2,1,2,0,0},
	{2,1,1,2,0,0,2,2,0,0},
	{2,1,1,2,0,0,0,0,0,0},
	{2,1,1,2,0,0,0,0,0,0},
	{2,1,1,2,0,0,0,0,0,0},
	{0,2,1,1,2,0,0,0,0,0},
	{0,2,1,1,1,2,0,0,0,0},
	{0,0,2,1,1,1,2,2,2,2},
	{0,0,0,2,1,1,1,1,1,1},
	{0,0,0,0,2,1,1,1,1,1},
	{0,0,0,0,0,2,2,1,1,1},
	{0,0,0,0,0,0,0,2,2,2},
   };

   static private int PLANCURSORDEF[][]={
    {0,0,0,0,0,0,2,2,2,2,2,2,2,2,2,2},
    {0,0,0,0,0,2,1,1,1,1,1,1,1,1,1,1},
    {0,0,0,0,0,2,1,2,2,2,2,2,2,2,2,2},
    {0,0,0,0,2,1,2,2,2,2,2,2,2,2,2,2},
    {0,0,0,0,2,1,2,2,2,2,2,2,2,2,2,2},
    {0,0,0,2,1,2,2,2,2,2,2,2,2,2,2,2},
    {0,0,0,2,1,2,2,2,2,2,2,2,2,2,2,2},
    {0,0,2,1,2,2,2,2,2,2,2,2,2,2,2,2},
    {0,0,2,1,2,2,2,2,2,2,2,2,2,2,2,2},
    {0,2,1,2,2,2,2,2,2,2,2,2,2,2,2,2},
    {0,2,1,2,2,2,2,2,2,2,2,2,2,2,2,2},
    {2,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2},
    {2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    {2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    {2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2},
   };

   static private int JOINDRECURSORDEF[][]={
      {0,0,0,0,0,0,0,0,0,2,1,0,2,1,0,0},
      {0,0,0,0,0,0,0,0,0,0,2,1,0,2,1,0},
      {0,0,0,0,0,0,0,0,0,0,0,2,1,2,1,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,0},
      {0,0,0,0,0,0,0,0,0,0,1,2,2,2,1,0},
      {1,1,1,1,1,1,1,1,1,1,1,2,2,2,1,0},
      {2,2,2,2,2,2,2,2,2,2,1,2,2,2,1,0},
      {0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,2,1,2,1,0},
      {0,0,0,0,0,0,0,0,0,0,2,1,0,2,1,0},
      {0,0,0,0,0,0,0,0,0,2,1,0,2,1,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,2,1,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
     };

   static private int TAGCURSORDEF[][]={
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0},
      {0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0},
      {0,0,0,0,0,0,1,2,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,1,2,0,0,0,0,0},
      {0,0,0,0,0,0,1,2,0,2,2,2,2,2,2,1,2,2,2,2,2,2,0,0,0,1,2,0,0,0,0,0},
      {0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,1,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
      {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
     };


//   static private Cursor createCustomCursor(Image im,Point p,String s) {
//      try {
//         Toolkit tk = Toolkit.getDefaultToolkit();
//         Class t = tk.getClass();
//         Method m = t.getDeclaredMethod("createCustomCursor",new Class[]{ Image.class, Point.class, String.class });
//         Object c = m.invoke(tk,new Object[]{im,p,s});
//         return (Cursor)c;
//
//      } catch( Exception e) { e.printStackTrace(); return Cursor.getDefaultCursor(); }
//   }

//   static private Dimension getBestCursorSize(int w,int h) {
//      try {
//         Toolkit tk = Toolkit.getDefaultToolkit();
//         Class t = tk.getClass();
//         Method m = t.getDeclaredMethod("getBestCursorSize",new Class[]{ Integer.TYPE, Integer.TYPE });
//         Object res = m.invoke(tk,new Object[]{new Integer(w),new Integer(h)});
//         return (Dimension)res;
//
//      } catch( Exception e) { e.printStackTrace(); return new Dimension(32,32); }
//  }

   /** G?n?ration d'un curseur pour la rotation des Apertures */
   static private Cursor getTurnCursor() {
      if( turnCursor==null ) turnCursor=createCursor(TURNCURSORDEF);
      return turnCursor;
   }

   /** G?n?ration d'un curseur pour le d?placement des plans */
   static private Cursor getPlanCursor() {
      if( planCursor==null ) planCursor=createCursor(PLANCURSORDEF);
      return planCursor;
   }

   /** G?n?ration d'un curseur pour le connection de points de controle */
   static private Cursor getJoindreCursor() {
      if( joindreCursor==null ) joindreCursor=createCursor(JOINDRECURSORDEF);
      return joindreCursor;
   }

   /** G?n?ration d'un curseur pour le connection de points de controle */
   static private Cursor getTagCursor() {
      if( tagCursor==null ) tagCursor=createCursor(TAGCURSORDEF);
      return tagCursor;
   }

   /** Construction d'un curseur sur mesure avec sym?trie verticale */
   static private Cursor createCursor(int cursor[][]) {
      Cursor myCursor;
      try {


//         int h = (int)Math.sqrt(cursor.length*2);
         int h = cursor.length;
         int w = cursor[0].length;
         boolean fold = w<30;
         int width = fold ? w*2 : w;
         Dimension d= Toolkit.getDefaultToolkit().getBestCursorSize(width,h);
//N'ETAIT PAS UTILISABLE POUR COMPATIBILITE JVM 1.1.4 Windows
//        Dimension d= getBestCursorSize(h,h);

         if( d.width<width  ) d.width=width;
         if( d.height<h ) d.height=h;
         int tc [] = new int[d.width*d.height];

         // On recopie, ?ventuellement par sym?trie verticale uniquement dans le coin en haut ? gauche
         // du curseur (d?pendant de la meilleure taille de curseur retourn?e par le syst?me)
         for( int i=0; i<h; i++ ) {
            for( int j=0; j<w; j++ ) {
               int c = cursor[i][j];
               if( c==1 ) c =0xFF000000;
               else if( c==2 ) c=0xFFFFFFFF;
               tc[i*d.width + j] = c;
               if( fold ) tc[i*d.width + w*2-j-1] = c;
            }
         }
         // On cr?e le curseur
         Image im = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(d.width,d.height,tc,0,d.width));
         myCursor = Toolkit.getDefaultToolkit().createCustomCursor(im,new Point(width/2-1,h/2-1),"Turn");
// N'ETAIT PAS UTILISABLE POUR COMPATIBILITE JVM 1.1.4 Windows
//         turnCursor = createCustomCursor(im,new Point(h/2,2),"Turn");
      } catch( Exception e ) {
         myCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
      } catch( Error e ) {
         myCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
      }
      return myCursor;
   }

   static protected boolean lockCursor=false;
   synchronized static protected void lockCursor(boolean flag) { lockCursor=flag; }

  /** Positionnement du curseur en fonction du type de machine
   *  java afin d'eviter une erreur de verif de la securite */
   protected static boolean makeCursor(Component c,int type) {

      if( lockCursor ) return false;
      if( Aladin.aladin.inHelp ) type=HANDCURSOR;
      Cursor cursor = type==PLANCURSOR ? getPlanCursor() :
         type==TURNCURSOR ? getTurnCursor() :
            type==JOINDRECURSOR ? getJoindreCursor() :
               type==TAGCURSOR? getTagCursor():
                  Cursor.getPredefinedCursor(
                        type==WAITCURSOR?Cursor.WAIT_CURSOR:
                           type==HANDCURSOR?Cursor.HAND_CURSOR:
                              type==CROSSHAIRCURSOR?Cursor.CROSSHAIR_CURSOR:
                                 type==MOVECURSOR?Cursor.MOVE_CURSOR:
                                    type==RESIZECURSOR?Cursor.N_RESIZE_CURSOR:
                                       type==STRECHCURSOR?Cursor.E_RESIZE_CURSOR:
                                          type==TEXTCURSOR?Cursor.TEXT_CURSOR:
                                             Cursor.DEFAULT_CURSOR
                  );
      if( c.getCursor()!=cursor ) c.setCursor(cursor);
      return true;
   }

  /** Ajout dans un Layout en fonction du type de machine Java */
   protected static void makeAdd(Container ct,Component c,String s) {
      if( ct instanceof JFrame ) ct = ((JFrame)ct).getContentPane();
      else if( ct instanceof JApplet ) ct = ((JApplet)ct).getContentPane();
      try { ct.add(c,s); } catch( Error e ) { ct.add(s,c); }
   }


  /** Activation/Desactivation des boutons du menu principal */
   protected void setButtonMode() {
      try {
         Plan pc = calque.getFirstSelectedPlan();
//         PlanImage pimg = calque.getFirstSelectedSimpleImage();
         PlanImage pimg = calque.getFirstSelectedImage();
         Plan base = calque.getPlanBase();
         boolean hasImage = base!=null;
         int nbPlanCat = calque.getNbPlanCat();
         int nbPlanObj = calque.getNbPlanTool();
         int nbPlanImg = calque.getNbPlanImg();
         int nbPlanMoc = calque.getNbPlanMoc();
         int nbPlanHealpix = calque.getNbPlanByClass(PlanHealpix.class);
         int nbPlanTranspImg = calque.getNbPlanTranspImg();
         int nbPlanImgWithoutBG = calque.getNbPlanImg(false);
         boolean hasSelectedObj = view.hasSelectedObj();
         boolean hasSelectedSrc = view.hasSelectedSource();
         boolean hasTagSrc = calque.hasTaggedSrc();
         boolean hasSelectedPlane = pc!=null;
         int m = view.getModeView();
         boolean hasSelectedCat = (pc!=null && pc.isCatalog());
         ViewSimple v = view.getCurrentView();
//         boolean isBG = v!=null && v.pref!=null && v.pref instanceof PlanBG;
         boolean isBG = pimg!=null && pimg instanceof PlanBG;
         boolean isCube = hasImage && (base.type==Plan.IMAGECUBE || base.type==Plan.IMAGECUBERGB);
//         boolean hasPixels = v!=null && v.pref!=null && v.pref.hasAvailablePixels() && v.pref.type!=Plan.IMAGEHUGE
//         && !isBG;
         boolean hasPixels = pimg!=null && pimg.hasAvailablePixels() && pimg.type!=Plan.IMAGEHUGE && !isBG;
         boolean hasProj = pimg!=null && Projection.isOk(pimg.projd);
         boolean isFree = calque.isFree();
         int nbPlans = calque.getNbPlans(true);
         boolean mode = nbPlans>0;
         boolean mode1 = nbPlans>1 || nbPlans==1 && !isBG;

//         if( console!=null ) console.clone.setEnabled(hasSelectedSrc);
         if( miROI!=null ) miROI.setEnabled( hasImage && (nbPlanCat>0 || nbPlanObj>0) );
         if( miCalImg!=null ) miCalImg.setEnabled( hasImage && !isBG );
         if( miCalCat!=null ) miCalCat.setEnabled( hasSelectedCat );
         if( miAddCol!=null ) miAddCol.setEnabled( hasSelectedCat );
         if( miPref!=null ) miPref.setEnabled( STANDALONE );
         if( miOpen!=null ) miOpen.setEnabled( STANDALONE );
         if( miPlugs!=null ) miPlugs.setEnabled( !isApplet() || !ISJNLP );
         if( miXmatch!=null ) miXmatch.setEnabled( nbPlanCat>0 );
         if( miDel!=null ) miDel.setEnabled(!isFree);
         if( miDelAll!=null ) miDelAll.setEnabled(!isFree);
         if( miPixel!=null ) miPixel.setEnabled(pimg!=null && (!isBG || isBG && !((PlanBG)pimg).isColored() ));
         if( miContour!=null ) miContour.setEnabled( hasImage );
         if( miVOtool!=null ) miVOtool.setEnabled(hasNoResctriction());
         if( miSave!=null ) miSave.setEnabled(mode && hasNoResctriction());
         if( miSaveG!=null ) miSaveG.setEnabled(mode && hasNoResctriction());
         if( miExport!=null ) miExport.setEnabled(mode1 && hasNoResctriction());
         if( miExportEPS!=null ) miExportEPS.setEnabled(mode1 && hasNoResctriction());
         if( miBackup!=null ) miBackup.setEnabled(mode1 && hasNoResctriction());
         if( miPrint!=null ) miPrint.setEnabled(!isFree && hasNoResctriction());
         if( miProp!=null ) miProp.setEnabled(!isFree);
         if( miPan!=null ) miPan.setEnabled(hasImage);
         if( miGlass!=null ) miGlass.setEnabled(hasImage && !isBG);
         if( miGlassTable!=null ) miGlassTable.setEnabled(hasImage && !isBG);
         if( miZoom!=null ) miZoom.setEnabled(!isFree);
         if( miPaste!=null ) miPaste.setEnabled(hasClipBoard());
         if( miCopy1!=null ) miCopy1.setEnabled(!isFree);
         if( miRsamp!=null ) miRsamp.setEnabled(nbPlanImgWithoutBG>1);
         if( miRGB!=null ) miRGB.setEnabled(nbPlanImgWithoutBG>1);
         if( miMosaic!=null ) miMosaic.setEnabled(nbPlanImgWithoutBG>1);
         if( miBlink!=null ) miBlink.setEnabled(nbPlanImgWithoutBG>1);
         if( miGrid!=null ) miGrid.setSelected( calque.hasGrid() );
         if( miHpxGrid!=null ) miHpxGrid.setSelected(calque.hasHpxGrid() );
         if( miOverlay!=null ) miOverlay.setSelected(calque.flagOverlay);
         if( miRainbow!=null ) {
            miRainbow.setEnabled( view.rainbowAvailable());
            miRainbow.setSelected(view.hasRainbow());
         }
//         if( miTip!=null ) miTip.setSelected(calque.flagTip);
         if( miMore!=null ) miMore.setEnabled(!view.allImageWithView());
         if( miOne!=null ) miOne.setEnabled(view.isMultiView() || view.getNbUsedView()>1 );
         if( miNext!=null ) miNext.setEnabled(nbPlanImg>1);
         if( miLock!=null ) miLock.setEnabled(v!=null && !v.isProjSync());
         if( miLock!=null ) miLock.setSelected(v!=null && v.locked);
         if( miNorthUp!=null ) miNorthUp.setEnabled(v!=null && v.canBeNorthUp() );
         if( miNorthUp!=null ) miNorthUp.setSelected(v!=null && v.northUp);
         if( miDelLock!=null ) miDelLock.setEnabled( view.hasLock() );
         if( miStick!=null ) {
            miStick.setSelected(v!=null && v.sticked);
            miStick.setEnabled(m>1);
         }
         if( miScreen!=null ) {
            miScreen.setEnabled( !isApplet() || flagDetach );
            miScreen.setSelected( isFullScreen() ) ;
         }
         if( miPScreen!=null ) {
            miPScreen.setEnabled( !isApplet() || flagDetach );
         }
//         if( miPScreen!=null )  miPScreen.setEnabled( !isApplet() );
         if( miReticle!=null ) {
            if( !calque.hasReticle() ) miNoReticle.setSelected(true);
            else if( calque.reticleMode==1 ) miReticle.setSelected(true);
            else if( calque.reticleMode==2 ) miReticleL.setSelected(true);
         }
         if( miTarget!=null ) miTarget.setSelected(calque.hasTarget());
         if( miSimbad!=null ) miSimbad.setSelected(calque.flagSimbad);
         if( miAutoDist!=null ) miAutoDist.setSelected(calque.flagAutoDist);
         if( miVizierSED!=null ) miVizierSED.setSelected(calque.flagVizierSED);
         if( miZoomPt!=null ) miZoomPt.setSelected(toolBox.tool[ToolBox.ZOOM].mode==Tool.DOWN);
//         if( miPrevPos!=null ) miPrevPos.setEnabled(view.canActivePrevUndo());
//         if( miNextPos!=null ) miNextPos.setEnabled(view.canActiveNextUndo());
         if( miZoomPt!=null ) miZoomPt.setSelected(toolBox.tool[ToolBox.ZOOM].mode==Tool.DOWN);
         if( miPan!=null ) miPan.setSelected(toolBox.tool[ToolBox.PAN].mode==Tool.DOWN);
         if( miGlass!=null ) miGlass.setSelected(toolBox.tool[ToolBox.WEN].mode==Tool.DOWN);
         if( miGlassTable!=null ) miGlassTable.setSelected(toolBox.tool[ToolBox.WEN].mode==Tool.DOWN && calque.zoom.zoomView.isPixelTable() );
         if( miPanel1!=null ) {
            if( m==ViewControl.MVIEW1 ) miPanel1.setSelected(true);
            else if( m==ViewControl.MVIEW2L ) miPanel2c.setSelected(true);
            else if( m==ViewControl.MVIEW2C ) miPanel2l.setSelected(true);
            else if( m==ViewControl.MVIEW4 ) miPanel4.setSelected(true);
            else if( m==ViewControl.MVIEW9 ) miPanel9.setSelected(true);
            else if( m==ViewControl.MVIEW16 )miPanel16.setSelected(true);
         }
         if( miGrey!=null ) miGrey.setEnabled(v!=null && v.pref!=null && v.pref.type==Plan.IMAGERGB);
         if( search!=null ) search.setEnabled(nbPlanCat>0);
         if( mesure!=null ) mesure.search.setEnabled(nbPlanCat>0);
//         if( pixel!=null ) pixel.setEnabled(nbPlanImg>0);
         if( miFilter!=null ) miFilter.setEnabled(nbPlanCat>0);
         if( miFilterB!=null ) miFilterB.setEnabled(nbPlanCat>0);
         if( miSearch!=null ) miSearch.setEnabled(nbPlanCat>0);
         if( miSelect!=null ) miSelect.setEnabled(nbPlanCat>0 || nbPlanObj>0);
         if( miSelectAll!=null )  miSelectAll.setEnabled(nbPlanCat>0 || nbPlanObj>0);
         if( miSelectTag!=null )  miSelectTag.setEnabled(hasTagSrc);
         if( miDetag!=null ) miDetag.setEnabled(hasTagSrc);
         if( miUnSelect!=null ) miUnSelect.setEnabled(hasSelectedObj);
         if( miCut!=null ) miCut.setEnabled(nbPlanImgWithoutBG>0);
         PlanImage pi = calque.getFirstSelectedPlanImage();
         if( miStatSurf!=null ) miStatSurf.setEnabled(hasPixels && (!isBG || pi instanceof PlanHealpix));
         if( miTransp!=null ) miTransp.setEnabled(pi!=null && calque.canBeTransparent(pi));
         if( miTranspon!=null ) miTranspon.setEnabled(nbPlanTranspImg>0);
         if( miDist!=null ) miDist.setEnabled(nbPlanImg>0);
         if( miDraw!=null ) miDraw.setEnabled(nbPlanImg>0);
         if( miTag!=null ) miTag.setEnabled(nbPlanImg>0);
         if( miTexte!=null ) miTexte.setEnabled(nbPlanImg>0);
         if( miInFold!=null ) miInFold.setEnabled(hasSelectedPlane);
         if( miClone!=null )  miClone.setEnabled(hasSelectedSrc);
         if( miTableInfo!=null )  miTableInfo.setEnabled(nbPlanCat>0);
         if( miPlotcat!=null )  miPlotcat.setEnabled(hasSelectedCat);
         if( miConcat!=null )  miConcat.setEnabled(nbPlanCat>1);
         if( miTagSelect!=null ) miTagSelect.setEnabled(hasSelectedSrc);
//         if( miHistory!=null ) miHistory.setEnabled(treeView!=null);        // IL FAUDRAIT UN TEST isFree()
         if( miArithm!=null ) miArithm.setEnabled(nbPlanImg>0 && !isBG && !isCube);
         if( miMocHips!=null ) miMocHips.setEnabled( pi instanceof PlanBG && ((PlanBG)pi).hasMoc() 
               || base instanceof PlanBG && ((PlanBG)base).hasMoc() );
         if( miMocGenImg!=null ) miMocGenImg.setEnabled( nbPlanImg>0 );
         if( miMocGenCat!=null ) miMocGenCat.setEnabled( nbPlanCat>0 );
         if( miMocOp!=null ) miMocOp.setEnabled(nbPlanMoc>0);
         if( miMocToOrder!=null ) miMocToOrder.setEnabled(nbPlanMoc>0);
         if( miMocFiltering!=null ) miMocFiltering.setEnabled(nbPlanMoc>0 && nbPlanCat>0 );
         if( miMocCrop!=null ) miMocCrop.setEnabled( pc instanceof PlanMoc );
         if( miHealpixArithm!=null ) miHealpixArithm.setEnabled(nbPlanHealpix>0);
         if( miConv!=null ) miConv.setEnabled(hasPixels && !isCube);
         if( miNorm!=null ) miNorm.setEnabled(hasPixels && !isCube);
         if( miBitpix!=null ) miBitpix.setEnabled(hasPixels && !isCube);
         if( miPixExtr!=null ) miPixExtr.setEnabled(hasPixels && !isCube);
         if( miCopy!=null ) miCopy.setEnabled(hasPixels /* && !isCube */);
         if( miCreateHpx!=null ) miCreateHpx.setEnabled( hasProj );
         if( miHpxDump!=null ) miHpxDump.setEnabled(v!=null && v.pref!=null && isBG );
         if( miFlip!=null ) miFlip.setEnabled(hasImage && !isCube && !isBG);
         int syncMode=sync.getMode();
         if( miSync!=null ) {
            miSync.setEnabled(syncMode!=0);
            miSync.setSelected(syncMode==2);
         }
         if( miSyncProj!=null ) {
            miSyncProj.setEnabled(syncMode!=0);
            miSyncProj.setSelected(syncMode==3);
         }

         if( miCrop!=null ) {
            miCrop.setEnabled(v!=null && v.pref!=null && (v.pref.isPixel() || isBG )
               && (v.pref.type!=Plan.IMAGEHUGE || ((PlanImageHuge)v.pref).fromSubImage(v.zoom, getWidth(), getHeight())));
         }

         Plan p = calque.getFirstSelectedPlan();
         if( miHead!=null ) miHead.setEnabled(p!=null && p.hasFitsHeader());

         if( ExportYourWork!=null ) ExportYourWork.setEnabled(mode && hasNoResctriction());
//         if( avant!=null ) avant.setEnabled(view.canActivePrevUndo());
//         if( apres!=null ) apres.setEnabled(view.canActiveNextUndo());

      } catch( Exception e ) { e.printStackTrace(); }

      // Test si le stack a ?volu?, et l'indique aux VO Observers correspondants
      if( VOObsEvent!=null ) {
         String status = command.getStatus("stack");
         if( !status.equals(ostatus) ) {
            ostatus=status;
            sendEventObserver();
         }
      }
  }

   String ostatus=null;

   static public void setIcon(Frame f) {
      if( Aladin.aladin!=null ) f.setIconImage(aladin.getImagette("AladinIconSS.gif"));
   }


 /** Determination du repertoire d'installation d'Aladin.
   * La methode consiste a balayer les valeurs de la variable
   * java.class.path en recherchant dans chacun de ces repertoires
   * la presence du fichier ALAGLU
   *
   * Met a jour la variable static HOME
   */
   protected static void setAladinHome() {
      String PS = System.getProperty("path.separator");

      HOME="."+Util.FS;		// Par defaut, le repertoire courant
      String path = System.getProperty("java.class.path");
      if( path==null ) return;
      StringTokenizer st = new StringTokenizer(path,PS);

      // Parcours de la liste des elements de classpath
      while( st.hasMoreTokens() ) {
         String s = st.nextToken();

         // Cas particulier du .jar
         if( s.endsWith(Util.FS+"Aladin.jar") ) s=s.substring(0,s.lastIndexOf(Util.FS));
         else if( s.endsWith("Aladin.jar") ) s=".";
         String sep = s.endsWith(Util.FS)?"":Util.FS;
         File f = new File(s+sep+ALAGLU);
         if( !f.canRead() ) continue;

         // C'est bon on a trouve
         HOME=s+sep;
         return;
      }
   }

  /** Lancement d'Aladin par une autre application java.
   * Il s'agit d'appeler la methode main() et de retourner
   * l'instance de l'objet Aladin
   */
   public static Aladin launch() { return launch(null,null); }
   public static Aladin launch(String s) { return launch(s,null); }
   public static Aladin launch(Applet applet) { return launch(null,applet); }
   public static Aladin launch(String s,Applet applet) {
      String args[];
      extApplet = applet;

      if( extApplet!=null ) {
         try {
            System.getProperty("java.home");
            STANDALONE=SIGNEDAPPLET=true;
         } catch( Exception e )  { }
      }

      if( s!=null ) {
         StringTokenizer st = new StringTokenizer(s);
         int n;
         args = new String[n=st.countTokens()];
         for( int i=0; i<n; i++ ) args[i] = new String(st.nextToken());
      } else args = new String[0];

      flagLaunch=true;
      main(args);
      return aladin;
   }



   // hide Aladin window - pour controle de Aladin par une autre app java
   @Override
public void hide() {
     if( flagLaunch && extApplet==null ) f.dispose();
     else super.hide();
   }

   // show Aladin window - pour controle de Aladin par une autre app java
   @Override
public void show() {
      if( flagLaunch && extApplet==null ) { f.pack(); f.show(); }
     else super.show();
   }


   static private final String USAGE =
      "Usage: Aladin [options...] [filenames...]\n"+
      "       Aladin -chart=\"[server[,server...]\" object\n"+
      "       Aladin -hipsgen ...\n"+
      "       Aladin -mocgen ...\n"+
      "       Aladin -help\n"+
      "       Aladin -version\n"+
      "\n"+
      "   Options:\n"+
      "       -local: without Internet test access\n"+
      "       -screen=\"full|preview\": starts Aladin in full screen\n" +
      "               or in a simple preview window\n"+
      "       -glufile=\"pathname|url[;...]\": local/remote GLU dictionaries describing\n"+
      "               additionnal data servers compatible with Aladin \n"+
      "       -stringfile=\"pathname[;...]\": string files for additionnal\n" +
      "               supported languages\n"+
      "       -scriptfile=\"pathname|url[;...]\": script by local files or url \n"+
      "       -script=\"cmd1;cmd2...\": script commands passed by parameter\n"+
      "       -nogui: no graphical interface (for script mode only) " +
      "               => noplugin, nobanner, nobookmarks, nohub\n"+
      "       -nobanner: no Aladin banner\n"+
      "       -noreleasetest: no Aladin new release test\n"+
      "       -[no]hub: no usage of the internal PLASTIC hub\n"+
      "       -[no]plugin: with/without plugin support\n"+
      "       -[no]bookmarks: with/without bookmarks support\n"+
      "       -[no]outreach: with/without outreach mode\n"+
      "       -[no]beta: with/without new features in beta test\n"+
      "       -[no]proto: with/without prototype features for demonstrations and tests\n"+
      "       -trace: trace mode for debugging purpose\n"+
      "       -debug: debug mode (very verbose)\n"+
      "\n"+
      "       -chart=: build a png field chart directly on stdout\n"+
      "       -hipsgen: build HEALPix progressive sky by script (see -hipsgen -h for help)\n"+
      "       -mocgen: build MOC by script (see -mocgen -h for help)\n"+
      "       -help: display this help\n"+
      "       -version: display the Aladin release number\n"+
      "\n"+
      "   The files specified in the command line can be :\n"+
      "       - images: FITS (gzipped,RICE,MEF,...), HEALPix maps, JPEG,GIF,PNG\n"+
      "       - tables: FITS, XML/VOTable, CSV, TSV, S-extractor, IPAC-TBL, Skycat or ASCII tables\n"+
      "       - graphics: Aladin or IDL or DS9 regions, MOCs\n"+
      "       - directories: all-sky HEALPix folders\n"+
      "       - Aladin backup : \".aj\" extension\n"+
      "       - Aladin scripts : \".ajs\" extension\n"+
      "";

   static private void usage() {
      System.out.println(USAGE);
   }

   static private void version() {
      System.out.println("Aladin version "+VERSION);
   }

   /** Affichage d'Aladin dans sa propre Frame */
   static protected void startInFrame(Aladin a) {
      a.f = new MyFrame(a,"");
      a.f.setIconImage(a.getImagette("AladinIconSS.gif"));
      JPanel p = (JPanel)a.f.getContentPane();
      p.setLayout( new BorderLayout(0,0) );
      makeAdd(p,a,"Center");
      a.myInit();
      int id = a.getInstanceId();
      a.f.setTitle(TITRE+" "+getReleaseNumber()
            +(OUTREACH?OUTREACH_VERSION : PROTO?PROTO_VERSION : BETA?BETA_VERSION:"")
            +(id>0?" ("+(id)+")":""));
      a.f.pack(); // M?me en mode script, le pack est indipensable pour cr?er les peer classes
      if( NOGUI ) return;
      Rectangle r = a.configuration.getWinLocation();
      if( r==null || r.x>SCREENSIZE.width || r.y>SCREENSIZE.height ) {
         a.f.setLocation(computeLocation(a.f));
         a.f.setSize(732,679);
         a.configuration.setInitWinLoc(a.f.getLocation().x,a.f.getLocation().y,
               a.f.getSize().width,a.f.getSize().height);
      } else {
         a.f.setLocation(new Point(r.x,r.y));
         if( r.width<0 ) { r.width = Math.abs(r.width); r.height=Math.abs(r.height); }
         a.f.setSize(r.width,r.height);
      }
      a.splitH.setMesureHeight( a.configuration.getWinDivider() );
      a.offsetLocation();
      a.f.setVisible(true);
      a.mesure.setReduced(true);
//      trace(2,"Aladin window size: "+a.getWidth()+"x"+a.getHeight());
   }

   /**
    * Positionne des flags et des propri?t?s sp?cifiques au Mac
    */
   static private void setMacProperties() {
		// propri?t? sp?cifique ? Mac OS permettant de faire apparaitre les ?l?ments de menu tout en haut (selon le L'n'F Mac)
		// (cf.	http://devworld.apple.com/documentation/Java/Conceptual/Java14Development/04-JavaUIToolkits/JavaUIToolkits.html#//apple_ref/doc/uid/TP40001901-209837)
		macPlateform = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
		// we set the property only if it has not been set yet (by -Dprop=value at startup for instance)
		// for an applet, we keep the menu the standard way
		if( macPlateform && System.getProperty("apple.laf.useScreenMenuBar")==null && !isApplet() ) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

   // Les commandes a ex?cuter apr?s la cr?ation d'Aladin (voir creanObj.run())
   private StringBuffer launchScript=null;
   private boolean quitAfterLaunchScript=false;

   /** Retourne le script de d?marrage, ou "" si rien ? faire */
   protected String getLaunchScript() {
      if( launchScript==null ) return "";
      return launchScript+";";
   }

   /** Ajoute une commande au script de d?marrage */
   protected void addLaunchScript(String s) {
      if( launchScript==null ) launchScript = new StringBuffer(s);
      else launchScript.append(";"+s);
   }

   /** Retourne true s'il faut quitter Aladin apr?s le script de d?marrage */
   protected boolean quitAfterLaunchScript() { return quitAfterLaunchScript; }

 /**
	 * Demarrage en mode standalone. Se contente de construire une Frame et d'y
	 * mettre l'objet Aladin dedans, puis de lancer aladin.init()
	 *
	 * See Aladin
	 */
   public static void main(String[] args) {
      String chart=null;		// en cas de demande de carte de champ
      int lastArg;		// Prochain indice des arguments a loader
      String SCREEN=null;
      if( extApplet==null ) STANDALONE = true;
      String scriptParam=null;

      // Pour l'affichage du bouton de debug
      lastArg=0;
      for( int i=0; i<args.length; i++ ) {
         if( args[i].equals("-h") || args[i].equals("-help") ) { usage(); System.exit(0); }
         
         else if( args[i].equalsIgnoreCase("-pixfoot") || args[i].equalsIgnoreCase("-mocgen"))      { 
            System.arraycopy(args, i+1, args, 0, args.length-i-1);
            MocGen.main(args);
            System.exit(0); 
         }
         else if( args[i].equalsIgnoreCase("-hipsgen") || args[i].equalsIgnoreCase("-skygen"))      { 
            String [] args1 = new String[args.length-i-1];
            System.arraycopy(args, i+1, args1, 0, args.length-i-1);
            HipsGen generator = new HipsGen();
            generator.execute(args);
            System.exit(0); 
         }
         
         else if( args[i].equals("-version") )     { version(); System.exit(0); }
         else if( args[i].equals("-test") )        { boolean rep=test(); System.exit(rep ? 0 : 1); }
         else if( args[i].equals("-trace") )       { levelTrace=3; lastArg=i+1; }
         else if( args[i].equals("-debug") )       { levelTrace=4; lastArg=i+1; }
         else if( args[i].equals("-beta") )        { BETA=true; lastArg=i+1; }
         else if( args[i].equals("-nolog") )       { LOG=false; lastArg=i+1; }
         else if( args[i].equals("-outreach") )    { OUTREACH=true; setOUTREACH=true; lastArg=i+1; }
         else if( args[i].equals("-proto") )       { PROTO=BETA=true; lastArg=i+1; }
         else if( args[i].equals("-nobeta") )      { BETA=false; lastArg=i+1; }
         else if( args[i].equals("-noproto") )     { PROTO=BETA=false; lastArg=i+1; }
         else if( args[i].equals("-nooutreach") )  { OUTREACH=false; setOUTREACH=true; lastArg=i+1; }
         else if( args[i].equals("-nogui") || args[i].equals("-script")) { NOGUI=true; BOOKMARKS=false; NOHUB=true; NOPLUGIN=true; lastArg=i+1; }
         else if( args[i].equals("-local") )       { NETWORK=false; lastArg=i+1; }
         else if( args[i].equals("-cds") )         { CDS=true; lastArg=i+1; }
         else if( args[i].equals("-nobanner") )    { BANNER=false; lastArg=i+1; }
         else if( args[i].equals("-nocredit") )    { CREDIT=false; lastArg=i+1; }
         else if( args[i].equals("-nobookmarks") ) { BOOKMARKS=false; lastArg=i+1; }
         else if( args[i].equals("-bookmarks") )   { BOOKMARKS=true; lastArg=i+1; }
         else if( args[i].equals("-samp") )        { USE_SAMP_REQUESTED=true; lastArg=i+1; }
         else if( args[i].equals("-antialiasing") )    { ALIASING=1; lastArg=i+1; }
         else if( args[i].equals("-noantialiasing") )  { ALIASING=-1; lastArg=i+1; }
         else if( args[i].equals("-plastic") )     { USE_PLASTIC_REQUESTED=true; lastArg=i+1; }
         else if( args[i].equals("-noplastic")
                  || args[i].equals("-nosamp") )   { PLASTIC_SUPPORT=false; lastArg=i+1; }
         else if( args[i].equals("-noconsole") )   { CONSOLE=false; lastArg=i+1; }
         else if( args[i].equals("-noreleasetest") )   { TESTRELEASE=false; lastArg=i+1; }
         else if( args[i].equals("-nohub") )       { NOHUB=true; lastArg=i+1; }
         else if( args[i].equals("-hub") )         { NOHUB=false; lastArg=i+1; }
         else if( args[i].equals("-noplugin") )    { NOPLUGIN=true; lastArg=i+1; }
         else if( args[i].equals("-plugin") )      { NOPLUGIN=false; lastArg=i+1; }
         else if( args[i].equals("-open") ) lastArg=i+1;	//Simplement ignor? pour supporter protocol Windows
         else if( args[i].startsWith("-screen=") ) { SCREEN=args[i].substring(8); lastArg=i+1; }
         else if( args[i].startsWith("-preview") ) { SCREEN="preview"; lastArg=i+1; }
         else if( args[i].startsWith("-script=") ) { scriptParam=args[i].substring(8); lastArg=i+1; }
         else if( args[i].startsWith("script=") )  { scriptParam=args[i].substring(7); lastArg=i+1; }
         else if( args[i].startsWith("-chart=") )  { chart=args[i].substring(7); lastArg=i+1; }
         else if( args[i].startsWith("-chart") )   { chart=args[i+1]; lastArg=i+2; }
         else if( args[i].startsWith("-rHost=") )  { RHOST=args[i].substring(7); lastArg=i+1; }
         else if( args[i].startsWith("-from=") )   { FROMDB=args[i].substring(6); lastArg=i+1; }
         else if( args[i].startsWith("-glufile=") ) { GLUFILE=args[i].substring(9); lastArg=i+1; }
         else if( args[i].startsWith("-registry=") ) { FrameServer.REGISTRY_BASE_URL=args[i].substring(10); lastArg=i+1; }
         else if( args[i].startsWith("-stringfile=") ) { STRINGFILE=args[i].substring(12); lastArg=i+1; }
         else if( args[i].startsWith("-scriptfile=") ) { SCRIPTFILE=args[i].substring(12); lastArg=i+1; }
         else if( args[i].startsWith("-font=") )   {
            try { SIZE= Integer.parseInt(args[i].substring(6)); } catch( Exception e ) { e.printStackTrace(); }
            trace(2,"default font size = "+SIZE);
            lastArg=i+1;
         }
         else if( args[i].charAt(0)=='-' ) { System.err.println("Aladin option unknown ["+args[i]+"]"); lastArg=i+1; }
      }

//      if( chart!=null ) NOGUI=true;

      // TODO : ? supprimer
      USE_ACR = PROTO;

      // Cr?ation d'Aladin
      setMacProperties(); // indispensable d'appeler cette m?thode avant la cr?ation de l'objet Aladin !
      aladin = new Aladin();
      aladin.SCREEN = SCREEN;
      aladin.flagScreen = SCREEN!=null;
      if( scriptParam!=null ) aladin.addLaunchScript(scriptParam);
      startInFrame(aladin);

      // Simplification d'?criture pour une simple carte de champ
      if( chart!=null ) {
         if( !chart.startsWith("get") ) chart="get "+chart;
         aladin.addLaunchScript(chart+";grid on;save -png");
      }

      // Chargement d'un fichier XML, FITS ou AJ
      while( lastArg<args.length ) {
        String s=args[lastArg++];
        aladin.addLaunchScript("load "+s);
      }

      // Y a-t-il une url qui pointe vers un script ? voire plusieurs ?
      // si oui on les traite via une commande "get File(url)"
      if( SCRIPTFILE!=null ) {
         StringTokenizer st = new StringTokenizer(SCRIPTFILE,";");
         while( st.hasMoreTokens() ) {
            aladin.addLaunchScript("get File("+st.nextToken()+")");
         }
      }

      // Passage d'un script par param?tre du main
      if( aladin.getLaunchScript().length()>0 && NOGUI ) aladin.quitAfterLaunchScript=true;

   }

   /** Attente d'une image */
   protected void waitImage(Image img) {
      // Methode standard (lente)
      MediaTracker mt = new MediaTracker(this);
      mt.addImage(img,0);
      try{ mt.waitForID(0); } catch( Exception e){ if( levelTrace>=3 ) e.printStackTrace(); }

      // Methode rapide, mais susceptible de ne pas marcher dans
      // des versions post?rieures de JVM
//      while( img.getWidth(this)<0 ) Util.pause(10);
   }

  /** Verifie que l'objet dialog a bien ete cree, sinon se met en attente */
   protected void waitDialog() {
      if( dialogOk() ) return;

//      long tps = System.currentTimeMillis() - startTime;
//      aladin.trace(3, "Aladin is ready (in "+(tps/1000.)+"s)");
      while( !dialogOk() ) {
//         trace(3,"Waiting dialog...");
         Util.pause(100);
      }
      long tps = System.currentTimeMillis() - startTime;
      aladin.trace(3, "Aladin is fully ready (in "+(tps/1000.)+"s)");
   }

   /** Retourne true si le dialog est pr?t */
   protected boolean dialogOk() {
      return dialog!=null && calque!=null /* && calque.getPlans()!=null*/ ;
   }

  /** Chargement d'un fichier pass? en param?tre */
   protected void load(String f,String label) {
      waitDialog();
      f=getFullFileName(f);
      calque.newPlan(f, label, null);
//      dialog.server[ServerDialog.LOCAL].creatPlane(null,null,"\""+f+"\"",label,null);
   }

  /** Retourne le target courant pour les FoV
   * @param planeLabel le nom du plan
   * @return Les coordonnees J2000 sexa du target
   */
   public String getTarget(String planeLabel) {
      int i;
      for( i=0; i<calque.plan.length; i++ ) {
         if( calque.plan[i].flagOk && calque.plan[i].label.equals(planeLabel) ) {
            if( calque.plan[i] instanceof PlanField )
               return ((PlanField)calque.plan[i]).getProjCenter();
            break;
         }
      }
      return null;
      
//      int i;
//      for( i=0; i<calque.plan.length; i++ ) {
//         if( calque.plan[i].flagOk && calque.plan[i].label.equals(planeLabel) ) {
//            if( calque.plan[i] instanceof PlanField )
//               return ((PlanField)calque.plan[i]).getProjCenter();
//            break;
//         }
//      }
//      return null;
   }

   protected Vector VOObsPos = null;	// Liste des VOObserver de la position courante
   protected Vector VOObsPix = null;  // Liste des VOObserver de la valeur courante du pixel
   protected Vector VOObsMes = null;  // Liste des VOObserver sur les mesures
   protected Vector VOObsEvent= null;  // Liste des VOObserver sur les ?v?nements de la pile

   /** Pour interface VOObserver */
   protected void sendObserver() {
       // 1. Transmission de la position courante
       // transmission via SAMP
       boolean flagPlastic = Aladin.PLASTIC_SUPPORT;
       if (Aladin.PLASTIC_SUPPORT && view.repere!=null) {
           this.getMessagingMgr().pointAtCoords(view.repere.raj,view.repere.dej);
       }
       // transmission via interface VOApp
      if( VOObsPos!=null && view.repere!=null ) {
         Enumeration e = VOObsPos.elements();
         while( e.hasMoreElements() ) {
            try { ((VOObserver)e.nextElement()).position(view.repere.raj,view.repere.dej); }
            catch( Exception e1 ) { if( levelTrace>=3 ) e1.printStackTrace(); }
         }
      }

       // 2. Transmission de la valeur du pixel
      if( VOObsPix!=null && view.repere!=null ) {
         double pixelValue = view.getPixelValue();

         Enumeration e = VOObsPix.elements();
         while( e.hasMoreElements() ) {
            try { ((VOObserver)e.nextElement()).pixel(pixelValue); }
            catch( Exception e1 ) { if( levelTrace>=3 ) e1.printStackTrace(); }
         }
      }

   }

//   /** Pour interface APTObserver */
//   protected void sendSelectionObserver() {
//
//      if( VOObsMes==null ) return;
//
//      // Recherche des coordonn?es des coins du rectangle de s?lection
//      ViewSimple v = view.getCurrentView();
//      if( v==null ) return;
//      Plan plan = v.pref;
//      Projection proj = v.getProj();
//      if( plan==null || !Projection.isOk(proj) ) return;
//      Rectangle r = v.rselect;
//      Coord a1 = new Coord(), a2 = new Coord();
//
//      for( int i=0; i<2; i++ ) {
//         PointD p;
//         Coord coo;
//         if( i==0 ) { p= v.getPosition((double)r.x,(double)r.y); coo=a1; }
//         else { p= v.getPosition((double)(r.x+r.width),(double)(r.y+r.height)); coo=a2; }
//         coo.x = p.x; coo.y = p.y;
//         proj.getCoord(coo);
//         if( Double.isNaN(coo.al) ) return;
//      }
//   }

   /** To register an observer of VO events.
    * see position() and pixel() associated callback methods
    * ex: addObserver(this,VOApp.POSITION|VOApp..PIXEL|VOApp.MEASURE)
    * @param app the application to register
    * @param eventMasq a bit field (use POSITION or PIXEL or MEASURE),
    *                  (0 to remove the observer)
    */
   public void addObserver(VOObserver app,int eventMasq) {

      // Suppression ?
      if( eventMasq==0 ) {
         if( VOObsPos!=null && VOObsPos.contains(app) ) VOObsPos.removeElement(app);
         if( VOObsPix!=null && VOObsPix.contains(app) ) VOObsPix.removeElement(app);
         if( VOObsMes!=null && VOObsMes.contains(app) ) VOObsMes.removeElement(app);
         if( VOObsEvent!=null && VOObsEvent.contains(app) ) VOObsEvent.removeElement(app);
         return;
      }

      // Ajout aux observers de la position
      if( (eventMasq&VOApp.POSITION)!=0 ) {
         if( VOObsPos==null ) VOObsPos = new Vector();
         if( !VOObsPos.contains(app) ) VOObsPos.addElement(app);
      }

      // Ajout aux observers du pixel
      if( (eventMasq&VOApp.PIXEL)!=0 ) {
         if( VOObsPix==null ) VOObsPix = new Vector();
         if( !VOObsPix.contains(app) ) VOObsPix.addElement(app);
      }

      // Ajout aux observers des modifs des tools de surface
      if( (eventMasq&VOApp.MEASURE)!=0 ) {
         if( VOObsMes==null ) VOObsMes = new Vector();
         if( !VOObsMes.contains(app) ) VOObsMes.addElement(app);
      }

      // Ajout aux observers des events de la piel
      if( (eventMasq&VOApp.STACKEVENT)!=0 ) {
         if( VOObsEvent==null ) VOObsEvent = new Vector();
         if( !VOObsEvent.contains(app) ) VOObsEvent.addElement(app);
      }
   }

   /** Envoi d'une commande aux observers des ?v?nements sur la pile pour indiquer un changement  */
   protected void sendEventObserver() {
      if( aladin.VOObsEvent==null || aladin.VOObsEvent.size()==0 ) return;
      String s = "info stackEvent";
      Enumeration e = aladin.VOObsEvent.elements();
      while( e.hasMoreElements() ) {
         try { ((VOApp)e.nextElement()).execCommand(s); }
         catch( Exception e1 ) { if( aladin.levelTrace>=3 ) e1.printStackTrace(); }
      }
   }


  /** Synchrone script command execution
   * @param cmd script command
   * @return null if the command is accepted, error message otherwise
   */
   public String execCommand(String cmd) {
      waitDialog();
      try { return command.execScript(cmd); }
      catch( Exception e ) {
         aladin.warning("Error: "+e,1);
//         System.out.println("Error: "+e);
         return("Error: "+e);
      }
   }

   /** Asynchrone script command execution.
    * In case of Javascript usage, some script command can not be executed
    * in synchrone mode for java script security restriction (for instance get, load).
    * Using this asynchrone alternative bypasses the restrictions
    * @param cmd cmd script command
    */
   public void execAsyncCommand(String cmd) {
      waitDialog();
      console.pushCmd(cmd);
   }

   protected static Class DEFAULT_MESSAGING_MGR = SAMPManager.class;
   protected static boolean USE_PLASTIC_REQUESTED = false;
   protected static boolean USE_SAMP_REQUESTED = false;
   protected boolean messagingMgrCreated = false;
   /**
    * retourne l'instance du AppMessagingInterface correspondant
    * cr?e l'objet si n?cessaire
    *
    */
   protected synchronized AppMessagingInterface getMessagingMgr() {
      // lazy initialization
      if( appMessagingMgr==null ) {
          // choice at user request ?
          if( USE_SAMP_REQUESTED ) {
              appMessagingMgr = new SAMPManager(this);
          }
          else if( USE_PLASTIC_REQUESTED ) {
              appMessagingMgr = new PlasticManager(this);
          }
          // TODO : test if hub is responding !!
          // else look for an existing conf file
          else if( SAMPManager.getLockFile().exists() ) {
              appMessagingMgr = new SAMPManager(this);
          }
          else if( PlasticManager.getLockFile().exists() ) {
              appMessagingMgr = new PlasticManager(this);
          }
          // else take default
          else if( DEFAULT_MESSAGING_MGR.equals(PlasticManager.class) ) {
              appMessagingMgr = new PlasticManager(this);
          }
          else {
              appMessagingMgr = new SAMPManager(this);
          }
          messagingMgrCreated = true;

          // add shutdown hook
          // to properly unregister from SAMP/Plastic
          // and shutdown internal hub
          Runtime.getRuntime().addShutdownHook(new Thread("AladinSAMPPlasticUnregister") {
              @Override
            public void run() {
                  Aladin.trace(1, "In shutdown hook");
                  if( Aladin.PLASTIC_SUPPORT && messagingMgrCreated ) {
                      try { getMessagingMgr().unregister(true, true); } catch( Exception e ) {}
      }
              }
          });
      }

      return appMessagingMgr;
   }


   /** VOApp interface */
   public String putVOTable(VOApp voApp, InputStream in,String label) {
      return putDataset(voApp,in,label);
   }

   /** VOApp interface */
   public String putVOTable(InputStream in,String label) {
      return putDataset(null,in,label);
   }

   /** VOApp interface */
   public String putFITS(InputStream in,String label) {
      return putDataset(null,in,label);
   }

   /** To get a dataset in VOTable format (typically for catalogs)
    * @param dataID the dataset identifier (application dependent
    * for instance, the plane name in Aladin)
    * @return a stream containing the VOTable
    */
   public InputStream getVOTable(String dataID) {
      MyByteArrayStream byteIn;

      if( dataID==null ) dataID="";
      try {
         Plan p = command.getFirstPlan(dataID);
         if( p==null || !p.isCatalog() ) return null;
         byteIn = writeObjectInVOTable(p);
      } catch( Exception e ) { e.printStackTrace(); return null; }

      return byteIn.getInputStream();
   }

   /** To get a dataset in FITS format (typically for images)
    * @param dataID the dataset identifier (application dependent
    * for instance, the plane name in Aladin)
    * @return a stream containing the FITS
    */
   public InputStream getFITS(String dataID) {
      InputStream in=null;

      if( dataID==null ) dataID="";
      try {
         Plan p = command.getFirstPlan(dataID);
         if( p==null || !p.isSimpleImage() && !(p instanceof PlanImageRGB) ) return null;
         in = save.saveImageFITS((OutputStream)null,(PlanImage)p);
      } catch( Exception e ) { e.printStackTrace(); }

      return in;
   }

   /** VOApp interface */
   public String putDataset(Object voApp,InputStream in, String label) {
      try {
         int n = calque.newPlan(in,label,null);
         if( n==-1 ) throw new Exception("Data format not recognized");
         
//         MyInputStream myIn = new MyInputStream(in);
//         int type = myIn.getType();
//         myIn = myIn.startRead();
//         Aladin.trace(3,(label==null?"Stream":label)+" => detect: "+myIn.decodeType(type));
//
//         if( (type & MyInputStream.FITS)!=0) {
//            n=calque.newPlanImage(myIn,label);
//
//         } else if( (type & (MyInputStream.FOV_ONLY))!=0 ) {
//            n=processFovVOTable(myIn,label,true);
//
//         } else if( (type & (MyInputStream.ASTRORES|MyInputStream.VOTABLE|MyInputStream.CSV))!=0 ) {
//            n=calque.newPlanCatalog(myIn,label);
//
//         } else throw new Exception("Data format not recognized");

         // magic code pour les FoV sans position (ie ne cr?ant pas de nouveau plan)
         if( n==-2 ) return null;

         if( voApp!=null ) {

            if( calque.plan[n].type==Plan.APERTURE ) ((PlanField)calque.plan[n]).addObserver((VOApp)voApp);
            else addVOAppObserver(voApp);
         }
         return calque.plan[n].label;

      } catch( Exception e ) { System.out.println("VOApp error!"); e.printStackTrace(); return null; }

//      try {
//         int n = calque.newPlan(in,label,null);
//         if( n==-1 ) throw new Exception("Data format not recognized");
//
//         // magic code pour les FoV sans position (ie ne cr?ant pas de nouveau plan)
//         if( n==-2 ) return null;
//
//         if( voApp!=null ) {
//
//            if( calque.plan[n].type==Plan.APERTURE ) ((PlanField)calque.plan[n]).addObserver((VOApp)voApp);
//            else addVOAppObserver(voApp);
//         }
//         return calque.plan[n].label;
//
//      } catch( Exception e ) { System.out.println("VOApp error!"); e.printStackTrace(); return null; }
   }

   private Vector VOAppObserver = null;
   private void addVOAppObserver(Object app) {
      if( VOAppObserver==null ) VOAppObserver = new Vector();
      if( !VOAppObserver.contains(app) ) VOAppObserver.addElement(app);
   }


   /**
    * Retourne true s'il y a au-moins un observer
    */
   protected boolean hasExtApp() {
      return VOAppObserver!=null && VOAppObserver.size()>0;
   }

  /** To transmit a VOTable to Aladin from another application
   * @param application reference to the external application (for callbacks)
   * @param in the stream containing VOTable structure. For callbacks, this
   * VOTable has to have an additionnal column in a first position giving
   * an unique identifier for the external application. This column should have
   * the following FIELD description :
   * <FIELD name="_OID" UCD="ID_NUMBER" type="hidden">
   */
   public void loadVOTable(ExtApp extApp, InputStream in) {
      putDataset(extApp,in,null);
//      this.extApp = extApp;
//      try {calque.newPlanCatalog(new MyInputStream(in),null); }
//      catch( Exception e ) { System.out.println("Ext App error!"); e.printStackTrace(); }
   }

   /**
    * Callback method allowing external application to ask Aladin
    * to SHOW a list of objects
    * @param oid list of oid
    */
   public void showVOTableObject(String oid[]) {
      Source o[] = calque.getSources(oid);
      if( o.length==0 ) { ooid=null; view.hideSource(); }
      else {
         ooid=o[0].getOID();
trace(3,"showVOTableObject("+o[0].id+"), "+ooid);
         view.showSource(o[0]);
      }
   }

   /**
    * Callback method allowing external application to ask Aladin
    * to SELECT a list of objects
    * @param oid list of oid
    */
   public void selectVOTableObject(String oid[]) {
      olistOid = oid;   // Evite des appels en aller/retour
if( levelTrace>=3 ) {
   StringBuffer s=null;
   for( int i=0; i<oid.length; i++ ) {
      if( s==null ) s=new StringBuffer(250);
      else s.append(",");
      s.append(oid[i]);
   }
   trace(3,"selectVOTableObject("+s+")");
}
      view.selectSourcesByOID(oid);
   }

   String ooid=null;
   protected void callbackShowVOApp(String oid) {
      if( ooid==oid || ooid!=null && ooid.equals(oid) ) return;
      ooid = oid;
      String l[] = new String[oid==null ? 0:1];
      if( oid!=null ) l[0] = oid;
      trace(3,"callbackShowVOApp("+(oid==null ? "null":oid)+")");
      if( VOAppObserver!=null ) {
         Enumeration e = VOAppObserver.elements();
         while( e.hasMoreElements() ) {
            Object app = e.nextElement();
            if( app instanceof ExtApp )  ((ExtApp)app).showVOTableObject(l);
            else ((VOApp)app).showVOTableObject(l);
         }
      }
   }

   String olistOid[]=null;
   protected void callbackSelectVOApp(String listOid[],int nbOid) {
      int i;
      if( olistOid!=null && olistOid.length==nbOid ) {
         for( i=0; i<nbOid; i++ ) if( olistOid[i]!=listOid[i] ) break;
         if( i==nbOid ) return;   // inutile, ca vient d'etre fait
      }
      String l[] = new String[nbOid];
if( levelTrace>=3 ) System.out.print("callbackSelectVOApp(");
      for( i=0; i<nbOid; i++ ) {
         l[i]=listOid[i];
if( levelTrace>=3 ) System.out.print((i>0?",":"")+l[i]);
      }
if( levelTrace>=3 ) System.out.println(")");
      olistOid = l;

      if( VOAppObserver!=null ) {
         Enumeration e = VOAppObserver.elements();
         while( e.hasMoreElements() ) {
            Object app = e.nextElement();
            if( app instanceof ExtApp )  ((ExtApp)app).selectVOTableObject(l);
            else ((VOApp)app).selectVOTableObject(l);
         }
      }
   }

   /** En fin de programme, supprimer les s?lections et show sur tous les observers si
    * c'est n?cessaire */
   private void resetCallbackVOApp() {
      if( VOAppObserver!=null ) {
         Enumeration e = VOAppObserver.elements();
         while( e.hasMoreElements() ) {
            Object app = e.nextElement();
            if( app instanceof ExtApp ) {
               ((ExtApp)app).showVOTableObject(new String[]{});
               ((ExtApp)app).selectVOTableObject(new String[]{});
            } else {
               ((VOApp)app).showVOTableObject(new String[]{});
               ((VOApp)app).selectVOTableObject(new String[]{});
            }
         }
      }
   }

   /** Dernier objet (Source) transmis ? un observer */
//   private Objet oVOApp=null;


   static protected void info(String s) { info(Aladin.aladin.f,s); }
   static public void info(Component c,String s) {
      if( NOGUI ) return;
      if( aladin.isFullScreen() && c==aladin.f ) c=aladin.fullScreen;
      Message.showMessage(c,s);
   }
   static public void warning(String s) { warning(Aladin.aladin.f,s,0); }
   static public void warning(Component c,String s) { warning(c,s,0); }
   static protected void warning(String s,int methode) { warning(Aladin.aladin.f,s,methode); }
   static protected void warning(Component c,String s,int methode) {
      if( s==null ) return;
      if( methode==1 ) aladin.command.printConsole("!!! "+s);
      if( NOGUI ) return;
      if( aladin.isFullScreen() && c==aladin.f ) c=aladin.fullScreen;
      Message.showWarning(c,s);
   }

   static public boolean confirmation(String s) { return confirmation(Aladin.aladin.f,s); }
   static public boolean confirmation(Component c,String s) {
      if( NOGUI ) return false;
      if( aladin.isFullScreen() && c==aladin.f ) c=aladin.fullScreen;
      boolean n=(Message.showConfirme(c,s)==Message.OUI);
      return n;
   }

   static protected boolean question(String s,Panel myPanel) { return question(Aladin.aladin.f,s,myPanel); }
   static protected boolean question(Component c,String s,Panel myPanel) {
      if( NOGUI ) return false;
      if( aladin.isFullScreen() && c==aladin.f ) c=aladin.fullScreen;
      boolean n=(Message.showQuestion(c,s,myPanel)==Message.OUI);
      return n;
   }

   /** Affiche une message de demande de confirmation s'il y a plus de 5 images ? charger simultan?ment */
   protected boolean testNbImgLoad(int n) {
      if( n<6 ) return true;
      if( n>16 ) {
         aladin.warning(chaine.getString("TOOMANYIMG")+" (<=16)");
         return false;
      }
      return aladin.confirmation(chaine.getString("NOTTOOMANY")+" ("+n+")");
   }

   /**
    * Mise en forme de l'indentation pour une sortie VOTable.
    * @param s le stream de sortie
    * @param indent le nombre de blancs pour indenter
    */
   private void writeIndent (OutputStream s, int indent) throws IOException {
      for( int i=0; i<indent; i++) writeBytes(s, " ");
   }

   /**
    * Generation du VOTable pour n attribut XML. Gere les retours a la ligne dans le cas
    * ou il y aurait trop d'attributs.
    * @param s le stream de sortie
    * @param nAtt le nombre d'attributs deja ecrits dans la ligne courante
    * @param limAtt le nombre max d'attributs autorises dans la ligne courant
    * @param indent le niveau d'indentation (nombre de blancs)
    * @param name le nom de l'attribut
    * @param value la valeur de l'attribut
    */
   private int writeAttribute(OutputStream s,int nAtt, int limAtt, int indent, String name, String value) throws IOException{
      if( value==null ) return 0;
      if( nAtt==limAtt ) { writeBytes(s, "\n"); writeIndent(s,indent+6); nAtt=0; }
      writeBytes(s, " "+name+"=\""+XMLParser.XMLEncode(value)+"\"");
      return nAtt+1;
   }

   /**
    * Generation du VOTable pour la legende d'un objet (les champs FIELD)
    * ATTENTION: Comme cette fonction est utilis?e pour passer les info ? VOPlot
    * et que VOPLOT ne veut tracer que les colonnes qui ont un datatype dument renseign?,
    * je force ? "double" si le datatype du field est ? null.
    * @param s le stream de sortie
    * @param o l'objet en question
    * @param writeOID true si on doit mettre la colonne OID
    * @param writeCoo true si on ecrit les colonnes _RAJ2000 et _DEJ2000
    */
   private void writeVOTableStartTable(OutputStream s,Source o,boolean writeOID,
                                       String linkSuffix,boolean addXY)
                                       throws IOException {
      int indent=4;
      Legende leg = o.leg;

      // On recupere le nom de la table sur le premier element "info" de l'objet (le triangle)
      StringTokenizer st = new StringTokenizer(o.info,"\t");
      String tableName=getValue(st.nextToken()); // Le nom de la table est "sur le triangle"
      writeIndent(s,indent);
      writeBytes(s, "<TABLE name=\""+XMLParser.XMLEncode(tableName)+"\">\n");
      indent+=3;
      
      // Les d?finitions par des groupes
      if( leg.hasGroup() ) {
         writeBytes(s, leg.getGroup() );
      }

      // Champs X et Y
      if( addXY ) {
          writeIndent(s,indent);
          writeBytes(s, "<FIELD name=\"X\" ID=\"X\" datatype=\"double\" />\n");

          writeIndent(s,indent);
          writeBytes(s, "<FIELD name=\"Y\" ID=\"Y\" datatype=\"double\" />\n");
      }

      // Le champ pour le OID
      if( writeOID ) {
         writeIndent(s,indent);
         writeBytes(s, "<FIELD name=\"_OID\" ucd=\"ID_NUMBER\" datatype=\"char\" type=\"hidden\"/>\n");
      }


      for( int i=0; i<leg.field.length; i++ ) {
         Field f = leg.field[i];
         if( !f.visible ) continue;
         writeIndent(s,indent); writeBytes(s, "<FIELD");
         int j=0;
         j=writeAttribute(s,j,3,indent,"ID",f.ID==null?f.name:f.ID);
         j=writeAttribute(s,j,3,indent,"name",f.name);
         j=writeAttribute(s,j,3,indent,"unit",f.unit);
         String ucd=f.ucd;
         j=writeAttribute(s,j,3,indent,"ucd",ucd);
         if( f.utype!=null && f.utype.length()>0 ) j=writeAttribute(s,j,3,indent,"utype",f.utype);
         if( f.datatype==null ) {
            j=writeAttribute(s,j,3,indent,"datatype","char");
            if( f.arraysize==null ) j=writeAttribute(s,j,3,indent,"arraysize","*");
         } else {
            j=writeAttribute(s,j,3,indent,"datatype",Field.typeFits2VOTable(f.datatype));
            if(f.arraysize==null && f.datatype.charAt(0)=='A' ) j=writeAttribute(s,j,3,indent,"arraysize","*");
         }
         j=writeAttribute(s,j,3,indent,"precision",f.precision);
         j=writeAttribute(s,j,3,indent,"width",f.width);
         if( !f.visible ) j=writeAttribute(s,j,3,indent,"type","hidden");
         if( f.arraysize!=null && f.datatype!=null ) j=writeAttribute(s,j,3,indent,"arraysize",f.arraysize);

         // Y a-t-il des tags dans le FIELD ?
         boolean flagLink = (linkSuffix!=null && f.gref!=null && f.gref.indexOf("url_spectrum")>=0);
         boolean flagDescription = (f.description!=null);
         if( flagLink || flagDescription ) writeBytes(s, ">\n");
         else writeBytes(s, "/>\n");

     	 // bidouille pour conserver lien sur tables apr?s x-match
     	 // ce sera plus simple quand on utilisera du VOTable pour stocker les catalogues
     	 if( flagLink ) {
     		int begin, end;
     		begin = f.gref.indexOf("{");
     		end = f.gref.indexOf("}");
     		String newVal = f.gref.substring(begin+1, end)+linkSuffix;
            writeIndent(s,indent+2);
     		writeBytes(s, "<LINK content-type=\"spectrumavo/fits\" title=\"Spectrum\" gref=\"Http ${"+newVal+"}\"/>\n");
     	 }
     	 if( flagDescription ) {
            writeIndent(s,indent+2);
     	    writeBytes(s, "<DESCRIPTION>"+XMLParser.XMLEncode(f.description)+"</DESCRIPTION>\n");
     	 }

     	 if( flagLink || flagDescription ) {
            writeIndent(s,indent);
            writeBytes(s, "</FIELD>\n");
     	 }
      }

      writeIndent(s,indent);
      writeBytes(s, "<DATA><TABLEDATA>\n");
   }

   /**
    * Generation du VOTable pour un objet
    * @param s le stream de sortie
    * @param writeOID s'il faut mettre la colonne OID
    * @param writeCoo true si on ecrit les colonnes _RAJ2000 et _DEJ2000
    * @param addXY ajout positions courantes X,Y
    * @param o L'objet a traiter
    */
   private void writeVOTableData(OutputStream s,Source o,boolean writeOID,boolean addXY)
                                 throws IOException {
      StringTokenizer st = new StringTokenizer(o.info,"\t");
      st.nextElement();		// On saute le triangle

      // Ajout colonnes X et Y
      PointD pAddXY=null;
      if( addXY ) {

          pAddXY = o.getViewCoordDouble(view.getCurrentView(),o.getL(),o.getL());
          // si hors champ, on ignore carr?ment la source !
          if( pAddXY==null ) {
              return;
          }
      }

      writeIndent(s,9);writeBytes(s, "<TR>");

      if( addXY ) {
          writeBytes(s, "<TD>"+pAddXY.x+"</TD>");
          writeBytes(s, "<TD>"+pAddXY.y+"</TD>");
      }

      // Sauvegarde de l'OID, (si necessaire, generation d'un oid)
      if( writeOID ) {
         String oid = o.getOID();
         if( oid==null ) oid = o.setOID();
         writeBytes(s, "<TD>"+oid+"</TD>");
      }

      for( int i=0; st.hasMoreTokens(); i++ ) {
         Words w = new Words(st.nextToken());
         if( !o.leg.isVisible(i) ) continue;
         if( i%5==0 && i>0 ) { writeBytes(s, "\n"); writeIndent(s,11); }
	     writeBytes(s, "<TD>"+xmlEncode(getValue(w.getText()))+"</TD>");
      }
      writeBytes(s, "</TR>\n");
   }
   
   /**
    * Generation du VOTable des objets du plan p.
    * Dans le cas ou il n'y a aucun objet (selectionne) dans le plan, la ressource
    * est totalement omise.
    * @param s1 le stream de sortie
    * @param o la source ? traiter
    * @param writeOID true si on ecrit la colonne OID
    */
   private void writeSourceInVOTable(OutputStream s1, Source o,
                                boolean writeOID, String linkSuffix, boolean addXY)
                                  throws IOException {
      Legende leg=null;

      // debut de la ressource (le plan)
      writeBytes(s1,
         "  <RESOURCE name=\""+XMLParser.XMLEncode(o.plan.label)+"\">\n"+
         "    <DESCRIPTION>"+XMLParser.XMLEncode(o.plan.label)+" object selection from Aladin</DESCRIPTION>\n"
      );

      if( addXY ) view.getCurrentView().paintComponent(null);

      leg = writeOneSourceInVOTable(s1,o,leg,writeOID,linkSuffix,addXY);
      if( leg==null ) {
         writeBytes(s1, "   </RESOURCE>\n");
         return;   // Il n'y avait aucun objet dans cette ressource
      }

      // Fin de la  table et fin de la ressource
      writeBytes(s1, "      </TABLEDATA></DATA></TABLE>\n");
      writeBytes(s1, "   </RESOURCE>\n");
   }

   /**
    * Generation du VOTable des objets du plan p.
    * Dans le cas ou il n'y a aucun objet (selectionne) dans le plan, la ressource
    * est totalement omise.
    * @param s1 le stream de sortie
    * @param p le plan a traiter
    * @param onlySelected true si on ne considere que les objets selectionnes par l'utilisateur
    * @param writeOID true si on ecrit la colonne OID
    */
   private void writePlanInVOTable(OutputStream s1, Plan p,boolean onlySelected,
								boolean writeOID, String linkSuffix, boolean addXY)
                                  throws IOException {
      Legende leg=null;

      // debut de la ressource (le plan)
      writeBytes(s1,
         "  <RESOURCE name=\""+XMLParser.XMLEncode(p.label)+"\">\n"+
         "    <DESCRIPTION>"+XMLParser.XMLEncode(p.label)+" object selection from Aladin</DESCRIPTION>\n"
      );


      if( addXY ) view.getCurrentView().paintComponent(null);

      // Parcours des objets
      Iterator<Obj> it = p.iterator();
      while( it.hasNext() ) {
         Obj o1 = it.next();
         if( !(o1 instanceof Source) ) continue;
         Source o = (Source)o1;

         // Ne traite que les objets selectionnes par l'utilisateur le cas echeant
         if( onlySelected && !((Position)o).isSelected() ) continue;
         
         leg = writeOneSourceInVOTable(s1,o,leg,writeOID,linkSuffix,addXY);
      }

      if( leg==null ) {
          writeBytes(s1, "   </RESOURCE>\n");
          return;	// Il n'y avait aucun objet dans cette ressource
      }

      // Fin de la derniere table et fin de la ressource
      writeBytes(s1, "      </TABLEDATA></DATA></TABLE>\n");
      writeBytes(s1, "   </RESOURCE>\n");

   }

   /**
    * Generation du VOTable pour une source particuli?re
    * @param s1 le stream de sortie
    * @param o la source ? traiter
    * @param oleg la l?gende de la source pr?c?dente, ou null si aucune
    * @param writeOID true si on ecrit la colonne OID
    * @return la l?gende de la source courante
    */
   private Legende writeOneSourceInVOTable(OutputStream s1, Source o, Legende oleg,
                                boolean writeOID, String linkSuffix, boolean addXY)
                                  throws IOException {
      
      // Nouvelle table dans le plan courant
      if( o.leg!=oleg ) {
         if( oleg!=null ) writeBytes(s1, "      </TABLEDATA></DATA></TABLE>\n");    // fin de la table precedente
         writeVOTableStartTable(s1,o,writeOID,linkSuffix,addXY);          //Nouvelle table
         oleg=o.leg;
      }

      // Ecriture des donnees pour l'objet courant
      writeVOTableData(s1,o,writeOID,addXY);
      
      return oleg;
   }

   /**
    * Generation de VOTable pour un plan donne ou pour les objets selectionnes
    * @param plans tableau de plans catalogue. Si null, on parcourt tous les plans
    * @param os outputstream dans lequel on ecrit. Si null, on ecrit dans le MyByteArrayStream retourne
    * @param writeOID si true, on ajoute une colonne OID
    * @param writeCoo si true, on ajoute les colonnes _RAJ2000 _DEJ2000
    * @param addXY si true, on ajoute les colonne X et Y (positions courantes)
    * @return MyByteArrayStream si os est null, retourne le stream dans lequel on a ecrit. retourne null sinon
    * @throws IOException
    */
   private MyByteArrayStream writeObjectInVOTable(Plan[] plans, OutputStream os,
         boolean writeOID, boolean xmatch, boolean addXY)
   throws IOException {
      return writeObjectInVOTable(plans,null,os,writeOID,xmatch,addXY);
   }
   protected MyByteArrayStream writeObjectInVOTable(Plan[] plans, Source src, OutputStream os,
            boolean writeOID, boolean xmatch, boolean addXY)
      throws IOException {
      MyByteArrayStream bas = null;
      OutputStream out;

      if( os!=null ) out = os;
      else out = bas = new MyByteArrayStream(10000);

      writeBytes(out,
            "<?xml version=\"1.0\"?>\n"+
            "<VOTABLE xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.1\""+
            " xmlns=\"http://www.ivoa.net/xml/VOTable/v1.1\""+
            " xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.1 http://www.ivoa.net/xml/VOTable/v1.1\">\n"+
            "  <DESCRIPTION>VOTable generated by Aladin</DESCRIPTION>\n"+
            // TODO : v?rifier si c'est encore conforme en VOTable 1.1
            "  <DEFINITIONS>\n"+
            "    <COOSYS ID=\"J2000\" equinox=\"2000.\" epoch=\"2000\" system=\"eq_FK5\"/>\n"+
            "  </DEFINITIONS>\n"
      );

      
      // plans non null : generation de VOTable pour les plans mentionn?s
      if( plans!=null ) {
         for( int i=0; i<plans.length; i++ ) {
            String linkSuffix = xmatch?"_tab"+(i+1):null;
            writePlanInVOTable(out, plans[i], false, writeOID, linkSuffix, addXY);
         }
         
      // G?n?ration d'un VOTable juste pour cette source
      } else if( src!=null ) {
         writeSourceInVOTable(out,src,writeOID,null,addXY);
         
      // sinon on g?n?re le VOTable pour tous les objets selectionnes
      } else {
         for( int i=calque.plan.length-1; i>=0; i-- ) {
            Plan p = calque.plan[i];
            if( !p.isCatalog()  || !p.flagOk || !p.active ) continue;
            writePlanInVOTable(out,p,view.hasSelectedObj(),writeOID,null,addXY);
         }
      }

      writeBytes(out, "</VOTABLE>\n");

      return bas;
   }

   /**
    * Generation de VOTable de tous les objets en cours de selection
    * dans Aladin (utilis? pour envoi VOTable ? VOPlot)
    */
   protected MyByteArrayStream writeObjectInVOTable(Plan pc) throws IOException {
      return writeObjectInVOTable(new Plan[] {pc}, null, false, false, false);
   }

   /**
    * Generation de VOTable de tous les objets en cours de selection
    * dans Aladin (utilis? pour envoi VOTable ? VOPlot)
    */
   protected MyByteArrayStream writeObjectInVOTable() throws IOException {
      return writeObjectInVOTable(null, null, true, false, false);
   }

	/**
	 * Generation de VOTable pour les objets du PlanCatalog pc
	 * (utilis? pour sauvegarde d'un plan en VOTable)
	 * @param pc le plan catalogue pour lequel on veut un VOTable
	 * @return ByteArrayStream
	 * @throws IOException
	 */
	protected MyByteArrayStream writePlaneInVOTable(Plan pc, OutputStream out, boolean addXY) throws IOException {
       return writeObjectInVOTable(new Plan[] {pc}, out, false, false, addXY);
	}

	protected MyByteArrayStream writePlanesInVOTable(Plan[] pc, OutputStream out, boolean writeOID, boolean xmatch) throws IOException {
       return writeObjectInVOTable(pc, out, writeOID, xmatch, false);
	}


   protected static void writeBytes(OutputStream out, String s) throws IOException {
      int n = s.length();
      byte b[] = new byte[n];
      for (int i = 0 ; i <n ; i++) b[i]=(byte)s.charAt(i);
      out.write(b);
   }

   // CETTE FONCTION DEVRAIT ETRE PLACEE DANS L'OBJET "Words" ET IL FAUDRAIT
   // L'UTILISER DANS "Source.getValue()"
   static String getValue(String s) {
      if( s.startsWith("<&") ) {
         int a = s.indexOf('|');
         if( a>0 ) {
            int b = s.indexOf('>',a+1);
            if( b>=0 ) return s.substring(a+1,b);
         }
      }
      return s;
   }

   static String xmlEncode(String s) {
      s = MetaDataTree.replace(s, "\"", "&quot;", -1);
      s = MetaDataTree.replace(s, "&", "&amp;", -1);
      s = MetaDataTree.replace(s, "<", "&lt;", -1);
      s = MetaDataTree.replace(s, ">", "&gt;", -1);
      return s;
   }
   /**
    * Cache d'imagettes (logo, images utilis?es dans les helps...)
    * @param aladinLabel Nom de l'image ? retrouver. Elle peut se trouver
    *             dans le fichier jar, dans le home directory ou
    *             sur le serveur de l'applet
    * @return L'image, ou null si erreur
    */
   private static Hashtable imageCache=null;
   public Image getImagette(String name) {
      if( imageCache==null ) imageCache = new Hashtable(10);

      // L'image a-t-elle ete deja chargee
      Object i = imageCache.get(name);
      if( i!=null ) {
         if( i instanceof Image) return (Image)i;
         else return null;
      }

      // Pas encore dans le cache, on la charge
      MyInputStream is=null;
      try {
      	 if( name.startsWith("http://") ) {
      	 	if( !NETWORK ) return null;
      	 	is = glu.getMyInputStream(name,false);
      	 } else is = new MyInputStream(Aladin.class.getResourceAsStream("/"+name));
         byte buf[] = is.readFully();
         if( buf.length==0 ) return null;  // Image introuvable
         Image img = Toolkit.getDefaultToolkit().createImage(buf);
         imageCache.put(name,img);
         return img;
      } 
      catch( Exception e ) { if( levelTrace>=3 ) e.printStackTrace(); }
      finally{ if( is!=null ) try { is.close(); } catch( Exception e) {} }

      // Cas d'erreur, on memorise dans le cache une chaine vide
      // histoire de ne pas essayer a chaque fois
      imageCache.put(name,"");
      return null;
   }

//  /** Appel a la generation par le serveur de l'applet ou "aladin.u-strasbg.fr"
//   * d'une page HTML permettant l'acces aux images originales de la pile
//   * Utilise le format HTTP suivant :
//   * frame=save&An=label&Dn=origine&Rn=format(FITS|HFITS|GFITS|MRCOMP)&Un=url
//   * ou n est un numero distinct pour chaque plan
//   *
//   * Dans le cas ou le plan vient du serveur d'images Aladin en JPEG,
//   * l'url du plan est modifiee pour que ce soit du FITS.
//   *
//   * RQ: IL PEUT Y AVOIR UN RISQUE DE DEBORDEMENT DE LA METHODE GET HTTP
//   * MAIS JE NE VOIS PAS COMMENT FAIRE CELA EN METHODE POST...QUI VIVRA VERRA
//   */
//   protected void saveHTML() {
//      StringBuffer pf=null;
//      int j=0;
//
//      synchronized( calque.pile ) {
//         for( int i=calque.plan.length-1; i>=0; i-- ) {
//            Plan p = calque.plan[i];
//         if( p.type!=Plan.IMAGE || !p.flagOk || p.error!=null ) continue;
//         if( p.u==null ) continue;
//
//         try {
//
//            if( pf==null ) pf=new StringBuffer();
//            else pf.append("&");
//
//            String u = p.getUrl();
//            String format= PlanImage.getFormat(((PlanImage)p).fmt);
//
//            // Cas particulier d'aladin en JPEG
//            if( ((PlanImage)p).isAladinJpeg() ) format="FITS";
//
//            pf.append("A"+j+"="+URLEncoder.encode(p.label)+
//                  "&D"+j+"="+URLEncoder.encode(p.copyright)+
//                  "&U"+j+"="+URLEncoder.encode(u)+
//                  "&R"+j+"="+URLEncoder.encode(format)
//            );
//            j++;
//         } catch( Exception e) {}
//      }
//      }
//
//      if( pf==null ) {
//         Aladin.warning(chaine.getString("NOIMGSTK"));
//         return;
//      }
//
//      String s=Aladin.STANDALONE?"http://aladin.u-strasbg.fr/java":CGIPATH;
//      String u = s+"/nph-aladin.pl?frame=save&"+pf;
//      trace(2,u);
//      glu.showDocument("Http",u,true);
//
//   }
   
/*
    void debug() {
      int i;

      // Affichage des infos des sources selectionnes
      System.out.println("DEBUG: INFORMATION SUR LES SOURCES SELECTIONNEES:\n");
      for( i=0; i<view.vselobj.size(); i++ ) {
         try {
            Source o = (Source) view.vselobj.elementAt(i);
            o.debug();
         } catch( Exception e1 ) {
            try {
               Objet o = (Objet) view.vselobj.elementAt(i);
               o.debug();
            } catch( Exception e2 ) {}
         }
         System.out.println("");
      }

      // Affichage des infos du zoom
      System.out.println("DEBUG: INFORMATION SUR LE ZOOM:\n");
      view.zoomview.debug();

      // Sauvegarde de la vue courante en JPEG
      System.out.println("DEBUG: Sauvegarde de la vue dans essai.jpg\n");
      JPEG j = new JPEG();
      j.compress(view.img,"essai.jpg");
      System.out.println( mesure.getGML());
   }
*/

  /** Imprime la vue courante
   void print() {
      PrintJob pj = Toolkit.getDefaultToolkit().getPrintJob(f,"Aladin",null);
      if( pj!=null ) {
         Graphics pg = pj.getGraphics();
         if( pg!=null ) {
            view.print(pg);

            pg.dispose();
         }
         pj.end();
      }
   }
*/

   /** thomas : je ne sais pas bien o? mettre cette m?thode
    * Traitement d'un document VOTable d?crivant un Field of View (MyInputStream.FOV_ONLY)
    * @param in
    */
   public int processFovVOTable(MyInputStream in, String label, boolean createNewPlane) {
   	Aladin.trace(2, "Processing FOV_ONLY document !!");
 	FootprintParser fp = new FootprintParser(in,null);
 	Hashtable<String, FootprintBean> hash = fp.getFooprintHash();
 	Enumeration<String> enumFov = hash.keys();
 	String key = null;
 	FootprintBean fpBean = null;
 	PlanField pf = null;
 	while(enumFov.hasMoreElements()) {
 		key = enumFov.nextElement();
 		fpBean = hash.get(key);
 		if ( ! fpBean.isDisplayInFovList() ) {
 		    continue;
 		}
 		((ServerFoV)dialog.server[ServerDialog.FIELD]).registerNewFovTemplate(key,pf=new PlanField(this,fpBean, key));
 		pf.make(0.,0.,0.);
 	}

 	if( createNewPlane && fpBean.coordsAreSet() ) {

		try {
		    // PF Jan 09
			if( fpBean.rotAreSet() ) {
			   pf.make(fpBean.getRa(), fpBean.getDe(), fpBean.getRaRot(), fpBean.getDeRot(),fpBean.getPosAngle());
			} else {
			   pf.make(fpBean.getRa(), fpBean.getDe(), fpBean.getPosAngle());
			}
		}
		catch( Exception e) {}

		pf.setRollable(fpBean.isRollable());
		pf.setMovable(fpBean.isMovable());

		if( label==null ) label = key;
 		return calque.newPlanField(pf, label);

 	}

 	return -1;

   }

   public int processFovVOTable(MyInputStream in) {
   	return processFovVOTable(in, null, false);
   }

   public void mouseExited(MouseEvent e) {
      localisation.setMode(MyBox.SAISIE);
//      pixel.setMode(MyBox.SAISIE);
   }

   @Override
public boolean handleEvent(Event e) {
      // pour sortir du mode Robot
      if( e.id==Event.KEY_PRESS && e.key==java.awt.event.KeyEvent.VK_ESCAPE && command.robotMode ) {
         stopRobot(f);
         return true;
      }
//      if( flagLoad ) return true;
      return super.handleEvent(e);
   }

   /** interrompt le tutorial deroule par le robot, et vide le stream */
   public void stopRobot(Component c) {
      ActionExecutor.interruptAction = true;
      Thread t = MyRobot.ae != null ? MyRobot.ae.runme : null;

      if( MyRobot.ae!=null ) t.suspend();
      boolean stop = (Message.showConfirme(c, chaine.getString("STOPDEMO")))
                     == Message.OUI;
      if( !stop ) {
          if( t!=null ) t.resume();
          ActionExecutor.interruptAction = false;
          return;
      }

      if( t!=null ) t.stop();
      System.out.println("Interrupting robot mode");
      ActionExecutor.interruptAction = false;
      ActionExecutor.ready = true;
      command.stackStream.removeAllElements();
      command.setStream(null);
      command.execLater("robot off");

      command.curTuto = null;

   }


   /** Lib?re toute la m?moire inutile */
   protected void gc() {
      if( gc ) {
         System.runFinalization();
         System.gc();
         Util.pause(100);
      }
      setMemory();
   }

   /** G?n?ration d'un log via le glu */
   public void log(String id,String param) {
      glu.log(id,param);
   }
   
   
   
   /** Positionnement de l'antialiasing */ 
   public void setAliasing(Graphics g) { setAliasing(g,ALIASING); }
   public void setAliasing(Graphics g,int aliasing) {
      if( aliasing==0 || !(g instanceof Graphics2D) ) return;
      
      if( aliasing==1 ) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
      } else {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
      }
   }

   static private int firstMem=0;
   static private String MB;



   boolean dontReconnectAutomatically = false ; // if user asked once to be unregistered from the hub
   /** met ? jour l'?tat de la connexion PLASTIC/SAMP
    * - si non connect? au hub : tente de se connecter (sauf si l'utilisateur s'est d?connect? de son fait)
    *
    * - si connect? : v?rifie que la connexion est valide
    *
    * Cette m?thode est appel?e par le timer d?fini au niveau de CreatObj.run()
    */
   protected void setPlastic(final boolean launchHubIfNeeded) {
       final AppMessagingInterface pMgr = getMessagingMgr();
       // if not connected, try to register with a hub
       if( ! pMgr.isRegistered() && ! dontReconnectAutomatically
       		&& plasticPrefs.getBooleanValue(PlasticPreferences.PREF_AUTOCONNECT ) ) {
           appMessagingMgr.trace("Trying to autoconnect to "+pMgr.getProtocolName()+" hub");

           new Thread("AladinPlasticRegister") {
               @Override
            public void run() {
                   try { pMgr.register(true, launchHubIfNeeded); }
                   catch( Exception e ) {
                      if( levelTrace>=3 ) e.printStackTrace();
                   }
               }
           }.start();

       }
       // TODO : test whether the hub is responsive
       else {
           boolean alive = appMessagingMgr.ping();
           appMessagingMgr.trace("Testing if "+appMessagingMgr.getProtocolName()+" hub is still alive: "+alive);
       }

       pMgr.updateState();
   }

   private int lastMem=0;
   private long lastNbSrc=0;
   private long lastSetMemory = 0L;

   /** Positionne le statut de l'usage de la m?moire (pour les data)
    * en bas ? droite de l'?cran d'Aladin */
   protected void setMemory() {
      long t = System.currentTimeMillis();
      if( t-lastSetMemory<1000 ) return;
      lastSetMemory=t;

      if( firstMem==0 ) {
         MB=chaine.getString("MB");
         System.runFinalization();
         System.gc();
         Util.pause(100);
      }
      if( MB==null ) MB="Mb";
      int nbPlan = calque.getNbUsedPlans();
      int nbView = view.getNbUsedView();
      int nbSel = view.vselobj.size();
      long nbSrc = calque.getNbSrc();
      double fps = calque.getFps();
// XXX A VOIR XXX      long cache = calque.isBkgdActive() ? (int)(calque.planBG.getCacheSize()/(1024*1024)) : 0;
      long cache=0;
      cache += (int)(sizeCache/(1024*1024));
      int mem = (int)( (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024*1024));
      if( firstMem==0 ) firstMem=mem;
      mem-=firstMem;
      String s= nbSel+" sel / "+nbSrc+" src    "
                    + (nbView>view.getModeView()?nbView+" views   ":"")
                    +(fps>0?(int)Math.round(fps)+"fps / ":"")+mem+MB;
      memStatus.setText(s);
      if( infoPanel!=null ) infoPanel.doLayout();
      Util.toolTip(memStatus,"<HTML><CENTER>"
                           +nbSrc+" source"+(nbSrc>1?"s":"")+", "
                           +nbPlan+" plan"+(nbPlan>1?"s":"")+", "
                           +nbView+" view"+(nbView>1?"s":"")+"<BR>"
                           +"Mem: "+mem+MB+" / "+MAXMEM+MB+"<BR>"
                           +(cache<1?"" : "Disk cache: "+cache+MB+"<BR>")
                           +(fps>0?"Fps: "+Util.round(fps,1)+"<BR>":"")
                           +"Paint: "+ViewSimple.timeForPaint+"ms</CENTER></HTML>");
      lastMem=mem;
      lastNbSrc=nbSrc;
      
      // Warnings pour les limites de m?moire
      int memory = (int)(MAXMEM-lastMem);
      if( memory<50 ) {
         trace(4,"Aladin.setMemory(): low memory ("+memory+" MB)");
         if( freeSomeRam()>0 ) return;
         urlStatus.setText("Warning: Aladin is running in low memory configuration ("+memory+"MB)");
//         if( memory<20 ) {
//            if( !lowMem ) {
//               warning("Low memory (only "+memory+"MB available) !!\nRemove some stack planes as soon as possible !");
//               lowMem=true;
//            }
//         } else lowMem=false;
      }
   }
   
   public long freeSomeRam() { return freeSomeRam(-1,null); }
   
   /** Demande de lib?ration de la m?moire non indispensable. Si -1, demande du max
    * @param askMem Nombre d'octets demand?s, -1 si max
    * @param saufPlan plan a ne pas lib?rer, null si aucun sp?cifi?
    * @return nombre d'octets lib?r?s
    */
   public long freeSomeRam(long askMem,Plan saufPlan) {
      long mem=0;
      Plan [] p = calque.getPlans();
      for( int i=0; i<p.length; i++ ) {
         if( p[i]==saufPlan ) continue;     // On ne lib?re pas ce plan
         if( p[i] instanceof PlanImageBlink ) {
            mem+= ((PlanImageBlink)p[i]).freeRam(askMem==-1 ? -1 : askMem-mem); 
         }
         if( askMem!=-1 && mem>=askMem ) break;
      }
      if( mem>0 ) gc();
      return mem;
   }
   
   private boolean lowMem=false;
   
   /** Retourne le nombre de m?gaoctets disponibles en RAM */
   protected double getMem() {
      double mem = (Runtime.getRuntime().maxMemory()-
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()))/(1024*1024.);
      return mem;
   }
   
   /** Retourne true si on a de la marge en RAM */
   protected boolean enoughMemory() { return MAXMEM-lastMem>256; }

   /** Ajuste rapidement le status de m?moire pour tenir compte de nouvelles sources
    * s?lectionn?es */
   void adjustNbSel() {
      if( MB==null ) MB="Mb";
      int nbSel = view.vselobj.size();
      String s= nbSel+" sel / "+lastNbSrc+" src    "+lastMem+MB;
      memStatus.setText(s);
  }

   /** Changement du niveau de trace */
   public void setTraceLevel(int n) {
      levelTrace=n;
//      if( n>0 ) pixel.addDebugItem();   // ajout de la possibilit? Pixel FITS value
      if( n==0 ) command.println("Trace off");
      else command.println("Trace on (level "+n+")");

   }

  /** Affichage des message de debugging. Si n est >= au niveau courant
   * le message sera affiche sur la sortie standard
   * @param n Le niveau de debogage
   * @param s Le message a afficher
   */
   static final public void trace(int n,String s) {
      if( n>levelTrace ) return;
      s =     n==1 ? ".    "+s+"..."
            : n==2 ? "--   "+s
            : n==3 ? "***  "+s
            :        ">>>> "+s;
      System.out.println(s);
//      if( n>2 && aladin!=null && aladin.console!=null ) aladin.console.setInPad(s+"\n");
   }

   static final public boolean isFootprintPlane(Plan p) {
      return p.type==Plan.FOV || p.type==Plan.APERTURE;
//      if( p instanceof PlanFov || p instanceof PlanField ) return true;
//	   return false;
   }

   /**
    * Create a new Aladin Image plane by plugin.
    * @param name plane name
    * @return The AladinData java object corresponding to this new plane
    * @throws AladinException
    */
   public AladinData createAladinData(String name) throws AladinException {
      return createAladinImage(name);
   }
//   public AladinData createAladinData(String name) throws AladinException {
//      try {
//         name = calque.newPlanPlugImg(name);
//         AladinData ag = getAladinData(name);
//         double pix[][] = new double[500][500];
//         ag.setPixels(pix);
//         ag.plan.error = PlanImage.NOREDUCTION;
//         ag.plan.planReady(true);
//         return ag;
//      } catch( Exception e ) {
//         e.printStackTrace();
//         throw new AladinException(AladinData.ERR009);
//      }
//   }

   /**
    * Create a new Aladin Catalog plane by plugin.
    * @param name plane name
    * @return The AladinData java object corresponding to this new plane
    * @throws AladinException
    */
   public AladinData createAladinCatalog(String name) throws AladinException {
      return new AladinData(this,2,name);
   }
//   public AladinData createAladinCatalog(String name) throws AladinException {
//      try {
//         name = calque.newPlanPlugCat(name);
//         AladinData ag = getAladinData(name);
////         ag.plan.planReady(true);
//         return ag;
//      } catch( Exception e ) {
//         e.printStackTrace();
//         throw new AladinException(AladinData.ERR009);
//      }
//   }
   
   /** Return the Aladin plugin directory */
   public String getPluginDir() {
      return plugins.getPlugPath();
   }

   /**
    * Create a new Aladin Image plane by plugin.
    * @param name plane name
    * @return The AladinData java object corresponding to this new plane
    * @throws AladinException
    */
   public AladinData createAladinImage(String name) throws AladinException {
      return new AladinData(this,1,name);
   }

   /** Provide a AladinData object allowing to manipulate the Aladin current Image
    * @return the AladinData corresponding to the current image
    * @throws AladinException
    */
   public AladinData getAladinImage() throws AladinException {
      return getAladinData(calque.getPlanBase().label);
   }

   /** Provide a AladinData object allowing to manipulate the first selected
    * Aladin plane (from the top of the stack
    * @return the AladinData corresponding to the first Aladin selected plane
    * @throws AladinException
    */
   public AladinData getAladinData() throws AladinException {
      return getAladinData(null);
   }
//   public AladinData getAladinData() throws AladinException {
//      try {
//         return getAladinData(calque.getFirstSelectedPlan().label);
//      } catch( Exception e ) { throw new AladinException(AladinData.ERR000); }
//   }

   /** Provide a AladinData object allowing to manipulate an Aladin plane
    * @param planeID plane ID (label or number (1 is the bottom of the stack)
    * @return the AladinData corresponding to the specified plane
    * @throws AladinException
    */
   public AladinData getAladinData(String planeID) throws AladinException {
      return new AladinData(this,0,planeID);
   }

   /** Return the list of Aladin plane IDs beginning by the plane at the bottom
    * of the stack
    * @return the list of Aladin plane IDs
    */
   public String [] getAladinStack() {
      return calque.getStackLabels();
   }

//	faire log IDL + log macro
//	faire FAQ IDL + FAQ macro

	/************** M?thodes li?es ? l'interaction IDL/Aladin ****************/

	// true si on n'a pas encore logu? l'usage via IDL
	private boolean mustLogIDL = true;

   protected String osName;
   protected String osArch;
   protected String osVersion;
   protected String javaVendor;

	/**
	 * logue l'usage d'IDL
	 *
	 */
	synchronized private void logIDL(String s) {
		if( mustLogIDL ) {
			mustLogIDL = false;
			log("IDLcall", s);
		}
	}

	/** transmet la position courante du r?ticule
	 * m?thode ad-hoc cr??e pour l'interaction avec IDL
	 *
	 * @return la position sous forme d'un tableau de double (1er elt : RAJ J2000, 2e elt : DE J2000)
	 */
	public double[] getReticlePos() {
		logIDL("getReticlePos");

		if( view.repere==null ) return null;

		return new double[] {view.repere.raj, view.repere.dej};
	}

	/** transmet la valeur du pixel pour la position courante du r?ticule
	 * m?thode ad-hoc cr??e pour l'interaction avec IDL
	 *
	 * @return la valeur du pixel sous la forme d'un double
	 */
	public double getPixelValAtReticlePos() {
		logIDL("getPixelValAtReticlePos");

		if( view.repere==null ) return Double.NaN;

		return view.getPixelValue();
	}


	/** s?lection de source selon
	 * (utilis? par IDL via IDL Java Bridge)
	 *
	 * @param planeName label du plan
	 * @param indexes num?ros d'ordre des sources
	 */
	public void selectSourcesByRowNumber(String planeName, int[] indexes) {
		logIDL("selectSourcesByRowNumber");

		Plan plan = command.getFirstPlan(planeName);

		if( plan==null ) {
			System.out.println("Could not find plane with name "+planeName);
			return;
		}

		view.selectSourcesByRowNumber((PlanCatalog)plan, indexes);
	}


	/** R?cup?re le contenu d'un plan catalogue dans des vecteurs
	 * (utilis? par IDL via IDL Java Bridge)
	 *
	 * @param planeName
	 * @param colNames
	 * @return
	 */
	public String[][] getTableVectors(String planeName, String[] colNames, String[] colDataTypes) {
		logIDL("getTableVectors");

		Plan plan = command.getFirstPlan(planeName);
		if( plan==null ) {
			System.out.println("Could not find plane with name "+planeName);
			return null;
		}
		else if( !plan.isCatalog() ) {
			System.out.println("Plane "+planeName+" is not a catalogue plane !");
			return null;
		}

		int[] colIdx = new int[colNames.length];
		for( int i=0; i<colIdx.length; i++ ) colIdx[i] = -1;
		Legende leg = plan.getFirstLegende();
		for (int i = 0; i < colNames.length; i++) {

			// au cas o? on n'a pas donn? de noms de colonnes
			if( colNames[i]==null || colNames[i].length()==0 ) {
				colIdx[i] = i;
				continue;
			}

			for( int j=0; j<leg.field.length; j++ ) {
				if( colNames[i].equals(leg.field[j].name ) ) {
					colIdx[i] = j;
					break;
				}
			}

			// exit if we can't find one of the column
			if( colIdx[i]==-1 ) {
				System.out.println("Could not find column "+colNames[i]);
				return null;
			}
		}

		Source s;
//		String[] values;
		String[][] data = new String[plan.getCounts()][colIdx.length];
		Iterator<Obj> it = plan.iterator();
		// boucle sur les objets du plan
		for( int i=0; it.hasNext(); i++ ) {
	         Obj o = it.next();
	         if( !(o instanceof Source) ) continue;
	         s = (Source)o;
//			values = Util.split(s.info, "\t");

			// boucle sur les colonnes ? r?cup?rer
			for( int j=0; j<colIdx.length; j++ ) {
//				System.out.println(s.getValue(colIdx[j]));
				data[i][j] = s.getValue(colIdx[j]);
			}
		}

		for( int i=0; i<colDataTypes.length; i++ ) {
			String dataType = leg.field[i].datatype;
			if( dataType==null ) dataType = "char";
			colDataTypes[i] = Field.typeFits2VOTable(dataType);
		}
		return data;
	}

	/** Charge une image ? partir de son path
	 *
	 * @param file path vers l'image
	 * @param planeName nom du plan cr??
	 */
	public void loadImageFromFile(String file, String planeName) {
		logIDL("loadImageFromFile");

		try {
   	  		putFITS(new FileInputStream(new File(file)), planeName);
		}
		catch(Exception e) {
			e.printStackTrace();

		}
	}

	/** Cr?e un nouveau plan catalogue ? partir d'un tableau de vecteurs
	 * (utilis? par IDL via IDL Java Bridge)
	 *
	 * @param vectors
	 * @param vecNames
	 * @param planeName
	 */
	public void loadTableFromVectors(String[][] vectors, String[] vecNames, String planeName) {
		logIDL("loadTableFromVectors");

		Vector vField = new Vector();
		// on donne des noms par d?faut si n?cessaire
		if( vecNames==null ) {
			vecNames = new String[vectors.length];
			for( int k=0; k<vecNames.length; k++ ) vecNames[k] = "col"+k;
		}

		// TODO : ? virer, plus n?cessaire
		vecNames[0] = "ra";
		vecNames[1] = "dec";

		Field f;
		for( int i=0; i<vecNames.length; i++ ) {
			f = new Field(vecNames[i]);
			if( i==0 ) f.coo = Field.RA;
			else if( i==1 ) f.coo = Field.DE;
//			if( i==0 || i==1 ) f.coo = true;
			vField.addElement(f);
		}
		Legende leg = new Legende(vField);

		int nbSources = vectors[0].length;
		int nbCol = vectors.length;


		// TODO : remplacer ceci par un passage direct des tableaux ?
		MyInputStream mis=null;
		MyByteArrayStream stream=null;
		try {
			stream = new MyByteArrayStream();
			for( int i=0; i<nbCol; i++ ) {
				stream.write(vecNames[i].getBytes());
				if( i!=nbCol-1 ) stream.write("\t".getBytes());
			}

			stream.write("\n".getBytes());

			for( int i=0; i<nbCol; i++ ) {
				stream.write("----------".getBytes());
				if( i!=nbCol-1 ) stream.write("\t".getBytes());
			}

			for( int j=0; j<nbSources; j++ ) {
				stream.write("\n".getBytes());

				for( int i=0; i<nbCol; i++ ) {
					stream.write(vectors[i][j].getBytes());
					if( i!=nbCol-1 ) stream.write("\t".getBytes());
				}
			}

			mis = new MyInputStream(stream.getInputStream());
			mis.startRead();
			calque.createPlanCatalog(mis, planeName);
		}
		catch(Exception e) { e.printStackTrace();return; }
		finally {
           if( stream!=null ) try { stream.close(); } catch( Exception e1 ) {} 
		}

		
//		int indice = calque.newPlanCatalog(mis, planeName);
//		PlanCatalog plan = (PlanCatalog)calque.plan[indice];
	}
	/******************* fin des m?thodes li?es ? IDL ************************/


    /** Cr?ation d'un fichier temporaire dans le r?pertoire "cache" d'Aladin
    *
    * @param prefix
    * @param suffix
    * @return
    */
   public File createTempFile(String prefix, String suffix) {
       if( !createCache() ) {
           Aladin.trace(3, "Couldn't create cache directory");
           return null;
       }

       File tmpFile;
       try {
           // should give a unique file name
           tmpFile = new File(new File(Aladin.CACHEDIR), prefix+System.currentTimeMillis()+suffix);
       }
       catch(Exception e) {
           Aladin.trace(3, "Error while creating temp file : "+e.getMessage());
           return null;
       }

       return tmpFile;
   }

     public void snapShot(OutputStream o) { snapShot(o,500,500); }
     public void snapShot(OutputStream o,int width,int height) {
          try {
             command.sync();
             ViewSimple v = view.getCurrentView();
             Image img = v.getImage(width,height);
             save.ImageWriter(img,"png",-1,true,o);
          } catch( Exception e ) { e.printStackTrace(); }
}

     public String getNearestUrl(int x, int y) {
        try {
           command.sync();
           ViewSimple v = view.getCurrentView();
           PointD p = v.getPosition((double)x,(double)y);
           Vector h = calque.getObjWith(v,p.x,p.y);
           Source s = (Source)h.elementAt(0);
           return s.getFirstLink();
        } catch( Exception e ) { e.printStackTrace(); }
        return "-";
     }

     public String setRepere(int x, int y ) {
        ViewSimple v = view.getCurrentView();
        PointD p = v.getPosition((double)x,(double)y);
        Coord coo = new Coord();
        coo.x=p.x; coo.y=p.y;
        v.getProj().getCoord(coo);
        view.setRepere(coo);
        return coo.getSexa();
     }

     public String getRepere() {
        return view.repere.getSexa();
     }

     public void mouseClicked(MouseEvent e) { }
     public void mouseEntered(MouseEvent e) { }
     public void mousePressed(MouseEvent e) {}
     public void mouseReleased(MouseEvent e) { }
     public void mouseDragged(MouseEvent e) { }
     
     
     static public boolean test() {
        System.out.println("Aladin code test running...");
        Aladin aladin = Aladin.aladin;
        if( aladin==null ) {
           NOGUI=true;
           aladin = new Aladin();
           startInFrame(aladin);
        }
        boolean rep=true;

        rep &= PlanCatalog.test(aladin);
        rep &= Calib.test();

        System.out.println( rep ? "Tout est bon dans l'cochon !" : "Aladin code test FAILED !");
        return rep;
     }

	public void creatLocalPlane(String filepath, String name) {
		calque.newPlan(filepath,name,null);
	}
	
}
