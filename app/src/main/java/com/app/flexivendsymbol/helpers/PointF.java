package com.app.flexivendsymbol.helpers;

import java.util.Locale;

public class PointF {

    public float X;
    public float Y;

    public PointF() {

    }

    public PointF(float x, float y) {
        X = x;
        Y = y;
    }

    public float Length() {
        return (float) Math.sqrt(X * X + Y * Y);
    }

    public float LengthSquare() {
        return X * X + Y * Y;
    }

    public PointF Normalized() {
        float l = this.Length();
        return new PointF(this.X / l, this.Y / l);
    }

    public PointF Add(PointF c2) {
        return new PointF(this.X + c2.X, this.Y + c2.Y);
    }

    public PointF Sub(PointF c2) {
        return new PointF(this.X - c2.X, this.Y - c2.Y);
    }

    public PointF Mul(float k) {
        return new PointF(this.X * k, this.Y * k);
    }

    public PointF Lerp(PointF x2, float k) {
        float m = 1.0f - k;
        return new PointF(X * m + x2.X * k, this.Y * m + x2.Y * k);
    }

    public PointF Rotate(float angle) {
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        return new PointF((float) (cosAngle * this.X - sinAngle * this.Y), (float) (sinAngle * this.X + cosAngle * this.Y));
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "{0:%.02f};{1:%.02f}", this.X, this.Y);
    }

}
