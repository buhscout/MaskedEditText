package com.scout.maskapp.Mask;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

/**
 * Текстовый редактор с маской ввода
 */
public class MaskedEditText extends AppCompatEditText {
    private int mCursorPosition;
    private String mUnmaskedText = "";
    private String mMask = "";
    private Character mMaskSymbol;
    private int mSourceMaxLen;

    public MaskedEditText(Context context) {
        super(context);
        init();
    }

    public MaskedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

    }

    public String getUnmaskedText(){
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
    }

    public String getMask() {
        return mMask;
    }

    public void setMask(String mask) {
        mMask = mask != null ? mask : "";
        mSourceMaxLen = 0;
        removeTextChangedListener(mMaskTextWatcher);
        if(mMaskSymbol == null || TextUtils.isEmpty(mMask)) {
            return;
        }
        for(Character c : mMask.toCharArray()) {
            if(c == mMaskSymbol) {
                mSourceMaxLen++;
            }
        }
        if(TextUtils.isEmpty(mUnmaskedText)){
            mUnmaskedText = getText().toString();
        }
        setText("");
        addTextChangedListener(mMaskTextWatcher);
        setText(mUnmaskedText);
    }

    TextWatcher mMaskTextWatcher = new TextWatcher() {
        private boolean mIsSelfChange;
        private String mTextBefore;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (!mIsSelfChange) {
                mCursorPosition = getSelectionEnd();
                mTextBefore = s.toString();
                if(TextUtils.isEmpty(mUnmaskedText)) {
                    mUnmaskedText = mTextBefore;
                }
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
            int srcIndex = getSourceStartIndex(mCursorPosition);
            if(before != 0) {
                int removeLen = getSourceRemoveLen(mCursorPosition, before);
                remove(srcIndex, removeLen);
                srcIndex -= removeLen;
                int cnt = 0;
                for(int i = mCursorPosition; i >= 0; i--) {
                    if (i - 1 < 0) {
                        cnt = 0;
                        break;
                    }
                    if (mMask.charAt(i - 1) == mMaskSymbol) {
                        break;
                    }
                    cnt++;
                }
                mCursorPosition -= cnt + before;
            }
            if(count != 0) {
                if (mMask.length() > mTextBefore.length() - before) {
                    CharSequence addText = s.subSequence(mCursorPosition, mCursorPosition + count);
                    insert(srcIndex, addText);
                    int insertLen = addText.length();
                    for(int i = mCursorPosition; i < mMask.length() && insertLen > 0; i++) {
                        if(mMask.charAt(i) == mMaskSymbol) {
                            insertLen--;
                        } else {
                            mCursorPosition++;
                        }
                    }
                }
            }
            String targetText = "";
            srcIndex = 0;
            for (int i = 0; i < mMask.length(); i++) {
                Character c = mMask.charAt(i);
                if (srcIndex == mUnmaskedText.length()) {
                    if(mSourceMaxLen != mUnmaskedText.length()) {
                        break;
                    }
                }
                if (c == mMaskSymbol) {
                    targetText += mUnmaskedText.charAt(srcIndex++);
                } else {
                    targetText += c;
                    if(i <= mCursorPosition && mTextBefore.length() <= mCursorPosition) {
                        mCursorPosition++;
                    }
                }
            }
            if (mMask.length() >= s.length()) {
                mCursorPosition = mCursorPosition + count;
            } else {
                mCursorPosition = s.length();
            }
            setText(targetText);
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!mIsSelfChange) {
                return;
            }
            setSelection(mCursorPosition > s.length() ? s.length() : mCursorPosition);
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
