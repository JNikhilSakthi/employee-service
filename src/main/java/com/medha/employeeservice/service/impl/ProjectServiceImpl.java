package com.medha.employeeservice.service.impl;

import com.medha.employeeservice.domain.Project;
import com.medha.employeeservice.dto.request.ProjectRequest;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import com.medha.employeeservice.dto.response.ProjectResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.mapper.EmployeeMapper;
import com.medha.employeeservice.mapper.ProjectMapper;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.repository.ProjectRepository;
import com.medha.employeeservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Project", "code", request.code());
        }

        Project project = new Project();
        project.setName(request.name());
        project.setCode(request.code());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        Project saved = projectRepository.save(project);
        return ProjectMapper.toResponse(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = findProjectOrThrow(id);
        return ProjectMapper.toResponse(project, employeeRepository.countByProjectsId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAll(Pageable pageable) {
        return PageResponse.from(projectRepository.findAll(pageable)
                .map(p -> ProjectMapper.toResponse(p, employeeRepository.countByProjectsId(p.getId()))));
    }

    @Override
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findProjectOrThrow(id);

        if (projectRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Project", "code", request.code());
        }

        project.setName(request.name());
        project.setCode(request.code());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        Project saved = projectRepository.save(project);
        return ProjectMapper.toResponse(saved, employeeRepository.countByProjectsId(id));
    }

    @Override
    public void delete(Long id) {
        Project project = findProjectOrThrow(id);

        if (employeeRepository.countByProjectsId(id) > 0) {
            throw new InvalidOperationException(
                    "Project '%s' cannot be deleted while employees are still assigned to it."
                            .formatted(project.getCode()));
        }

        projectRepository.delete(project);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> getMembers(Long projectId, Pageable pageable) {
        findProjectOrThrow(projectId);
        return PageResponse.from(employeeRepository.findByProjectsId(projectId, pageable)
                .map(EmployeeMapper::toSummary));
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }
}
