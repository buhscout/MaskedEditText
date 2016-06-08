package com.scout.maskapp.Mask;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * Текстовый редактор с маской ввода
 */
public class MaskedEditText2 extends AppCompatEditText {
    private int mCursorPosition;
    private String mUnmaskedText = "";
    private String mMask;
    private Character mMaskSymbol;
    private int mSourceMaxLen;

    public MaskedEditText2(Context context) {
        super(context);
        init();
    }

    public MaskedEditText2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskedEditText2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setMask("++\\d\\d--\\d\\d\\d-\\d\\d");
        setText("12345678");
    }

    public String getUnmaskedText(){
        String text = "";
        for (Symbol symbol : mUsedSymbols) {
            if(symbol.isMask()) {
                text += symbol.getChar();
            }
        }
        return text;
    }

    /*public Character getMaskSymbol() {
        return mMaskSymbol;
    }

    public void setMaskSymbol(Character symbol) {
        mMaskSymbol = symbol;
        if(!TextUtils.isEmpty(mMask)) {
            setMask(mMask);
        }
    }*/

    public String getMask() {
        return mMask;
    }

    private Symbol getMaskSymbol(char maskChar) {
        switch (maskChar) {
            case DecimalSymbol.MaskChar:
                return new DecimalSymbol();
            case CharSymbol.MaskChar:
                return new CharSymbol();
            case UppercaseCharSymbol.MaskChar:
                return new UppercaseCharSymbol();
        }
        return null;
    }

    private ArrayList<Symbol> mAvailableSymbols = new ArrayList<>();
    private ArrayList<Symbol> mUsedSymbols = new ArrayList<>();

    public void setMask(String mask) {
        if(TextUtils.isEmpty(mask) || TextUtils.equals(mMask, mask)) {
            return;
        }
        removeTextChangedListener(mMaskTextWatcher);
        String value = getUnmaskedText();
        mAvailableSymbols.clear();
        mUsedSymbols.clear();
        mMask = mask;
        for(int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            if(c == '\\') {
                if(i == mask.length() - 1) {
                    throw new IllegalArgumentException("Wrong mask format! Position " + String.valueOf(i));
                }
                Symbol symbol = getMaskSymbol(mask.charAt(i + 1));
                if(symbol == null) {
                    throw new IllegalArgumentException("Wrong mask format! Unsupported mask symbol '" + mask.charAt(i + 1) + "'. Position " + String.valueOf(i));
                }
                mAvailableSymbols.add(symbol);
                i++;
            } else {
                mAvailableSymbols.add(new StaticSymbol(c));
            }
        }
        addTextChangedListener(mMaskTextWatcher);
        setText(value);
    }

    private boolean mShowFutureMask = true;

    TextWatcher mMaskTextWatcher = new TextWatcher() {
        private int mSelectionStart;
        private int mSelectionEnd;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mSelectionStart = getSelectionStart();
            mSelectionEnd = getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String temp = "";
            mCursorPosition = start + count;
            if(before == 0 && mAvailableSymbols.size() == 0) {
                mCursorPosition -= count;
                return;
            }

            for (int i = mUsedSymbols.size() - 1; i >= start + before; i--) {
                Symbol symbol = mUsedSymbols.remove(i);
                if (i >= start + before && symbol.isMask()) {
                    temp = symbol.getChar() + temp;
                }
                mAvailableSymbols.add(0, symbol);
            }

            if(before == 1 && count == 0 && mSelectionStart == mSelectionEnd) {
                for (int i = mUsedSymbols.size() - 1; i >= 0; i--) {
                    Symbol symbol = mUsedSymbols.remove(i);
                    mAvailableSymbols.add(0, symbol);
                    if(symbol.isMask()) {
                        break;
                    }
                    mCursorPosition--;
                }
            }

            int maskedRemoved = 0;
            for (int i = mUsedSymbols.size() - 1; i >= start; i--) {
                Symbol symbol = mUsedSymbols.remove(i);
                mAvailableSymbols.add(0, symbol);
                if(symbol.isMask()) {
                    maskedRemoved++;
                }
            }

            if (count != 0) {
                CharSequence insertText = s.subSequence(start, start + count);
                insert(insertText, maskedRemoved + mUsedSymbols.size() + mAvailableSymbols.size() - getText().length())
                //temp =  + temp;
            }

        }

        private void insert(CharSequence text, int count, boolean isInserted) {
            for (int i = 0; i < text.length() && i < count && mAvailableSymbols.size() > 0; ) {
                Symbol symbol = mAvailableSymbols.get(0);
                if (symbol.isMask()) {
                    for (; i < text.length(); i++) {
                        if (((MaskSymbol) symbol).trySetChar(text.charAt(i))) {
                            mUsedSymbols.add(mAvailableSymbols.remove(0));
                            i++;
                            break;
                        }
                        mCursorPosition--;
                    }
                } else {
                    if (symbol.getChar() == text.charAt(i)) {
                        i++;
                    } else if(isInserted) {
                        mCursorPosition++;
                    }
                    mUsedSymbols.add(mAvailableSymbols.remove(0));
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = "";
            for (Symbol symbol : mUsedSymbols) {
                text += symbol.getChar();
            }
            removeTextChangedListener(mMaskTextWatcher);
            setText(text);
            if(mCursorPosition > text.length()) {
                mCursorPosition = text.length();
            } else if(mCursorPosition < 0) {
                mCursorPosition = 0;
            }
            if (mCursorPosition == mSelectionStart) {
                setText(text);
            }
            addTextChangedListener(mMaskTextWatcher);
            setSelection(mCursorPosition);
        }

    };
}
