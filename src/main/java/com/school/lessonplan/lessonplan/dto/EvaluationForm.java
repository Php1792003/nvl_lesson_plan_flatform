package com.school.lessonplan.lessonplan.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@GroupSequence({EvaluationForm.class, FormatGroup.class})
public class EvaluationForm {

    @NotNull
    private Long versionId;

    @NotBlank
    @Size(min = 5, max = 2000, groups = FormatGroup.class)
    private String content;

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }
}
