package com.github.cheukbinli.original.sql.parser.model;

import com.github.cheukbinli.original.sql.parser.model.content.BaseContent;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MeatdataInfo<T extends BaseContent> implements Serializable {

    private static final long serialVersionUID = -6446218504234876544L;

    public MeatdataInfo(String name, String alias, List<T> content) {
        this.name = name;
        this.alias = alias;
        this.content = content;
    }

    public MeatdataInfo(String name, String alias, List<T> content, Map<String, Object> additionalParams) {
        this.name = name;
        this.alias = alias;
        this.content = content;
        this.additionalParams = additionalParams;
    }

//    void init() {
//        if (null == content) {
//            return;
//        } else if (content instanceof GroupByContent) {
//            this.metaDataType = MetaDataType.GROUP_BY;
//        } else if (content instanceof ConditionContent) {
//            this.metaDataType = MetaDataType.CONDITION;
//        } else if (content instanceof ColumnContent) {
//            this.metaDataType = MetaDataType.COLUMN;
//        }
//    }

    /***
     * 表命名键值
     */
    private String name;
    /***
     * 别名
     */
    private String alias;
    /***
     * SQL拼接值
     */
    private List<T> content;
    /***
     * 元素类型
     */
//    private MetaDataType metaDataType;

    /***
     * 辅加内容
     */
//    private List<MeatdataInfo> child;

    /***
     * 辅加参数
     */
    private Map<String, Object> additionalParams;


    public String getName() {
        return name;
    }

    public MeatdataInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public MeatdataInfo setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public List<T> getContent() {
        return content;
    }

    public MeatdataInfo setContent(List<T> content) {
        this.content = content;
        return this;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public MeatdataInfo setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
        return this;
    }

    //    public MetaDataType getMetaDataType() {
//        return metaDataType;
//    }
//
//    public MeatdataInfo setMetaDataType(MetaDataType metaDataType) {
//        this.metaDataType = metaDataType;
//        return this;
//    }
//
//    public List<MeatdataInfo> getChild() {
//        return child;
//    }
//
//    public MeatdataInfo setChild(List<MeatdataInfo> child) {
//        this.child = child;
//        return this;
//    }

}
