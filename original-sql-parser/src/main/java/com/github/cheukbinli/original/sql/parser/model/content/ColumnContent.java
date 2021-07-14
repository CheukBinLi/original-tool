package com.github.cheukbinli.original.sql.parser.model.content;

import java.io.Serializable;

public class ColumnContent extends BaseContent<String> implements Serializable {
    private static final long serialVersionUID = 5935355535753509826L;

    public ColumnContent(String value) {
        super(ContentType.COLUMN);
        setValue(value);
    }

    public ColumnContent(String value, ContentType type) {
        super(value, type);
    }
}
