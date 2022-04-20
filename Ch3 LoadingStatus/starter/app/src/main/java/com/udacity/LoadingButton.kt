package com.udacity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var widthSize = 0
    private var heightSize = 0

    // Animation related
    private val valueAnimator = ValueAnimator()
    private var progressBackgroundWidth = 0f
    private var progressCircleSweepAngle = 0f
    private var currentText = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.default_text_size)
        typeface = Typeface.create("", Typeface.BOLD)
    }

    // Custom attributes
    private var idleBackgroundColor = 0
    private var loadingBackgroundColor = 0
    private var progressCircleColor = 0
    private var progressCircleDiameter = 0f
    private var progressCirclePadding = 0f
    private var textColor = 0

    //region Button state
    private var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Completed) { _, old, new ->
        when (new) {
            ButtonState.Clicked -> {
                currentText = context.getString(R.string.button_loading)
                startAnimation()
                changeButtonState()
            }
            ButtonState.Loading -> {}
            ButtonState.Completed -> {
                currentText = context.getString(R.string.button_name)
                stopAnimation()
            }
        }
    }

    fun changeButtonState() {
        buttonState = when (buttonState) {
            ButtonState.Clicked -> ButtonState.Loading
            ButtonState.Loading -> ButtonState.Completed
            ButtonState.Completed -> ButtonState.Clicked
        }
        invalidate()
    }
    //endregion

    init {
        isClickable = true

        // Cache custom attributes
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            idleBackgroundColor = getColor(R.styleable.LoadingButton_idleBackgroundColor, 0)
            loadingBackgroundColor = getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            progressCircleColor = getColor(R.styleable.LoadingButton_progressCircleColor, 0)
            progressCircleDiameter =
                getDimension(R.styleable.LoadingButton_progressCircleDiameter, 0f)
            progressCirclePadding =
                getDimension(R.styleable.LoadingButton_progressCirclePadding, 0f)
            textColor = getColor(R.styleable.LoadingButton_textColor, 0)
        }

        buttonState = ButtonState.Completed
    }

    //region Drawing
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        // 1. Button background (color the whole view)
        canvas.drawColor(idleBackgroundColor)

        // 2. Rectangle progress background
        if (buttonState != ButtonState.Completed) canvas.drawProgressBackground()

        // 3. Button text
        canvas.drawButtonText()

        // 4. Progress Circle
        if (buttonState != ButtonState.Completed) canvas.drawProgressCircle()
    }

    private fun Canvas.drawProgressBackground() {
        save()
        paint.color = loadingBackgroundColor
        drawRect(0f, 0f, progressBackgroundWidth, heightSize.toFloat(), paint)
        restore()
    }

    private fun Canvas.drawButtonText() {
        save()
        paint.color = textColor
        // Center vertically
        // 1. Get the whole view's vertical center
        // 2. Calculate the actual text height (not including padding below/above baseline)
        // 3. Subtract half of the text height from the vertical center
        // https://stackoverflow.com/a/36321422
        // https://www.jianshu.com/p/71cf11c120f0
        val verticalPosition = (heightSize / 2).toFloat() - ((paint.descent() + paint.ascent()) / 2)

        drawText(
            currentText,
            (widthSize / 2).toFloat(), // Center horizontally
            verticalPosition,
            paint
        )
        restore()
    }

    private fun Canvas.drawProgressCircle() {
        save()
        paint.color = progressCircleColor

        val textWidth = paint.measureText(currentText)
        // Get the horizontal center, add half of the text width, plus some padding
        val startPosition = (widthSize / 2) + (textWidth / 2) + progressCirclePadding

        drawArc(
            // Start
            startPosition,
            // Top: Get the vertical center, then subtract radius (half) of the circle
            (heightSize / 2).toFloat() - (progressCircleDiameter / 2),
            // End
            startPosition + progressCircleDiameter,
            // Bottom: Get the vertical center, add radius of the circle
            (heightSize / 2).toFloat() + (progressCircleDiameter / 2),
            // Start angle (starting point of the sweep animation): from the right of the circle
            0f,
            progressCircleSweepAngle,
            true,
            paint
        )

        restore()
    }
    //endregion

    //region Animation
    private fun startAnimation() {
        valueAnimator.apply {
            setFloatValues(0f, 1f)
            duration = 3000
            addUpdateListener { animator ->
                val progressPercentage = animator.animatedValue as Float
                progressBackgroundWidth = widthSize * progressPercentage
                progressCircleSweepAngle = 360f * progressPercentage
                postInvalidate()
            }
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopAnimation() {
        valueAnimator.cancel()
        valueAnimator.removeAllUpdateListeners()
    }
    //endregion

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

}