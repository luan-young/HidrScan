package com.example.luan.hidrscan.utils;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 * Created by luan on 08/04/18.
 */

public class TensorFlowDigitClassifier {

    // Config values.
    private String inputName;
    private String outputName;
    private int inputSizeY;
    private int inputSizeX;

    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private float[] outputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    private boolean runStats = false;

    private TensorFlowDigitClassifier() {
    }

    public static TensorFlowDigitClassifier create(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSizeY,
            int inputSizeX,
            String inputName,
            String outputName)
            throws IOException {
        TensorFlowDigitClassifier c = new TensorFlowDigitClassifier();
        c.inputName = inputName;
        c.outputName = outputName;

        // Read the label names into memory.
        // TODO(andrewharp): make this handle non-assets.
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
//        Log.i(TAG, "Reading labels from: " + actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
        String line;
        while ((line = br.readLine()) != null) {
            c.labels.add(line);
        }
        br.close();

        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
        int numClasses =
                (int) c.inferenceInterface.graph().operation(outputName).output(0).shape().size(0);
//        Log.i(TAG, "Read " + c.labels.size() + " labels, output layer size is " + numClasses);

        // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
        // the placeholder node for input in the graphdef typically used does not specify a shape, so it
        // must be passed in as a parameter.
        c.inputSizeY = inputSizeY;
        c.inputSizeX = inputSizeX;

        // Pre-allocate buffers.
        c.outputNames = new String[]{outputName};
//        c.outputNames = new String[]{
//                "output_node0",
//                "output_node1",
//                "output_node2",
//                "output_node3",
//                "output_node4",
//                "output_node5",
//                "output_node6",
//                "output_node7",
//                "output_node8",
//                "output_node9"
//        };
        c.outputs = new float[numClasses];

        return c;
    }

    public String recognizeImage(final float[] pixels) {
        // Log this method so that it can be analyzed with systrace.
//        TraceCompat.beginSection("recognizeImage");

        // Copy the input data into TensorFlow.
//        TraceCompat.beginSection("feed");
//        inferenceInterface.feed(inputName, pixels, new long[]{inputSizeY * inputSizeX});
        inferenceInterface.feed(inputName, pixels, 1, inputSizeY, inputSizeX, 1);
//        TraceCompat.endSection();

        // Run the inference call.
//        TraceCompat.beginSection("run");
        inferenceInterface.run(outputNames, runStats);
//        TraceCompat.endSection();

        // Copy the output Tensor back into the output array.
//        TraceCompat.beginSection("fetch");
        inferenceInterface.fetch(outputName, outputs);
//        TraceCompat.endSection();

        // Find the best classifications.
        int bestIdx = 0;
        float best = outputs[bestIdx];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > best) {
                bestIdx = i;
                best = outputs[i];
            }
        }

        return labels.size() > bestIdx ? labels.get(bestIdx) : "unknown";
    }


}
