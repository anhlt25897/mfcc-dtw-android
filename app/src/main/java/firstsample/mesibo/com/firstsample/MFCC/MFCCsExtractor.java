package firstsample.mesibo.com.firstsample.MFCC;

import java.util.ArrayList;
import java.util.List;

public class MFCCsExtractor {
    //region VARS
    private static MFCCsExtractor mInstance;

    private static final int mfccNoFeatures = 13;
    private static final int mNoMelsFilter = 20;            //number of mel-filter
    private static final double mFreqMin = 0.0;             //min mel-freq min for human ear
    private final FFT fft = new FFT();
    private int mFrameSize = 1024;
    private int mFrameShift = 512;

    private double mSampleRate;
    private double mFreqMax;
    private double[] mData;

    private List<MFCCVector> mMFCCs;
    //endregion

    // region CONSTRUCTOR
    private MFCCsExtractor() {
    }

    public static MFCCsExtractor newInstance(double[] data, double sampleRate, int chanel) {
        mInstance = new MFCCsExtractor();
        mInstance.mData = data;
        mInstance.mSampleRate = sampleRate;
        mInstance.mFrameSize = mInstance.mFrameSize * chanel;
        mInstance.mFrameShift = mInstance.mFrameSize / 2;
        mInstance.mFreqMax = sampleRate / 2.0;
        return mInstance;
    }
    //endregion

    //region PUBLIC UTILS
    public MFCCsExtractor process() {
        this.mMFCCs = dctToMfcc(preEmphasis(mData));
        return mInstance;
    }

    public List<MFCCVector> getMFCC() {
        return mMFCCs;
    }
    //endregion

    //region PRIVATE UTILS

    //De-Noise
    private double[] preEmphasis(double[] inputs) {
        for (int i = 1; i < inputs.length - 1; i++) {
            inputs[i] -= 0.97 * inputs[i - 1];
        }
        return inputs;
    }

    //DCT to mMFCCs, librosa
    private List<MFCCVector> dctToMfcc(double[] y) {
        final double[][] spectrogram = powerToDb(melSpectrogram(y));
        final double[][] dctBasis = dctFilter(mfccNoFeatures, mNoMelsFilter);
        List<MFCCVector> mfccSpectro = new ArrayList<>();

        for (int i = 0; i < mfccNoFeatures; i++) {
            for (int j = 0; j < spectrogram[0].length; j++) {
                if (j >= mfccSpectro.size()) {
                    mfccSpectro.add(new MFCCVector());
                }
                for (int k = 0; k < spectrogram.length; k++) {
                    if (i == 0) {
                        mfccSpectro.get(j).energy += dctBasis[i][k] * spectrogram[k][j];
                    } else {
                        mfccSpectro.get(j).props[i - 1] += dctBasis[i][k] * spectrogram[k][j];
                    }
                }
            }
        }
        return mfccSpectro;
    }

    //mel spectrogram, librosa
    private double[][] melSpectrogram(double[] y) {
        double[][] melBasis = melFilter();
        double[][] spectro = stftMagSpec(y);
        double[][] melS = new double[melBasis.length][spectro[0].length];
        for (int i = 0; i < melBasis.length; i++) {
            for (int j = 0; j < spectro[0].length; j++) {
                for (int k = 0; k < melBasis[0].length; k++) {
                    melS[i][j] += melBasis[i][k] * spectro[k][j];
                }
            }
        }
        return melS;
    }

    //stft, librosa
    private double[][] stftMagSpec(double[] y) {
        //Short-time Fourier transform (STFT)
        final double[] fftwin = getWindow();
        //pad y with reflect mode so it's centered. This reflect padding implementation is
        // not perfect but works for this demo.
        double[] ypad = new double[mFrameSize + y.length];
        for (int i = 0; i < mFrameSize / 2; i++) {
            ypad[(mFrameSize / 2) - i - 1] = y[i + 1];
            ypad[(mFrameSize / 2) + y.length + i] = y[y.length - 2 - i];
        }
        System.arraycopy(y, 0, ypad, mFrameSize / 2, y.length);

        final double[][] frame = breakFrame(ypad);
        double[][] fftmagSpec = new double[1 + mFrameSize / 2][frame[0].length];
        double[] fftFrame = new double[mFrameSize];
        for (int k = 0; k < frame[0].length; k++) {
            for (int l = 0; l < mFrameSize; l++) {
                fftFrame[l] = fftwin[l] * frame[l][k];
            }
            double[] magSpec = magSpectrogram(fftFrame);
            for (int i = 0; i < 1 + mFrameSize / 2; i++) {
                fftmagSpec[i][k] = magSpec[i];
            }
        }
        return fftmagSpec;
    }

    private double[] magSpectrogram(double[] frame) {
        double[] magSpec = new double[frame.length];
        fft.process(frame);
        for (int m = 0; m < frame.length; m++) {
            magSpec[m] = fft.real[m] * fft.real[m] + fft.imag[m] * fft.imag[m];
        }
        return magSpec;
    }

    //get hamming window
    private double[] getWindow() {
        //Return a Hann window for even mFrameSize.
        //The Hann window is a taper formed by using a raised cosine or sine-squared
        //with ends that touch zero.
        double[] win = new double[mFrameSize];
        for (int i = 0; i < mFrameSize; i++) {
            win[i] = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / mFrameSize);
        }
        return win;
    }

    //break data to frame
    private double[][] breakFrame(double[] data) {
        final int n_frames = 1 + (data.length - mFrameSize) / mFrameShift;
        double[][] winFrames = new double[mFrameSize][n_frames];
        for (int i = 0; i < mFrameSize; i++) {
            for (int j = 0; j < n_frames; j++) {
                winFrames[i][j] = data[j * mFrameShift + i];
            }
        }
        return winFrames;
    }

    //power to db, librosa
    private double[][] powerToDb(double[][] melS) {
        //  Convert a power spectrogram (amplitude squared) to decibel (dB) units
        //  This computes the scaling ``10 * log10(S / ref)`` in a numerically
        //  stable way.
        double[][] log_spec = new double[melS.length][melS[0].length];
        double maxValue = -100;
        for (int i = 0; i < melS.length; i++) {
            for (int j = 0; j < melS[0].length; j++) {
                double magnitude = Math.abs(melS[i][j]);
                if (magnitude > 1e-10) {
                    log_spec[i][j] = 10.0 * log10(magnitude);
                } else {
                    log_spec[i][j] = 10.0 * (-10);
                }
                if (log_spec[i][j] > maxValue) {
                    maxValue = log_spec[i][j];
                }
            }
        }

        //set top_db to 80.0
        for (int i = 0; i < melS.length; i++) {
            for (int j = 0; j < melS[0].length; j++) {
                if (log_spec[i][j] < maxValue - 80.0) {
                    log_spec[i][j] = maxValue - 80.0;
                }
            }
        }
        //ref is disabled, maybe later.
        return log_spec;
    }

    //dct, librosa
    private double[][] dctFilter(int n_filters, int n_input) {
        //Discrete cosine transform (DCT type-III) basis.
        double[][] basis = new double[n_filters][n_input];
        double[] samples = new double[n_input];
        for (int i = 0; i < n_input; i++) {
            samples[i] = (1 + 2 * i) * Math.PI / (2.0 * (n_input));
        }
        for (int j = 0; j < n_input; j++) {
            basis[0][j] = 1.0 / Math.sqrt(n_input);
        }
        for (int i = 1; i < n_filters; i++) {
            for (int j = 0; j < n_input; j++) {
                basis[i][j] = Math.cos(i * samples[j]) * Math.sqrt(2.0 / (n_input));
            }
        }
        return basis;
    }

    //mel, librosa
    private double[][] melFilter() {
        //Create a Filterbank matrix to combine FFT bins into Mel-frequency bins.
        // Center freqs of each FFT bin
        final double[] fftFreqs = fftFreq();
        //'Center freqs' of mel bands - uniformly spaced between limits
        final double[] melF = melFreq(mNoMelsFilter + 2);

        double[] fdiff = new double[melF.length - 1];
        for (int i = 0; i < melF.length - 1; i++) {
            fdiff[i] = melF[i + 1] - melF[i];
        }

        double[][] ramps = new double[melF.length][fftFreqs.length];
        for (int i = 0; i < melF.length; i++) {
            for (int j = 0; j < fftFreqs.length; j++) {
                ramps[i][j] = melF[i] - fftFreqs[j];
            }
        }

        double[][] weights = new double[mNoMelsFilter][1 + mFrameSize / 2];
        for (int i = 0; i < mNoMelsFilter; i++) {
            for (int j = 0; j < fftFreqs.length; j++) {
                double lowerF = -ramps[i][j] / fdiff[i];
                double upperF = ramps[i + 2][j] / fdiff[i + 1];
                if (lowerF > upperF && upperF > 0) {
                    weights[i][j] = upperF;
                } else if (lowerF > upperF && upperF < 0) {
                    weights[i][j] = 0;
                } else if (lowerF < upperF && lowerF > 0) {
                    weights[i][j] = lowerF;
                } else if (lowerF < upperF && lowerF < 0) {
                    weights[i][j] = 0;
                }
            }
        }

        double enorm[] = new double[mNoMelsFilter];
        for (int i = 0; i < mNoMelsFilter; i++) {
            enorm[i] = 2.0 / (melF[i + 2] - melF[i]);
            for (int j = 0; j < fftFreqs.length; j++) {
                weights[i][j] *= enorm[i];
            }
        }
        return weights;
        //need to check if there's an empty channel somewhere
    }

    //fft frequencies, librosa
    private double[] fftFreq() {
        //Alternative implementation of np.mFFT.fftfreqs
        double[] freqs = new double[1 + mFrameSize / 2];
        for (int i = 0; i < 1 + mFrameSize / 2; i++) {
            freqs[i] = 0 + (mSampleRate / 2) / (mFrameSize / 2) * i;
        }
        return freqs;
    }

    //mel frequencies, librosa
    private double[] melFreq(int numMels) {
        //'Center freqs' of mel bands - uniformly spaced between limits
        double[] LowFFreq = new double[1];
        double[] HighFFreq = new double[1];
        LowFFreq[0] = mFreqMin;
        HighFFreq[0] = mFreqMax;
        final double[] melFLow = freqToMel(LowFFreq);
        final double[] melFHigh = freqToMel(HighFFreq);
        double[] mels = new double[numMels];
        for (int i = 0; i < numMels; i++) {
            mels[i] = melFLow[0] + (melFHigh[0] - melFLow[0]) / (numMels - 1) * i;
        }
        return melToFreq(mels);
    }

    //mel to hz, htk, librosa
    private double[] melToFreq(double[] mels) {
        double[] freqs = new double[mels.length];
        for (int i = 0; i < mels.length; i++) {
            freqs[i] = 700.0 * (Math.pow(10, mels[i] / 2595.0) - 1.0);
        }
        return freqs;
    }

    // hz to mel, htk, librosa
    private double[] freqToMel(double[] freqs) {
        double[] mels = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            mels[i] = 2595.0 * log10(1.0 + freqs[i] / 700.0);
        }
        return mels;
    }

    // log10
    private double log10(double value) {
        return Math.log(value) / Math.log(10);
    }
    //endregion
}
