package com.kollectivemobile.euki.ui.pin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.ui.main.MainActivity;
import com.kollectivemobile.euki.ui.onboarding.PinSetupFragment;
import com.kollectivemobile.euki.utils.Utils;

import javax.inject.Inject;

public class CheckPinFragment extends PinSetupFragment {
    @Inject AppSettingsManager mAppSettingsManager;

    private Boolean shouldShowMainViewController = false;

    private View fakeScreenLayout;
    private TextView tvError;
    private Button btnSetPin;

    public static CheckPinFragment newInstance(Boolean shouldShowMainViewController) {
        Bundle args = new Bundle();
        CheckPinFragment fragment = new CheckPinFragment();
        fragment.shouldShowMainViewController = shouldShowMainViewController;
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pin_check, container, false);

        // Initialize views
        fakeScreenLayout = view.findViewById(R.id.fake_screen);
        tvError = view.findViewById(R.id.tv_error);
        btnSetPin = view.findViewById(R.id.btn_set_pin);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fakeScreenLayout.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        code = "";
        showDots();
    }

    @Override
    protected void initInjection() {
        if (getActivity() != null) {
            ((App)getActivity().getApplication()).getAppComponent().inject(this);
        }
    }

    @Override
    public void setPin() {
        String currentPin = mAppSettingsManager.getPinCode();
        String fakePin = currentPin.equals("1111") ? "2222" : "1111";

        if (currentPin.equals(code)) {
            if (shouldShowMainViewController) {
                startActivity(MainActivity.makeIntent(getActivity()));
            } else {
                getActivity().finish();
            }
        } else if (code.equals(fakePin)) {
            fakeScreenLayout.setVisibility(View.VISIBLE);
        } else {
            tvError.setVisibility(View.VISIBLE);
            code = "";
            showDots();
        }
    }

    @Override
    protected void showDots() {
        super.showDots();
        boolean isCompleted = code.length() == 4;
        btnSetPin.setAlpha(isCompleted ? 1.0F : 0.5F);
        btnSetPin.setEnabled(isCompleted);
        btnSetPin.setText(getString(R.string.go));

        int codeLength = code.length();

        for (TextView textView : tvDigits) {
            Integer number = Utils.parseInt(textView.getTag().toString());
            String digit = codeLength > (number - 1) ? "*" : "";
            textView.setText(digit);
        }

        for (View lineView : vDigits) {
            int colorRes = codeLength > 0 ? R.color.blueberry : R.color.blueberry;
            lineView.setBackgroundColor(ContextCompat.getColor(App.getContext(), colorRes));
        }
    }
}
