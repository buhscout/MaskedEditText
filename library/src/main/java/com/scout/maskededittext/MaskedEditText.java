package com.scout.maskededittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.scout.maskededittext.symbols.CharSymbol;
import com.scout.maskededittext.symbols.DecimalSymbol;
import com.scout.maskededittext.symbols.MaskSymbol;
import com.scout.maskededittext.symbols.StaticSymbol;
import com.scout.maskededittext.symbols.Symbol;
import com.scout.maskededittext.symbols.UppercaseCharSymbol;

import java.util.ArrayList;
import java.util.List;

public class MaskedEditText extends AppCompatEditText {
    private MaskTextWatcher mMaskTextWatcher;
    private boolean mIsForwardMask;

    public MaskedEditText(Context context) {
        this(context, null);
    }

    public MaskedEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.editTextStyle);
    }

    public MaskedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.MaskedEditText, defStyleAttr, 0);

        int indexCount = attributes.getIndexCount();
        String mask = null;
        for (int i = 0; i < indexCount; i++) {
            int attribute = attributes.getIndex(i);
            if (attribute == R.styleable.MaskedEditText_mask) {
                mask = attributes.getString(attribute);
            } else if (attribute == R.styleable.MaskedEditText_forward_mask) {
                mIsForwardMask = attributes.getBoolean(attribute, false);
            }
        }
        attributes.recycle();
        setMask(mask);
    }

    public CharSequence getUnmaskedText(){
        return mMaskTextWatcher != null ? mMaskTextWatcher.getUnmaskedText() : getText();
    }

    public CharSequence getMask() {
        return mMaskTextWatcher != null ? mMaskTextWatcher.getMask() : null;
    }

    public void setMask(CharSequence mask) {
        if(TextUtils.equals(getMask(), mask)) {
            return;
        }
        if(mMaskTextWatcher != null) {
            removeTextChangedListener(mMaskTextWatcher);
        }
        if(TextUtils.isEmpty(mask)) {
            mMaskTextWatcher = null;
            return;
        }
        CharSequence text = getUnmaskedText();
        mMaskTextWatcher = new MaskTextWatcher(mask, mIsForwardMask);
        addTextChangedListener(mMaskTextWatcher);
        setText(text);
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

    class MaskTextWatcher implements TextWatcher {
        private CharSequence mMask;
        private ArrayList<Symbol> mAvailableSymbols = new ArrayList<>();
        private ArrayList<Symbol> mUsedSymbols = new ArrayList<>();
        private boolean mIsForwardMask;
        private int mCursorPosition;
        private int mSelectionStart;
        private int mSelectionEnd;
        private CharSequence mTextBefore;

        public MaskTextWatcher(CharSequence mask, boolean showFutureMask) {
            mIsForwardMask = showFutureMask;
            if(TextUtils.isEmpty(mask)) {
                throw new IllegalArgumentException("Mask is null");
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
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mTextBefore = s;
            mSelectionStart = getSelectionStart();
            mSelectionEnd = getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mCursorPosition = start + count;
            if(before == 0 && mAvailableSymbols.size() == 0) {
                mCursorPosition -= count;
                return;
            }

            String rightBuffer = "";
            for (int i = mUsedSymbols.size() - 1; i >= start + before; i--) {
                Symbol symbol = removeSymbol();
                if (i >= start + before && symbol.isMask()) {
                    rightBuffer = symbol.getChar() + rightBuffer;
                }
            }

            if(before == 1 && count == 0 && mSelectionStart == mSelectionEnd) {
                for (int i = mUsedSymbols.size() - 1; i >= 0; i--) {
                    Symbol symbol = mUsedSymbols.get(mUsedSymbols.size() - 1);
                    if(symbol.isMask()) {
                        removeSymbol();
                        break;
                    }
                    if(!mIsForwardMask || isContainsMaskSymbol(mUsedSymbols, 0, mCursorPosition + 1)) {
                        mCursorPosition--;
                        removeSymbol();
                    }
                }
            }

            int maskedRemoved = 0;
            for (int i = mUsedSymbols.size() - 1; i >= start; i--) {
                Symbol symbol = removeSymbol();
                if(symbol.isMask()) {
                    maskedRemoved++;
                }
            }

            if (count != 0) {
                CharSequence insertText = s.subSequence(start, start + count);
                addText(insertText, mUsedSymbols.size() + mAvailableSymbols.size() - mTextBefore.length() + maskedRemoved, true);
            }
            addText(rightBuffer, rightBuffer.length(), false);

            if(!isContainsMaskSymbol(mAvailableSymbols)) {
                addEndStaticSymbols();
            } else {
                boolean isLastPosition = mUsedSymbols.size() == mCursorPosition;
                if(mIsForwardMask) {
                    int addedCount = addEndStaticSymbols();
                    if(addedCount > 0 && isLastPosition) {
                        mCursorPosition += addedCount;
                    }
                } else {
                    int removedCount = removeEndStaticSymbols();
                    if(removedCount > 0 && isLastPosition) {
                        mCursorPosition -= removedCount;
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

        public CharSequence getMask() {
            return mMask;
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

        private boolean isContainsMaskSymbol(List<Symbol> symbols) {
            return isContainsMaskSymbol(symbols, 0, symbols.size());
        }

        private boolean isContainsMaskSymbol(List<Symbol> symbols, int start, int len) {
            for (int i = start; i < len; i++) {
                Symbol symbol = symbols.get(i);
                if (symbol.isMask()) {
                    return true;
                }
            }
            return false;
        }

        private int removeEndStaticSymbols() {
            for(int i = 0; mUsedSymbols.size() > 0; i++) {
                int index = mUsedSymbols.size() - 1;
                Symbol symbol = mUsedSymbols.get(index);
                if(symbol.isMask()) {
                    return i;
                }
                mAvailableSymbols.add(0, mUsedSymbols.remove(index));
            }
            return 0;
        }

        private int addEndStaticSymbols() {
            for(int i = 0; mAvailableSymbols.size() > 0; i++) {
                Symbol symbol = mAvailableSymbols.get(0);
                if(symbol.isMask()) {
                    return i;
                }
                mUsedSymbols.add(mAvailableSymbols.remove(0));
            }
            return 0;
        }

        private Symbol removeSymbol() {
            Symbol symbol = mUsedSymbols.remove(mUsedSymbols.size() - 1);
            mAvailableSymbols.add(0, symbol);
            return  symbol;
        }

        private void addText(CharSequence text, int count, boolean isInserted) {
            for (int i = 0, maskAdded = 0; i < text.length() && maskAdded <= count && mAvailableSymbols.size() > 0; ) {
                Symbol symbol = mAvailableSymbols.get(0);
                if (symbol.isMask()) {
                    for (; i < text.length(); i++) {
                        if (((MaskSymbol) symbol).trySetChar(text.charAt(i))) {
                            mUsedSymbols.add(mAvailableSymbols.remove(0));
                            i++;
                            maskAdded++;
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

    }
}
