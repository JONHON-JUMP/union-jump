package cn.jonhon.jump.module.mes.process.enums;

public enum YesOrNo {
    YES(1, "Y", "是"),
    NO(0, "N", "否");

    private Integer type;

    private String value;

    private String desc;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    YesOrNo(Integer type, String value, String desc) {
        this.type = type;
        this.value = value;
        this.desc = desc;
    }
}
