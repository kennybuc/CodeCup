package com.app.flexivendsymbol.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("SuspiciousNameCombination")
public class Recognizer {

    private float ThresholdParam;
    private float Tolerance;

    private short[][] Binarized;
    private short[][] Debug;

    public Recognizer() {
        ThresholdParam = 1f;
        Tolerance = 0.75f;
    }

    private PointF CenterLeft;
    private PointF CenterRight;

    private List<Circle> Circles;
    private List<PointF> SpecialPoints;

    private int H;
    private int W;

    public long Recognize(short[][] pixels, boolean inverse) {
        H = pixels.length;
        W = pixels[0].length;

        short[][] Filtered = new short[H][];
        short[][] Normalized;
        Binarized = new short[H][];
        Debug = new short[H][];

        // filtration
        for (int y = 0; y < H; y++)
            Filtered[y] = MedianFilter(pixels[y]);

        // normalization
        Normalized = Normalize(Filtered);

        // binarization
        Binarized = Binarize(Normalized);

        //inverse
        if (inverse)
            for (int y = 0; y < H; y++)
                Binarized[y] = Inverse(Binarized[y]);

        //find special points
        SpecialPoints = FindSpecialPoints();
        if (SpecialPoints.size() < 20) return -1;

        //find liner
        FindLiner();

        //find circles
        FindCircles();

        //find code of circles
        FindCodeOfCircles();

        //calc number
        return CalcResult();
    }

    private short[][] Binarize(short[][] pixels) {
        short[][] res = new short[pixels.length][];
        int window = W / 20;
        for (int y = 0; y < H; y++) {
            short[] line = pixels[y];
            int[] integral = new int[W + 1];
            int sum = 0;
            for (int x = 0; x < W; x++) {
                sum += line[x];
                integral[x + 1] = sum;
            }

            short[] resLine = res[y] = new short[W];
            for (int x = 0; x < W; x++) {
                int from = x - window / 2;
                int to = x + window / 2;
                if (from < 0) from = 0;
                if (to >= W) to = W - 1;
                float avg = 1f * (integral[to] - integral[from]) / (to - from);
                avg = (3 * avg + 128) / (3 + 1f);
                if (line[x] > avg * ThresholdParam)
                    resLine[x] = 255;
            }
        }

        return res;
    }

    private long CalcResult() {
        //check head circle
        if (Circles.size() != 8) return -1;
        int dir = 0;
        if (Circles.get(0).BottomCode == 0 && Circles.get(0).TopCode == 0) dir = 1;
        if (Circles.get(7).BottomCode == 0 && Circles.get(7).TopCode == 0) dir = -1;
        if (dir == 0) return -1;//no head circle

        //reverse
        if (dir == -1) {
            ArrayList<Circle> circles = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Circle c = Circles.get(7 - i);
                int temp = c.TopCode;
                c.TopCode = c.BottomCode;
                c.BottomCode = temp;
                circles.add(c);
            }

            Circles = circles;
        }

        //calc
        long res = 0;

        for (int i = 1; i < 8; i++) {
            res = res * 24;
            Circle c = Circles.get(i);
            if (c.TopCode < 0 || c.BottomCode < 0 || c.TopCode + c.BottomCode == 0)
                return -1;//error in circle
            int n = (c.TopCode) * 5 + c.BottomCode - 1;
            res += n;
        }

        //decode
        res = new CoderDecoder().Decode(res);

        return res;
    }

    private void FindCodeOfCircles() {
        for (Circle c : Circles) {
            c.TopCode = FindCodeOfCircle(c, true);
            c.BottomCode = FindCodeOfCircle(c, false);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void FindLiner() {
        List<PointF> points = SpecialPoints;

        //sort points by X
        Collections.sort(points, new Comparator<PointF>() {
            @Override
            public int compare(PointF p1, PointF p2) {
                if (p1.X < p2.X) return -1;
                else if (p1.X == p2.X) return 0;
                return 1;
            }
        });

        //find ends of line
        {
            int cutCount = points.size() / 7;
            int right = points.size() - cutCount;
            int left = cutCount;

            PointF bestP1 = new PointF();
            PointF bestP2 = new PointF();
            float max = 0f;
            for (int i2 = points.size() - 1; i2 >= right; i2--)
                for (int i1 = 0; i1 < left; i1++) {
                    PointF p1 = points.get(i1);
                    PointF p2 = points.get(i2);

                    float rate = CheckLine(p1, p2, points);
                    if (rate > max) {
                        max = rate;
                        bestP1 = p1;
                        bestP2 = p2;
                    }
                }

            CenterLeft = bestP1;
            CenterRight = bestP2;
        }

        PointF TopSubLineLeft;
        PointF BottomSubLineLeft;
        PointF TopSubLineRight;
        PointF BottomSubLineRight;
        //find sublines
        {
            PointF dir = CenterRight.Sub(CenterLeft).Normalized();
            PointF normal = new PointF(dir.Y, -dir.X);
            TopSubLineLeft = FindBlackPixel(CenterLeft, normal);
            BottomSubLineLeft = FindBlackPixel(CenterLeft, normal.Mul(-1));
            TopSubLineRight = FindBlackPixel(CenterRight, normal);
            BottomSubLineRight = FindBlackPixel(CenterRight, normal.Mul(-1));
        }

        //adjust end of liner
        CenterLeft = new PointF(CenterLeft.X, Avg(TopSubLineLeft.Y, BottomSubLineLeft.Y));
        CenterRight = new PointF(CenterRight.X, Avg(TopSubLineRight.Y, BottomSubLineRight.Y));
    }

    private void FindCircles() {
        //calc R
        float r = 2 * CenterRight.Sub(CenterLeft).Length() / (7 * 9);

        //find circles
        Circles = new ArrayList<>();
        PointF dir = CenterRight.Sub(CenterLeft).Normalized();

        for (int i = 0; i < 8; i++) {
            PointF p = CenterLeft.Lerp(CenterRight, i / 7f);
            PointF right = FindBlackPixel(p, dir);
            PointF left = FindBlackPixel(p, dir.Mul(-1));
            Circle circle = new Circle();
            circle.Centre = new PointF(Avg(right.X, left.X), p.Y);
            circle.R = r;
            Circles.add(circle);
        }
    }

    private int FindCodeOfCircle(Circle circle, boolean top) {
        PointF dir = CenterRight.Sub(CenterLeft).Normalized().Mul(circle.R * 1.5f);
        float da = (float) Math.PI / 5;//Math.PI/7;
        float startAngle = da * 1.5f + (float) Math.PI / 2;
        if (top) startAngle -= (float) Math.PI;
        int counter = 0;
        int res = 0;

        for (int n = 0; n < 4; n++) {
            float a = startAngle - n * da;
            PointF d = dir.Rotate(a);
            PointF p = circle.Centre.Add(d);
            try {
                Debug[(int) p.Y][(int) p.X] = (byte) 0xFF;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (IsWhitePixel(p)) {
                counter++;
                res = n + 1;
            }
        }

        if (counter > 1)
            res = -1;

        return res;
    }

    private PointF FindBlackPixel(PointF start, PointF dir) {
        PointF n = new PointF(dir.Y, -dir.X).Mul(2);
        while (true) {
            if (GetPixel(start) == 0) return start;
            if (GetPixel(start.Add(n)) == 0) return start;
            if (GetPixel(start.Sub(n)) == 0) return start;
            start = start.Add(dir);
        }
    }

    private boolean IsWhitePixel(PointF p) {
        float cx = p.X;
        float cy = p.Y;
        int counter = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) {
                short v = GetPixel(new PointF(cx + dx, cy + dy));
                if (v > 0) counter++;
            }

        return counter >= 3;
    }

    private short GetPixel(PointF p) {
        int x = Math.round(p.X);
        int y = Math.round(p.Y);
        if (y < 0 || y >= H) return 0;
        if (x < 0 || x >= W) return 0;
        return Binarized[y][x];
    }

    private float CheckLine(PointF p1, PointF p2, List<PointF> points) {
        PointF line = p2.Sub(p1);
        float lineLength = line.Length();
        if (lineLength < W / 3) return 0;

        float counter = 0f;
        double sum = 0d;

        float lx = p2.X - p1.X;

        for (int i = 0; i < points.size(); i++) {
            float dot = points.get(i).X - p1.X;
            if (dot < 0 || dot > lx) continue;
            float dy = Math.abs(points.get(i).Y - p1.Lerp(p2, dot / lx).Y);
            if (dy < H / 8)
                sum += (1 / (1 + dy));
            counter++;
        }
        if (counter == 0)
            return 0;

        return (float) sum;
    }

    private List<PointF> FindSpecialPoints() {
        List<PointF> list = new ArrayList<>();
        float expectedParam = (W / 8f) / 8f;

        for (int y = 0; y < H; y++) {
            Debug[y] = new short[W];

            Interval[] intervals = GetIntervals(Binarized[y]);
            for (int i = 2; i < intervals.length; i++) {
                Interval int1 = intervals[i - 2];
                Interval int2 = intervals[i - 1];
                Interval int3 = intervals[i];

                if (int3.Val > 0)//white pixel
                {
                    //find centers of circles
                    if (AreEqual(int1.Length + int3.Length, int2.Length, Tolerance)) // formula of black pixels: X - 2X - X
                        if (AreEqual(int1.Length, int3.Length, Tolerance)) {
                            float x = Avg(int1.StartX - int1.Length, int3.StartX);
                            float param = (int3.StartX - (int1.StartX - int1.Length)) / 8f; // unit X
                            //filter by unit
                            if (param < expectedParam * 1.1f)
                                if (param > expectedParam / 3f) {
                                    list.add(new PointF(x, y));
                                    //Debug[y][(int)Math.Round(x)] = 40;
                                }
                        }

                    //find point between circles
                    if (AreEqual((int1.Length + int3.Length) / 4f, int2.Length, Tolerance)) // formula of black pixels: 2X - X - 2X
                        if (AreEqual(int1.Length, int3.Length, Tolerance)) {
                            float x = Avg(int1.StartX - int1.Length, int3.StartX);
                            float param = (int3.StartX - (int1.StartX - int1.Length)) / 5f; // unit X
                            //filter by unit
                            if (param < expectedParam * 1.1f)
                                if (param > expectedParam / 3f) {
                                    list.add(new PointF(x, y));
                                    //Debug[y][(int)Math.Round(x)] = 40;
                                }
                        }
                }
            }
        }

        return list;
    }

    private Interval[] GetIntervals(short[] bin) {
        // find length of intervals
        int prevX = 0;
        int startX = 0;
        List<Interval> res = new ArrayList<>();

        for (int x = startX + 1; x < bin.length; x++) {
            if (bin[x] != bin[x - 1]) {
                res.add(new Interval(x, x - prevX, bin[x]));
                prevX = x;
            }
        }
        return res.toArray(new Interval[res.size()]);
    }

    private short[] MedianFilter(short[] pixels) {
        short[] res = new short[pixels.length];
        res[0] = pixels[0];
        res[pixels.length - 1] = pixels[pixels.length - 1];

        for (int x = 1; x < pixels.length - 1; x++) {
            short p1 = pixels[x - 1];
            short p2 = pixels[x];
            short p3 = pixels[x + 1];
            if (p2 > p1 && p2 > p3) p2 = (short) Math.max(p1, p3);
            else if (p2 < p1 && p2 < p3) p2 = (short) Math.min(p1, p3);
            res[x] = p2;
        }

        return res;
    }

    private short[][] Normalize(short[][] pixels) {
        int H = pixels.length;
        int W = pixels[0].length;
        int min = 255;
        int max = 0;
        for (short[] line : pixels) {
            for (int x = 0; x < W; x++) {
                short p = line[x];
                if (p > max) max = p;
                if (p < min) min = p;
            }
        }

        float k = 255f / (max - min);
        short[][] res = new short[H][];

        for (int y = 0; y < H; y++) {
            short[] resLine = res[y] = new short[W];
            short[] line = pixels[y];
            for (int x = 0; x < W; x++) {
                short p = (short) ((line[x] - min) * k);
                if (p > 255) p = 255;
                resLine[x] = p;
            }
        }

        return res;
    }

    private float Avg(float... X) {
        float res = 0f;
        for (float aX : X) {
            res += aX;
        }

        return (res / X.length);
    }


    private short[] Inverse(short[] pixels) {
        short[] res = new short[pixels.length];
        for (int x = 0; x < pixels.length; x++) {
            res[x] = (byte) (255 - pixels[x]);
        }

        return res;
    }

    private boolean AreEqual(float v1, float v2, float tolerance) {
        if (v2 == 0 && v1 == 0) return true;
        if (v2 > v1) return v1 / v2 >= tolerance;
        return v2 / v1 >= tolerance;
    }

    private static class Interval {
        float StartX;
        float Length;
        short Val;

        Interval(float startX, float length, short val) {
            StartX = startX;
            Length = length;
            Val = val;
        }
    }

    private static class Circle {
        PointF Centre;
        public float R;
        int BottomCode;
        int TopCode;
    }

}
