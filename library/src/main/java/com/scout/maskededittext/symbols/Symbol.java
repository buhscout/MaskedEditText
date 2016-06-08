package com.scout.maskededittext.symbols;

public abstract class Symbol {
    private char mChar;

    public char getChar() {
        return mChar;
    }

    protected void setChar(char c) {
        mChar = c;
    }
    public abstract boolean isMask();
}
