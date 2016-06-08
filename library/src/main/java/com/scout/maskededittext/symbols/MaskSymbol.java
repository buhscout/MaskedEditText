package com.scout.maskededittext.symbols;

public abstract class MaskSymbol extends Symbol {
    public abstract boolean trySetChar(char c);

    @Override
    public boolean isMask() {
        return true;
    }
}
