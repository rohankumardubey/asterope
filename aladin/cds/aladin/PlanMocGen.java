// Copyright 2012 - UDS/CNRS
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

import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;

/** Generation d'un plan MOC ? partir d'une liste de plans (Image, Catalogue ou map HEALPix) 
 * @author P.Fernique [CDS]
 * @version 1.0 - nov 2012
 */
public class PlanMocGen extends PlanMoc {
   
   private Plan [] p;       // Liste des plans ? ajouter dans le MOC
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private double pixMin;   // Pour un plan Image ou map Healpix, valeur minimale des pixels retenus (sinon NaN)
   private double pixMax;   // Pour un plan Image ou map Healpix, valeur maximale des pixels retenus (sinon NaN)
   private int res;         // R?solution (ordre) demand?e
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est termin?)
   
   protected PlanMocGen(Aladin aladin,String label,Plan[] p,int res,double radius,double pixMin,double pixMax) {
      super(aladin,null,null,label,p[0].co,30);
      this.p = p;
      this.res=res;
      this.radius=radius;
      this.pixMin=pixMin;
      this.pixMax=pixMax;
      
      pourcent=0;
      gapPourcent = 100/p.length;
      
      suiteSpecific();
      threading();
      log();
   }
   
   protected void suite1() {}
      
   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double radius) {
      Iterator<Obj> it = p1.iterator();
      Healpix hpx = new Healpix();
      int order = res;
      Coord coo = new Coord();
      int n=0;
      int m= p1.getCounts();
      double incrPourcent = gapPourcent/m;
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         if( m<100 || n%100==0 ) pourcent+=incrPourcent;
         try {
            coo.al = ((Position)o).raj;
            coo.del = ((Position)o).dej;
            long [] npix ;
            if( radius==0 ) npix = new long[] { hpx.ang2pix(order, coo.al, coo.del) };
            else npix = hpx.queryDisc(order, coo.al, coo.del, radius);
            
            for( long pix : npix ) moc.add(order,pix);
            n+=npix.length;
            if( n>10000 ) { moc.checkAndFix(); n=0; }
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
   }

   // Ajout d'un plan Image au MOC en cours de construction
   private void addMocFromImage(Plan p1,double pixMin,double pixMax) {
      boolean flagRange = !Double.isNaN(pixMin) || !Double.isNaN(pixMax);
      PlanImage pimg = (PlanImage)p1;
      Healpix hpx = new Healpix();
      int order = res;
      Coord coo = new Coord();
      double gap=1;
      double gapA=0,gapD=0;
      try { 
         gapA = Math.min(p1.projd.getPixResAlpha(),p1.projd.getPixResDelta());
         for( order=res; CDSHealpix.pixRes( CDSHealpix.pow2(order) )/3600. <= gapA*2; order--);
      } catch( Exception e1 ) {
         e1.printStackTrace();
      }
//      if( gap<1 || Double.isNaN(gap) ) gap=1;
//      
      gapD = CDSHealpix.pixRes( CDSHealpix.pow2(order) )/3600.;
//      System.out.println("res="+res+" order="+order+" gapA ="+Coord.getUnit(gapA)+" gapD ="+Coord.getUnit(gapD)+" gap="+gap);
      
      // Pour garder en m?moire les pixels 
      pimg.setLockCacheFree(true);
      pimg.pixelsOriginFromCache();

      double incrPourcent = gapPourcent/pimg.naxis2;
      long oNpix=-1;  
      for( double y=0; y<pimg.naxis2; y+=gap ) {
         pourcent += incrPourcent;
         for( double x=0; x<pimg.naxis1; x+=gap ) {
            try {
               coo.x = x;
               coo.y = (pimg.naxis2-y-1);
               
               // dans du vide - on test d'abord le buffers 8bits, et on v?rifie si on tombe sur 0
               if( pimg.getPixel8Byte((int)x,(int)coo.y)==0 && Double.isNaN(pimg.getPixel((int)x,(int)y)) ) continue;
               
               // Hors de la plage de pixels
               if( flagRange ) {
                  double pix = pimg.getPixel((int)x,(int)y);
                  if( !Double.isNaN(pixMin) && pix<pixMin ) continue;
                  if( !Double.isNaN(pixMax) && pix>pixMax ) continue;
               }
               
               pimg.projd.getCoord(coo);
               long npix=0;
               npix = hpx.ang2pix(order, coo.al, coo.del);

               // Juste pour ?viter d'ins?rer 2x de suite le m?me npix
               if( npix==oNpix ) continue;
               
               moc.add(order,npix);
               oNpix=npix;
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      }
      
      // Les pixels peuvent d?sormais ?tre lib?r?s
      pimg.setLockCacheFree(false);
   }
   
   // Ajout d'un plan map Healpix au MOC en cours de construction
   private void addMocFromPlanBG(Plan p1,int res, double pixMin,double pixMax) {
      boolean flagRange = !Double.isNaN(pixMin) || !Double.isNaN(pixMax);
      PlanBG p = (PlanBG)p1;
      
      // D?termination de l'ordre pixel (order) et tuiles (fileOrder)
      int order = p.getLosangeOrder();
      int z = (int)p.getZ();
      
      int divOrder=0;
      int fileOrder = res - order;
      
      
      // L'ordre des tuiles ne peut ?tre inf?rieur ? 3
      if( fileOrder<3 ) {
         divOrder=(3-fileOrder)*2;
         fileOrder=3;
      }
      
      /// L'ordre des tuiles ne peut entrainer le d?passement de la r?solution
      // de la map HEALPix
      if( fileOrder>p.getMaxFileOrder() ) {
         fileOrder=p.getMaxFileOrder();
         res = fileOrder+order;
      }
      
      // On g?n?re d'abord un MOC dans le syst?me de r?f?rence de la map HEALPix
      // on fera la conversion en ICRS ? la fin du processus
      moc.setCoordSys( p.frameOrigin==Localisation.GAL ? "G" : 
                       p.frameOrigin==Localisation.ECLIPTIC ? "E" : "C");
      frameOrigin = p.frameOrigin;
      
      // Nombre de losanges ? traiter
      int n = (int)CDSHealpix.pow2(fileOrder); 
      n=12*n*n;
      
      System.out.println("Nombre de losanges ? traiter : "+n);
      
      // Sans doute inutile car d?j? fait
      try { p.createHealpixOrder(order); } catch( Exception e1 ) { e1.printStackTrace(); }
      long nsize = CDSHealpix.pow2(order);
      
      double incrPourcent = gapPourcent/n;
      
      for( int npixFile=0; npixFile<n; npixFile++ ) {
         System.out.println("Traitement de "+npixFile);
         pourcent += incrPourcent;
         HealpixKey h = p.getHealpixLowLevel(fileOrder,npixFile,z,HealpixKey.SYNC);
         if( h==null ) continue;
         
         long min = nsize * nsize * npixFile;
         try {
            int nb=0;
            long oNpix=-1;  
            for( int y=0; y<h.height; y++ ) {
               for( int x=0; x<h.width; x++ ) {
                  try {
                     int idx = y * h.width + x;
                     double pixel = h.getPixel(idx,HealpixKey.NOW);
                     
                     // Pixel vide
                     if( Double.isNaN( pixel ) || pixel==blank ) continue;
                     
                     // En dehors de la plage
                     if( flagRange ) {
                        if( !Double.isNaN(pixMin) && pixel<pixMin ) continue;
                        if( !Double.isNaN(pixMax) && pixel>pixMax ) continue;
                     }
                     
                     long npix = min + p.xy2hpx(idx);

                     // Juste pour ?viter d'ins?rer 2x de suite le m?me npix
                     if( npix==oNpix ) continue;
                     
                     moc.add(res,npix>>>divOrder);
                     oNpix=npixFile;
                     nb++;
                  } catch( Exception e ) {
                     e.printStackTrace();
                  }
               }
            }
            
            // On remet imm?diatement au propre le MOC uniquement si on a ins?r?
            // au-moins 1000 cellules (histoire de ne pas exploser la m?moire)
            if( nb>1000 ) moc.checkAndFix();
            
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
      
      // Conversion en ICRS si n?cessaire
      try {
         moc=toReferenceFrame("C");
         frameOrigin=Localisation.ICRS;
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   
   protected boolean waitForPlan() {
      try {
         moc = new HealpixMoc();
         moc.setMinLimitOrder(3);
         moc.setMaxLimitOrder(res);
         moc.setCoordSys("C");
         frameOrigin=Localisation.ICRS;
         moc.setCheckConsistencyFlag(false);
         for( Plan p1 : p ) {
            if( p1.isCatalog() ) addMocFromCatalog(p1,radius);
            else if( p1.isImage() ) addMocFromImage(p1,pixMin,pixMax);
            else if( p1 instanceof PlanBG ) addMocFromPlanBG(p1,res,pixMin,pixMax);
         }
         moc.setCheckConsistencyFlag(true);
      } catch( Exception e ) {
         error=e.getMessage();
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      flagProcessing=false;
      flagOk=true;
      return true;
   }
   

      
}

