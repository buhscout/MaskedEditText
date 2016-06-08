# MaskedEditText
EditText with mask support

Usage
<com.scout.maskededittext.MaskedEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"`
        android:inputType="number"
        android:hint="Hello World!"
        app:mask="+7 \\d\\d\\d \\d\\d\\d-\\d\\d-\\d\\d"
        app:forward_mask="true"/>
Supports mask symbols:

\d - Any digit
\c - Any char
\C - Uppercase char
If forward_mask is true, then mask symbols will be show before last entered symbol
