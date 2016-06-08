package com.scout.maskededittext.symbols;

public class DecimalSymbol extends MaskSymbol {
    public static final char MaskChar = 'd';

    @Override
    public boolean trySetChar(char c) {
        if (Character.isDigit(c)) {
            setChar(c);
            return true;
        }
        return false;
    }
}
