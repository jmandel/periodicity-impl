package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.adapter.DailyLogAdapter;
import com.kollectivemobile.euki.ui.common.listeners.DailyLogViewListener;
import com.kollectivemobile.euki.ui.common.views.SelectableButton;
import com.kollectivemobile.euki.utils.Constants;
import com.kollectivemobile.euki.utils.Utils;

import java.util.Arrays;
import java.util.List;

public class DailyLogContraceptionHolder extends BaseDailyLogHolder implements View.OnClickListener {
    public LinearLayout llDailyContainer;
    public LinearLayout llLongTermContainer;

    public List<SelectableButton> sbPills;
    public List<SelectableButton> sbOther;
    public List<SelectableButton> sbIUD;
    public List<SelectableButton> sbImplant;
    public List<SelectableButton> sbPatch;
    public List<SelectableButton> sbRing;
    public List<SelectableButton> sbShot;

    public DailyLogContraceptionHolder(@NonNull View itemView, DailyLogViewListener listener) {
        super(itemView, listener);

        // Manual view binding
        llDailyContainer = itemView.findViewById(R.id.ll_daily_container);
        llLongTermContainer = itemView.findViewById(R.id.ll_longer_term_methods_container);

        sbPills = Arrays.asList(
                itemView.findViewById(R.id.sb_contraception_pill_1),
                itemView.findViewById(R.id.sb_contraception_pill_2),
                itemView.findViewById(R.id.sb_contraception_pill_3)
        );
        sbOther = Arrays.asList(
                itemView.findViewById(R.id.sb_contraception_other_1),
                itemView.findViewById(R.id.sb_contraception_other_2),
                itemView.findViewById(R.id.sb_contraception_other_3),
                itemView.findViewById(R.id.sb_contraception_other_4),
                itemView.findViewById(R.id.sb_contraception_other_5),
                itemView.findViewById(R.id.sb_contraception_other_6),
                itemView.findViewById(R.id.sb_contraception_other_7)
        );
        sbIUD = Arrays.asList(
                itemView.findViewById(R.id.sb_uid_1),
                itemView.findViewById(R.id.sb_uid_2),
                itemView.findViewById(R.id.sb_uid_3)
        );
        sbImplant = Arrays.asList(
                itemView.findViewById(R.id.sb_implant_1),
                itemView.findViewById(R.id.sb_implant_2)
        );
        sbPatch = Arrays.asList(
                itemView.findViewById(R.id.sb_patch_1),
                itemView.findViewById(R.id.sb_patch_2)
        );
        sbRing = Arrays.asList(
                itemView.findViewById(R.id.sb_ring_1),
                itemView.findViewById(R.id.sb_ring_2)
        );
        sbShot = Arrays.asList(
                itemView.findViewById(R.id.sb_shot_1)
        );

        // Set click listeners manually
        setOnClickListeners(sbPills);
        setOnClickListeners(sbOther);
        setOnClickListeners(sbIUD);
        setOnClickListeners(sbImplant);
        setOnClickListeners(sbPatch);
        setOnClickListeners(sbRing);
        setOnClickListeners(sbShot);

        // Set click listeners for section titles
        itemView.findViewById(R.id.ll_daily_title).setOnClickListener(this);
        itemView.findViewById(R.id.ll_longer_term_methods_title).setOnClickListener(this);
    }

    private void setOnClickListeners(List<SelectableButton> buttons) {
        for (SelectableButton button : buttons) {
            button.setOnClickListener(this);
        }
    }

    public static DailyLogContraceptionHolder create(ViewGroup parent, DailyLogViewListener listener) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_daily_log_contraception, parent, false);
        return new DailyLogContraceptionHolder(view, listener);
    }

    @Override
    public DailyLogAdapter.ViewType getViewType() {
        return DailyLogAdapter.ViewType.CONTRACEPTION;
    }

    @Override
    public Boolean hasData() {
        return mCalendarItem.hasContraception();
    }

    @Override
    public void bind(CalendarItem calendarItem, Boolean selected, DailyLogAdapter.ViewType selectedType) {
        super.bind(calendarItem, selected, selectedType);

        if (mCalendarItem.getContraceptionPills() != null) {
            sbPills.get(mCalendarItem.getContraceptionPills().ordinal()).changeSelected(true);
        }
        for (Constants.ContraceptionDailyOther other : mCalendarItem.getContraceptionDailyOther()) {
            sbOther.get(other.ordinal()).changeSelected(true);
        }
        if (mCalendarItem.getContraceptionIUD() != null) {
            sbIUD.get(mCalendarItem.getContraceptionIUD().ordinal()).changeSelected(true);
        }
        if (mCalendarItem.getContraceptionImplant() != null) {
            sbImplant.get(mCalendarItem.getContraceptionImplant().ordinal()).changeSelected(true);
        }
        if (mCalendarItem.getContraceptionPatch() != null) {
            sbPatch.get(mCalendarItem.getContraceptionPatch().ordinal()).changeSelected(true);
        }
        if (mCalendarItem.getContraceptionRing() != null) {
            sbRing.get(mCalendarItem.getContraceptionRing().ordinal()).changeSelected(true);
        }
        if (mCalendarItem.getContraceptionShot() != null) {
            sbShot.get(mCalendarItem.getContraceptionShot().ordinal()).changeSelected(true);
        }

        llDailyContainer.setVisibility(View.GONE);
        llLongTermContainer.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof SelectableButton && view.getTag() instanceof String) {
            SelectableButton selectableButton = (SelectableButton) view;
            Integer index = Integer.parseInt((String) view.getTag());

            if (sbPills.contains(selectableButton)) {
                mCalendarItem.setContraceptionPills(selectableButton.getSelected() ? Constants.ContraceptionPills.values()[index - 1] : null);
            } else if (sbOther.contains(selectableButton)) {
                Constants.ContraceptionDailyOther other = Constants.ContraceptionDailyOther.values()[index - 1];
                if (mCalendarItem.getContraceptionDailyOther().contains(other)) {
                    mCalendarItem.getContraceptionDailyOther().remove(other);
                } else {
                    mCalendarItem.getContraceptionDailyOther().add(other);
                }
            } else if (sbIUD.contains(selectableButton)) {
                mCalendarItem.setContraceptionIUD(selectableButton.getSelected() ? Constants.ContraceptionIUD.values()[index - 1] : null);
            } else if (sbImplant.contains(selectableButton)) {
                mCalendarItem.setContraceptionImplant(selectableButton.getSelected() ? Constants.ContraceptionImplant.values()[index - 1] : null);
            } else if (sbPatch.contains(selectableButton)) {
                mCalendarItem.setContraceptionPatch(selectableButton.getSelected() ? Constants.ContraceptionPatch.values()[index - 1] : null);
            } else if (sbRing.contains(selectableButton)) {
                mCalendarItem.setContraceptionRing(selectableButton.getSelected() ? Constants.ContraceptionRing.values()[index - 1] : null);
            } else if (sbShot.contains(selectableButton)) {
                mCalendarItem.setContraceptionShot(selectableButton.getSelected() ? Constants.ContraceptionShot.values()[index - 1] : null);
            }

            updateTitle();
            mListener.dataChanged();
        } else if (view.getId() == R.id.ll_daily_title) {
            onClickDailyTitle();
        } else if (view.getId() == R.id.ll_longer_term_methods_title) {
            onClickLongerTermsTitle();
        }
    }

    private void onClickDailyTitle() {
        if (llDailyContainer.getVisibility() == View.VISIBLE) {
            llDailyContainer.setVisibility(View.GONE);
        } else {
            llDailyContainer.setVisibility(View.VISIBLE);
        }
        llLongTermContainer.setVisibility(View.GONE);
        mListener.categorySelected(getLayoutPosition(), Utils.dpFromInt(85));
    }

    private void onClickLongerTermsTitle() {
        if (llLongTermContainer.getVisibility() == View.VISIBLE) {
            llLongTermContainer.setVisibility(View.GONE);
        } else {
            llLongTermContainer.setVisibility(View.VISIBLE);
        }
        llDailyContainer.setVisibility(View.GONE);
        mListener.categorySelected(getLayoutPosition(), 2 * Utils.dpFromInt(85));
    }
}
