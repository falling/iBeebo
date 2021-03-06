
package org.zarroboogs.weibo.setting.fragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.db.task.FilterDBTask;
import org.zarroboogs.weibo.setting.CommonAppDefinedFilterList;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FilterKeywordFragment extends AbstractFilterFragment {

    @Override
    protected List<String> getDBDataImpl() {
        return FilterDBTask.getFilterKeywordList(FilterDBTask.TYPE_KEYWORD);
    }

    @Override
    protected void addFilterImpl(Collection<String> set) {
        FilterDBTask.addFilterKeyword(FilterDBTask.TYPE_KEYWORD, set);
    }

    @Override
    protected List<String> removeAndGetFilterListImpl(Collection<String> set) {
        return FilterDBTask.removeAndGetNewFilterKeywordList(FilterDBTask.TYPE_KEYWORD, set);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actionbar_menu_filterkeywordfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_common) {
            Set<String> words = CommonAppDefinedFilterList.getDefinedFilterKeywordAndUserList();
            words.removeAll(list);
            addFilter(words);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
