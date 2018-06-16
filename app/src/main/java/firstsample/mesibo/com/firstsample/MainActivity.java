package firstsample.mesibo.com.firstsample;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

import firstsample.mesibo.com.firstsample.Comparator.DTW;
import firstsample.mesibo.com.firstsample.MFCC.MFCCsExtractor;
import firstsample.mesibo.com.firstsample.MFCC.MFCCVector;
import firstsample.mesibo.com.firstsample.SignalUtils.WavFile;
import firstsample.mesibo.com.firstsample.SignalUtils.WavFileException;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        }

        File open1 = new File(Environment.getExternalStorageDirectory(), "rainfr.wav");
        File open2 = new File(Environment.getExternalStorageDirectory(), "source.wav");

        long time = System.currentTimeMillis();

        WavFile wavFile1 = null;
        WavFile wavFile2 = null;
        try {
            wavFile1 = WavFile.openWavFile(open1);
            wavFile2 = WavFile.openWavFile(open2);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WavFileException e) {
            e.printStackTrace();
        }

        Log.i("LOGGGGG", "START");

        assert wavFile1 != null;
        assert wavFile2 != null;
        float[] pcmAsFloats1 = new float[(int) wavFile1.getFileSize()];
        float[] pcmAsFloats2 = new float[(int) wavFile2.getFileSize()];

        try {
            wavFile1.readFrames(pcmAsFloats1, (int) (wavFile1.getFileSize() / 2));
            wavFile2.readFrames(pcmAsFloats2, (int) (wavFile2.getFileSize() / 2));

            double[] datas1 = convertFloatsToDoubles(pcmAsFloats1);
            double[] datas2 = convertFloatsToDoubles(pcmAsFloats2);

            //MFCCsExtractor java library.
            MFCCsExtractor converter1 = MFCCsExtractor.newInstance(datas1, wavFile1.getSampleRate(), wavFile1.getNumChannels()).process();
            List<MFCCVector> mfccInput1 = converter1.getMFCC();
            Log.i("LOGGGGG", "DONE 1" + " | LENGTH " + datas1.length + " | TIME: " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            MFCCsExtractor converter2 = MFCCsExtractor.newInstance(datas2, wavFile2.getSampleRate(), wavFile2.getNumChannels()).process();
            List<MFCCVector> mfccInput2 = converter2.getMFCC();
            Log.i("LOGGGGG", "DONE 2" + " | LENGTH " + datas2.length + " | TIME: " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            DTW.Result result = DTW.getInstance().process(mfccInput2, mfccInput1);

            Log.i("LOGGGGG", "DISTANCE: " + result.getDistance() + " | TIME: " + (System.currentTimeMillis() - time) + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WavFileException e) {
            e.printStackTrace();
        }
    }

    public static double[] convertFloatsToDoubles(float[] input) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission() {
        requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
    }

    private static final int READ_EXTERNAL_STORAGE = 13;
}