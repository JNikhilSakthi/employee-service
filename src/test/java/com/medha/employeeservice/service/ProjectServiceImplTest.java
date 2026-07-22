package com.medha.employeeservice.service;

import com.medha.employeeservice.domain.Project;
import com.medha.employeeservice.dto.request.ProjectRequest;
import com.medha.employeeservice.dto.response.ProjectResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.repository.ProjectRepository;
import com.medha.employeeservice.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void create_savesProject_whenCodeIsUnique() {
        ProjectRequest request = new ProjectRequest("Platform Migration", "PRJ-1", "Move to cloud",
                LocalDate.of(2024, 1, 1), null);
        when(projectRepository.existsByCode("PRJ-1")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectResponse response = projectService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("PRJ-1");
        assertThat(response.memberCount()).isZero();
    }

    @Test
    void create_throwsDuplicate_whenCodeAlreadyExists() {
        ProjectRequest request = new ProjectRequest("Platform Migration", "PRJ-1", "Move to cloud",
                LocalDate.of(2024, 1, 1), null);
        when(projectRepository.existsByCode("PRJ-1")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsInvalidOperation_whenProjectHasMembers() {
        Project project = new Project();
        project.setId(2L);
        project.setCode("PRJ-2");

        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(employeeRepository.countByProjectsId(2L)).thenReturn(3L);

        assertThatThrownBy(() -> projectService.delete(2L))
                .isInstanceOf(InvalidOperationException.class);

        verify(projectRepository, never()).delete(any());
    }
}
