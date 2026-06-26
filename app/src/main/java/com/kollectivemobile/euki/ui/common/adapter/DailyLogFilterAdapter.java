package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.LayoutDailyLogItemBinding;
import com.kollectivemobile.euki.model.FilterItem;
import com.kollectivemobile.euki.ui.common.listeners.FilterItemListener;
import com.kollectivemobile.euki.utils.Utils;
import com.kollectivemobile.euki.utils.ViewUtils;
import com.kollectivemobile.euki.utils.advrecyclerview.draggable.DraggableItemAdapter;
import com.kollectivemobile.euki.utils.advrecyclerview.draggable.ItemDraggableRange;
import com.kollectivemobile.euki.utils.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DailyLogFilterAdapter extends RecyclerView.Adapter<DailyLogFilterAdapter.FilterHolder> implements DraggableItemAdapter<DailyLogFilterAdapter.FilterHolder> {
    private List<FilterItem> mFilterItems;
    private FilterItemListener mListener;
    private WeakReference<Context> mContext;

    public DailyLogFilterAdapter(Context context, FilterItemListener listener) {
        mFilterItems = new ArrayList<>();
        mContext = new WeakReference<>(context);
        mListener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public FilterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutDailyLogItemBinding binding = LayoutDailyLogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FilterHolder(binding, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterHolder holder, int position) {
        FilterItem filterItem = mFilterItems.get(position);
        holder.binding.vCircle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext.get(), filterItem.getColor())));
        holder.binding.tvTitle.setText(Utils.getLocalized(filterItem.getTitle()));
        holder.binding.ibChange.setImageResource(filterItem.getOn() ? R.drawable.ic_tile_remove : R.drawable.ic_tile_add);
        holder.binding.ivMove.setVisibility(filterItem.getOn() ? View.VISIBLE : View.GONE);
    }

    @Override
    public long getItemId(int position) {
        return mFilterItems.get(position).getTitle().hashCode();
    }

    @Override
    public int getItemCount() {
        return mFilterItems.size();
    }

    @Override
    public boolean onCheckCanStartDrag(@NonNull FilterHolder holder, int position, int x, int y) {
        final View containerView = holder.binding.rlMain;
        final View dragHandleView = holder.binding.ivMove;

        final int offsetX = containerView.getLeft() + (int) (containerView.getTranslationX() + 0.5f);
        final int offsetY = containerView.getTop() + (int) (containerView.getTranslationY() + 0.5f);

        return ViewUtils.hitTest(dragHandleView, x - offsetX, y - offsetY);
    }

    @Nullable
    @Override
    public ItemDraggableRange onGetItemDraggableRange(@NonNull FilterHolder holder, int position) {
        return null;
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        if (mListener != null) {
            mListener.moveItems(fromPosition, toPosition);
        }
    }

    @Override
    public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
        return mFilterItems.get(dropPosition).getOn();
    }

    @Override
    public void onItemDragStarted(int position) {
        notifyDataSetChanged();
    }

    @Override
    public void onItemDragFinished(int fromPosition, int toPosition, boolean result) {
        notifyDataSetChanged();
    }

    static class FilterHolder extends AbstractDraggableItemViewHolder {
        private FilterItemListener mListener;
        final LayoutDailyLogItemBinding binding;

        public FilterHolder(LayoutDailyLogItemBinding binding, FilterItemListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            mListener = listener;

            this.binding.ibChange.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.filterChanged(getLayoutPosition());
                }
            });
        }
    }

    public void update(List<FilterItem> filterItems) {
        if (filterItems != null) {
            mFilterItems.clear();
            mFilterItems.addAll(filterItems);
            notifyDataSetChanged();
        }
    }
}