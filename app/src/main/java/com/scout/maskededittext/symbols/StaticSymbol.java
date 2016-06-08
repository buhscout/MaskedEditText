package com.scout.maskededittext.symbols;

public class StaticSymbol extends Symbol {
    public StaticSymbol(char c) {
        setChar(c);
    }

    @Override
    public boolean isMask() {
        return false;
    }
}
