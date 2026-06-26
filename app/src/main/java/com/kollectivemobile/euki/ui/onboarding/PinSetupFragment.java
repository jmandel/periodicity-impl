package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentPinCodeBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PinSetupFragment extends BaseFragment {
    @Inject
    AppSettingsManager mAppSettingsManager;

    private FragmentPinCodeBinding binding;
    protected String code = "";
    protected List<TextView> tvDigits;
    protected List<View> vDigits;

    private Button btnSetPin;
    private TextView tvButton0, tvButton1, tvButton2, tvButton3, tvButton4,
            tvButton5, tvButton6, tvButton7, tvButton8, tvButton9;
    private TextView tvDigit1, tvDigit2, tvDigit3, tvDigit4;
    private View vDigit1, vDigit2, vDigit3, vDigit4;
    private ImageView tvButtonBack;

    public static PinSetupFragment newInstance() {
        Bundle args = new Bundle();
        PinSetupFragment fragment = new PinSetupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initInjection();

        // Initialize views
        btnSetPin = view.findViewById(R.id.btn_set_pin);
        tvButton0 = view.findViewById(R.id.tv_button_0);
        tvButton1 = view.findViewById(R.id.tv_button_1);
        tvButton2 = view.findViewById(R.id.tv_button_2);
        tvButton3 = view.findViewById(R.id.tv_button_3);
        tvButton4 = view.findViewById(R.id.tv_button_4);
        tvButton5 = view.findViewById(R.id.tv_button_5);
        tvButton6 = view.findViewById(R.id.tv_button_6);
        tvButton7 = view.findViewById(R.id.tv_button_7);
        tvButton8 = view.findViewById(R.id.tv_button_8);
        tvButton9 = view.findViewById(R.id.tv_button_9);
        tvButtonBack = view.findViewById(R.id.tv_button_back);
        tvDigit1 = view.findViewById(R.id.tv_digit_1);
        tvDigit2 = view.findViewById(R.id.tv_digit_2);
        tvDigit3 = view.findViewById(R.id.tv_digit_3);
        tvDigit4 = view.findViewById(R.id.tv_digit_4);
        vDigit1 = view.findViewById(R.id.v_digit_1);
        vDigit2 = view.findViewById(R.id.v_digit_2);
        vDigit3 = view.findViewById(R.id.v_digit_3);
        vDigit4 = view.findViewById(R.id.v_digit_4);

        setUIElements();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentPinCodeBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_code, container, false);
    }

    protected void initInjection() {
        if (getActivity() != null) {
            ((App) getActivity().getApplication()).getAppComponent().inject(this);
        }
    }

    protected void setUIElements() {
        showDots();

        btnSetPin.setOnClickListener(v -> setPin());

        View.OnClickListener numberClickListener = this::buttonPressed;

        tvButton0.setOnClickListener(numberClickListener);
        tvButton1.setOnClickListener(numberClickListener);
        tvButton2.setOnClickListener(numberClickListener);
        tvButton3.setOnClickListener(numberClickListener);
        tvButton4.setOnClickListener(numberClickListener);
        tvButton5.setOnClickListener(numberClickListener);
        tvButton6.setOnClickListener(numberClickListener);
        tvButton7.setOnClickListener(numberClickListener);
        tvButton8.setOnClickListener(numberClickListener);
        tvButton9.setOnClickListener(numberClickListener);
        tvButtonBack.setOnClickListener(numberClickListener);
    }

    protected void setPin() {
        if (code.length() == 4) {
            mAppSettingsManager.savePinCode(code);
            mInteractionListener.replaceFragment(PinConfirmationFragment.newInstance(code.length() == 4), true);
        } else {
            mInteractionListener.replaceFragment(TermsAndCondsFragment.newInstance(), true);
        }
    }

    private void buttonPressed(View view) {
        Integer number = Utils.parseInt(view.getTag().toString());

        if (number != null) {
            if (code.length() == 4) {
                return;
            }
            code = code + number;
        } else {
            if (!code.isEmpty()) {
                code = code.substring(0, code.length() - 1);
            }
        }
        showDots();
    }

    protected void showDots() {
        int textRes = code.length() != 4 ? R.string.skip : R.string.set_pin;
        btnSetPin.setText(textRes);

        int codeLength = code.length();

        tvDigits = new ArrayList<>();
        vDigits = new ArrayList<>();

        tvDigits.add(tvDigit1);
        tvDigits.add(tvDigit2);
        tvDigits.add(tvDigit3);
        tvDigits.add(tvDigit4);

        vDigits.add(vDigit1);
        vDigits.add(vDigit2);
        vDigits.add(vDigit3);
        vDigits.add(vDigit4);

        for (TextView textView : tvDigits) {
            Integer number = Utils.parseInt(textView.getTag().toString());
            String digit = codeLength > (number - 1) ? Character.toString(code.charAt(number - 1)) : "";
            textView.setText(digit);
        }

        for (View lineView : vDigits) {
            int colorRes = codeLength > 0 ? R.color.blueberry : R.color.blueberry;
            lineView.setBackgroundColor(ContextCompat.getColor(App.getContext(), colorRes));
        }
    }
}

