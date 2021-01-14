package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;

public class Job {
    private static final String MODEL_URL = "https://dl.dropboxusercontent.com/s/tqnp49apphpzb40/dataTraining.zip?dl=0";
    // private static final String MODEL_URL =
    // "http://localhost:7979/static/model.zip"; // if you want to download model
    // faster, upload it in local nginx or local filesystem
    private static ComputationGraph computationGraph;
    private static String MODEL_ZIP_FILE_PATH = "/cat-and-dog-recognizer/model.zip";
    private static double THRESHOLD = 0.5;

    private static void writeFile(String outputFilePath, byte[] body) throws IOException {
        String message = String.format("writing file >\npath: %s", outputFilePath);
        System.out.println(message);
        File outputFile = new File(outputFilePath);
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(body);
        fos.flush();
        fos.close();
    }

    private static void writeOutput(HashMap<String, Integer> detectionResults, String outputFilePath)
            throws IOException {
        FileWriter csvWriter = new FileWriter(outputFilePath);
        for (Map.Entry<String, Integer> entry : detectionResults.entrySet()) {
            String imageURL = entry.getKey();
            Integer res = entry.getValue();
            csvWriter.append(imageURL);
            csvWriter.append("#");
            csvWriter.append(String.valueOf(res));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }

    private static byte[] download(String downloadUrl) throws IOException {
        long s = System.currentTimeMillis();
        InputStream is = new URL(downloadUrl).openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024 * 1024 * 10];
        int current;
        while ((current = bis.read(data, 0, data.length)) != -1) {
            baos.write(data, 0, current);
        }
        long e = System.currentTimeMillis();
        String m = String.format("download status >\nfile: %s\ntime: %s milliseconds", downloadUrl, (e - s));
        System.out.println(m);
        return baos.toByteArray();
    }

    private static void downloadModelForTheFirstTime(String searchPath) throws IOException {
        File modelFile = new File(searchPath + MODEL_ZIP_FILE_PATH);
        if (!modelFile.exists()) {
            System.out.println("model not found.");
            boolean created = modelFile.getParentFile().mkdir();
            System.out.println("created dir? " + created);
            byte[] modelBytes = download(MODEL_URL);
            writeFile(modelFile.getAbsolutePath(), modelBytes);
        }
    }

    private static ComputationGraph loadModel(String modelPath) throws IOException {
        computationGraph = ModelSerializer.restoreComputationGraph(new File(modelPath));
        return computationGraph;
    }

    private static String getParentPath(String path) {
        String[] arr = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length - 1; i++) {
            sb.append(arr[i]);
            if (i != arr.length - 2)
                sb.append("/");
        }
        return sb.toString();
    }

    private static ArrayList<String> getImageURLs(String inputFileURL) throws IOException {
        URL url = new URL(inputFileURL);
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String row;
        ArrayList<String> urls = new ArrayList<String>();
        while ((row = csvReader.readLine()) != null)
            urls.add(row);
        csvReader.close();
        return urls;
    }

    private static int detectCat(String imageURL) throws IOException {
        byte[] imageBytes = download(imageURL);
        writeFile("image", imageBytes);
        File imageFile = new File("image");
        NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
        INDArray image = loader.asMatrix(new FileInputStream(imageFile));
        DataNormalization scaler = new VGG16ImagePreProcessor();
        scaler.transform(image);
        INDArray output = computationGraph.outputSingle(false, image);
        if (output.getDouble(0) > THRESHOLD)
            return 1;
        else if (output.getDouble(1) > THRESHOLD)
            return 0;
        else
            return -1;
    }

    private static HashMap<String, Integer> detectAll(String inputFileURL) throws IOException {
        ArrayList<String> imagesURLCollection = getImageURLs(inputFileURL);
        HashMap<String, Integer> detectionResults = new HashMap<>();
        for (String imageURL : imagesURLCollection) {
            int res = detectCat(imageURL);
            detectionResults.put(imageURL, res);
        }
        return detectionResults;
    }

    public static void start(String inputFileURL, String outputFilePath, int fraction, int totalFractions)
            throws IOException {
        String outputFileParentPath = getParentPath(outputFilePath);
        System.out.println(inputFileURL);
        System.out.println(outputFilePath);
        System.out.println(fraction);
        System.out.println(totalFractions);
        downloadModelForTheFirstTime(outputFileParentPath);
        computationGraph = loadModel(outputFileParentPath + MODEL_ZIP_FILE_PATH);
        computationGraph.init();
        System.out.println(computationGraph.summary());
        HashMap<String, Integer> detectionResults = detectAll(inputFileURL);
        writeOutput(detectionResults, outputFilePath);
    }

    public static void main(String[] args) throws IOException {
        String inputFileURL = args[0];
        String outputFilePath = args[1];
        int fraction = Integer.parseInt(args[2]);
        int totalFractions = Integer.parseInt(args[3]);
        start(inputFileURL, outputFilePath, fraction, totalFractions);
    }
}