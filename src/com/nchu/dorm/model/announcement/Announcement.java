package com.nchu.dorm.model.announcement;

import com.nchu.dorm.util.TextUtil;

/**
 * 全校公告（宿管科发布，全体学生可见）。
 * <p>当前用于「贵重物品遗失」通报：宿管把学生报损的记录上报宿管科，宿管科审核文案后发布，
 * 公告内容形如「某学生（学号：…）xx 物品遗失…」。{@link #getRelatedId()} 保留来源记录编号，
 * 便于回溯公告对应的 {@link com.nchu.dorm.model.record.ValuablesRecord}。</p>
 */
public class Announcement {

    /** 类型：贵重物品遗失通报 */
    public static final String TYPE_LOST_ITEM = "LOST_ITEM";

    /** 公告编号，如 AN0001 */
    private String id;

    /** 类型，见 TYPE_* 常量 */
    private String type;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 发布人工号（宿管科） */
    private String publisherId;

    /** 发布时间 yyyy-MM-dd HH:mm:ss */
    private String publishTime;

    /** 关联的业务记录编号（如贵重物品记录 VA0001），无关联时为空 */
    private String relatedId;

    public Announcement() {
    }

    public Announcement(String id, String type, String title, String content,
                        String publisherId, String publishTime, String relatedId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.publisherId = publisherId;
        this.publishTime = publishTime;
        this.relatedId = relatedId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    /** 类型中文名。 */
    public String getTypeName() {
        if (TYPE_LOST_ITEM.equals(type)) {
            return "物品遗失";
        }
        return "系统公告";
    }

    public String toLine() {
        return TextUtil.escape(id) + "|"
                + TextUtil.escape(type) + "|"
                + TextUtil.escape(title) + "|"
                + TextUtil.escape(content) + "|"
                + TextUtil.escape(publisherId) + "|"
                + TextUtil.escape(publishTime) + "|"
                + TextUtil.escape(relatedId);
    }

    public static Announcement fromLine(String line) {
        String[] f = TextUtil.split(line);
        Announcement a = new Announcement();
        a.id = f[0];
        a.type = f[1];
        a.title = f[2];
        a.content = f[3];
        a.publisherId = f[4];
        a.publishTime = f[5];
        a.relatedId = f.length > 6 ? f[6] : "";
        return a;
    }
}
