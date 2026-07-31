package br.com.fiap.postech.carworkshop.workorder.domain.entity;

public enum StatusWO {
    RECEIVED,
    UNDER_DIAGNOSIS,
    PENDING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    CANCELED,
    DELIVERED
}
