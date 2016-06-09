[![](https://jitpack.io/v/buhscout/MaskedEditText.svg)](https://jitpack.io/#buhscout/MaskedEditText)
# MaskedEditText
Android EditText widget with custom masks support

## Adding to project
For use MaskedEditText library, add these dependency to the build.gradle of the module:
### Maven
```
<dependency>
  <groupId>com.github.buhscout</groupId>
  <artifactId>maskededittext</artifactId>
  <version>1.0.4</version>
  <type>pom</type>
</dependency>
```
### Gradle
```
dependencies {
    compile 'com.github.buhscout:maskededittext:1.0.4'
}
```

## Usage

E.g. for phone number with format +7 123 456-78-90:

```
<com.scout.MaskedEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"`
        app:mask="+7 \\d\\d\\d \\d\\d\\d-\\d\\d-\\d\\d"
        app:forward_mask="false"/>
```

If **forward_mask** is true, then mask symbols will be show before last entered symbol

For getting unmasked text just use
```
maskedEditText.getUnmaskedText()
```

### Supports mask symbols:
* **\d** - Any digit
* **\c** - Any letter
* **\C** - Uppercase letter
* **Any custom symbols**

You can create custom mask symbol and use it in the mask:
```
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ...
    maskedEditText = (MaskedEditText)findViewById(R.id.phone);
    maskedEditText.registerMaskSymbol(CountrySymbol.Symbol, CountrySymbol.class);
    maskedEditText.setMask("+\\z \\d\\d\\d \\d\\d\\d-\\d\\d-\\d\\d");
}

/**
* This custom mask symbol forbids to enter any character except '1', '2', '3', 
* where using '\z' mask symbol
**/
public static class CountrySymbol extends MaskedEditText.MaskSymbol {
    // Mask character
    public static final char Symbol = 'z';
    
    // Allowed characters
    private List<Character> mAvailableCharacters = Arrays.asList('1', '2', '7');

    /**
    * Accept or deny input character for this mask symbol
    **/
    @Override
    public boolean isAccept(char inputCharacter) {
        return mAvailableCharacters.contains(inputCharacter);
    }
    
    /**
    * You can change input character before it will be set to EditText
    * This method invoked after isAccept method
    **/
    @Override
    protected void setChar(char inputCharacter) {
        char newCharacter = Character.toUpperCase(inputCharacter);
        super.setChar(newCharacter);
    }
}
```
