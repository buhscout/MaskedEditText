package com.scout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaskedEditText extends AppCompatEditText {
    private MaskTextWatcher mMaskTextWatcher;
    private boolean mIsForwardMask;
    private Map<Character, Class<? extends MaskSymbol>> mSupportSymbols = new HashMap<>();
    private CharSequence mMask;

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
        initSupportSymbols();
        setMask(mask);
    }

    protected void initSupportSymbols() {
        registerMaskSymbol(CharSymbol.MaskChar, CharSymbol.class);
        registerMaskSymbol(DecimalSymbol.MaskChar, DecimalSymbol.class);
        registerMaskSymbol(UppercaseCharSymbol.MaskChar, UppercaseCharSymbol.class);
    }

    public void registerMaskSymbol(char symbol, Class<? extends MaskSymbol> type) {
        if(type == null) {
            throw new IllegalArgumentException("Symbol type is null");
        }
        mSupportSymbols.put(symbol, type);
        if(mMaskTextWatcher != null) {
            refreshMask();
        }
    }

    public CharSequence getUnmaskedText(){
        return mMaskTextWatcher != null ? mMaskTextWatcher.getUnmaskedText() : getText();
    }

    public CharSequence getMask() {
        return mMask;
    }

    public void setMask(CharSequence mask) {
        if(TextUtils.equals(getMask(), mask)) {
            return;
        }
        mMask = mask;
        refreshMask();
    }

    public void refreshMask() {
        if(mMaskTextWatcher != null) {
            removeTextChangedListener(mMaskTextWatcher);
        }
        if(TextUtils.isEmpty(mMask)) {
            mMaskTextWatcher = null;
            return;
        }
        CharSequence value = getUnmaskedText();
        mMaskTextWatcher = new MaskTextWatcher(mMask, mIsForwardMask);
        addTextChangedListener(mMaskTextWatcher);
        setText(value);
    }

    class MaskTextWatcher implements TextWatcher {
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
                char maskChar = mask.charAt(i);
                if(maskChar == '\\') {
                    if(i == mask.length() - 1) {
                        mAvailableSymbols.add(new StaticSymbol(maskChar));
                        break;
                    }
                    char maskValue = mask.charAt(i + 1);
                    Symbol symbol = createMaskSymbol(maskValue);
                    if (symbol != null) {
                        mAvailableSymbols.add(symbol);
                    } else {
                        mAvailableSymbols.add(new StaticSymbol(maskChar));
                        mAvailableSymbols.add(new StaticSymbol(maskValue));
                    }
                    i++;
                } else {
                    mAvailableSymbols.add(new StaticSymbol(maskChar));
                }
            }
        }

        private MaskSymbol createMaskSymbol(char c) {
            Class<?> symbolClass = mSupportSymbols.get(c);
            if(symbolClass != null) {
                Constructor constructor;
                try {
                    constructor = symbolClass.getConstructor();
                    constructor.setAccessible(true);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format("Error create instance of %s.Symbol type must have empty public constructor", symbolClass.getName()), e);
                }
                try {
                    return  (MaskSymbol)constructor.newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format("Error create instance of %s", symbolClass.getName()), e);
                }
            }
            return null;
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

    /************************************** Symbols **********************************************/

    static abstract class Symbol {
        private char mChar;

        public final char getChar() {
            return mChar;
        }

        protected void setChar(char c) {
            mChar = c;
        }

        protected abstract boolean isMask();
    }

    class StaticSymbol extends Symbol {
        public StaticSymbol(char c) {
            setChar(c);
        }

        @Override
        protected boolean isMask() {
            return false;
        }
    }

    public static abstract class MaskSymbol extends Symbol {
        public abstract boolean isAccept(char c);

        protected final boolean trySetChar(char c) {
            if (isAccept(c)) {
                setChar(c);
                return true;
            }
            return false;
        }

        @Override
        protected final boolean isMask() {
            return true;
        }
    }

    /********************************** Mask Symbols **********************************************/

    public static class CharSymbol extends MaskSymbol {
        private static final char MaskChar = 'c';

        @Override
        public boolean isAccept(char c) {
            return Character.isLetter(c);
        }
    }

    public static class DecimalSymbol extends MaskSymbol {
        private static final char MaskChar = 'd';

        @Override
        public boolean isAccept(char c) {
            return Character.isDigit(c);
        }
    }

    public static class UppercaseCharSymbol extends MaskSymbol {
        private static final char MaskChar = 'C';

        @Override
        public boolean isAccept(char c) {
            return Character.isLetter(c);
        }

        @Override
        protected void setChar(char c) {
            if (Character.isLowerCase(c)) {
                c = Character.toUpperCase(c);
            }
            super.setChar(c);
        }
    }
}
