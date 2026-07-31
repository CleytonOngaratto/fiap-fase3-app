package br.com.fiap.postech.carworkshop.workorder.usecase.port.in;

import br.com.fiap.postech.carworkshop.workorder.adapter.dto.DiagnosisRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.WorkOrderRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.DiagnosisResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderDetailResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;

import java.util.List;

public interface WorkOrderUseCase {
    WorkOrderResponse create(WorkOrderRequest request);
    List<WorkOrderDetailResponse> findAll();
    List<WorkOrderDetailResponse> findAllActive();
    WorkOrderDetailResponse findById(Long id);
    DiagnosisResponse completeDiagnosis(Long id, DiagnosisRequest request);
    WorkOrderDetailResponse completeWorkOrder(Long id);
    WorkOrderDetailResponse deliverWorkOrder(Long id);
    void deleteWorkOrder(Long id);
    WorkOrderTrackingResponse findForCustomer(Long id);
    WorkOrderTrackingResponse approveWorkOrder(Long id);
    WorkOrderTrackingResponse rejectWorkOrder(Long id);
    Double getAverageCompletionTimeInHours();
}
