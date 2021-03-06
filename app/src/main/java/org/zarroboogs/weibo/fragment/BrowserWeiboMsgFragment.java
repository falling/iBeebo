
package org.zarroboogs.weibo.fragment;

import org.zarroboogs.msrl.widget.MaterialSwipeRefreshLayout;
import org.zarroboogs.util.net.WeiboException;
import org.zarroboogs.utils.Constants;
import org.zarroboogs.utils.file.FileLocationMethod;
import org.zarroboogs.weibo.BeeboApplication;
import org.zarroboogs.weibo.IRemoveItem;
import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.activity.AppMapActivity;
import org.zarroboogs.weibo.activity.BrowserWeiboMsgActivity;
import org.zarroboogs.weibo.activity.UserInfoActivity;
import org.zarroboogs.weibo.adapter.BrowserWeiboMsgCommentAndRepostAdapter;
import org.zarroboogs.weibo.asynctask.GetWeiboLocationInfoTask;
import org.zarroboogs.weibo.asynctask.MyAsyncTask;
import org.zarroboogs.weibo.asynctask.UpdateMessageTask;
import org.zarroboogs.weibo.bean.AsyncTaskLoaderResult;
import org.zarroboogs.weibo.bean.CommentListBean;
import org.zarroboogs.weibo.bean.GeoBean;
import org.zarroboogs.weibo.bean.MessageBean;
import org.zarroboogs.weibo.bean.data.RepostListBean;
import org.zarroboogs.weibo.dao.DestroyCommentDao;
import org.zarroboogs.weibo.fragment.base.BaseStateFragment;
import org.zarroboogs.weibo.loader.CommentsByIdMsgLoader;
import org.zarroboogs.weibo.loader.RepostByIdMsgLoader;
import org.zarroboogs.weibo.setting.SettingUtils;
import org.zarroboogs.weibo.support.asyncdrawable.IWeiboDrawable;
import org.zarroboogs.weibo.support.asyncdrawable.MsgDetailReadWorker;
import org.zarroboogs.weibo.support.asyncdrawable.TimeLineBitmapDownloader;
import org.zarroboogs.weibo.support.gallery.GalleryAnimationActivity;
import org.zarroboogs.weibo.support.lib.AnimationRect;
import org.zarroboogs.weibo.support.lib.ClickableTextViewMentionLinkOnTouchListener;
import org.zarroboogs.weibo.support.utils.AppEventAction;
import org.zarroboogs.weibo.support.utils.ThemeUtility;
import org.zarroboogs.weibo.support.utils.Utility;
import org.zarroboogs.weibo.support.utils.ViewUtility;
import org.zarroboogs.weibo.widget.ProfileTopAvatarImageView;
import org.zarroboogs.weibo.widget.SwipeFrameLayout;
import org.zarroboogs.weibo.widget.WeiboDetailImageView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BrowserWeiboMsgFragment extends BaseStateFragment implements IRemoveItem {

    private MessageBean msg;

    private BrowserWeiboMsgLayout layout;

    private UpdateMessageTask updateMsgTask;

    private GetWeiboLocationInfoTask geoTask;

    private MsgDetailReadWorker picTask;

    private Handler handler = new Handler();

    private ListView listView;

    private BrowserWeiboMsgCommentAndRepostAdapter adapter;

    private CommentListBean commentList = new CommentListBean();

    private RepostListBean repostList = new RepostListBean();

    private TextView repostTab;

    private TextView commentTab;

    private static final int NEW_COMMENT_LOADER_ID = 1;

    private static final int OLD_COMMENT_LOADER_ID = 2;

    private static final int NEW_REPOST_LOADER_ID = 3;

    private static final int OLD_REPOST_LOADER_ID = 4;

    private boolean isCommentList = true;

    private View progressHeader;

    private TextView emptyHeader;

    private BroadcastReceiver sendCommentCompletedReceiver;

    private BroadcastReceiver sendRepostCompletedReceiver;

    private RemoveTask removeTask;

    private boolean canLoadOldCommentData = true;

    private boolean canLoadOldRepostData = true;

    private static class BrowserWeiboMsgLayout {

        TextView username;

        TextView content;

        TextView recontent;

        TextView time;

        TextView location;

        TextView source;

        ImageView mapView;

        ProfileTopAvatarImageView avatar;

        WeiboDetailImageView content_pic;

        GridLayout content_pic_multi;

        WeiboDetailImageView repost_pic;

        GridLayout repost_pic_multi;

        LinearLayout repost_layout;

        TextView comment_count;

        TextView repost_count;

        View count_layout;

    }

    public static BrowserWeiboMsgFragment newInstance(MessageBean msg) {
        return new BrowserWeiboMsgFragment(msg);
    }

    public BrowserWeiboMsgFragment() {
    }

    @SuppressLint("ValidFragment")
    public BrowserWeiboMsgFragment(MessageBean msg) {
        this.msg = msg;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("msg", msg);
        outState.putParcelable("commentList", commentList);
        outState.putParcelable("repostList", repostList);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        switch (getCurrentState(savedInstanceState)) {
            case FIRST_TIME_START:
                if (Utility.isTaskStopped(updateMsgTask)) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateMsgTask = new UpdateMessageTask(BrowserWeiboMsgFragment.this, layout.content,
                                    layout.recontent, msg, false);
                            updateMsgTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }, 2000);
                }
                buildViewData(true);
                loadNewCommentData();
                break;
            case SCREEN_ROTATE:
                // nothing
                buildViewData(true);
                break;
            case ACTIVITY_DESTROY_AND_CREATE:
                msg = savedInstanceState.getParcelable("msg");
                commentList.replaceAll((CommentListBean) savedInstanceState.getParcelable("commentList"));
                repostList.replaceAll((RepostListBean) savedInstanceState.getParcelable("repostList"));
                buildViewData(true);
                adapter.notifyDataSetChanged();
                if (commentList.getSize() > 0) {
                    emptyHeader.setVisibility(View.GONE);
                }
                break;
        }

        Loader loader = getLoaderManager().getLoader(NEW_COMMENT_LOADER_ID);
        if (loader != null) {
            getLoaderManager().initLoader(NEW_COMMENT_LOADER_ID, null, commentMsgCallback);
        }

        loader = getLoaderManager().getLoader(OLD_COMMENT_LOADER_ID);
        if (loader != null) {
            getLoaderManager().initLoader(OLD_COMMENT_LOADER_ID, null, commentMsgCallback);
        }

    }

    // android has a bug,I am tired. I use another color and disable underline
    // for link,but when I open "dont save activity" in
    // developer option,click the link to open another activity, then press
    // back,this fragment is restored,
    // but the link color is restored to android own blue color,not my custom
    // color,the underline appears
    // the workaround is set textview value in onresume() method
    @Override
    public void onResume() {
        super.onResume();
        // buildViewData(false);
        // if (hasGpsInfo())
        // layout.mapView.onResume();
        getListView().setFastScrollEnabled(SettingUtils.allowFastScroll());
        sendCommentCompletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isCommentList) {
                    loadNewCommentData();
                }

            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(sendCommentCompletedReceiver,
                new IntentFilter(AppEventAction.buildSendCommentOrReplySuccessfullyAction(msg)));

        sendRepostCompletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isCommentList) {
                    loadNewRepostData();
                }

            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(sendRepostCompletedReceiver,
                new IntentFilter(AppEventAction.buildSendRepostSuccessfullyAction(msg)));

    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(sendCommentCompletedReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(sendRepostCompletedReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utility.cancelTasks(updateMsgTask, geoTask, picTask);

        layout.avatar.setImageDrawable(null);
        layout.content_pic.setImageDrawable(null);
        layout.repost_pic.setImageDrawable(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SwipeFrameLayout swipeFrameLayout = (SwipeFrameLayout) inflater.inflate(R.layout.browser_weibo_msg_layout,
                container, false);

        MaterialSwipeRefreshLayout swipeRefreshLayout = ViewUtility.findViewById(swipeFrameLayout, R.id.browserWeiboMsgSRL);
        swipeRefreshLayout.setEnableSount(SettingUtils.getEnableSound());
        swipeRefreshLayout.setOnlyLoadMore();

        listView = ViewUtility.findViewById(swipeFrameLayout, R.id.pullToFreshView);

//        pullToRefreshListView.setOnLastItemVisibleListener(onLastItemVisibleListener);
        listView.setOnScrollListener(listViewOnScrollListener);

//        listView = pullToRefreshListView.getRefreshableView();

        View header = inflater.inflate(R.layout.browserweibomsgfragment_layout, listView, false);
        listView.addHeaderView(header);

        View switchView = inflater.inflate(R.layout.browserweibomsgfragment_switch_list_type_header, listView, false);
        listView.addHeaderView(switchView);

        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // empty
            }
        });

        View progressHeaderLayout = inflater.inflate(R.layout.browserweibomsgfragment_progress_header, listView, false);
        progressHeader = progressHeaderLayout.findViewById(R.id.listLoadMoreProgress);
        progressHeader.setVisibility(View.GONE);
        listView.addHeaderView(progressHeaderLayout);

        View emptyLayout = inflater.inflate(R.layout.browserweibomsgfragment_empty_header, listView, false);
        emptyHeader = (TextView) emptyLayout.findViewById(R.id.empty_text);
        emptyHeader.setOnClickListener(new EmptyHeaderOnClickListener());
        listView.addHeaderView(emptyLayout);

        dismissFooterView();

        repostTab = (TextView) switchView.findViewById(R.id.repost);
        commentTab = (TextView) switchView.findViewById(R.id.comment);

        repostTab.setOnClickListener(new RepostTabOnClickListener());
        commentTab.setOnClickListener(new CommentTabOnClickListener());

        commentTab.setTextColor(getResources().getColor(R.color.orange));
        listView.setFooterDividersEnabled(false);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(commentOnItemClickListener);
        // listView.setOnItemLongClickListener(commentOnItemLongClickListener);

        initView(header, savedInstanceState);
        adapter = new BrowserWeiboMsgCommentAndRepostAdapter(this, listView, commentList.getItemList(),
                repostList.getItemList());
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        listView.setHeaderDividersEnabled(false);

        return swipeFrameLayout;
    }

    private void initView(View view, Bundle savedInstanceState) {
        layout = new BrowserWeiboMsgLayout();
        layout.username = (TextView) view.findViewById(R.id.username);
        layout.content = (TextView) view.findViewById(R.id.content);
        layout.recontent = (TextView) view.findViewById(R.id.repost_content);
        layout.time = (TextView) view.findViewById(R.id.time);
        layout.location = (TextView) view.findViewById(R.id.location);
        layout.source = (TextView) view.findViewById(R.id.source);

        layout.mapView = (ImageView) view.findViewById(R.id.map);

        layout.comment_count = (TextView) view.findViewById(R.id.comment_count);
        layout.repost_count = (TextView) view.findViewById(R.id.repost_count);
        layout.count_layout = view.findViewById(R.id.count_layout);

        layout.avatar = (ProfileTopAvatarImageView) view.findViewById(R.id.avatar);
        layout.content_pic = (WeiboDetailImageView) view.findViewById(R.id.content_pic);
        layout.content_pic_multi = (GridLayout) view.findViewById(R.id.content_pic_multi);
        layout.repost_pic = (WeiboDetailImageView) view.findViewById(R.id.repost_content_pic);
        layout.repost_pic_multi = (GridLayout) view.findViewById(R.id.repost_content_pic_multi);

        layout.repost_layout = (LinearLayout) view.findViewById(R.id.repost_layout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layout.location.setOnClickListener(locationInfoOnClickListener);
        view.findViewById(R.id.first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                intent.putExtra(Constants.TOKEN, BeeboApplication.getInstance().getAccessTokenHack());
                intent.putExtra("user", msg.getUser());
                startActivity(intent);
            }
        });
        layout.recontent.setOnClickListener(repostContentOnClickListener);
    }

    public void buildViewData(final boolean refreshPic) {
        layout.avatar.checkVerified(msg.getUser());
        if (msg.getUser() != null) {
            if (TextUtils.isEmpty(msg.getUser().getRemark())) {
                layout.username.setText(msg.getUser().getScreen_name());
            } else {
                layout.username.setText(msg.getUser().getScreen_name() + "(" + msg.getUser().getRemark() + ")");
            }

            TimeLineBitmapDownloader.getInstance().downloadAvatar(layout.avatar.getImageView(), msg.getUser());
        }
        layout.content.setText(msg.getListViewSpannableString());
        layout.content.setOnTouchListener(new ClickableTextViewMentionLinkOnTouchListener());

        layout.time.setText(msg.getTimeInFormat());

        if (msg.getGeo() != null && msg.getGeo().getLat() != 0.0 && msg.getGeo().getLon() != 0.0) {
            layout.mapView.setVisibility(View.VISIBLE);
            if (Utility.isTaskStopped(geoTask)) {
                geoTask = new GetWeiboLocationInfoTask(getActivity(), msg.getGeo(), layout.mapView, layout.location);
                geoTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            layout.mapView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(msg.getSource())) {
            layout.source.setText(Html.fromHtml(msg.getSource()).toString());
        }

        layout.content_pic.setVisibility(View.GONE);
        layout.content_pic_multi.setVisibility(View.GONE);

        // sina weibo official account can send repost message with picture,
        // fuck sina weibo
        if (msg.havePicture() && msg.getRetweeted_status() == null) {
            displayPictures(msg, layout.content_pic_multi, layout.content_pic, refreshPic);
        }

        final MessageBean repostMsg = msg.getRetweeted_status();

        layout.repost_layout.setVisibility(repostMsg != null ? View.VISIBLE : View.GONE);

        if (repostMsg != null) {
            // sina weibo official account can send repost message with picture,
            // fuck sina weibo
            layout.content_pic.setVisibility(View.GONE);

            layout.repost_layout.setVisibility(View.VISIBLE);
            layout.recontent.setVisibility(View.VISIBLE);
            layout.recontent.setOnTouchListener(new ClickableTextViewMentionLinkOnTouchListener());
            if (repostMsg.getUser() != null) {
                layout.recontent.setText(repostMsg.getListViewSpannableString());
                buildRepostCount();
            } else {
                layout.recontent.setText(repostMsg.getListViewSpannableString());
            }

            layout.repost_pic.setVisibility(View.GONE);
            layout.repost_pic_multi.setVisibility(View.GONE);

            if (repostMsg.havePicture()) {
                displayPictures(repostMsg, layout.repost_pic_multi, layout.repost_pic, refreshPic);
            }
        }

        Utility.buildTabCount(commentTab, getString(R.string.comments), msg.getComments_count());
        Utility.buildTabCount(repostTab, getString(R.string.repost), msg.getReposts_count());

        ((BrowserWeiboMsgActivity) getActivity()).updateCommentCount(msg.getComments_count());
        ((BrowserWeiboMsgActivity) getActivity()).updateRepostCount(msg.getReposts_count());

    }

    private void displayPictures(final MessageBean msg, final GridLayout layout, WeiboDetailImageView view,
                                 boolean refreshPic) {

        if (!msg.isMultiPics()) {
            view.setVisibility(View.VISIBLE);
            if (Utility.isTaskStopped(picTask) && refreshPic) {
                picTask = new MsgDetailReadWorker(view, msg);
                picTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                picTask.setView(view);
            }
        } else {
            layout.setVisibility(View.VISIBLE);

            final int count = msg.getPicCount();
            for (int i = 0; i < count; i++) {
                final IWeiboDrawable pic = (IWeiboDrawable) layout.getChildAt(i);
                pic.setVisibility(View.VISIBLE);

                if (SettingUtils.getEnableBigPic()) {
                    TimeLineBitmapDownloader.getInstance().displayMultiPicture(pic, msg.getHighPicUrls().get(i),
                            FileLocationMethod.picture_large);
                } else {
                    TimeLineBitmapDownloader.getInstance().displayMultiPicture(pic, msg.getMiddlePicUrls().get(i),
                            FileLocationMethod.picture_bmiddle);
                }

                final int finalI = i;
                pic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        ArrayList<AnimationRect> animationRectArrayList = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            final IWeiboDrawable pic = (IWeiboDrawable) layout.getChildAt(i);
                            ImageView imageView = (ImageView) pic;
                            if (imageView.getVisibility() == View.VISIBLE) {
                                AnimationRect rect = AnimationRect.buildFromImageView(imageView);
                                animationRectArrayList.add(rect);
                            }
                        }
                        Intent intent = GalleryAnimationActivity.newIntent(msg, animationRectArrayList, finalI);
                        getActivity().startActivity(intent);
                    }
                });

            }

            if (count < 9) {
                for (int i = count; i < 9; i++) {
                    ImageView pic = (ImageView) layout.getChildAt(i);
                    pic.setVisibility(View.GONE);
                }
            }
        }

    }

    private void buildRepostCount() {
        MessageBean repostBean = msg.getRetweeted_status();

        if (repostBean.getComments_count() == 0 && repostBean.getReposts_count() == 0) {
            layout.count_layout.setVisibility(View.GONE);
            return;
        } else {
            layout.count_layout.setVisibility(View.VISIBLE);
        }

        if (repostBean.getComments_count() > 0) {
            layout.comment_count.setVisibility(View.VISIBLE);
            layout.comment_count.setText(String.valueOf(repostBean.getComments_count()));
        } else {
            layout.comment_count.setVisibility(View.GONE);
        }

        if (repostBean.getReposts_count() > 0) {
            layout.repost_count.setVisibility(View.VISIBLE);
            layout.repost_count.setText(String.valueOf(repostBean.getReposts_count()));
        } else {
            layout.repost_count.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            if (Utility.isTaskStopped(updateMsgTask)) {
                updateMsgTask = new UpdateMessageTask(BrowserWeiboMsgFragment.this, layout.content, layout.recontent,
                        msg, true);
                updateMsgTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
            if (isCommentList) {
                loadNewCommentData();
            } else {
                loadNewRepostData();
            }
        }
        return false;
    }

    protected void showFooterView() {
    }

    protected void dismissFooterView() {
    }

    protected void showErrorFooterView() {
    }

    // only can remove comment
    @Override
    public void removeItem(int position) {
        if (!isCommentList) {
            return;
        }
        if (removeTask == null || removeTask.getStatus() == MyAsyncTask.Status.FINISHED) {
            removeTask = new RemoveTask(BeeboApplication.getInstance().getAccessTokenHack(), commentList.getItemList()
                    .get(position).getId(), position);
            removeTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void removeCancel() {
    }

    private ListView getListView() {
        return listView;
    }

    private View.OnClickListener repostContentOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // This condition will satisfy only when it is not an autolinked
            // text
            // onClick action
            boolean isNotLink = layout.recontent.getSelectionStart() == -1 && layout.recontent.getSelectionEnd() == -1;
            boolean isDeleted = msg.getRetweeted_status() == null || msg.getRetweeted_status().getUser() == null;

            if (isNotLink && !isDeleted) {
                startActivity(BrowserWeiboMsgActivity.newIntent(BeeboApplication.getInstance().getAccountBean(),
                        msg.getRetweeted_status(), BeeboApplication
                                .getInstance().getAccessTokenHack()));
            } else if (isNotLink) {
                Toast.makeText(getActivity(), getString(R.string.cant_open_deleted_weibo), Toast.LENGTH_SHORT).show();
            }

        }
    };

    private View.OnClickListener locationInfoOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Utility.isGooglePlaySafe(getActivity())) {
                GeoBean bean = msg.getGeo();
                Intent intent = new Intent(getActivity(), AppMapActivity.class);
                intent.putExtra("lat", bean.getLat());
                intent.putExtra("lon", bean.getLon());
                if (!String.valueOf(bean.getLat() + "," + bean.getLon()).equals(layout.location.getText())) {
                    intent.putExtra("locationStr", layout.location.getText());
                }
                startActivity(intent);
            } else {
                GeoBean bean = msg.getGeo();
                String geoUriString = "geo:" + bean.getLat() + "," + bean.getLon() + "?q=" + layout.location.getText();
                Uri geoUri = Uri.parse(geoUriString);
                Intent mapCall = new Intent(Intent.ACTION_VIEW, geoUri);
                if (Utility.isIntentSafe(getActivity(), mapCall)) {
                    startActivity(mapCall);
                }

            }
        }
    };

    private AdapterView.OnItemClickListener repostOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            getListView().clearChoices();

            if (position - listView.getHeaderViewsCount() < repostList.getSize()
                    && position >= listView.getHeaderViewsCount()) {
                startActivity(BrowserWeiboMsgActivity.newIntent(BeeboApplication.getInstance().getAccountBean(),
                        repostList.getItemList().get(position - listView.getHeaderViewsCount()), BeeboApplication.getInstance()
                                .getAccessTokenHack()));
            } else {
                loadOldRepostData();
            }
        }
    };

    protected AdapterView.OnItemClickListener commentOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            getListView().clearChoices();

            if (position - listView.getHeaderViewsCount() >= commentList.getSize()) {
                loadOldCommentData();
            }
        }
    };

    private AbsListView.OnScrollListener listViewOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            if (getListView().getLastVisiblePosition() > 7
                    && getListView().getFirstVisiblePosition() != getListView().getHeaderViewsCount()) {

                if (isCommentList) {

                    if (getListView().getLastVisiblePosition() > commentList.getSize() - 3) {
                        loadOldCommentData();
                    }
                } else {
                    if (getListView().getLastVisiblePosition() > repostList.getSize() - 3) {
                        loadOldRepostData();
                    }
                }
            }
        }
    };

    private class EmptyHeaderOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (isCommentList) {
                loadNewCommentData();
            } else {
                loadNewRepostData();
            }
        }
    }

    private class RepostTabOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            listView.setOnItemClickListener(repostOnItemClickListener);
            // listView.setOnItemLongClickListener(repostOnItemLongClickListener);
            emptyHeader.setText(R.string.repost_is_empty);

            dismissFooterView();
            if (isCommentList) {
                isCommentList = false;
                adapter.switchToRepostType();
                repostTab.setTextColor(ThemeUtility
                        .getColor(R.attr.browser_weibo_detail_comments_reposts_category_color_selected));
                commentTab.setTextColor(ThemeUtility
                        .getColor(R.attr.browser_weibo_detail_comments_reposts_category_color_unselected));
                if (repostList.getSize() == 0) {
                    loadNewRepostData();
                } else {
                    Loader<Object> loader = getLoaderManager().getLoader(NEW_REPOST_LOADER_ID);
                    if (loader != null) {
                        progressHeader.setVisibility(View.VISIBLE);
                    } else {
                        progressHeader.setVisibility(View.GONE);
                    }
                }
            } else {
                loadNewRepostData();
            }

            if (repostList.getSize() > 0) {
                emptyHeader.setVisibility(View.GONE);
            } else {
                emptyHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    private class CommentTabOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            listView.setOnItemClickListener(commentOnItemClickListener);
            // listView.setOnItemLongClickListener(commentOnItemLongClickListener);
            emptyHeader.setText(R.string.comment_is_empty);

            dismissFooterView();
            if (!isCommentList) {
                isCommentList = true;
                adapter.switchToCommentType();
                commentTab.setTextColor(ThemeUtility
                        .getColor(R.attr.browser_weibo_detail_comments_reposts_category_color_selected));
                repostTab.setTextColor(ThemeUtility
                        .getColor(R.attr.browser_weibo_detail_comments_reposts_category_color_unselected));

                if (commentList.getSize() == 0) {
                    loadNewCommentData();
                } else {
                    Loader<Object> loader = getLoaderManager().getLoader(NEW_COMMENT_LOADER_ID);
                    if (loader != null) {
                        progressHeader.setVisibility(View.VISIBLE);
                    } else {
                        progressHeader.setVisibility(View.GONE);
                    }
                }
            } else {
                loadNewCommentData();
            }

            if (commentList.getSize() > 0) {
                emptyHeader.setVisibility(View.GONE);
            } else {
                emptyHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    public void loadNewCommentData() {
        canLoadOldCommentData = true;
        if (getLoaderManager().getLoader(NEW_COMMENT_LOADER_ID) != null) {
            return;
        }
        progressHeader.setVisibility(View.VISIBLE);
        getLoaderManager().destroyLoader(OLD_COMMENT_LOADER_ID);
        getLoaderManager().restartLoader(NEW_COMMENT_LOADER_ID, null, commentMsgCallback);
    }

    public void loadNewRepostData() {
        canLoadOldRepostData = true;
        if (getLoaderManager().getLoader(NEW_REPOST_LOADER_ID) != null) {
            return;
        }
        progressHeader.setVisibility(View.VISIBLE);
        getLoaderManager().destroyLoader(OLD_REPOST_LOADER_ID);
        getLoaderManager().restartLoader(NEW_REPOST_LOADER_ID, null, repostMsgCallback);
    }

    public void loadOldCommentData() {
        if (getLoaderManager().getLoader(OLD_COMMENT_LOADER_ID) != null || !canLoadOldCommentData) {
            return;
        }
        showFooterView();
        getLoaderManager().destroyLoader(NEW_COMMENT_LOADER_ID);
        getLoaderManager().restartLoader(OLD_COMMENT_LOADER_ID, null, commentMsgCallback);
    }

    public void loadOldRepostData() {
        if (getLoaderManager().getLoader(OLD_REPOST_LOADER_ID) != null || !canLoadOldRepostData) {
            return;
        }
        showFooterView();
        getLoaderManager().destroyLoader(NEW_REPOST_LOADER_ID);
        getLoaderManager().restartLoader(OLD_REPOST_LOADER_ID, null, repostMsgCallback);
    }

    protected LoaderManager.LoaderCallbacks<AsyncTaskLoaderResult<CommentListBean>> commentMsgCallback =
            new LoaderManager.LoaderCallbacks<AsyncTaskLoaderResult<CommentListBean>>() {

                @Override
                public Loader<AsyncTaskLoaderResult<CommentListBean>> onCreateLoader(int id, Bundle args) {
                    String token = BeeboApplication.getInstance().getAccessTokenHack();

                    switch (id) {
                        case NEW_COMMENT_LOADER_ID:
                            String sinceId = null;
                            return new CommentsByIdMsgLoader(getActivity(), msg.getId(), token, sinceId, null);
                        case OLD_COMMENT_LOADER_ID:
                            String maxId = null;
                            if (commentList.getItemList().size() > 0) {
                                maxId = commentList.getItemList().get(commentList.getItemList().size() - 1).getId();
                            }
                            return new CommentsByIdMsgLoader(getActivity(), msg.getId(), token, null, maxId);
                    }

                    return null;
                }

                @Override
                public void onLoadFinished(Loader<AsyncTaskLoaderResult<CommentListBean>> loader,
                                           AsyncTaskLoaderResult<CommentListBean> result) {

                    CommentListBean data = result != null ? result.data : null;
                    WeiboException exception = result != null ? result.exception : null;

                    if (data != null) {
                        Utility.buildTabCount(commentTab, getString(R.string.comments), data.getTotal_number());
                        ((BrowserWeiboMsgActivity) getActivity()).updateCommentCount(data.getTotal_number());
                    }

                    switch (loader.getId()) {
                        case NEW_COMMENT_LOADER_ID:
                            if (isCommentList) {
                                progressHeader.setVisibility(View.GONE);
                            }
                            if (Utility.isAllNotNull(exception)) {
                                Toast.makeText(getActivity(), exception.getError(), Toast.LENGTH_SHORT).show();
                            } else {
                                if (data != null && data.getSize() > 0) {
                                    commentList.replaceAll(data);
                                    adapter.notifyDataSetChanged();

                                }

                                if (commentList.getSize() > 0 && isCommentList) {
                                    emptyHeader.setVisibility(View.GONE);
                                } else if (isCommentList) {
                                    emptyHeader.setVisibility(View.VISIBLE);
                                }
                            }
                            break;
                        case OLD_COMMENT_LOADER_ID:

                            if (Utility.isAllNotNull(exception)) {
                                Toast.makeText(getActivity(), exception.getError(), Toast.LENGTH_SHORT).show();
                                showErrorFooterView();
                            } else {
                                canLoadOldCommentData = !(data != null && data.getSize() <= 1);
                                dismissFooterView();
                                commentList.addOldData(data);
                                adapter.notifyDataSetChanged();
                            }
                            break;
                    }
                    getLoaderManager().destroyLoader(loader.getId());
                }

                @Override
                public void onLoaderReset(Loader<AsyncTaskLoaderResult<CommentListBean>> loader) {

                }
            };

    protected LoaderManager.LoaderCallbacks<AsyncTaskLoaderResult<RepostListBean>> repostMsgCallback =
            new LoaderManager.LoaderCallbacks<AsyncTaskLoaderResult<RepostListBean>>() {

                @Override
                public Loader<AsyncTaskLoaderResult<RepostListBean>> onCreateLoader(int id, Bundle args) {
                    String token = BeeboApplication.getInstance().getAccessTokenHack();

                    switch (id) {
                        case NEW_REPOST_LOADER_ID:
                            String sinceId = null;
                            return new RepostByIdMsgLoader(getActivity(), msg.getId(), token, sinceId, null);
                        case OLD_REPOST_LOADER_ID:
                            String maxId = null;

                            if (repostList.getSize() > 0) {
                                maxId = repostList.getItemList().get(repostList.getSize() - 1).getId();
                            }

                            return new RepostByIdMsgLoader(getActivity(), msg.getId(), token, null, maxId);
                    }

                    return null;
                }

                @Override
                public void onLoadFinished(Loader<AsyncTaskLoaderResult<RepostListBean>> loader,
                                           AsyncTaskLoaderResult<RepostListBean> result) {

                    RepostListBean data = result != null ? result.data : null;
                    WeiboException exception = result != null ? result.exception : null;

                    if (data != null) {
                        Utility.buildTabCount(repostTab, getString(R.string.repost), data.getTotal_number());
                        ((BrowserWeiboMsgActivity) getActivity()).updateRepostCount(data.getTotal_number());
                    }

                    switch (loader.getId()) {
                        case NEW_REPOST_LOADER_ID:
                            if (!isCommentList) {
                                progressHeader.setVisibility(View.GONE);
                            }
                            if (Utility.isAllNotNull(exception)) {
                                Toast.makeText(getActivity(), exception.getError(), Toast.LENGTH_SHORT).show();
                            } else {
                                if (data != null && data.getSize() > 0) {
                                    repostList.replaceAll(data);
                                    adapter.notifyDataSetChanged();

                                }

                                if (repostList.getSize() > 0 && !isCommentList) {
                                    emptyHeader.setVisibility(View.GONE);
                                } else if (!isCommentList) {
                                    emptyHeader.setVisibility(View.VISIBLE);
                                }
                            }
                            break;
                        case OLD_REPOST_LOADER_ID:

                            if (Utility.isAllNotNull(exception)) {
                                Toast.makeText(getActivity(), exception.getError(), Toast.LENGTH_SHORT).show();
                                showErrorFooterView();
                            } else {
                                canLoadOldRepostData = !(data != null && data.getSize() <= 1);
                                dismissFooterView();
                                repostList.addOldData(data);
                                adapter.notifyDataSetChanged();
                            }
                            break;
                    }
                    getLoaderManager().destroyLoader(loader.getId());
                }

                @Override
                public void onLoaderReset(Loader<AsyncTaskLoaderResult<RepostListBean>> loader) {

                }
            };

    class RemoveTask extends MyAsyncTask<Void, Void, Boolean> {

        String token;

        String id;

        int positon;

        WeiboException e;

        public RemoveTask(String token, String id, int positon) {
            this.token = token;
            this.id = id;
            this.positon = positon;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DestroyCommentDao dao = new DestroyCommentDao(token, id);
            try {
                return dao.destroy();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return false;
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            if (Utility.isAllNotNull(getActivity(), this.e)) {
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                adapter.removeCommentItem(positon);

            }
        }
    }

}
