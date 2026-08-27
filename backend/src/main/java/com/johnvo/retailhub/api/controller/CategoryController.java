package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.api.exception.ResultResponseMapper;
import com.johnvo.retailhub.application.features.catalog.command.createcategory.CreateCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.createcategory.CreateCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.deletecategory.DeleteCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.deletecategory.DeleteCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.updatecategory.UpdateCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.updatecategory.UpdateCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.query.getcategories.GetCategoriesQuery;
import com.johnvo.retailhub.application.features.catalog.query.getcategories.GetCategoriesQueryHandler;
import com.johnvo.retailhub.application.features.catalog.query.getcategory.GetCategoryQuery;
import com.johnvo.retailhub.application.features.catalog.query.getcategory.GetCategoryQueryHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CreateCategoryCommandHandler create;
    private final UpdateCategoryCommandHandler update;
    private final DeleteCategoryCommandHandler delete;
    private final GetCategoriesQueryHandler list;
    private final GetCategoryQueryHandler get;
    private final ResultResponseMapper results;

    public CategoryController(CreateCategoryCommandHandler create, UpdateCategoryCommandHandler update,
                              DeleteCategoryCommandHandler delete, GetCategoriesQueryHandler list,
                              GetCategoryQueryHandler get, ResultResponseMapper results) {
        this.create = create;
        this.update = update;
        this.delete = delete;
        this.list = list;
        this.get = get;
        this.results = results;
    }

    @GetMapping
    ResponseEntity<?> list() {
        return results.ok(list.handle(new GetCategoriesQuery()));
    }

    @GetMapping("/{id}")
    ResponseEntity<?> get(@PathVariable UUID id) {
        return results.ok(get.handle(new GetCategoryQuery(id)));
    }

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CategoryRequest request) {
        return results.created(create.handle(new CreateCategoryCommand(request.name(), request.description())),
                result -> URI.create("/api/categories/" + result.id()));
    }

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return results.noContent(update.handle(
                new UpdateCategoryCommand(id, request.name(), request.description(), request.active())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable UUID id) {
        return results.noContent(delete.handle(new DeleteCategoryCommand(id)));
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            boolean active
    ) {
    }
}
