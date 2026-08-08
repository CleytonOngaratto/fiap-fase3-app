package br.com.fiap.postech.carworkshop.vehicle.usecase.port.out;

/**
 * OUT-port the vehicle use case uses to enforce the D2 invariant ("a vehicle always has an owner"):
 * it only needs to know whether a customer exists, not the customer's data. Renamed from the former
 * {@code CustomerDataPort} (V7) to avoid the name clash with {@code workorder}'s own port.
 */
public interface CustomerExistencePort {
    boolean existsById(Long customerId);
}
