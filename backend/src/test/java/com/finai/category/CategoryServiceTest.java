package com.finai.category;

import com.finai.common.ApiException;
import com.finai.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long CATEGORY_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrentUserService currentUserService;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, currentUserService);
        when(currentUserService.requireUserId()).thenReturn(USER_ID);
    }

    private Category category(CategoryType type, boolean system, Long owner, boolean active) {
        Category category = system
                ? Category.system("Cat", "CAT", type, "c", "#000")
                : Category.personal("Cat", "CAT", type, "c", "#000", owner);
        if (!active) {
            category.changeActive(false);
        }
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }

    // ------------------------------------------------------------ requireUsable

    @Test
    @DisplayName("a system category of the right type is usable")
    void acceptsUsableSystemCategory() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CategoryType.EXPENSE, true, null, true)));

        assertThat(service.requireUsable(CATEGORY_ID, CategoryType.EXPENSE, USER_ID))
                .isNotNull();
    }

    @Test
    @DisplayName("a type mismatch is refused with a specific code")
    void rejectsTypeMismatch() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CategoryType.EXPENSE, true, null, true)));

        assertThatThrownBy(() -> service.requireUsable(CATEGORY_ID, CategoryType.INCOME, USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("another user's personal category is refused without confirming it exists")
    void rejectsForeignPersonalCategory() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CategoryType.EXPENSE, false, OTHER_USER_ID, true)));

        assertThatThrownBy(() -> service.requireUsable(CATEGORY_ID, CategoryType.EXPENSE, USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403))
                // The message must not leak the other user's category name.
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("Cat"));
    }

    @Test
    @DisplayName("an inactive category is refused")
    void rejectsInactiveCategory() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CategoryType.EXPENSE, true, null, false)));

        assertThatThrownBy(() -> service.requireUsable(CATEGORY_ID, CategoryType.EXPENSE, USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_INACTIVE"));
    }

    @Test
    @DisplayName("an unknown category is a 400, not a 500")
    void rejectsUnknownCategory() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireUsable(CATEGORY_ID, CategoryType.EXPENSE, USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_NOT_FOUND"));
    }

    // ------------------------------------------------------- personal lifecycle

    @Test
    @DisplayName("a personal category code is derived from the name and made unique")
    void generatesUniqueCode() {
        when(categoryRepository.findByUserIdAndCodeIgnoreCase(eq(USER_ID), any()))
                .thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service.createPersonalCategory(
                new CategoryService.UpsertPersonalCategoryRequest("Coffee & Tea", CategoryType.EXPENSE, "c", "#000"));

        verify(categoryRepository).save(org.mockito.ArgumentMatchers.argThat(
                category -> "COFFEE_TEA".equals(category.getCode())));
    }

    @Test
    @DisplayName("a colliding code gets a numeric suffix rather than failing")
    void disambiguatesCollidingCode() {
        when(categoryRepository.findByUserIdAndCodeIgnoreCase(eq(USER_ID), eq("COFFEE")))
                .thenReturn(Optional.of(category(CategoryType.EXPENSE, false, USER_ID, true)));
        when(categoryRepository.findByUserIdAndCodeIgnoreCase(eq(USER_ID), eq("COFFEE_1")))
                .thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service.createPersonalCategory(
                new CategoryService.UpsertPersonalCategoryRequest("coffee", CategoryType.EXPENSE, "c", "#000"));

        verify(categoryRepository).save(org.mockito.ArgumentMatchers.argThat(
                category -> "COFFEE_1".equals(category.getCode())));
    }

    @Test
    @DisplayName("a personal category defaults to EXPENSE when no type is given")
    void defaultsMissingTypeToExpense() {
        when(categoryRepository.findByUserIdAndCodeIgnoreCase(eq(USER_ID), any()))
                .thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryService.CategoryView view = service.createPersonalCategory(
                new CategoryService.UpsertPersonalCategoryRequest("Misc", null, null, null));

        assertThat(view.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(view.systemCategory()).isFalse();
    }

    @Test
    @DisplayName("a blank category name is rejected")
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.createPersonalCategory(
                new CategoryService.UpsertPersonalCategoryRequest("   ", CategoryType.EXPENSE, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_NAME_REQUIRED"));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivating a personal category keeps the row for history")
    void deactivationKeepsTheRow() {
        Category personal = category(CategoryType.EXPENSE, false, USER_ID, true);
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(personal));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service.deactivatePersonalCategory(CATEGORY_ID);

        assertThat(personal.isActive()).isFalse();
        verify(categoryRepository).save(personal);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("updating someone else's personal category is a 404")
    void cannotUpdateForeignPersonalCategory() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePersonalCategory(CATEGORY_ID,
                new CategoryService.UpsertPersonalCategoryRequest("Hijack", CategoryType.EXPENSE, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_NOT_FOUND"));
        verify(categoryRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ listing

    @Test
    @DisplayName("a user sees the system catalogue plus their own personal categories")
    void listsSystemAndOwnCategories() {
        Category system = category(CategoryType.EXPENSE, true, null, true);
        ReflectionTestUtils.setField(system, "id", 1L);
        Category mine = category(CategoryType.INCOME, false, USER_ID, true);
        ReflectionTestUtils.setField(mine, "id", 2L);
        Category theirs = category(CategoryType.EXPENSE, false, OTHER_USER_ID, true);
        ReflectionTestUtils.setField(theirs, "id", 3L);

        when(categoryRepository.findByUserIdIsNullAndActiveTrueOrderByTypeAscNameAsc())
                .thenReturn(List.of(system));
        when(categoryRepository.findByUserIdAndActiveTrueOrderByTypeAscNameAsc(USER_ID))
                .thenReturn(List.of(mine));

        List<CategoryService.CategoryView> views = service.listForCurrentUser(null);

        assertThat(views).extracting(CategoryService.CategoryView::id)
                .containsExactly(1L, 2L)
                .doesNotContain(3L);
    }

    @Test
    @DisplayName("the type filter narrows the visible catalogue")
    void filtersByType() {
        Category systemExpense = category(CategoryType.EXPENSE, true, null, true);
        ReflectionTestUtils.setField(systemExpense, "id", 1L);
        when(categoryRepository.findByUserIdIsNullAndActiveTrueOrderByTypeAscNameAsc())
                .thenReturn(List.of(systemExpense));
        when(categoryRepository.findByUserIdAndActiveTrueOrderByTypeAscNameAsc(USER_ID))
                .thenReturn(List.of());

        assertThat(service.listForCurrentUser(CategoryType.EXPENSE)).hasSize(1);
        assertThat(service.listForCurrentUser(CategoryType.INCOME)).isEmpty();
    }
}
