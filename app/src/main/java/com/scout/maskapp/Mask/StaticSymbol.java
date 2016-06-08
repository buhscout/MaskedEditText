package com.scout.maskapp.Mask;

class StaticSymbol extends Symbol {
    public StaticSymbol(char c) {
        setChar(c);
    }

    @Override
    public boolean isMask() {
        return false;
    }
}
