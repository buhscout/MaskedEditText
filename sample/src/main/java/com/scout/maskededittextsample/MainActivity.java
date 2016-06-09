package com.scout.maskededittextsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.scout.widget.MaskedEditText;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    MaskedEditText maskedEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        maskedEditText = (MaskedEditText)findViewById(R.id.phone);
        //setCountryMask();
    }

    // Set custom mask symbol
    private void setCountryMask() {
        maskedEditText.registerMaskSymbol(CountrySymbol.Symbol, CountrySymbol.class);
        maskedEditText.setMask("+\\z \\d\\d\\d \\d\\d\\d-\\d\\d-\\d\\d");
    }

    // If custom MaskSymbol is nested class, then it must be a static
    public static class CountrySymbol extends MaskedEditText.MaskSymbol {
        // Mask character
        public static final char Symbol = 'z';

        private List<Character> mAvailableCharacters = Arrays.asList('1', '2', '7');

        @Override
        public boolean isAccept(char c) {
            /*
                Any code for accept or deny input character
             */
            return mAvailableCharacters.contains(c);
        }
    }
}
