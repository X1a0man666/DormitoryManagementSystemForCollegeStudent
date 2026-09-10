package com.nchu.dorm.service;

import com.nchu.dorm.model.Bed;
import com.nchu.dorm.model.Building;
import com.nchu.dorm.model.Room;
import com.nchu.dorm.model.Student;
import com.nchu.dorm.storage.DataCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 宿舍分配推荐：把学生安排到"最能融入"的房间。
 * <p>
 * 融入优先顺序：<b>同班 &gt; 同专业 &gt; 同学院 &gt; 其他</b>；同档位下优先填补空床更少的房间
 * （即先塞进已有人的宿舍，实在无处可融入才启用整间空房），最后按房号数值升序。
 * 三个人以上转入同一专业但不同班时，会依次落进同一间宿舍，而不是各自独占一间。
 * </p>
 * <p>
 * 班级编码 6 位 = 届(2) + 学院(2) + 专业(1) + 班(1)，故：
 * 同班 = 6 位全等；同专业 = 第 3-5 位（学院+专业）相等，跨届亦算同专业；同学院 = 第 3-4 位相等。
 * 编码位置的口径与 {@link DormApplicationService#collegeOfClass} / {@link DormApplicationService#gradeOfClass} 一致。
 * </p>
 * <p>
 * 规则收口在 service，UI 只按本类给出的顺序渲染下拉并预选第一个。
 * </p>
 */
public final class RoomAssignmentService {

    /** 融入档位：同班 */
    public static final int TIER_CLASS = 1;
    /** 融入档位：同专业（跨届） */
    public static final int TIER_MAJOR = 2;
    /** 融入档位：同学院 */
    public static final int TIER_COLLEGE = 3;
    /** 融入档位：其他（含整间空房，无人可融入） */
    public static final int TIER_OTHER = 4;

    private RoomAssignmentService() {
    }

    /** 房间加融入档位的推荐结果。 */
    public static final class Suggestion {

        private final Room room;
        private final int tier;

        Suggestion(Room room, int tier) {
            this.room = room;
            this.tier = tier;
        }

        public Room getRoom() {
            return room;
        }

        public int getTier() {
            return tier;
        }

        /** 档位文案："同班"/"同专业"/"同学院"/"其他"。 */
        public String tag() {
            return tierTag(tier);
        }
    }

    /**
     * 某楼栋内可分配房间，按"融入优先"排序。
     *
     * @param buildingName  目标楼栋
     * @param targetClass   学生将归属的班级编码（转专业换宿须传<b>目标班级</b>，因审批是占床后才改档案）
     * @param excludeRoomNo 需排除的房间号，可为 null（如转宿时排除学生现居房间）
     */
    public static List<Suggestion> suggestRooms(String buildingName, String targetClass, String excludeRoomNo) {
        Map<String, String> classOf = classOfOccupants();
        List<Suggestion> result = new ArrayList<>();
        for (Room r : DataCenter.instance().findRoomsOfBuilding(buildingName)) {
            if (r.isFull()) {
                continue;
            }
            if (excludeRoomNo != null && excludeRoomNo.equals(r.getRoomNo())) {
                continue;
            }
            result.add(new Suggestion(r, tierOf(r, targetClass, classOf)));
        }
        result.sort((a, b) -> {
            if (a.getTier() != b.getTier()) {
                return a.getTier() - b.getTier();
            }
            int freeA = a.getRoom().availableBedCount();
            int freeB = b.getRoom().availableBedCount();
            if (freeA != freeB) {
                return freeA - freeB; // 先塞满已有人的房间，整间空房排到最后
            }
            return roomNoValue(a.getRoom()) - roomNoValue(b.getRoom());
        });
        return result;
    }

    /**
     * 在目标学院的楼栋中挑一栋用于跨学院转专业接收：优先已住有<b>目标班级</b>学生的楼栋，
     * 其次同专业、再次同学院；都没有则退化为任意有空床的楼栋。
     *
     * @return 楼栋名；该学院无性别匹配且有床位空闲的楼栋时返回 null
     */
    public static String recommendBuilding(String collegeCode, String gender, String targetClass) {
        Map<String, String> classOf = classOfOccupants();
        boolean male = "男".equals(gender);
        String best = null;
        int bestTier = Integer.MAX_VALUE;
        int bestCount = -1;
        for (Building b : DataCenter.instance().findBuildingsOfCollege(collegeCode)) {
            if (male != b.isMale()) {
                continue;
            }
            int tier = Integer.MAX_VALUE;
            int sameClassRooms = 0;
            for (Room r : DataCenter.instance().findRoomsOfBuilding(b.getName())) {
                if (r.isFull()) {
                    continue;
                }
                int t = tierOf(r, targetClass, classOf);
                if (t < tier) {
                    tier = t;
                }
                if (t == TIER_CLASS) {
                    sameClassRooms++;
                }
            }
            if (tier == Integer.MAX_VALUE) {
                continue; // 该楼栋无空床
            }
            // 融入档位越靠前越好；同档位时同班房间越多越好；再并列则保持楼栋表原序，结果确定
            if (tier < bestTier || (tier == bestTier && sameClassRooms > bestCount)) {
                bestTier = tier;
                bestCount = sameClassRooms;
                best = b.getName();
            }
        }
        return best;
    }

    /** 档位文案。 */
    public static String tierTag(int tier) {
        switch (tier) {
            case TIER_CLASS:
                return "同班";
            case TIER_MAJOR:
                return "同专业";
            case TIER_COLLEGE:
                return "同学院";
            default:
                return "其他";
        }
    }

    // ==================== 私有工具 ====================

    private static int tierOf(Room room, String targetClass, Map<String, String> classOf) {
        if (targetClass == null || targetClass.length() < 6) {
            return TIER_OTHER;
        }
        int best = TIER_OTHER;
        for (Bed b : room.getBeds()) {
            String occupantId = b.getOccupantId();
            if (occupantId == null) {
                continue;
            }
            int t = tierBetween(targetClass, classOf.get(occupantId));
            if (t < best) {
                best = t;
            }
        }
        return best;
    }

    private static int tierBetween(String targetClass, String occupantClass) {
        if (occupantClass == null || occupantClass.length() < 6) {
            return TIER_OTHER;
        }
        if (targetClass.equals(occupantClass)) {
            return TIER_CLASS;
        }
        if (targetClass.substring(2, 5).equals(occupantClass.substring(2, 5))) {
            return TIER_MAJOR;
        }
        if (targetClass.substring(2, 4).equals(occupantClass.substring(2, 4))) {
            return TIER_COLLEGE;
        }
        return TIER_OTHER;
    }

    /**
     * 学生 id -&gt; 班级编码 快照。
     * 转专业审批会改写学生班级，故每次调用现建，不做跨调用的静态缓存（否则会按过期班级归拢）。
     */
    private static Map<String, String> classOfOccupants() {
        Map<String, String> map = new HashMap<>();
        for (Student s : DataCenter.instance().getStudents()) {
            map.put(s.getId(), s.getClassName());
        }
        return map;
    }

    /** 房号数值，用于排序（字符串比较会出错："1010" &lt; "822"）。非纯数字房号排到最后。 */
    private static int roomNoValue(Room room) {
        try {
            return Integer.parseInt(room.getRoomNo());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
