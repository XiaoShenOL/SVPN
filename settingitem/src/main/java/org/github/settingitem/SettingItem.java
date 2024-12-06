package org.github.settingitem;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.CheckBox;
import android.widget.Switch;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class SettingItem extends RelativeLayout {
    /*左侧图标*/
    private Drawable mLeftIcon;
    /*右侧图标*/
    private Drawable mRightIcon;
    /*右侧显示文本大小*/
    private float mRightTextSize;
    /*右侧显示文本颜色*/
    private int mRightTextColor;
    /*整体根布局view*/
    private View mView;
    /*根布局*/
    private RelativeLayout mRootLayout;
    /*左侧文本控件*/
    private TextView mTvTitle;
	/*左侧第二行文本控件*/
    private TextView mTvSubTitle;
    /*右侧文本控件*/
    private TextView mTvRightText;
    /*分割线*/
    private View mUnderLine;
    /*左侧图标控件*/
    private ImageView mIvLeftIcon;
    /*左侧图标大小*/
    private int mLeftIconSzie;
    /*右侧图标控件区域,默认展示图标*/
    private FrameLayout mRightLayout;
    /*右侧图标控件,默认展示图标*/
    private ImageView mIvRightIcon;
    /*右侧图标控件,选择样式图标*/
    private CheckBox mRightIcon_check;
    /*右侧图标控件,开关样式图标*/
    private Switch mRightIcon_switch;
    /*右侧图标展示风格*/
    private int mRightStyle = 0;
    /*选中状态*/
    private boolean mChecked;
    /*点击事件*/
    private OnItemClickListener mOnItemClickListener;
    /*长按事件*/
    private OnItemLongClickListener mOnItemLongClickListener;

    public SettingItem(Context context) {
        this(context, null);
    }

    public SettingItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        getCustomStyle(context, attrs);
        //获取到右侧展示风格，进行样式切换
        switchRightStyle(mRightStyle);
        mRootLayout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemClick();
				}
			});
        mRightIcon_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mOnItemClickListener!=null) {
						mOnItemClickListener.onItemClick(mView,isChecked);
					}
				}
			});
        mRightIcon_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mOnItemClickListener!=null) {
						mOnItemClickListener.onItemClick(mView,isChecked);
					}
				}
			});
    }

	public interface OnItemClickListener {
        public void onItemClick(View v, boolean isChecked);
    }

	public interface OnItemLongClickListener {
        void onItemLongClick(View v);
    }

    public void setOnItemClickListener(OnItemClickListener OnItemClickListener) {
        this.mOnItemClickListener = OnItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener OnItemLongClickListener) {
        this.mOnItemLongClickListener = OnItemLongClickListener;
		mRootLayout.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if(mOnItemLongClickListener != null) {
						mOnItemLongClickListener.onItemLongClick(mView);
					}
					//表示此事件已经消费，不会触发单击事件
					return true;
				}
			});
    }

    /**
     * 初始化自定义属性
     *
     * @param context
     * @param attrs
     */
    public void getCustomStyle(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.setting_item);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.setting_item_title) {
				setTitle(a.getString(attr));
            } else if (attr == R.styleable.setting_item_subTitle) {
            	setSubTitle(a.getString(attr));
            } else if (attr == R.styleable.setting_item_titleIcon) {
                // 左侧图标
                mLeftIcon = a.getDrawable(attr);
                if (null != mLeftIcon) {
                    mIvLeftIcon.setImageDrawable(mLeftIcon);
                    mIvLeftIcon.setVisibility(View.VISIBLE);
                }
            } else if (attr == R.styleable.setting_item_titleIconSize) {
                mLeftIconSzie = (int) a.getDimension(attr, 16);
                RelativeLayout.LayoutParams layoutParams = (LayoutParams) mIvLeftIcon.getLayoutParams();
                layoutParams.width = mLeftIconSzie;
                layoutParams.height = mLeftIconSzie;
                mIvLeftIcon.setLayoutParams(layoutParams);
            } else if (attr == R.styleable.setting_item_title_MarginLeft) {
                int leftMargin = (int) a.getDimension(attr, 16);
				//设置主标题左边距
                RelativeLayout.LayoutParams title = (LayoutParams) mTvTitle.getLayoutParams();
                title.leftMargin = leftMargin;
                mTvTitle.setLayoutParams(title);
				//副标题跟随主标题
				RelativeLayout.LayoutParams subtitle = (LayoutParams) mTvSubTitle.getLayoutParams();
                subtitle.leftMargin = leftMargin;
				mTvSubTitle.setLayoutParams(subtitle);
            } else if (attr == R.styleable.setting_item_rightIcon) {
                // 右侧图标
                mRightIcon = a.getDrawable(attr);
                mIvRightIcon.setImageDrawable(mRightIcon);
            } else if (attr == R.styleable.setting_item_titleSize) {
                // 默认设置为16sp
                float textSize = a.getFloat(attr, 16);
                mTvTitle.setTextSize(textSize);
				//副标题跟随主标题 比主标题小 2
				mTvSubTitle.setTextSize(textSize-2);

            } else if (attr == R.styleable.setting_item_titleColor) {
                //文字默认灰色
				setTitleColor(a.getColor(attr, Color.LTGRAY));
            } else if (attr == R.styleable.setting_item_rightStyle) {
                mRightStyle = a.getInt(attr, 0);
            } else if (attr == R.styleable.setting_item_isShowUnderLine) {
                //默认显示分割线
                if (!a.getBoolean(attr, true)) {
                    mUnderLine.setVisibility(View.GONE);
                }
            } else if (attr == R.styleable.setting_item_rightText) {
                setRightText(a.getString(attr));
            } else if (attr == R.styleable.setting_item_rightTextSize) {

                // 默认设置为16sp
                mRightTextSize = a.getFloat(attr, 14);
                mTvRightText.setTextSize(mRightTextSize);
            } else if (attr == R.styleable.setting_item_rightTextColor) {
                //文字默认灰色
                mRightTextColor = a.getColor(attr, Color.GRAY);
                mTvRightText.setTextColor(mRightTextColor);
            }
        }
        a.recycle();
    }

    /**
     * 根据设定切换右侧展示样式，同时更新点击事件处理方式
     *
     * @param mRightStyle
     */
    private void switchRightStyle(int mRightStyle) {
        switch (mRightStyle) {
            case 0:
                //默认展示样式，只展示一个图标
                mIvRightIcon.setVisibility(View.VISIBLE);
                mRightIcon_check.setVisibility(View.GONE);
                mRightIcon_switch.setVisibility(View.GONE);
                break;
            case 1:
                //隐藏右侧图标
                mRightLayout.setVisibility(View.INVISIBLE);
                mRightLayout.getLayoutParams().width = 38;//多加一行这个将文字设置靠右对齐即可
                break;
            case 2:
                //显示选择框样式
                mIvRightIcon.setVisibility(View.GONE);
                mRightIcon_check.setVisibility(View.VISIBLE);
                mRightIcon_switch.setVisibility(View.GONE);
                break;
            case 3:
                //显示开关切换样式
                mIvRightIcon.setVisibility(View.GONE);
                mRightIcon_check.setVisibility(View.GONE);
                mRightIcon_switch.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void initView(Context context) {
        mView = View.inflate(context, R.layout.settingitem, this);
        mRootLayout = (RelativeLayout) mView.findViewById(R.id.rootLayout);
        mUnderLine = (View) mView.findViewById(R.id.underline);
        mTvTitle = (TextView) mView.findViewById(R.id.tv_left_title);
		mTvSubTitle = (TextView) mView.findViewById(R.id.tv_left_subtitle);
		mTvRightText = (TextView) mView.findViewById(R.id.tv_righttext);
        mIvLeftIcon = (ImageView) mView.findViewById(R.id.iv_lefticon);
        mIvRightIcon = (ImageView) mView.findViewById(R.id.iv_righticon);
        mRightLayout = (FrameLayout) mView.findViewById(R.id.rightlayout);
        mRightIcon_check = (CheckBox) mView.findViewById(R.id.rightcheck);
        mRightIcon_switch = (Switch) mView.findViewById(R.id.rightswitch);
    }

    /**
     * 处理点击事件
     */
    private void onItemClick() {
        switch (mRightStyle) {
            case 0:
            case 1:
                if (null != mOnItemClickListener) {
                    mOnItemClickListener.onItemClick(mView,mChecked);
                }
                break;
            case 2:
                //选择框切换选中状态
                mRightIcon_check.setChecked(!mRightIcon_check.isChecked());
                mChecked = mRightIcon_check.isChecked();
                break;
            case 3:
                //开关切换状态
                mRightIcon_switch.setChecked(!mRightIcon_switch.isChecked());
                mChecked = mRightIcon_switch.isChecked();
                break;
        }
    }

    /**
     * 获取根布局对象
     *
     * @return
     */
    public RelativeLayout getmRootLayout() {
        return mRootLayout;
    }

	/**
     * 设置选中状态
     *
     * @return
     */
	public void setChecked(boolean isChecked) {
		//选择框切换选中状态
		mRightIcon_check.setChecked(isChecked);
		mRightIcon_switch.setChecked(isChecked);
    }

    /**
     * 获取选中状态
     *
     * @return
     */
    public boolean isChecked() {
        return !mChecked;
    }

    /**
     * 更改左侧文字
     */
    public void setTitle(String info) {
        mTvTitle.setText(info);
    }

	/**
     * 获得左侧文字
     */
    public String getTitle() {
        return mTvTitle.getText().toString();
    }

	/**
     * 更改左侧文字颜色
     */
    public void setTitleColor(int color) {
        mTvTitle.setTextColor(color);
    }

    /**
     * 更改左侧第二行文字
     */
    public void setSubTitle(String info) {
        mTvSubTitle.setText(info);
		mTvSubTitle.setVisibility(View.VISIBLE);
    }

	/**
     * 获得左侧第二行文字
     */
    public String getSubTitle() {
        return mTvSubTitle.getText().toString();
    }

	/**
     * 更改左侧第二行文字颜色
     */
    public void setSubTitleColor(int color) {
        mTvSubTitle.setTextColor(color);
    }

    /**
     * 更改右侧文字
     */
    public void setRightText(String info) {
        mTvRightText.setText(info);
		mTvRightText.setVisibility(View.VISIBLE);
    }

	/**
     * 获得右侧文字
     */
    public String getRightText() {
        return mTvRightText.getText().toString();
    }

	/**
     * 更改右侧文字颜色
     */
    public void setRightTextColor(int color) {
        mTvRightText.setTextColor(color);
    }

}

