package com.github.cheukbinli.original.sql.parser.model.content;

public class ConditionContent extends BaseContent<String> {
    private static final long serialVersionUID = 2177685197254333533L;

    private String name;
    private Operator operator;
    private Operator leftOperator;

    public enum Operator {
        NotEqual("!=", 110),
        NotLessThan("!<", 110),
        NotGreaterThan("!>", 110),
        GreaterThan(">", 110),
        GreaterThanOrEqual(">=", 110),
        LessThan("<", 110),
        LessThanOrEqual("<=", 110),
        LessThanOrEqualOrGreaterThan("<=>", 110),
        LessThanOrGreater("<>", 110),
        Is("IS", 110),
        Like("LIKE", 110),
        IsNot("IS NOT", 110),
        Escape("ESCAPE", 110),
        RegExp("REGEXP", 110),
        NotRegExp("NOT REGEXP", 110),
        Equality("=", 110),

        BooleanAnd("AND", 140),
        BooleanXor("XOR", 150),
        BooleanOr("OR", 160);


        public final String name;
        public final String name_lcase;
        public final int priority;

        Operator() {
            this(null, 0);
        }

        Operator(String name, int priority) {
            this.name = name;
            this.name_lcase = name.toLowerCase();
            this.priority = priority;
        }

        public String getName() {
            return this.name;
        }

        public int getPriority() {
            return this.priority;
        }
    }

    public ConditionContent() {
        super(ContentType.CONDITION);
    }

    public ConditionContent(Operator leftOperator, String name, Operator operator, String value) {
        super(value, ContentType.CONDITION);
        this.name = name;
        this.operator = operator;
        this.leftOperator = leftOperator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Operator getLeftOperator() {
        return leftOperator;
    }

    public void setLeftOperator(Operator leftOperator) {
        this.leftOperator = leftOperator;
    }
}
