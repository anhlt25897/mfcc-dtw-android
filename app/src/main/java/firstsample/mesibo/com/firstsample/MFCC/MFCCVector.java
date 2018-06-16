package firstsample.mesibo.com.firstsample.MFCC;

public class MFCCVector {
    public double[] props;
    public double energy;

    public MFCCVector() {
        this.props = new double[12];
        this.energy = 0.0;
    }

    public MFCCVector(double[] props, double energy) {
        this.props = props;
        this.energy = energy;
    }
}
