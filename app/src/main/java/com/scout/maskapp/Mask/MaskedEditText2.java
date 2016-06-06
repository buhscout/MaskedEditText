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
        setMask("\\d\\d-\\d\\d\\d-\\d\\d");
    }

    /*public String getUnmaskedText(){
        return TextUtils.isEmpty(mUnmaskedText) ? getText().toString() : mUnmaskedText;
    }

    public Character getMaskSymbol() {
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
        removeTextChangedListener(mMaskTextWatcher);
        if(TextUtils.isEmpty(mask)) {
            return;
        }
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
    }

    TextWatcher mMaskTextWatcher = new TextWatcher() {
        private boolean mIsSelfChange;
        private String mTextBefore;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (!mIsSelfChange) {
                mCursorPosition = getSelectionEnd();
                mTextBefore = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mIsSelfChange) {
                return;
            }
            mIsSelfChange = true;
            if(before == 0 && (count == 0 || mTextBefore.length() >= mMask.length())) {
                setText(mTextBefore);
                return;
            }
            if(before != 0) {
            }
            if(count != 0) {
                String str = "";
                if (mAvailableSymbols.size() > 0) {
                    for (int i = mUsedSymbols.size() - 1; i >= mCursorPosition; i--) {
                        Symbol symbol = mUsedSymbols.remove(i);
                        mAvailableSymbols.add(0, symbol);
                        if (symbol instanceof MaskSymbol) {
                            str = symbol.getChar() + str;
                        }
                    }
                    str = s.subSequence(mCursorPosition, mCursorPosition + count) + str;
                    for (int i = 0; i < str.length() && mAvailableSymbols.size() > 0; i++) {
                        char c = str.charAt(i);
                        Symbol symbol = mAvailableSymbols.get(0);
                        if (symbol instanceof StaticSymbol) {
                            mUsedSymbols.add(symbol);
                            mAvailableSymbols.remove(0);
                            if (symbol.getChar() != c) {
                                i--;
                            }
                            continue;
                        }
                        MaskSymbol maskSymbol = (MaskSymbol) symbol;
                        if (maskSymbol.trySetChar(c)) {
                            mUsedSymbols.add(symbol);
                            mAvailableSymbols.remove(0);
                        }
                    }
                }
            }
            String text = "";
            for (Symbol symbol : mUsedSymbols) {
                text += symbol.getChar();
            }
            setText(text);
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!mIsSelfChange) {
                return;
            }
            //setSelection(mCursorPosition > s.length() ? s.length() : mCursorPosition);
            mIsSelfChange = false;
        }

        private void insert(int position, CharSequence text) {
            mUnmaskedText = mUnmaskedText.substring(0, position)
                    + text
                    + (position > 0 ? mUnmaskedText.substring(position) : "");
            int overlapSize = mUnmaskedText.length() - mSourceMaxLen;
            if(overlapSize > 0) {
                mUnmaskedText = mUnmaskedText.substring(0, mSourceMaxLen);
            }
        }

        private void remove(int start, int len){
            mUnmaskedText = mUnmaskedText.substring(0, start - len)
                    + mUnmaskedText.substring(start);
        }

        private int getSourceRemoveLen(int start, int len) {
            int index = 0;
            boolean charFound = false;
            for (int i = start - 1; i >= 0; i--) {
                if (mMask.charAt(i) == mMaskSymbol) {
                    index++;
                    charFound = true;
                }
                if(charFound && i <= start - len) {
                    break;
                }
            }
            return index;
        }

        private int getSourceStartIndex(int resultStartIndex) {
            int index = 0;
            for (int i = 0; i < resultStartIndex; i++) {
                if (mMask.charAt(i) == mMaskSymbol) {
                    index++;
                }
            }
            return index;
        }

    };
}
