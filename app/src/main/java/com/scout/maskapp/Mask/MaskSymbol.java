package com.scout.maskapp.Mask;

/**
 * Created by Scout on 07.06.2016.
 */
abstract class MaskSymbol extends Symbol {
    public abstract boolean trySetChar(char c);

    @Override
    public boolean isMask() {
        return true;
    }
}
