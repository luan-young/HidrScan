package com.example.luan.hidrscan.utils;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by luan on 14/03/18.
 */

public class ImageProcessUtil {

    static public MatOfPoint2f ConvertIndexesToPoints2f(MatOfPoint contour, MatOfInt indexes) {
        int[] arrIndex = indexes.toArray();
        Point[] arrContour = contour.toArray();
        Point[] arrPoints = new Point[arrIndex.length];
        for (int i = 0; i < arrIndex.length; i++) {
            arrPoints[i] = arrContour[arrIndex[i]];
        }
        MatOfPoint2f hull = new MatOfPoint2f();
        hull.fromArray(arrPoints);
        return hull;
    }

    static public double GetPointsDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    static public double FindAngleHoriz(Point pt1, Point pt2) {
        if ((pt2.x - pt1.x) == 0) {
            if (pt2.y == pt1.y) return 0.0;
            else if (pt2.y > pt1.y) return -Math.PI/2;
            else return Math.PI/2;
        }
        return Math.atan((pt2.y - pt1.y) / (pt2.x - pt1.x)) * -1.0;
    }

    static public double FindAngleVert(Point pt1, Point pt2) {
        if ((pt2.y - pt1.y) == 0) {
            if (pt2.x == pt1.x) return 0.0;
            else if (pt2.x > pt1.x) return Math.PI/2;
            else return -Math.PI/2;
        }
        return Math.atan((pt2.x - pt1.x) / (pt2.y - pt1.y));
    }

    static public Point RotatePointByTeta(Point p, double t) {
        Point r = new Point();
        double c = Math.cos(t);
        double s = Math.sin(t);
        r.x = p.x * c - p.y * s;
        r.y = p.x * s + p.y * c;
        return r;
    }

    static public boolean InRangeHoriz(Point p, Point l, Point r) {
        final double dx = 60;
        final double dy = 50;
        double minx = l.x + dx;
        double maxx = r.x - dx;
        double miny = l.y - dy;
        double maxy = l.y + dy;
        if ((p.x < minx) || (p.x > maxx) || (p.y < miny) || (p.y > maxy)) return false;
        return true;
    }

    static public boolean InRangeVert(Point p, Point t, Point b) {
        final double dy = 60;
        final double dx = 50;
        double miny = t.y + dy;
        double maxy = b.y - dy;
        double minx = t.x - dx;
        double maxx = t.x + dx;
        if ((p.x < minx) || (p.x > maxx) || (p.y < miny) || (p.y > maxy)) return false;
        return true;
    }

    static public Point FindLinesIntersection(LineEquation line1, LineEquation line2) {
        Point interscection = new Point();
        interscection.x = (line2.c - line1.c) / (line1.m - line2.m);
        interscection.y = interscection.x * line1.m + line1.c;
        return interscection;
    }

    static public LineEquation FindHorizontalLineSeparating2Sets(
            MatOfPoint pointsB, MatOfPoint pointsL, Point pointLeft, Point pointRight) {

        LineEquation lineEq = new LineEquation();
        Point[] arrPointsB = pointsB.toArray();
        Point[] arrPointsL = pointsL.toArray();

        double teta = FindAngleHoriz(pointLeft, pointRight);
        Point ptLefRot = RotatePointByTeta(pointLeft, teta);
        Point ptRigRot = RotatePointByTeta(pointRight, teta);

        List pointsLineAbove = new ArrayList(arrPointsB.length);
        List pointsLineBellow = new ArrayList(arrPointsB.length);

        Point rot;
        for (Point p : arrPointsB) {
            rot = RotatePointByTeta(p, teta);
            if (InRangeHoriz(rot, ptLefRot, ptRigRot)) {
                if (rot.y < ptLefRot.y) pointsLineAbove.add(p);
                else pointsLineBellow.add(p);
            }
        }
        for (Point p : arrPointsL) {
            rot = RotatePointByTeta(p, teta);
            if (InRangeHoriz(rot, ptLefRot, ptRigRot)) {
                if (rot.y < ptLefRot.y) pointsLineAbove.add(p);
                else pointsLineBellow.add(p);
            }
        }

        MatOfPoint matPoints = new MatOfPoint();
        Mat line = new Mat();

        // Get equation from line above

        matPoints.fromList(pointsLineAbove);
        Imgproc.fitLine(matPoints, line, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);

        // lineCoefs = (vx, vy, x0, y0)
        float[] lineCoefs = new float[(int)line.total() * line.channels()];
        line.get(0,0, lineCoefs);

        // Line equation: y = mx + c ==> m = vy / vx and c = y0 - m*x0
        float m1 = lineCoefs[1] / lineCoefs[0];
        float c1 = lineCoefs[3] - m1 * lineCoefs[2];

        // Get equation from line bellow

        matPoints.fromList(pointsLineBellow);
        Imgproc.fitLine(matPoints, line, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);

        // lineCoefs = (vx, vy, x0, y0)
        line.get(0,0, lineCoefs);

        // Line equation: y = mx + c ==> m = vy / vx and c = y0 - m*x0
        float m2 = lineCoefs[1] / lineCoefs[0];
        float c2 = lineCoefs[3] - m2 * lineCoefs[2];

        lineEq.m = (m1 + m2) / 2;
        lineEq.c = (c1 + c2) / 2;

        return lineEq;
    }

    static public LineEquation FindVerticalLineSeparating2Sets(
            MatOfPoint pointsB, MatOfPoint pointsL, Point pointTop, Point pointBotton) {

        LineEquation lineEq = new LineEquation();
        Point[] arrPointsB = pointsB.toArray();
        Point[] arrPointsL = pointsL.toArray();

        double teta = FindAngleVert(pointTop, pointBotton);
        Point ptTopRot = RotatePointByTeta(pointTop, teta);
        Point ptBotRot = RotatePointByTeta(pointBotton, teta);

        List pointsLineLeft = new ArrayList(arrPointsB.length);
        List pointsLineRight = new ArrayList(arrPointsB.length);

        Point rot;
        for (Point p : arrPointsB) {
            rot = RotatePointByTeta(p, teta);
            if (InRangeVert(rot, ptTopRot, ptBotRot)) {
                if (rot.x < ptTopRot.x) pointsLineLeft.add(p);
                else pointsLineRight.add(p);
            }
        }
        for (Point p : arrPointsL) {
            rot = RotatePointByTeta(p, teta);
            if (InRangeVert(rot, ptTopRot, ptBotRot)) {
                if (rot.x < ptTopRot.x) pointsLineLeft.add(p);
                else pointsLineRight.add(p);
            }
        }

        MatOfPoint matPoints = new MatOfPoint();
        Mat line = new Mat();

        // Get equation from line left

        matPoints.fromList(pointsLineLeft);
        Imgproc.fitLine(matPoints, line, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);

        // lineCoefs = (vx, vy, x0, y0)
        float[] lineCoefs = new float[(int)line.total() * line.channels()];
        line.get(0,0, lineCoefs);

        // Line equation: y = mx + c ==> m = vy / vx and c = y0 - m*x0
        float m1 = lineCoefs[1] / lineCoefs[0];
        float c1 = lineCoefs[3] - m1 * lineCoefs[2];

        // Get equation from line bellow

        matPoints.fromList(pointsLineRight);
        Imgproc.fitLine(matPoints, line, Imgproc.CV_DIST_L2, 0, 0.01, 0.01);

        // lineCoefs = (vx, vy, x0, y0)
        line.get(0,0, lineCoefs);

        // Line equation: y = mx + c ==> m = vy / vx and c = y0 - m*x0
        float m2 = lineCoefs[1] / lineCoefs[0];
        float c2 = lineCoefs[3] - m2 * lineCoefs[2];

        lineEq.m = (m1 + m2) / 2;
        lineEq.c = (c1 + c2) / 2;

        return lineEq;
    }

    static public OrderedCorners GetOrderedCorners(Point[] pointsB, Point[] pointsL) {

        OrderedCorners corners = new OrderedCorners();
        boolean isVertical;
        boolean isInverted;

        // If pointsB's x coordinates are colseser than y coordinates
        // Those points are considered as vertical displaced
        if (Math.abs(pointsB[0].x - pointsB[1].x) < Math.abs(pointsB[0].y - pointsB[1].y)) {
            isVertical = true;
            // If the pointB's x coordinate is to the left than pointL's x coordinate
            // The templates positions are NOT inverted, are at the expected positions
            if (pointsB[0].x < pointsL[0].x) {  // could test any point of each template, as both should
                isInverted = false;             // be more left or more right than the other template
            }
            else {
                isInverted = true;
            }
        }
        else {
            isVertical = false;
            // If the pointB's y coordinate is to the botton than pointL's y coordinate
            // The templates positions are NOT inverted, are at the expected positions
            if (pointsB[0].x < pointsL[0].x) {  // could test any point of each template, as both should
                isInverted = false;             // be more botton or more up than the other template
            }
            else {
                isInverted = true;
            }
        }

        if (isVertical) {
            if (pointsB[0].y < pointsB[1].y) {
                corners.pBT = pointsB[0];
                corners.pBB = pointsB[1];
            }
            else {
                corners.pBT = pointsB[1];
                corners.pBB = pointsB[0];
            }
            if (pointsL[0].y < pointsL[1].y) {
                corners.pLT = pointsL[0];
                corners.pLB = pointsL[1];
            }
            else {
                corners.pLT = pointsL[1];
                corners.pLB = pointsL[0];
            }
        }
        else {
            if (pointsB[0].x < pointsB[1].x) {
                corners.pBT = pointsB[0];
                corners.pBB = pointsB[1];
            }
            else {
                corners.pBT = pointsB[1];
                corners.pBB = pointsB[0];
            }
            if (pointsL[0].x < pointsL[1].x) {
                corners.pLT = pointsL[0];
                corners.pLB = pointsL[1];
            }
            else {
                corners.pLT = pointsL[1];
                corners.pLB = pointsL[0];
            }
        }

        if (isInverted) {
            Point tmp = corners.pBB;
            corners.pBB = corners.pBT;
            corners.pBT = tmp;
            tmp = corners.pLB;
            corners.pLB = corners.pLT;
            corners.pLT = tmp;
        }

        return  corners;
    }

    static public Point[] Get2MeanPointsOf2ClosestPairs(MatOfPoint2f points) {

        Point[] arrPoints = points.toArray();
        Point[] arrReturn = new Point[2];
        for (int i = 0; i < arrReturn.length; i++) {
            arrReturn[i] = new Point();
        }

        // Gera todas as combinações de pares de pontos: [(0,1), (0,2), ..., (1,2), (1,3)...]
        // Calcula um array com todas as distâncias entre os 2 pontos do par
        // Cria um array com os índices dos pares, onde cada índice corresponde a uma distância calculada
        // O array com os ídices será ordenado de acordo com a distância correspondente
        // O primeiro índice da lista corresponderá ao par cujos 2 elementos são os pontos mais próximos
        // O segundo índice da lista corresponderá ao par cujos 2 elementos são os segundos pontos mais próximos
        List<Tuple<Integer, Integer>> arrCombsOf2Idx = TupleUtil.Generate2Combinations(arrPoints.length);
        double[] arrPairDists = new double[arrCombsOf2Idx.size()];
        for (int i = 0; i < arrCombsOf2Idx.size(); i++) {
            arrPairDists[i] = GetPointsDistance(arrPoints[arrCombsOf2Idx.get(i).x],
                    arrPoints[arrCombsOf2Idx.get(i).y]);;
        }

        Integer[] arrOrderedDistsIdx = SortArgMinMax.argsort(arrPairDists);

        Point closest1A = arrPoints[arrCombsOf2Idx.get(arrOrderedDistsIdx[0]).x];
        Point closest1B = arrPoints[arrCombsOf2Idx.get(arrOrderedDistsIdx[0]).y];
        Point closest2A = arrPoints[arrCombsOf2Idx.get(arrOrderedDistsIdx[1]).x];
        Point closest2B = arrPoints[arrCombsOf2Idx.get(arrOrderedDistsIdx[1]).y];

        arrReturn[0].x = (closest1A.x + closest1B.x) / 2;
        arrReturn[0].y = (closest1A.y + closest1B.y) / 2;
        arrReturn[1].x = (closest2A.x + closest2B.x) / 2;
        arrReturn[1].y = (closest2A.y + closest2B.y) / 2;

        return arrReturn;
    }

    static public MatOfPoint2f Get6VerticesFromTemplate(MatOfPoint template) {

        double startPerc = 0.001;
        double stepPerc = 0.0001;

        MatOfPoint2f vertices = new MatOfPoint2f();

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(template, hull);
        MatOfPoint2f hullPoints = ConvertIndexesToPoints2f(template, hull);

//        MatOfPoint2f tp = new MatOfPoint2f();
//        template.convertTo(tp, CvType.CV_32F);

        double perim = Imgproc.arcLength(hullPoints, true);

        double epsilon = startPerc * perim;
        Imgproc.approxPolyDP(hullPoints, vertices, epsilon, true);

        Size sz = vertices.size();
        while (sz.height > 6) {
            startPerc += stepPerc;
            epsilon = startPerc * perim;
            Imgproc.approxPolyDP(hullPoints, vertices, epsilon, true);
            sz = vertices.size();
        }

        return vertices;
    }

    static public float[] BitmapToNormalizedFloatArray(Bitmap image) {
        Mat matImage = new Mat();
        Utils.bitmapToMat(image, matImage);
        Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_RGB2GRAY);
        matImage.convertTo(matImage, CvType.CV_32F);
        Core.add(matImage, new Scalar(-127.5), matImage);
        Core.divide(matImage, new Scalar(255.0), matImage);
        float arrImage[] = new float[(int)matImage.total() * matImage.channels()];
        matImage.get(0,0, arrImage);
        return arrImage;
    }

    static public float[] MatToNormalizedFloatArray(Mat matImage) {
        matImage.convertTo(matImage, CvType.CV_32F);
        Core.add(matImage, new Scalar(-127.5), matImage);
        Core.divide(matImage, new Scalar(255.0), matImage);
        float arrImage[] = new float[(int)matImage.total() * matImage.channels()];
        matImage.get(0,0, arrImage);
        return arrImage;
    }
}

class OrderedCorners {
    public Point pBT; // big top
    public Point pBB; // big botton
    public Point pLT; // little top
    public Point pLB; // little botton

    public OrderedCorners() {
        pBT = new Point();
        pBB = new Point();
        pLT = new Point();
        pLB = new Point();
    }
}

class LineEquation {
    public float m;
    public float c;
}
