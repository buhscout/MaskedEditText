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

    /**
     * This custom mask symbol forbids to enter any character except '1', '2', '3',
     * where using '\z' mask symbol
     **/
    public static class CountrySymbol extends MaskedEditText.MaskSymbol {
        // Mask character
        public static final char Symbol = 'z';

        // Allowed characters
        private List<Character> mAvailableCharacters = Arrays.asList('1', '2', '7');

        /**
         * Accept or deny input character for this mask symbol
         **/
        @Override
        public boolean isAccept(char inputCharacter) {
            return mAvailableCharacters.contains(inputCharacter);
        }

        /**
         * You can change input character before it will be set to EditText
         * This method invoked after isAccept method
         **/
        @Override
        protected void setChar(char inputCharacter) {
            char newCharacter = Character.toUpperCase(inputCharacter);
            super.setChar(newCharacter);
        }
    }
}
