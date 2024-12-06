package org.github.itoast;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class IToast {
    //全局上下文对象
    public static Context mContext;

    private static Integer warningIcon;
    private static Integer errorIcon;
    private static Integer infoIcon;
    private static Integer successIcon;

    private static Integer duration;

    private  Builder builder;
    private IToast(Builder builder) {
        this.mContext=builder.mContext;
        this.builder=builder;

        this.infoIcon=builder.infoIcon;
        if(infoIcon==null)
            infoIcon= R.drawable.ic_dialog_tip_info;
        this.warningIcon=builder.warningIcon;
        if(warningIcon==null)
            warningIcon= R.drawable.ic_dialog_tip_warning;
        this.errorIcon=builder.errorIcon;
        if(errorIcon==null)
            errorIcon= R.drawable.ic_dialog_tip_error;
        this.successIcon=builder.successIcon;
        if(successIcon==null)
            successIcon= R.drawable.ic_dialog_tip_finish;
        if(builder.duration!=null){
            this.duration=builder.duration;
        }else{
            this.duration=Toast.LENGTH_SHORT;
        }
    }

    public static Builder newBuilder(Context context) {
        return new Builder(context);
    }
    public static class Builder {
        private Context mContext;
        private Integer warningIcon;
        private Integer errorIcon;
        private Integer infoIcon;
        private Integer successIcon;
        private Integer duration;
        public Builder(Context context) {
            this.mContext = context.getApplicationContext();
        }

        public IToast build() {
            return new IToast(this);
        }

        /**
         * 设置字体颜色，智能设置一种
         * @param textColor
         * @return
         */
        public Builder setWarningIcon(int warningIcon) {
            this.warningIcon=warningIcon;
            return this;
        }
        public Builder setErrorIcon(int errorIcon) {
            this.errorIcon=errorIcon;
            return this;
        }
        public Builder setInfoIcon(int infoIcon) {
            this.infoIcon=infoIcon;
            return this;
        }
        public Builder setSuccessIcon(int successIcon) {
            this.successIcon=successIcon;
            return this;
        }

        public Builder setDuration(int duration) {
            this.duration=duration;
            return this;
        }
    }

    private static final String TOAST_TYPEFACE = "sans-serif-condensed";

    /**
     * 上次显示的内容
     */
    private static String oldMsg;
    /**
     * 上次时间
     */
    private static long oldTime = 0;
    /**
     * Toast对象
     */
    private static Toast mToast = null;

    public static Toast warning(String message) {
        Drawable icon = null;
		if (warningIcon!=null) {
			icon = IToast.mContext.getResources().getDrawable(warningIcon);
		}
        if(duration==null)
            duration=Toast.LENGTH_SHORT;
        return custom(message, icon, duration);
    }

    public static Toast info(String message) {
        Drawable icon = null;
		if (infoIcon!=null) {
			icon = IToast.mContext.getResources().getDrawable(infoIcon);
		}
        if(duration==null)
            duration=Toast.LENGTH_SHORT;
        return custom(message, icon, duration);
    }

    public static Toast success(String message) {
        Drawable icon = null;
		if (successIcon!=null) {
			icon = IToast.mContext.getResources().getDrawable(successIcon);
		}
        if(duration==null)
            duration=Toast.LENGTH_SHORT;
        return custom(message, icon, duration);
    }

    public static Toast error(String message) {
        Drawable icon = null;
		if (errorIcon!=null) {
			icon = IToast.mContext.getResources().getDrawable(errorIcon);
		}
        if(duration==null)
            duration=Toast.LENGTH_SHORT;
        return custom(message, icon, duration);
    }

    /**
     * 自定义toast方法
     *
     * @param message   提示消息文本
     * @param icon      提示消息的icon,传入null代表不显示
     * @param duration  显示时长
     * @return
     */
    private static Toast custom(String message, Drawable icon, int duration) {
        View toastLayout = LayoutInflater.from(IToast.mContext).inflate(R.layout.htoast_view, null);
        ImageView toastIcon = (ImageView) toastLayout.findViewById(R.id.htoast_icon);
        TextView toastText = (TextView) toastLayout.findViewById(R.id.htoast_text);

        Drawable drawableFrame = IToast.mContext.getResources().getDrawable(R.drawable.dialog_toast_bg);
        toastLayout.setBackground(drawableFrame);

        if (icon == null) {
            toastIcon.setVisibility(View.GONE);
        } else {
            toastIcon.setImageDrawable(icon);
        }

        toastText.setTextColor(Color.WHITE);
		toastText.setTextSize(16);
        toastText.setText(message);
        toastText.setTypeface(Typeface.create(TOAST_TYPEFACE, Typeface.NORMAL));

        if (mToast == null) {
            mToast = new Toast(IToast.mContext);
            mToast.setView(toastLayout);
            mToast.setGravity(Gravity.CENTER,0,0);
            mToast.setDuration(duration);
            mToast.show();
            oldTime = System.currentTimeMillis();
        } else {
            if (message.equals(oldMsg)) {
                if (System.currentTimeMillis() - oldTime > Toast.LENGTH_SHORT) {
                    mToast.show();
                }
            } else {
                oldMsg = message;
                mToast.setView(toastLayout);
                mToast.setDuration(duration);
                mToast.show();
            }
        }

        oldTime = System.currentTimeMillis();
        return mToast;
    }
}
