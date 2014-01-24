package info.guardianproject.yakreader.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import info.guardianproject.yakreader.R;
import info.guardianproject.yakreader.models.ViewPagerIndicator;

public class DottedProgressView extends View implements ViewPagerIndicator
{
	private Paint mPaint;
	private int mGravity;
	private int mDistance; // Distance between dots
	private int mRadius; // Size of a dot
	private int mColor; // Dot color
	private int mRimColor; // Dot rim color
	private int mColorCurrent; // Dot color
	private int mRimColorCurrent; // Dot rim color
	private int mNumDots; // Total number of dots
	private int mCurrentDot; // Current dot
	private boolean mHideIfOnlyOne = true;

	public DottedProgressView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	public DottedProgressView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public DottedProgressView(Context context)
	{
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		mGravity = Gravity.CENTER;

		if (attrs != null)
		{
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DottedProgressView);
			mGravity = a.getInt(R.styleable.DottedProgressView_android_gravity, Gravity.CENTER);
			mDistance = a.getInt(R.styleable.DottedProgressView_dot_distance, 21);
			mRadius = a.getInt(R.styleable.DottedProgressView_dot_radius, 5);
			mColor = a.getColor(R.styleable.DottedProgressView_dot_color, Color.TRANSPARENT);
			mRimColor = a.getColor(R.styleable.DottedProgressView_dot_rim_color, 0xff888888);
			mColorCurrent = a.getColor(R.styleable.DottedProgressView_dot_color_current, 0xff888888);
			mRimColorCurrent = a.getColor(R.styleable.DottedProgressView_dot_rim_color_current, 0xff888888);
			mNumDots = a.getInt(R.styleable.DottedProgressView_dot_total, 3);
			mCurrentDot = a.getInt(R.styleable.DottedProgressView_dot_current, 1);
			mHideIfOnlyOne = a.getBoolean(R.styleable.DottedProgressView_hide_if_only_one, true);
			a.recycle();
		}
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(mColor);
		mPaint.setStrokeWidth(1);
	}

	/*
	 * Get the total number of dots for the view.
	 */
	public int getNumberOfDots()
	{
		return mNumDots;
	};

	/*
	 * Set the total number of dots for the view.
	 */
	public void setNumberOfDots(int dots)
	{
		if (dots != mNumDots)
		{
			mNumDots = dots;
			invalidate();
		}
	}

	/*
	 * Get the current dot index for the view (0-indexed).
	 */
	public int getCurrentDot()
	{
		return mCurrentDot;
	};

	/*
	 * Set the current dot index for the view (0-indexed).
	 */
	public void setCurrentDot(int index)
	{
		if (index != mCurrentDot)
		{
			mCurrentDot = index;
			invalidate();
		}
	}

	/*
	 * Calculate the number of dots we can fit in the screen space we are given.
	 */
	private int getMaxNumberOfDots()
	{
		if (mRadius < 1 || mDistance <= (2 * mRadius))
			return 0;

		int w = this.getWidth();
		w -= 2 * mRadius;

		int n = 1 + w / mDistance;
		if (n < 1)
			return 0;
		return n;
	}

	private int getDotXPosition(int dot, int numToDraw)
	{
		if (mGravity == Gravity.RIGHT)
		{
			int x = getWidth() - mDistance;
			int widthToDraw = ((numToDraw - 1) * mDistance);
			x -= widthToDraw;
			x += dot * mDistance;
			return x;
		}
		else
		{
			int x = getWidth() / 2;
			int widthToDraw = ((numToDraw - 1) * mDistance);
			x -= widthToDraw / 2;
			x += dot * mDistance;
			return x;
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mNumDots <= 1 && mHideIfOnlyOne)
		{
			return;
		}

		int sc = canvas.save();

		int numDotsToDraw = mNumDots;
		int currentDot = mCurrentDot;

		// If all dots don't fit, show only the remaining pages (see design
		// document)
		//
		int dotsPerPage = getMaxNumberOfDots();
		if (dotsPerPage < mNumDots)
		{
			// Get the current "page" for the current dot
			int page = (int) ((float) mCurrentDot / (dotsPerPage - 1));
			currentDot = (int) ((float) mCurrentDot % (dotsPerPage - 1));
			numDotsToDraw = Math.min(dotsPerPage, 1 + mNumDots - (page * dotsPerPage));
		}

		// Do the draw
		for (int i = 0; i < numDotsToDraw; i++)
		{
			if (i == currentDot)
			{
				mPaint.setStyle(Paint.Style.FILL);
				mPaint.setColor(mColorCurrent);
				canvas.drawCircle(getDotXPosition(i, numDotsToDraw), getHeight() / 2, mRadius, mPaint);
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setColor(mRimColorCurrent);
				canvas.drawCircle(getDotXPosition(i, numDotsToDraw), getHeight() / 2, mRadius, mPaint);
			}
			else
			{
				mPaint.setStyle(Paint.Style.FILL);
				mPaint.setColor(mColor);
				canvas.drawCircle(getDotXPosition(i, numDotsToDraw), getHeight() / 2, mRadius, mPaint);
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setColor(mRimColor);
				canvas.drawCircle(getDotXPosition(i, numDotsToDraw), getHeight() / 2, mRadius, mPaint);
			}
		}

		canvas.restoreToCount(sc);
	}

	@Override
	public void onTotalChanged(int total)
	{
		this.setNumberOfDots(total);
		if (mHideIfOnlyOne)
		{
			if (total <= 1)
				setVisibility(View.INVISIBLE);
			else
				setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCurrentChanged(int current)
	{
		this.setCurrentDot(current);
	}
}
