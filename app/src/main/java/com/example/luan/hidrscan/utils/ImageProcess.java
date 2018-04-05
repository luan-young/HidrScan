package com.example.luan.hidrscan.utils;

/**
 * Created by luan on 10/03/18.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.luan.hidrscan.R;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.utils.Converters;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageProcess {

    private Context mContext;
    private Mat mOrigImage;
    private Mat mCurrentImage;
    private Mat mTransformedImage;
    private int mState;
    private boolean mFinished = false;
    List<MatOfPoint> mContours;
    Mat mHierarchy;
    List<Double> mContoursAreas;
    MatOfPoint mTemplateB;
    MatOfPoint mTemplateL;
    MatOfPoint mFoundTemplateB;
    MatOfPoint mFoundTemplateL;
    OrderedCorners mCornersPoints;
    List<Point> mTemplateCornersXY;

    private final double[] arrSquaresParamsX = {125.73713527, 81.2846127, 69.85396404}; // sz, offset, disp
    private final double[] arrSquaresParamsY = {277.60652423, 40.98888277}; // sz, offset

    private final int mParamBlurK = 13;
    private final int mParamThreshLevel = 91;
    private final int mParamThreshC = 5;
    private final int mParamCannyThresh1 = 100;
    private final int mParamCannyThresh2 = 200;

    public ImageProcess(Context context) {

        this.mContext = context;

//        mCurrentImage = new Mat();
//        mOrigImage = new Mat();
//        mHierarchy = new Mat();
//        mContours = new ArrayList<MatOfPoint>();
    }

    public void StartNewProcessing(Bitmap imageBitmap) {
        mState = 1;
        mFinished = false;
        if (mCurrentImage == null){
            mCurrentImage = new Mat();
        }
        if (mOrigImage == null){
            mOrigImage = new Mat();
        }
        if (mTransformedImage == null){
            mTransformedImage = new Mat();
        }

        if (mHierarchy == null){
            mHierarchy = new Mat();
        }
        if (mContours == null){
            mContours = new ArrayList<MatOfPoint>();
        }
        if (mContoursAreas == null) {
            mContoursAreas = new ArrayList<Double>();
        }
        if (mCornersPoints == null) {
            mCornersPoints = new OrderedCorners();
        }
        Utils.bitmapToMat(imageBitmap, mOrigImage);
        mOrigImage = this.ResizeImageToTemplateSize(mOrigImage);
//        Utils.bitmapToMat(imageBitmap, mCurrentImage);
        mCurrentImage = mOrigImage.clone();
    }

    public Bitmap GetCurrImage() {
        if (mState == 0) {
            return Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888);
        }
        Bitmap imageBitmap = Bitmap.createBitmap(mCurrentImage.width(), mCurrentImage.height(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mCurrentImage, imageBitmap);
        return imageBitmap;
    }

    public boolean HasFinished() {
        return mFinished;
    }

    public void ProcessNextStep() {

        if (mState == 1) {
            mState++;

            // Transform to graycolor
            Imgproc.cvtColor(mCurrentImage, mCurrentImage, Imgproc.COLOR_RGB2GRAY);
        }
        else if (mState == 2) {
            mState++;

            // Blur
            org.opencv.core.Size sz = new Size(mParamBlurK, mParamBlurK);
            Imgproc.GaussianBlur(mCurrentImage, mCurrentImage, sz,0, 0);
        }
        else if (mState == 3) {
            mState++;

            // Adaptive Threshold
            Imgproc.adaptiveThreshold(mCurrentImage, mCurrentImage,255,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, mParamThreshLevel,
                    mParamThreshC);
        }
        else if (mState == 4) {
            mState++;

            // Canny
            Imgproc.Canny(mCurrentImage, mCurrentImage, mParamCannyThresh1, mParamCannyThresh2);
        }
        else if (mState == 5) {
            mState++;

            // Countrs search
            mContours.clear();
            Imgproc.findContours(mCurrentImage, mContours, mHierarchy, Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);
            mHierarchy.release();

            // Contours drawing
            mCurrentImage = mOrigImage.clone();
            Imgproc.drawContours(mCurrentImage, mContours, -1, new Scalar(0, 255, 0));
        }
        else if (mState == 6) {
            mState++;

            FilterContoursByArea();

            // Contours drawing
            mCurrentImage = mOrigImage.clone();
            Imgproc.drawContours(mCurrentImage, mContours, -1, new Scalar(0, 255, 0));
        }
        else if (mState == 7) {
            mState++;

            FindTemplates();

            // Contours drawing
            mCurrentImage = mOrigImage.clone();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>(2);
            contours.add(mFoundTemplateB);
            contours.add(mFoundTemplateL);
            Imgproc.drawContours(mCurrentImage, contours, -1, new Scalar(0, 255, 0));
        }
        else if (mState == 8) {
            mState++;

            FindCornersOfTemplates();

            // Corners drawing
            mCurrentImage = mOrigImage.clone();
            Imgproc.circle(mCurrentImage, mCornersPoints.pBT, 3, new Scalar(0, 255, 0), -1);
            Imgproc.circle(mCurrentImage, mCornersPoints.pBB, 3, new Scalar(0, 255, 0), -1);
            Imgproc.circle(mCurrentImage, mCornersPoints.pLT, 3, new Scalar(0, 255, 0), -1);
            Imgproc.circle(mCurrentImage, mCornersPoints.pLB, 3, new Scalar(0, 255, 0), -1);
        }
        else if (mState == 9) {
            mState++;

            TransformImageToAlignTemplates();

            mCurrentImage = mTransformedImage.clone();
        }
        else if (mState == 10) {
            mState++;

            mFinished = true;

            MakeDigitSquares();

            mCurrentImage = mTransformedImage.clone();
        }

    }

    private void MakeDigitSquares() {

        final double xsz = arrSquaresParamsX[0];
        final double xoff = arrSquaresParamsX[1];
        final double xdisp = arrSquaresParamsX[2];
        final double ysz = arrSquaresParamsY[0];
        final double yoff = arrSquaresParamsY[1];

        Point ptUL = new Point(mTemplateCornersXY.get(0).x + xoff, mTemplateCornersXY.get(0).y + yoff);
        Point ptBR = new Point(ptUL.x + xsz, ptUL.y + ysz);

        for (int i = 0; i < 6; i++) {
            Imgproc.rectangle(mTransformedImage, ptUL, ptBR, new Scalar(0, 255, 0), 3);
            ptUL.x = ptBR.x + xdisp;
            ptBR.x = ptUL.x + xsz;
        }
    }

    private void TransformImageToAlignTemplates() {

        Mat templatePointsToTransform = new Mat();
        Mat foundTemplatePointsToTransform = new Mat();

        if (mTemplateCornersXY == null) {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.templates_corners_xy);
            ReadCsvTemplate csvFile = new ReadCsvTemplate();
            mTemplateCornersXY = csvFile.read(inputStream);
        }
//        mTemplatePointsToTransform.fromList(mTemplateCornersXY);
        templatePointsToTransform = Converters.vector_Point_to_Mat(mTemplateCornersXY);

        List<Point> templatePoints = new ArrayList<Point>(4);
        templatePoints.clear();
        templatePoints.add(mCornersPoints.pBT);
        templatePoints.add(mCornersPoints.pBB);
        templatePoints.add(mCornersPoints.pLB);
        templatePoints.add(mCornersPoints.pLT);
        //mFoundTemplatePointsToTransform.fromList(templatePoints);
        foundTemplatePointsToTransform = Converters.vector_Point_to_Mat(templatePoints);

        templatePointsToTransform.convertTo(templatePointsToTransform, CvType.CV_32FC2);
        foundTemplatePointsToTransform.convertTo(foundTemplatePointsToTransform, CvType.CV_32FC2);

        Mat tranformMat = Imgproc.getPerspectiveTransform(foundTemplatePointsToTransform,
                templatePointsToTransform);

        Imgproc.warpPerspective(mOrigImage, mTransformedImage, tranformMat, mOrigImage.size());
    }

    private void FindCornersOfTemplates() {

        MatOfPoint2f vertices = ImageProcessUtil.Get6VerticesFromTemplate(mFoundTemplateB);
        Point[] cornersB = ImageProcessUtil.Get2MeanPointsOf2ClosestPairs(vertices);

        vertices = ImageProcessUtil.Get6VerticesFromTemplate(mFoundTemplateL);
        Point[] cornersL = ImageProcessUtil.Get2MeanPointsOf2ClosestPairs(vertices);

        OrderedCorners corners = ImageProcessUtil.GetOrderedCorners(cornersB, cornersL);

        LineEquation lineEqTop = ImageProcessUtil.FindHorizontalLineSeparating2Sets(mFoundTemplateB,
                mFoundTemplateL, corners.pBT, corners.pLT);
        LineEquation lineEqBotton = ImageProcessUtil.FindHorizontalLineSeparating2Sets(mFoundTemplateB,
                mFoundTemplateL, corners.pBB, corners.pLB);
        LineEquation lineEqLeft = ImageProcessUtil.FindVerticalLineSeparating2Sets(mFoundTemplateB,
                mFoundTemplateL, corners.pBT, corners.pBB);
        LineEquation lineEqRight = ImageProcessUtil.FindVerticalLineSeparating2Sets(mFoundTemplateB,
                mFoundTemplateL, corners.pLT, corners.pLB);

        mCornersPoints.pBT = ImageProcessUtil.FindLinesIntersection(lineEqTop, lineEqLeft);
        mCornersPoints.pBB = ImageProcessUtil.FindLinesIntersection(lineEqBotton, lineEqLeft);
        mCornersPoints.pLT = ImageProcessUtil.FindLinesIntersection(lineEqTop, lineEqRight);
        mCornersPoints.pLB = ImageProcessUtil.FindLinesIntersection(lineEqBotton, lineEqRight);
    }

    private void FindTemplates() {

        if ((mTemplateB == null) || (mTemplateL == null)) {
            mTemplateB = new MatOfPoint();
            mTemplateL = new MatOfPoint();

            InputStream inputStream = mContext.getResources().openRawResource(R.raw.template_big);
            ReadCsvTemplate csvFile = new ReadCsvTemplate();
            List<Point> contourPoints = csvFile.read(inputStream);
            mTemplateB.fromList(contourPoints);

            inputStream = mContext.getResources().openRawResource(R.raw.template_little);
            contourPoints = csvFile.read(inputStream);
            mTemplateL.fromList(contourPoints);
        }

        /**
         * Para encontrar quais dos contornos são os templates B e L, uma série de critérios é
         * avaliada:
         * - Match do contorno com o template B
         * - Match do contorno com o template L
         * - O par de contornos que tem a relação de área mais próxima da relação de área entre B e L
         * - O par de contornos que tem a relação de perímetro mais próxima da relação de perímetro entre B e L
         * Ao final, os par de contorno que melhor atende a esses critérios é selecionado com B e L
         */

        /**
         * Primeiro, calcula a nota de match de cada contorno com os templates B e L.
         * Dá uma nota para cada contorno de acordo com o match com os templates B e L. O array que
         * tiver o melhor match terá nota máxima (= total de contornos) e o pior match terá nota
         * mínima (= 1). Então, ao final nós teremos dois arrays com as notas de cada contorno,
         * como por exemplo, se os matches com B forem [1, 0.1, 2, 0.5, 1.5], as notas com B serão
         * [3, 5, 1, 4, 2]
         */

        // Calcula o match de cada contorno com os templates B e L
        final double[] arrContoursMatchesB = new double[mContours.size()];
        final double[] arrContoursMatchesL = new double[mContours.size()];
        Integer[] arrContoursIdx = new Integer[mContours.size()];
        for (int i = 0; i < mContours.size(); i++) {
            arrContoursMatchesB[i] = Imgproc.matchShapes(mContours.get(i), mTemplateB,
                    Imgproc.CONTOURS_MATCH_I3, 0);
            arrContoursMatchesL[i] = Imgproc.matchShapes(mContours.get(i), mTemplateL,
                    Imgproc.CONTOURS_MATCH_I3, 0);
            arrContoursIdx[i] = i;
        }

        // Ordena os contornos de acordo com o melhor match. Mas, para facilitar as coisas,
        // a ordem é guardada através dos idx de cada contorno
        Integer[] arrContoursIdxOrderedByMatchB = arrContoursIdx.clone();
        Integer[] arrContoursIdxOrderedByMatchL = arrContoursIdx.clone();
        Arrays.sort(arrContoursIdxOrderedByMatchB, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(arrContoursMatchesB[o1], arrContoursMatchesB[o2]);
            }
        });
        Arrays.sort(arrContoursIdxOrderedByMatchL, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(arrContoursMatchesL[o1], arrContoursMatchesL[o2]);
            }
        });

        // A nota de cada contorno é obtida pela posição que seu idx ficou no array ordenado de matches
        int[] arrContoursGradesMatchB = new int[mContours.size()];
        int[] arrContoursGradesMatchL = new int[mContours.size()];
        for (int i = 0; i < mContours.size(); i++) {
            int grade = mContours.size() - i;
            arrContoursGradesMatchB[arrContoursIdxOrderedByMatchB[i]] = grade;
            arrContoursGradesMatchL[arrContoursIdxOrderedByMatchL[i]] = grade;
        }

        /**
         * Calcula uma nota de relação de área e uma de perímetro para cada par possível de contornos.
         * Ou seja, tem que gerar todas as relações possíveis, [(0, 1), (0, 2), ..., (1, 0), (1, 2),...],
         * calcular as relações de área e perímetro para cada combinação e atribuir uma nota de
         * acordo com qual combinação tiver as relações mais próximas das relações entre os templates B e L.
         */

        // Gera todas as 2-permutações possíveis dos mContours.size() índices
        List<Tuple<Integer, Integer>> listContoursIdxPerms = TupleUtil.Generate2Permutations(
                mContours.size());

        // Calcula relações dos templates e calcula perimetro dos contornos (área já tem calculado)
        double templatesAreaRel = Imgproc.contourArea(mTemplateB) / Imgproc.contourArea(mTemplateL);
        MatOfPoint2f tpB = new MatOfPoint2f();
        MatOfPoint2f tpL = new MatOfPoint2f();
        mTemplateB.convertTo(tpB, CvType.CV_32F);
        mTemplateL.convertTo(tpL, CvType.CV_32F);
        double templatesPerimRel = Imgproc.arcLength(tpB, true) /
                Imgproc.arcLength(tpL, true);
        List<Double> contoursPerims = new ArrayList<Double>(mContours.size());
        for (MatOfPoint c : mContours) {
            c.convertTo(tpB, CvType.CV_32F);
            contoursPerims.add(Imgproc.arcLength(tpB, true));
        }

        // Calcula as relações de área e perímetro de cada par de contornos
        final double[] arrContoursPairsAreaRel = new double[listContoursIdxPerms.size()];
        final double[] arrContoursPairsPerimRel = new double[listContoursIdxPerms.size()];
        Integer[] arrContoursPairsIdx = new Integer[listContoursIdxPerms.size()];
        for (int i = 0; i < listContoursIdxPerms.size(); i++) {
            Tuple<Integer, Integer> p = listContoursIdxPerms.get(i);
            arrContoursPairsAreaRel[i] = Math.abs(templatesAreaRel -
                    (mContoursAreas.get(p.x) / mContoursAreas.get(p.y)));
            arrContoursPairsPerimRel[i] = Math.abs(templatesPerimRel -
                    (contoursPerims.get(p.x) / contoursPerims.get(p.y)));
            arrContoursPairsIdx[i] = i;
        }

        // Ordena os pares de acordo com qual par tem a relação mais próxima com a dos templates.
        // Mas, para facilitar as coisas,
        // a ordem é guardada através dos idx de cada par
        Integer[] arrContoursPairsIdxOrderedByArea = arrContoursPairsIdx.clone();
        Integer[] arrContoursPairsIdxOrderedByPerim = arrContoursPairsIdx.clone();
        Arrays.sort(arrContoursPairsIdxOrderedByArea, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(arrContoursPairsAreaRel[o1], arrContoursPairsAreaRel[o2]);
            }
        });
        Arrays.sort(arrContoursPairsIdxOrderedByPerim, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(arrContoursPairsPerimRel[o1], arrContoursPairsPerimRel[o2]);
            }
        });

        // A nota de cada par é obtida pela posição que seu idx ficou nos arrays ordenados das
        // semelhanças da relação de área e da relação de perímetro.
        // Na nota final do par, são somadas as notas obtidas pelo match do primeiro cont do par
        // com o template B e pelo match do segundo cont do par com a template L.
        // Como na mesma iteração as 3 notas a serem atribuidas não são necessariamente do mesmo
        // par, ao invés de atribuir a nota diretamente, temos que somar à nota anterior, tendo a
        // garantia de que ao final do loop cada par recebeu a soma das 3 notas que lhe correspondiam.
        // Para isso, temos que garantir que o array é inicializado com 0 em cada elemento.
        double[] arrContoursPairsGrades = new double[listContoursIdxPerms.size()];    // java inicializa com 0s
        for (int i = 0; i < listContoursIdxPerms.size(); i++) {
            double grade = (listContoursIdxPerms.size() - i) * 0.5;
            arrContoursPairsGrades[arrContoursPairsIdxOrderedByArea[i]] += grade;
            arrContoursPairsGrades[arrContoursPairsIdxOrderedByPerim[i]] += grade;
            Tuple<Integer, Integer> p = listContoursIdxPerms.get(i);
            int grB = arrContoursGradesMatchB[p.x];
            int grL = arrContoursGradesMatchB[p.y];
            arrContoursPairsGrades[i] += (grB + grL);
        }

        int bestContourPairIdx = SortArgMinMax.argmax(arrContoursPairsGrades);
        mFoundTemplateB = mContours.get(listContoursIdxPerms.get(bestContourPairIdx).x);
        mFoundTemplateL = mContours.get(listContoursIdxPerms.get(bestContourPairIdx).y);
    }

    private void FilterContoursByArea() {

        final double minContourArea = 5000;
        final int maxContours = 10;
        double area;

        List<MatOfPoint> contoursFiltByMinArea = new ArrayList<MatOfPoint>();
        final List<Double> contoursAreasFiltByMinArea = new ArrayList<Double>();
        for (MatOfPoint c : mContours) {
            area = Imgproc.contourArea(c);
            if (area > minContourArea) {
                contoursFiltByMinArea.add(c);
                contoursAreasFiltByMinArea.add(area);
            }
        }

        // Somente pega maxContours de maior área depois de ter filtrado os que tem área menor que
        // minContourArea para evitar ter que ordenar um número enorme de contornos
        if (contoursAreasFiltByMinArea.size() > maxContours) {
            List<Integer> listContIdx = new ArrayList<>(contoursAreasFiltByMinArea.size());
            for (int i = 0; i < contoursAreasFiltByMinArea.size(); i++) {
                listContIdx.add(i);
            }
            // arg sort by contour area in reverse order
            Collections.sort(listContIdx, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    double a1 = contoursAreasFiltByMinArea.get(o1);
                    double a2 = contoursAreasFiltByMinArea.get(o2);
                    return a1 < a2 ? 1 // reverse order
                            : a1 > a2 ? -1
                            : 0;
                }
            });
            mContours.clear();
            mContoursAreas.clear();
            for (int i = 0; i < maxContours; i++) {
                int idx = listContIdx.get(i);
                mContours.add(contoursFiltByMinArea.get(idx));
                mContoursAreas.add(contoursAreasFiltByMinArea.get(idx));
            }
        }
        else {
            mContours = contoursFiltByMinArea;
            mContoursAreas = contoursAreasFiltByMinArea;
        }
    }

    private Mat ResizeImageToTemplateSize(Mat image) {

        final int txsz = 3264;
        final int tysz = 2448;
        float factor;

        int xsz = image.cols();
        int ysz = image.rows();

        Log.d("resize", "x: " + Integer.toString(xsz));
        Log.d("resize", "y: " + Integer.toString(ysz));

        if ((xsz == txsz) && (ysz == tysz)) {
            return image;
        }

        if ((xsz <= txsz) && (ysz <= tysz)) {
            factor = Math.max((float)txsz/xsz, (float)tysz/ysz);
        }
        else if ((xsz >= txsz) && (ysz >= tysz)) {
            factor = Math.min((float) txsz / xsz, (float) tysz / ysz);
        }
        else if (xsz <= txsz) {
            factor = (float)txsz/xsz;
        }
        else {
            factor = (float)tysz/ysz;
        }

        int interpolation;
        if (factor >= 1) {
            interpolation = Imgproc.INTER_CUBIC;
        }
        else {
            interpolation = Imgproc.INTER_AREA;
        }
        Imgproc.resize(image, image, new Size(0, 0), factor, factor, interpolation);

        xsz = image.cols();
        ysz = image.rows();

        Log.d("resize", "x: " + Integer.toString(xsz));
        Log.d("resize", "y: " + Integer.toString(ysz));

        if ((xsz == txsz) && (ysz == tysz)) {
            return image;
        }

        int xcropstart = Math.abs(xsz - txsz) / 2;
        Mat cropedRef = new Mat(image, new Rect(xcropstart, 0, txsz, tysz));
        Mat resizedImage = new Mat();
        resizedImage = cropedRef.clone();
        cropedRef.release();
        image.release();

        xsz = resizedImage.cols();
        ysz = resizedImage.rows();
        Log.d("resize", "x: " + Integer.toString(xsz));
        Log.d("resize", "y: " + Integer.toString(ysz));

        return resizedImage;
    }
}
