package com.school.lessonplan.lessonplan.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@GroupSequence({EvaluationReplyForm.class, FormatGroup.class})
public class EvaluationReplyForm {

    @NotBlank
    @Size(min = 2, max = 2000, groups = FormatGroup.class)
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }
}
