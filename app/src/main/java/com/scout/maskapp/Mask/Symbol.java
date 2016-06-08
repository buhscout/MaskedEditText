package com.scout.maskapp.Mask;

/**
 * Created by Scout on 07.06.2016.
 */
abstract class Symbol {
    private char mChar;

    public char getChar() {
        return mChar;
    }

    protected void setChar(char c) {
        mChar = c;
    }
    public abstract boolean isMask();
}
