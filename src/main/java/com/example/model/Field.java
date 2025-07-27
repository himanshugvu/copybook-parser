package com.example.model;

import java.util.List;

public class Field {
    public int level;
    public String name;
    public String picture;
    public int startPosition;
    public int endPosition;
    public int length;
    public String dataType;
    public String usage;
    public boolean signed;
    public boolean decimal;
    public int decimalPlaces;
    public int occursCount;
    public List<ConditionName> conditionNames;
    public List<ArrayElement> arrayElements;
    public List<Field> fields;
}
