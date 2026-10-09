package com.xpense.dto;

public class AlertDTO {
    private String id;
    private String type;      // "WARN", "DANGER", "INFO"
    private String title;
    private String message;
    private String category;  // "BUDGET", "WALLET"
    private String resourceId;

    public AlertDTO() {
    }

    public AlertDTO(String id, String type, String title, String message, String category, String resourceId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.category = category;
        this.resourceId = resourceId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}
