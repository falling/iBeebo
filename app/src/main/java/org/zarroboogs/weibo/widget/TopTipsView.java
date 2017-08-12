package org.zarroboogs.weibo.widget;

import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.bean.data.DataItem;
import org.zarroboogs.weibo.bean.data.DataListItem;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class TopTipsView extends android.support.v7.widget.AppCompatTextView {

    // always show toptipbar, or hide itself when handling out of date data (for
    // example user scroll down listview)
    public enum Type {
        ALWAYS, AUTO
    }

    private TreeSet<Long> ids = null;

    private boolean disappear = false;

    private Runnable lastRunnable;

    private OnChangeListener onChangeListener;

    private Type type;

    private static class TopTipBarComparator implements Comparator<Long>,
            Serializable {
        private static final long serialVersionUID = -7797951527481522498L;

        @Override
        public int compare(Long a, Long b) {
            Long resultL = a - b;
            int result = 0;
            if (resultL > 0L) {
                result = 1;
            } else if (resultL < 0L) {
                result = -1;
            }
            return result;
        }
    }

    public interface OnChangeListener {

        void onChange(int count);

    }

    public void setOnChangeListener(OnChangeListener l) {
        this.onChangeListener = l;
        this.onChangeListener.onChange(ids.size());

    }

    public TopTipsView(Context context) {
        this(context, null);
    }

    public TopTipsView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public TopTipsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        type = Type.AUTO;
        ids = new TreeSet<>(new TopTipBarComparator());
    }

    public void setType(Type type) {
        this.type = type;
        setCount();
    }

    public TreeSet<Long> getValues() {
        TreeSet<Long> copy = new TreeSet<>();
        copy.addAll(this.ids);
        return copy;
    }

    /**
     * the json library gson has a bug, may convert database value to Double
     * type, so I have to force cast to Long type;
     */
    public void setValue(Set<Long> values) {
        this.ids.clear();
        for (Long object : values) {
            this.ids.add(object);
        }

        this.disappear = false;
        setCount();
    }

    public void setValue(DataListItem<?, ?> listData, boolean disappear) {
        this.disappear = disappear;
        List<? extends DataItem> values = listData.getItemList();
        for (DataItem b : values) {
            if (b != null) {
                ids.add(b.getIdLong());
            }
        }
        setCount();
        if (disappear) {
            disappear(3000);
            ids.clear();
        }

        if (this.onChangeListener != null) {
            this.onChangeListener.onChange(ids.size());
        }
    }

    private void disappear(int duration) {
        if (getVisibility() == View.INVISIBLE || getVisibility() == View.GONE) {
            return;
        }
        if (lastRunnable != null) {
            Handler handler = getHandler();
            if (handler != null) {
                handler.removeCallbacks(lastRunnable);
            }
        }
        lastRunnable = new Runnable() {
            @Override
            public void run() {
                animate().alpha(0).setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                setVisibility(View.INVISIBLE);
                                animate().alpha(1.0f).setListener(null);
                                if (ids.size() > 0) {
                                    setCount();
                                }
                            }
                        });
            }

        };
        Handler handler = getHandler();
        if (handler != null) {
            handler.postDelayed(lastRunnable, duration);
        }
    }

    private void setCount() {

        int count = ids.size();
        if (count > 0) {
            setVisibility(View.VISIBLE);
            setText(String.format(getContext().getString(R.string.new_messages_count), new Object[]{String.valueOf(ids.size())}));
            setBackgroundResource(R.color.top_tip_bar_tip);
        } else {
            disappear(0);
        }
    }

    // helperId can be used to keep TopTipBar stay Visible status
    public void handle(long id, long helperId) {
        if (disappear || id == 0L) {
            return;
        }

        NavigableSet<Long> tmp = ids.headSet(id, true);
        if (tmp.size() > 0) {
            tmp.clear();
            setCount();

            if (this.onChangeListener != null) {
                this.onChangeListener.onChange(ids.size());
            }
        }

        if (type == Type.ALWAYS) {
            return;
        }

        if (helperId == 0L) {
            return;
        }
        if (ids.contains(helperId)) {
            setCount();
        } else {
            setVisibility(View.INVISIBLE);
        }

    }

    public void clearAndReset() {
        if (disappear) {
            return;
        }
        ids.clear();
        disappear(0);
        if (this.onChangeListener != null) {
            this.onChangeListener.onChange(ids.size());
        }
    }

    public void setError(String error) {
        this.disappear = true;
        setVisibility(View.VISIBLE);
        animate().alpha(1.0f);
        setText(error);
        disappear(3000);
        setBackgroundResource(R.color.top_tip_bar_error);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        // end

        ss.ids = this.ids;
        ss.disappear = this.disappear;
        ss.visible = this.isShown();
        ss.type = this.type;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.ids = ss.ids;
        this.disappear = ss.disappear;
        this.type = ss.type;
        if (ss.visible) {
            setVisibility(View.VISIBLE);
        }
    }

    static class SavedState extends BaseSavedState {

        TreeSet<Long> ids;

        boolean disappear;

        boolean visible;

        Type type;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unchecked")
        private SavedState(Parcel in) {
            super(in);
            Bundle bundle = in.readBundle();
            this.ids = (TreeSet<Long>) bundle.getSerializable("ids");
            this.type = (Type) bundle.getSerializable("type");
            boolean[] disappearArray = new boolean[2];
            in.readBooleanArray(disappearArray);
            this.disappear = disappearArray[0];
            this.visible = disappearArray[1];
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            Bundle bundle = new Bundle();
            bundle.putSerializable("ids", ids);
            bundle.putSerializable("type", type);
            out.writeBundle(bundle);
            out.writeBooleanArray(new boolean[]{this.disappear, this.visible});
        }

        // required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
