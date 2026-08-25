package com.ecommerce.productservice.helper;

import java.util.Optional;

import com.ecommerce.productservice.Dto.CategoryDto;
import com.ecommerce.productservice.entity.Category;

public interface CategoryMappingHelper {
    static CategoryDto map(final Category category) {
        CategoryDto.CategoryDtoBuilder builder = CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryTitle(category.getCategoryTitle())
                .imageUrl(category.getImageUrl());

        if (category.getParentCategory() != null) {
                Category parent = category.getParentCategory();

                builder.parentCategoryDto(
                        CategoryDto.builder()
                                .categoryId(parent.getCategoryId())
                                .categoryTitle(parent.getCategoryTitle())
                                .imageUrl(parent.getImageUrl())
                                .build()
                );
        }

        return builder.build();
        }
    static Category map(CategoryDto categoryDto) {
        Category.CategoryBuilder builder = Category.builder()
                .categoryId(categoryDto.getCategoryId())
                .categoryTitle(categoryDto.getCategoryTitle())
                .imageUrl(categoryDto.getImageUrl());

        if (categoryDto.getParentCategoryDto() != null
                && categoryDto.getParentCategoryDto().getCategoryId() != null) {

                builder.parentCategory(
                        Category.builder()
                                .categoryId(
                                        categoryDto.getParentCategoryDto().getCategoryId()
                                )
                                .build()
                );
        }

        return builder.build();
        }

}
