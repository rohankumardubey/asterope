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

/*
 * Created on 12 f?vr. 2004
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

/**
 * @author Thomas Boch [CDS]
 */
public class XMatchResult {
    int idx1, idx2;
    double dist;
    
    XMatchResult(int idx1, int idx2, double dist) {
        this.idx1 = idx1;
        this.idx2 = idx2;
        this.dist = dist;
    }
    
    public String toString() {
        return idx1+"\t"+idx2+"\t"+dist;
    }
}
