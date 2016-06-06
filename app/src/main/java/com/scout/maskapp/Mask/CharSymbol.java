package com.scout.maskapp.Mask;

/**
 * Created by Scout on 07.06.2016.
 */
class CharSymbol extends MaskSymbol {
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
