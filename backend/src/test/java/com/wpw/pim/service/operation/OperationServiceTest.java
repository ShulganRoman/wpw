package com.wpw.pim.service.operation;

import com.wpw.pim.domain.operation.Operation;
import com.wpw.pim.repository.operation.OperationRepository;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.operation.ApplicationTagUpsertDto;
import com.wpw.pim.web.dto.operation.OperationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link OperationService}.
 * Покрывают CRUD операций (application tags) и кэширование.
 */
@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock private OperationRepository operationRepository;
    @Mock private ProductService productService;

    @InjectMocks
    private OperationService operationService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("возвращает все операции отсортированные по sortOrder")
        void findAll_returnsSortedOperations() {
            Operation op1 = createOperation("drilling", "Drilling", 0);
            Operation op2 = createOperation("milling", "Milling", 1);

            when(operationRepository.findAllByOrderBySortOrder()).thenReturn(List.of(op1, op2));

            List<Operation> result = operationService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("drilling");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("создаёт операцию из имени (генерирует code)")
        void create_validName_createsOperation() {
            ApplicationTagUpsertDto dto = new ApplicationTagUpsertDto("Surface Processing", 5);
            when(operationRepository.existsById("surface-processing")).thenReturn(false);
            when(operationRepository.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

            OperationDto result = operationService.create(dto);

            assertThat(result.code()).isEqualTo("surface-processing");
            assertThat(result.name()).isEqualTo("Surface Processing");
            assertThat(result.sortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("автоматически назначает sortOrder если не указан")
        void create_noSortOrder_usesRepoCount() {
            ApplicationTagUpsertDto dto = new ApplicationTagUpsertDto("New Tag", null);
            when(operationRepository.existsById("new-tag")).thenReturn(false);
            when(operationRepository.count()).thenReturn(3L);
            when(operationRepository.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

            OperationDto result = operationService.create(dto);

            assertThat(result.sortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("бросает CONFLICT если операция уже существует")
        void create_duplicateCode_throwsConflict() {
            ApplicationTagUpsertDto dto = new ApplicationTagUpsertDto("Drilling", null);
            when(operationRepository.existsById("drilling")).thenReturn(true);

            assertThatThrownBy(() -> operationService.create(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("обновляет имя операции")
        void update_validRequest_updatesName() {
            Operation existing = createOperation("drilling", "Drilling", 0);
            when(operationRepository.findById("drilling")).thenReturn(Optional.of(existing));
            when(operationRepository.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

            OperationDto result = operationService.update("drilling", new ApplicationTagUpsertDto("Deep Drilling", null));

            assertThat(result.name()).isEqualTo("Deep Drilling");
        }

        @Test
        @DisplayName("обновляет sortOrder")
        void update_sortOrder_updated() {
            Operation existing = createOperation("drilling", "Drilling", 0);
            when(operationRepository.findById("drilling")).thenReturn(Optional.of(existing));
            when(operationRepository.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

            OperationDto result = operationService.update("drilling", new ApplicationTagUpsertDto(null, 10));

            assertThat(result.sortOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("бросает NOT_FOUND если операция не найдена")
        void update_notFound_throws404() {
            when(operationRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> operationService.update("nonexistent", new ApplicationTagUpsertDto("X", null)))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет операцию успешно")
        void delete_existing_deletesSuccessfully() {
            when(operationRepository.existsById("drilling")).thenReturn(true);

            operationService.delete("drilling");

            verify(operationRepository).deleteById("drilling");
        }

        @Test
        @DisplayName("бросает NOT_FOUND если операция не найдена")
        void delete_notFound_throws404() {
            when(operationRepository.existsById("nonexistent")).thenReturn(false);

            assertThatThrownBy(() -> operationService.delete("nonexistent"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // --- Helpers ---

    private Operation createOperation(String code, String name, int sortOrder) {
        Operation op = new Operation();
        op.setCode(code);
        op.setName(name);
        op.setNameKey("op." + code);
        op.setSortOrder(sortOrder);
        return op;
    }
}
