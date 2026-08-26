package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.features.catalog.command.createcategory.CreateCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.createcategory.CreateCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.deletecategory.DeleteCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.deletecategory.DeleteCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.updatecategory.UpdateCategoryCommand;
import com.johnvo.retailhub.application.features.catalog.command.updatecategory.UpdateCategoryCommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.CategoryView;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CreateCategoryCommandHandler create;
    private final UpdateCategoryCommandHandler update;
    private final DeleteCategoryCommandHandler delete;
    private final GetCategoriesQueryHandler list;
    private final GetCategoryQueryHandler get;

    public CategoryController(CreateCategoryCommandHandler create, UpdateCategoryCommandHandler update,
                              DeleteCategoryCommandHandler delete, GetCategoriesQueryHandler list,
                              GetCategoryQueryHandler get) {
        this.create = create;
        this.update = update;
        this.delete = delete;
        this.list = list;
        this.get = get;
    }

    @GetMapping
    List<CategoryView> list() {
        return list.handle(new GetCategoriesQuery());
    }

    @GetMapping("/{id}")
    CategoryView get(@PathVariable UUID id) {
        return get.handle(new GetCategoryQuery(id));
    }

    @PostMapping
    ResponseEntity<CreatedId> create(@Valid @RequestBody CategoryRequest request) {
        CreatedId result = create.handle(new CreateCategoryCommand(request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/categories/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        update.handle(new UpdateCategoryCommand(id, request.name(), request.description(), request.active()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        delete.handle(new DeleteCategoryCommand(id));
        return ResponseEntity.noContent().build();
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            boolean active
    ) {
    }
}

