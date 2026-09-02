package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 学院信息（对应南昌航空大学真实学院）。
 * 一个学院可被分配若干栋宿舍楼，见 {@link #buildingNames}。
 */
public class College {

    /** 学院代码，如 01、02 */
    private String code;

    /** 学院名称，如"材料科学与工程学院" */
    private String name;

    /** 分配给本学院的宿舍楼栋名称列表 */
    private final List<String> buildingNames = new ArrayList<>();

    public College() {
    }

    public College(String code, String name, List<String> buildingNames) {
        this.code = code;
        this.name = name;
        if (buildingNames != null) {
            this.buildingNames.addAll(buildingNames);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getBuildingNames() {
        return buildingNames;
    }

    /** 文本序列化：code|name|楼栋1;楼栋2 */
    public String toLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(TextUtil.escape(code)).append("|").append(TextUtil.escape(name)).append("|");
        for (int i = 0; i < buildingNames.size(); i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(TextUtil.escape(buildingNames.get(i)));
        }
        return sb.toString();
    }

    public static College fromLine(String line) {
        String[] f = TextUtil.split(line);
        College c = new College();
        c.code = f[0];
        c.name = f[1];
        String buildings = f[2];
        if (buildings != null && !buildings.isEmpty()) {
            for (String b : buildings.split(";")) {
                if (!b.isEmpty()) {
                    c.buildingNames.add(b);
                }
            }
        }
        return c;
    }

    @Override
    public String toString() {
        return name;
    }
}
