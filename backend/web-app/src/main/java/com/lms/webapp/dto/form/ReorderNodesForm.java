package com.lms.webapp.dto.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderNodesForm {

    @NotEmpty(message = "Danh sách sắp xếp không được để trống")
    @Valid
    private List<NodeOrderItem> nodes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeOrderItem {
        @NotNull(message = "ID node không được để trống")
        private String nodeId;
        private String parentId;
        private Integer order;
    }
}
