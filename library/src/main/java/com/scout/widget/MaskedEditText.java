package com.scout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

public class MaskedEditText extends AppCompatEditText {
    private CharSequence mMask;
    private MaskTextWatcher mMaskTextWatcher;
    private boolean mIsForwardMask;
    private Map<Character, Class<? extends MaskSymbol>> mSupportSymbols;

    public MaskedEditText(Context context) {
        super(context, null);
        init(context, null);
    }

    public MaskedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MaskedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.MaskedEditText);

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

    void initSupportSymbols() {
        mSupportSymbols = new HashMap<>(4);
        registerMaskSymbol(AnySymbol.MaskChar, AnySymbol.class);
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
        private LinkedList<Symbol> mAvailableSymbols = new LinkedList<>();
        private LinkedList<Symbol> mUsedSymbols = new LinkedList<>();
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
                        mAvailableSymbols.addLast(new StaticSymbol(maskChar));
                        break;
                    }
                    char maskValue = mask.charAt(i + 1);
                    Symbol symbol = createMaskSymbol(maskValue);
                    if (symbol != null) {
                        mAvailableSymbols.addLast(symbol);
                    } else {
                        mAvailableSymbols.addLast(new StaticSymbol(maskChar));
                        mAvailableSymbols.addLast(new StaticSymbol(maskValue));
                    }
                    i++;
                } else {
                    mAvailableSymbols.addLast(new StaticSymbol(maskChar));
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
            mTextBefore = s.toString();
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
                    if(mUsedSymbols.getLast().isMask()) {
                        removeSymbol();
                        break;
                    }
                    if(!mIsForwardMask || isContainsMaskSymbol(mUsedSymbols, 0, mCursorPosition + 1)) {
                        mCursorPosition--;
                        removeSymbol();
                    } else {
                        break;
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
                boolean anyUsedSymbolIsMask = isContainsMaskSymbol(mUsedSymbols);
                boolean isLastPosition = mUsedSymbols.size() == mCursorPosition;
                if(mIsForwardMask || !anyUsedSymbolIsMask) {
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
                if(!anyUsedSymbolIsMask) {
                    mCursorPosition = mUsedSymbols.size();
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
                s.replace(0, s.length(), text);
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

        private boolean isContainsMaskSymbol(LinkedList<Symbol> symbols) {
            return isContainsMaskSymbol(symbols, 0, symbols.size());
        }

        private boolean isContainsMaskSymbol(LinkedList<Symbol> symbols, int start, int len) {
            ListIterator<Symbol> iterator = symbols.listIterator(start);
            while (iterator.hasNext() && start++ < len) {
                if (iterator.next().isMask()) {
                    return true;
                }
            }
            return false;
        }

        private int removeEndStaticSymbols() {
            for(int i = 0; mUsedSymbols.size() > 0; i++) {
                if(mUsedSymbols.getLast().isMask()) {
                    return i;
                }
                mAvailableSymbols.addFirst(mUsedSymbols.removeLast());
            }
            return 0;
        }

        private int addEndStaticSymbols() {
            for(int i = 0; mAvailableSymbols.size() > 0; i++) {
                if(mAvailableSymbols.getFirst().isMask()) {
                    return i;
                }
                mUsedSymbols.addLast(mAvailableSymbols.removeFirst());
            }
            return 0;
        }

        private Symbol removeSymbol() {
            Symbol symbol = mUsedSymbols.removeLast();
            mAvailableSymbols.addFirst(symbol);
            return symbol;
        }

        private void addText(CharSequence text, int count, boolean isInserted) {
            for (int i = 0, maskAdded = 0; i < text.length() && maskAdded <= count && mAvailableSymbols.size() > 0; ) {
                Symbol symbol = mAvailableSymbols.getFirst();
                if (symbol.isMask()) {
                    for (; i < text.length(); i++) {
                        if (((MaskSymbol) symbol).trySetChar(text.charAt(i))) {
                            mUsedSymbols.addLast(mAvailableSymbols.removeFirst());
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
                    mUsedSymbols.addLast(mAvailableSymbols.removeFirst());
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

    public static class AnySymbol extends MaskSymbol {
        private static final char MaskChar = '.';

        @Override
        public boolean isAccept(char c) {
            return true;
        }
    }

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
