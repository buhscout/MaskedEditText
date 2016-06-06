package com.scout.maskapp.Mask;

/**
 * Created by Scout on 07.06.2016.
 */
class UppercaseCharSymbol extends MaskSymbol {
    public static final char MaskChar = 'C';

    @Override
    public boolean trySetChar(char c) {
        if (!Character.isLetter(c)) {
            return false;
        }
        if (Character.isLowerCase(c)) {
            c = Character.toUpperCase(c);
        }
        setChar(c);
        return true;
    }
}
