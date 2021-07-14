package com.github.cheukbinli.original.sql.parser.model.content;

import java.util.List;

public class GroupByContent extends BaseContent<List<String>> {
    public GroupByContent() {
        super(ContentType.GROUP_BY);
    }

    public GroupByContent(List<String> value) {
        super(value, ContentType.GROUP_BY);
    }
}
