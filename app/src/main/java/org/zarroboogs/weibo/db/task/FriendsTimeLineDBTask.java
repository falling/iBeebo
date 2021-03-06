
package org.zarroboogs.weibo.db.task;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.zarroboogs.utils.AppLoggerUtils;
import org.zarroboogs.weibo.BeeboApplication;
import org.zarroboogs.weibo.bean.GroupBean;
import org.zarroboogs.weibo.bean.GroupListBean;
import org.zarroboogs.weibo.bean.MessageBean;
import org.zarroboogs.weibo.bean.MessageListBean;
import org.zarroboogs.weibo.bean.MessageTimeLineData;
import org.zarroboogs.weibo.bean.TimeLinePosition;
import org.zarroboogs.weibo.db.DatabaseHelper;
import org.zarroboogs.weibo.db.table.HomeTable;
import org.zarroboogs.weibo.support.utils.AppConfig;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FriendsTimeLineDBTask {

    /**
     * the number of messages to read is calculated by listview position, for example, if you have
     * 1000 messages, but the first position of listview is 60, weiciyuan will save 1000 messages to
     * database, but at the next time when app need to read database, app will read only 60+
     * DB_CACHE_COUNT_OFFSET =70 messages.
     */

    private FriendsTimeLineDBTask() {

    }

    private static SQLiteDatabase getWsd() {

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
        return databaseHelper.getWritableDatabase();
    }

    private static SQLiteDatabase getRsd() {
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
        return databaseHelper.getReadableDatabase();
    }

    private static void addHomeLineMsg(MessageListBean list, String accountId) {

        if (list == null || list.getSize() == 0) {
            return;
        }

        Gson gson = new Gson();
        List<MessageBean> msgList = list.getItemList();
        DatabaseUtils.InsertHelper ih = new DatabaseUtils.InsertHelper(getWsd(), HomeTable.HomeDataTable.HOME_DATA_TABLE);
        final int mblogidColumn = ih.getColumnIndex(HomeTable.HomeDataTable.MBLOGID);
        final int accountidColumn = ih.getColumnIndex(HomeTable.HomeDataTable.ACCOUNTID);
        final int jsondataColumn = ih.getColumnIndex(HomeTable.HomeDataTable.JSONDATA);
        try {
            getWsd().beginTransaction();
            for (int i = 0; i < msgList.size(); i++) {
                MessageBean msg = msgList.get(i);
                ih.prepareForInsert();
                if (msg != null) {
                    ih.bind(mblogidColumn, msg.getId());
                    ih.bind(accountidColumn, accountId);
                    String json = gson.toJson(msg);
                    ih.bind(jsondataColumn, json);
                } else {
                    ih.bind(mblogidColumn, "-1");
                    ih.bind(accountidColumn, accountId);
                    ih.bind(jsondataColumn, "");
                }
                ih.execute();
            }
            getWsd().setTransactionSuccessful();
        } catch (SQLException e) {
        } finally {
            getWsd().endTransaction();
            ih.close();
        }
        reduceHomeTable(accountId);
    }

    private static void reduceHomeTable(String accountId) {
        String searchCount = "select count(" + HomeTable.HomeDataTable.ID + ") as total" + " from "
                + HomeTable.HomeDataTable.HOME_DATA_TABLE + " where "
                + HomeTable.HomeDataTable.ACCOUNTID + " = " + accountId;
        int total = 0;
        Cursor c = getWsd().rawQuery(searchCount, null);
        if (c.moveToNext()) {
            total = c.getInt(c.getColumnIndex("total"));
        }

        c.close();

        AppLoggerUtils.e("total=" + total);
    }

    private static void replace(MessageListBean list, String accountId, String groupId) {
        if (groupId.equals("0")) {
            deleteAllHomes(accountId);
            addHomeLineMsg(list, accountId);
        } else {
            HomeOtherGroupTimeLineDBTask.replace(list, accountId, groupId);
        }
    }

    // todo may occur ConcurrentModificationException
    public static void asyncReplace(final MessageListBean list, final String accountId, final String groupId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                replace(list, accountId, groupId);
            }
        }).start();

    }

    public static void asyncUpdatePosition(final TimeLinePosition position, final String accountId, final String groupId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FriendsTimeLineDBTask.updatePosition(position, accountId, groupId);
            }
        };

        new Thread(runnable).start();
    }

    private static void updatePosition(TimeLinePosition position, String accountId, String groupId) {
        if (groupId.equals("0")) {
            updatePosition(position, accountId);
        } else {
            HomeOtherGroupTimeLineDBTask
                    .updatePosition(position, BeeboApplication.getInstance().getCurrentAccountId(), groupId);
        }
    }

    private static void updatePosition(TimeLinePosition position, String accountId) {
        String sql = "select * from " + HomeTable.TABLE_NAME + " where " + HomeTable.ACCOUNTID + "  = " + accountId;
        Cursor c = getRsd().rawQuery(sql, null);
        Gson gson = new Gson();
        if (c.getCount() > 0) {
            try {
                String[] args = {
                        accountId
                };
                ContentValues cv = new ContentValues();
                cv.put(HomeTable.TIMELINEDATA, gson.toJson(position));
                getWsd().update(HomeTable.TABLE_NAME, cv, HomeTable.ACCOUNTID + "=?", args);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            ContentValues cv = new ContentValues();
            cv.put(HomeTable.ACCOUNTID, accountId);
            cv.put(HomeTable.TIMELINEDATA, gson.toJson(position));
            getWsd().insert(HomeTable.TABLE_NAME, HomeTable.ID, cv);
        }

    }

    private static TimeLinePosition getPosition(String accountId) {
        String sql = "select * from " + HomeTable.TABLE_NAME + " where " + HomeTable.ACCOUNTID + "  = " + accountId;
        Cursor c = getRsd().rawQuery(sql, null);
        Gson gson = new Gson();
        while (c.moveToNext()) {
            String json = c.getString(c.getColumnIndex(HomeTable.TIMELINEDATA));
            if (!TextUtils.isEmpty(json)) {
                try {
                    TimeLinePosition value = gson.fromJson(json, TimeLinePosition.class);
                    c.close();
                    return value;

                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }

        }
        c.close();
        return new TimeLinePosition(0, 0);
    }

    public static String getRecentGroupId(String accountId) {
        String sql = "select * from " + HomeTable.TABLE_NAME + " where " + HomeTable.ACCOUNTID + "  = " + accountId;
        Cursor c = getRsd().rawQuery(sql, null);
        Gson gson = new Gson();
        while (c.moveToNext()) {
            String id = c.getString(c.getColumnIndex(HomeTable.RECENT_GROUP_ID));
            if (!TextUtils.isEmpty(id)) {
                return id;
            }

        }
        c.close();
        return "0";
    }

    public static void asyncUpdateRecentGroupId(String accountId, final String groupId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FriendsTimeLineDBTask.updateRecentGroupId(BeeboApplication.getInstance().getCurrentAccountId(), groupId);
            }
        };

        new Thread(runnable).start();
    }

    private static void updateRecentGroupId(String accountId, String groupId) {

        String sql = "select * from " + HomeTable.TABLE_NAME + " where " + HomeTable.ACCOUNTID + "  = " + accountId;
        Cursor c = getRsd().rawQuery(sql, null);
        if (c.moveToNext()) {
            try {
                String[] args = {
                        accountId
                };
                ContentValues cv = new ContentValues();
                cv.put(HomeTable.RECENT_GROUP_ID, groupId);
                getWsd().update(HomeTable.TABLE_NAME, cv, HomeTable.ACCOUNTID + "=?", args);
            } catch (JsonSyntaxException e) {

            }
        } else {

            ContentValues cv = new ContentValues();
            cv.put(HomeTable.ACCOUNTID, accountId);
            cv.put(HomeTable.RECENT_GROUP_ID, groupId);
            getWsd().insert(HomeTable.TABLE_NAME, HomeTable.ID, cv);
        }
    }

    static void deleteAllHomes(String accountId) {
        String sql = "delete from " + HomeTable.HomeDataTable.HOME_DATA_TABLE + " where " + HomeTable.HomeDataTable.ACCOUNTID
                + " in " + "(" + accountId + ")";

        getWsd().execSQL(sql);
    }

    public static void asyncUpdateCount(final String msgId, final int commentCount, final int repostCount) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                FriendsTimeLineDBTask.updateCount(msgId, commentCount, repostCount);
                HomeOtherGroupTimeLineDBTask.updateCount(msgId, commentCount, repostCount);
            }
        }).start();

    }

    private static void updateCount(String msgId, int commentCount, int repostCount) {


        String sql = "select * from " + HomeTable.HomeDataTable.HOME_DATA_TABLE + " where " + HomeTable.HomeDataTable.MBLOGID
                + "  = " + msgId + " order by "
                + HomeTable.HomeDataTable.ID + " asc limit 50";



        Cursor c = getRsd().rawQuery(sql, null);
        Gson gson = new Gson();
        while (c.moveToNext()) {
            String id = c.getString(c.getColumnIndex(HomeTable.HomeDataTable.ID));
            String json = c.getString(c.getColumnIndex(HomeTable.HomeDataTable.JSONDATA));
            if (!TextUtils.isEmpty(json)) {
                try {
                    MessageBean value = gson.fromJson(json, MessageBean.class);
                    value.setComments_count(commentCount);
                    value.setReposts_count(repostCount);
                    String[] args = {
                            id
                    };
                    ContentValues cv = new ContentValues();
                    cv.put(HomeTable.HomeDataTable.JSONDATA, gson.toJson(value));
                    getWsd().update(HomeTable.HomeDataTable.HOME_DATA_TABLE, cv, HomeTable.HomeDataTable.ID + "=?", args);
                } catch (JsonSyntaxException e) {

                }

            }
        }
        c.close();
    }

    public static MessageTimeLineData getRecentGroupData(String accountId) {
        String groupId = getRecentGroupId(accountId);
        MessageListBean msgList;
        TimeLinePosition position;
        if (groupId.equals("0")) {
            position = getPosition(accountId);
            msgList = getHomeLineMsgList(accountId, position.position + AppConfig.DB_CACHE_COUNT_OFFSET);
        } else {
            position = HomeOtherGroupTimeLineDBTask.getPosition(accountId, groupId);
            msgList = HomeOtherGroupTimeLineDBTask.get(accountId, groupId, position.position
                    + AppConfig.DB_CACHE_COUNT_OFFSET);
        }

        return new MessageTimeLineData(groupId, msgList, position);
    }

    public static List<MessageTimeLineData> getOtherGroupData(String accountId, String exceptGroupId) {
        List<MessageTimeLineData> data = new ArrayList<MessageTimeLineData>();

        if (!"0".equals(exceptGroupId)) {
            TimeLinePosition position = getPosition(accountId);
            MessageListBean msgList = getHomeLineMsgList(accountId, position.position + AppConfig.DB_CACHE_COUNT_OFFSET);
            MessageTimeLineData home = new MessageTimeLineData("0", msgList, position);
            data.add(home);
        }

        MessageTimeLineData biGroup = HomeOtherGroupTimeLineDBTask.getTimeLineData(accountId, "1");
        data.add(biGroup);

        GroupListBean groupListBean = GroupDBTask.get(accountId);

        if (groupListBean != null) {
            List<GroupBean> lists = groupListBean.getLists();
            for (GroupBean groupBean : lists) {
                MessageTimeLineData dbMsg = HomeOtherGroupTimeLineDBTask.getTimeLineData(accountId, groupBean.getId());
                data.add(dbMsg);
            }
        }

        Iterator<MessageTimeLineData> iterator = data.iterator();
        while (iterator.hasNext()) {
            MessageTimeLineData single = iterator.next();
            if (single.groupId.equals(exceptGroupId)) {
                iterator.remove();
                break;
            }
        }

        return data;
    }

    private static MessageListBean getHomeLineMsgList(String accountId, int limitCount) {

        Gson gson = new Gson();
        MessageListBean result = new MessageListBean();
        int limit = limitCount > AppConfig.DEFAULT_MSG_COUNT_50 ? limitCount : AppConfig.DEFAULT_MSG_COUNT_50;
        List<MessageBean> msgList = new ArrayList<>();
        String sql = "select * from " + HomeTable.HomeDataTable.HOME_DATA_TABLE + " where " + HomeTable.HomeDataTable.ACCOUNTID
                + "  = " + accountId + " order by "
                + HomeTable.HomeDataTable.ID + " asc limit " + limit;
        Cursor c = getRsd().rawQuery(sql, null);
        while (c.moveToNext()) {
            String json = c.getString(c.getColumnIndex(HomeTable.HomeDataTable.JSONDATA));
            if (!TextUtils.isEmpty(json)) {
                try {
                    MessageBean value = gson.fromJson(json, MessageBean.class);
                    value.getListViewSpannableString();
                    msgList.add(value);
                } catch (JsonSyntaxException e) {
                    AppLoggerUtils.e(e.getMessage());
                }

            } else {
                msgList.add(null);
            }
        }

        // delete the null flag at the head positon and the end position
        for (int i = msgList.size() - 1; i >= 0; i--) {
            if (msgList.get(i) == null) {
                msgList.remove(i);
            } else {
                break;
            }
        }

        for (int i = 0; i < msgList.size(); i++) {
            if (msgList.get(i) == null) {
                msgList.remove(i);
            } else {
                break;
            }
        }

        result.setStatuses(msgList);
        c.close();
        return result;

    }
}
