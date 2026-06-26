package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.Appointment;
import com.kollectivemobile.euki.ui.common.adapter.DailyLogAdapter;

import java.lang.ref.WeakReference;

public class DailyLogAppointmentExistentHolder extends RecyclerView.ViewHolder {
    private TextView tvTitle;
    private final AppointmentExistentListener mListener;
    private WeakReference<Appointment> mAppointment;

    private DailyLogAppointmentExistentHolder(@NonNull View itemView, AppointmentExistentListener listener) {
        super(itemView);
        mListener = listener;
        tvTitle = itemView.findViewById(R.id.tv_title); // View Binding replacement for ButterKnife
        itemView.findViewById(R.id.ll_main).setOnClickListener(v -> appointmentSelected());
    }

    public static DailyLogAppointmentExistentHolder create(ViewGroup parent, AppointmentExistentListener listener) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_daily_log_appointment_existent, parent, false);
        return new DailyLogAppointmentExistentHolder(view, listener);
    }

    public DailyLogAdapter.ViewType getViewType() {
        return DailyLogAdapter.ViewType.APPOINTMENTEXISTENT;
    }

    public void bind(Appointment appointment) {
        mAppointment = new WeakReference<>(appointment);
        tvTitle.setText(appointment.getTitle());
    }

    private void appointmentSelected() {
        if (mListener != null) {
            mListener.selectedAppointment(mAppointment.get());
        }
    }

    public interface AppointmentExistentListener {
        void selectedAppointment(Appointment appointment);
    }
}
