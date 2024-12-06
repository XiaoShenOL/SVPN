package org.svpn.proxy;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.svpn.proxy.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Tnno Wu on 2018/01/04.
 */

public class SingleAdapter extends RecyclerView.Adapter<SingleAdapter.SingleViewHolder> {

    private Context mContext;

    private List<AppCap> mList = new ArrayList<>();

    private HashMap<Integer, Boolean> map = new HashMap<>();

    public SingleAdapter(Context context) {
        mContext = context;
    }

    public void setDataList(List<AppCap> list) {
        mList = list;

        for (int i = 0; i < mList.size(); i++) {
            map.put(i, false);
        }

        notifyDataSetChanged();
    }

    public void singleSelect(int position) {
        Set<Map.Entry<Integer, Boolean>> entries = map.entrySet();
        for (Map.Entry<Integer, Boolean> entry : entries) {
            entry.setValue(false);
        }
        map.put(position, true);
        notifyDataSetChanged();
    }

    public HashMap<Integer, Boolean> getMap() {
        return map;
    }

    @Override
    public SingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_single, parent, false);
        return new SingleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SingleViewHolder holder, final int position) {
		AppCap cap = mList.get(position);
        holder.textView.setText(cap.getAppName());

        holder.checkBox.setChecked(map.get(position));
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.put(position, !map.get(position));
                notifyDataSetChanged();
                singleSelect(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public class SingleViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        TextView textView;

        public SingleViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cb_single);
            textView = itemView.findViewById(R.id.tv_name_single);
        }
    }
}
