package com.github.cheukbinli.original.sql.parser.model.content;

import java.io.Serializable;

public class BaseContent<T> implements Serializable {
    private static final long serialVersionUID = -5149965118567590585L;

    public enum ContentType {COLUMN, CONDITION, GROUP_BY, ORDER_BY, OTHER}

    private final ContentType type;

    private T value;

    public BaseContent(ContentType type) {
        this.type = type;
    }

    public BaseContent(T value, ContentType type) {
        this.value = value;
        this.type = type;
    }

    public T getValue() {
        return value;
    }

    public BaseContent setValue(T value) {
        this.value = value;
        return this;
    }

    public ContentType getType() {
        return type;
    }
}
