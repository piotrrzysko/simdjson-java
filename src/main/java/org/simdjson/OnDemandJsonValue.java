package org.simdjson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class OnDemandJsonValue {
    private final Map<Object, OnDemandJsonValue> children;
    private OnDemandJsonValue parent;
    private ResolvedClass.ResolvedClassCategory type;
    private Object value;
    private long version;
    private boolean isLeaf;

    public OnDemandJsonValue() {
        this.children = new HashMap<>();
        this.parent = null;
        this.value = null;
        this.version = 0L;
        this.isLeaf = false;
    }

    public Map<Object, OnDemandJsonValue> getChildren() {
        return children;
    }

    public OnDemandJsonValue getParent() {
        return parent;
    }

    public void setParent(OnDemandJsonValue parent) {
        this.parent = parent;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
    public ResolvedClass.ResolvedClassCategory getType() {
        return type;
    }

    public void setType(ResolvedClass.ResolvedClassCategory type) {
        this.type = type;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }
}
