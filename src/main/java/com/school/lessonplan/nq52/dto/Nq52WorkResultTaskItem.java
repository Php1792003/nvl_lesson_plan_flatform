package com.school.lessonplan.nq52.dto;

public class Nq52WorkResultTaskItem {

    private String id;
    private String category; // I, II, III, IV
    private int orderNum;
    private String content; // Nội dung công việc/sản phẩm được giao
    private String nature = "NORMAL"; // NORMAL, COMPLEX, VERY_COMPLEX
    private String role = "LEAD"; // LEAD, COORDINATE
    private String status = "COMPLETED"; // COMPLETED, UNCOMPLETED
    private String timeRequired; // Thời gian yêu cầu của nhiệm vụ đặt ra
    private String timeProgress = "ON_TIME"; // EARLY, ON_TIME, LATE
    private String quality = "STANDARD"; // SUBSTANDARD, STANDARD, EXCEEDED
    private String suggestions; // Kiến nghị, đề xuất
    private String explanation; // Giải trình

    public Nq52WorkResultTaskItem() {}

    public Nq52WorkResultTaskItem(String id, String category, int orderNum, String content) {
        this.id = id;
        this.category = category;
        this.orderNum = orderNum;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimeRequired() { return timeRequired; }
    public void setTimeRequired(String timeRequired) { this.timeRequired = timeRequired; }

    public String getTimeProgress() { return timeProgress; }
    public void setTimeProgress(String timeProgress) { this.timeProgress = timeProgress; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String suggestions) { this.suggestions = suggestions; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}

