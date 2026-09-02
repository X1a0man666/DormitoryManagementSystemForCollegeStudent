package com.nchu.dorm.model;

import com.nchu.dorm.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间（宿舍信息组合链：楼栋 -> 房间 -> 床位）。
 * 每个房间包含若干床位，床位可记录入住学生。
 */
public class Room {

    /** 所属楼栋名称 */
    private String buildingName;

    /** 房间号，如 101 */
    private String roomNo;

    /** 所在楼层 */
    private int floor;

    /** 可容纳床位数（南昌航空大学以 4 人间为主） */
    private int capacity;

    /** 床位列表 */
    private final List<Bed> beds = new ArrayList<>();

    public Room() {
    }

    public Room(String buildingName, String roomNo, int floor, int capacity) {
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.floor = floor;
        this.capacity = capacity;
        for (int i = 1; i <= capacity; i++) {
            beds.add(new Bed(i));
        }
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<Bed> getBeds() {
        return beds;
    }

    /** 空床数量 */
    public int availableBedCount() {
        int count = 0;
        for (Bed b : beds) {
            if (b.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** 找到第一个空床，没有则返回 null */
    public Bed findEmptyBed() {
        for (Bed b : beds) {
            if (b.isEmpty()) {
                return b;
            }
        }
        return null;
    }

    /** 是否已住满 */
    public boolean isFull() {
        return availableBedCount() == 0;
    }

    /** 房间是否包含某学生 */
    public boolean containsStudent(String studentId) {
        for (Bed b : beds) {
            if (studentId.equals(b.getOccupantId())) {
                return true;
            }
        }
        return false;
    }

    /** 移除某学生（退宿） */
    public boolean removeStudent(String studentId) {
        for (Bed b : beds) {
            if (studentId.equals(b.getOccupantId())) {
                b.setOccupantId(null);
                return true;
            }
        }
        return false;
    }

    /** 房间展示名，如"1栋-101" */
    public String displayKey() {
        return buildingName + "-" + roomNo;
    }

    /** 文本序列化：buildingName|roomNo|floor|capacity|床位号=学生号;床位号=学生号 */
    public String toLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(TextUtil.escape(buildingName)).append("|")
                .append(TextUtil.escape(roomNo)).append("|")
                .append(floor).append("|")
                .append(capacity).append("|");
        boolean first = true;
        for (Bed b : beds) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(b.getBedNo()).append("=").append(TextUtil.escape(b.getOccupantId()));
        }
        return sb.toString();
    }

    public static Room fromLine(String line) {
        String[] f = TextUtil.split(line);
        Room r = new Room();
        r.buildingName = f[0];
        r.roomNo = f[1];
        r.floor = Integer.parseInt(f[2]);
        r.capacity = Integer.parseInt(f[3]);
        for (int i = 1; i <= r.capacity; i++) {
            r.beds.add(new Bed(i));
        }
        String bedsPart = f[4];
        if (bedsPart != null && !bedsPart.isEmpty()) {
            for (String item : bedsPart.split(";")) {
                int eq = item.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                int bedNo = Integer.parseInt(item.substring(0, eq));
                String occupant = item.substring(eq + 1);
                if (bedNo >= 1 && bedNo <= r.beds.size()) {
                    r.beds.get(bedNo - 1).setOccupantId(occupant.isEmpty() ? null : occupant);
                }
            }
        }
        return r;
    }

    @Override
    public String toString() {
        return displayKey();
    }
}
