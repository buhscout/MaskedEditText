package com.scout.maskapp.Mask;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
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
        setMask("++\\d\\d--\\d\\c\\c-\\c\\c");
    }

    public String getUnmaskedText(){
        String text = "";
        for (Symbol symbol : mUsedSymbols) {
            if(symbol instanceof MaskSymbol) {
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

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mSelectionStart = getSelectionStart();
            mCursorPosition = getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int cursorPosition = mCursorPosition;
            String str = "";
            if (before != 0) {
                if(before > mCursorPosition) {
                    mCursorPosition = before;
                    cursorPosition = mCursorPosition;
                }
                int finalPosition = mCursorPosition - before;
                int delCount = before;
                if (count == 0) {
                    if (before == 1 && mSelectionStart == mCursorPosition) {
                        for (int i = mCursorPosition; i > 0; i--) {
                            Symbol symbol = mUsedSymbols.get(i - 1);
                            if (symbol instanceof MaskSymbol) {
                                break;
                            }
                            if(finalPosition > 0) {
                                finalPosition--;
                                delCount++;
                            }
                        }
                    }
                }
                if(finalPosition < 0) {
                    finalPosition = 0;
                }
                for (int i = mUsedSymbols.size() - 1; i >= finalPosition; i--) {
                    Symbol symbol = mUsedSymbols.remove(i);
                    mAvailableSymbols.add(0, symbol);
                    if (symbol instanceof MaskSymbol && i >= finalPosition + delCount) {
                        str = symbol.getChar() + str;
                    }
                    if (i < finalPosition + delCount) {
                        if(mCursorPosition > 0) {
                            mCursorPosition--;
                        }
                    }
                }
                if(!mShowFutureMask) {
                    for (int i = mUsedSymbols.size(); i > 0; i--) {
                        Symbol symbol = mUsedSymbols.get(i - 1);
                        if (symbol instanceof MaskSymbol) {
                            break;
                        }
                        mAvailableSymbols.add(0, symbol);
                        mUsedSymbols.remove(i - 1);
                        if(mCursorPosition > 0) {
                            mCursorPosition--;
                        }
                    }
                }
            }
            if (mAvailableSymbols.size() > 0) {
                if (count != 0) {
                    for (int i = mUsedSymbols.size() - 1; i >= mCursorPosition; i--) {
                        Symbol symbol = mUsedSymbols.remove(i);
                        mAvailableSymbols.add(0, symbol);
                        if (symbol instanceof MaskSymbol) {
                            str = symbol.getChar() + str;
                        }
                    }
                    str = s.subSequence(cursorPosition - before, cursorPosition - before + count) + str;
                }
                int cc = 0;
                for (int i = 0; i < str.length() && mAvailableSymbols.size() > 0; i++) {
                    char c = str.charAt(i);
                    Symbol symbol = mAvailableSymbols.get(0);
                    if (symbol instanceof StaticSymbol) {
                        mUsedSymbols.add(symbol);
                        mAvailableSymbols.remove(0);
                        if (symbol.getChar() != c) {
                            i--;
                        }
                        if (cc < count) {
                            mCursorPosition++;
                        }
                        continue;
                    }
                    MaskSymbol maskSymbol = (MaskSymbol) symbol;
                    if (maskSymbol.trySetChar(c)) {
                        mUsedSymbols.add(symbol);
                        mAvailableSymbols.remove(0);
                        if (cc < count) {
                            mCursorPosition++;
                            cc++;
                        }
                    }
                }
                if(mSelectionStart != mCursorPosition - 1 && !mShowFutureMask) {
                    for (int i = mCursorPosition; i > 0; i--) {
                        Symbol symbol = mUsedSymbols.get(i - 1);
                        if (symbol instanceof MaskSymbol) {
                            for (int j = mCursorPosition; j > i; j--) {
                                symbol = mUsedSymbols.remove(j - 1);
                                mAvailableSymbols.add(0, symbol);
                                if(mCursorPosition > 0) {
                                    mCursorPosition--;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if(mShowFutureMask) {
                if (mSelectionStart == cursorPosition && before - count == 1) {
                    if (mUsedSymbols.size() > 0 && mAvailableSymbols.size() > 0) {
                        Symbol symbol = mAvailableSymbols.get(0);
                        if (symbol instanceof StaticSymbol) {
                            symbol = mUsedSymbols.get(mUsedSymbols.size() - 1);
                            mUsedSymbols.remove(mUsedSymbols.size() - 1);
                            mAvailableSymbols.add(0, symbol);
                            if(mCursorPosition > 0) {
                                mCursorPosition--;
                            }
                            if (symbol instanceof StaticSymbol) {
                                for (int j = mUsedSymbols.size() - 1; j > 0; j--) {
                                    symbol = mUsedSymbols.get(j);
                                    mUsedSymbols.remove(j);
                                    mAvailableSymbols.add(0, symbol);
                                    if(mCursorPosition > 0) {
                                        mCursorPosition--;
                                    }
                                    if (symbol instanceof MaskSymbol) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                for (int i = 0; i < mAvailableSymbols.size(); i++) {
                    Symbol symbol = mAvailableSymbols.get(0);
                    if (symbol instanceof StaticSymbol) {
                        mUsedSymbols.add(symbol);
                        mAvailableSymbols.remove(0);
                        mCursorPosition++;
                    } else {
                        break;
                    }
                }
            }
            if(mCursorPosition == 0 && mUsedSymbols.size() > 0) {
                for(int i = 0; i < mUsedSymbols.size(); i++) {
                    Symbol symbol = mUsedSymbols.get(i);
                    if(symbol instanceof StaticSymbol) {
                        mCursorPosition++;
                    } else {
                        break;
                    }
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
