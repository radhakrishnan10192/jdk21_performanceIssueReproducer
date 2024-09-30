package org.reproducer;

public class WorstEverPojo {
    private String field1;

    public WorstEverPojo() {}

    public WorstEverPojo(final int numChars) {
        final StringBuilder str = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            str.append("a");
        }
        field1 = str.toString();
    }

    public String getField1() {
        return field1;
    }

    public void setField1(final String field1) {
        this.field1 = field1;
    }
}
