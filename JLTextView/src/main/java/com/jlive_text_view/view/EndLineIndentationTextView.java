package com.jlive_text_view.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.jlive_text_view.R;

public class EndLineIndentationTextView extends View implements View.OnTouchListener {

    private static final int FOLD = 1;//折叠状态
    private static final int EXPAND = 2;//展开状态

    private static final int END = 3;
    private static final int START = 4;

    private StaticLayout mStaticLayout;

    private float mTextSize;
    //文字后面跟随的文字 字体大小
    private float mFollowedTextSize;
    private int mTextColor;
    private int mMaxLines;
    //行间距
    private float mLineSpacingExtraMult, mLineSpacingExtraAdd;
    //跟随字体颜色
    private int mFollowedTextColor;
    //折叠状态 跟随的文字
    private String mFollowedFoldText;
    //展开状态 跟随的文字
    private String mFollowedExpandText;
    //正文文字
    private String mText;
    //跟随文字与 正文的间距
    private int mEndLineIndent;
    private TextPaint mTextPaint;
    private TextPaint mFollowedPaint;

    private int mEndingDrawableId;
    private Bitmap mEndingDrawableBitmap;
    private int mEndDrawableHeight;
    private int mEndDrawableWidth;

    private int mViewWidth, mViewHeight;
    private int mFollowedFoldTextWidth, mFollowedFoldTextHeight;
    private int mFollowedExpandTextWidth, mFollowedExpandTextHeight;
    private int mFollowedWidth, mFollowedHeight;
    private Rect mRect;
    /**
     * 文本是否大于maxLines
     * true是大
     */
    private boolean mIsTextMoreThanMaxLines;
    /**
     * 跟随文字 在行数不够maxLines的时候是否隐藏
     * true 是隐藏 false 是不隐藏
     */
    private boolean mFollowedHideable;
    private int mFollowType;

    private int mState = FOLD;
    private float clickTextX, clickTextY;

    private OnFollowedClickListener onFollowedClickListener;

    private String TAG = "EndLineIndentationTextView";

    public EndLineIndentationTextView(Context context) {
        this(context, null);
    }

    public EndLineIndentationTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EndLineIndentationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EndLineIndentationTextView);
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_text_size, 10);
        mFollowedTextSize = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_followed_text_size, 10);
        mTextColor = typedArray.getColor(R.styleable.EndLineIndentationTextView_text_color, Color.BLACK);
        mFollowedTextColor = typedArray.getColor(R.styleable.EndLineIndentationTextView_followed_text_color, Color.BLACK);
        mMaxLines = typedArray.getInt(R.styleable.EndLineIndentationTextView_max_lines, 1);
        mLineSpacingExtraMult = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_lineSpacingExtraMult, 1);
        mLineSpacingExtraAdd = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_lineSpacingExtraAdd, 0);
        mText = typedArray.getString(R.styleable.EndLineIndentationTextView_text);
        mFollowedExpandText = typedArray.getString(R.styleable.EndLineIndentationTextView_followed_expand_text);
        mFollowedFoldText = typedArray.getString(R.styleable.EndLineIndentationTextView_followed_fold_text);
        mEndLineIndent = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_endLineIndent, 0);
        mFollowedHideable = typedArray.getBoolean(R.styleable.EndLineIndentationTextView_followedHideable, true);
        mFollowType = typedArray.getInt(R.styleable.EndLineIndentationTextView_follow_type, 3);
        mEndingDrawableId = typedArray.getResourceId(R.styleable.EndLineIndentationTextView_ending_drawable, -1);
        mEndDrawableWidth = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_ending_drawable_width, 0);
        mEndDrawableHeight = typedArray.getDimensionPixelSize(R.styleable.EndLineIndentationTextView_ending_drawable_height, 0);
        typedArray.recycle();
        initTextPaint();
        if (!TextUtils.isEmpty(mFollowedExpandText) && !TextUtils.isEmpty(mFollowedFoldText)) {
            initFollowTextPaint();
            initFoldTextViewSize();
            initExpendTextViewSize();
        } else if (mEndingDrawableId != -1) {
            mEndingDrawableBitmap = BitmapFactory.decodeResource(context.getResources(), mEndingDrawableId);
        }
        this.setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        initIsTextMoreThanMaxLines();
        initStaticLayout();

        // 获取宽-测量规则的模式和大小
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        // 获取高-测量规则的模式和大小
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        Log.e(TAG, "onMeasure: mViewWidth == " + mViewWidth);
        // 当模式是AT_MOST（即wrap_content）时设置默认值
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mViewWidth, mViewHeight);
            // 宽 / 高任意一个模式为AT_MOST（即wrap_content）时，都设置默认值
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mViewWidth, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, mViewHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        mStaticLayout.draw(canvas);
        if (mState == FOLD) {
            if (mIsTextMoreThanMaxLines) {
                drawFold(canvas);
            } else {
                if (!mFollowedHideable) {
                    drawFollowHideable(canvas);
                }
            }
        } else {
            drawExpand(canvas);
        }
    }

    private void initStaticLayout() {
        if (mState == FOLD) {
            if (mIsTextMoreThanMaxLines) {
                mStaticLayout = getFoldStaticLayout();
            } else {
                mStaticLayout = getExpandStaticLayout();
            }
        } else {
            mStaticLayout = getExpandStaticLayout();
        }
        initViewHeight();
    }


    private void initViewHeight() {
        float lastLineWith = mStaticLayout.getLineWidth(mStaticLayout.getLineCount() - 1);
        if (lastLineWith + mFollowedWidth + mEndLineIndent > mViewWidth) {//需要换行
            mViewHeight = (int) (mStaticLayout.getHeight() + mFollowedHeight + mStaticLayout.getSpacingMultiplier() + mStaticLayout.getSpacingAdd());
        } else {
            mViewHeight = Math.max(mStaticLayout.getHeight(), mFollowedHeight);
        }

    }

    //画收起状态下的文本 跟随文本
    private void drawFold(Canvas canvas) {
        clickTextX = mStaticLayout.getWidth() - mFollowedWidth;
        if (mEndingDrawableBitmap != null) {
            clickTextY = mStaticLayout.getHeight() - mFollowedHeight - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
            canvas.drawBitmap(mEndingDrawableBitmap, clickTextX, clickTextY, mTextPaint);
        } else {
            clickTextY = mStaticLayout.getHeight() - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
            canvas.drawText(mFollowedFoldText, clickTextX, clickTextY, mFollowedPaint);
        }
    }

    //画展开状态下的问题 有跟随文本 跟随文本分 末尾对齐 还是跟随对齐
    private void drawExpand(Canvas canvas) {
        float lastLineWith = mStaticLayout.getLineWidth(mStaticLayout.getLineCount() - 1);

        if (lastLineWith + mFollowedWidth + mEndLineIndent > mViewWidth) {//需要换行
            if (mFollowType == END) {
                clickTextX = mViewWidth - mFollowedWidth;
            } else {
                clickTextX = 0;
            }

            if (mEndingDrawableBitmap != null) {
                clickTextY = mStaticLayout.getHeight();
                canvas.drawBitmap(mEndingDrawableBitmap, clickTextX, clickTextY, mTextPaint);
            } else {
                clickTextY = mStaticLayout.getHeight() + mFollowedHeight + mLineSpacingExtraAdd;
                canvas.drawText(mFollowedExpandText, clickTextX, clickTextY, mFollowedPaint);
            }
        } else {
            if (mFollowType == END) {
                clickTextX = mViewWidth - mFollowedWidth;
            } else {
                clickTextX = lastLineWith + mEndLineIndent;
            }

            if (mEndingDrawableBitmap != null) {
                clickTextY = mStaticLayout.getHeight() - mFollowedHeight - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
                canvas.drawBitmap(mEndingDrawableBitmap, clickTextX, clickTextY, mTextPaint);
            } else {
                clickTextY = mStaticLayout.getHeight() - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
                canvas.drawText(mFollowedExpandText, clickTextX, clickTextY, mFollowedPaint);
            }
        }
    }

    //在行数不够MaxLines的情况下 还要显示 跟随文本 有末尾对齐 跟随对齐两种
    private void drawFollowHideable(Canvas canvas) {
        float x, y;
        if (mStaticLayout.getLineWidth(mStaticLayout.getLineCount() - 1) + mFollowedWidth + mEndLineIndent > mViewWidth) {//需要换行
            if (mFollowType == END) {
                x = mViewWidth - mFollowedWidth;
            } else {
                x = 0;
            }
            if (mEndingDrawableBitmap != null) {
                y = mStaticLayout.getHeight() - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1) + mLineSpacingExtraAdd;
                canvas.drawBitmap(mEndingDrawableBitmap, x, y, mTextPaint);
            } else {
                y = mStaticLayout.getHeight() + mFollowedHeight - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1) + mLineSpacingExtraAdd;
                canvas.drawText(mFollowedFoldText, x, y, mFollowedPaint);
            }
        } else {

            if (mFollowType == END) {
                x = mViewWidth - mFollowedWidth;
            } else {
                x = mStaticLayout.getLineWidth(mStaticLayout.getLineCount() - 1) + mEndLineIndent;
            }
            if (mEndingDrawableBitmap != null) {
                y = mStaticLayout.getHeight() - mFollowedHeight - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
                canvas.drawBitmap(mEndingDrawableBitmap, x, y, mTextPaint);
            } else {
                y = mStaticLayout.getHeight() - mStaticLayout.getLineDescent(mStaticLayout.getLineCount() - 1);
                canvas.drawText(mFollowedFoldText, x, y, mFollowedPaint);
            }
        }

    }

    private StaticLayout getFoldStaticLayout() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, mViewWidth);
            builder.setEllipsize(TextUtils.TruncateAt.END);
            builder.setMaxLines(mMaxLines);
            builder.setLineSpacing(mLineSpacingExtraAdd, mLineSpacingExtraMult);
            builder.setEllipsizedWidth((int) (mViewWidth - mFollowedWidth - mEndLineIndent));
            return builder.build();
        } else {
            StaticLayout staticLayout = new StaticLayout(mText, 0, mText.length(), mTextPaint, mViewWidth, Layout.Alignment.ALIGN_NORMAL, mLineSpacingExtraMult, mLineSpacingExtraAdd, false, TextUtils.TruncateAt.END, ((int) (mViewWidth - mFollowedWidth - mEndLineIndent)));
            int lineCount = staticLayout.getLineCount();
            if (lineCount > mMaxLines) {
                staticLayout = new StaticLayout(mText, 0, staticLayout.getLineEnd(mMaxLines - 1), mTextPaint, mViewWidth,
                        Layout.Alignment.ALIGN_NORMAL, mLineSpacingExtraMult, 0f, false, TextUtils.TruncateAt.END, ((int) (mViewWidth - mFollowedWidth - mEndLineIndent)));
            }
            return staticLayout;
        }
    }

    private StaticLayout getExpandStaticLayout() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, mViewWidth);
            builder.setLineSpacing(mLineSpacingExtraAdd, mLineSpacingExtraMult);
            return builder.build();
        } else {
            return new StaticLayout(mText, 0, mText.length(), mTextPaint, mViewWidth, Layout.Alignment.ALIGN_NORMAL, mLineSpacingExtraMult, mLineSpacingExtraAdd, false);
        }
    }

    //判断显示文本行数是否大于maxLine
    private void initIsTextMoreThanMaxLines() {
        int num = getExpandStaticLayout().getLineCount();
        if (num > mMaxLines) {
            mIsTextMoreThanMaxLines = true;
        }
        initFollowedSize();
    }

    //初始化 收起状态下 跟随文字的大小
    private void initFoldTextViewSize() {
        mRect = new Rect();
        mFollowedPaint.getTextBounds(mFollowedFoldText, 0, mFollowedFoldText.length(), mRect);
        mFollowedFoldTextWidth = mRect.width();
        mFollowedFoldTextHeight = mRect.height();
    }

    //初始化 展开状态下 跟随文字的大小
    private void initExpendTextViewSize() {
        mRect = new Rect();
        mFollowedPaint.getTextBounds(mFollowedExpandText, 0, mFollowedExpandText.length(), mRect);
        mFollowedExpandTextWidth = mRect.width();
        mFollowedExpandTextHeight = mRect.height();
    }

    //初始化文本画笔
    private void initTextPaint() {
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStyle(Paint.Style.FILL);
    }

    //初始化跟随文本画笔
    private void initFollowTextPaint() {
        mFollowedPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mFollowedPaint.setTextSize(mFollowedTextSize);
        mFollowedPaint.setColor(mFollowedTextColor);
        mFollowedPaint.setStyle(Paint.Style.FILL);
    }

    private void initFollowedSize() {
        if (!TextUtils.isEmpty(mFollowedExpandText) && !TextUtils.isEmpty(mFollowedFoldText)) {
            if (mIsTextMoreThanMaxLines) {
                if (mState == FOLD) {
                    mFollowedWidth = mFollowedFoldTextWidth;
                    mFollowedHeight = mFollowedFoldTextHeight;
                } else {
                    mFollowedWidth = mFollowedExpandTextWidth;
                    mFollowedHeight = mFollowedExpandTextHeight;
                }
            } else {
                if (!mFollowedHideable) {
                    mFollowedWidth = mFollowedFoldTextWidth;
                    mFollowedHeight = mFollowedFoldTextHeight;
                } else {
                    mFollowedWidth = 0;
                    mFollowedHeight = 0;
                }
            }

        } else if (mEndingDrawableBitmap != null) {
            if (mEndDrawableWidth == 0 || mEndDrawableHeight == 0) {
                mFollowedWidth = mEndingDrawableBitmap.getWidth();
                mFollowedHeight = mEndingDrawableBitmap.getHeight();
            } else {
                mFollowedWidth = mEndDrawableWidth;
                mFollowedHeight = mEndDrawableHeight;
            }
        } else {
            mFollowedWidth = 0;
            mFollowedHeight = 0;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (mEndingDrawableBitmap != null) {
                if (x >= clickTextX - 20 && x <= clickTextX + mFollowedWidth + 20 && y <= clickTextY + mFollowedHeight && y >= clickTextY - 20) {
                    if (this.onFollowedClickListener == null) {
                        changeState();
                    } else {
                        this.onFollowedClickListener.onClick(mState);
                    }
                }
            } else {
                if (x >= clickTextX - 20 && x <= clickTextX + mFollowedWidth + 20 && y >= clickTextY - mFollowedHeight && y <= clickTextY + 20) {
                    if (this.onFollowedClickListener == null) {
                        changeState();
                    } else {
                        this.onFollowedClickListener.onClick(mState);
                    }
                }
            }

        }
        return super.onTouchEvent(event);
    }

    public void setState(int state) {
        if (mState != state) {
            this.mState = state;
            initStaticLayout();
            this.requestLayout();
        }
    }

    //状态切换
    public void changeState() {
        if (mState == FOLD) {
            mState = EXPAND;
        } else {
            mState = FOLD;
        }
        initStaticLayout();
        this.requestLayout();
    }

    public int getState() {
        return mState;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public void setOnFollowedClickListener(OnFollowedClickListener onFollowedClickListener) {
        this.onFollowedClickListener = onFollowedClickListener;
    }

    public interface OnFollowedClickListener {
        //点击时的状态
        void onClick(int state);
    }

}
