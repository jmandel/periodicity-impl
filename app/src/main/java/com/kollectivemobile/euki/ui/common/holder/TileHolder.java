package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.TileItem;
import com.kollectivemobile.euki.utils.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.util.List;

public class TileHolder extends AbstractDraggableItemViewHolder {
    public CardView cvMain;
    public ImageButton ibAction;
    public ImageView ivIcon;
    public TextView tvTitle;

    private TileListener mTileListener;

    public TileHolder(@NonNull View itemView, TileListener tileListener) {
        super(itemView);
        mTileListener = tileListener;

        cvMain = itemView.findViewById(R.id.cv_main);
        ibAction = itemView.findViewById(R.id.ib_action);
        ivIcon = itemView.findViewById(R.id.iv_icon);
        tvTitle = itemView.findViewById(R.id.tv_title);

        cvMain.setOnClickListener(v -> {
            if (mTileListener != null) {
                mTileListener.tileSelected(getLayoutPosition());
            }
        });

        ibAction.setOnClickListener(v -> {
            if (mTileListener != null) {
                mTileListener.actionSelected(getLayoutPosition());
            }
        });
    }

    public interface TileListener {
        void tileSelected(int position);
        void actionSelected(int position);
        void orderChanged(List<TileItem> tileItems);
        void editStarted();
    }
}
