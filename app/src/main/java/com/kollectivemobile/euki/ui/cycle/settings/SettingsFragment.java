package com.kollectivemobile.euki.ui.cycle.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.cycleig.CycleIgDates;
import com.kollectivemobile.euki.cycleig.CycleIgSampleData;
import com.kollectivemobile.euki.cycleig.CycleIgScope;
import com.kollectivemobile.euki.cycleig.CycleIgShare;
import com.kollectivemobile.euki.cycleig.CycleIgShareClient;
import com.kollectivemobile.euki.cycleig.CycleIgSnapshot;
import com.kollectivemobile.euki.cycleig.CycleIgSnapshotFactory;
import com.kollectivemobile.euki.databinding.FragmentCycleSettingsBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.manager.CalendarManager;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.common.BaseFragment;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class SettingsFragment extends BaseFragment {
    @Inject AppSettingsManager mAppSettingsManager;
    @Inject CalendarManager mCalendarManager;

    private FragmentCycleSettingsBinding binding;
    private final CycleIgSnapshotFactory snapshotFactory = new CycleIgSnapshotFactory();
    private final CycleIgShareClient shareClient = new CycleIgShareClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CycleIgSnapshot cycleIgSnapshot;
    private CycleIgShare cycleIgShare;
    private boolean cycleIgBusy = false;

    public static SettingsFragment newInstance() {
        Bundle args = new Bundle();
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ((App) getActivity().getApplication()).getAppComponent().inject(this);
        }
        setUIElements();
        setCycleIgElements();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCycleSettingsBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle_settings, container, false);
    }

    @Override
    public boolean showBack() {
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.getRoot().post(this::updateCycleIgControls);
        }
    }

    private void setUIElements() {
        Boolean trackPeriodEnabled = mAppSettingsManager.trackPeriodEnabled();
        binding.stchTrackPeriod.setChecked(trackPeriodEnabled);
        binding.stchPeriodPrediction.setChecked(mAppSettingsManager.periodPredictionEnabled());
        binding.stchPeriodPrediction.setEnabled(trackPeriodEnabled);

        binding.stchTrackPeriod.setOnCheckedChangeListener((buttonView, isChecked) -> trackPeriod(isChecked));
        binding.stchPeriodPrediction.setOnCheckedChangeListener((buttonView, isChecked) -> periodPrediction(isChecked));
    }

    void trackPeriod(boolean checked) {
        mAppSettingsManager.saveTrackPeriodEnabled(checked);
        if (!checked) {
            mAppSettingsManager.savePeriodPredictionEnabled(false);
        }
        setUIElements();
    }

    void periodPrediction(boolean checked) {
        mAppSettingsManager.savePeriodPredictionEnabled(checked);
    }

    private void setCycleIgElements() {
        binding.btnLoadCycleIgSample.setOnClickListener(view -> loadCycleIgSampleData());
        binding.btnPreviewCycleIg.setOnClickListener(view -> previewCycleIgShare());
        binding.btnShareCycleIg.setOnClickListener(view -> createCycleIgShare());
        binding.btnCopyCycleIgLink.setOnClickListener(view -> copyCycleIgLink());
        binding.btnNativeShareCycleIg.setOnClickListener(view -> nativeShareCycleIgLink());
        binding.btnOpenCycleIgViewer.setOnClickListener(view -> openCycleIgViewer());
        binding.btnStopCycleIgShare.setOnClickListener(view -> stopCycleIgShare());
        binding.etCycleIgStartDate.addTextChangedListener(invalidateCycleIgTextWatcher());
        binding.etCycleIgEndDate.addTextChangedListener(invalidateCycleIgTextWatcher());
        binding.cbCycleIgEmotions.setOnCheckedChangeListener((buttonView, isChecked) -> invalidateCycleIgSnapshot());
        binding.cbCycleIgBody.setOnCheckedChangeListener((buttonView, isChecked) -> invalidateCycleIgSnapshot());
        binding.cbCycleIgNotes.setOnCheckedChangeListener((buttonView, isChecked) -> invalidateCycleIgSnapshot());
        updateCycleIgControls();
    }

    private void loadCycleIgSampleData() {
        if (cycleIgBusy) {
            return;
        }

        setCycleIgBusy(true);
        executor.execute(() -> {
            try {
                List<CalendarItem> sampleItems = CycleIgSampleData.create();
                for (CalendarItem sampleItem : sampleItems) {
                    CalendarItem existing = mCalendarManager.getCalendarItem(sampleItem.getDate());
                    sampleItem.setId(existing.getId());
                    mCalendarManager.saveItem(sampleItem, new EukiCallback<>() {
                        @Override
                        public void onSuccess(Boolean saved) {
                        }

                        @Override
                        public void onError(ServerError serverError) {
                        }
                    });
                }
                mAppSettingsManager.saveTrackPeriodEnabled(true);
                mAppSettingsManager.savePeriodPredictionEnabled(false);
                runOnUiThread(() -> {
                    binding.tvCycleIgSampleStatus.setText(getString(R.string.cycle_ig_sample_loaded, sampleItems.size()));
                    binding.tvCycleIgSampleStatus.setVisibility(View.VISIBLE);
                    binding.etCycleIgStartDate.setText("");
                    binding.etCycleIgEndDate.setText("");
                    cycleIgSnapshot = null;
                    cycleIgShare = null;
                    setUIElements();
                    setCycleIgBusy(false);
                    updateCycleIgControls();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setCycleIgBusy(false);
                    showError(exception.getMessage());
                });
            }
        });
    }

    private void previewCycleIgShare() {
        try {
            cycleIgSnapshot = buildSnapshot();
            cycleIgShare = null;
            renderCycleIgPreview();
            updateCycleIgControls();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private CycleIgSnapshot buildSnapshot() {
        final List<CalendarItem>[] result = new List[1];
        final ServerError[] error = new ServerError[1];
        mCalendarManager.getDayscalendarItems(new EukiCallback<>() {
            @Override
            public void onSuccess(List<CalendarItem> calendarItems) {
                result[0] = calendarItems;
            }

            @Override
            public void onError(ServerError serverError) {
                error[0] = serverError;
            }
        });

        if (error[0] != null) {
            throw new IllegalStateException(error[0].getMessage());
        }

        return snapshotFactory.create(
                result[0],
                binding.etCycleIgStartDate.getText().toString(),
                binding.etCycleIgEndDate.getText().toString(),
                currentCycleIgScope()
        );
    }

    private CycleIgScope currentCycleIgScope() {
        return new CycleIgScope(
                binding.cbCycleIgEmotions.isChecked(),
                binding.cbCycleIgBody.isChecked(),
                binding.cbCycleIgNotes.isChecked()
        );
    }

    private void createCycleIgShare() {
        if (cycleIgBusy) {
            return;
        }

        try {
            if (cycleIgSnapshot == null) {
                cycleIgSnapshot = buildSnapshot();
                renderCycleIgPreview();
            }
        } catch (Exception exception) {
            showError(exception.getMessage());
            return;
        }

        setCycleIgBusy(true);
        executor.execute(() -> {
            try {
                CycleIgShare createdShare = shareClient.createShare(cycleIgSnapshot);
                Bitmap qrBitmap = qrBitmap(createdShare.viewerLink, 720);
                runOnUiThread(() -> {
                    cycleIgShare = createdShare;
                    binding.imgCycleIgQr.setImageBitmap(qrBitmap);
                    binding.tvCycleIgShareStatus.setText(getString(
                            R.string.cycle_ig_share_status,
                            CycleIgDates.formatDate(new java.util.Date(createdShare.exp * 1000L)),
                            createdShare.maxUses
                    ));
                    setCycleIgBusy(false);
                    updateCycleIgControls();
                });
            } catch (IOException | WriterException exception) {
                runOnUiThread(() -> {
                    setCycleIgBusy(false);
                    showError(exception.getMessage());
                });
            }
        });
    }

    private void stopCycleIgShare() {
        if (cycleIgBusy || cycleIgShare == null) {
            return;
        }

        setCycleIgBusy(true);
        executor.execute(() -> {
            try {
                shareClient.revoke(cycleIgShare);
                runOnUiThread(() -> {
                    cycleIgShare = null;
                    binding.imgCycleIgQr.setImageDrawable(null);
                    binding.tvCycleIgShareStatus.setText(R.string.cycle_ig_share_stopped);
                    binding.tvCycleIgShareStatus.setVisibility(View.VISIBLE);
                    setCycleIgBusy(false);
                    updateCycleIgControls();
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    setCycleIgBusy(false);
                    showError(exception.getMessage());
                });
            }
        });
    }

    private void copyCycleIgLink() {
        if (cycleIgShare == null || getActivity() == null) {
            return;
        }

        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.cycle_ig_title), cycleIgShare.viewerLink));
        showToast(getString(R.string.cycle_ig_link_copied));
    }

    private void nativeShareCycleIgLink() {
        if (cycleIgShare == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, cycleIgShare.viewerLink);
        startActivity(Intent.createChooser(intent, getString(R.string.cycle_ig_native_share_button)));
    }

    private void openCycleIgViewer() {
        if (cycleIgShare == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cycleIgShare.viewerLink));
        startActivity(intent);
    }

    private void renderCycleIgPreview() {
        if (cycleIgSnapshot == null) {
            binding.tvCycleIgPreview.setVisibility(View.GONE);
            binding.btnShareCycleIg.setVisibility(View.GONE);
            return;
        }

        CycleIgSnapshot.Preview preview = cycleIgSnapshot.getPreview();
        binding.tvCycleIgPreview.setText(getString(
                R.string.cycle_ig_preview_text,
                cycleIgSnapshot.getStartDateString(),
                cycleIgSnapshot.getEndDateString(),
                preview.dayCount,
                preview.menstrualBleedingFacts,
                preview.nonMenstrualBleedingFacts,
                preview.flowFacts,
                preview.productFacts,
                preview.clotFacts,
                preview.emotionFacts,
                preview.bodyFacts,
                preview.noteFacts
        ));
        binding.tvCycleIgPreview.setVisibility(View.VISIBLE);
        binding.btnShareCycleIg.setVisibility(View.VISIBLE);
    }

    private TextWatcher invalidateCycleIgTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                invalidateCycleIgSnapshot();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private void invalidateCycleIgSnapshot() {
        if (cycleIgShare != null || cycleIgBusy) {
            return;
        }
        cycleIgSnapshot = null;
        binding.tvCycleIgPreview.setVisibility(View.GONE);
        binding.btnShareCycleIg.setVisibility(View.GONE);
    }

    private void updateCycleIgControls() {
        boolean hasShare = cycleIgShare != null;
        if (!hasShare && !cycleIgBusy && getString(R.string.cycle_ig_working).equals(binding.tvCycleIgShareStatus.getText().toString())) {
            binding.tvCycleIgShareStatus.setText("");
        }
        binding.etCycleIgStartDate.setEnabled(!hasShare && !cycleIgBusy);
        binding.etCycleIgEndDate.setEnabled(!hasShare && !cycleIgBusy);
        binding.cbCycleIgEmotions.setEnabled(!hasShare && !cycleIgBusy);
        binding.cbCycleIgBody.setEnabled(!hasShare && !cycleIgBusy);
        binding.cbCycleIgNotes.setEnabled(!hasShare && !cycleIgBusy);
        binding.btnLoadCycleIgSample.setEnabled(!cycleIgBusy);
        binding.btnPreviewCycleIg.setEnabled(!hasShare && !cycleIgBusy);
        binding.btnShareCycleIg.setEnabled(!cycleIgBusy);

        binding.llCycleIgActiveMark.setVisibility(hasShare ? View.VISIBLE : View.GONE);
        binding.imgCycleIgQr.setVisibility(hasShare ? View.VISIBLE : View.GONE);
        binding.tvCycleIgShareStatus.setVisibility(hasShare || binding.tvCycleIgShareStatus.getText().length() > 0 ? View.VISIBLE : View.GONE);
        binding.btnCopyCycleIgLink.setVisibility(hasShare ? View.VISIBLE : View.GONE);
        binding.btnNativeShareCycleIg.setVisibility(hasShare ? View.VISIBLE : View.GONE);
        binding.btnOpenCycleIgViewer.setVisibility(hasShare ? View.VISIBLE : View.GONE);
        binding.btnStopCycleIgShare.setVisibility(hasShare ? View.VISIBLE : View.GONE);
    }

    private void setCycleIgBusy(boolean busy) {
        cycleIgBusy = busy;
        if (busy) {
            binding.tvCycleIgShareStatus.setText(R.string.cycle_ig_working);
            binding.tvCycleIgShareStatus.setVisibility(View.VISIBLE);
        } else if (cycleIgShare == null && getString(R.string.cycle_ig_working).equals(binding.tvCycleIgShareStatus.getText().toString())) {
            binding.tvCycleIgShareStatus.setText("");
            binding.tvCycleIgShareStatus.setVisibility(View.GONE);
        }
        updateCycleIgControls();
    }

    private Bitmap qrBitmap(String value, int size) throws WriterException {
        BitMatrix bitMatrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }
}
