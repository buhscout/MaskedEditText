package com.scout.maskapp.Mask;

/**
 * Created by Scout on 07.06.2016.
 */
class DecimalSymbol extends MaskSymbol {
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
