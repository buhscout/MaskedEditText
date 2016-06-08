package com.scout.maskededittext.symbols;

public class CharSymbol extends MaskSymbol {
    public static final char MaskChar = 'c';

    @Override
    public boolean trySetChar(char c) {
        if (Character.isLetter(c)) {
            setChar(c);
            return true;
        }
        return false;
    }
}
