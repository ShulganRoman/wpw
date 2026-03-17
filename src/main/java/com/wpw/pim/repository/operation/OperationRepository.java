package com.wpw.pim.repository.operation;

import com.wpw.pim.domain.operation.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, String> {
    List<Operation> findAllByOrderBySortOrder();
}
