package com.school.lessonplan.lessonplan.dto;

import org.springframework.web.multipart.MultipartFile;

public class PlanVersionUploadForm {

    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
