
package org.zarroboogs.weibo.widget;

import org.zarroboogs.weibo.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageView;

public class LinearGradientCoverImageView extends android.support.v7.widget.AppCompatImageView {

    private LinearGradient linearGradient;

    private Paint paint = new Paint();

    public LinearGradientCoverImageView(Context context) {
        this(context, null);
    }

    public LinearGradientCoverImageView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LinearGradientCoverImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (linearGradient == null) {
            int colorLinear[] = {
                    Color.TRANSPARENT, getResources().getColor(R.color.dark_gray)
            };
            linearGradient = new LinearGradient(0, 0, 0, getHeight(), colorLinear, null, Shader.TileMode.REPEAT);
            paint.setShader(linearGradient);
        }
        canvas.drawPaint(paint);
    }

}
