package com.app.flexivendsymbol.helpers;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;

import java.util.List;

@SuppressWarnings("deprecation")
public class Helper {

    public static short[][] GetPixelStrip(Bitmap image, int y1, int y2) {
        if (image == null) {
            return new short[0][];
        }

        if (y1 == -1) {
            y1 = 0;
            y2 = image.getHeight();
        }

        int H = y2 - y1;
        short[][] res = new short[H][];
        short[] line;
        int pixel;

        for (int y = y1; y < y2; y++) {
            int w = image.getWidth();
            line = res[y - y1] = new short[w];
            for (int ci = 0; ci < w; ci++) {
                pixel = image.getPixel(ci, y);
                line[ci] = (short) Color.green(pixel);
            }
        }

        return res;
    }

    public static short[][] getPixelStripRotated(Bitmap image) {
        if (image == null) {
            return new short[0][];
        }

        int x1 = image.getWidth() / 4;
        int x2 = image.getWidth() / 2;
        int y1 = 0;
        int y2 = image.getHeight();

        int W = y2 - y1;
        int H = x2 - x1;

        int pixel;
        short[][] res = new short[H][W];
        short[] line;
        for (int x = x1; x < x2; x++) {
            line = res[x - x1] = new short[W];
            for (int y = y1; y < y2; y++) {
                pixel = image.getPixel(x, y2 - y - 1);
                line[y] = (short) Color.green(pixel);
            }
        }

        return res;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static short[][] getPixelStripRotated(int[] pixels, int width, int height) {
        int x1 = width / 4;
        int x2 = width / 2;
        int y1 = 0;
        int y2 = height;

        int W = y2 - y1;
        int H = x2 - x1;

        short[][] res = new short[H][W];
        short[] line;
        int pixel;
        for (int x = x1, actualY = 0; x < x2; x++, actualY++) {
            line = res[actualY] = new short[W];
            for (int y = y2 - 1, actualX = 0; y >= y1; y--, actualX++) {
                pixel = pixels[height * y + x];
                line[actualX] = (short) Color.green(pixel);
            }
        }

        return res;
    }

    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    public static Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea < resultArea) {
                    result = size;
                }
            }
        }

        return (result);
    }

}
