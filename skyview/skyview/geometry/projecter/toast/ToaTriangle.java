

package skyview.geometry.projecter.toast;

/**
 * 
 * @author tmcglynn
 */
public class ToaTriangle {
    
    public static void main(String[] args) throws Exception {
        
        int level = 1;
        if (args.length  > 0) {
            level = Integer.parseInt(args[0]);
        }
        System.out.println("QUAD");
        printTriangles(new double[]{ 1.,0.,0.},new double[]{0.,  1., 0.}, new double[]{0., 0.,  1.}, 1, level);
        printTriangles(new double[]{ 1.,0.,0.},new double[]{0.,  1., 0.}, new double[]{0., 0., -1.}, 1, level);
        printTriangles(new double[]{ 1.,0.,0.},new double[]{0., -1., 0.}, new double[]{0., 0.,  1.}, 1, level);
        printTriangles(new double[]{ 1.,0.,0.},new double[]{0., -1., 0.}, new double[]{0., 0., -1.}, 1, level);
        printTriangles(new double[]{-1.,0.,0.},new double[]{0.,  1., 0.}, new double[]{0., 0.,  1.}, 1, level);
        printTriangles(new double[]{-1.,0.,0.},new double[]{0.,  1., 0.}, new double[]{0., 0., -1.}, 1, level);
        printTriangles(new double[]{-1.,0.,0.},new double[]{0., -1., 0.}, new double[]{0., 0.,  1.}, 1, level);
        printTriangles(new double[]{-1.,0.,0.},new double[]{0., -1., 0.}, new double[]{0., 0., -1.}, 1, level);
    }
    
    static void printTriangles(double[] x, double[] y, double[] z, int curr, int des) {
        if (curr == des) {
            System.out.printf("%f %f %f  ", x[0],x[1],x[2]);
            // Print out y (arbitrary) twice since the program wants quads
            System.out.printf("%f %f %f  ", y[0],y[1],y[2]);
            System.out.printf("%f %f %f  ", y[0],y[1],y[2]);
            System.out.printf("%f %f %f\n", z[0],z[1],z[2]);
        } else {
            double[] xy = mid(x,y);
            double[] xz = mid(x,z);
            double[] yz = mid(y,z);
            printTriangles(x, xy, xz, curr+1, des);
            printTriangles(y, yz, xy, curr+1, des);
            printTriangles(z, xz, yz, curr+1, des);
            printTriangles(xy, yz, xz, curr+1, des);                        
        }
    }
    static double[] mid(double[] x, double[] y) {
        double[] a = x.clone();
        a[0] += y[0];
        a[1] += y[1];
        a[2] += y[2];
        double r = Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
        a[0] /= r;
        a[1] /= r;
        a[2] /= r;
        return a;        
    }
}