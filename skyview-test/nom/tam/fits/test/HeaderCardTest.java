package nom.tam.fits.test;
/*
 * This code is part of the Java FITS library developed 1996-2012 by T.A. McGlynn (NASA/GSFC)
 * The code is available in the public domain and may be copied, modified and used
 * by anyone in any fashion for any purpose without restriction. 
 * 
 * No warranty regarding correctness or performance of this code is given or implied.
 * Users may contact the author if they have questions or concerns.
 * 
 * The author would like to thank many who have contributed suggestions, 
 * enhancements and bug fixes including:
 * David Glowacki, R.J. Mathar, Laurent Michel, Guillaume Belanger,
 * Laurent Bourges, Rose Early, Jorgo Baker, A. Kovacs, V. Forchi, J.C. Segovia,
 * Booth Hartley and Jason Weiss.  
 * I apologize to any contributors whose names may have been inadvertently omitted.
 * 
 *      Tom McGlynn
 */

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import junit.framework.JUnit4TestAdapter;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.FitsFactory;

public class HeaderCardTest {

    @Test
    public void test1() throws Exception {

        HeaderCard p;
        p = new HeaderCard("SIMPLE  =                     T");

        assertEquals("t1", "SIMPLE", p.getKey());
        assertEquals("t2", "T", p.getValue());
        assertNull("t3", p.getComment());

        p = new HeaderCard("VALUE   =                   123");
        assertEquals("t4", "VALUE", p.getKey());
        assertEquals("t5", "123", p.getValue());
        assertNull("t3", p.getComment());

        p = new HeaderCard("VALUE   =    1.23698789798798E23 / Comment ");
        assertEquals("t6", "VALUE", p.getKey());
        assertEquals("t7", "1.23698789798798E23", p.getValue());
        assertEquals("t8", "Comment", p.getComment());

        String lng = "111111111111111111111111111111111111111111111111111111111111111111111111";
        p = new HeaderCard("COMMENT " + lng);
        assertEquals("t9", "COMMENT", p.getKey());
        assertNull("t10", p.getValue());
        assertEquals("t11", lng, p.getComment());

        boolean thrown = false;
        try {
            //
            p = new HeaderCard("VALUE   = '   ");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("t12", true, thrown);


        p = new HeaderCard("COMMENT " + lng + lng);
        assertEquals("t13", lng, p.getComment());

        HeaderCard z = new HeaderCard("TTTT", 1.234567891234567891234567e101, "a comment");
        assertTrue("t14", z.toString().indexOf("E") > 0);
    }

    @Test
    public void test3() throws Exception {

        HeaderCard p = new HeaderCard("KEY", "VALUE", "COMMENT");
        assertEquals("x1",
                "KEY     = 'VALUE   '           / COMMENT                                        ",
                p.toString());

        p = new HeaderCard("KEY", 123, "COMMENT");
        assertEquals("x2",
                "KEY     =                  123 / COMMENT                                        ",
                p.toString());
        p = new HeaderCard("KEY", 1.23, "COMMENT");
        assertEquals("x3",
                "KEY     =                 1.23 / COMMENT                                        ",
                p.toString());
        p = new HeaderCard("KEY", true, "COMMENT");
        assertEquals("x4",
                "KEY     =                    T / COMMENT                                        ",
                p.toString());


        boolean thrown = false;
        try {
            p = new HeaderCard("LONGKEYWORD", 123, "COMMENT");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("x5", true, thrown);

        thrown = false;
        String lng = "00000000001111111111222222222233333333334444444444555555555566666666667777777777";
        try {
            p = new HeaderCard("KEY", lng, "COMMENT");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("x6", true, thrown);


        // Only trailing spaces are stripped.
        p = new HeaderCard("STRING", "VALUE", null);
        assertEquals("x6", "VALUE", p.getValue());

        p = new HeaderCard("STRING", "VALUE ", null);
        assertEquals("x7", "VALUE", p.getValue());

        p = new HeaderCard("STRING", " VALUE", null);
        assertEquals("x8", " VALUE", p.getValue());

        p = new HeaderCard("STRING", " VALUE ", null);
        assertEquals("x9", " VALUE", p.getValue());

        p = new HeaderCard("QUOTES", "ABC'DEF", null);
        assertEquals("x10", "ABC'DEF", p.getValue());
        assertEquals("x10b", p.toString().indexOf("''") > 0, true);

        p = new HeaderCard("QUOTES", "ABC''DEF", null);
        assertEquals("x11", "ABC''DEF", p.getValue());
        assertEquals("x10b", p.toString().indexOf("''''") > 0, true);
    }

    @Test
    public void testHierarch() throws Exception {

        HeaderCard hc;
        String key = "HIERARCH.TEST1.TEST2.INT";
        boolean thrown = false;
        try {
            hc = new HeaderCard(key, 123, "Comment");
        } catch (Exception e) {
            thrown = true;
        }
        assertEquals("h1", true, thrown);

        String card = "HIERARCH TEST1 TEST2 INT=           123 / Comment                               ";
        hc = new HeaderCard(card);
        assertEquals("h2", "HIERARCH", hc.getKey());
        assertNull("h3", hc.getValue());
        assertEquals("h4", "TEST1 TEST2 INT=           123 / Comment", hc.getComment());

        FitsFactory.setUseHierarch(true);

        hc = new HeaderCard(key, 123, "Comment");

        assertEquals("h5", key, hc.getKey());
        assertEquals("h6", "123", hc.getValue());
        assertEquals("h7", "Comment", hc.getComment());

        hc = new HeaderCard(card);
        assertEquals("h8", key, hc.getKey());
        assertEquals("h9", "123", hc.getValue());
        assertEquals("h10", "Comment", hc.getComment());
    }

    @Test
    public void testLongDoubles() throws Exception {
        // Check to see if we make long double values
        // fit in the recommended space.
        HeaderCard hc = new HeaderCard("TEST", -1.234567890123456789e-123, "dummy");
        String val = hc.getValue();
        assertEquals("tld1", val.length(), 20);
    }
}
